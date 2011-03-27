package com.mendhak.gpslogger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.mendhak.gpslogger.helpers.*;
import com.mendhak.gpslogger.interfaces.*;
import com.mendhak.gpslogger.model.*;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class GpsLoggingService extends Service implements IFileLoggingHelperCallback
{
	private static NotificationManager gpsNotifyManager;
	private static int NOTIFICATION_ID;

	/**
	 * General all purpose handler used for updating the UI from threads.
	 */
	public final Handler handler = new Handler();
	Handler autoEmailHandler = new Handler();
	private final IBinder mBinder = new GpsLoggingBinder();
	public static IGpsLoggerServiceClient mainServiceClient;

	// ---------------------------------------------------
	// Helpers and managers
	// ---------------------------------------------------
	GeneralLocationListener gpsLocationListener;
	GeneralLocationListener towerLocationListener;
	SeeMyMapHelper seeMyMapHelper;
	FileLoggingHelper fileHelper;
	public LocationManager gpsLocationManager;
	public LocationManager towerLocationManager;

	Intent alarmIntent;

	// ---------------------------------------------------

	@Override
	public IBinder onBind(Intent arg0)
	{
		Utilities.LogDebug("GpsLoggingService.onBind called");
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		Utilities.LogDebug("GpsLoggingService.onCreate called");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String lang = prefs.getString("locale_override", "");

		if (!lang.equalsIgnoreCase(""))
		{
			Utilities.LogVerbose("Setting app to user specified locale: " + lang);
			Locale locale = new Locale(lang);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		fileHelper = new FileLoggingHelper(this);

		Utilities.LogInfo("GPSLoggerService created");
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		Utilities.LogDebug("GpsLoggingService.onStart called");
		HandleIntent(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{

		Utilities.LogDebug("GpsLoggingService.onStartCommand called");
		HandleIntent(intent);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy()
	{
		Utilities.LogWarning("GpsLoggingService is being destroyed by Android OS.");
		mainServiceClient = null;
		super.onDestroy();
	}

	@Override
	public void onLowMemory()
	{
		Utilities.LogWarning("Android is low on memory.");
		super.onLowMemory();
	}
	
	public void HandleIntent(Intent intent)
	{
		
		Utilities.LogDebug("GpsLoggingService.handleIntent called");
		GetPreferences();
		// SetupAutoEmailTimers();
		
		Utilities.LogDebug("Null intent? " + String.valueOf(intent == null));

		if (intent != null)
		{
			Bundle bundle = intent.getExtras();

			if (bundle != null)
			{
				boolean startRightNow = bundle.getBoolean("immediate");
				boolean alarmWentOff = bundle.getBoolean("alarmWentOff");
				boolean buttonPressed = bundle.getBoolean("buttonPressed");
				
				Utilities.LogDebug("startRightNow - " + String.valueOf(startRightNow));
				Utilities.LogDebug("buttonPressed - " + String.valueOf(buttonPressed));
				Utilities.LogDebug("alarmWentOff - " + String.valueOf(alarmWentOff));
				
				if (startRightNow || buttonPressed)
				{
					Utilities.LogInfo("Auto starting logging");
					
					StartLogging();
				}

				if (alarmWentOff)
				{
				
					Utilities.LogDebug("setEmailReadyToBeSent = true");

					Session.setEmailReadyToBeSent(true);
					AutoEmailLogFile();
				}
				
				if(buttonPressed == false)
				{
					Utilities.LogDebug("buttonPressed - false. Stop logging.");
					StopLogging();
				}
			}
		}
		else
		{
			// A null intent is passed in if the service has been killed and
			// restarted.
			Utilities.LogDebug("Service restarted with null intent. Start logging.");
			StartLogging();

		}
	}

	/**
	 * Can be used from calling classes as the go-between for methods and
	 * properties.
	 * 
	 */
	public class GpsLoggingBinder extends Binder
	{
		public GpsLoggingService getService()
		{
			Utilities.LogDebug("GpsLoggingBinder.getService called.");
			return GpsLoggingService.this;
		}
	}

	/**
	 * Sets up the auto email timers based on user preferences.
	 */
	public void SetupAutoEmailTimers()
	{
		Utilities.LogDebug("GpsLoggingService.SetupAutoEmailTimers called.");
		Utilities.LogDebug("isAutoEmailEnabled - " + String.valueOf(AppSettings.isAutoEmailEnabled()));
		Utilities.LogDebug("Session.getAutoEmailDelay - " + String.valueOf(Session.getAutoEmailDelay()));
		if (AppSettings.isAutoEmailEnabled() && Session.getAutoEmailDelay() > 0)
		{
			Utilities.LogDebug("Setting up email alarm");
			long triggerTime = System.currentTimeMillis()
					+ (long) (Session.getAutoEmailDelay() * 60 * 60 * 1000);

			alarmIntent = new Intent(getBaseContext(), AlarmReceiver.class);

			PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);

		}
		else
		{
			Utilities.LogDebug("Checking if alarmIntent is null");
			if (alarmIntent != null)
			{
				Utilities.LogDebug("alarmIntent was null, canceling alarm");
				CancelAlarm();
			}
		}
	}

	private void CancelAlarm()
	{
		Utilities.LogDebug("GpsLoggingService.CancelAlarm called");

		if (alarmIntent != null)
		{
			Utilities.LogDebug("GpsLoggingService.CancelAlarm called");
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			Utilities.LogDebug("Pending alarm intent was null? " + String.valueOf(sender == null));
			am.cancel(sender);
		}

	}

	/**
	 * Method to be called if user has chosen to auto email log files when he
	 * stops logging
	 */
	private void AutoEmailLogFileOnStop()
	{
		// autoEmailDelay 0 means send it when you stop logging.
		if (AppSettings.isAutoEmailEnabled() && Session.getAutoEmailDelay() == 0)
		{
			Session.setEmailReadyToBeSent(true);
			AutoEmailLogFile();
		}
	}

	/**
	 * Calls the Auto Email Helper which processes the file and sends it.
	 */
	public void AutoEmailLogFile()
	{

		Utilities.LogDebug("GpsLoggingService.AutoEmailLogFile called.");
		Utilities.LogVerbose("isEmailReadyToBeSent - " + Session.isEmailReadyToBeSent());

		// Check that auto emailing is enabled, there's a valid location and
		// file name.
		// if (AppSettings.isAutoEmailEnabled() && Session.hasValidLocation()
		// && Session.getCurrentFileName() != null &&
		// Session.getCurrentFileName().length() > 0)
		if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0
				&& Session.isEmailReadyToBeSent())
		{
			Utilities.LogInfo("Emailing Log File");
			AutoEmailHelper aeh = new AutoEmailHelper(GpsLoggingService.this);
			aeh.SendLogFile(Session.getCurrentFileName(), Utilities.GetPersonId(getBaseContext()));

			SetupAutoEmailTimers();
		}
	}

	public final Runnable updateResultsEmailSendError = new Runnable()
	{
		public void run()
		{
			AutoEmailTooManySentError();
		}
	};

	public final Runnable updateResultsBadGPX = new Runnable()
	{
		public void run()
		{
			AutoEmailBadGPXError();
		}
	};

	private void AutoEmailBadGPXError()
	{
		Utilities.LogWarning("Could not send email, invalid GPS data.");
	}

	private void AutoEmailTooManySentError()
	{
		Utilities.LogWarning("Could not send email, user has exceeded the daily limit.");
	}

	/**
	 * Sets the activity form for this service. The activity form needs to
	 * implement IGpsLoggerServiceClient.
	 * 
	 * @param mainForm
	 */
	public static void SetServiceClient(IGpsLoggerServiceClient mainForm)
	{
		mainServiceClient = mainForm;
	}

	/**
	 * Gets preferences chosen by the user and populates the AppSettings object.
	 * Also sets up email timers if required.
	 */
	public void GetPreferences()
	{
		Utilities.LogDebug("GpsLoggingService.GetPreferences called");
		Utilities.PopulateAppSettings(getBaseContext());

		Utilities.LogDebug("Session.getAutoEmailDelay: " + Session.getAutoEmailDelay());
		Utilities.LogDebug("AppSettings.getAutoEmailDelay: " + AppSettings.getAutoEmailDelay());

		if (Session.getAutoEmailDelay() != AppSettings.getAutoEmailDelay())
		{
			Utilities.LogDebug("Old autoEmailDelay - " + String.valueOf(Session.getAutoEmailDelay())
					+ "; New -" + String.valueOf(AppSettings.getAutoEmailDelay()));
			Session.setAutoEmailDelay(AppSettings.getAutoEmailDelay());
			SetupAutoEmailTimers();
		}
		
	}

	/**
	 * Resets the form, resets file name if required, reobtains preferences
	 */
	public void StartLogging()
	{
		Utilities.LogDebug("GpsLoggingService.StartLogging called");
		Session.setAddNewTrackSegment(true);

		if (Session.isStarted())
		{
			return;
		}

		Utilities.LogInfo("Starting logging procedures");
		startForeground(NOTIFICATION_ID, null);
		Session.setStarted(true);

		GetPreferences();
		Notify();
		ResetCurrentFileName();
		ClearForm();
		StartGpsManager();

	}

	/**
	 * Asks the main service client to clear its form.
	 */
	private void ClearForm()
	{
		if (IsMainFormVisible())
		{
			mainServiceClient.ClearForm();
		}
	}

	/**
	 * Stops logging, removes notification, stops GPS manager, stops email timer
	 */
	public void StopLogging()
	{
		Utilities.LogDebug("GpsLoggingService.StopLogging called");
		Session.setAddNewTrackSegment(true);

		Utilities.LogInfo("Stopping logging");
		Session.setStarted(false);
		// Email log file before setting location info to null
		AutoEmailLogFileOnStop();
		CancelAlarm();
		Session.setCurrentLocationInfo(null);
		stopForeground(true);

		RemoveNotification();
		StopGpsManager();
	}

	/**
	 * Manages the notification in the status bar
	 */
	private void Notify()
	{

		Utilities.LogDebug("GpsLoggingService.Notify called");
		if (AppSettings.shouldShowInNotificationBar())
		{
			gpsNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			ShowNotification();
		}
		else
		{
			RemoveNotification();
		}
	}

	/**
	 * Hides the notification icon in the status bar if it's visible.
	 */
	private void RemoveNotification()
	{
		Utilities.LogDebug("GpsLoggingService.RemoveNotification called");
		try
		{
			if (Session.isNotificationVisible())
			{
				gpsNotifyManager.cancelAll();
			}
		}
		catch (Exception ex)
		{
			Utilities.LogError("RemoveNotification", ex);
		}
		finally
		{
			// notificationVisible = false;
			Session.setNotificationVisible(false);
		}
	}

	/**
	 * Shows a notification icon in the status bar for GPS Logger
	 */
	private void ShowNotification()
	{
		Utilities.LogDebug("GpsLoggingService.ShowNotification called");
		// What happens when the notification item is clicked
		Intent contentIntent = new Intent(this, GpsMainActivity.class);

		PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0, contentIntent,
				android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification nfc = new Notification(R.drawable.gpsstatus5, null, System.currentTimeMillis());
		nfc.flags |= Notification.FLAG_ONGOING_EVENT;

		NumberFormat nf = new DecimalFormat("###.######");

		String contentText = getString(R.string.gpslogger_still_running);
		if (Session.hasValidLocation())
		// if (currentLatitude != 0 && currentLongitude != 0)
		{
			contentText = nf.format(Session.getCurrentLatitude()) + ","
					+ nf.format(Session.getCurrentLongitude());
		}

		nfc.setLatestEventInfo(getBaseContext(), getString(R.string.gpslogger_still_running),
				contentText, pending);

		gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
		Session.setNotificationVisible(true);
	}

	/**
	 * Starts the location manager. There are two location managers - GPS and
	 * Cell Tower. This code determines which manager to request updates from
	 * based on user preference and whichever is enabled. If GPS is enabled on
	 * the phone, that is used. But if the user has also specified that they
	 * prefer cell towers, then cell towers are used. If neither is enabled,
	 * then nothing is requested.
	 */
	public void StartGpsManager()
	{
		Utilities.LogDebug("GpsLoggingService.StartGpsManager");

		GetPreferences();

		gpsLocationListener = new GeneralLocationListener(this);
		towerLocationListener = new GeneralLocationListener(this);

		gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		CheckTowerAndGpsStatus();

		if (Session.isGpsEnabled() && !AppSettings.shouldPreferCellTower())
		{
			Utilities.LogInfo("Requesting GPS location updates");
			// gps satellite based
			gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					AppSettings.getMinimumSeconds() * 1000, AppSettings.getMinimumDistance(),
					gpsLocationListener);

			gpsLocationManager.addGpsStatusListener(gpsLocationListener);

			Session.setUsingGps(true);
		}
		else if (Session.isTowerEnabled())
		{
			Utilities.LogInfo("Requesting tower location updates");
			Session.setUsingGps(false);
			// isUsingGps = false;
			// Cell tower and wifi based
			towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					AppSettings.getMinimumSeconds() * 1000, AppSettings.getMinimumDistance(),
					towerLocationListener);

		}
		else
		{
			Utilities.LogInfo("No provider available");
			Session.setUsingGps(false);
			SetStatus(R.string.gpsprovider_unavailable);
			return;
		}

		SetStatus(R.string.started);
	}

	/**
	 * This method is called periodically to determine whether the cell tower /
	 * gps providers have been enabled, and sets class level variables to those
	 * values.
	 */
	private void CheckTowerAndGpsStatus()
	{
		Session.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
		Session.setGpsEnabled(gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
	}

	/**
	 * Stops the location managers
	 */
	public void StopGpsManager()
	{

		Utilities.LogDebug("GpsLoggingService.StopGpsManager");

		if (towerLocationListener != null)
		{
			towerLocationManager.removeUpdates(towerLocationListener);
		}

		if (gpsLocationListener != null)
		{
			gpsLocationManager.removeUpdates(gpsLocationListener);
			gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
		}

		SetStatus(getString(R.string.stopped));
	}

	/**
	 * Sets the current file name based on user preference.
	 */
	private void ResetCurrentFileName()
	{

		Utilities.LogDebug("GpsLoggingService.ResetCurrentFileName called");

		String newFileName;
		if (AppSettings.shouldCreateNewFileOnceADay())
		{
			// 20100114.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			newFileName = sdf.format(new Date());
			Session.setCurrentFileName(newFileName);
		}
		else
		{
			// 20100114183329.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			newFileName = sdf.format(new Date());
			Session.setCurrentFileName(newFileName);
		}

		if (IsMainFormVisible())
		{
			mainServiceClient.onFileName(newFileName);
		}

	}

	/**
	 * Gives a status message to the main service client to display
	 */
	public void SetStatus(String status)
	{
		if (IsMainFormVisible())
		{
			mainServiceClient.OnStatusMessage(status);
		}
	}

	/**
	 * Gets string from given resource ID, passes to SetStatus(String)
	 * 
	 * @param stringId
	 */
	public void SetStatus(int stringId)
	{
		String s = getString(stringId);
		SetStatus(s);
	}

	/**
	 * Stops location manager, then starts it.
	 */
	public void RestartGpsManagers()
	{
		Utilities.LogDebug("GpsLoggingService.RestartGpsManagers");
		StopGpsManager();
		StartGpsManager();
	}

	/**
	 * Checks to see if providers have been enabled and switches providers based
	 * on user preferences.
	 */
	public void ResetManagersIfRequired()
	{
		CheckTowerAndGpsStatus();

		if (Session.isUsingGps() && AppSettings.shouldPreferCellTower())
		{
			RestartGpsManagers();
		}
		// If GPS is enabled and user doesn't prefer celltowers
		else if (Session.isGpsEnabled() && !AppSettings.shouldPreferCellTower())
		{
			// But we're not *already* using GPS
			if (!Session.isUsingGps())
			{
				RestartGpsManagers();
			}
			// Else do nothing
		}
	}

	/**
	 * This event is raised when the GeneralLocationListener has a new location.
	 * This method in turn updates notification, writes to file, reobtains
	 * preferences, notifies main service client and resets location managers.
	 * 
	 * @param loc
	 */
	public void OnLocationChanged(Location loc)
	{

		// Don't do anything until the proper time has elapsed
		long currentTimeStamp = System.currentTimeMillis();
		if ((currentTimeStamp - Session.getLatestTimeStamp()) < (AppSettings.getMinimumSeconds() * 1000))
		{
			return;
		}

		Utilities.LogInfo("New location obtained");
		Session.setLatestTimeStamp(System.currentTimeMillis());
		Session.setCurrentLocationInfo(loc);
		Notify();
		WriteToFile(loc);
		AutoEmailLogFile();
		GetPreferences();
		ResetManagersIfRequired();

		if (IsMainFormVisible())
		{
			mainServiceClient.OnLocationUpdate(loc);
		}
	}

	/**
	 * Calls file helper to write a given location to a file.
	 * 
	 * @param loc
	 */
	private void WriteToFile(Location loc)
	{
		fileHelper.WriteToFile(loc);
	}

	/**
	 * Informs the main service client of the number of visible satellites.
	 * 
	 * @param count
	 */
	public void SetSatelliteInfo(int count)
	{
		if (IsMainFormVisible())
		{
			mainServiceClient.OnSatelliteCount(count);
		}
	}

	public Activity GetActivity()
	{
		return null;
	}

	public Context GetContext()
	{
		return getBaseContext();
	}

	public boolean IsMainFormVisible()
	{
		return mainServiceClient != null;
	}

	public Context GetMainFormContext()
	{

		return mainServiceClient.GetContext();

	}

}

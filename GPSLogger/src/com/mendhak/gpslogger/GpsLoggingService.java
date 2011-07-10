package com.mendhak.gpslogger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;

import com.mendhak.gpslogger.senders.AlarmReceiver;
import com.mendhak.gpslogger.senders.email.AutoEmailHelper;
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

public class GpsLoggingService extends Service
{
	private static NotificationManager gpsNotifyManager;
	private static int NOTIFICATION_ID;

	/**
	 * General all purpose handler used for updating the UI from threads.
	 */
	public final Handler handler = new Handler();
	private final IBinder mBinder = new GpsLoggingBinder();
	private static IGpsLoggerServiceClient mainServiceClient;

	// ---------------------------------------------------
	// Helpers and managers
	// ---------------------------------------------------
	private GeneralLocationListener gpsLocationListener;
	private GeneralLocationListener towerLocationListener;
	LocationManager gpsLocationManager;
	private LocationManager towerLocationManager;

	private Intent alarmIntent;

	// ---------------------------------------------------

	@Override
	public IBinder onBind(Intent arg0)
	{
		Utilities.LogDebug("GpsLoggingService.onBind");
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		Utilities.LogDebug("GpsLoggingService.onCreate");
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

		Utilities.LogInfo("GPSLoggerService created");
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		Utilities.LogDebug("GpsLoggingService.onStart");
		HandleIntent(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{

		Utilities.LogDebug("GpsLoggingService.onStartCommand");
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
	
	private void HandleIntent(Intent intent)
	{
		
		Utilities.LogDebug("GpsLoggingService.handleIntent");
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
				
				Utilities.LogDebug("startRightNow - " + String.valueOf(startRightNow));

				Utilities.LogDebug("alarmWentOff - " + String.valueOf(alarmWentOff));
				
				if (startRightNow)
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
	class GpsLoggingBinder extends Binder
	{
		public GpsLoggingService getService()
		{
			Utilities.LogDebug("GpsLoggingBinder.getService");
			return GpsLoggingService.this;
		}
	}

	/**
	 * Sets up the auto email timers based on user preferences.
	 */
	private void SetupAutoEmailTimers()
	{
		Utilities.LogDebug("GpsLoggingService.SetupAutoEmailTimers");
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
		Utilities.LogDebug("GpsLoggingService.CancelAlarm");

		if (alarmIntent != null)
		{
			Utilities.LogDebug("GpsLoggingService.CancelAlarm");
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
        Utilities.LogDebug("GpsLoggingService.AutoEmailLogFileOnStop");
        Utilities.LogVerbose("isAutoEmailEnabled - " + AppSettings.isAutoEmailEnabled());
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
	private void AutoEmailLogFile()
	{

		Utilities.LogDebug("GpsLoggingService.AutoEmailLogFile");
		Utilities.LogVerbose("isEmailReadyToBeSent - " + Session.isEmailReadyToBeSent());

		// Check that auto emailing is enabled, there's a valid location and
		// file name.
		if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0
				&& Session.isEmailReadyToBeSent())
		{
			if(IsMainFormVisible())
			{
				Utilities.ShowProgress(mainServiceClient.GetActivity(), getString(R.string.autoemail_sending),
					getString(R.string.please_wait));
			}
			
			Utilities.LogInfo("Emailing Log File");
			AutoEmailHelper aeh = new AutoEmailHelper(GpsLoggingService.this);
			aeh.SendLogFile(Session.getCurrentFileName(), false);
			SetupAutoEmailTimers();
			
			if(IsMainFormVisible())
			{
				Utilities.HideProgress();
			}
		}
	}
	
	protected void ForceEmailLogFile()
	{
		
		Utilities.LogDebug("GpsLoggingService.ForceEmailLogFile");
		if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0)
		{
			if(IsMainFormVisible())
			{
				Utilities.ShowProgress(mainServiceClient.GetActivity(), getString(R.string.autoemail_sending),
					getString(R.string.please_wait));
			}
			
			Utilities.LogInfo("Force emailing Log File");
			AutoEmailHelper aeh = new AutoEmailHelper(GpsLoggingService.this);
			aeh.SendLogFile(Session.getCurrentFileName(), true);
			
			if(IsMainFormVisible())
			{
				Utilities.HideProgress();
			}
		}
	}

	public final Runnable updateResultsEmailSendError = new Runnable()
	{
		public void run()
		{
			AutoEmailGenericError();
		}
	};


	private void AutoEmailGenericError()
	{
		Utilities.LogWarning("Could not send email, please check Internet and auto email settings.");
	}

	/**
	 * Sets the activity form for this service. The activity form needs to
	 * implement IGpsLoggerServiceClient.
	 * 
	 * @param mainForm
	 */
	protected static void SetServiceClient(IGpsLoggerServiceClient mainForm)
	{
		mainServiceClient = mainForm;
	}

	/**
	 * Gets preferences chosen by the user and populates the AppSettings object.
	 * Also sets up email timers if required.
	 */
	private void GetPreferences()
	{
		Utilities.LogDebug("GpsLoggingService.GetPreferences");
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
	protected void StartLogging()
	{
		Utilities.LogDebug("GpsLoggingService.StartLogging");
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
	protected void StopLogging()
	{
		Utilities.LogDebug("GpsLoggingService.StopLogging");
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
        StopMainActivity();
	}

	/**
	 * Manages the notification in the status bar
	 */
	private void Notify()
	{

		Utilities.LogDebug("GpsLoggingService.Notify");
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
		Utilities.LogDebug("GpsLoggingService.RemoveNotification");
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
		Utilities.LogDebug("GpsLoggingService.ShowNotification");
		// What happens when the notification item is clicked
		Intent contentIntent = new Intent(this, GpsMainActivity.class);

		PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0, contentIntent,
				android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification nfc = new Notification(R.drawable.gpsloggericon2, null, System.currentTimeMillis());
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
	private void StartGpsManager()
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
            SetFatalMessage(R.string.gpsprovider_unavailable);
            StopLogging();
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
	private void StopGpsManager()
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

		Utilities.LogDebug("GpsLoggingService.ResetCurrentFileName");

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
     * @param status
     */
	void SetStatus(String status)
	{
		if (IsMainFormVisible())
		{
			mainServiceClient.OnStatusMessage(status);
		}
	}

    /**
     * Gives an error message to the main service client to display
     * @param messageId
     */
    void SetFatalMessage(int messageId)
    {
        if(IsMainFormVisible())
        {
            mainServiceClient.OnFatalMessage(getString(messageId));
        }
    }

	/**
	 * Gets string from given resource ID, passes to SetStatus(String)
	 * 
	 * @param stringId
	 */
	private void SetStatus(int stringId)
	{
		String s = getString(stringId);
		SetStatus(s);
	}

    /**
     * Notifies main form that logging has stopped
     */
    void StopMainActivity()
    {
        if(IsMainFormVisible())
        {
            mainServiceClient.OnStopLogging();
        }
    }


	/**
	 * Stops location manager, then starts it.
	 */
	void RestartGpsManagers()
	{
		Utilities.LogDebug("GpsLoggingService.RestartGpsManagers");
		StopGpsManager();
		StartGpsManager();
	}

	/**
	 * Checks to see if providers have been enabled and switches providers based
	 * on user preferences.
	 */
	private void ResetManagersIfRequired()
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
	void OnLocationChanged(Location loc)
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
        Utilities.LogDebug("GpsLoggingService.WriteToFile");
		List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers();
		for(IFileLogger logger : loggers)
		{
			try
			{
				logger.Write(loc);
				Session.setAllowDescription(true);
			}
			catch (Exception e)
			{
				SetStatus(R.string.could_not_write_to_file);
			}
		}
		
	}

	/**
	 * Informs the main service client of the number of visible satellites.
	 * 
	 * @param count
	 */
	void SetSatelliteInfo(int count)
	{
		if (IsMainFormVisible())
		{
			mainServiceClient.OnSatelliteCount(count);
		}
	}


	private boolean IsMainFormVisible()
	{
		return mainServiceClient != null;
	}


}

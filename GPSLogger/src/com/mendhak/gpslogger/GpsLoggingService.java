package com.mendhak.gpslogger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.mendhak.gpslogger.helpers.AutoEmailHelper;
import com.mendhak.gpslogger.helpers.FileLoggingHelper;
import com.mendhak.gpslogger.helpers.GeneralLocationListener;
import com.mendhak.gpslogger.helpers.SeeMyMapHelper;
import com.mendhak.gpslogger.interfaces.IFileLoggingHelperCallback;
import com.mendhak.gpslogger.interfaces.IGpsLoggerServiceClient;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.model.Session;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Address;
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
	// ---------------------------------------------------


	@Override
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String lang = prefs.getString("locale_override", "");

		if (!lang.equalsIgnoreCase(""))
		{
			Locale locale = new Locale(lang);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		fileHelper = new FileLoggingHelper(this);
	
		Utilities.LogInfo("GPSLogger started");
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		GetPreferences();
		SetupAutoEmailTimers();
		
		Bundle bundle = intent.getExtras();

		if (bundle != null)
		{
			boolean startRightNow = bundle.getBoolean("immediate");
			if (startRightNow)
			{
				Utilities.LogInfo("Auto starting logging");
				StartLogging();
			}
		}

	}

	/**
	 * Can be used from calling classes as the go-between for methods and properties.
	 *
	 */
	public class GpsLoggingBinder extends Binder
	{
		GpsLoggingService getService()
		{
			return GpsLoggingService.this;
		}
	}

	/**
	 * Sets up the auto email timers based on user preferences.
	 */
	private void SetupAutoEmailTimers()
	{
		if (AppSettings.isAutoEmailEnabled() && Session.getAutoEmailDelay() > 0)
		{
			autoEmailHandler.removeCallbacks(autoEmailTimeTask);
			autoEmailHandler.postDelayed(autoEmailTimeTask, Session.getAutoEmailDelay() );
			
		}
	}

	/**
	 * Runnable which can be called from timers, use to auto email and reapply
	 * timer
	 */
	private Runnable autoEmailTimeTask = new Runnable()
	{
		public void run()
		{
			if (AppSettings.isAutoEmailEnabled() && Session.getAutoEmailDelay() > 0)
			{
				AutoEmailLogFile();
				autoEmailHandler.postDelayed(autoEmailTimeTask, Session.getAutoEmailDelay() );
			}

		}
	};

	/**
	 * Method to be called if user has chosen to auto email log files when he
	 * stops logging
	 */
	private void AutoEmailLogFileOnStop()
	{
		// autoEmailDelay 0 means send it when you stop logging.
		if (AppSettings.isAutoEmailEnabled() && Session.getAutoEmailDelay() == 0)
		{
			AutoEmailLogFile();
		}
	}

	/**
	 * Calls the Auto Email Helper which processes the file and sends it.
	 */
	private void AutoEmailLogFile()
	{
		// Check that auto emailing is enabled, there's a valid location and
		// file name.
		if (AppSettings.isAutoEmailEnabled() && Session.hasValidLocation()
				&& Session.getCurrentFileName() != null
				&& Session.getCurrentFileName().length() > 0)
		{
			// Ensure that a point has been logged since the last time we did
			// this.
			// And that we're writing to a file.
			if (Session.getLatestTimeStamp() > Session.getAutoEmailTimeStamp() 
					&& (AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml()))
			{
				Utilities.LogInfo("Auto Email Log File");
				AutoEmailHelper aeh = new AutoEmailHelper(GpsLoggingService.this);
				aeh.SendLogFile(Session.getCurrentFileName(), Utilities.GetPersonId(getBaseContext()));
				Session.setAutoEmailTimeStamp(System.currentTimeMillis());
			}
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
		// Utilities.MsgBox(getString(R.string.error),
		// "GPS data sent is invalid.", this);
	}

	private void AutoEmailTooManySentError()
	{
		Utilities.LogWarning("Could not send email, user has exceeded the daily limit.");
		// Utilities.MsgBox(getString(R.string.error),
		// "You have sent too many emails today", this);
	}

	/**
	 * Returns whether the service is currently logging.
	 * @return
	 */
	public boolean IsRunning()
	{
		return Session.isStarted();
	}

	/**
	 * Sets the activity form for this service. The activity form needs to implement IGpsLoggerServiceClient.
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
		Utilities.PopulateAppSettings(getBaseContext());
		
		if (Session.getAutoEmailDelay() != AppSettings.getAutoEmailDelay() * 3600000)
		{
			Session.setAutoEmailDelay((long) (AppSettings.getAutoEmailDelay() * 3600000));
			SetupAutoEmailTimers();
		}
	}

	/**
	 * Resets the form, resets file name if required, reobtains preferences
	 */
	public void StartLogging()
	{
		Session.setAddNewTrackSegment(true);

		if (Session.isStarted())
		{
			return;
		}

		Utilities.LogInfo("Starting logging procedures");
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
		if (mainServiceClient != null)
		{
			mainServiceClient.ClearForm();
		}
	}

	/**
	 * Stops logging, removes notification, stops GPS manager, stops email timer
	 */
	public void StopLogging()
	{
		Session.setAddNewTrackSegment(true);

		Utilities.LogInfo("Stopping logging");
		Session.setStarted(false);
		Session.setCurrentLocationInfo(null);
		AutoEmailLogFileOnStop();
		RemoveNotification();
		StopGpsManager();
	}

	/**
	 * Manages the notification in the status bar
	 */
	private void Notify()
	{

		Utilities.LogInfo("Notification");
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
		Utilities.LogInfo("Remove notification");
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
			//notificationVisible = false;
			Session.setNotificationVisible(false);
		}
	}

	/**
	 * Shows a notification icon in the status bar for GPS Logger
	 */
	private void ShowNotification()
	{
		// What happens when the notification item is clicked
		Intent contentIntent = new Intent(this, GpsMainActivity.class);

		PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0, contentIntent,
				android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification nfc = new Notification(R.drawable.gpsstatus5, null, System.currentTimeMillis());
		nfc.flags |= Notification.FLAG_ONGOING_EVENT;

		NumberFormat nf = new DecimalFormat("###.######");

		String contentText = getString(R.string.gpslogger_still_running);
		if(Session.hasValidLocation())
		//if (currentLatitude != 0 && currentLongitude != 0)
		{
			contentText = nf.format(Session.getCurrentLatitude()) + "," + nf.format(Session.getCurrentLongitude());
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

		Utilities.LogInfo("Starting GPS Manager");

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
					AppSettings.getMinimumSeconds() * 1000, AppSettings.getMinimumDistance(), gpsLocationListener);

			gpsLocationManager.addGpsStatusListener(gpsLocationListener);

			Session.setUsingGps(true);
		}
		else if (Session.isTowerEnabled())
		{
			Utilities.LogInfo("Requesting tower location updates");
			Session.setUsingGps(false);
			//isUsingGps = false;
			// Cell tower and wifi based
			towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					AppSettings.getMinimumSeconds() * 1000, AppSettings.getMinimumDistance(), towerLocationListener);

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

		Utilities.LogInfo("Stopping GPS managers");

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

		if (AppSettings.shouldCreateNewFileOnceADay())
		{
			// 20100114.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			Session.setCurrentFileName(sdf.format(new Date()));
		}
		else
		{
			// 20100114183329.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			Session.setCurrentFileName(sdf.format(new Date()));
		}

	}

	/**
	 * Gives a status message to the main service client to display
	 */
	public void SetStatus(String status)
	{
		if (mainServiceClient != null)
		{
			mainServiceClient.OnStatusMessage(status);
		}
	}

	/**
	 * Gets string from given resource ID, passes to SetStatus(String)
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
		Utilities.LogInfo("Restarting GPS Managers");
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
			if(!Session.isUsingGps())
			{
				RestartGpsManagers();
			}
			// Else do nothing
		}
	}

	/**
	 * This event is raised when the GeneralLocationListener has a new location.
	 * This method in turn updates notification, writes to file, reobtains preferences, notifies main service client and resets location managers.
	 * @param loc
	 */
	public void OnLocationChanged(Location loc)
	{

		//Don't do anything until the proper time has elapsed
		long currentTimeStamp = System.currentTimeMillis();
		if ((currentTimeStamp - Session.getLatestTimeStamp()) < (AppSettings.getMinimumSeconds() * 1000))
		{
			return;
		}
		
		Session.setLatestTimeStamp(System.currentTimeMillis());
		
		Session.setCurrentLocationInfo(loc);
		Notify();
		WriteToFile(loc);
		GetPreferences();
		ResetManagersIfRequired();

		if (mainServiceClient != null)
		{
			mainServiceClient.OnLocationUpdate(loc);
		}
	}

	/**
	 * Calls file helper to write a given location to a file.
	 * @param loc
	 */
	private void WriteToFile(Location loc)
	{
		fileHelper.WriteToFile(loc);
	}

	/**
	 * Informs the main service client of the number of visible satellites.
	 * @param count
	 */
	public void SetSatelliteInfo(int count)
	{
		if (mainServiceClient != null)
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

}

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
import com.mendhak.gpslogger.interfaces.IGpsLoggerServiceClient;

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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;


public class GpsLoggingService extends Service
{

	// ---------------------------------------------------
	// User Preferences
	// ---------------------------------------------------
	boolean useImperial = false;
	boolean newFileOnceADay;
	boolean preferCellTower;
	public boolean useSatelliteTime;
	public boolean logToKml;
	public boolean logToGpx;
	boolean showInNotificationBar;
	String subdomain;
	int minimumDistance;
	int minimumSeconds;
	String newFileCreation;
	public String seeMyMapUrl;
	public String seeMyMapGuid;
	// ---------------------------------------------------

	/**
	 * General all purpose handler used for updating the UI from threads.
	 */
	public final Handler handler = new Handler();

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

	// ---------------------------------------------------
	// Others
	// ---------------------------------------------------
	boolean towerEnabled;
	boolean gpsEnabled;
	boolean isStarted;
	boolean isUsingGps;
	public String currentFileName;
	public int satellites;
	boolean notificationVisible;

	public double currentLatitude;
	public double currentLongitude;
	long latestTimeStamp;
	long autoEmailTimeStamp;

	public boolean addNewTrackSegment = true;

	private NotificationManager gpsNotifyManager;
	private int NOTIFICATION_ID;
	static final int DATEPICKER_ID = 0;

	private long autoEmailDelay = 0;
	private boolean autoEmailEnabled = false;

	// ---------------------------------------------------

	private final IBinder mBinder = new GpsLoggingBinder();
	public static IGpsLoggerServiceClient MainForm;

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

	}
	
	Handler autoEmailHandler = new Handler();

	/**
	 * Sets up the auto email timers based on user preferences.
	 */
	private void SetupAutoEmailTimers()
	{
		if (autoEmailEnabled && autoEmailDelay > 0)
		{
			autoEmailHandler.removeCallbacks(autoEmailTimeTask);
			autoEmailHandler.postDelayed(autoEmailTimeTask, autoEmailDelay );
			
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
			if (autoEmailEnabled && autoEmailDelay > 0)
			{
				AutoEmailLogFile();
				autoEmailHandler.postDelayed(autoEmailTimeTask, autoEmailDelay );
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
		if (autoEmailEnabled && autoEmailDelay == 0)
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
		if (autoEmailEnabled && currentLatitude != 0 && currentLongitude != 0 && currentFileName != null
				&& currentFileName.length() > 0)
		{
			// Ensure that a point has been logged since the last time we did
			// this.
			// And that we're writing to a file.
			if (latestTimeStamp > autoEmailTimeStamp && (logToGpx || logToKml))
			{
				Utilities.LogInfo("Auto Email Log File");
				AutoEmailHelper aeh = new AutoEmailHelper(GpsLoggingService.this);
				aeh.SendLogFile(currentFileName, Utilities.GetPersonId(getBaseContext()));
				autoEmailTimeStamp = System.currentTimeMillis();
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


	public class GpsLoggingBinder extends Binder
	{
		GpsLoggingService getService()
		{
			return GpsLoggingService.this;
		}
	}

	public boolean IsRunning()
	{
		return isStarted;
	}

	public static void SetParent(IGpsLoggerServiceClient mainForm)
	{
		MainForm = mainForm;
	}

	/**
	 * Gets preferences chosen by the user
	 */
	public void GetPreferences()
	{
		Utilities.LogInfo("Getting preferences");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		useImperial = prefs.getBoolean("useImperial", false);

		useSatelliteTime = prefs.getBoolean("satellite_time", false);

		logToKml = prefs.getBoolean("log_kml", false);
		logToGpx = prefs.getBoolean("log_gpx", false);
		showInNotificationBar = prefs.getBoolean("show_notification", true);

		preferCellTower = prefs.getBoolean("prefer_celltower", false);
		subdomain = prefs.getString("subdomain", "where");

		String minimumDistanceString = prefs.getString("distance_before_logging", "0");

		if (minimumDistanceString != null && minimumDistanceString.length() > 0)
		{
			minimumDistance = Integer.valueOf(minimumDistanceString);
		}
		else
		{
			minimumDistance = 0;
		}

		if (useImperial)
		{
			minimumDistance = Utilities.FeetToMeters(minimumDistance);
		}

		String minimumSecondsString = prefs.getString("time_before_logging", "60");

		if (minimumSecondsString != null && minimumSecondsString.length() > 0)
		{
			minimumSeconds = Integer.valueOf(minimumSecondsString);
		}
		else
		{
			minimumSeconds = 60;
		}

		newFileCreation = prefs.getString("new_file_creation", "onceaday");
		if (newFileCreation.equals("onceaday"))
		{
			newFileOnceADay = true;
		}
		else
		{
			newFileOnceADay = false;
		}

		seeMyMapUrl = prefs.getString("seemymap_URL", "");
		seeMyMapGuid = prefs.getString("seemymap_GUID", "");

		useImperial = prefs.getBoolean("useImperial", false);

		autoEmailEnabled = prefs.getBoolean("autoemail_enabled", false);
		float newAutoEmailDelay = Float.valueOf(prefs.getString("autoemail_frequency", "0"));

		if (autoEmailDelay != newAutoEmailDelay * 3600000)
		{
			autoEmailDelay = (long) (newAutoEmailDelay * 3600000);
			SetupAutoEmailTimers();
		}

	}

	public void StartLogging()
	{
		addNewTrackSegment = true;

		if (isStarted)
		{
			return;
		}

		Utilities.LogInfo("Starting logging procedures");
		isStarted = true;
		GetPreferences();
		Notify();
		ResetCurrentFileName();
		ClearForm();
		StartGpsManager();

	}

	private void ClearForm()
	{
		if (MainForm != null)
		{
			MainForm.ClearForm();
		}

	}

	public void StopLogging()
	{
		addNewTrackSegment = true;

		Utilities.LogInfo("Stopping logging");
		isStarted = false;
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
		if (showInNotificationBar)
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
			if (notificationVisible)
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
			notificationVisible = false;
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
		if (currentLatitude != 0 && currentLongitude != 0)
		{
			contentText = nf.format(currentLatitude) + "," + nf.format(currentLongitude);
		}

		nfc.setLatestEventInfo(getBaseContext(), getString(R.string.gpslogger_still_running),
				contentText, pending);

		gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
		notificationVisible = true;
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

		if (gpsEnabled && !preferCellTower)
		{
			Utilities.LogInfo("Requesting GPS location updates");
			// gps satellite based
			gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					minimumSeconds * 1000, minimumDistance, gpsLocationListener);

			gpsLocationManager.addGpsStatusListener(gpsLocationListener);

			isUsingGps = true;
		}
		else if (towerEnabled)
		{
			Utilities.LogInfo("Requesting tower location updates");
			isUsingGps = false;
			// Cell tower and wifi based
			towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					minimumSeconds * 1000, minimumDistance, towerLocationListener);

		}
		else
		{
			Utilities.LogInfo("No provider available");
			isUsingGps = false;
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
		towerEnabled = towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		gpsEnabled = gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

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

		if (newFileOnceADay)
		{
			// 20100114.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			currentFileName = sdf.format(new Date());
		}
		else
		{
			// 20100114183329.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			currentFileName = sdf.format(new Date());
		}

	}

	public void SetStatus(String status)
	{
		if (MainForm != null)
		{
			MainForm.OnStatusMessage(status);
		}
	}

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

		if (isUsingGps && preferCellTower)
		{
			RestartGpsManagers();
		}
		// If GPS is enabled and user doesn't prefer celltowers
		else if (gpsEnabled && !preferCellTower)
		{
			// But we're not *already* using GPS
			if (!isUsingGps)
			{
				RestartGpsManagers();
			}
			// Else do nothing
		}

	}

	public void OnLocationChanged(Location loc)
	{

		Notify();
		WriteToFile(loc);
		GetPreferences();
		ResetManagersIfRequired();

		if (MainForm != null)
		{
			MainForm.OnLocationUpdate(loc);
		}

	}

	private void WriteToFile(Location loc)
	{
		fileHelper.WriteToFile(loc);

	}

	public void SetSatelliteInfo(int count)
	{
		if (MainForm != null)
		{
			MainForm.OnSatelliteCount(count);
		}

	}

}

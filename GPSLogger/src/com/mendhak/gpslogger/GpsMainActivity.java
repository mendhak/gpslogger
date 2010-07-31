package com.mendhak.gpslogger;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.mendhak.gpslogger.helpers.*;
import com.mendhak.gpslogger.R;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager; //import android.os.AsyncTask;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GpsMainActivity extends Activity implements OnCheckedChangeListener
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

	/**
	 * Event raised when the form is created for the first time
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Utilities.LogInfo("GPSLogger started");

		seeMyMapHelper = new SeeMyMapHelper(this);
		fileHelper = new FileLoggingHelper(this);

		setContentView(R.layout.main);

		ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		buttonOnOff.setOnCheckedChangeListener(this);

		Intent i = getIntent();
		Bundle bundle = i.getExtras();

		if (bundle != null)
		{
			boolean startRightNow = bundle.getBoolean("immediate");
			if (startRightNow)
			{
				Utilities.LogInfo("Auto starting logging");
				buttonOnOff.setChecked(true);
				onCheckedChanged(buttonOnOff, true);
			}
		}

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
				AutoEmailHelper aeh = new AutoEmailHelper(GpsMainActivity.this);
				aeh.SendLogFile(currentFileName, Utilities.GetPersonId(getBaseContext()));
				autoEmailTimeStamp = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Called when the toggle button is clicked
	 */
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		addNewTrackSegment = true;

		try
		{
			if (isChecked)
			{
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
			else
			{
				Utilities.LogInfo("Stopping logging");
				isStarted = false;
				AutoEmailLogFileOnStop();
				RemoveNotification();
				StopGpsManager();
			}

		}
		catch (Exception ex)
		{
			Utilities.LogError("onCheckedChanged", ex);
			SetStatus(getString(R.string.button_click_error) + ex.getMessage());
		}

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
		int newAutoEmailDelay =Integer.valueOf(prefs.getString("autoemail_frequency", "0")); 
		if(autoEmailDelay != newAutoEmailDelay)
		{
			autoEmailDelay = newAutoEmailDelay * 3600000;
			SetupAutoEmailTimers();
		}

		try
		{
			ShowPreferencesSummary();
		}
		catch (Exception ex)
		{
			Utilities.LogError("GetPreferences", ex);
		}
	}

	/**
	 * Displays a human readable summary of the preferences chosen by the user
	 * on the main form
	 */
	private void ShowPreferencesSummary()
	{

		// TextView lblSummary = (TextView) findViewById(R.id.lblSummary);
		TextView txtLoggingTo = (TextView) findViewById(R.id.txtLoggingTo);
		TextView txtFrequency = (TextView) findViewById(R.id.txtFrequency);
		TextView txtDistance = (TextView) findViewById(R.id.txtDistance);
		TextView txtFilename = (TextView) findViewById(R.id.txtFileName);

		if (!logToKml && !logToGpx)
		{
			txtLoggingTo.setText(R.string.summary_loggingto_screen);

		}
		else if (logToGpx && logToKml)
		{
			txtLoggingTo.setText(R.string.summary_loggingto_both);
		}
		else
		{
			txtLoggingTo.setText((logToGpx ? "GPX" : "KML"));

		}

		if (minimumSeconds > 0)
		{
			String descriptiveTime = Utilities.GetDescriptiveTimeString(minimumSeconds, getBaseContext());

			txtFrequency.setText(descriptiveTime);
		}
		else
		{
			txtFrequency.setText(R.string.summary_freq_max);

		}

		if (minimumDistance > 0)
		{

			if (useImperial)
			{
				int minimumDistanceInFeet = Utilities.MetersToFeet(minimumDistance);
				txtDistance.setText(((minimumDistanceInFeet == 1)
					? getString(R.string.foot)
					: String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
			}
			else
			{
				txtDistance.setText(((minimumDistance == 1)
					? getString(R.string.meter)
					: String.valueOf(minimumDistance) + getString(R.string.meters)));
			}

		}
		else
		{
			txtDistance.setText(R.string.summary_dist_regardless);
		}

		if ((logToGpx || logToKml) && (currentFileName != null && currentFileName.length() > 0))
		{
			txtFilename.setText(getString(R.string.summary_current_filename_format, currentFileName));
		}
	}

	/**
	 * Handles the hardware back-button press
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			moveTaskToBack(true);
			Toast.makeText(getBaseContext(), getString(R.string.toast_gpslogger_stillrunning),
					Toast.LENGTH_LONG).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Called when the menu is created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);

		if (!Utilities.Flag())
		{
			menu.getItem(1).setVisible(false);
		}

		return true;

	}

	/**
	 * Called when one of the menu items is selected.
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{

		int itemId = item.getItemId();
		Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

		switch (itemId)
		{
			case R.id.mnuSeeMyMap:
				break;
			case R.id.mnuSettings:
				Intent settingsActivity = new Intent(getBaseContext(), GpsSettingsActivity.class);
				startActivity(settingsActivity);
				break;
			case R.id.mnuShareLatest:
				SendLocation();
				break;
			case R.id.mnuClearMap:
				ClearMap();
				break;
			case R.id.mnuSeeMyMapMore:
				startActivity(new Intent("com.mendhak.gpslogger.SEEMYMAP_SETUP"));
				break;
			case R.id.mnuViewInBrowser:
				ViewInBrowser();
				break;
			case R.id.mnuShareAnnotated:
				showDialog(DATEPICKER_ID);
				break;
			case R.id.mnuAnnotate:
				Annotate();
				break;
			case R.id.mnuShare:
				Share();
				break;
			case R.id.mnuExit:
				RemoveNotification();
				StopGpsManager();
				System.exit(0);
				break;
		}
		return false;
	}

	private void ViewInBrowser()
	{

		seeMyMapHelper.ViewInBrowser();

	}

	/**
	 * Allows user to send a GPX/KML file along with location, or location only
	 * using a provider. 'Provider' means any application that can accept such
	 * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
	 */
	private void Share()
	{

		try
		{

			if (!Utilities.Flag())
			{
				Utilities.MsgBox(getString(R.string.sharing), getString(R.string.sharing_pro), this);
				return;
			}

			final String locationOnly = getString(R.string.sharing_location_only);
			final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
			if (gpxFolder.exists())
			{
				String[] enumeratedFiles = gpxFolder.list();
				List<String> fileList = new ArrayList<String>(Arrays.asList(enumeratedFiles));
				Collections.reverse(fileList);
				fileList.add(0, locationOnly);
				final String[] files = fileList.toArray(new String[0]);

				final Dialog dialog = new Dialog(this);
				dialog.setTitle(R.string.sharing_pick_file);
				dialog.setContentView(R.layout.filelist);
				ListView thelist = (ListView) dialog.findViewById(R.id.listViewFiles);

				thelist.setAdapter(new ArrayAdapter<String>(getBaseContext(),
						android.R.layout.simple_list_item_single_choice, files));

				thelist.setOnItemClickListener(new OnItemClickListener()
				{

					public void onItemClick(AdapterView<?> av, View v, int index, long arg)
					{
						dialog.dismiss();
						String chosenFileName = files[index];

						final Intent intent = new Intent(Intent.ACTION_SEND);

						// intent.setType("text/plain");
						// intent.setType("application/gpx+xml");
						intent.setType("*/*");

						if (chosenFileName.equalsIgnoreCase(locationOnly))
						{
							intent.setType("text/plain");
						}

						intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_mylocation));
						if (currentLatitude != 0 && currentLongitude != 0)
						{
							String bodyText = getString(R.string.sharing_latlong_text,
									String.valueOf(currentLatitude), String.valueOf(currentLongitude));
							intent.putExtra(Intent.EXTRA_TEXT, bodyText);
							intent.putExtra("sms_body", bodyText);
						}

						if (chosenFileName != null && chosenFileName.length() > 0
								&& !chosenFileName.equalsIgnoreCase(locationOnly))
						{
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(gpxFolder,
									chosenFileName)));
						}

						startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));

					}
				});
				dialog.show();
			}

			// startActivity(new Intent("com.mendhak.gpslogger.FILELISTVIEW"));
			// Intent settingsActivity = new
			// Intent(getBaseContext(),FileListViewActivity.class);
			// startActivity(settingsActivity);
		}
		catch (Exception ex)
		{
			Utilities.LogError("Share", ex);
		}

	}

	/**
	 * Called when the date picker dialog is asked for
	 */
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Calendar c = Calendar.getInstance();
		int cyear = c.get(Calendar.YEAR);
		int cmonth = c.get(Calendar.MONTH);
		int cday = c.get(Calendar.DAY_OF_MONTH);
		switch (id)
		{
			case DATEPICKER_ID:
				return new DatePickerDialog(this, dateSetListener, cyear, cmonth, cday);
		}
		return null;
	}

	/**
	 * Handles the 'set' button click in the date picker, and calls SeeMyMap
	 * functionality
	 */
	private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
	{
		// onDateSet method
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
		{

			seeMyMapHelper.SendAnnotatedPointsSince(year, monthOfYear, dayOfMonth);
		}
	};

	/**
	 * Prompts user for input, then adds text to log file
	 */
	private void Annotate()
	{

		fileHelper.Annotate();
	}

	/**
	 * Directly adds the given description to the log file
	 * 
	 * @param desc
	 */
	public void AddNoteToLastPoint(String desc)
	{

		fileHelper.AddNoteToLastPoint(desc);
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

	/**
	 * Update the UI: There was a connection error
	 */
	public final Runnable updateResultsConnectionError = new Runnable()
	{
		public void run()
		{
			ThereWasAConnectionError();
		}
	};

	/**
	 * Update the UI: The point has been added to the map
	 */
	public final Runnable updateResults = new Runnable()
	{
		public void run()
		{
			AddedToMap();
		}
	};

	/**
	 * Update the UI: The map has been cleared
	 */
	public final Runnable updateResultsClearMap = new Runnable()
	{
		public void run()
		{
			MapCleared();
		}
	};

	/**
	 * Update the UI: The points have been read and sent to the map
	 */
	public final Runnable updateResultsSentPoints = new Runnable()
	{
		public void run()
		{
			PointsSent();
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
	 * MessageBox: There was a connection error.
	 */
	private void ThereWasAConnectionError()
	{
		Utilities.MsgBox(getString(R.string.error), getString(R.string.error_connection), this);

	}

	/**
	 * MessageBox: the points have been read and sent to the server
	 */
	private void PointsSent()
	{
		Utilities.MsgBox(getString(R.string.sent), getString(R.string.sent_server), this);
	}

	/**
	 * MessageBox: The point has been sent to the server
	 */
	private void AddedToMap()
	{
		Utilities.MsgBox(getString(R.string.sent), getString(R.string.sent_location), this);
	}

	/**
	 * MessageBox: The map has been cleared
	 */
	private void MapCleared()
	{
		Utilities.MsgBox(getString(R.string.cleared), getString(R.string.cleared_map), this);
	}

	/**
	 * Clears the user's SeeMyMap map
	 */
	private void ClearMap()
	{

		seeMyMapHelper.ClearMap();
	}

	/**
	 * Sends the user input to his SeeMyMap map
	 */
	private void SendLocation()
	{

		seeMyMapHelper.SendAnnotatedPoint();
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

	/**
	 * Clears the table, removes all values.
	 */
	private void ClearForm()
	{

		TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
		TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
		TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

		TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

		TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

		TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
		TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
		TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);

		tvLatitude.setText("");
		tvLongitude.setText("");
		tvDateTime.setText("");
		tvAltitude.setText("");
		txtSpeed.setText("");
		txtSatellites.setText("");
		txtDirection.setText("");
		txtAccuracy.setText("");

	}

	public void SetStatus(int stringId)
	{
		String s = getString(stringId);
		SetStatus(s);
	}

	/**
	 * Sets the message in the top status label.
	 * 
	 * @param message
	 */
	public void SetStatus(String message)
	{
		TextView tvStatus = (TextView) findViewById(R.id.textStatus);
		tvStatus.setText(message);
	}

	/**
	 * Sets the number of satellites in the satellite row in the table.
	 * 
	 * @param number
	 */
	public void SetSatelliteInfo(int number)
	{
		satellites = number;
		TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
		txtSatellites.setText(String.valueOf(number));
	}

	/**
	 * Given a location fix, processes it and displays it in the table on the
	 * form.
	 * 
	 * @param loc
	 */
	public void DisplayLocationInfo(Location loc)
	{
		try
		{

			long currentTimeStamp = System.currentTimeMillis();
			if ((currentTimeStamp - latestTimeStamp) < (minimumSeconds * 1000))
			{
				return;
			}

			latestTimeStamp = System.currentTimeMillis();

			Notify();

			TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
			TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
			TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

			TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

			TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

			TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
			TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
			TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
			String providerName = loc.getProvider();

			if (providerName.equalsIgnoreCase("gps"))
			{
				providerName = getString(R.string.providername_gps);
			}
			else
			{
				providerName = getString(R.string.providername_celltower);
			}

			tvDateTime.setText(new Date().toLocaleString()
					+ getString(R.string.providername_using, providerName));
			tvLatitude.setText(String.valueOf(loc.getLatitude()));
			tvLongitude.setText(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude())
			{

				double altitude = loc.getAltitude();

				if (useImperial)
				{
					tvAltitude.setText(String.valueOf(Utilities.MetersToFeet(altitude))
							+ getString(R.string.feet));
				}
				else
				{
					tvAltitude.setText(String.valueOf(altitude) + getString(R.string.meters));
				}

			}
			else
			{
				tvAltitude.setText(R.string.not_applicable);
			}

			if (loc.hasSpeed())
			{

				float speed = loc.getSpeed();
				if (useImperial)
				{
					txtSpeed.setText(String.valueOf(Utilities.MetersToFeet(speed))
							+ getString(R.string.feet_per_second));
				}
				else
				{
					txtSpeed.setText(String.valueOf(speed) + getString(R.string.meters_per_second));
				}

			}
			else
			{
				txtSpeed.setText(R.string.not_applicable);
			}

			if (loc.hasBearing())
			{

				float bearingDegrees = loc.getBearing();
				String direction = getString(R.string.unknown_direction);

				direction = Utilities.GetBearingDescription(bearingDegrees, getBaseContext());

				txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
						+ getString(R.string.degree_symbol) + ")");
			}
			else
			{
				txtDirection.setText(R.string.not_applicable);
			}

			if (!isUsingGps)
			{
				txtSatellites.setText(R.string.not_applicable);
				satellites = 0;
			}

			if (loc.hasAccuracy())
			{

				float accuracy = loc.getAccuracy();

				if (useImperial)
				{
					txtAccuracy.setText(getString(R.string.accuracy_within,
							String.valueOf(Utilities.MetersToFeet(accuracy)), getString(R.string.feet)));

				}
				else
				{
					txtAccuracy.setText(getString(R.string.accuracy_within, String.valueOf(accuracy),
							getString(R.string.meters)));
				}

			}
			else
			{
				txtAccuracy.setText(R.string.not_applicable);
			}

			WriteToFile(loc);
			GetPreferences();
			ResetManagersIfRequired();

		}
		catch (Exception ex)
		{
			SetStatus(getString(R.string.error_displaying, ex.getMessage()));
		}

	}

	private void WriteToFile(Location loc)
	{

		fileHelper.WriteToFile(loc);

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

}

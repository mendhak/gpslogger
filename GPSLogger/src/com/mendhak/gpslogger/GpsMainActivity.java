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

import com.mendhak.gpslogger.helpers.FileLoggingHelper;
import com.mendhak.gpslogger.helpers.GeneralLocationListener;
import com.mendhak.gpslogger.helpers.SeeMyMapHelper;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ListAdapter;
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

	public boolean addNewTrackSegment = true;

	private NotificationManager gpsNotifyManager;
	private int NOTIFICATION_ID;
	static final int DATEPICKER_ID = 0;

	// ---------------------------------------------------

	/**
	 * Event raised when the form is created for the first time
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		seeMyMapHelper = new SeeMyMapHelper(this);
		fileHelper = new FileLoggingHelper(this);

		setContentView(R.layout.main);

		ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		buttonOnOff.setOnCheckedChangeListener(this);

		GetPreferences();

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

				isStarted = true;
				GetPreferences();
				Notify();
				ResetCurrentFileName();
				ClearForm();
				StartGpsManager();

			}
			else
			{

				isStarted = false;
				RemoveNotification();
				StopGpsManager();
			}

		}
		catch (Exception ex)
		{
			SetStatus("Button click error: " + ex.getMessage());
		}

	}

	/**
	 * Manages the notification in the status bar
	 */
	private void Notify()
	{

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

		try
		{

			if (notificationVisible)
			{
				gpsNotifyManager.cancelAll();
			}
		}
		catch (Exception ex)
		{
			// nothing

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

		String contentText = "GPSLogger is still running.";
		if (currentLatitude != 0 && currentLongitude != 0)
		{
			contentText = nf.format(currentLatitude) + "," + nf.format(currentLongitude);
		}

		nfc.setLatestEventInfo(getBaseContext(), "GPSLogger is running", contentText, pending);

		gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
		notificationVisible = true;

	}

	/**
	 * Gets preferences chosen by the user
	 */
	public void GetPreferences()
	{

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

		try
		{
			ShowPreferencesSummary();
		}
		catch (Exception ex)
		{
			/* Do nothing, displaying a summary should not prevent logging. */
		}
	}

	/**
	 * Displays a human readable summary of the preferences chosen by the user
	 * on the main form
	 */
	private void ShowPreferencesSummary()
	{

		TextView lblSummary = (TextView) findViewById(R.id.lblSummary);
		String summarySentence = "";
		if (!logToKml && !logToGpx)
		{
			summarySentence = "Logging only to screen,";
		}
		else if (logToGpx && logToKml)
		{
			summarySentence = "Logging to both GPX and KML,";
		}
		else
		{
			summarySentence = "Logging to " + (logToGpx ? "GPX," : "KML,");
		}

		if (minimumSeconds > 0)
		{
			String descriptiveTime = Utilities.GetDescriptiveTimeString(minimumSeconds);

			summarySentence = summarySentence + " every " + descriptiveTime;
		}
		else
		{
			summarySentence = summarySentence + " as frequently as possible";
		}

		if (minimumDistance > 0)
		{

			if (useImperial)
			{
				int minimumDistanceInFeet = Utilities.MetersToFeet(minimumDistance);
				summarySentence = summarySentence
						+ " and roughly every "
						+ ((minimumDistanceInFeet == 1)
							? " foot."
							: String.valueOf(minimumDistanceInFeet) + " feet.");
			}
			else
			{
				summarySentence = summarySentence
						+ " and roughly every "
						+ ((minimumDistance == 1) ? " meter." : String.valueOf(minimumDistance)
								+ " meters.");
			}

		}
		else
		{
			summarySentence = summarySentence + ", regardless of distance traveled.";
		}

		if ((logToGpx || logToKml) && (currentFileName != null && currentFileName.length() > 0))
		{
			summarySentence = summarySentence + " The current file name is " + currentFileName
					+ ", which you will find in the GPSLogger folder on your SD card.";
		}

		lblSummary.setText(summarySentence);
	}

	/**
	 * Handles the hardware back-button press
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			moveTaskToBack(true);
			Toast.makeText(getBaseContext(),
					"GPSLogger is still running. You can exit the application from the menu options.",
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

		// Intent sendIntent = new Intent(Intent.ACTION_SEND);
		// sendIntent.putExtra(Intent.EXTRA_TEXT, "email text");
		// sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
		// sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new
		// File
		// ("file:///sdcard/GPSLogger/20100317")));
		// sendIntent.setType("application/gpx+xml");
		// startActivity(Intent.createChooser(sendIntent, "Title:"));
		// return false;
		// }

	}

	/**
	 * Allows user to send a GPX/KML file along with location, or location only using a provider. 'Provider' means 
	 * any application that can accept such an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
	 */
	private void Share()
	{

		try
		{
			final String locationOnly = "Location only";
			final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
			if (gpxFolder.exists())
			{
				String[] enumeratedFiles = gpxFolder.list();
				List<String> fileList = new ArrayList<String>(Arrays.asList(enumeratedFiles));
				Collections.reverse(fileList);
				fileList.add(0, locationOnly);
				final String[] files = fileList.toArray(new String[0]);

				final Dialog dialog = new Dialog(this);
				dialog.setTitle("Pick a file to share");
				dialog.setContentView(R.layout.filelist);
				ListView thelist = (ListView) dialog.findViewById(R.id.listViewFiles);

				thelist.setAdapter(new ArrayAdapter<String>(getBaseContext(),
						android.R.layout.simple_list_item_single_choice, files));

				thelist.setOnItemClickListener(new OnItemClickListener()
				{

					public void onItemClick(AdapterView av, View v, int index, long arg)
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

						intent.putExtra(Intent.EXTRA_SUBJECT, "My location");
						if (currentLatitude != 0 && currentLongitude != 0)
						{
							String bodyText = "Lat:" + String.valueOf(currentLatitude) + ", " + "Long:"
									+ String.valueOf(currentLongitude);
							intent.putExtra(Intent.EXTRA_TEXT, bodyText);
							intent.putExtra("sms_body", bodyText);
						}

						if (chosenFileName != null && chosenFileName.length() > 0
								&& !chosenFileName.equalsIgnoreCase(locationOnly))
						{
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(gpxFolder,
									chosenFileName)));
						}

						startActivity(Intent.createChooser(intent, "Share via"));

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
			System.out.println(ex.getMessage());
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

	/**
	 * MessageBox: There was a connection error.
	 */
	private void ThereWasAConnectionError()
	{
		Utilities.MsgBox("Error", "Connection Error. Please check your phone settings.", this);

	}

	/**
	 * MessageBox: the points have been read and sent to the server
	 */
	private void PointsSent()
	{
		Utilities.MsgBox("Sent", "Points sent to the server", this);
	}

	/**
	 * MessageBox: The point has been sent to the server
	 */
	private void AddedToMap()
	{

		Utilities.MsgBox("Sent", "Location sent to server", this);

	}

	/**
	 * MessageBox: The map has been cleared
	 */
	private void MapCleared()
	{
		Utilities.MsgBox("Cleared", "The map has been cleared", this);
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

		GetPreferences();

		gpsLocationListener = new GeneralLocationListener(this);
		towerLocationListener = new GeneralLocationListener(this);

		gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		CheckTowerAndGpsStatus();

		if (gpsEnabled && !preferCellTower)
		{
			// gps satellite based
			gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					minimumSeconds * 1000, minimumDistance, gpsLocationListener);

			gpsLocationManager.addGpsStatusListener(gpsLocationListener);

			isUsingGps = true;
		}
		else if (towerEnabled)
		{
			isUsingGps = false;
			// Cell tower and wifi based
			towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					minimumSeconds * 1000, minimumDistance, towerLocationListener);

		}
		else
		{
			isUsingGps = false;
			SetStatus("No GPS provider available");
			return;
		}

		SetStatus("Started");
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

		if (towerLocationListener != null)
		{
			towerLocationManager.removeUpdates(towerLocationListener);
		}

		if (gpsLocationListener != null)
		{
			gpsLocationManager.removeUpdates(gpsLocationListener);
			gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
		}

		SetStatus("Stopped");
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
				providerName = "GPS";
			}
			else
			{
				providerName = "cell towers";
			}

			tvDateTime.setText(new Date().toLocaleString() + "    (Using " + providerName + ")");
			tvLatitude.setText(String.valueOf(loc.getLatitude()));
			tvLongitude.setText(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude())
			{

				double altitude = loc.getAltitude();

				if (useImperial)
				{
					tvAltitude.setText(String.valueOf(Utilities.MetersToFeet(altitude)) + " feet");
				}
				else
				{
					tvAltitude.setText(String.valueOf(altitude) + " meters");
				}

			}
			else
			{
				tvAltitude.setText("n/a");
			}

			if (loc.hasSpeed())
			{

				float speed = loc.getSpeed();
				if (useImperial)
				{
					txtSpeed.setText(String.valueOf(Utilities.MetersToFeet(speed)) + " ft/s");
				}
				else
				{
					txtSpeed.setText(String.valueOf(speed) + " m/s");
				}

			}
			else
			{
				txtSpeed.setText("n/a");
			}

			if (loc.hasBearing())
			{

				float bearingDegrees = loc.getBearing();
				String direction = "unknown";

				direction = Utilities.GetBearingDescription(bearingDegrees);

				txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
						+ "\00B0)");
			}
			else
			{
				txtDirection.setText("n/a");
			}

			if (!isUsingGps)
			{
				txtSatellites.setText("n/a");
				satellites = 0;
			}

			if (loc.hasAccuracy())
			{

				float accuracy = loc.getAccuracy();

				if (useImperial)
				{
					txtAccuracy.setText("within " + String.valueOf(Utilities.MetersToFeet(accuracy))
							+ " feet");
				}
				else
				{
					txtAccuracy.setText("within " + String.valueOf(accuracy) + " meters");
				}

			}
			else
			{
				txtAccuracy.setText("n/a");
			}

			WriteToFile(loc);
			GetPreferences();
			ResetManagersIfRequired();

		}
		catch (Exception ex)
		{
			SetStatus("Error in displaying location info: " + ex.getMessage());
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

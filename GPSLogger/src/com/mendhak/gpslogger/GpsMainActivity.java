package com.mendhak.gpslogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager; //import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AutoCompleteTextView.Validator;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GpsMainActivity extends Activity implements
		OnCheckedChangeListener {
	GeneralLocationListener gpsLocationListener;
	GeneralLocationListener towerLocationListener;
	LocationManager gpsLocationManager;
	LocationManager towerLocationManager;

	FileLock gpxLock;
	FileLock kmlLock;

	boolean towerEnabled;
	boolean gpsEnabled;

	boolean isStarted;
	boolean isUsingGps;

	String currentFileName;
	int satellites;

	boolean logToKml;
	boolean logToGpx;
	boolean showInNotificationBar;
	boolean notificationVisible;
	int minimumDistance;
	int minimumSeconds;
	String newFileCreation;
	boolean newFileOnceADay;
	boolean preferCellTower;

	String seeMyMapUrl;
	String seeMyMapGuid;

	double currentLatitude;
	double currentLongitude;
	long latestTimeStamp;

	String subdomain;

	boolean addNewTrackSegment = true;
	boolean allowDescription = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		buttonOnOff.setOnCheckedChangeListener(this);

		GetPreferences();

	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// 

		addNewTrackSegment = true;

		try {

			if (isChecked) {
				if (isStarted) {
					return;
				}

				isStarted = true;
				GetPreferences();
				Notify();
				ResetCurrentFileName();
				ClearForm();
				StartGpsManager();

			} else {

				isStarted = false;
				RemoveNotification();
				StopGpsManager();
			}

		} catch (Exception ex) {
			SetStatus("Button click error: " + ex.getMessage());
		}

	}

	private NotificationManager gpsNotifyManager;
	private int NOTIFICATION_ID;

	private void Notify() {

		if (showInNotificationBar) {
			gpsNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			ShowNotification();
		} else {
			RemoveNotification();
		}
	}

	private void RemoveNotification() {

		try {

			if (notificationVisible) {
				gpsNotifyManager.cancelAll();
			}
		} catch (Exception ex) {
			// nothing

		} finally {
			notificationVisible = false;
		}

	}

	private void ShowNotification() {

		// What happens when the notification item is clicked
		Intent contentIntent = new Intent(this, GpsMainActivity.class);

		PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0,
				contentIntent, android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification nfc = new Notification(R.drawable.gpsstatus5, null, System
				.currentTimeMillis());
		nfc.flags |= Notification.FLAG_ONGOING_EVENT;

		NumberFormat nf = new DecimalFormat("###.####");

		String contentText = "GPSLogger is still running.";
		if (currentLatitude != 0 && currentLongitude != 0) {
			contentText = nf.format(currentLatitude) + ","
					+ nf.format(currentLongitude);
		}

		nfc.setLatestEventInfo(getBaseContext(), "GPSLogger is running",
				contentText, pending);

		gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
		notificationVisible = true;

	}

	private void GetPreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		logToKml = prefs.getBoolean("log_kml", false);
		logToGpx = prefs.getBoolean("log_gpx", false);
		showInNotificationBar = prefs.getBoolean("show_notification", true);

		preferCellTower = prefs.getBoolean("prefer_celltower", false);
		subdomain = prefs.getString("subdomain", "where");

		String minimumDistanceString = prefs.getString(
				"distance_before_logging", "0");

		if (minimumDistanceString != null && minimumDistanceString.length() > 0) {
			minimumDistance = Integer.valueOf(minimumDistanceString);
		} else {
			minimumDistance = 0;
		}

		String minimumSecondsString = prefs.getString("time_before_logging",
				"60");

		if (minimumSecondsString != null && minimumSecondsString.length() > 0) {
			minimumSeconds = Integer.valueOf(minimumSecondsString);
		} else {
			minimumSeconds = 60;
		}

		newFileCreation = prefs.getString("new_file_creation", "onceaday");
		if (newFileCreation.equals("onceaday")) {
			newFileOnceADay = true;
		} else {
			newFileOnceADay = false;
		}

		seeMyMapUrl = prefs.getString("seemymap_URL", "");
		seeMyMapGuid = prefs.getString("seemymap_GUID", "");

		try {
			ShowPreferencesSummary();
		} catch (Exception ex) {
			/* Do nothing, displaying a summary should not prevent logging. */

		}

	}

	private void ShowPreferencesSummary() {

		TextView lblSummary = (TextView) findViewById(R.id.lblSummary);
		String summarySentence = "";
		if (!logToKml && !logToGpx) {
			summarySentence = "Logging only to screen,";
		} else if (logToGpx && logToKml) {
			summarySentence = "Logging to both GPX and KML,";
		} else {
			summarySentence = "Logging to " + (logToGpx ? "GPX," : "KML,");
		}

		if (minimumSeconds > 0) {
			String descriptiveTime = GetDescriptiveTimeString(minimumSeconds);

			summarySentence = summarySentence + " every " + descriptiveTime;
		} else {
			summarySentence = summarySentence + " as frequently as possible";
		}

		if (minimumDistance > 0) {

			summarySentence = summarySentence
					+ " and roughly every "
					+ ((minimumDistance == 1) ? " meter." : String
							.valueOf(minimumDistance)
							+ " meters.");
		} else {
			summarySentence = summarySentence
					+ ", regardless of distance traveled.";
		}

		if ((logToGpx || logToKml)
				&& (currentFileName != null && currentFileName.length() > 0)) {
			summarySentence = summarySentence
					+ " The current file name is "
					+ currentFileName
					+ ", which you will find in the GPSLogger folder on your SD card.";
		}

		lblSummary.setText(summarySentence);
	}

	private String GetDescriptiveTimeString(int numberOfSeconds) {

		String descriptive = "";
		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		int remainingSeconds = 0;

		// Special cases
		if (numberOfSeconds == 1) {
			return "second";
		}

		if (numberOfSeconds == 30) {
			return "half a minute";
		}

		if (numberOfSeconds == 60) {
			return "minute";
		}

		if (numberOfSeconds == 900) {
			return "quarter hour";
		}

		if (numberOfSeconds == 1800) {
			return "half an hour";
		}

		if (numberOfSeconds == 3600) {
			return "hour";
		}

		if (numberOfSeconds == 4800) {
			return "1½ hours";
		}

		if (numberOfSeconds == 9000) {
			return "2½ hours";
		}

		// For all other cases, calculate

		hours = numberOfSeconds / 3600;
		remainingSeconds = numberOfSeconds % 3600;
		minutes = remainingSeconds / 60;
		seconds = remainingSeconds % 60;

		if (hours == 1) {
			descriptive = String.valueOf(hours) + " hour";
		} else if (hours > 1) {
			descriptive = String.valueOf(hours) + " hours";
		}

		if (minutes >= 0 && hours > 0) {
			String joiner = (seconds > 0) ? ", " : " and ";
			String minuteWord = (minutes == 1) ? " minute" : " minutes";
			descriptive = descriptive + joiner + String.valueOf(minutes)
					+ minuteWord;
			// 4 hours, 2 minutes
			// 1 hours, 0 minutes
			// 2 hours, 0 minutes
			// 3 hours and 35 minutes
			// 1 hour and 8 minutes
		} else if (minutes > 0 && hours == 0) {
			String minuteWord = (minutes == 1) ? " minute" : " minutes";
			descriptive = String.valueOf(minutes) + minuteWord;
			// 45 minutes
		}

		if ((hours > 0 || minutes > 0) && seconds > 0) {
			String secondsWord = (seconds == 1) ? " second" : " seconds";

			descriptive = descriptive + " and " + String.valueOf(seconds)
					+ secondsWord;
			// 2 hours, 0 minutes and 5 seconds
			// 1 hour, 12 minutes and 9 seconds
		} else if (hours == 0 && minutes == 0 && seconds > 0) {
			String secondsWord = (seconds == 1) ? " second" : " second";
			descriptive = String.valueOf(seconds) + secondsWord;
		}

		return descriptive;

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			Toast
					.makeText(
							getBaseContext(),
							"GPSLogger is still running. You can exit the application from the menu options.",
							Toast.LENGTH_LONG).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);

		if (!Utilities.Flag()) {
			menu.getItem(1).setVisible(false);
		}

		return true;

	}

	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getTitle().equals("Settings")) {
			Intent settingsActivity = new Intent(getBaseContext(),
					GpsSettingsActivity.class);
			startActivity(settingsActivity);
			return false;

		} else if (item.getTitle().equals("Send")) {
			SendLocation();
			return false;
		} else if (item.getTitle().equals("Annotate")) {

			Annotate();
			return false;

		} else {
			RemoveNotification();
			StopGpsManager();
			// super.onStop();
			System.exit(0);
			// finish();

			return false;
		}

	}

	private void Annotate() {

		if (!allowDescription) {
			Utilities
					.MsgBox(
							"Not yet",
							"You can't add a description until the next point has been logged to a file.",
							this);
			return;
		}

		
		
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Add a description");
		alert.setMessage("Use only letters and numbers");

				
		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				if (!logToGpx && !logToKml) {
					return;
				}

				final String desc = Utilities.CleanDescription(input.getText().toString());
				
				AddNoteToLastPoint(desc);

			}
			
		});
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();

	}

	
	private void AddNoteToLastPoint(String desc)
	{
		File gpxFolder = new File(Environment
				.getExternalStorageDirectory(), "GPSLogger");

		if (!gpxFolder.exists()) {
			return;
		}

		int offsetFromEnd;
		String description;
		long startPosition;

		if (logToGpx) {

			File gpxFile = new File(gpxFolder.getPath(),
					currentFileName + ".gpx");

			if (!gpxFile.exists()) {
				return;
			}
			offsetFromEnd = 29;

			startPosition = gpxFile.length() - offsetFromEnd;

			description = "<name>" + desc + "</name><desc>" + desc
					+ "</desc></trkpt></trkseg></trk></gpx>";
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(gpxFile, "rw");
				gpxLock = raf.getChannel().lock();
				raf.seek(startPosition);
				raf.write(description.getBytes());
				gpxLock.release();
				raf.close();

				SetStatus("Description added to point.");
				allowDescription = false;

			} catch (Exception e) {
				SetStatus("Couldn't write description to GPX file.");
			}

		}

		if (logToKml) {

			File kmlFile = new File(gpxFolder.getPath(),
					currentFileName + ".kml");

			if (!kmlFile.exists()) {
				return;
			}

			offsetFromEnd = 37;

			description = "<name>" + desc
					+ "</name></Point></Placemark></Document></kml>";

			startPosition = kmlFile.length() - offsetFromEnd;
			try {
				RandomAccessFile raf = new RandomAccessFile(kmlFile,
						"rw");
				kmlLock = raf.getChannel().lock();
				raf.seek(startPosition);
				raf.write(description.getBytes());
				kmlLock.release();
				raf.close();

				allowDescription = false;
			} catch (Exception e) {
				SetStatus("Couldn't write description to KML file.");
			}

		}

		// </Point></Placemark></Document></kml>
	}
	
	final Handler handler = new Handler();
	final Runnable updateResults = new Runnable() {
		public void run() {
			AddedToMap();
		}

	};

	private void AddedToMap() {

		Utilities.MsgBox("Sent", "Location sent to server", this);

	}

	private String CleanString(String input) {

		input = input.replace(":", "");

		input = Utilities.EncodeHTML(input);

		input = URLEncoder.encode(input);
		return input;
	}

	private void SendToServer(String input) {

		input = CleanString(input);

		String whereUrl = "http://192.168.1.5:8101/SeeMyMapService.svc/savepoint/?guid="
				+ seeMyMapGuid
				+ "&lat="
				+ String.valueOf(currentLatitude)
				+ "&lon=" + String.valueOf(currentLongitude) + "&des=" + input;

		String response = Utilities.GetUrl(whereUrl);

	}

	private void SendLocation() {

		if (seeMyMapUrl == null || seeMyMapUrl.length() == 0
				|| seeMyMapGuid == null || seeMyMapGuid.length() == 0) {
			startActivity(new Intent("com.mendhak.gpslogger.SEEMYMAP_SETUP"));
		} else {

			if (currentLatitude != 0 && currentLongitude != 0) {

				final ProgressDialog pd = ProgressDialog.show(
						GpsMainActivity.this, "Sending...",
						"Sending to server", true, true);

				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle("Add a description");
				alert.setMessage("Use only letters and numbers");

				// Set an EditText view to get user input
				final EditText input = new EditText(this);
				alert.setView(input);

				alert.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								
								Thread t = new Thread() {
									public void run() {
										//Send to server
										SendToServer(input.getText().toString());
										//Also add to the file being logged
										AddNoteToLastPoint(input.getText().toString());
										pd.dismiss();
										handler.post(updateResults);
									}
								};
								t.start();

							}
						});
				alert.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Canceled.
							}
						});

				alert.show();

			}

			else {
				Utilities.MsgBox("Not yet", "Nothing to send yet", this);

			}
		}

	}

	public void StartGpsManager() {

		GetPreferences();

		gpsLocationListener = new GeneralLocationListener();
		towerLocationListener = new GeneralLocationListener();

		gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		CheckTowerAndGpsStatus();

		if (gpsEnabled && !preferCellTower) {
			// gps satellite based
			gpsLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, minimumSeconds * 1000,
					minimumDistance, gpsLocationListener);

			gpsLocationManager.addGpsStatusListener(gpsLocationListener);

			isUsingGps = true;
		} else if (towerEnabled) {
			isUsingGps = false;
			// Cell tower and wifi based
			towerLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, minimumSeconds * 1000,
					minimumDistance, towerLocationListener);

		} else {
			isUsingGps = false;
			SetStatus("No GPS provider available");
			return;
		}

		SetStatus("Started");
	}

	private void CheckTowerAndGpsStatus() {
		towerEnabled = towerLocationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		gpsEnabled = gpsLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

	}

	public void StopGpsManager() {

		if (towerLocationListener != null) {
			towerLocationManager.removeUpdates(towerLocationListener);
		}

		if (gpsLocationListener != null) {
			gpsLocationManager.removeUpdates(gpsLocationListener);
			gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
		}

		SetStatus("Stopped");
	}

	private void ResetCurrentFileName() {

		if (newFileOnceADay) {
			// 20100114.gpx
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			currentFileName = sdf.format(new Date());
		} else {
			// 20100114183329.gpx

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			currentFileName = sdf.format(new Date());
		}

	}

	private void ClearForm() {

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

	public void SetStatus(String message) {
		TextView tvStatus = (TextView) findViewById(R.id.textStatus);
		tvStatus.setText(message);
	}

	public void SetSatelliteInfo(int number) {
		satellites = number;
		TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
		txtSatellites.setText(String.valueOf(number));
	}

	public void DisplayLocationInfo(Location loc) {
		try {

			long currentTimeStamp = System.currentTimeMillis();
			if ((currentTimeStamp - latestTimeStamp) < (minimumSeconds * 1000)) {
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

			if (providerName.equalsIgnoreCase("gps")) {
				providerName = "GPS";
			} else {
				providerName = "cell towers";
			}

			tvDateTime.setText(new Date().toLocaleString() + "    (Using "
					+ providerName + ")");
			tvLatitude.setText(String.valueOf(loc.getLatitude()));
			tvLongitude.setText(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude()) {
				tvAltitude.setText(String.valueOf(loc.getAltitude())
						+ " meters");
			} else {
				tvAltitude.setText("n/a");
			}

			if (loc.hasSpeed()) {
				txtSpeed.setText(String.valueOf(loc.getSpeed()) + " m/s");
			} else {
				txtSpeed.setText("n/a");
			}

			if (loc.hasBearing()) {

				float bearingDegrees = loc.getBearing();
				String direction = "unknown";

				if (bearingDegrees > 348.75 || bearingDegrees <= 11.25) {
					direction = "Roughly North";
				} else if (bearingDegrees > 11.25 && bearingDegrees <= 33.75) {
					direction = "Roughly North-NorthEast";
				} else if (bearingDegrees > 33.75 && bearingDegrees <= 56.25) {
					direction = "Roughly NorthEast";
				} else if (bearingDegrees > 56.25 && bearingDegrees <= 78.75) {
					direction = "Roughly East-NorthEast";
				} else if (bearingDegrees > 78.75 && bearingDegrees <= 101.25) {
					direction = "Roughly East";
				} else if (bearingDegrees > 101.25 && bearingDegrees <= 123.75) {
					direction = "Roughly East-SouthEast";
				} else if (bearingDegrees > 123.75 && bearingDegrees <= 146.26) {
					direction = "Roughly SouthEast";
				} else if (bearingDegrees > 146.25 && bearingDegrees <= 168.75) {
					direction = "Roughly South-SouthEast";
				} else if (bearingDegrees > 168.75 && bearingDegrees <= 191.25) {
					direction = "Roughly South";
				} else if (bearingDegrees > 191.25 && bearingDegrees <= 213.75) {
					direction = "Roughly South-SouthWest";
				} else if (bearingDegrees > 213.75 && bearingDegrees <= 236.25) {
					direction = "Roughly SouthWest";
				} else if (bearingDegrees > 236.25 && bearingDegrees <= 258.75) {
					direction = "Roughly West-SouthWest";
				} else if (bearingDegrees > 258.75 && bearingDegrees <= 281.25) {
					direction = "Roughly West";
				} else if (bearingDegrees > 281.25 && bearingDegrees <= 303.75) {
					direction = "Roughly West-NorthWest";
				} else if (bearingDegrees > 303.75 && bearingDegrees <= 326.25) {
					direction = "Roughly NorthWest";
				} else if (bearingDegrees > 326.25 && bearingDegrees <= 348.75) {
					direction = "Roughly North-NorthWest";
				} else {
					direction = "Unknown";
				}

				txtDirection.setText(direction + "("
						+ String.valueOf(Math.round(bearingDegrees)) + "°)");
			} else {
				txtDirection.setText("n/a");
			}

			if (!isUsingGps) {
				txtSatellites.setText("n/a");
				satellites = 0;
			}

			if (loc.hasAccuracy()) {
				txtAccuracy.setText("within "
						+ String.valueOf(loc.getAccuracy()) + " meters");
			} else {
				txtAccuracy.setText("n/a");
			}

			WriteToFile(loc);
			GetPreferences();
			ResetManagersIfRequired();

		} catch (Exception ex) {
			SetStatus("Error in displaying location info: " + ex.getMessage());
		}

	}

	private void WriteToFile(Location loc) {

		if (!logToGpx && !logToKml) {
			return;
		}

		try {

			boolean brandNewFile = false;
			// if (root.canWrite()){
			// File gpxFolder = new File("/sdcard/GPSLogger");
			File gpxFolder = new File(
					Environment.getExternalStorageDirectory(), "GPSLogger");

			Log.i("MAIN", String.valueOf(gpxFolder.canWrite()));

			if (!gpxFolder.exists()) {
				gpxFolder.mkdirs();
				brandNewFile = true;
			}

			if (logToGpx) {
				WriteToGpxFile(loc, gpxFolder, brandNewFile);
			}

			if (logToKml) {
				WriteToKmlFile(loc, gpxFolder, brandNewFile);

			}

			allowDescription = true;

		} catch (Exception e) {
			Log.e("Main", "Could not write file " + e.getMessage());
			SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	private void WriteToKmlFile(Location loc, File gpxFolder,
			boolean brandNewFile) {
		// TODO Auto-generated method stub
		try {
			File kmlFile = new File(gpxFolder.getPath(), currentFileName
					+ ".kml");

			if (!kmlFile.exists()) {
				kmlFile.createNewFile();
				brandNewFile = true;
			}

			Date now = new Date();
			// SimpleDateFormat sdf = new
			// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			// String dateTimeString = sdf.format(now);

			if (brandNewFile) {
				FileOutputStream initialWriter = new FileOutputStream(kmlFile,
						true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(
						initialWriter);

				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>"
						+ "</Document></kml>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			long startPosition = kmlFile.length() - 17;

			String placemark = "<Placemark><description>"
					+ now.toLocaleString() + "</description>"
					+ "<Point><coordinates>"
					+ String.valueOf(loc.getLongitude()) + ","
					+ String.valueOf(loc.getLatitude()) + ","
					+ String.valueOf(loc.getAltitude())
					+ "</coordinates></Point></Placemark></Document></kml>";

			RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
			kmlLock = raf.getChannel().lock();
			raf.seek(startPosition);
			raf.write(placemark.getBytes());
			kmlLock.release();
			raf.close();

		} catch (IOException e) {
			Log.e("Main", "Could not write file " + e.getMessage());
			SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	private void WriteToGpxFile(Location loc, File gpxFolder,
			boolean brandNewFile) {

		try {
			File gpxFile = new File(gpxFolder.getPath(), currentFileName
					+ ".gpx");

			if (!gpxFile.exists()) {
				gpxFile.createNewFile();
				brandNewFile = true;
			}

			Date now = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String dateTimeString = sdf.format(now);

			if (brandNewFile) {
				FileOutputStream initialWriter = new FileOutputStream(gpxFile,
						true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(
						initialWriter);

				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">"
						+ "<time>" + dateTimeString + "</time>" + "<bounds />"
						+ "<trk></trk></gpx>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			int offsetFromEnd = (addNewTrackSegment) ? 12 : 21;

			long startPosition = gpxFile.length() - offsetFromEnd;

			String trackPoint = GetTrackPointXml(loc, dateTimeString);

			addNewTrackSegment = false;

			// Leaving this commented code in - may want to give user the choice
			// to
			// pick between WPT and TRK. Choice is good.
			//
			// String waypoint = "<wpt lat=\"" +
			// String.valueOf(loc.getLatitude())
			// + "\" lon=\"" + String.valueOf(loc.getLongitude()) + "\">"
			// + "<time>" + dateTimeString + "</time>";
			//
			// if (loc.hasAltitude()) {
			// waypoint = waypoint + "<ele>"
			// + String.valueOf(loc.getAltitude()) + "</ele>";
			// }
			//
			// if (loc.hasBearing()) {
			// waypoint = waypoint + "<course>"
			// + String.valueOf(loc.getBearing()) + "</course>";
			// }
			//
			// if (loc.hasSpeed()) {
			// waypoint = waypoint + "<speed>"
			// + String.valueOf(loc.getSpeed()) + "</speed>";
			// }
			//
			// waypoint = waypoint + "<src>" + loc.getProvider() + "</src>";
			//
			// if (satellites > 0) {
			// waypoint = waypoint + "<sat>" + String.valueOf(satellites)
			// + "</sat>";
			// }
			//
			// waypoint = waypoint + "</wpt></gpx>";

			RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
			gpxLock = raf.getChannel().lock();
			raf.seek(startPosition);
			raf.write(trackPoint.getBytes());
			gpxLock.release();
			raf.close();

		} catch (IOException e) {
			Log.e("Main", "Could not write file " + e.getMessage());
			SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	private String GetTrackPointXml(Location loc, String dateTimeString) {
		String track = "";
		if (addNewTrackSegment) {
			track = track + "<trkseg>";
		}

		track = track + "<trkpt lat=\"" + String.valueOf(loc.getLatitude())
				+ "\" lon=\"" + String.valueOf(loc.getLongitude()) + "\">";

		if (loc.hasAltitude()) {
			track = track + "<ele>" + String.valueOf(loc.getAltitude())
					+ "</ele>";
		}

		if (loc.hasBearing()) {
			track = track + "<course>" + String.valueOf(loc.getBearing())
					+ "</course>";
		}

		if (loc.hasSpeed()) {
			track = track + "<speed>" + String.valueOf(loc.getSpeed())
					+ "</speed>";
		}

		track = track + "<src>" + loc.getProvider() + "</src>";

		if (satellites > 0) {
			track = track + "<sat>" + String.valueOf(satellites) + "</sat>";
		}

		track = track + "<time>" + dateTimeString + "</time>";

		track = track + "</trkpt>";

		track = track + "</trkseg></trk></gpx>";

		return track;
	}

	public void RestartGpsManagers() {

		StopGpsManager();
		StartGpsManager();

	}

	public void ResetManagersIfRequired() {
		CheckTowerAndGpsStatus();

		if (isUsingGps && preferCellTower) {
			RestartGpsManagers();
		}
		// If GPS is enabled and user doesn't prefer celltowers
		else if (gpsEnabled && !preferCellTower) {
			// But we're not *already* using GPS
			if (!isUsingGps) {
				RestartGpsManagers();
			}
			// Else do nothing
		}

	}

	public class GeneralLocationListener implements LocationListener,
			GpsStatus.Listener {

		public void onLocationChanged(Location loc) {

			try {
				if (loc != null) {

					Latitude = loc.getLatitude();
					Longitude = loc.getLongitude();

					currentLatitude = loc.getLatitude();
					currentLongitude = loc.getLongitude();

					DisplayLocationInfo(loc);

				}

			} catch (Exception ex) {
				SetStatus(ex.getMessage());
			}

		}

		public boolean GotLocation;
		public double Latitude;
		public double Longitude;

		public void onProviderDisabled(String provider) {

			RestartGpsManagers();

			// Toast.makeText(getBaseContext(),
			// provider + "Disabled",
			// Toast.LENGTH_SHORT).show();
		}

		public void onProviderEnabled(String provider) {
			// Toast.makeText(getBaseContext(),
			// provider + "Enabled",
			// Toast.LENGTH_SHORT).show();

			RestartGpsManagers();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

			// Toast.makeText(getBaseContext(),
			// provider + " status " + String.valueOf(status),
			// Toast.LENGTH_SHORT);
		}

		public void onGpsStatusChanged(int event) {

			switch (event) {
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				SetStatus("Fix obtained");
				break;

			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

				GpsStatus status = gpsLocationManager.getGpsStatus(null);

				Iterator<GpsSatellite> it = status.getSatellites().iterator();
				int count = 0;
				while (it.hasNext()) {
					count++;
					GpsSatellite oSat = (GpsSatellite) it.next();
					Log.i("Main",
							"LocationActivity - onGpsStatusChange: Satellites:"
									+ oSat.getSnr());
				}

				SetSatelliteInfo(count);
				break;

			case GpsStatus.GPS_EVENT_STARTED:
				SetStatus("GPS Started, waiting for fix");
				break;

			case GpsStatus.GPS_EVENT_STOPPED:
				SetStatus("GPS Stopped");
				break;

			}

		}

	}

}
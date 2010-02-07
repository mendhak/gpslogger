package com.mendhak.gpslogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class GpsMainActivity extends Activity implements OnClickListener {
	GeneralLocationListener gpsLocationListener;
	GeneralLocationListener towerLocationListener;
	LocationManager gpsLocationManager;
	LocationManager towerLocationManager;

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

	double currentLatitude;
	double currentLongitude;
	long latestTimeStamp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		Button buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(this);

		Button buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonStop.setOnClickListener(this);

		GetPreferences();

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
		// gpsNotifyManager.cancel(NOTIFICATION_ID);
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

		Notification nfc = new Notification(R.drawable.gpsstatus, null, System
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
		minimumDistance = Integer.valueOf(prefs.getString(
				"distance_before_logging", "10"));
		minimumSeconds = Integer.valueOf(prefs.getString("time_before_logging",
				"60"));
		newFileCreation = prefs.getString("new_file_creation", "onceaday");
		if (newFileCreation.equals("onceaday")) {
			newFileOnceADay = true;
		} else {
			newFileOnceADay = false;
		}
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
		return true;

	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getTitle().equals("Settings")) {
			Intent settingsActivity = new Intent(getBaseContext(),
					GpsSettingsActivity.class);
			startActivity(settingsActivity);
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

	public void StartGpsManager() {

		GetPreferences();

		gpsLocationListener = new GeneralLocationListener();
		towerLocationListener = new GeneralLocationListener();

		gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		CheckTowerAndGpsStatus();

		if (gpsEnabled) {
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

	public void onClick(View v) {
		// 

		String buttonId = (String) v.getTag();

		try {

			if (buttonId.equalsIgnoreCase("Start")) {
				if (isStarted) {
					return;
				}

				isStarted = true;
				GetPreferences();
				Notify();
				ResetCurrentFileName();
				ClearForm();
				StartGpsManager();

			} else if (buttonId.equalsIgnoreCase("Stop")) {

				isStarted = false;
				RemoveNotification();
				StopGpsManager();
			}

		} catch (Exception ex) {
			SetStatus("Button click error: " + ex.getMessage());
		}
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
				tvAltitude.setText(String.valueOf(loc.getAltitude()));
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
						+ String.valueOf(loc.getAccuracy()) + "m");
			} else {
				txtAccuracy.setText("n/a");
			}

			WriteToFile(loc);
			GetPreferences();
			ResetManagersIfRequired();

		} catch (Exception ex) {
			SetStatus("Error in displaylocationinfo: " + ex.getMessage());
		}

	}

	private void WriteToFile(Location loc) {

		if (!logToGpx && !logToKml) {
			return;
		}

		try {

			boolean brandNewFile = false;
			// if (root.canWrite()){
			File gpxFolder = new File("/sdcard/GPSLogger");

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
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String dateTimeString = sdf.format(now);

			if (brandNewFile) {
				FileOutputStream initialWriter = new FileOutputStream(kmlFile,
						true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(
						initialWriter);

				/*<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Placemark>
    <name>Simple placemark</name>
    <description>Attached to the ground. Intelligently places itself 
       at the height of the underlying terrain.</description>
    <Point>
      <coordinates>-122.0822035425683,37.42228990140251,0</coordinates>
    </Point>
  </Placemark>
</kml>*/
				
				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\">"
						+ "</kml>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			long startPosition = kmlFile.length() - 6;
		

			String placemark = "<Placemark><name>" + now.toLocaleString() 
			        + "</name><description>" + now.toLocaleString() + "</description>" 
			        + "<Point><coordinates>" + String.valueOf(loc.getLongitude()) + "," 
			        + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getAltitude())
			        + "</coordinates></Point></Placemark></kml>";

			RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
			raf.seek(startPosition);
			raf.write(placemark.getBytes());
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
						+ "</gpx>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			long startPosition = gpxFile.length() - 6;

			String waypoint = "<wpt lat=\"" + String.valueOf(loc.getLatitude())
					+ "\" lon=\"" + String.valueOf(loc.getLongitude()) + "\">"
					+ "<time>" + dateTimeString + "</time>";

			if (loc.hasAltitude()) {
				waypoint = waypoint + "<ele>"
						+ String.valueOf(loc.getAltitude()) + "</ele>";
			}

			if (loc.hasBearing()) {
				waypoint = waypoint + "<course>"
						+ String.valueOf(loc.getBearing()) + "</course>";
			}

			if (loc.hasSpeed()) {
				waypoint = waypoint + "<speed>"
						+ String.valueOf(loc.getSpeed()) + "</speed>";
			}

			waypoint = waypoint + "<src>" + loc.getProvider() + "</src>";

			if (satellites > 0) {
				waypoint = waypoint + "<sat>" + String.valueOf(satellites)
						+ "</sat>";
			}

			waypoint = waypoint + "</wpt></gpx>";

			RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
			raf.seek(startPosition);
			raf.write(waypoint.getBytes());
			raf.close();

		} catch (IOException e) {
			Log.e("Main", "Could not write file " + e.getMessage());
			SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	public void RestartGpsManagers() {

		StopGpsManager();
		StartGpsManager();
	}

	public void ResetManagersIfRequired() {
		CheckTowerAndGpsStatus();

		// If GPS is enabled
		if (gpsEnabled) {
			// But we're not currently using GPS
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
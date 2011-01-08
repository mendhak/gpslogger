package com.mendhak.gpslogger;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.mendhak.gpslogger.helpers.*;
import com.mendhak.gpslogger.interfaces.IFileLoggingHelperCallback;
import com.mendhak.gpslogger.interfaces.IGpsLoggerServiceClient;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.model.Session;
import com.mendhak.gpslogger.R;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager; //import android.os.AsyncTask;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
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
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GpsMainActivity extends Activity 
implements OnCheckedChangeListener, IGpsLoggerServiceClient, IFileLoggingHelperCallback
{

	/**
	 * General all purpose handler used for updating the UI from threads.
	 */
	public final Handler handler = new Handler();
	static final int DATEPICKER_ID = 0;

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

	private GpsLoggingService loggingService;
	
	/**
	 * Provides a connection to the GPS Logging Service
	 */
	private ServiceConnection gpsServiceConnection =  new ServiceConnection()
	{
		
		public void onServiceDisconnected(ComponentName name)
		{
			loggingService = null;
		}
		
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			loggingService = ((GpsLoggingService.GpsLoggingBinder)service).getService();
			GpsLoggingService.SetServiceClient(GpsMainActivity.this);
			
			//Form setup - toggle button, display existing location info
			ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
			
			if(loggingService.IsRunning())
			{
				buttonOnOff.setChecked(true);
				DisplayLocationInfo(Session.getCurrentLocationInfo());
			}
			
			buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);
		}
	};
	
	/**
	 * Event raised when the form is created for the first time
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    String lang = prefs.getString("locale_override", "");

	    if(!lang.equalsIgnoreCase(""))
	    {
	    	Locale locale = new Locale(lang);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration
				(config, getBaseContext().getResources().getDisplayMetrics());
	    }
			    
		super.onCreate(savedInstanceState);

		Utilities.LogInfo("GPSLogger started");

		seeMyMapHelper = new SeeMyMapHelper(this);
		fileHelper = new FileLoggingHelper(this);

		setContentView(R.layout.main);

		GetPreferences();
		
		Intent serviceIntent = new Intent(this, GpsLoggingService.class);
		//Start the service in case it isn't already running
		startService(serviceIntent);
		//Now bind to service
	    bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
	    Session.setBoundToService(true);
	}


	/**
	 * Called when the toggle button is clicked
	 */
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		GetPreferences();
		
		if(isChecked)
		{
			loggingService.StartLogging();
		}
		else
		{
			loggingService.StopLogging();
		}
	}


	/**
	 * Gets preferences chosen by the user
	 */
	public void GetPreferences()
	{
		Utilities.PopulateAppSettings(getBaseContext());
		ShowPreferencesSummary();

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

		if (!AppSettings.shouldLogToKml() && !AppSettings.shouldLogToGpx())
		{
			txtLoggingTo.setText(R.string.summary_loggingto_screen);

		}
		else if (AppSettings.shouldLogToGpx() && AppSettings.shouldLogToKml())
		{
			txtLoggingTo.setText(R.string.summary_loggingto_both);
		}
		else
		{
			txtLoggingTo.setText((AppSettings.shouldLogToGpx() ? "GPX" : "KML"));

		}

		if (AppSettings.getMinimumSeconds() > 0)
		{
			String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(), getBaseContext());

			txtFrequency.setText(descriptiveTime);
		}
		else
		{
			txtFrequency.setText(R.string.summary_freq_max);

		}

		if (AppSettings.getMinimumDistance() > 0)
		{

			if (AppSettings.shouldUseImperial())
			{
				int minimumDistanceInFeet = Utilities.MetersToFeet(AppSettings.getMinimumDistance());
				txtDistance.setText(((minimumDistanceInFeet == 1)
					? getString(R.string.foot)
					: String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
			}
			else
			{
				txtDistance.setText(((AppSettings.getMinimumDistance() == 1)
					? getString(R.string.meter)
					: String.valueOf(AppSettings.getMinimumDistance()) + getString(R.string.meters)));
			}

		}
		else
		{
			txtDistance.setText(R.string.summary_dist_regardless);
		}

		if ((AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml()) 
				&& (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0))
		{
			txtFilename.setText(getString(R.string.summary_current_filename_format, Session.getCurrentFileName()));
		}
	}

	/**
	 * Handles the hardware back-button press
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));
		
		if(keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService())
		{
			unbindService(gpsServiceConnection);
		}

//		if (keyCode == KeyEvent.KEYCODE_BACK)
//		{
//			moveTaskToBack(true);
//			Toast.makeText(getBaseContext(), getString(R.string.toast_gpslogger_stillrunning),
//					Toast.LENGTH_LONG).show();
//			return true;
//		}
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
				loggingService.StopLogging();
				loggingService.stopSelf();
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
						if (Session.hasValidLocation())
						{
							String bodyText = getString(R.string.sharing_latlong_text,
									String.valueOf(Session.getCurrentLatitude()), String.valueOf(Session.getCurrentLongitude()));
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
	 * Clears the table, removes all values.
	 */
	public void ClearForm()
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
	 * @param stringId
	 */
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
		Session.setSatelliteCount(number);
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

			if(loc==null)
			{
				return;
			}
			

			Session.setLatestTimeStamp(System.currentTimeMillis());

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

				if (AppSettings.shouldUseImperial())
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
				if (AppSettings.shouldUseImperial())
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

			if(!Session.isUsingGps())
			{
				txtSatellites.setText(R.string.not_applicable);
				Session.setSatelliteCount(0);
			}

			if (loc.hasAccuracy())
			{

				float accuracy = loc.getAccuracy();

				if (AppSettings.shouldUseImperial())
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



		}
		catch (Exception ex)
		{
			SetStatus(getString(R.string.error_displaying, ex.getMessage()));
		}

	}

	

	public void OnBeginGpsLogging()
	{

		
	}
	
	public void OnStopGpsLogging()
	{

		
	}

	public void OnLocationUpdate(Location loc)
	{
		DisplayLocationInfo(loc);
		
	}

	public void OnSatelliteCount(int count)
	{
		SetSatelliteInfo(count);
		
	}

	public void OnStatusMessage(String message)
	{
		SetStatus(message);
	}

	public Activity GetActivity()
	{
		return this;
	}


	public Context GetContext()
	{
		return getBaseContext();
	}






}

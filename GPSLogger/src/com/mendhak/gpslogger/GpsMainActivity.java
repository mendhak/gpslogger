package com.mendhak.gpslogger;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class GpsMainActivity extends Activity implements OnCheckedChangeListener,
        IGpsLoggerServiceClient, View.OnClickListener, IActionListener
{

    /**
     * General all purpose handler used for updating the UI from threads.
     */
    public final Handler handler = new Handler();
    private static Intent serviceIntent;
    private GpsLoggingService loggingService;

    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection()
    {

        public void onServiceDisconnected(ComponentName name)
        {
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
            GpsLoggingService.SetServiceClient(GpsMainActivity.this);


            Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);

            buttonSinglePoint.setOnClickListener(GpsMainActivity.this);

            if (Session.isStarted())
            {
                if (Session.isSinglePointMode())
                {
                    SetMainButtonEnabled(false);
                }
                else
                {
                    SetMainButtonChecked(true);
                    SetSinglePointButtonEnabled(false);
                }

                DisplayLocationInfo(Session.getCurrentLocationInfo());
            }

            // Form setup - toggle button, display existing location info
            ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
            buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);
        }
    };


    /**
     * Event raised when the form is created for the first time
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        Utilities.LogDebug("GpsMainActivity.onCreate");

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

        super.onCreate(savedInstanceState);

        Utilities.LogInfo("GPSLogger started");

        setContentView(R.layout.main);

        GetPreferences();

        StartAndBindService();
    }

    @Override
    protected void onStart()
    {
        Utilities.LogDebug("GpsMainActivity.onStart");
        super.onStart();
        StartAndBindService();
    }

    @Override
    protected void onResume()
    {
        Utilities.LogDebug("GpsMainactivity.onResume");
        super.onResume();
        StartAndBindService();
    }

    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService()
    {
        Utilities.LogDebug("StartAndBindService - binding now");
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void StopAndUnbindServiceIfRequired()
    {
        Utilities.LogDebug("GpsMainActivity.StopAndUnbindServiceIfRequired");
        if (Session.isBoundToService())
        {
            unbindService(gpsServiceConnection);
            Session.setBoundToService(false);
        }

        if (!Session.isStarted())
        {
            Utilities.LogDebug("StopServiceIfRequired - Stopping the service");
            //serviceIntent = new Intent(this, GpsLoggingService.class);
            stopService(serviceIntent);
        }

    }

    @Override
    protected void onPause()
    {

        Utilities.LogDebug("GpsMainActivity.onPause");
        StopAndUnbindServiceIfRequired();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {

        Utilities.LogDebug("GpsMainActivity.onDestroy");
        StopAndUnbindServiceIfRequired();
        super.onDestroy();

    }

    /**
     * Called when the toggle button is clicked
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Utilities.LogDebug("GpsMainActivity.onCheckedChanged");

        if (isChecked)
        {
            GetPreferences();
            SetSinglePointButtonEnabled(false);
            loggingService.StartLogging();
        }
        else
        {
            SetSinglePointButtonEnabled(true);
            loggingService.StopLogging();
        }
    }

    /**
     * Called when the single point button is clicked
     */
    public void onClick(View view)
    {
        Utilities.LogDebug("GpsMainActivity.onClick");

        if (!Session.isStarted())
        {
            SetMainButtonEnabled(false);
            loggingService.StartLogging();
            Session.setSinglePointMode(true);
        }
        else if (Session.isStarted() && Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }
    }


    public void SetSinglePointButtonEnabled(boolean enabled)
    {
        Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);
        buttonSinglePoint.setEnabled(enabled);
    }

    public void SetMainButtonEnabled(boolean enabled)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setEnabled(enabled);
    }

    public void SetMainButtonChecked(boolean checked)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setChecked(checked);
    }

    /**
     * Gets preferences chosen by the user
     */
    private void GetPreferences()
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
        Utilities.LogDebug("GpsMainActivity.ShowPreferencesSummary");
        try
        {
            TextView txtLoggingTo = (TextView) findViewById(R.id.txtLoggingTo);
            TextView txtFrequency = (TextView) findViewById(R.id.txtFrequency);
            TextView txtAutoEmail = (TextView) findViewById(R.id.txtAutoEmail);

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
                String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(),
                        getBaseContext());

                txtFrequency.setText(descriptiveTime);
            }
            else
            {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            if (AppSettings.isAutoEmailEnabled())
            {
                String autoEmailResx;

                if (AppSettings.getAutoEmailDelay() == 0)
                {
                    autoEmailResx = "autoemail_frequency_whenistop";
                }
                else
                {

                    autoEmailResx = "autoemail_frequency_"
                            + String.valueOf(AppSettings.getAutoEmailDelay()).replace(".", "");
                }

                String autoEmailDesc = getString(getResources().getIdentifier(autoEmailResx, "string", getPackageName()));

                txtAutoEmail.setText(autoEmailDesc);
            }
            else
            {
                TableRow trAutoEmail = (TableRow) findViewById(R.id.trAutoEmail);
                trAutoEmail.setVisibility(View.INVISIBLE);
            }
        }
        catch (Exception ex)
        {
            Utilities.LogError("ShowPreferencesSummary", ex);
        }


    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService())
        {
            StopAndUnbindServiceIfRequired();
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
            case R.id.mnuSettings:
                Intent settingsActivity = new Intent(getBaseContext(), GpsSettingsActivity.class);
                startActivity(settingsActivity);
                break;
            case R.id.mnuOSM:
                UploadToOpenStreetMap();
                break;
            case R.id.mnuDropBox:
                UploadToDropBox();
                break;
            case R.id.mnuAnnotate:
                Annotate();
                break;
            case R.id.mnuShare:
                Share();
                break;
            case R.id.mnuEmailnow:
                EmailNow();
                break;
            case R.id.mnuExit:
                loggingService.StopLogging();
                loggingService.stopSelf();
                finish();
                break;
        }
        return false;
    }


    private void EmailNow()
    {
        Utilities.LogDebug("GpsMainActivity.EmailNow");

        if (Utilities.IsEmailSetup(getBaseContext()))
        {
            loggingService.ForceEmailLogFile();
        }
        else
        {
            Intent emailSetup = new Intent(getBaseContext(), AutoEmailActivity.class);
            startActivity(emailSetup);
        }

    }

    /**
     * Allows user to send a GPX/KML file along with location, or location only
     * using a provider. 'Provider' means any application that can accept such
     * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
     */
    private void Share()
    {
        Utilities.LogDebug("GpsMainActivity.Share");
        try
        {

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
                        intent.setType("*/*");

                        if (chosenFileName.equalsIgnoreCase(locationOnly))
                        {
                            intent.setType("text/plain");
                        }

                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_mylocation));
                        if (Session.hasValidLocation())
                        {
                            String bodyText = getString(R.string.sharing_latlong_text,
                                    String.valueOf(Session.getCurrentLatitude()),
                                    String.valueOf(Session.getCurrentLongitude()));
                            intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                            intent.putExtra("sms_body", bodyText);
                        }

                        if (chosenFileName.length() > 0
                                && !chosenFileName.equalsIgnoreCase(locationOnly))
                        {
                            intent.putExtra(Intent.EXTRA_STREAM,
                                    Uri.fromFile(new File(gpxFolder, chosenFileName)));
                        }

                        startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));

                    }
                });
                dialog.show();
            }
            else
            {
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
            }
        }
        catch (Exception ex)
        {
            Utilities.LogError("Share", ex);
        }

    }


    private void UploadToDropBox()
    {
        Utilities.LogDebug("GpsMainActivity.UploadToDropBox");

        final DropBoxHelper dropBoxHelper = new DropBoxHelper(getBaseContext(), this);


        if (!dropBoxHelper.IsLinked())
        {
            startActivity(new Intent("com.mendhak.gpslogger.DROPBOX_SETUP"));
            return;
        }

        final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

        if (gpxFolder.exists())
        {

            String[] enumeratedFiles = gpxFolder.list();
            List<String> fileList = new ArrayList<String>(Arrays.asList(enumeratedFiles));
            Collections.reverse(fileList);
            final String[] files = fileList.toArray(new String[0]);

            final Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.dropbox_upload);
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
                    Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.dropbox_uploading), getString(R.string.please_wait));
                    dropBoxHelper.UploadFile(chosenFileName);
                }
            });
            dialog.show();
        }
        else
        {
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }


    /**
     * Uploads a GPS Trace to OpenStreetMap.org.
     */
    private void UploadToOpenStreetMap()
    {
        Utilities.LogDebug("GpsMainactivity.UploadToOpenStreetMap");

        if (!Utilities.IsOsmAuthorized(getBaseContext()))
        {
            startActivity(Utilities.GetOsmSettingsIntent(getBaseContext()));
            return;
        }

        final String goToOsmSettings = getString(R.string.menu_settings);

        final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

        if (gpxFolder.exists())
        {
            FilenameFilter select = new FilenameFilter()
            {

                public boolean accept(File dir, String filename)
                {
                    return filename.toLowerCase().contains(".gpx");
                }
            };

            String[] enumeratedFiles = gpxFolder.list(select);
            List<String> fileList = new ArrayList<String>(Arrays.asList(enumeratedFiles));
            Collections.reverse(fileList);
            fileList.add(0, goToOsmSettings);
            final String[] files = fileList.toArray(new String[0]);

            final Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.osm_pick_file);
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

                    if (chosenFileName.equalsIgnoreCase(goToOsmSettings))
                    {
                        startActivity(Utilities.GetOsmSettingsIntent(getBaseContext()));
                    }
                    else
                    {
                        OSMHelper osm = new OSMHelper(GpsMainActivity.this);
                        Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.osm_uploading), getString(R.string.please_wait));
                        osm.UploadGpsTrace(chosenFileName);
                    }


                }
            });
            dialog.show();
        }
        else
        {
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }

    public final Runnable updateOsmUpload = new Runnable()
    {
        public void run()
        {
            OnOSMUploadComplete();
        }
    };

    public final Runnable updateOsmFailed = new Runnable()
    {
        public void run()
        {
            OnOSMUploadComplete();
        }
    };

    private void OnOSMUploadComplete()
    {
        Utilities.HideProgress();
    }


    /**
     * Prompts user for input, then adds text to log file
     */
    private void Annotate()
    {
        Utilities.LogDebug("GpsMainActivity.Annotate");

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml())
        {
            return;
        }

        if (!Session.shoulAllowDescription())
        {
            Utilities.MsgBox(getString(R.string.not_yet),
                    getString(R.string.cant_add_description_until_next_point),
                    GetActivity());

            return;

        }

        AlertDialog.Builder alert = new AlertDialog.Builder(GpsMainActivity.this);

        alert.setTitle(R.string.add_description);
        alert.setMessage(R.string.letters_numbers);

        // Set an EditText view to get user input
        final EditText input = new EditText(getBaseContext());
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                final String desc = Utilities.CleanDescription(input.getText().toString());
                Annotate(desc);
            }

        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Cancelled.
            }
        });

        alert.show();
    }

    private void Annotate(String description)
    {
        Utilities.LogDebug("GpsMainActivity.Annotate(description)");

        List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers();

        for (IFileLogger logger : loggers)
        {
            try
            {
                logger.Annotate(description);
                SetStatus(getString(R.string.description_added));
                Session.setAllowDescription(false);
            }
            catch (Exception e)
            {
                SetStatus(getString(R.string.could_not_write_to_file));
            }
        }
    }

    /**
     * Clears the table, removes all values.
     */
    public void ClearForm()
    {

        Utilities.LogDebug("GpsMainActivity.ClearForm");

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

    public void OnStopLogging()
    {
        Utilities.LogDebug("GpsMainActivity.OnStopLogging");
        SetMainButtonChecked(false);
    }

    /**
     * Sets the message in the top status label.
     *
     * @param message The status message
     */
    private void SetStatus(String message)
    {
        Utilities.LogDebug("GpsMainActivity.SetStatus: " + message);
        TextView tvStatus = (TextView) findViewById(R.id.textStatus);
        tvStatus.setText(message);
        Utilities.LogInfo(message);
    }

    /**
     * Sets the number of satellites in the satellite row in the table.
     *
     * @param number The number of satellites
     */
    private void SetSatelliteInfo(int number)
    {
        Session.setSatelliteCount(number);
        TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
        txtSatellites.setText(String.valueOf(number));
    }

    /**
     * Given a location fix, processes it and displays it in the table on the
     * form.
     *
     * @param loc Location information
     */
    private void DisplayLocationInfo(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.DisplayLocationInfo");
        try
        {

            if (loc == null)
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
                String unit;
                if (AppSettings.shouldUseImperial())
                {
                    if (speed > 1.47)
                    {
                        speed = speed * 0.6818f;
                        unit = getString(R.string.miles_per_hour);

                    }
                    else
                    {
                        speed = Utilities.MetersToFeet(speed);
                        unit = getString(R.string.feet_per_second);
                    }
                }
                else
                {
                    if (speed > 0.277)
                    {
                        speed = speed * 3.6f;
                        unit = getString(R.string.kilometers_per_hour);
                    }
                    else
                    {
                        unit = getString(R.string.meters_per_second);
                    }
                }

                txtSpeed.setText(String.valueOf(speed) + unit);

            }
            else
            {
                txtSpeed.setText(R.string.not_applicable);
            }

            if (loc.hasBearing())
            {

                float bearingDegrees = loc.getBearing();
                String direction;

                direction = Utilities.GetBearingDescription(bearingDegrees, getBaseContext());

                txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                        + getString(R.string.degree_symbol) + ")");
            }
            else
            {
                txtDirection.setText(R.string.not_applicable);
            }

            if (!Session.isUsingGps())
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

    public void OnLocationUpdate(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.OnLocationUpdate");
        DisplayLocationInfo(loc);
        ShowPreferencesSummary();

        if (Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }

    }

    public void OnSatelliteCount(int count)
    {
        SetSatelliteInfo(count);

    }

    public void onFileName(String newFileName)
    {
        TextView txtFilename = (TextView) findViewById(R.id.txtFileName);

        if (AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml())
        {


            txtFilename.setText(getString(R.string.summary_current_filename_format,
                    Session.getCurrentFileName()));
        }
        else
        {
            txtFilename.setText("");
        }


    }

    public void OnStatusMessage(String message)
    {
        SetStatus(message);
    }

    public void OnFatalMessage(String message)
    {
        Utilities.MsgBox(getString(R.string.sorry), message, this);
    }

    public Activity GetActivity()
    {
        return this;
    }


    @Override
    public void OnComplete()
    {
        Utilities.HideProgress();
    }

    @Override
    public void OnFailure()
    {
        Utilities.HideProgress();
    }
}

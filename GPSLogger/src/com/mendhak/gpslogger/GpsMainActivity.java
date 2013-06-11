/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

//TODO: Move GPSMain email now call to gpsmain to allow closing of progress bar

package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.mendhak.gpslogger.fragments.*;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.dropbox.DropBoxAuthorizationActivity;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.ftp.AutoFtpActivity;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsSettingsActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import com.mendhak.gpslogger.senders.opengts.OpenGTSActivity;
import net.kataplop.gpslogger.R;

import java.io.File;
import java.util.*;

public class GpsMainActivity extends SherlockFragmentActivity implements OnCheckedChangeListener,
        IGpsLoggerServiceClient, View.OnClickListener, IActionListener, IWidgetContainer
{

    /**
     * General all purpose handler used for updating the UI from threads.
     */
    private static Intent serviceIntent;
    private GpsLoggingService loggingService;

    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            loggingService = null;
        }

        @Override
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

        super.onCreate(savedInstanceState);

        Utilities.LogInfo("GPSLogger started");

        setContentView(R.layout.main_fragment);

        // Moved to onResume to update the list of loggers
        //GetPreferences();

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
        super.onResume();

        Utilities.LogDebug("GpsMainactivity.onResume");
        GetPreferences();

        if (AppSettings.getForceScreenOn()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        FragmentManager fm = getSupportFragmentManager();

        Fragment main_frag = fm.findFragmentById(R.id.main_frag_container);
        Fragment no_frag = fm.findFragmentById(R.id.frag_NO);
        Fragment ne_frag = fm.findFragmentById(R.id.frag_NE);
        Fragment so_frag = fm.findFragmentById(R.id.frag_SO);
        Fragment se_frag = fm.findFragmentById(R.id.frag_SE);

        View modular_view = findViewById(R.id.main_modular_container);
        View normal_view = findViewById(R.id.main_frag_container);

        View not_modular_view[] = {findViewById(R.id.trAutoEmail),
                findViewById(R.id.fileName),
                findViewById(R.id.distance),
                findViewById(R.id.frequency),
                findViewById(R.id.buttonSinglePoint),
        };
//
//        TextView frag_NO_title = (TextView) findViewById(R.id.frag_NO_title);
//        TextView frag_NE_title = (TextView) findViewById(R.id.frag_NE_title);
//        TextView frag_SO_title = (TextView) findViewById(R.id.frag_SO_title);
//        TextView frag_SE_title = (TextView) findViewById(R.id.frag_SE_title);

        if (!AppSettings.getUseModularView()){
            if (modular_view.getVisibility() != View.GONE){
                FragmentTransaction ft = fm.beginTransaction();
                modular_view.setVisibility(View.GONE);
                normal_view.setVisibility(View.VISIBLE);

                if (no_frag != null){
                    ft.remove(no_frag);
                }
                if (ne_frag != null){
                    ft.remove(ne_frag);
                }
                if (so_frag != null){
                    ft.remove(so_frag);
                }
                if (se_frag != null){
                    ft.remove(se_frag);
                }

                if (main_frag == null){
                    ft.add(R.id.main_frag_container, new GpsMainFragment());
                }
                if (!ft.isEmpty()){
                    ft.commit();
                }

                for (View v : not_modular_view){
                    v.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (normal_view.getVisibility() != View.GONE) {
                normal_view.setVisibility(View.GONE);
                modular_view.setVisibility(View.VISIBLE);

                FragmentTransaction ft = fm.beginTransaction();

                if (main_frag != null){
                    ft.remove(main_frag);
                }

                if (no_frag == null) {
                    SpeedFragment sf = new SpeedFragment();
//                    frag_NO_title.setText(sf.getTitle());
                    ft.add(R.id.frag_NO, sf);
                }

                if (ne_frag == null) {
                    CompassFragment cf = new CompassFragment();
//                    frag_NE_title.setText(cf.getTitle());
                    ft.add(R.id.frag_NE, cf);
                }

                if (se_frag == null) {
                    AltitudeFragment sf = new AltitudeFragment();
//                    frag_SE_title.setText(sf.getTitle());
                    ft.add(R.id.frag_SE, sf);
                }

                if (so_frag == null) {
                    GlideRatioFragment cf = new GlideRatioFragment();
//                    frag_SO_title.setText(cf.getTitle());
                    ft.add(R.id.frag_SO, cf);
                }
                if (!ft.isEmpty()){
                    ft.commit();
                }

                for (View v : not_modular_view){
                    v.setVisibility(View.GONE);
                }
            }
        }

        StartAndBindService();
    }

    @Override
    public void setTitle(final String title, IWidgetFragment self){
        FragmentManager fm = getSupportFragmentManager();
        TextView frag_NO_title = (TextView) findViewById(R.id.frag_NO_title);
        TextView frag_NE_title = (TextView) findViewById(R.id.frag_NE_title);
        TextView frag_SO_title = (TextView) findViewById(R.id.frag_SO_title);
        TextView frag_SE_title = (TextView) findViewById(R.id.frag_SE_title);

        Fragment no_frag = fm.findFragmentById(R.id.frag_NO);
        Fragment ne_frag = fm.findFragmentById(R.id.frag_NE);
        Fragment so_frag = fm.findFragmentById(R.id.frag_SO);
        Fragment se_frag = fm.findFragmentById(R.id.frag_SE);
        if (no_frag == self){
            frag_NO_title.setText(title);
        }

        if (ne_frag == self){
            frag_NE_title.setText(title);
        }

        if (so_frag == self){
            frag_SO_title.setText(title);
        }

        if (se_frag == self){
            frag_SE_title.setText(title);
        }
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
            GpsLoggingService.SetServiceClient(null);
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
        super.onDestroy();

        Utilities.LogDebug("GpsMainActivity.onDestroy");
        StopAndUnbindServiceIfRequired();
    }

    /**
     * Called when the toggle button is clicked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Utilities.LogDebug("GpsMainActivity.onCheckedChanged");

        if (isChecked)
        {
            GetPreferences();
            SetSinglePointButtonEnabled(false);
            loggingService.SetupAutoSendTimers();
            loggingService.StartLogging();
        }
        else
        {
            SetSinglePointButtonEnabled(true);
            loggingService.StopLogging();
            ShowPreferencesSummary();
        }
    }

    /**
     * Called when the single point button is clicked
     */
    @Override
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
            ShowPreferencesSummary();

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
        Utilities.PopulateAppSettings(getApplicationContext());
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
            TextView txtDistance = (TextView) findViewById(R.id.txtDistance);
            TextView txtAutoEmail = (TextView) findViewById(R.id.txtAutoEmail);


            List<String> loggers;
            if (!Session.isStarted()){
                 loggers = FileLoggerFactory.GetFileLoggersNames();
            } else {
                loggers = new ArrayList<String>();
                for (IFileLogger lo : Session.getFileLoggers()){
                    loggers.add(lo.getName());
                }
            }

            if (loggers.size() > 0)
            {
                StringBuffer sb = new StringBuffer();
                sb.append(loggers.get(0));
                boolean b = false;
                for (final String log : loggers)
                {
                    if (!b) {
                        b = true;
                        continue;
                    }
                    sb.append(", ");
                    sb.append(log);
                }
                txtLoggingTo.setText(sb.toString());
            }
            else
            {

                txtLoggingTo.setText(R.string.summary_loggingto_screen);

            }

            if (AppSettings.getMinimumSeconds() > 0)
            {
                String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(),
                        getApplicationContext());

                txtFrequency.setText(descriptiveTime);
            }
            else
            {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            if (AppSettings.getMinimumDistanceInMeters() > 0)
            {
                if (AppSettings.shouldUseImperial())
                {
                    int minimumDistanceInFeet = Utilities.MetersToFeet(AppSettings.getMinimumDistanceInMeters());
                    txtDistance.setText(((minimumDistanceInFeet == 1)
                            ? getString(R.string.foot)
                            : String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
                }
                else
                {
                    txtDistance.setText(((AppSettings.getMinimumDistanceInMeters() == 1)
                            ? getString(R.string.meter)
                            : String.valueOf(AppSettings.getMinimumDistanceInMeters()) + getString(R.string.meters)));
                }
            }
            else
            {
                txtDistance.setText(R.string.summary_dist_regardless);
            }


            if (AppSettings.isAutoSendEnabled())
            {
                String autoEmailResx;

                if (AppSettings.getAutoSendDelay() == 0)
                {
                    autoEmailResx = "autoemail_frequency_whenistop";
                }
                else
                {

                    autoEmailResx = "autoemail_frequency_"
                            + String.valueOf(AppSettings.getAutoSendDelay()).replace(".", "");
                }

                String autoEmailDesc = getString(getResources().getIdentifier(autoEmailResx, "string", getPackageName()));

                txtAutoEmail.setText(autoEmailDesc);
            }
            else
            {
                TableRow trAutoEmail = (TableRow) findViewById(R.id.trAutoEmail);
                trAutoEmail.setVisibility(View.INVISIBLE);
            }

            onFileName(Session.getCurrentFileName());
        }
        catch (Exception ex)
        {
            Utilities.LogError("ShowPreferencesSummary", ex);
        }
    }

    /**
     * Handles the hardware back-button press
     */
    @Override
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
    public boolean onCreateOptionsMenu(Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.optionsmenu, menu);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        return true;
    }


    /**
     * Called when one of the menu items is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case R.id.mnuSettings:
                Intent settingsActivity = new Intent(getApplicationContext(), GpsSettingsActivity.class);
                startActivity(settingsActivity);
                break;
            case R.id.mnuOSM:
                UploadToOpenStreetMap();
                break;
            case R.id.mnuDropBox:
                UploadToDropBox();
                break;
            case R.id.mnuGDocs:
                UploadToGoogleDocs();
                break;
            case R.id.mnuOpenGTS:
                SendToOpenGTS();
                break;
            case R.id.mnuFtp:
                SendToFtp();
                break;
            case R.id.mnuEmail:
                SelectAndEmailFile();
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
            case R.id.mnuFAQ:
                Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                startActivity(faqtivity);
                break;
            case R.id.mnuExit:
                loggingService.StopLogging();
                ShowPreferencesSummary();
                loggingService.stopSelf();
                finish();
                break;
        }
        return false;
    }


    private void EmailNow()
    {
        Utilities.LogDebug("GpsMainActivity.EmailNow");

        if (AppSettings.isAutoSendEnabled())
        {
            loggingService.ForceEmailLogFile();
        }
        else
        {

            Intent pref = new Intent().setClass(this, GpsSettingsActivity.class);
            pref.putExtra("autosend_preferencescreen", true);
            startActivity(pref);

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

                File[] enumeratedFiles = gpxFolder.listFiles();

                Arrays.sort(enumeratedFiles, new Comparator<File>()
                {
                    public int compare(File f1, File f2)
                    {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

                for (File f : enumeratedFiles)
                {
                    fileList.add(f.getName());
                }

                fileList.add(0, locationOnly);
                final String[] files = fileList.toArray(new String[fileList.size()]);

                final Dialog dialog = new Dialog(this);
                dialog.setTitle(R.string.sharing_pick_file);
                dialog.setContentView(R.layout.filelist);
                ListView thelist = (ListView) dialog.findViewById(R.id.listViewFiles);

                thelist.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
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
                            String bodyText = getString(R.string.sharing_googlemaps_link,
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

    private void SelectAndEmailFile()
    {
        Utilities.LogDebug("GpsMainActivity.SelectAndEmailFile");

        Intent settingsIntent = new Intent(getApplicationContext(), AutoEmailActivity.class);

        if (!Utilities.IsEmailSetup())
        {

            startActivity(settingsIntent);
        }
        else
        {
            ShowFileListDialog(settingsIntent, FileSenderFactory.GetEmailSender(this));
        }

    }

    private void SendToFtp()
    {
        Utilities.LogDebug("GpsMainActivity.SendToFTP");

        Intent settingsIntent = new Intent(getApplicationContext(), AutoFtpActivity.class);

        if(!Utilities.IsFtpSetup())
        {
            startActivity(settingsIntent);
        }
        else
        {
            IFileSender fs = FileSenderFactory.GetFtpSender(getApplicationContext(), this);
            ShowFileListDialog(settingsIntent, fs);

        }
    }

    private void SendToOpenGTS()
    {
        Utilities.LogDebug("GpsMainActivity.SendToOpenGTS");

        Intent settingsIntent = new Intent(getApplicationContext(), OpenGTSActivity.class);

        if (!Utilities.IsOpenGTSSetup())
        {
            startActivity(settingsIntent);
        }
        else
        {
            IFileSender fs = FileSenderFactory.GetOpenGTSSender(getApplicationContext(), this);
            ShowFileListDialog(settingsIntent, fs);
        }
    }


    private void UploadToGoogleDocs()
    {
        Utilities.LogDebug("GpsMainActivity.UploadToGoogleDocs");

        if (!GDocsHelper.IsLinked(getApplicationContext()))
        {
            startActivity(new Intent(GpsMainActivity.this, GDocsSettingsActivity.class));
            return;
        }

        Intent settingsIntent = new Intent(GpsMainActivity.this, GDocsSettingsActivity.class);
        ShowFileListDialog(settingsIntent, FileSenderFactory.GetGDocsSender(getApplicationContext(), this));
    }


    private void UploadToDropBox()
    {
        Utilities.LogDebug("GpsMainActivity.UploadToDropBox");

        final DropBoxHelper dropBoxHelper = new DropBoxHelper(getApplicationContext(), this);

        if (!dropBoxHelper.IsLinked())
        {
            startActivity(new Intent("com.mendhak.gpslogger.DROPBOX_SETUP"));
            return;
        }

        Intent settingsIntent = new Intent(GpsMainActivity.this, DropBoxAuthorizationActivity.class);
        ShowFileListDialog(settingsIntent, FileSenderFactory.GetDropBoxSender(getApplication(), this));

    }


    /**
     * Uploads a GPS Trace to OpenStreetMap.org.
     */
    private void UploadToOpenStreetMap()
    {
        Utilities.LogDebug("GpsMainactivity.UploadToOpenStreetMap");

        if (!OSMHelper.IsOsmAuthorized(getApplicationContext()))
        {
            startActivity(OSMHelper.GetOsmSettingsIntent(getApplicationContext()));
            return;
        }

        Intent settingsIntent = OSMHelper.GetOsmSettingsIntent(getApplicationContext());

        ShowFileListDialog(settingsIntent, FileSenderFactory.GetOsmSender(getApplicationContext(), this));

    }

    private void ShowFileListDialog(final Intent settingsIntent, final IFileSender sender)
    {

        final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

        if (gpxFolder.exists())
        {
            File[] enumeratedFiles = gpxFolder.listFiles(sender);

            Arrays.sort(enumeratedFiles, new Comparator<File>()
            {
                public int compare(File f1, File f2)
                {
                    return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });

            List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

            for (File f : enumeratedFiles)
            {
                fileList.add(f.getName());
            }

            final String settingsText = getString(R.string.menu_settings);

            fileList.add(0, settingsText);
            final String[] files = fileList.toArray(new String[fileList.size()]);

            final Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.osm_pick_file);
            dialog.setContentView(R.layout.filelist);
            ListView displayList = (ListView) dialog.findViewById(R.id.listViewFiles);

            displayList.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_single_choice, files));

            displayList.setOnItemClickListener(new OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> av, View v, int index, long arg)
                {

                    dialog.dismiss();
                    String chosenFileName = files[index];

                    if (chosenFileName.equalsIgnoreCase(settingsText))
                    {
                        startActivity(settingsIntent);
                    }
                    else
                    {
                        Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.please_wait),
                                getString(R.string.please_wait));
                        List<File> files = new ArrayList<File>();
                        files.add(new File(gpxFolder, chosenFileName));
                        sender.UploadFile(files);
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

        if (!Session.shouldAllowDescription())
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
        final EditText input = new EditText(getApplicationContext());
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

        List<IFileLogger> loggers = Session.getFileLoggers();

        for (IFileLogger logger : loggers)
        {
            try
            {
                logger.Annotate(description, Session.getCurrentLocationInfo());
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
    @Override
    public void ClearForm()
    {
        FragmentManager fm = getSupportFragmentManager();

        Utilities.LogDebug("GpsMainActivity.ClearForm");
        if (!AppSettings.getUseModularView()) {
            IWidgetFragment gpsmf = (IWidgetFragment) fm.findFragmentById(R.id.main_frag_container);
            if (gpsmf == null) {
                Utilities.LogDebug("GpsMainFragment not found");
                return;
            }
            gpsmf.clear();
        } else {
            int frags_id[] = {R.id.frag_SO, R.id.frag_SE, R.id.frag_NE, R.id.frag_NO};
            for (int id : frags_id) {
                IWidgetFragment widget = (IWidgetFragment) fm.findFragmentById(id);
                if (widget == null) {
                    Utilities.LogDebug("Widget not found");
                    return;
                }
                widget.clear();
            }
        }

        Session.setPreviousLocationInfo(null);
        Session.setTotalTravelled(0d);
    }

    @Override
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
        FragmentManager fm = getSupportFragmentManager();

        Utilities.LogDebug("GpsMainActivity.ClearForm");
        if (!AppSettings.getUseModularView()) {
            IWidgetFragment gpsmf = (IWidgetFragment) fm.findFragmentById(R.id.main_frag_container);
            if (gpsmf == null) {
                Utilities.LogDebug("GpsMainFragment not found");
                return;
            }
            gpsmf.setStatus(message);
        } else {
            int frags_id[] = {R.id.frag_SO, R.id.frag_SE, R.id.frag_NE, R.id.frag_NO};
            for (int id : frags_id) {
                IWidgetFragment widget = (IWidgetFragment) fm.findFragmentById(id);
                if (widget == null) {
                    Utilities.LogDebug("Widget not found");
                    return;
                }
                widget.setStatus(message);
            }
        }
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
        FragmentManager fm = getSupportFragmentManager();

        if (!AppSettings.getUseModularView()){
            IWidgetFragment gmf = (IWidgetFragment) fm.findFragmentById(R.id.main_frag_container);
            gmf.setSatelliteInfo(number);
        } else {
            int frags_id[] = {R.id.frag_SO, R.id.frag_SE, R.id.frag_NE, R.id.frag_NO};
            for (int id : frags_id) {
                IWidgetFragment widget = (IWidgetFragment) fm.findFragmentById(id);
                if (widget == null) {
                    Utilities.LogDebug("Widget not found");
                    return;
                }
                widget.setSatelliteInfo(number);
            }
        }
    }

    /**
     * Given a location fix, processes it and displays it in the table on the
     * form.
     *
     * @param loc Location information
     */
    private void DisplayLocationInfo(Location loc)
    {
        FragmentManager fm = getSupportFragmentManager();


        if (!AppSettings.getUseModularView()) {
            IWidgetFragment gpsmf = (IWidgetFragment) fm.findFragmentById(R.id.main_frag_container);
            if (gpsmf == null) {
                Utilities.LogDebug("GpsMainFragment not found");
                return;
            }
            gpsmf.onLocationChanged(loc);
        } else {
            int frags_id[] = {R.id.frag_SO, R.id.frag_SE, R.id.frag_NE, R.id.frag_NO};
            for (int id : frags_id) {
                IWidgetFragment widget = (IWidgetFragment) fm.findFragmentById(id);
                if (widget == null) {
                    Utilities.LogDebug("Widget not found");
                    return;
                }
                widget.onLocationChanged(loc);
            }
        }
    }

    @Override
    public void OnLocationUpdate(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.OnLocationUpdate");
        DisplayLocationInfo(loc);
        ShowPreferencesSummary();
        SetMainButtonChecked(true);

        if (Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            ShowPreferencesSummary();

            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }

    }

    @Override
    public void OnSatelliteCount(int count)
    {
        SetSatelliteInfo(count);
    }

    @Override
    public void onFileName(String newFileName)
    {
        if (newFileName == null || newFileName.length() <= 0)
        {
            return;
        }

        TextView txtFilename = (TextView) findViewById(R.id.txtFileName);

        if (AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml() || AppSettings.shouldLogToIgc())
        {
            txtFilename.setText(getString(R.string.summary_current_filename_format,
                    Session.getCurrentFileName()));
        }
        else
        {
            txtFilename.setText("");
        }
    }

    @Override
    public void OnStatusMessage(String message)
    {
        SetStatus(message);
    }

    @Override
    public void OnFatalMessage(String message)
    {
        Utilities.MsgBox(getString(R.string.sorry), message, this);
    }

    @Override
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

package com.mendhak.gpslogger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.dropbox.DropBoxAuthorizationActivity;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.ftp.AutoFtpActivity;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsSettingsActivity;
import com.mendhak.gpslogger.senders.opengts.OpenGTSActivity;
import com.mendhak.gpslogger.senders.osm.OSMAuthorizationActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import com.mendhak.gpslogger.settings.GeneralSettingsActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsActivity;
import com.mendhak.gpslogger.settings.UploadSettingsActivity;
import com.mendhak.gpslogger.views.GenericViewFragment;
import com.mendhak.gpslogger.views.GpsBigViewFragment;
import com.mendhak.gpslogger.views.GpsDetailedViewFragment;
import com.mendhak.gpslogger.views.GpsSimpleViewFragment;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GpsMainActivity extends Activity
        implements GenericViewFragment.IGpsViewCallback, NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener, IGpsLoggerServiceClient, IActionListener {

    private static Intent serviceIntent;
    private GpsLoggingService loggingService;

    FragmentManager fragmentManager;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    MenuItem mnuAnnotate;
    MenuItem mnuOnePoint;
    MenuItem mnuAutoSendNow;
    private boolean annotationMarked;
    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Utilities.ConfigureLogbackDirectly(getApplicationContext());
        tracer = LoggerFactory.getLogger(GpsMainActivity.class.getSimpleName());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_main);

        SetUpNavigationDrawer();

        if (fragmentManager == null) {
            tracer.debug("Creating fragmentManager");
            fragmentManager = getFragmentManager();
        }

        SetUpActionBar();
        StartAndBindService();
    }




    @Override
    protected void onStart() {
        super.onStart();
        StartAndBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetPreferences();
        StartAndBindService();
        enableDisableMenuItems();
    }

    @Override
    protected void onPause() {
        StopAndUnbindServiceIfRequired();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        StopAndUnbindServiceIfRequired();
        super.onDestroy();

    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            navigationDrawerFragment.toggleDrawer();
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService()) {
            StopAndUnbindServiceIfRequired();
        }

        return super.onKeyDown(keyCode, event);
    }


    /**
     *
     */
    private void SetUpNavigationDrawer() {
        // Set up the drawer
        navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }



    /**
     * Handles drawer item selection
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {

        tracer.debug("Navigation menu item: " + String.valueOf(position));
        switch (position) {
            case 0:
                break;
            case 1:
                LaunchActivity(GeneralSettingsActivity.class);
                break;
            case 2:
                LaunchActivity(LoggingSettingsActivity.class);
                break;
            case 3:
                LaunchActivity(UploadSettingsActivity.class);
                break;
            case 4:
                LaunchActivity(AutoFtpActivity.class);
                break;
            case 5:
                LaunchActivity(AutoEmailActivity.class);
                break;
            case 6:
                LaunchActivity(OpenGTSActivity.class);
                break;
            case 7:
                LaunchActivity(GDocsSettingsActivity.class);
                break;
            case 8:
                LaunchActivity(OSMAuthorizationActivity.class);
                break;
            case 9:
                LaunchActivity(DropBoxAuthorizationActivity.class);
                break;
            default:
                loggingService.StopLogging();
                loggingService.stopSelf();
                finish();
                break;

        }

    }

    /**
     * Launches activity in a delayed handler, less stutter
     */
    private void LaunchActivity(final Class activityClass) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent targetActivity = new Intent(getApplicationContext(), activityClass);
                startActivity(targetActivity);
            }
        }, 120);
    }


    public void SetUpActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(), R.array.gps_main_views, android.R.layout.simple_spinner_dropdown_item);

        actionBar.setListNavigationCallbacks(spinnerAdapter, this);

        //Reload the user's previously selected view
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        actionBar.setSelectedNavigationItem(prefs.getInt("dropdownview", 0));

        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("");
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar);

        ImageButton helpButton = (ImageButton) actionBar.getCustomView().findViewById(R.id.imgHelp);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                startActivity(faqtivity);
            }
        });

    }

    /**
     * Handles dropdown selection
     */
    @Override
    public boolean onNavigationItemSelected(int position, long id) {

        tracer.debug("Changing main view: " + String.valueOf(position));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("dropdownview", position);
        editor.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        switch (position) {
            case 0:
                transaction.replace(R.id.container, GpsSimpleViewFragment.newInstance());
                break;
            case 1:
                transaction.replace(R.id.container, GpsDetailedViewFragment.newInstance());
                break;
            default:
            case 2:
                transaction.replace(R.id.container, GpsBigViewFragment.newInstance());
                break;
        }
        transaction.commitAllowingStateLoss();

        return true;
    }


    /**
     * Creates menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gps_main, menu);
        mnuAnnotate = menu.findItem(R.id.mnuAnnotate);
        mnuOnePoint = menu.findItem(R.id.mnuOnePoint);
        mnuAutoSendNow = menu.findItem(R.id.mnuAutoSendNow);
        enableDisableMenuItems();
        return true;
    }

    private void enableDisableMenuItems() {

        OnWaitingForLocation(Session.isWaitingForLocation());
        SetBulbStatus(Session.isStarted());

        if (mnuOnePoint != null) {
            mnuOnePoint.setEnabled(!Session.isStarted());
            mnuOnePoint.setIcon( (Session.isStarted()  ? R.drawable.singlepoint_disabled : R.drawable.singlepoint ) );
        }

        if(mnuAutoSendNow != null){
            mnuAutoSendNow.setEnabled(Session.isStarted());
        }

        if (mnuAnnotate != null) {

            if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl()) {
                mnuAnnotate.setIcon(R.drawable.annotate2_disabled);
                mnuAnnotate.setEnabled(false);
            } else {
                if (annotationMarked) {
                    mnuAnnotate.setIcon(R.drawable.annotate2_active);
                } else {
                    mnuAnnotate.setIcon(R.drawable.annotate2);
                }
            }

        }
    }


    /**
     * Handles menu item selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        tracer.debug("Menu Item: " + String.valueOf(item.getTitle()));

        switch (id) {
            case R.id.mnuAnnotate:
                Annotate();
                return true;
            case R.id.mnuOnePoint:
                LogSinglePoint();
                return true;
            case R.id.mnuShare:
                Share();
                return true;
            case R.id.mnuOSM:
                UploadToOpenStreetMap();
                return true;
            case R.id.mnuDropBox:
                UploadToDropBox();
                return true;
            case R.id.mnuGDocs:
                UploadToGoogleDocs();
                return true;
            case R.id.mnuOpenGTS:
                SendToOpenGTS();
                return true;
            case R.id.mnuFtp:
                SendToFtp();
                return true;
            case R.id.mnuEmail:
                SelectAndEmailFile();
                return true;
            case R.id.mnuAutoSendNow:
                ForceAutoSendNow();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void ForceAutoSendNow()
    {
        tracer.debug("Auto send now");

        if (AppSettings.isAutoSendEnabled())
        {

                Utilities.ShowProgress(this, getString(R.string.autosend_sending),
                        getString(R.string.please_wait));
                loggingService.ForceAutoSendNow();


        }
        else
        {
            Intent pref = new Intent().setClass(this, UploadSettingsActivity.class);
            startActivity(pref);
        }

    }

    private void LogSinglePoint() {
        loggingService.LogOnce();
        enableDisableMenuItems();
    }

    /**
     * Annotates GPX and KML files, TXT files are ignored.
     * <p/>
     * The annotation is done like this:
     * <wpt lat="##.##" lon="##.##">
     * <name>user input</name>
     * </wpt>
     * <p/>
     * The user is prompted for the content of the <name> tag. If a valid
     * description is given, the logging service starts in single point mode.
     */
    private void Annotate() {

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl()) {
            tracer.debug("GPX, KML, URL disabled; annotation shouldn't work");
            Toast.makeText(getApplicationContext(), getString(R.string.annotation_requires_logging), 1000).show();
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(GpsMainActivity.this);
        alert.setTitle(R.string.add_description);
        alert.setMessage(R.string.letters_numbers);

        // Set an EditText view to get user input
        final EditText input = new EditText(getApplicationContext());
        input.setTextColor(getResources().getColor(android.R.color.black));
        input.setText(Session.getDescription());
        alert.setView(input);

        /* ok */
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String desc = Utilities.CleanDescription(input.getText().toString());
                if (desc.length() == 0) {
                    tracer.debug("Clearing annotation");
                    Session.clearDescription();
                    OnClearAnnotation();
                } else {
                    tracer.debug("Setting annotation: " + desc);
                    Session.setDescription(desc);
                    OnSetAnnotation();
                    // logOnce will start single point mode.
                    if (!Session.isStarted()) {
                        tracer.debug("Will start log-single-point");
                        LogSinglePoint();
                    }
                }
            }

        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }


    /**
     * Uploads a GPS Trace to OpenStreetMap.org.
     */
    private void UploadToOpenStreetMap() {
        if (!OSMHelper.IsOsmAuthorized(getApplicationContext())) {
            tracer.debug("Not authorized, opening OSM activity");
            startActivity(OSMHelper.GetOsmSettingsIntent(getApplicationContext()));
            return;
        }

        Intent settingsIntent = OSMHelper.GetOsmSettingsIntent(getApplicationContext());
        ShowFileListDialog(settingsIntent, FileSenderFactory.GetOsmSender(getApplicationContext(), this));
    }

    private void UploadToDropBox() {
        final DropBoxHelper dropBoxHelper = new DropBoxHelper(getApplicationContext(), this);

        if (!dropBoxHelper.IsLinked()) {
            tracer.debug("Not linked, opening Dropbox activity");
            startActivity(new Intent("com.mendhak.gpslogger.DROPBOX_SETUP"));
            return;
        }

        Intent settingsIntent = new Intent(GpsMainActivity.this, DropBoxAuthorizationActivity.class);
        ShowFileListDialog(settingsIntent, FileSenderFactory.GetDropBoxSender(getApplication(), this));
    }

    private void SendToOpenGTS() {
        Intent settingsIntent = new Intent(getApplicationContext(), OpenGTSActivity.class);

        if (!Utilities.IsOpenGTSSetup()) {
            tracer.debug("Not set up, opening OpenGTS activity");
            startActivity(settingsIntent);
        } else {
            IFileSender fs = FileSenderFactory.GetOpenGTSSender(getApplicationContext(), this);
            ShowFileListDialog(settingsIntent, fs);
        }
    }

    private void UploadToGoogleDocs() {
        if (!GDocsHelper.IsLinked(getApplicationContext())) {
            tracer.debug("Not linked, opening Google Docs setup activity");
            startActivity(new Intent(GpsMainActivity.this, GDocsSettingsActivity.class));
            return;
        }

        Intent settingsIntent = new Intent(GpsMainActivity.this, GDocsSettingsActivity.class);
        ShowFileListDialog(settingsIntent, FileSenderFactory.GetGDocsSender(getApplicationContext(), this));
    }

    private void SendToFtp() {
        Intent settingsIntent = new Intent(getApplicationContext(), AutoFtpActivity.class);

        if (!Utilities.IsFtpSetup()) {
            tracer.debug("Not setup, opening FTP setup activity");
            startActivity(settingsIntent);
        } else {
            IFileSender fs = FileSenderFactory.GetFtpSender(getApplicationContext(), this);
            ShowFileListDialog(settingsIntent, fs);

        }
    }

    private void SelectAndEmailFile() {
        Intent settingsIntent = new Intent(getApplicationContext(), AutoEmailActivity.class);

        if (!Utilities.IsEmailSetup()) {
            tracer.debug("Not set up, opening email setup activity");
            startActivity(settingsIntent);
        } else {
            ShowFileListDialog(settingsIntent, FileSenderFactory.GetEmailSender(this));
        }

    }

    private void ShowFileListDialog(final Intent settingsIntent, final IFileSender sender) {

        final File gpxFolder = new File(AppSettings.getGpsLoggerFolder());

        if (gpxFolder != null && gpxFolder.exists()) {
            File[] enumeratedFiles = Utilities.GetFilesInFolder(gpxFolder, sender);

            Arrays.sort(enumeratedFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    if(f1 != null && f2 != null){
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                    return -1;
                }
            });

            List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

            for (File f : enumeratedFiles) {
                fileList.add(f.getName());
            }

            final String settingsText = getString(R.string.menu_settings);

            fileList.add(0, settingsText);
            final String[] files = fileList.toArray(new String[fileList.size()]);

            final Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.osm_pick_file);
            dialog.setContentView(R.layout.common_filelist);
            ListView displayList = (ListView) dialog.findViewById(R.id.listViewFiles);

            displayList.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    R.layout.common_list_black_text, R.id.list_content, files));

            displayList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> av, View v, int index, long arg) {

                    dialog.dismiss();
                    String chosenFileName = files[index];

                    if (chosenFileName.equalsIgnoreCase(settingsText)) {
                        tracer.debug("User chose to open settings");
                        startActivity(settingsIntent);
                    } else {
                        tracer.info("Selected file to upload- " + chosenFileName);
                        Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.please_wait),
                                getString(R.string.please_wait));
                        List<File> files = new ArrayList<File>();
                        files.add(new File(gpxFolder, chosenFileName));
                        sender.UploadFile(files);
                    }
                }
            });
            dialog.show();
        } else {
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }

    /**
     * Allows user to send a GPX/KML file along with location, or location only
     * using a provider. 'Provider' means any application that can accept such
     * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
     */
    private void Share() {
        tracer.debug("GpsMainActivity.Share");
        try {

            final String locationOnly = getString(R.string.sharing_location_only);
            final File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
            if (gpxFolder.exists()) {

                File[] enumeratedFiles = Utilities.GetFilesInFolder(gpxFolder);

                Arrays.sort(enumeratedFiles, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

                for (File f : enumeratedFiles) {
                    fileList.add(f.getName());
                }

                fileList.add(0, locationOnly);
                final String[] files = fileList.toArray(new String[fileList.size()]);

                final Dialog dialog = new Dialog(this);
                dialog.setTitle(R.string.sharing_pick_file);
                dialog.setContentView(R.layout.common_filelist);
                ListView thelist = (ListView) dialog.findViewById(R.id.listViewFiles);

                thelist.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                        R.layout.common_list_black_text, R.id.list_content, files));

                thelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    public void onItemClick(AdapterView<?> av, View v, int index, long arg) {
                        dialog.dismiss();
                        String chosenFileName = files[index];

                        final Intent intent = new Intent(Intent.ACTION_SEND);

                        // intent.setType("text/plain");
                        intent.setType("*/*");

                        if (chosenFileName.equalsIgnoreCase(locationOnly)) {
                            tracer.debug("User selected location only");
                            intent.setType("text/plain");
                        }

                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_mylocation));
                        if (Session.hasValidLocation()) {
                            String bodyText = getString(R.string.sharing_googlemaps_link,
                                    String.valueOf(Session.getCurrentLatitude()),
                                    String.valueOf(Session.getCurrentLongitude()));
                            intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                            intent.putExtra("sms_body", bodyText);
                        }

                        if (chosenFileName.length() > 0
                                && !chosenFileName.equalsIgnoreCase(locationOnly)) {

                            tracer.info("Selected file to share - " + chosenFileName);
                            intent.putExtra(Intent.EXTRA_STREAM,
                                    Uri.fromFile(new File(gpxFolder, chosenFileName)));
                        }

                        startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));

                    }
                });
                dialog.show();
            } else {
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
            }
        } catch (Exception ex) {
            tracer.error("Share", ex);
        }

    }


    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            tracer.debug("Disconnected from GPSLoggingService from MainActivity");
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            tracer.debug("Connected to GPSLoggingService from MainActivity");
            loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
            GpsLoggingService.SetServiceClient(GpsMainActivity.this);

            if (Session.hasDescription()) {
                OnSetAnnotation();
            }

        }
    };


    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService() {
        tracer.debug(".");
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
    private void StopAndUnbindServiceIfRequired() {
        tracer.debug(".");
        if (Session.isBoundToService()) {

            try{
                unbindService(gpsServiceConnection);
                Session.setBoundToService(false);
            }catch (Exception e){
                tracer.error("Could not unbind service", e);
            }
        }

        if (!Session.isStarted()) {
            tracer.debug("Stopping the service");
            //serviceIntent = new Intent(this, GpsLoggingService.class);
            try{
                stopService(serviceIntent);
            } catch(Exception e){
                tracer.error("Could not stop the service", e);
            }

        }

    }

    //IGpsLoggerServiceClient callbacks

    @Override
    public void OnStatusMessage(String message) {
        tracer.debug(message);

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetStatusMessage(message);
        }
    }

    @Override
    public void OnFatalMessage(String message) {
        tracer.debug(message);
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetFatalMessage(message);
        }
    }

    @Override
    public void OnLocationUpdate(Location loc) {
        tracer.debug(".");
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetLocation(loc);
        }

    }

    @Override
    public void OnSatelliteCount(int count) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetSatelliteCount(count);
        }
    }

    @Override
    public void OnStartLogging() {
        tracer.debug(".");

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetLoggingStarted();
        }

        enableDisableMenuItems();

    }

    @Override
    public void OnStopLogging() {
        tracer.debug(".");
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).SetLoggingStopped();
        }

        enableDisableMenuItems();
    }

    private void SetBulbStatus(boolean started) {
        ImageView bulb = (ImageView) findViewById(R.id.notification_bulb);
        bulb.setImageResource(started ? R.drawable.circle_green : R.drawable.circle_none);
    }

    @Override
    public void OnSetAnnotation() {
        tracer.debug(".");
        this.annotationMarked = true;
        enableDisableMenuItems();
    }

    @Override
    public void OnClearAnnotation() {
        tracer.debug(".");
        this.annotationMarked = false;
        enableDisableMenuItems();

    }


    @Override
    public void onFileName(String newFileName) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            ((GenericViewFragment) currentFragment).OnFileNameChange(newFileName);
        }
    }

    @Override
    public void OnWaitingForLocation(boolean inProgress) {
        ProgressBar fixBar = (ProgressBar) findViewById(R.id.progressBarGpsFix);
        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
    }


    // IActionListener callbacks
    @Override
    public void OnComplete() {
        tracer.debug(".");
        Utilities.HideProgress();
    }

    @Override
    public void OnFailure() {
        tracer.debug(".");
        Utilities.HideProgress();
    }

    //IGpsViewCallback callbacks
    //These methods come from the fragments
    @Override
    public void onRequestStartLogging() {
        tracer.info(".");
        StartLogging();
    }

    @Override
    public void onRequestStopLogging() {
        tracer.info(".");
        StopLogging();
    }

    @Override
    public void onRequestToggleLogging() {
        tracer.info(".");

        if (Session.isStarted()) {
            tracer.info("Toggle requested - stopping");
            StopLogging();
        } else {
            tracer.info("Toggle requested - starting");
            StartLogging();
        }

    }

    private void StartLogging() {
        GetPreferences();
        loggingService.SetupAutoSendTimers();
        loggingService.StartLogging();
        enableDisableMenuItems();
    }

    private void StopLogging() {
        loggingService.StopLogging();
        enableDisableMenuItems();
    }

    /**
     * Gets preferences chosen by the user
     */
    private void GetPreferences() {
        Utilities.PopulateAppSettings(getApplicationContext());
    }


}

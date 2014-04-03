package com.mendhak.gpslogger;

import android.app.*;
import android.content.*;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.*;
import android.support.v4.widget.DrawerLayout;
import android.widget.*;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.dropbox.DropBoxAuthorizationActivity;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.ftp.AutoFtpActivity;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsSettingsActivity;
import com.mendhak.gpslogger.senders.opengts.OpenGTSActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import com.mendhak.gpslogger.settings.GeneralSettingsActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsActivity;
import com.mendhak.gpslogger.settings.UploadSettingsActivity;
import com.mendhak.gpslogger.views.GpsDetailedViewFragment;
import com.mendhak.gpslogger.views.GpsSimpleViewFragment;
import com.mendhak.gpslogger.views.GpsLegacyFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GpsMainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener, GpsLegacyFragment.IGpsLegacyFragmentListener, IGpsLoggerServiceClient, IActionListener {

    private static Intent serviceIntent;
    private GpsLoggingService loggingService;

    FragmentManager fragmentManager;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    MenuItem mnuAnnotate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utilities.LogDebug("GpsMainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_main);

        SetUpNavigationDrawer();

        if (fragmentManager == null) {
            fragmentManager = getFragmentManager();
        }

        SetUpActionBar();
        StartAndBindService();
    }

    @Override
    protected void onStart() {
        Utilities.LogDebug("GpsMainActivity.onStart");
        super.onStart();
        StartAndBindService();
    }

    @Override
    protected void onResume() {
        Utilities.LogDebug("GpsMainActivity.onResume");
        super.onResume();
        StartAndBindService();
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

    public boolean onKeyUp(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_MENU){
            Utilities.LogInfo("KeyUp Menu");
            navigationDrawerFragment.toggleDrawer();
        }

        return super.onKeyUp(keyCode, event);
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
     *
     */
    private void SetUpNavigationDrawer() {
        // Set up the drawer
        navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    private void changeMainView(int view) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        switch (view) {
            case 0:
                transaction.replace(R.id.container, new GpsSimpleViewFragment());
                break;
            case 1:
                transaction.replace(R.id.container, new GpsDetailedViewFragment());
                break;
            default:
            case 2:
                transaction.replace(R.id.container, GpsLegacyFragment.newInstance());
                break;
        }
        transaction.commit();
    }

    /**
     * Handles drawer item selection
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {

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
                loggingService.StopLogging();
                loggingService.stopSelf();
                finish();
                break;
            default:

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
        actionBar.setDisplayShowTitleEnabled(false);

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
        changeMainView(position);
        return true;

    }


    /**
     * Creates menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.gps_main, menu);
            mnuAnnotate = menu.findItem(R.id.mnuAnnotate);
            enableDisableMenuItems();
            return true;
    }

    private void enableDisableMenuItems() {


        if(mnuAnnotate == null)
        {
            return;
        }

        if(!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl())
        {
            mnuAnnotate.setIcon(R.drawable.ic_menu_edit_disabled);
            mnuAnnotate.setEnabled(false);
        }
        else
        {
            mnuAnnotate.setIcon(android.R.drawable.ic_menu_edit);
        }
    }

    public void SetAnnotationButtonMarked(boolean marked)
    {
        if(mnuAnnotate == null){
            return;
        }

        if (marked)
        {
            mnuAnnotate.setIcon(R.drawable.ic_menu_edit_active);
        }
        else
        {
            mnuAnnotate.setIcon(android.R.drawable.ic_menu_edit);
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

        switch(id){
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
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void LogSinglePoint() {
        GpsLegacyFragment frag = (GpsLegacyFragment)getFragmentManager().findFragmentById(R.id.container);
        frag.setCurrentlyLogging(true);
        loggingService.LogOnce();
    }

    /**
     * Annotates GPX and KML files, TXT files are ignored.
     *
     * The annotation is done like this:
     *     <wpt lat="##.##" lon="##.##">
     *         <name>user input</name>
     *     </wpt>
     *
     * The user is prompted for the content of the <name> tag. If a valid
     * description is given, the logging service starts in single point mode.
     *
     */
    private void Annotate()
    {
        Utilities.LogDebug("GpsMainActivity.Annotate");

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl())
        {
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
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                final String desc = Utilities.CleanDescription(input.getText().toString());
                if (desc.length() == 0)
                {
                    Session.clearDescription();
                    OnClearAnnotation();
                }
                else
                {
                    Session.setDescription(desc);
                    OnSetAnnotation();
                    // logOnce will start single point mode.
                    if (!Session.isStarted()){
                        LogSinglePoint();
                    }
                }
            }

        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Cancelled.
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
        //alert.show();
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

    private void ShowFileListDialog(final Intent settingsIntent, final IFileSender sender)
    {

        final File gpxFolder = new File(AppSettings.getGpsLoggerFolder());

        if (gpxFolder != null && gpxFolder.exists())
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
                    R.layout.list_black_text, R.id.list_content, files));

            displayList.setOnItemClickListener(new AdapterView.OnItemClickListener()
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
            final File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
            if (gpxFolder.exists())
            {

                File[] enumeratedFiles = gpxFolder.listFiles();

                Arrays.sort(enumeratedFiles, new Comparator<File>() {
                    public int compare(File f1, File f2) {
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
                        R.layout.list_black_text, R.id.list_content, files));

                thelist.setOnItemClickListener(new AdapterView.OnItemClickListener()
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


    @Override
    public void OnNewGpsLegacyMessage(String message) {
        Utilities.LogDebug(message);
    }

    @Override
    public void OnGpsLegacyButtonClick() {
        Utilities.LogDebug("Starting logging");

        if (!Session.isStarted())
        {
            loggingService.StartLogging();
        }
        else
        {
            loggingService.StopLogging();
        }
    }



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


//            Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);
//
//            buttonSinglePoint.setOnClickListener(GpsMainActivity.this);

//            if (Session.isStarted())
//            {
//                if (Session.isSinglePointMode())
//                {
//                    SetMainButtonEnabled(false);
//                }
//                else
//                {
//                    SetMainButtonChecked(true);
//                    SetSinglePointButtonEnabled(false);
//                }
//
//                DisplayLocationInfo(Session.getCurrentLocationInfo());
//            }

//            // Form setup - toggle button, display existing location info
//            ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
//            buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);

            if (Session.hasDescription()){
                OnSetAnnotation();
            }

        }
    };


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
    public void OnStatusMessage(String message) {

    }

    @Override
    public void OnFatalMessage(String message) {

    }

    @Override
    public void OnLocationUpdate(Location loc) {
        GpsLegacyFragment frag = (GpsLegacyFragment)getFragmentManager().findFragmentById(R.id.container);
        //frag.onTextUpdate(String.valueOf(loc.getLatitude()));
        frag.setLocationInfo(loc);
    }

    @Override
    public void OnSatelliteCount(int count) {

    }

    @Override
    public void ClearForm() {

    }

    @Override
    public void OnStopLogging() {
        GpsLegacyFragment frag = (GpsLegacyFragment)getFragmentManager().findFragmentById(R.id.container);
        frag.onTextUpdate("Stopped logging");
    }

    @Override
    public void OnSetAnnotation() {
        Utilities.LogDebug("GpsMainActivity.OnSetAnnotation");
        SetAnnotationButtonMarked(true);
    }

    @Override
    public void OnClearAnnotation() {
        Utilities.LogDebug("GpsMainActivity.OnClearAnnotation");
        SetAnnotationButtonMarked(false);
    }

    @Override
    public Activity GetActivity() {
        return null;
    }

    @Override
    public void onFileName(String newFileName) {

    }

    // IActionListener callbacks
    @Override
    public void OnComplete() {
        Utilities.HideProgress();
    }

    @Override
    public void OnFailure() {
        Utilities.HideProgress();
    }
}

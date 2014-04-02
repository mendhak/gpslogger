package com.mendhak.gpslogger;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.support.v4.widget.DrawerLayout;
import android.widget.*;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.settings.GeneralSettingsActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsActivity;
import com.mendhak.gpslogger.settings.UploadSettingsActivity;
import com.mendhak.gpslogger.views.GpsLegacyFragment;

import java.util.ArrayList;

public class GpsMainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener, GpsLegacyFragment.IGpsLegacyFragmentListener, IGpsLoggerServiceClient {

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
     *
     */
    private void SetUpNavigationDrawer() {
        // Set up the drawer
        navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    private void ShowFragment(int fragment_number) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (fragment_number == 0) {
            transaction.replace(R.id.container, GpsLegacyFragment.newInstance());
        } else {

            transaction.replace(R.id.container, new NavigationDrawerFragment());
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
        // Our logic
        ShowFragment(position);
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
}

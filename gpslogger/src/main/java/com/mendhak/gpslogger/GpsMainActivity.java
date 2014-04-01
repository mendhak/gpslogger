package com.mendhak.gpslogger;

import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.*;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        super.onStart();
        StartAndBindService();
    }

    @Override
    protected void onResume() {
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
        actionBar.setTitle("TEST TITEL");
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
            return true;
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
        if (id == R.id.mnuAnnotate) {
            Utilities.LogDebug("GpsMainActivity - Menu Annotate");
            Toast.makeText(this, "Annotate", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
               // OnSetAnnotation();
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
        frag.onTextUpdate(String.valueOf(loc.getLatitude()));
    }

    @Override
    public void OnSatelliteCount(int count) {

    }

    @Override
    public void ClearForm() {

    }

    @Override
    public void OnStopLogging() {

    }

    @Override
    public void OnSetAnnotation() {

    }

    @Override
    public void OnClearAnnotation() {

    }

    @Override
    public Activity GetActivity() {
        return null;
    }

    @Override
    public void onFileName(String newFileName) {

    }
}

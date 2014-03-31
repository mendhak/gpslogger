package com.mendhak.gpslogger;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.settings.GeneralSettingsActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsActivity;
import com.mendhak.gpslogger.settings.UploadSettingsActivity;
import com.mendhak.gpslogger.views.GpsLegacyFragment;

import java.util.ArrayList;

public class GpsMainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener, GpsLegacyFragment.IGpsLegacyFragmentListener {


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
    }

    /**
     *
     */
    private void SetUpNavigationDrawer() {
        // Set up the drawer
        navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        //Prevents the action bar spinner from hiding when drawer is opened
        drawerLayout.setDrawerListener( new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) {
                getActionBar().setDisplayShowTitleEnabled(false);
            }

            @Override
            public void onDrawerOpened(View view) {
                getActionBar().setDisplayShowTitleEnabled(false);
            }

            @Override
            public void onDrawerClosed(View view) {
                getActionBar().setDisplayShowTitleEnabled(false);
            }

            @Override
            public void onDrawerStateChanged(int i) {
                getActionBar().setDisplayShowTitleEnabled(false);
            }
        });
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void OnNewGpsLegacyMessage(String message) {
        Utilities.LogDebug(message);
    }
}

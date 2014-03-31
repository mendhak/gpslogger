package com.mendhak.gpslogger;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.mendhak.gpslogger.settings.GeneralSettingsActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsActivity;
import com.mendhak.gpslogger.settings.UploadSettingsActivity;
import com.mendhak.gpslogger.views.GpsLegacyFragment;

import java.util.ArrayList;

public class GpsMainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener {


    FragmentManager fragmentManager;
    GpsLegacyFragment fragment1;
    NavigationDrawerFragment fragment2;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_main);

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (fragmentManager == null) {
            fragmentManager = getFragmentManager();
        }

        restoreActionBar();
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

    private void LaunchActivity(final Class activityClass) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent targetActivity = new Intent(getApplicationContext(), activityClass);
                startActivity(targetActivity);
            }
        }, 120);
    }


    public void restoreActionBar() {


        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayList<String> itemList = new ArrayList<String>();
        itemList.add("Dropdown 1");
        itemList.add("Dropdown 2");

        ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(this, R.layout.simpledropdown, R.id.simpletext, itemList);
        getActionBar().setListNavigationCallbacks(aAdpt, this);
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setTitle("TEST TITEL");
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.actionbar);

    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {

        // Our logic
        ShowFragment(position);
        return true;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.gps_main, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

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


}

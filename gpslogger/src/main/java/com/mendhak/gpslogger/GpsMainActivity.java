package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class GpsMainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ActionBar.OnNavigationListener  {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));


    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();

        switch(position){
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance("Only the beginning"))
                        .commit();
                mTitle = "First";
                break;
            case 1:
                Intent settingsActivity = new Intent(getApplicationContext(), GeneralSettingsActivity.class);
                startActivity(settingsActivity);
                break;
            default:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance("Default case"))
                        .commit();
                mTitle = "SEcond";
        }


    }


    public void restoreActionBar() {



        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayList<String> itemList = new ArrayList<String>();
        itemList.add("Section 1");
        itemList.add("Section 2");

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
        return true;

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.gps_main, menu);
            restoreActionBar();
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(String message) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            //args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putString("parent_message", message);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.d("GPSLogger", "-------------------onCreateView : " + getArguments().getString("parent_message"));




            View rootView;
            TextView textView;
            rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);
            textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getArguments().getString("parent_message"));


//            switch(sectionNumber){
//                case 1:
//                    rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);
//                    textView = (TextView) rootView.findViewById(R.id.section_label);
//                    textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
//                    break;
//                case 2:
//                default:
//                    rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);
//                    textView = (TextView) rootView.findViewById(R.id.section_label);
//                    textView.setText("This is the default case");
//                    break;
//
//            }
//            View rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
//            ((GpsMainActivity) activity).onSectionAttached(
//                    getArguments().getString("parent_message"));
        }
    }

}

/*******************************************************************************
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.mendhak.gpslogger;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.afollestad.materialdialogs.MaterialDialog;
import com.heinrichreimersoftware.materialdrawer.DrawerView;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerItem;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.common.events.*;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import com.mendhak.gpslogger.views.GenericViewFragment;
import com.mendhak.gpslogger.views.GpsBigViewFragment;
import com.mendhak.gpslogger.views.GpsDetailedViewFragment;
import com.mendhak.gpslogger.views.GpsSimpleViewFragment;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GpsMainActivity extends ActionBarActivity
        implements
        Toolbar.OnMenuItemClickListener,
        ActionBar.OnNavigationListener {

    private static Intent serviceIntent;
    private ActionBarDrawerToggle drawerToggle;
    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utilities.ConfigureLogbackDirectly(getApplicationContext());
        tracer = LoggerFactory.getLogger(GpsMainActivity.class.getSimpleName());

        loadPresetProperties();

        setContentView(R.layout.activity_gps_main);

        SetUpToolbar();
        SetUpNavigationDrawer();
        LoadDefaultFragmentView();
        StartAndBindService();
        RegisterEventBus();
        AppCompatActivityMenuKeyInterceptor.intercept(this);
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void UnregisterEventBus(){
        try {
        EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
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

        if (Session.hasDescription()) {
            SetAnnotationReady();
        }

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
        UnregisterEventBus();
        super.onDestroy();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            ToggleDrawer();
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

        if(keyCode == KeyEvent.KEYCODE_BACK){
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
                ToggleDrawer();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }



    private void loadPresetProperties() {

        //Either look for /<appfolder>/gpslogger.properties or /sdcard/gpslogger.properties
        File file =  new File(Utilities.GetDefaultStorageFolder(getApplicationContext()) + "/gpslogger.properties");
        if(!file.exists()){
            file = new File(Environment.getExternalStorageDirectory() + "/gpslogger.properties");
            if(!file.exists()){
                return;
            }
        }

        try {
            Properties props = new Properties();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            props.load(reader);

            for(Object key : props.keySet()){

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();

                String value = props.getProperty(key.toString());
                tracer.info("Setting preset property: " + key.toString() + " to " + value.toString());

                if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")){
                    editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
                }
                else {
                    editor.putString(key.toString(), value);
                }
                editor.apply();
            }

        } catch (Exception e) {
            tracer.error("Could not load preset properties", e);
        }
    }


    /**
     * Helper method, launches activity in a delayed handler, less stutter
     */
    private void LaunchPreferenceScreen(final String whichFragment) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent targetActivity = new Intent(getApplicationContext(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", whichFragment);
                startActivity(targetActivity);
            }
        }, 250);
    }



    public Toolbar GetToolbar(){
        return (Toolbar)findViewById(R.id.toolbar);
    }

    public void SetUpToolbar(){
        try{
            Toolbar toolbar = GetToolbar();
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            //Deprecated in Lollipop but required if targeting 4.x
            SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.gps_main_views, R.layout.spinner_dropdown_item);
            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, this);
            getSupportActionBar().setSelectedNavigationItem(GetUserSelectedNavigationItem());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
        }
        catch(Exception ex){
            //http://stackoverflow.com/questions/26657348/appcompat-v7-v21-0-0-causing-crash-on-samsung-devices-with-android-v4-2-2
            tracer.error("Thanks for this, Samsung", ex);
        }

    }

    public void SetUpNavigationDrawer() {

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final DrawerView drawer = (DrawerView) findViewById(R.id.drawer);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                GetToolbar(),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ){

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };


        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primaryColorDark));
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.closeDrawer(drawer);

        drawer.addDivider();
        drawer.addItem(new DrawerItem()
                        .setId(0)
                        .setImage(getResources().getDrawable(R.drawable.settings))
                        .setTextPrimary(getString(R.string.pref_general_title))
                        .setTextSecondary(getString(R.string.pref_general_summary))
        );

        drawer.addItem(new DrawerItem()
                        .setId(1)
                        .setImage(getResources().getDrawable(R.drawable.loggingsettings))
                        .setTextPrimary(getString(R.string.pref_logging_title))
                        .setTextSecondary(getString(R.string.pref_logging_summary))
        );

        drawer.addItem(new DrawerItem()
                        .setId(2)
                        .setImage(getResources().getDrawable(R.drawable.performance))
                        .setTextPrimary(getString(R.string.pref_performance_title))
                        .setTextSecondary(getString(R.string.pref_performance_summary))
        );


        drawer.addDivider();


        drawer.addItem(new DrawerItem()
                .setId(3)
                .setImage(getResources().getDrawable(R.drawable.autosend))
                .setTextPrimary(getString(R.string.pref_autosend_title))
                .setTextSecondary(getString(R.string.pref_autosend_summary)));

        drawer.addItem(new DrawerItem()
                        .setId(4)
                        .setImage(getResources().getDrawable(R.drawable.googledrive))
                        .setTextPrimary(getString(R.string.gdocs_setup_title))
        );

        drawer.addItem(new DrawerItem()
                        .setId(5)
                        .setImage(getResources().getDrawable(R.drawable.dropbox))
                        .setTextPrimary(getString(R.string.dropbox_setup_title))
        );

        drawer.addItem(new DrawerItem()
                        .setId(6)
                        .setImage(getResources().getDrawable(R.drawable.email))
                        .setTextPrimary(getString(R.string.autoemail_title))
        );

        drawer.addItem(new DrawerItem()
                        .setId(7)
                        .setImage(getResources().getDrawable(R.drawable.ftp))
                        .setTextPrimary(getString(R.string.autoftp_setup_title))
        );

        drawer.addItem(new DrawerItem()
                        .setId(8)
                        .setImage(getResources().getDrawable(R.drawable.opengts))
                        .setTextPrimary(getString(R.string.opengts_setup_title))
        );

        drawer.addItem(new DrawerItem()
                        .setId(9)
                        .setImage(getResources().getDrawable(R.drawable.openstreetmap))
                        .setTextPrimary(getString(R.string.osm_setup_title))
        );

        drawer.addDivider();

        drawer.addItem(new DrawerItem()
                        .setId(10)
                        .setImage(getResources().getDrawable(R.drawable.helpfaq))
                        .setTextPrimary(getString(R.string.menu_faq))
        );

        drawer.addItem(new DrawerItem()
                        .setId(11)
                        .setImage(getResources().getDrawable(R.drawable.exit))
                        .setTextPrimary(getString(R.string.menu_exit)));

        //drawer.selectItem(3);

        drawer.setOnItemClickListener(new DrawerItem.OnItemClickListener() {
            @Override
            public void onClick(DrawerItem drawerItem, int id, int position) {
                //drawer.selectItem(3);
                drawerLayout.closeDrawer(drawer);

                switch(id){
                    case 0:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.GENERAL);
                        break;
                    case 1:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.LOGGING);
                        break;
                    case 2:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.PERFORMANCE);
                        break;
                    case 3:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.UPLOAD);
                        break;
                    case 4:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.GDOCS);
                        break;
                    case 5:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.DROPBOX);
                        break;
                    case 6:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.EMAIL);
                        break;
                    case 7:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.FTP);
                        break;
                    case 8:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.OPENGTS);
                        break;
                    case 9:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.OSM);
                        break;
                    case 10:
                        Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                        startActivity(faqtivity);
                        break;
                    case 11:
                        EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                        finish();
                        break;
                }
            }
        });

        ImageButton helpButton = (ImageButton) findViewById(R.id.imgHelp);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                startActivity(faqtivity);
            }
        });

    }

    public void ToggleDrawer(){
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
        else {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    private int GetUserSelectedNavigationItem(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sp.getInt("SPINNER_SELECTED_POSITION", 0);
    }

    private void LoadDefaultFragmentView() {
        int currentSelectedPosition = GetUserSelectedNavigationItem();
        LoadFragmentView(currentSelectedPosition);
    }

    private void LoadFragmentView(int position){
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

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
    }

    private GenericViewFragment GetCurrentFragment(){
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            return ((GenericViewFragment) currentFragment);
        }
        return null;
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("SPINNER_SELECTED_POSITION", position);
        editor.apply();

        LoadFragmentView(position);
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Toolbar toolbarBottom = (Toolbar) findViewById(R.id.toolbarBottom);

        if(toolbarBottom.getMenu().size() > 0){ return true;}

        toolbarBottom.inflateMenu(R.menu.gps_main);
        setupEvenlyDistributedToolbar();
        toolbarBottom.setOnMenuItemClickListener(this);

        enableDisableMenuItems();
        return true;
    }

    public void setupEvenlyDistributedToolbar(){
        //http://stackoverflow.com/questions/26489079/evenly-spaced-menu-items-on-toolbar

        // Use Display metrics to get Screen Dimensions
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarBottom);

        // Add 10 spacing on either side of the toolbar
        toolbar.setContentInsetsAbsolute(10, 10);

        // Get the ChildCount of your Toolbar, this should only be 1
        int childCount = toolbar.getChildCount();
        // Get the Screen Width in pixels
        int screenWidth = metrics.widthPixels;

        // Create the Toolbar Params based on the screenWidth
        Toolbar.LayoutParams toolbarParams = new Toolbar.LayoutParams(screenWidth, Toolbar.LayoutParams.WRAP_CONTENT);

        // Loop through the child Items
        for(int i = 0; i < childCount; i++){
            // Get the item at the current index
            View childView = toolbar.getChildAt(i);
            // If its a ViewGroup
            if(childView instanceof ViewGroup){
                // Set its layout params
                childView.setLayoutParams(toolbarParams);
                // Get the child count of this view group, and compute the item widths based on this count & screen size
                int innerChildCount = ((ViewGroup) childView).getChildCount();
                int itemWidth  = (screenWidth / innerChildCount);
                // Create layout params for the ActionMenuView
                ActionMenuView.LayoutParams params = new ActionMenuView.LayoutParams(itemWidth, Toolbar.LayoutParams.WRAP_CONTENT);
                // Loop through the children
                for(int j = 0; j < innerChildCount; j++){
                    View grandChild = ((ViewGroup) childView).getChildAt(j);
                    if(grandChild instanceof ActionMenuItemView){
                        // set the layout parameters on each View
                        grandChild.setLayoutParams(params);
                    }
                }
            }
        }
    }

    private void enableDisableMenuItems() {

        OnWaitingForLocation(Session.isWaitingForLocation());
        SetBulbStatus(Session.isStarted());

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbarBottom);
        MenuItem mnuAnnotate = toolbar.getMenu().findItem(R.id.mnuAnnotate);
        MenuItem mnuOnePoint = toolbar.getMenu().findItem(R.id.mnuOnePoint);
        MenuItem mnuAutoSendNow = toolbar.getMenu().findItem(R.id.mnuAutoSendNow);

        if (mnuOnePoint != null) {
            mnuOnePoint.setEnabled(!Session.isStarted());
            mnuOnePoint.setIcon((Session.isStarted() ? R.drawable.singlepoint_disabled : R.drawable.singlepoint));
        }

        if (mnuAutoSendNow != null) {
            mnuAutoSendNow.setEnabled(Session.isStarted());
        }

        if (mnuAnnotate != null) {

            if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl()) {
                mnuAnnotate.setIcon(R.drawable.annotate2_disabled);
                mnuAnnotate.setEnabled(false);
            } else {
                if (Session.isAnnotationMarked()) {
                    mnuAnnotate.setIcon(R.drawable.annotate2_active);
                } else {
                    mnuAnnotate.setIcon(R.drawable.annotate2);
                }
            }

        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
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
                return true;
        }
    }


    private void ForceAutoSendNow() {
        tracer.debug("User forced an auto send");

        if (AppSettings.isAutoSendEnabled()) {
            Utilities.ShowProgress(this, getString(R.string.autosend_sending),getString(R.string.please_wait));
            EventBus.getDefault().post(new CommandEvents.AutoSend(null));

        } else {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.UPLOAD);
        }
    }

    private void LogSinglePoint() {
        EventBus.getDefault().post(new CommandEvents.LogOnce());
        enableDisableMenuItems();
    }

    /**
     * Annotates GPX and KML files, TXT files are ignored.
     * The user is prompted for the content of the <name> tag. If a valid
     * description is given, the logging service starts in single point mode.
     */
    private void Annotate() {

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml() && !AppSettings.shouldLogToCustomUrl()) {
            Toast.makeText(getApplicationContext(), getString(R.string.annotation_requires_logging), Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog alertDialog = new MaterialDialog.Builder(GpsMainActivity.this)
                .title(R.string.add_description)
                .customView(R.layout.alertview, true)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        EditText userInput = (EditText) dialog.getCustomView().findViewById(R.id.alert_user_input);
                        EventBus.getDefault().postSticky(new CommandEvents.Annotate(userInput.getText().toString()));
                    }
                }).build();

        EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
        userInput.setText(Session.getDescription());
        TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
        tvMessage.setText(R.string.letters_numbers);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }


    private void UploadToOpenStreetMap() {
        if (!OSMHelper.IsOsmAuthorized(getApplicationContext())) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.OSM);
            return;
        }

        ShowFileListDialog(FileSenderFactory.GetOsmSender(getApplicationContext()));
    }

    private void UploadToDropBox() {
        final DropBoxHelper dropBoxHelper = new DropBoxHelper(getApplicationContext());

        if (!dropBoxHelper.IsLinked()) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.DROPBOX);
            return;
        }

        ShowFileListDialog(FileSenderFactory.GetDropBoxSender(getApplication()));
    }

    private void SendToOpenGTS() {
        if (!Utilities.IsOpenGTSSetup()) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.OPENGTS);
        } else {
            IFileSender fs = FileSenderFactory.GetOpenGTSSender(getApplicationContext());
            ShowFileListDialog(fs);
        }
    }

    private void UploadToGoogleDocs() {
        if (!GDocsHelper.IsLinked(getApplicationContext())) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.GDOCS);
            return;
        }

        ShowFileListDialog(FileSenderFactory.GetGDocsSender(getApplicationContext()));
    }

    private void SendToFtp() {
        if (!Utilities.IsFtpSetup()) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.FTP);
        } else {
            IFileSender fs = FileSenderFactory.GetFtpSender(getApplicationContext());
            ShowFileListDialog(fs);
        }
    }

    private void SelectAndEmailFile() {
        if (!Utilities.IsEmailSetup()) {
            LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.EMAIL);
        } else {
            ShowFileListDialog(FileSenderFactory.GetEmailSender(this));
        }
    }

    private void ShowFileListDialog(final IFileSender sender) {

        if (!Utilities.isNetworkAvailable(this)) {
            Utilities.MsgBox(getString(R.string.sorry),getString(R.string.no_network_message), this);
            return;
        }

        final File gpxFolder = new File(AppSettings.getGpsLoggerFolder());

        if (gpxFolder != null && gpxFolder.exists() && Utilities.GetFilesInFolder(gpxFolder, sender).length > 0) {
            File[] enumeratedFiles = Utilities.GetFilesInFolder(gpxFolder, sender);

            //Order by last modified
            Arrays.sort(enumeratedFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    if (f1 != null && f2 != null) {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                    return -1;
                }
            });

            List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

            for (File f : enumeratedFiles) {
                fileList.add(f.getName());
            }

            final String[] files = fileList.toArray(new String[fileList.size()]);

            new MaterialDialog.Builder(this)
                    .title(R.string.osm_pick_file)
                    .items(files)
                    .positiveText(R.string.ok)
                    .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            List<Integer> selectedItems = Arrays.asList(integers);

                            List<File> chosenFiles = new ArrayList<File>();

                            for (Object item : selectedItems) {
                                tracer.info("Selected file to upload- " + files[Integer.valueOf(item.toString())]);
                                chosenFiles.add(new File(gpxFolder, files[Integer.valueOf(item.toString())]));
                            }

                            if (chosenFiles.size() > 0) {
                                Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.please_wait),
                                        getString(R.string.please_wait));
                                sender.UploadFile(chosenFiles);
                            }
                            return true;
                        }
                    }).show();

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

                new MaterialDialog.Builder(this)
                        .title(R.string.osm_pick_file)
                        .items(files)
                        .positiveText(R.string.ok)
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                                List<Integer> selectedItems = Arrays.asList(integers);

                                final Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("*/*");

                                if (selectedItems.size() <= 0) {
                                    return false;
                                }

                                if (selectedItems.contains(0)) {

                                    intent.setType("text/plain");

                                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_mylocation));
                                    if (Session.hasValidLocation()) {
                                        String bodyText = getString(R.string.sharing_googlemaps_link,
                                                String.valueOf(Session.getCurrentLatitude()),
                                                String.valueOf(Session.getCurrentLongitude()));
                                        intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                                        intent.putExtra("sms_body", bodyText);
                                        startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                                    }
                                } else {

                                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
                                    intent.setType("*/*");

                                    ArrayList<Uri> chosenFiles = new ArrayList<Uri>();

                                    for (Object path : selectedItems) {
                                        File file = new File(gpxFolder, files[Integer.valueOf(path.toString())]);
                                        Uri uri = Uri.fromFile(file);
                                        chosenFiles.add(uri);
                                    }

                                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, chosenFiles);
                                    startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                                }
                                return true;
                            }
                        }).show();


            } else {
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
            }
        } catch (Exception ex) {
            tracer.error("Sharing problem", ex);
        }
    }


    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            tracer.debug("Disconnected from GPSLoggingService from MainActivity");
            //loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            tracer.debug("Connected to GPSLoggingService from MainActivity");
            //loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
        }
    };


    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService() {
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
        if (Session.isBoundToService()) {

            try {
                unbindService(gpsServiceConnection);
                Session.setBoundToService(false);
            } catch (Exception e) {
                tracer.error("Could not unbind service", e);
            }
        }

        if (!Session.isStarted()) {
            tracer.debug("Stopping the service");
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                tracer.error("Could not stop the service", e);
            }
        }
    }

    private void SetBulbStatus(boolean started) {
        ImageView bulb = (ImageView) findViewById(R.id.notification_bulb);
        bulb.setImageResource(started ? R.drawable.circle_green : R.drawable.circle_none);
    }

    public void SetAnnotationReady() {
        Session.setAnnotationMarked(true);
        enableDisableMenuItems();
    }

    public void SetAnnotationDone() {
        Session.setAnnotationMarked(false);
        enableDisableMenuItems();
    }

    public void OnWaitingForLocation(boolean inProgress) {
        ProgressBar fixBar = (ProgressBar) findViewById(R.id.progressBarGpsFix);
        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
    }


    private void GetPreferences() {
        Utilities.PopulateAppSettings(getApplicationContext());
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenGTS upload){
        tracer.debug("Open GTS Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.AutoEmail upload){
        tracer.debug("Auto Email Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenStreetMap upload){
        tracer.debug("OSM Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Dropbox upload){
        tracer.debug("Dropbox Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GDocs upload){
        tracer.debug("GDocs Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp upload){
        tracer.debug("FTP Event completed, success: " + upload.success);
        Utilities.HideProgress();
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        OnWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus){
        if(annotationStatus.annotationWritten){
            SetAnnotationDone();
        }
        else {
            SetAnnotationReady();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
            enableDisableMenuItems();
    }
}

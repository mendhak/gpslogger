/*
 * Copyright (C) 2016 mendhak
 *
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
 */

package com.mendhak.gpslogger;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amulyakhare.textdrawable.TextDrawable;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ProfileEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.loggers.Streams;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.GpsLoggerDrawerItem;
import com.mendhak.gpslogger.ui.fragments.display.*;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class GpsMainActivity extends AppCompatActivity
        implements
        Toolbar.OnMenuItemClickListener,
        ActionBar.OnNavigationListener {

    private static boolean userInvokedUpload;
    private static Intent serviceIntent;
    private ActionBarDrawerToggle drawerToggle;
    private static final Logger LOG = Logs.of(GpsMainActivity.class);

    Drawer materialDrawer;
    AccountHeader drawerHeader;
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private Session session = Session.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadPresetProperties();
        loadVersionSpecificProperties();
        Systems.setLocale(preferenceHelper.getUserSpecifiedLocale(), getBaseContext(),getResources());

        setContentView(R.layout.activity_gps_main);

        setUpToolbar();
        setUpNavigationDrawer(savedInstanceState);

        loadDefaultFragmentView();
        startAndBindService();
        registerEventBus();

        if(!Systems.hasUserGrantedAllNecessaryPermissions(this)){
            Systems.askUserForPermissions(this, null);
        }
        else {
            LOG.debug("Permission check OK");

            if(preferenceHelper.shouldStartLoggingOnAppLaunch()){
                LOG.debug("Start logging on app launch");
                EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Systems.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        //Save the drawer's selected values to bundle
        //useful if activity recreated due to rotation
        outState = materialDrawer.saveInstanceState(outState);
        outState = drawerHeader.saveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
        EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAndBindService();
    }



    @Override
    protected void onResume() {
        super.onResume();
        startAndBindService();

        if (session.hasDescription()) {
            setAnnotationReady();
        }

        populateProfilesList();
        enableDisableMenuItems();


    }

    @Override
    protected void onPause() {
        stopAndUnbindServiceIfRequired();
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
        if (!isFinishing()) {
            stopAndUnbindServiceIfRequired();
        }
    }

    @Override
    protected void onDestroy() {
        stopAndUnbindServiceIfRequired();
        unregisterEventBus();
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
            toggleDrawer();
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && session.isBoundToService()) {
            stopAndUnbindServiceIfRequired();
        }

        if(keyCode == KeyEvent.KEYCODE_BACK){
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
                toggleDrawer();
                return true;
            }

            removeFragmentsAndActionBar();
            finish();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void removeFragmentsAndActionBar(){
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(getCurrentFragment());
        transaction.commit();
        getSupportActionBar().hide();
    }





    private void loadVersionSpecificProperties(){
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionCode = packageInfo.versionCode;

            if(preferenceHelper.getLastVersionSeen() <= 74){
                LOG.debug("preferenceHelper.getLastVersionSeen() " + preferenceHelper.getLastVersionSeen());
                LOG.debug("Overriding minimum accuracy to 40");

                if(preferenceHelper.getMinimumAccuracy() == 0){
                    preferenceHelper.setMinimumAccuracy(40);
                }
            }

            preferenceHelper.setLastVersionSeen(versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void loadPresetProperties() {

        //Either look for /<appfolder>/gpslogger.properties or /sdcard/gpslogger.properties
        File file =  new File(Files.storageFolder(getApplicationContext()) + "/gpslogger.properties");
        if(!file.exists()){
            file = new File(Environment.getExternalStorageDirectory() + "/gpslogger.properties");
            if(!file.exists()){
                return;
            }
        }

        try {
            LOG.warn("gpslogger.properties found, setting app preferences");
            preferenceHelper.setPreferenceFromPropertiesFile(file);
        } catch (Exception e) {
            LOG.error("Could not load preset properties", e);
        }
    }


    /**
     * Helper method, launches activity in a delayed handler, less stutter
     */
    private void launchPreferenceScreen(final String whichFragment) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent targetActivity = new Intent(getApplicationContext(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", whichFragment);
                startActivity(targetActivity);
            }
        }, 250);
    }



    public Toolbar getToolbar(){
        return (Toolbar)findViewById(R.id.toolbar);
    }

    @SuppressWarnings("deprecation")
    public void setUpToolbar(){
        try{
            Toolbar toolbar = getToolbar();
            setSupportActionBar(toolbar);
            if(getSupportActionBar() != null){
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }


            //Deprecated in Lollipop but required if targeting 4.x
            SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.gps_main_views, R.layout.spinner_dropdown_item);
            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, this);
            getSupportActionBar().setSelectedNavigationItem(getUserSelectedNavigationItem());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
        }
        catch(Exception ex){
            //http://stackoverflow.com/questions/26657348/appcompat-v7-v21-0-0-causing-crash-on-samsung-devices-with-android-v4-2-2
            LOG.error("Thanks for this, Samsung", ex);
        }

    }

    public void setUpNavigationDrawer(Bundle savedInstanceState) {

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                getToolbar(),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };


        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withCompactStyle(true)
                .withAccountHeader(R.layout.smaller_header)
                .withSavedInstance(savedInstanceState)
                .withProfileImagesVisible(false)
                .withHeaderBackground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.accentColor)))
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {

                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {

                        //Add new profile
                        if (profile.getIdentifier() == 101) {
                            new MaterialDialog.Builder(GpsMainActivity.this)
                                    .title(getString(R.string.profile_create_new))
                                    .inputType(InputType.TYPE_CLASS_TEXT)
                                    .negativeText(R.string.cancel)
                                    .input("", "", false, new MaterialDialog.InputCallback() {
                                        @Override
                                        public void onInput(@NonNull MaterialDialog materialDialog, CharSequence charSequence) {
                                            String profileName = charSequence.toString().trim();
                                            if(!Strings.isNullOrEmpty(profileName)){
                                                final String[] ReservedChars = {"|", "\\", "?", "*", "<", "\"", ":", ">", ".", "/", "'", ";"};

                                                for (String c : ReservedChars) {
                                                    profileName = profileName.replace(c,"");
                                                }

                                                EventBus.getDefault().post(new ProfileEvents.CreateNewProfile(profileName));
                                            }
                                        }
                                    })
                                    .show();
                            return true;
                        }

                        if (profile.getIdentifier() == 102) {
                            new MaterialDialog.Builder(GpsMainActivity.this)
                                    .title("Properties file URL")
                                    .inputType(InputType.TYPE_CLASS_TEXT)
                                    .negativeText(R.string.cancel)
                                    .input("", "", false, new MaterialDialog.InputCallback() {
                                        @Override
                                        public void onInput(@NonNull MaterialDialog materialDialog, CharSequence charSequence) {

                                            EventBus.getDefault().post(new ProfileEvents.DownloadProfile(charSequence.toString()));
                                            Dialogs.progress(GpsMainActivity.this,getString(R.string.please_wait),getString(R.string.please_wait));

                                        }
                                    })
                                    .show();
                            return true;
                        }

                        //Clicked on profile name
                        String newProfileName = profile.getName().getText();
                        EventBus.getDefault().post(new ProfileEvents.SwitchToProfile(newProfileName));
                        refreshProfileIcon(profile.getName().getText());
                        return true;
                    }
                })
                .withOnAccountHeaderItemLongClickListener(new AccountHeader.OnAccountHeaderItemLongClickListener() {
                    @Override
                    public boolean onProfileLongClick(View view, final IProfile iProfile, boolean b) {
                        if (iProfile.getIdentifier() > 150 ) {

                            if( preferenceHelper.getCurrentProfileName().equals(iProfile.getName().getText()) ){
                                Dialogs.alert(getString(R.string.sorry), getString(R.string.profile_switch_before_delete), GpsMainActivity.this);
                            }
                            else {
                                new MaterialDialog.Builder(GpsMainActivity.this)
                                        .title(getString(R.string.profile_delete))
                                        .content(iProfile.getName().getText())
                                        .positiveText(R.string.ok)
                                        .negativeText(R.string.cancel)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                                EventBus.getDefault().post(new ProfileEvents.DeleteProfile(iProfile.getName().getText()));
                                            }
                                        })
                                        .show();
                            }
                        }
                        return false;
                    }
                })
                .build();


        populateProfilesList();


        materialDrawer = new DrawerBuilder()
                .withActivity(this)
                .withSavedInstance(savedInstanceState)
                .withToolbar(getToolbar())
                .withActionBarDrawerToggle(drawerToggle)
                .withDrawerGravity(Gravity.LEFT)
                .withAccountHeader(drawerHeader)
                .withSelectedItem(-1)
                .build();



        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.pref_general_title, R.string.pref_general_summary, R.drawable.settings, 1000));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.pref_logging_title, R.string.pref_logging_summary, R.drawable.loggingsettings, 1001));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.pref_performance_title, R.string.pref_performance_summary, R.drawable.performance, 1002));
        materialDrawer.addItem(new DividerDrawerItem());

        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.pref_autosend_title, R.string.pref_autosend_summary, R.drawable.autosend, 1003));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.gdocs_setup_title, R.drawable.googledrive, 1004));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.dropbox_setup_title, R.drawable.dropbox, 1005));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.sftp_setup_title, R.drawable.sftp, 1015));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.opengts_setup_title, R.drawable.opengts, 1008));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.osm_setup_title, R.drawable.openstreetmap, 1009));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.autoemail_title, R.drawable.email, 1006));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.owncloud_setup_title, R.drawable.owncloud, 1010));
        materialDrawer.addItem(GpsLoggerDrawerItem.newSecondary(R.string.autoftp_setup_title, R.drawable.ftp, 1007));

        materialDrawer.addItem(new DividerDrawerItem());

        materialDrawer.addStickyFooterItem(GpsLoggerDrawerItem.newSecondary(R.string.menu_faq, R.drawable.helpfaq, 9000));
        materialDrawer.addStickyFooterItem(GpsLoggerDrawerItem.newSecondary(R.string.menu_exit, R.drawable.exit, 9001));


        materialDrawer.setOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int i, IDrawerItem iDrawerItem) {

                switch (iDrawerItem.getIdentifier()) {
                    case 1000:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GENERAL);
                        break;
                    case 1001:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.LOGGING);
                        break;
                    case 1002:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.PERFORMANCE);
                        break;
                    case 1003:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.UPLOAD);
                        break;
                    case 1004:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GDOCS);
                        break;
                    case 1005:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.DROPBOX);
                        break;
                    case 1006:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.EMAIL);
                        break;
                    case 1007:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP);
                        break;
                    case 1008:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                        break;
                    case 1009:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OSM);
                        break;
                    case 1010:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OWNCLOUD);
                        break;
                    case 1015:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP);
                        break;
                    case 9000:
                        Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                        startActivity(faqtivity);
                        break;
                    case 9001:
                        EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                        finish();
                        break;

                }
                return false;
            }
        });

    }

    private void refreshProfileIcon(String profileName){

        ImageView imgLetter = (ImageView)drawerHeader.getView().findViewById(R.id.profiletextletter);
        TextDrawable drawLetter = TextDrawable.builder()
                .beginConfig()
                .bold()
                .textColor(ContextCompat.getColor(getApplicationContext(),R.color.golden))
                .useFont(Typeface.SANS_SERIF)
                .endConfig()
                .buildRound(profileName.substring(0, 1).toUpperCase(), ContextCompat.getColor(getApplicationContext(), R.color.primaryColorLight));

        imgLetter.setImageDrawable(drawLetter);
    }

    private void populateProfilesList() {

        LOG.debug("Current profile:" + preferenceHelper.getCurrentProfileName());

        drawerHeader.clear();

        drawerHeader.addProfiles(
                new ProfileDrawerItem()
                        .withName(getString(R.string.profile_default))
                        .withIdentifier(100)
                        .withTag("PROFILE_DEFAULT")
                        .withTextColorRes(R.color.primaryColorText)
                ,
                new ProfileSettingDrawerItem()
                        .withIcon(R.drawable.library_plus)
                        .withIdentifier(101)
                        .withName(getString(R.string.profile_add_new))
                        .withTag("PROFILE_ADD")
                        .withTextColorRes(R.color.primaryColorText)
                ,
                new ProfileSettingDrawerItem()
                        .withIcon(R.drawable.download_outline)
                        .withIdentifier(102)
                        .withName("From URL")
                        .withTag("PROFILE_URL")
                        .withTextColorRes(R.color.primaryColorText)

        );


        File gpsLoggerDir = Files.storageFolder(this);
        File[] propertyFiles = gpsLoggerDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) {
                return fileName.endsWith(".properties") && !fileName.equalsIgnoreCase(getString(R.string.profile_default) + ".properties");
            }
        });

        if(propertyFiles == null){
            return;
        }

        for(File propertyFile: propertyFiles){

            String name = propertyFile.getName();
            int pos = name.lastIndexOf(".");
            if (pos > 0) {
                name = name.substring(0, pos);
            }

            ProfileDrawerItem pdi = new ProfileDrawerItem().withName(name).withTextColorRes(R.color.primaryColorText);

            drawerHeader.addProfile(pdi, 1);

            if(name.equals(preferenceHelper.getCurrentProfileName())){
                pdi.withSetSelected(true);
                drawerHeader.setActiveProfile(pdi);
            }

        }

        refreshProfileIcon(preferenceHelper.getCurrentProfileName());


    }

    public void toggleDrawer(){
        if(materialDrawer.isDrawerOpen()){
            materialDrawer.closeDrawer();

        }
        else {
            materialDrawer.openDrawer();
        }
    }

    private int getUserSelectedNavigationItem(){
        return preferenceHelper.getUserSelectedNavigationItem();
    }

    private void loadDefaultFragmentView() {
        int currentSelectedPosition = getUserSelectedNavigationItem();
        loadFragmentView(currentSelectedPosition);
    }

    private void loadFragmentView(int position){
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        switch (position) {
            default:
            case 0:
                transaction.replace(R.id.container, GpsSimpleViewFragment.newInstance());
                break;
            case 1:
                transaction.replace(R.id.container, GpsDetailedViewFragment.newInstance());
                break;
            case 2:
                transaction.replace(R.id.container, GpsBigViewFragment.newInstance());
                break;
            case 3:
                transaction.replace(R.id.container, GpsLogViewFragment.newInstance());
                break;

        }
        transaction.commitAllowingStateLoss();
    }

    private GenericViewFragment getCurrentFragment(){
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof GenericViewFragment) {
            return ((GenericViewFragment) currentFragment);
        }
        return null;
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        preferenceHelper.setUserSelectedNavigationItem(position);
        loadFragmentView(position);
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

        onWaitingForLocation(session.isWaitingForLocation());
        setBulbStatus(session.isStarted());

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbarBottom);
        MenuItem mnuAnnotate = toolbar.getMenu().findItem(R.id.mnuAnnotate);
        MenuItem mnuOnePoint = toolbar.getMenu().findItem(R.id.mnuOnePoint);
        MenuItem mnuAutoSendNow = toolbar.getMenu().findItem(R.id.mnuAutoSendNow);

        if (mnuOnePoint != null) {
            mnuOnePoint.setEnabled(!session.isStarted());
            mnuOnePoint.setIcon((session.isStarted() ? R.drawable.singlepoint_disabled : R.drawable.singlepoint));
        }

        if (mnuAutoSendNow != null) {
            mnuAutoSendNow.setEnabled(session.isStarted());
        }

        if (mnuAnnotate != null) {

            if (!preferenceHelper.shouldLogToCSV() && !preferenceHelper.shouldLogToGpx()
                    && !preferenceHelper.shouldLogToKml() && !preferenceHelper.shouldLogToCustomUrl()
                    && !preferenceHelper.shouldLogToGeoJSON()) {
                mnuAnnotate.setIcon(R.drawable.annotate2_disabled);
                mnuAnnotate.setEnabled(false);
            }
            else {
                if (session.isAnnotationMarked()) {
                    mnuAnnotate.setIcon(R.drawable.annotate2_active);
                }
                else {
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

        LOG.debug("Menu Item: " + String.valueOf(item.getTitle()));

        switch (id) {
            case R.id.mnuAnnotate:
                annotate();
                return true;
            case R.id.mnuOnePoint:
                logSinglePoint();
                return true;
            case R.id.mnuShare:
                share();
                return true;
            case R.id.mnuOSM:
                uploadToOpenStreetMap();
                return true;
            case R.id.mnuDropBox:
                uploadToDropBox();
                return true;
            case R.id.mnuGDocs:
                uploadToGoogleDocs();
                return true;
            case R.id.mnuOpenGTS:
                sendToOpenGTS();
                return true;
            case R.id.mnuFtp:
                sendToFtp();
                return true;
            case R.id.mnuEmail:
                selectAndEmailFile();
                return true;
            case R.id.mnuAutoSendNow:
                forceAutoSendNow();
            case R.id.mnuOwnCloud:
                uploadToOwnCloud();
                return true;
            case R.id.mnuSFTP:
                uploadToSFTP();
                return true;
            default:
                return true;
        }
    }


    private void forceAutoSendNow() {
        LOG.debug("User forced an auto send");

        if (preferenceHelper.isAutoSendEnabled()) {
            Dialogs.progress(this, getString(R.string.autosend_sending), getString(R.string.please_wait));
            EventBus.getDefault().post(new CommandEvents.AutoSend(null));

        } else {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.UPLOAD);
        }
    }

    private void logSinglePoint() {
        EventBus.getDefault().post(new CommandEvents.LogOnce());
        enableDisableMenuItems();
    }

    /**
     * Annotates GPX and KML files, TXT files are ignored.
     * The user is prompted for the content of the <name> tag. If a valid
     * description is given, the logging service starts in single point mode.
     */
    private void annotate() {

        if (!preferenceHelper.shouldLogToCSV() && !preferenceHelper.shouldLogToGpx() && !preferenceHelper.shouldLogToKml()
                && !preferenceHelper.shouldLogToCustomUrl() && !preferenceHelper.shouldLogToGeoJSON()) {
            Toast.makeText(getApplicationContext(), getString(R.string.annotation_requires_logging), Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialDialog.Builder(this)
                .title(R.string.add_description)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(R.string.cancel)
                .input(getString(R.string.letters_numbers), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence input) {
                        LOG.info("Annotation entered : " + input.toString());
                        EventBus.getDefault().post(new CommandEvents.Annotate(input.toString()));
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .show();

    }


    private void uploadToOpenStreetMap() {
        if (!FileSenderFactory.getOsmSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OSM);
            return;
        }

        showFileListDialog(FileSenderFactory.getOsmSender());
    }

    private void uploadToDropBox() {

        if (!FileSenderFactory.getDropBoxSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.DROPBOX);
            return;
        }

        showFileListDialog(FileSenderFactory.getDropBoxSender());
    }


    private void uploadToSFTP(){
        if(!FileSenderFactory.getSFTPSender().isAvailable()){
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP);
            return;
        }

        showFileListDialog(FileSenderFactory.getSFTPSender());
    }

    private void uploadToOwnCloud() {

        if (!FileSenderFactory.getOwnCloudSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OWNCLOUD);
            return;
        }

        showFileListDialog(FileSenderFactory.getOwnCloudSender());
    }

    private void sendToOpenGTS() {
        if (!FileSenderFactory.getOpenGTSSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
        } else {
            showFileListDialog(FileSenderFactory.getOpenGTSSender());
        }
    }

    private void uploadToGoogleDocs() {
        if (!FileSenderFactory.getGoogleDriveSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GDOCS);
            return;
        }

        showFileListDialog(FileSenderFactory.getGoogleDriveSender());
    }

    private void sendToFtp() {
        if (!FileSenderFactory.getFtpSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP);
        } else {
            showFileListDialog(FileSenderFactory.getFtpSender());
        }
    }

    private void selectAndEmailFile() {
        if (!FileSenderFactory.getEmailSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.EMAIL);
        } else {
            showFileListDialog(FileSenderFactory.getEmailSender());
        }
    }

    private void showFileListDialog(final FileSender sender) {

        if (!Systems.isNetworkAvailable(this)) {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_network_message), this);
            return;
        }

        final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

        if (gpxFolder.exists() && Files.fromFolder(gpxFolder, sender).length > 0) {
            File[] enumeratedFiles = Files.fromFolder(gpxFolder, sender);

            //Order by last modified
            Arrays.sort(enumeratedFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    if (f1 != null && f2 != null) {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                    return -1;
                }
            });

            List<String> fileList = new ArrayList<>(enumeratedFiles.length);

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

                            List<File> chosenFiles = new ArrayList<>();

                            for (Object item : selectedItems) {
                                LOG.info("Selected file to upload- " + files[Integer.parseInt(item.toString())]);
                                chosenFiles.add(new File(gpxFolder, files[Integer.parseInt(item.toString())]));
                            }

                            if (chosenFiles.size() > 0) {
                                Dialogs.progress(GpsMainActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
                                userInvokedUpload = true;
                                sender.uploadFile(chosenFiles);

                            }
                            return true;
                        }
                    }).show();

        } else {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }

    /**
     * Allows user to send a GPX/KML file along with location, or location only
     * using a provider. 'Provider' means any application that can accept such
     * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
     */
    private void share() {

        try {

            final String locationOnly = getString(R.string.sharing_location_only);
            final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
            if (gpxFolder.exists()) {

                File[] enumeratedFiles = Files.fromFolder(gpxFolder);

                Arrays.sort(enumeratedFiles, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                List<String> fileList = new ArrayList<>(enumeratedFiles.length);

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
                                    if (session.hasValidLocation()) {
                                        String bodyText = String.format("http://maps.google.com/maps?q=%s,%s",
                                                String.valueOf(session.getCurrentLatitude()),
                                                String.valueOf(session.getCurrentLongitude()));
                                        intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                                        intent.putExtra("sms_body", bodyText);
                                        startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                                    }
                                } else {

                                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
                                    intent.setType("*/*");

                                    ArrayList<Uri> chosenFiles = new ArrayList<>();

                                    for (Object path : selectedItems) {
                                        File file = new File(gpxFolder, files[Integer.parseInt(path.toString())]);
                                        Uri providedUri = FileProvider.getUriForFile(getApplicationContext(),
                                                "com.mendhak.gpslogger.fileprovider", file);
                                        chosenFiles.add(providedUri);
                                    }

                                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, chosenFiles);
                                    startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                                }
                                return true;
                            }
                        }).show();


            } else {
                Dialogs.alert(getString(R.string.sorry), getString(R.string.no_files_found), this);
            }
        } catch (Exception ex) {
            LOG.error("Sharing problem", ex);
        }
    }


    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            LOG.debug("Disconnected from GPSLoggingService from MainActivity");
            //loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            LOG.debug("Connected to GPSLoggingService from MainActivity");
            //loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
        }
    };


    /**
     * Starts the service and binds the activity to it.
     */
    private void startAndBindService() {
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);

        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void stopAndUnbindServiceIfRequired() {
        if (session.isBoundToService()) {

            try {
                unbindService(gpsServiceConnection);
                session.setBoundToService(false);
            } catch (Exception e) {
                LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Could not unbind service", e);
            }
        }

        if (!session.isStarted()) {
            LOG.debug("Stopping the service");
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                LOG.error("Could not stop the service", e);
            }
        }
    }

    private void setBulbStatus(boolean started) {
        ImageView bulb = (ImageView) findViewById(R.id.notification_bulb);
        bulb.setImageResource(started ? R.drawable.circle_green : R.drawable.circle_none);
    }

    public void setAnnotationReady() {
        session.setAnnotationMarked(true);
        enableDisableMenuItems();
    }

    public void setAnnotationDone() {
        session.setAnnotationMarked(false);
        enableDisableMenuItems();
    }

    public void onWaitingForLocation(boolean inProgress) {
        ProgressBar fixBar = (ProgressBar) findViewById(R.id.progressBarGpsFix);
        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenGTS upload){
        LOG.debug("Open GTS Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.opengts_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));

            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.AutoEmail upload){
        LOG.debug("Auto Email Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.autoemail_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenStreetMap upload){
        LOG.debug("OSM Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.osm_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Dropbox upload){
        LOG.debug("Dropbox Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.dropbox_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GDrive upload){
        LOG.debug("GDrive Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.gdocs_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp upload){
        LOG.debug("FTP Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.autoftp_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.SFTP upload){

        LOG.debug("SFTP Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.sftp_setup_title) + "- " + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.OwnCloud upload){
        LOG.debug("OwnCloud Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.owncloud_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));

            if(userInvokedUpload){
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        onWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus){
        if(annotationStatus.annotationWritten){
            setAnnotationDone();
        }
        else {
            setAnnotationReady();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
            enableDisableMenuItems();
    }

    @EventBusHook
    public void onEventMainThread(ProfileEvents.CreateNewProfile createProfileEvent){

        LOG.debug("Creating profile: " + createProfileEvent.newProfileName);

        try {
            File f = new File(Files.storageFolder(GpsMainActivity.this), createProfileEvent.newProfileName+".properties");
            f.createNewFile();

            populateProfilesList();

        } catch (IOException e) {
            LOG.error("Could not create properties file for new profile ", e);
        }

    }

    @EventBusHook
    public void onEventMainThread(ProfileEvents.PopulateProfiles populateProfileEvent){
        populateProfilesList();
    }

    @EventBusHook
    public void onEventBackgroundThread(ProfileEvents.DownloadProfile downloadProfileEvent){

        LOG.debug("Downloading profile from URL: " + downloadProfileEvent.profileUrl);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadProfileEvent.profileUrl).build();
        try {
            Response response = client.newCall(request).execute();

            if(response.isSuccessful()){
                LOG.debug("Response successful");
                InputStream inputStream = response.body().byteStream();

                File destFile =  new File(Files.storageFolder(getApplicationContext()) + "/test.properties");
                OutputStream outputStream = new FileOutputStream(destFile);
                Streams.copyIntoStream(inputStream, outputStream);
                response.body().close();

                LOG.debug("Posting to other events");
                EventBus.getDefault().post(new ProfileEvents.SwitchToProfile("test"));
                EventBus.getDefault().post(new ProfileEvents.PopulateProfiles());
                Dialogs.hideProgress();

            }



        } catch (IOException e) {
            LOG.error("Could not download properties file", e);
        }


    }

    @EventBusHook
    public void onEventMainThread(ProfileEvents.DeleteProfile deleteProfileEvent){
        LOG.debug("Deleting profile: " + deleteProfileEvent.profileName);
        File f = new File(Files.storageFolder(GpsMainActivity.this), deleteProfileEvent.profileName+".properties");
        f.delete();

        populateProfilesList();
    }
}

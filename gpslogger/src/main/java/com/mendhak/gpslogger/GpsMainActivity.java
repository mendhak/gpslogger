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


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.amulyakhare.textdrawable.TextDrawable;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ProfileEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.ConscryptProviderInstaller;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.GpsLoggerDrawerItem;
import com.mendhak.gpslogger.ui.fragments.display.AnnotationViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GenericViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsBigViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsDetailedViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsLogViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsSimpleViewFragment;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

public class GpsMainActivity extends AppCompatActivity
        implements
        Toolbar.OnMenuItemClickListener,
        SimpleDialog.OnDialogResultListener,
        ActionBar.OnNavigationListener {

    private static boolean userInvokedUpload;
    private static Intent serviceIntent;
    private ActionBarDrawerToggle drawerToggle;
    private static final Logger LOG = Logs.of(GpsMainActivity.class);

    Drawer materialDrawer;
    AccountHeader drawerHeader;
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private Session session = Session.getInstance();

    // Flag to prevent the service from starting in case we're going through a permission workflow
    // This is required because the service needs to start and show a notification, but the
    // permission workflow causes the service to stop and start multiple times.
    private boolean permissionWorkflowInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadPresetProperties();
        Systems.setLocale(preferenceHelper.getUserSpecifiedLocale(), getBaseContext(),getResources());

        setContentView(R.layout.activity_gps_main);

        setUpToolbar();
        setUpNavigationDrawer(savedInstanceState);

        loadDefaultFragmentView();


        if(!Systems.hasUserGrantedAllNecessaryPermissions(this)){
            LOG.debug("Permission check - missing permissions");
            permissionWorkflowInProgress = true;
            askUserForBasicPermissions();
        }
        else {
            LOG.debug("Permission check - OK");

            startAndBindService();
            registerEventBus();
            registerConscryptProvider();

            if(preferenceHelper.shouldStartLoggingOnAppLaunch()){
                LOG.debug("Start logging on app launch");
                EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
            }

            if(preferenceHelper.shouldStopLoggingOnAppLaunch()){
                LOG.debug("Stop logging on app launch");
                EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(false));
                logSinglePoint();
            }
        }
    }

    private final ActivityResultLauncher<String> backgroundPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    grantResults -> {
                        LOG.debug("Launcher result: " + grantResults.toString());
                        if (grantResults) {
                            LOG.debug("Background permissions granted. Now request ignoring battery optimizations");
                            askUserToDisableBatteryOptimization();
                        } else {
                            LOG.warn("Background location permission was not granted");
                            Dialogs.alert(getString(R.string.gpslogger_permissions_rationale_title), getString(R.string.gpslogger_permissions_permanently_denied), this);
                            permissionWorkflowInProgress=false;
                        }
                    });

    private final ActivityResultLauncher<String[]> basicPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    grantResults -> {
                        LOG.debug("Launcher result: " + grantResults.toString());
                        if (grantResults.containsValue(false)) {
                            LOG.warn("At least one of the permissions was not granted");
                            Dialogs.alert(getString(R.string.gpslogger_permissions_rationale_title), getString(R.string.gpslogger_permissions_permanently_denied), this);
                            permissionWorkflowInProgress=false;
                        } else {
                            LOG.debug("Basic permissions granted. Now ask for background location permissions.");
                            askUserForBackgroundPermissions();
                        }
                    });

    private final ActivityResultLauncher<Intent> batteryOptimizationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    LOG.debug(String.valueOf(result.getResultCode()));
                    String packageName = getPackageName();
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!pm.isIgnoringBatteryOptimizations(packageName)){
    //                    if(result.getResultCode() != Activity.RESULT_OK){
                            LOG.warn("Request to ignore battery optimization was denied.");
                        }
                        else {
                            LOG.debug("Request to ignore battery optimization was granted.");
                        }
                    }
                    permissionWorkflowInProgress=false;
                }
            });

    public void askUserForBasicPermissions() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SimpleDialog.build()
                    .title(getString(R.string.gpslogger_permissions_rationale_title))
                    .msgHtml(getString(R.string.gpslogger_permissions_rationale_message_basic)
                            + "<br />" + getString(R.string.gpslogger_permissions_rationale_message_location)
                            + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?  "<br />" + getString(R.string.gpslogger_permissions_rationale_message_notification) : "")
                            + "<br />" + getString(R.string.gpslogger_permissions_rationale_message_storage)
                            + "<br />" + getString(R.string.gpslogger_permissions_rationale_message_location_background)
                            + "<br />" + getString(R.string.gpslogger_permissions_rationale_message_battery_optimization)
                            )
                    .neut(getString(R.string.privacy_policy))
                    .cancelable(false)
                    .show(this, "PERMISSIONS_START");

        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        LOG.debug(dialogTag);
        if (dialogTag.equalsIgnoreCase("PERMISSIONS_START")){
            switch(which){
                case BUTTON_NEUTRAL:
                    String url = "https://gpslogger.app/privacypolicy.html";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return true;
                case BUTTON_POSITIVE:
                    LOG.debug("Beginning request for multiple permissions");
                    ArrayList<String> permissions = new ArrayList<String>(Systems.getListOfNecessaryPermissions(true));
                    basicPermissionsLauncher.launch(permissions.toArray(new String[0]));
                    return true;
            }
            return true;
        }

        if(dialogTag.equalsIgnoreCase("BACKGROUND_LOCATION") && which == BUTTON_POSITIVE){
            LOG.debug("Beginning request for Background Location permission");
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            return true;
        }

        if(dialogTag.equalsIgnoreCase("NEW_PROFILE_NAME") && which == BUTTON_POSITIVE){

            String profileName = extras.getString("NEW_PROFILE_NAME");
            if(!Strings.isNullOrEmpty(profileName)) {
                final String[] ReservedChars = {"|", "\\", "?", "*", "<", "\"", ":", ">", ".", "/", "'", ";"};
                for (String c : ReservedChars) {
                    profileName = profileName.replace(c,"");
                }
                EventBus.getDefault().post(new ProfileEvents.CreateNewProfile(profileName));
            }
            return true;
        }

        if(dialogTag.equalsIgnoreCase("PROFILE_DELETE") && which == BUTTON_POSITIVE){
            String profileName = extras.getString("PROFILE_DELETE");
            EventBus.getDefault().post(new ProfileEvents.DeleteProfile(profileName));
            return true;
        }

        if(dialogTag.equalsIgnoreCase("PROFILE_DOWNLOAD_URL") && which == BUTTON_POSITIVE){
            String profileDownloadUrl = extras.getString("PROFILE_DOWNLOAD_URL");
            EventBus.getDefault().post(new ProfileEvents.DownloadProfile(profileDownloadUrl));
            Dialogs.progress(GpsMainActivity.this,getString(R.string.please_wait));
            return true;
        }

        if(dialogTag.equalsIgnoreCase("annotations") && which == BUTTON_POSITIVE){
            String enteredText = extras.getString("annotations");
            //Replace all whitespace and newlines, with single space
            enteredText = enteredText.replaceAll("\\s+"," ");
            LOG.info("Annotation entered : " + enteredText);
            EventBus.getDefault().post(new CommandEvents.Annotate(enteredText));
            Files.addItemToCacheFile(enteredText, "annotations", GpsMainActivity.this);
            return true;
        }

        if(dialogTag.equalsIgnoreCase("OSM_FILE_UPLOAD_DIALOG") && which == BUTTON_POSITIVE){
            //As a special case.  For OSM, let user set the description, tags, before passing details along to the actual upload step.

            Bundle extra = new Bundle();
            //Pass along the sender name and list of files, from the previous dialog.
            extra.putString("SENDER_NAME", extras.getString("SENDER_NAME"));
            extra.putStringArrayList(SimpleListDialog.SELECTED_LABELS, extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS));

            SimpleFormDialog.build()
                    .title(R.string.osm_setup_title)
                    .extra(extra)
                    .fields(Dialogs.getOpenStreetMapFormElementsForDialog(preferenceHelper).toArray(new FormElement[0]))
                    .pos(R.string.ok)
                    .show(this, "FILE_UPLOAD_DIALOG");
            return true;
        }

        if(dialogTag.equalsIgnoreCase("FILE_UPLOAD_DIALOG") && which == BUTTON_POSITIVE){

            String senderName = extras.getString("SENDER_NAME");

            //As a special case, if it's an OpenStreetMap upload, save the preferences before uploading
            setOpenStreetMapPreferencesFromDialogPrompt(extras);

            final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
            List<File> chosenFiles = new ArrayList<>();
            ArrayList<String> selectedItems = extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS);
            for (String item : selectedItems) {
                LOG.info("Selected file to upload- " + item);
                chosenFiles.add(new File(gpxFolder, item));
            }
            LOG.info("Using sender: " + senderName);

            if (chosenFiles.size() > 0) {
                Dialogs.progress(GpsMainActivity.this, getString(R.string.please_wait));
                userInvokedUpload = true;
                FileSender sender = FileSenderFactory.getSenderByName(senderName);
                sender.uploadFile(chosenFiles);
            }
            return true;
        }

        if(dialogTag.equalsIgnoreCase("FILE_SHARE_DIALOG") && which == BUTTON_POSITIVE){
            ArrayList<String> selectedItems = extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS);
            if (selectedItems.size() <= 0) {
                return true;
            }

            if(selectedItems.contains(getString(R.string.sharing_location_only))){
                final Intent intent = new Intent(Intent.ACTION_SEND);
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
            }
            else {

                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
                intent.setType("*/*");

                ArrayList<Uri> chosenFiles = new ArrayList<>();
                final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

                for (String path : selectedItems) {
                    File file = new File(gpxFolder, path);
                    Uri providedUri = FileProvider.getUriForFile(getApplicationContext(),
                            "com.mendhak.gpslogger.fileprovider", file);
                    chosenFiles.add(providedUri);
                }

                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, chosenFiles);
                startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
            }
            return true;
        }

        if(dialogTag.equalsIgnoreCase("shouldpromptbeforelogging") && which == BUTTON_POSITIVE){

            if(extras.containsKey(PreferenceNames.CUSTOM_FILE_NAME)){
                String chosenFileName = extras.getString(PreferenceNames.CUSTOM_FILE_NAME);
                if(!preferenceHelper.getCustomFileName().equalsIgnoreCase(chosenFileName)){
                    preferenceHelper.setCustomFileName(chosenFileName);
                    Files.addItemToCacheFile(chosenFileName, PreferenceNames.CUSTOM_FILE_NAME, GpsMainActivity.this);
                }
            }


            setOpenStreetMapPreferencesFromDialogPrompt(extras);

            getCurrentFragment().toggleLogging();
            return true;
        }

        if(dialogTag.equalsIgnoreCase("GPS_PROVIDER_UNAVAILABLE") && which==BUTTON_POSITIVE){

            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            return true;
        }

        return false;
    }

    private void setOpenStreetMapPreferencesFromDialogPrompt(Bundle extras) {
        if(extras.containsKey(PreferenceNames.OPENSTREETMAP_TAGS)){
            String chosenOsmTags = extras.getString(PreferenceNames.OPENSTREETMAP_TAGS);
            if(!preferenceHelper.getOSMTags().equalsIgnoreCase(chosenOsmTags)){
                preferenceHelper.setOSMTags(chosenOsmTags);
            }
        }

        if(extras.containsKey(PreferenceNames.OPENSTREETMAP_DESCRIPTION)){
            String chosenOsmDescription = extras.getString(PreferenceNames.OPENSTREETMAP_DESCRIPTION);
            if(!preferenceHelper.getOSMDescription().equalsIgnoreCase(chosenOsmDescription)){
                preferenceHelper.setOSMDescription(chosenOsmDescription);
            }
        }

        if(extras.containsKey(PreferenceNames.OPENSTREETMAP_VISIBILITY)){
            String chosenOsmVisibility = extras.getString(PreferenceNames.OPENSTREETMAP_VISIBILITY);
            if(!preferenceHelper.getOSMVisibility().equalsIgnoreCase(chosenOsmVisibility)){
                preferenceHelper.setOSMVisibility(chosenOsmVisibility);
            }
        }
    }


    public void askUserForBackgroundPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            SimpleDialog.build()
                    .title(getString(R.string.gpslogger_permissions_rationale_title))
                    .msgHtml(getString(R.string.gpslogger_permissions_background_location) + "<br /><br /><b>" + getPackageManager().getBackgroundPermissionOptionLabel() + "</b>"
                    )
                    .show(this, "BACKGROUND_LOCATION");

        }
        else {
            LOG.debug("Not on Android R, proceed to battery optimization permission request");
            askUserToDisableBatteryOptimization();
        }
    }

    @SuppressLint("BatteryLife")
    public void askUserToDisableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            try {
                if (!pm.isIgnoringBatteryOptimizations(packageName)){
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    batteryOptimizationLauncher.launch(intent);
                }
                else {
                    // On older Android versions, a device might report that it is already ignoring battery optimizations. It's lying.
                    // https://stackoverflow.com/questions/50231908/powermanager-isignoringbatteryoptimizations-always-returns-true-even-if-is-remov
                    // https://issuetracker.google.com/issues/37067894?pli=1
                    LOG.debug("App is already ignoring battery optimization. On some earlier versions of Android this is incorrectly reported, it can only be corrected manually.");
                    permissionWorkflowInProgress=false;
                }
            }
            catch(Exception e){
                LOG.error("Unable to request ignoring battery optimizations.", e);
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        //Save the drawer's selected values to bundle
        //useful if activity recreated due to rotation
        outState = materialDrawer.saveInstanceState(outState);
        outState = drawerHeader.saveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    private void registerConscryptProvider(){
        ConscryptProviderInstaller.installIfNeeded(this);
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

    // Might need this - if the notification keeps reappearing - Issue #933
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
        // !!!!!!In case battery saver mode starts causing crashes.!!!!!!
//        // Adding android:configChanges="uiMode" in AndroidManifest.xml prevents the light/dark mode change
//        // from restarting the Activity. It raises this event instead.
//        // Necessary, because restarting the Activity, with a foreground service was causing a crash.
//        // https://stackoverflow.com/q/44425584/974369
//        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
//        switch (currentNightMode) {
//            case Configuration.UI_MODE_NIGHT_NO:
//            case Configuration.UI_MODE_NIGHT_YES:
//                if(preferenceHelper.getAppThemeSetting().equalsIgnoreCase("system")){
//                    LOG.info("Dark/Light Mode has changed, but will not take effect until the application is reopened.");
//                    Toast.makeText(this, R.string.restart_required, Toast.LENGTH_LONG).show();
//                }
//                break;
//        }

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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(getCurrentFragment());
        transaction.commit();
        getSupportActionBar().hide();
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

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
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
                if(drawerHeader.isSelectionListShown()){
                    drawerHeader.toggleSelectionList(getApplicationContext());
                }
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
                .withCloseDrawerOnProfileListClick(false)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {

                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {

                        //Add new profile
                        if (profile.getIdentifier() == 101) {

                            SimpleFormDialog
                                    .build()
                                    .title(getString(R.string.profile_create_new))
                                    .neg(R.string.cancel)
                                    .pos(R.string.ok)
                                    .fields(
                                            Input.plain("NEW_PROFILE_NAME")
                                                    .required()
                                    ).show(GpsMainActivity.this, "NEW_PROFILE_NAME");

                            return true;
                        }

                        if (profile.getIdentifier() == 102) {

                            SimpleFormDialog
                                    .build()
                                    .title(getString(R.string.properties_file_url))
                                    .neg(R.string.cancel)
                                    .fields(
                                            Input.plain("PROFILE_DOWNLOAD_URL")
                                                    .required()
                                    ).show(GpsMainActivity.this, "PROFILE_DOWNLOAD_URL");

                            return true;
                        }

                        if (profile.getIdentifier() == 103) {
                            EventBus.getDefault().post(new ProfileEvents.SaveProfile());
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

                                Bundle p = new Bundle();
                                p.putString("PROFILE_DELETE", iProfile.getName().getText());

                                SimpleDialog.build()
                                        .title(getString(R.string.profile_delete))
                                        .msg(iProfile.getName().getText())
                                        .pos(R.string.ok)
                                        .neg(R.string.cancel)
                                        .extra(p)
                                        .show(GpsMainActivity.this, "PROFILE_DELETE");
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
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.log_customurl_setup_title, null, R.drawable.customurlsender, 1020));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.dropbox_setup_title, null, R.drawable.dropbox, 1005));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.google_drive_setup_title, null, R.drawable.googledrive, 1011));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.sftp_setup_title, null, R.drawable.sftp, 1015));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.opengts_setup_title, null, R.drawable.opengts, 1008));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.osm_setup_title, null, R.drawable.openstreetmap, 1009));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.autoemail_title, null, R.drawable.email, 1006));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.owncloud_setup_title, null, R.drawable.owncloud, 1010));
        materialDrawer.addItem(GpsLoggerDrawerItem.newPrimary(R.string.autoftp_setup_title, null, R.drawable.ftp, 1007));

        materialDrawer.addItem(new DividerDrawerItem());

        materialDrawer.addStickyFooterItem(GpsLoggerDrawerItem.newPrimary(R.string.menu_faq, null, R.drawable.helpfaq, 9000));
        materialDrawer.addStickyFooterItem(GpsLoggerDrawerItem.newPrimary(R.string.menu_exit, null, R.drawable.exit, 9001));


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
                    case 1011:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GOOGLEDRIVE);
                        break;
                    case 1015:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP);
                        break;
                    case 1020:
                        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
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
                ,
                new ProfileSettingDrawerItem()
                        .withIcon(R.drawable.library_plus)
                        .withIdentifier(101)
                        .withName(getString(R.string.profile_add_new))
                        .withTag("PROFILE_ADD")
                ,
                new ProfileSettingDrawerItem()
                        .withIcon(R.drawable.link_plus)
                        .withIdentifier(102)
                        .withName(getString(R.string.profile_add_from_url))
                        .withTag("PROFILE_URL")
                ,
                new ProfileSettingDrawerItem()
                        .withIcon(R.drawable.download_outline)
                        .withIdentifier(103)
                        .withName(getString(R.string.save))
                        .withTag("PROFILE_SAVE")

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

            ProfileDrawerItem pdi = new ProfileDrawerItem().withName(name);

            if(Systems.isDarkMode(this)){
                pdi.withTextColorRes(R.color.primaryColorLight);
            }

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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

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
            case 4:
                transaction.replace(R.id.container, AnnotationViewFragment.newInstance());
                break;

        }
        transaction.commitAllowingStateLoss();
    }

    private GenericViewFragment getCurrentFragment(){
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            // Apply the insets as a margin to the view so it doesn't overlap with status bar
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            // mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);

            // Alternatively set the padding on the view itself.
            // v.setPadding(0, 0, 0, 0);

            // Return CONSUMED if you don't want want the window insets to keep passing down to descendant views.
            // return windowInsets;
            return WindowInsetsCompat.CONSUMED;
        });

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
        setBulbStatus();

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
            case R.id.mnuGoogleDrive:
                uploadToGoogleDrive();
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
            case R.id.mnuCustomUrl:
                uploadToCustomURL();
                return true;
            default:
                return true;
        }
    }


    private void forceAutoSendNow() {
        LOG.debug("User forced an auto send");

        if (preferenceHelper.isAutoSendEnabled()) {
            Dialogs.progress(this, getString(R.string.autosend_sending));
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


        Dialogs.autoSuggestDialog(GpsMainActivity.this, "annotations",
                getString(R.string.add_description), getString(R.string.letters_numbers), session.getDescription());

    }


    private void uploadToOpenStreetMap() {
        if (!FileSenderFactory.getOsmSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.OSM);
            return;
        }

        showFileListDialog(FileSenderFactory.getOsmSender());
    }

    private void uploadToGoogleDrive() {
        if(!FileSenderFactory.getGoogleDriveSender().isAvailable()){
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GOOGLEDRIVE);
            return;
        }

        showFileListDialog(FileSenderFactory.getGoogleDriveSender());
    }

    private void uploadToDropBox() {

        if (!FileSenderFactory.getDropBoxSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.DROPBOX);
            return;
        }

        showFileListDialog(FileSenderFactory.getDropBoxSender());
    }


    private void uploadToCustomURL(){
        if(!FileSenderFactory.getCustomUrlSender().isAvailable()){
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
            return;
        }

        showFileListDialog(FileSenderFactory.getCustomUrlSender());
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

            Bundle extra = new Bundle();
            extra.putString("SENDER_NAME", sender.getName());
            String dialogTag = "FILE_UPLOAD_DIALOG";

            //As a special case.  If it's an OpenStreetMap upload, let user set description, tags before upload.
            if(sender.getName().equalsIgnoreCase(FileSender.SenderNames.OPENSTREETMAP)){
                dialogTag = "OSM_FILE_UPLOAD_DIALOG";
            }

            SimpleListDialog.build()
                    .title(R.string.osm_pick_file)
                    .extra(extra)
                    .items(files)
                    .choiceMode(CustomListDialog.MULTI_CHOICE)
                    .show(GpsMainActivity.this, dialogTag);

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

                SimpleListDialog.build()
                        .title(R.string.osm_pick_file)
                        .items(files)
                        .choiceMode(CustomListDialog.MULTI_CHOICE)
                        .show(GpsMainActivity.this, "FILE_SHARE_DIALOG");


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


    private void setBulbStatus() {
        ImageView bulb = (ImageView) findViewById(R.id.notification_bulb);

        if (!session.isStarted()) {
            bulb.setImageResource(R.drawable.circle_none);
            bulb.setOnClickListener(null);
        } else {
            if (session.isLocationServiceUnavailable()) {
                bulb.setImageResource(R.drawable.circle_warning);
                bulb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(GpsMainActivity.this, R.string.gpsprovider_unavailable, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                bulb.setImageResource(session.isStarted() ? R.drawable.circle_green : R.drawable.circle_none);
                bulb.setOnClickListener(null);
            }

        }
    }

    /**
     * Starts the service and binds the activity to it.
     */
    private void startAndBindService() {
        if(permissionWorkflowInProgress){
            LOG.debug("Don't start service while permissions haven't been granted yet.");
            return;
        }
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);

        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void stopAndUnbindServiceIfRequired() {
        if(!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // Alright. Why is this needed.
            // If the notification permission has been revoked or not granted for whatever reason.
            // When the application opens, the service starts, then stops right away.
            // Android requires a notification to be shown for a foreground service within 5 seconds.
            // So the application crashes and comes back repeatedly. Very weird.
            // The answer - if notifications are disabled, don't unbind the service. It will stop on its own.
            // Might be related: https://stackoverflow.com/questions/73067939/start-foreground-service-after-notification-permission-was-disabled-causes-crash
            return;
        }
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
                // Stop service crashes if the intent is null. lol
                if(serviceIntent == null){
                    serviceIntent = new Intent(this, GpsLoggingService.class);
                }
                stopService(serviceIntent);
            } catch (Exception e) {
                LOG.error("Could not stop the service", e);
            }
        }
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.CustomUrl upload){
        LOG.debug("Custom URL Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.log_customurl_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));

            if(userInvokedUpload){
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GoogleDrive upload){
        LOG.debug("Google Drive Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if(!upload.success){
            LOG.error(getString(R.string.google_drive_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
                Dialogs.showError(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
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
        Dialogs.hideProgress();
    }

    @EventBusHook
    public void onEventBackgroundThread(ProfileEvents.DownloadProfile downloadProfileEvent){

        LOG.debug("Downloading profile from URL: " + downloadProfileEvent.profileUrl);

        try {
            final String profileName = Files.getBaseName(downloadProfileEvent.profileUrl);
            File destFile =  new File(Files.storageFolder(getApplicationContext()) + "/" + profileName + ".properties");
            Files.DownloadFromUrl(downloadProfileEvent.profileUrl, destFile);

            LOG.debug("Posting to other events");
            EventBus.getDefault().post(new ProfileEvents.SwitchToProfile(profileName));
            EventBus.getDefault().post(new ProfileEvents.PopulateProfiles());


        } catch (Exception e) {
            LOG.error("Could not download properties file", e);
            Dialogs.hideProgress();
            Dialogs.showError("Could not download properties file","Could not download properties file",e.getMessage(), e, this);
        }

    }

    @EventBusHook
    public void onEventMainThread(ProfileEvents.DeleteProfile deleteProfileEvent){
        LOG.debug("Deleting profile: " + deleteProfileEvent.profileName);
        File f = new File(Files.storageFolder(GpsMainActivity.this), deleteProfileEvent.profileName+".properties");
        f.delete();

        populateProfilesList();
    }

    @EventBusHook
    public void onEventMainThread(ProfileEvents.SaveProfile saveProfileEvent){

        Dialogs.progress(GpsMainActivity.this, getString(R.string.please_wait));
        File f = new File(Files.storageFolder(GpsMainActivity.this), preferenceHelper.getCurrentProfileName()+".properties");
        try {
            preferenceHelper.savePropertiesFromPreferences(f);
        } catch (Exception e) {
            Dialogs.showError(getString(R.string.error), e.getMessage(), e.getMessage(), e, this);
            LOG.error("Could not save profile to file", e);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Dialogs.hideProgress();
            }
        },800);

    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationServicesUnavailable locationServicesUnavailable) {
        setBulbStatus();
    }
}

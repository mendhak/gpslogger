/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.PreferenceNames;
import com.mendhak.gpslogger.R;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class AppSettings extends Application {

    private static JobManager jobManager;
    private static SharedPreferences prefs;
    private static AppSettings instance;
    private static org.slf4j.Logger tracer = LoggerFactory.getLogger(AppSettings.class.getSimpleName());


    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();

        Configuration config = new Configuration.Builder(getInstance())
                .networkUtil(new WifiNetworkUtil(getInstance()))
                .consumerKeepAlive(60)
                .minConsumerCount(2)
                .build();
        jobManager = new JobManager(this, config);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    /**
     * Returns a configured Job Queue Manager
     */
    public static JobManager GetJobManager() {
        return jobManager;
    }

    public AppSettings() {
        instance = this;
    }

    /**
     * Returns a singleton instance of this class
     */
    public static AppSettings getInstance() {
        return instance;
    }


    /**
     * The minimum seconds interval between logging points
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_INTERVAL)
    public static int getMinimumLoggingInterval() {
        return Utilities.parseWithDefault(prefs.getString(PreferenceNames.MINIMUM_INTERVAL, "60"), 60);
    }

    /**
     * Sets the minimum time interval between logging points
     *
     * @param minimumSeconds - in seconds
     */
    public static void setMinimumLoggingInterval(int minimumSeconds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.MINIMUM_INTERVAL, String.valueOf(minimumSeconds));
        editor.apply();
    }


    /**
     * The minimum distance, in meters, to have traveled before a point is recorded
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_DISTANCE)
    public static int getMinimumDistanceInterval() {
        return (Utilities.parseWithDefault(prefs.getString(PreferenceNames.MINIMUM_DISTANCE, "0"), 0));
    }

    /**
     * Sets the minimum distance to have traveled before a point is recorded
     *
     * @param distanceBeforeLogging - in meters
     */
    public static void setMinimumDistanceInMeters(int distanceBeforeLogging) {
        prefs.edit().putString(PreferenceNames.MINIMUM_DISTANCE, String.valueOf(distanceBeforeLogging)).apply();
    }


    /**
     * The minimum accuracy of a point before the point is recorded, in meters
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_ACCURACY)
    public static int getMinimumAccuracy() {
        return (Utilities.parseWithDefault(prefs.getString(PreferenceNames.MINIMUM_ACCURACY, "0"), 0));
    }


    /**
     * Whether to keep GPS on between fixes
     */
    @ProfilePreference(name= PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES)
    public static boolean shouldKeepGPSOnBetweenFixes() {
        return prefs.getBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, false);
    }

    /**
     * Set whether to keep GPS on between fixes
     */
    public static void setShouldKeepGPSOnBetweenFixes(boolean keepFix) {
        prefs.edit().putBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, keepFix).apply();
    }


    /**
     * How long to keep retrying for a fix if one with the user-specified accuracy hasn't been found
     */
    @ProfilePreference(name= PreferenceNames.LOGGING_RETRY_TIME)
    public static int getLoggingRetryPeriod() {
        return (Utilities.parseWithDefault(prefs.getString(PreferenceNames.LOGGING_RETRY_TIME, "60"), 60));
    }


    /**
     * Sets how long to keep trying for an accurate fix
     *
     * @param retryInterval in seconds
     */
    public static void setLoggingRetryPeriod(int retryInterval) {
        prefs.edit().putString(PreferenceNames.LOGGING_RETRY_TIME, String.valueOf(retryInterval)).apply();
    }

    /**
     * How long to keep retrying for an accurate point before giving up
     */
    @ProfilePreference(name= PreferenceNames.ABSOLUTE_TIMEOUT)
    public static int getAbsoluteTimeoutForAcquiringPosition() {
        return (Utilities.parseWithDefault(prefs.getString(PreferenceNames.ABSOLUTE_TIMEOUT, "120"), 120));
    }

    /**
     * Sets how long to keep retrying for an accurate point before giving up
     *
     * @param absoluteTimeout in seconds
     */
    public static void setAbsoluteTimeoutForAcquiringPosition(int absoluteTimeout) {
        prefs.edit().putString(PreferenceNames.ABSOLUTE_TIMEOUT, String.valueOf(absoluteTimeout)).apply();
    }

    /**
     * Whether to start logging on application launch
     */
    @ProfilePreference(name= PreferenceNames.START_LOGGING_ON_APP_LAUNCH)
    public static boolean shouldStartLoggingOnAppLaunch() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_APP_LAUNCH, false);
    }

    /**
     * Whether to start logging when phone is booted up
     */
    @ProfilePreference(name= PreferenceNames.START_LOGGING_ON_BOOTUP)
    public static boolean shouldStartLoggingOnBootup() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_BOOTUP, false);
    }


    /**
     * Which navigation item the user selected
     */
    public static int getUserSelectedNavigationItem() {
        return Utilities.parseWithDefault(prefs.getString(PreferenceNames.SELECTED_NAVITEM, "0"),0);
    }

    /**
     * Sets which navigation item the user selected
     */
    public static void setUserSelectedNavigationItem(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.SELECTED_NAVITEM, String.valueOf(position));
        editor.apply();
    }

    /**
     * Whether to hide the buttons when displaying the app notification
     */
    @ProfilePreference(name= PreferenceNames.HIDE_NOTIFICATION_BUTTONS)
    public static boolean shouldHideNotificationButtons() {
        return prefs.getBoolean(PreferenceNames.HIDE_NOTIFICATION_BUTTONS, false);
    }


    /**
     * Whether to display certain values using imperial units
     */
    @ProfilePreference(name= PreferenceNames.DISPLAY_IMPERIAL)
    public static boolean shouldDisplayImperialUnits() {
        return prefs.getBoolean(PreferenceNames.DISPLAY_IMPERIAL, false);
    }


    /**
     * Whether to log to KML file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_KML)
    public static boolean shouldLogToKml() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_KML, false);
    }


    /**
     * Whether to log to GPX file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_GPX)
    public static boolean shouldLogToGpx() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_GPX, true);
    }


    /**
     * Whether to log to a plaintext CSV file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_CSV)
    public static boolean shouldLogToPlainText() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_CSV, false);
    }


    /**
     * Whether to log to NMEA file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_NMEA)
    public static boolean shouldLogToNmea() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_NMEA, false);
    }


    /**
     * Whether to log to a custom URL. The app will log to the URL returned by {@link #getCustomLoggingUrl()}
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_URL)
    public static boolean shouldLogToCustomUrl() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_URL, false);
    }

    /**
     * The custom URL to log to.  Relevant only if {@link #shouldLogToCustomUrl()} returns true.
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_URL_PATH)
    public static String getCustomLoggingUrl() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_PATH, "http://localhost/log?lat=%LAT&longitude=%LON&time=%TIME&s=%SPD");
    }

    /**
     * Sets custom URL to log to, if {@link #shouldLogToCustomUrl()} returns true.
     */
    public static void setCustomLoggingUrl(String customLoggingUrl) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_PATH, customLoggingUrl).apply();
    }

    /**
     * Whether to log to OpenGTS.  See their <a href="http://opengts.sourceforge.net/OpenGTS_Config.pdf">installation guide</a>
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_OPENGTS)
    public static boolean shouldLogToOpenGTS() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_OPENGTS, false);
    }


    /**
     * Gets a list of location providers that the app will listen to
     */
    @ProfilePreference(name= PreferenceNames.LOCATION_LISTENERS)
    public static Set<String> getChosenListeners() {
        Set<String> defaultListeners = new HashSet<String>(GetDefaultListeners());
        return prefs.getStringSet(PreferenceNames.LOCATION_LISTENERS, defaultListeners);
    }

    /**
     * Sets the list of location providers that the app will listen to
     *
     * @param chosenListeners a Set of listener names
     */
    public static void setChosenListeners(Set<String> chosenListeners) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(PreferenceNames.LOCATION_LISTENERS, chosenListeners);
        editor.apply();
    }

    /**
     * Sets the list of location providers that the app will listen to given their array positions in {@link #GetAvailableListeners()}.
     */
    public static void setChosenListeners(Integer... listenerIndices) {
        List<Integer> selectedItems = Arrays.asList(listenerIndices);
        final Set<String> chosenListeners = new HashSet<String>();

        for (Integer selectedItem : selectedItems) {
            chosenListeners.add(GetAvailableListeners().get(selectedItem));
        }

        if (chosenListeners.size() > 0) {
            setChosenListeners(chosenListeners);

        }
    }


    /**
     * Default set of listeners
     */
    public static List<String> GetDefaultListeners(){
        List<String> listeners = new ArrayList<String>();
        listeners.add(LocationManager.GPS_PROVIDER);
        listeners.add(LocationManager.NETWORK_PROVIDER);
        return listeners;
    }


    /**
     * All the possible listeners
     * @return
     */
    public static List<String> GetAvailableListeners() {

        List<String> listeners = new ArrayList<String>();
        listeners.add(LocationManager.GPS_PROVIDER);
        listeners.add(LocationManager.NETWORK_PROVIDER);
        listeners.add(LocationManager.PASSIVE_PROVIDER);
        return listeners;
    }



    /**
     * New file creation preference:
     * onceaday - once a day,
     * customfile - custom file (static),
     * everystart - every time the service starts
     */
    @ProfilePreference(name=PreferenceNames.NEW_FILE_CREATION_MODE)
    public static String getNewFileCreationMode() {
        return prefs.getString(PreferenceNames.NEW_FILE_CREATION_MODE, "onceaday");
    }


    /**
     * Whether a new file should be created daily
     */
    public static boolean shouldCreateNewFileOnceADay() {
        return (getNewFileCreationMode().equals("onceaday"));
    }


    /**
     * Whether only a custom file should be created
     */
    public static boolean shouldCreateCustomFile() {
        return getNewFileCreationMode().equals("custom") || getNewFileCreationMode().equals("static");
    }


    /**
     * The custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name= PreferenceNames.CUSTOM_FILE_NAME)
    public static String getCustomFileName() {
        return prefs.getString(PreferenceNames.CUSTOM_FILE_NAME, "gpslogger");
    }


    /**
     * Sets custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    public static void setCustomFileName(String customFileName) {
        prefs.edit().putString(PreferenceNames.CUSTOM_FILE_NAME, customFileName).apply();
    }

    /**
     * Whether to prompt for a custom file name each time logging starts, if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name= PreferenceNames.ASK_CUSTOM_FILE_NAME)
    public static boolean shouldAskCustomFileNameEachTime() {
        return prefs.getBoolean(PreferenceNames.ASK_CUSTOM_FILE_NAME, true);
    }

    /**
     * Whether automatic sending to various targets (email,ftp, dropbox, etc) is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ENABLED)
    public static boolean isAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ENABLED, false);
    }


    /**
     * The time, in minutes, before files are sent to the auto-send targets
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_FREQUENCY)
    public static int getAutoSendInterval() {
        return Math.round(Float.valueOf(prefs.getString(PreferenceNames.AUTOSEND_FREQUENCY, "60")));
    }


    /**
     * Whether to auto send to targets when logging is stopped
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ON_STOP)
    public static boolean shouldAutoSendOnStopLogging() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ON_STOP, false);
    }

    public static void setDebugToFile(boolean writeToFile) {
        prefs.edit().putBoolean(PreferenceNames.DEBUG_TO_FILE, writeToFile).apply();
    }

    /**
     * Whether to write log messages to a debuglog.txt file
     */
    public static boolean shouldDebugToFile() {
        return prefs.getBoolean(PreferenceNames.DEBUG_TO_FILE, false);
    }


    /**
     * Whether to zip the files up before auto sending to targets
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ZIP)
    public static boolean shouldSendZipFile() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ZIP, true);
    }


    /**
     * Whether to auto send to OpenGTS Server
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OPENGTS_ENABLED)
    public static boolean isOpenGtsAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OPENGTS_ENABLED, false);
    }


    /**
     * OpenGTS Server name
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_SERVER)
    public static String getOpenGTSServer() {
        return prefs.getString(PreferenceNames.OPENGTS_SERVER, "");
    }


    /**
     * OpenGTS Server Port
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_PORT)
    public static String getOpenGTSServerPort() {
        return prefs.getString(PreferenceNames.OPENGTS_PORT, "");
    }


    /**
     * Communication method when talking to OpenGTS (either UDP or HTTP)
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_PROTOCOL)
    public static String getOpenGTSServerCommunicationMethod() {
        return prefs.getString(PreferenceNames.OPENGTS_PROTOCOL, "");
    }


    /**
     * OpenGTS Server Path
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_SERVER_PATH)
    public static String getOpenGTSServerPath() {
        return prefs.getString(PreferenceNames.OPENGTS_SERVER_PATH, "");
    }


    /**
     * Device ID for OpenGTS communication
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_DEVICE_ID)
    public static String getOpenGTSDeviceId() {
        return prefs.getString(PreferenceNames.OPENGTS_DEVICE_ID, "");
    }


    /**
     * Account name for OpenGTS communication
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_ACCOUNT_NAME)
    public static String getOpenGTSAccountName() {
        return prefs.getString(PreferenceNames.OPENGTS_ACCOUNT_NAME, "");
    }


    /**
     * Whether to auto send to Google Drive
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_GOOGLEDRIVE_ENABLED)
    public static boolean isGDocsAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_GOOGLEDRIVE_ENABLED, false);
    }

    /**
     * Target directory for Google Drive auto send
     */
    @ProfilePreference(name= PreferenceNames.GOOGLEDRIVE_FOLDERNAME)
    public static String getGoogleDriveFolderName() {
        return prefs.getString(PreferenceNames.GOOGLEDRIVE_FOLDERNAME, "GPSLogger for Android");
    }

    /**
     * Google Drive OAuth token
     */
    public static String getGoogleDriveAuthToken(){
        return prefs.getString(PreferenceNames.GOOGLEDRIVE_AUTHTOKEN, "");
    }

    /**
     * Sets OAuth token for Google Drive auto send
     */
    public static void setGoogleDriveAuthToken(String authToken) {
        prefs.edit().putString(PreferenceNames.GOOGLEDRIVE_AUTHTOKEN, authToken).apply();
    }

    /**
     * Gets Google account used for Google Drive auto send
     */
    @ProfilePreference(name= PreferenceNames.GOOGLEDRIVE_ACCOUNTNAME)
    public static String getGoogleDriveAccountName() {
        return prefs.getString(PreferenceNames.GOOGLEDRIVE_ACCOUNTNAME, "");
    }

    /**
     * Sets account name to use for Google Drive auto send
     */
    public static void setGoogleDriveAccountName(String accountName) {
        prefs.edit().putString(PreferenceNames.GOOGLEDRIVE_ACCOUNTNAME, accountName).apply();
    }


    /**
     * Sets OpenStreetMap OAuth Token for auto send
     */
    public static void setOSMAccessToken(String token) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN, token).apply();
    }


    /**
     * Gets access token for OpenStreetMap auto send
     */
    public static String getOSMAccessToken() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN, "");
    }


    /**
     * Sets OpenStreetMap OAuth secret for auto send
     */
    public static void setOSMAccessTokenSecret(String secret) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN_SECRET, secret).apply();
    }

    /**
     * Gets access token secret for OpenStreetMap auto send
     */
    public static String getOSMAccessTokenSecret() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN_SECRET, "");
    }

    /**
     * Sets request token for OpenStreetMap auto send
     */
    public static void setOSMRequestToken(String token) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_REQUEST_TOKEN, token).apply();
    }

    /**
     * Sets request token secret for OpenStreetMap auto send
     */
    public static void setOSMRequestTokenSecret(String secret) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_REQUEST_TOKEN_SECRET, secret).apply();
    }

    /**
     * Description of uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name = PreferenceNames.OPENSTREETMAP_DESCRIPTION)
    public static String getOSMDescription() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_DESCRIPTION, "");
    }

    /**
     * Tags associated with uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.OPENSTREETMAP_TAGS)
    public static String getOSMTags() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_TAGS, "");
    }

    /**
     * Visibility of uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.OPENSTREETMAP_VISIBILITY)
    public static String getOSMVisibility() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_VISIBILITY, "private");
    }




    /**
     * Whether to auto send to OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OSM_ENABLED)
    public static boolean isOsmAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OSM_ENABLED, false);
    }


    /**
     * FTP Server name for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_SERVER)
    public static String getFtpServerName() {
        return prefs.getString(PreferenceNames.FTP_SERVER, "");
    }


    /**
     * FTP Port for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PORT)
    public static int getFtpPort() {
        return Utilities.parseWithDefault(prefs.getString(PreferenceNames.FTP_PORT, "21"), 21);
    }


    /**
     * FTP Username for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_USERNAME)
    public static String getFtpUsername() {
        return prefs.getString(PreferenceNames.FTP_USERNAME, "");
    }


    /**
     * FTP Password for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PASSWORD)
    public static String getFtpPassword() {
        return prefs.getString(PreferenceNames.FTP_PASSWORD, "");
    }

    /**
     * Whether to use FTPS
     */
    @ProfilePreference(name= PreferenceNames.FTP_USE_FTPS)
    public static boolean FtpUseFtps() {
        return prefs.getBoolean(PreferenceNames.FTP_USE_FTPS, false);
    }


    /**
     * FTP protocol to use (SSL or TLS)
     */
    @ProfilePreference(name= PreferenceNames.FTP_SSLORTLS)
    public static String getFtpProtocol() {
        return prefs.getString(PreferenceNames.FTP_SSLORTLS, "");
    }


    /**
     * Whether to use FTP Implicit mode for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_IMPLICIT)
    public static boolean FtpImplicit() {
        return prefs.getBoolean(PreferenceNames.FTP_IMPLICIT, false);
    }


    /**
     * Whether to auto send to FTP target
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_FTP_ENABLED)
    public static boolean isFtpAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_FTP_ENABLED, false);
    }


    /**
     * FTP Directory on the server for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_DIRECTORY)
    public static String getFtpDirectory() {
        return prefs.getString(PreferenceNames.FTP_DIRECTORY, "GPSLogger");
    }


    /**
     * OwnCloud server for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_SERVER)
    public static String getOwnCloudServerName() {
        return prefs.getString(PreferenceNames.OWNCLOUD_SERVER, "");
    }


    /**
     * OwnCloud username for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_USERNAME)
    public static String getOwnCloudUsername() {
        return prefs.getString(PreferenceNames.OWNCLOUD_USERNAME, "");
    }


    /**
     * OwnCloud password for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_PASSWORD)
    public static String getOwnCloudPassword() {
        return prefs.getString(PreferenceNames.OWNCLOUD_PASSWORD, "");
    }


    /**
     * OwnCloud target directory for autosend
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_DIRECTORY)
    public static String getOwnCloudDirectory() {
        return prefs.getString(PreferenceNames.OWNCLOUD_DIRECTORY, "/gpslogger");
    }


    /**
     * Whether to auto send to OwnCloud
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OWNCLOUD_ENABLED)
    public static boolean isOwnCloudAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OWNCLOUD_ENABLED, false);
    }


    /**
     * GPS Logger folder path on phone.  Falls back to {@link Utilities#GetDefaultStorageFolder(Context)} if nothing specified.
     */
    @ProfilePreference(name= PreferenceNames.GPSLOGGER_FOLDER)
    public static String getGpsLoggerFolder() {
        return prefs.getString(PreferenceNames.GPSLOGGER_FOLDER, Utilities.GetDefaultStorageFolder(getInstance()).getAbsolutePath());
    }


    /**
     * Sets GPS Logger folder path
     */
    public static void setGpsLoggerFolder(String folderPath) {
        prefs.edit().putString(PreferenceNames.GPSLOGGER_FOLDER, folderPath).apply();
    }



    /**
     * Whether to prefix the phone's serial number to the logging file
     */
    @ProfilePreference(name= PreferenceNames.PREFIX_SERIAL_TO_FILENAME)
    public static boolean shouldPrefixSerialToFileName() {
        return prefs.getBoolean(PreferenceNames.PREFIX_SERIAL_TO_FILENAME, false);
    }


    /**
     * Whether to detect user activity and if the user is still, pause logging
     */
    @ProfilePreference(name= PreferenceNames.ACTIVITYRECOGNITION_DONTLOGIFSTILL)
    public static boolean shouldNotLogIfUserIsStill() {
        return prefs.getBoolean(PreferenceNames.ACTIVITYRECOGNITION_DONTLOGIFSTILL, false);
    }


    /**
     * Whether to subtract GeoID height from the reported altitude to get Mean Sea Level altitude instead of WGS84
     */
    @ProfilePreference(name= PreferenceNames.ALTITUDE_SHOULD_ADJUST)
    public static boolean shouldAdjustAltitudeFromGeoIdHeight() {
        return prefs.getBoolean(PreferenceNames.ALTITUDE_SHOULD_ADJUST, false);
    }


    /**
     * How much to subtract from the altitude reported
     */
    @ProfilePreference(name= PreferenceNames.ALTITUDE_SUBTRACT_OFFSET)
    public static int getSubtractAltitudeOffset() {
        return Utilities.parseWithDefault(prefs.getString(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET, "0"),0);
    }


    /**
     * Whether to autosend only if wifi is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_WIFI_ONLY)
    public static boolean shouldAutoSendOnWifiOnly() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_WIFI_ONLY, false);
    }


    @ProfilePreference(name= PreferenceNames.CURRENT_PROFILE_NAME)
    public static String getCurrentProfileName() {
        return prefs.getString(PreferenceNames.CURRENT_PROFILE_NAME, getInstance().getString(R.string.profile_default));
    }

    public static void setCurrentProfileName(String profileName){
        prefs.edit().putString(PreferenceNames.CURRENT_PROFILE_NAME, profileName).apply();
    }

    /**
     * A preference to keep track of version specific changes.
     */
    @ProfilePreference(name= PreferenceNames.LAST_VERSION_SEEN_BY_USER)
    public static int getLastVersionSeen(){
        return prefs.getInt(PreferenceNames.LAST_VERSION_SEEN_BY_USER, 1);
    }

    public static void setLastVersionSeen(int lastVersionSeen){
        prefs.edit().putInt(PreferenceNames.LAST_VERSION_SEEN_BY_USER,lastVersionSeen).apply();
    }


    public static void SavePropertiesFromPreferences(File f) throws IOException {

        Properties props = new Properties();

        Method[] methods = AppSettings.class.getMethods();
        for(Method m : methods){

            Annotation a = m.getAnnotation(ProfilePreference.class);
            if(a != null){
                try {
                    Object val = m.invoke(null);

                    if(val != null){

                        if(((ProfilePreference)a).name().equals("listeners")){
                            String listeners = "";
                            Set<String> chosenListeners = (Set<String>)val;
                            StringBuilder sbListeners = new StringBuilder();
                            for (String l : chosenListeners) {
                                sbListeners.append(l);
                                sbListeners.append(",");
                            }
                            if(sbListeners.length() > 0){
                                listeners = sbListeners.substring(0, sbListeners.length() -1);
                            }
                            tracer.debug("LISTENERS - " + listeners);
                            props.setProperty("listeners", listeners);
                        }
                        else {
                            props.setProperty(((ProfilePreference)a).name(),String.valueOf(val));
                            tracer.debug(((ProfilePreference)a).name() + " : " + String.valueOf(val) );
                        }
                    }
                    else {
                        tracer.debug("Null value: " +((ProfilePreference)a).name() + " : " + String.valueOf(val) );
                    }

                } catch (Exception e) {
                    tracer.error("Could not save preferences to profile", e);
                }
            }
        }

        OutputStream outStream = new FileOutputStream(f);
        props.store(outStream,"Warning: This file can contain server names, passwords, email addresses and other sensitive information.");

    }


    /**
     * Sets preferences in a generic manner from a .properties file
     */

    public static void SetPreferenceFromPropertiesFile(File file) throws IOException {
        Properties props = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
        props.load(reader);

        for (Object key : props.keySet()) {

            SharedPreferences.Editor editor = prefs.edit();
            String value = props.getProperty(key.toString());
            tracer.info("Setting preset property: " + key.toString() + " to " + value.toString());

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
            } else if (key.equals("listeners")) {
                List<String> availableListeners = GetAvailableListeners();
                Set<String> chosenListeners = new HashSet<>();
                String[] csvListeners = value.split(",");
                for (String l : csvListeners) {
                    if (availableListeners.contains(l)) {
                        chosenListeners.add(l);
                    }
                }
                if (chosenListeners.size() > 0) {
                    prefs.edit().putStringSet("listeners", chosenListeners).apply();
                }

            } else {
                editor.putString(key.toString(), value);
            }
            editor.apply();
        }

    }
}

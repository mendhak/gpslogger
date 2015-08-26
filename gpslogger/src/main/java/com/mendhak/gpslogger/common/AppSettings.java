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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

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

    public static JobManager GetJobManager(){
        return jobManager;
    }

    public AppSettings() {
        instance = this;
    }

    public static AppSettings getInstance() {
        return instance;
    }



    /**
     * The minimum seconds interval between logging points
     */
    public static int getMinimumSeconds() {
        String minimumSecondsString = prefs.getString("time_before_logging", "60");
        return (Integer.valueOf(minimumSecondsString));
    }

    /**
     * Whether to start logging on application launch
     */
    public static boolean shouldStartLoggingOnAppLaunch() {
        return prefs.getBoolean("startonapplaunch", false);
    }


    /**
     * Which navigation item the user selected
     */
    public static int getUserSelectedNavigationItem() {
        return prefs.getInt("SPINNER_SELECTED_POSITION", 0);
    }

    /**
     * Sets which navigation item the user selected
     */
    public static void setUserSelectedNavigationItem(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("SPINNER_SELECTED_POSITION", position);
        editor.apply();
    }

    /**
     * Whether to hide the buttons when displaying the app notification
     */
    public static boolean shouldHideNotificationButtons() {
        return prefs.getBoolean("hide_notification_buttons", false);
    }



    /**
     * Whether to display certain values using imperial units
     */
    public static boolean shouldUseImperial() {
        return prefs.getBoolean("useImperial", false);
    }



    /**
     * Whether to log to KML file
     */
    public static boolean shouldLogToKml() {
        return prefs.getBoolean("log_kml", false);
    }


    /**
     * Whether to log to GPX file
     */
    public static boolean shouldLogToGpx() {
        return prefs.getBoolean("log_gpx", true);
    }


    /**
     * Whether to log to a plaintext CSV file
     */
    public static boolean shouldLogToPlainText() {
        return prefs.getBoolean("log_plain_text", false);
    }


    /**
     * Whether to log to NMEA file
     */
    public static boolean shouldLogToNmea() {
        return prefs.getBoolean("log_nmea", false);
    }


    /**
     * Whether to log to a custom URL. The app will log to the URL returned by {@link #getCustomLoggingUrl()}
     */
    public static boolean shouldLogToCustomUrl() {
        return prefs.getBoolean("log_customurl_enabled", false);
    }

    /**
     * The custom URL to log to.  Relevant only if {@link #shouldLogToCustomUrl()} returns true.
     */
    public static String getCustomLoggingUrl() {
        return prefs.getString("log_customurl_url", "");
    }


    /**
     * Whether to log to OpenGTS.  See their <a href="http://opengts.sourceforge.net/OpenGTS_Config.pdf">installation guide</a>
     */
    public static boolean shouldLogToOpenGTS() {
        return prefs.getBoolean("log_opengts", false);
    }


    /**
     * Gets a list of location providers that the app will listen to
     */
    public static Set<String> getChosenListeners() {
        Set<String> defaultListeners = new HashSet<String>(GetDefaultListeners());
        return prefs.getStringSet("listeners", defaultListeners);
    }

    /**
     * Sets the list of location providers that the app will listen to
     * @param chosenListeners a Set of listener names
     */
    public static void setChosenListeners(Set<String> chosenListeners){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("listeners", chosenListeners);
        editor.apply();
    }

    /**
     * Sets the list of location providers that the app will listen to given their array positions in {@link #GetDefaultListeners()}.
     */
    public static void setChosenListeners(Integer... listenerIndices){
        List<Integer> selectedItems = Arrays.asList(listenerIndices);
        final Set<String> chosenListeners = new HashSet<String>();

        for (Integer selectedItem : selectedItems) {
            chosenListeners.add(GetDefaultListeners().get(selectedItem));
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
        listeners.add("gps");
        listeners.add("network");
        listeners.add("passive");

        return listeners;
    }


    /**
     * The minimum distance to have traveled before a point is recorded
     */
    public static int getMinimumDistanceInMeters() {
        String minimumDistanceString = prefs.getString("distance_before_logging", "0");

        if (minimumDistanceString != null && minimumDistanceString.length() > 0) {
            return (Integer.valueOf(minimumDistanceString));
        }

        return 0;

    }


    /**
     * The minimum accuracy of a point before the point is recorded
     */
    public static int getMinimumAccuracyInMeters() {
        String minimumAccuracyString = prefs.getString("accuracy_before_logging", "0");

        if (minimumAccuracyString != null && minimumAccuracyString.length() > 0) {
            return (Integer.valueOf(minimumAccuracyString));
        }

        return 0;
    }



    /**
     * Whether to keep GPS on between fixes
     */
    public static boolean shouldkeepFix() {
        return prefs.getBoolean("keep_fix", false);
    }


    /**
     * How long to keep retrying for a fix if one with the user-specified accuracy hasn't been found
     */
    public static int getRetryInterval() {
        String retryIntervalString = prefs.getString("retry_time", "60");

        if (retryIntervalString != null && retryIntervalString.length() > 0) {
            return (Integer.valueOf(retryIntervalString));
        }

        return 60;
    }


    /**
     * New file creation preference:
     *     onceaday - once a day,
     *     customfile - custom file (static),
     *     everystart - every time the service starts
     */
    static String getNewFileCreation() {
        return prefs.getString("new_file_creation", "onceaday");
    }


    /**
     * Whether a new file should be created daily
     */
    public static boolean shouldCreateNewFileOnceADay() {
        return (getNewFileCreation().equals("onceaday"));
    }


    /**
     * Whether only a custom file should be created
     */
    public static boolean isCustomFile() {
        return getNewFileCreation().equals("custom") || getNewFileCreation().equals("static");
    }


    /**
     * The custom filename to use if {@link #isCustomFile()} returns true
     */
    public static String getCustomFileName() {
        return prefs.getString("new_file_custom_name", "gpslogger");
    }

    /**
     * Whether to prompt for a custom file name each time logging starts, if {@link #isCustomFile()} returns true
     */
    public static boolean shouldAskCustomFileNameEachTime() {
        return prefs.getBoolean("new_file_custom_each_time", true);
    }


    /**
     * Whether automatic sending to various targets (email,ftp, dropbox, etc) is enabled
     * @return
     */
    public static boolean isAutoSendEnabled() {
        return prefs.getBoolean("autosend_enabled", false);
    }



    /**
     * The time, in minutes, before files are sent to the auto-send targets
     */
    public static Float getAutoSendDelay() {
        try{
            return Float.valueOf(prefs.getString("autosend_frequency_minutes", "60"));
        }
        catch (Exception e)  {
            return 60f;
        }
    }


    /**
     * Whether to auto send to targets when logging is stopped
     */
    public static boolean shouldAutoSendWhenIPressStop() {
        return prefs.getBoolean("autosend_frequency_whenstoppressed", false);
    }


    /**
     * SMTP Server to use when sending emails
     * @return
     */
    public static String getSmtpServer() {
        return prefs.getString("smtp_server", "");
    }

    /**
     * Whether automatic sending to email is enabled
     */
    public static boolean isEmailAutoSendEnabled() {
        return prefs.getBoolean("autoemail_enabled", false);
    }

    /**
     * SMTP Port to use when sending emails
     */
    public static String getSmtpPort() {
        return prefs.getString("smtp_port", "25");
    }

    /**
     * SMTP Username to use when sending emails
     */
    public static String getSmtpUsername() {
        return prefs.getString("smtp_username", "");
    }


    /**
     * SMTP Password to use when sending emails
     */
    public static String getSmtpPassword() {
        return prefs.getString("smtp_password", "");
    }


    /**
     * Whether SSL is enabled when sending emails
     */
    public static boolean isSmtpSsl() {
        return prefs.getBoolean("smtp_ssl", true);
    }

    /**
     * Email addresses to send to
     */
    public static String getAutoEmailTargets() {
        return prefs.getString("autoemail_target", "");
    }


    /**
     * SMTP from address to use

     */
    private static String getSmtpFrom() {
        return prefs.getString("smtp_from", "");
    }

    /**
     * The from address to use when sending an email, uses {@link #getSmtpUsername()} if {@link #getSmtpFrom()} is not specified
     *
     */
    public static String getSenderAddress() {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }


    /**
     * Whether to write log messages to a debuglog.txt file
     */
    public static boolean isDebugToFile() {
        return prefs.getBoolean("debugtofile", false);
    }


    /**
     * Whether to zip the files up before auto sending to targets
     */
    public static boolean shouldSendZipFile() {
        return prefs.getBoolean("autosend_sendzip", true);
    }


    /**
     * Whether to auto send to OpenGTS Server
     */
    public static boolean isOpenGtsAutoSendEnabled() {
        return prefs.getBoolean("autoopengts_enabled", false);
    }


    /**
     * OpenGTS Server name
     */
    public static String getOpenGTSServer() {
        return prefs.getString("opengts_server", "");
    }


    /**
     * OpenGTS Server Port
     */
    public static String getOpenGTSServerPort() {
        return prefs.getString("opengts_server_port", "");
    }


    /**
     * Communication method when talking to OpenGTS (either UDP or HTTP)
     */
    public static String getOpenGTSServerCommunicationMethod() {
        return prefs.getString("opengts_server_communication_method", "");
    }


    /**
     * OpenGTS Server Path
     */
    public static String getOpenGTSServerPath() {
        return prefs.getString("autoopengts_server_path", "");
    }


    /**
     * Device ID for OpenGTS communication
     */
    public static String getOpenGTSDeviceId() {
        return prefs.getString("opengts_device_id", "");
    }


    /**
     * Account name for OpenGTS communication
     */
    public static String getOpenGTSAccountName() {
        return prefs.getString("opengts_accountname","");
    }





    /**
     * Sets preferences in a generic manner from a .properties file
     */
    public static void SetPreferenceFromProperties(Properties props){
        for(Object key : props.keySet()){

            SharedPreferences.Editor editor = prefs.edit();
            String value = props.getProperty(key.toString());
            tracer.info("Setting preset property: " + key.toString() + " to " + value.toString());

            if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")){
                editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
            }
            else if(key.equals("listeners")){
                List<String> availableListeners = GetDefaultListeners();
                Set<String> chosenListeners = new HashSet<>();
                String[] csvListeners = value.split(",");
                for(String l : csvListeners){
                    if(availableListeners.contains(l)){
                        chosenListeners.add(l);
                    }
                }
                if(chosenListeners.size() > 0){
                    prefs.edit().putStringSet("listeners", chosenListeners).apply();
                }

            } else {
                editor.putString(key.toString(), value);
            }
            editor.apply();
        }
    }






    // ---------------------------------------------------
    // User Preferences
    // ---------------------------------------------------
    private static boolean useImperial = false;
    private static boolean hideNotificationButtons = false;
    private static boolean newFileOnceADay;

    private static boolean logToKml;
    private static boolean logToGpx;
    private static boolean logToPlainText;
    private static boolean logToNmea;
    private static boolean logToCustomUrl;
    private static String customLoggingUrl;
    private static int minimumSeconds;
    private static boolean keepFix;
    private static int retryInterval;
    private static String newFileCreation;
    private static Float autoSendDelay = 0f;
    private static boolean autoSendEnabled = false;
    private static boolean emailAutoSendEnabled = false;
    private static String smtpServer;
    private static String smtpPort;
    private static String smtpUsername;
    private static String smtpPassword;
    private static String smtpFrom;
    private static String autoEmailTargets;
    private static boolean smtpSsl;
    private static boolean debugToFile;
    private static int minimumDistance;
    private static int minimumAccuracy;
    private static boolean shouldSendZipFile;

    private static boolean logToOpenGts;

    private static boolean openGtsAutoSendEnabled;
    private static String openGTSServer;
    private static String openGTSServerPort;
    private static String openGTSServerCommunicationMethod;
    private static String openGTSServerPath;
    private static String openGTSDeviceId;
    private static String openGTSAccountName;

    private static boolean ftpAutoSendEnabled;
    private static String ftpServerName;
    private static int ftpPort;
    private static String ftpUsername;
    private static String ftpPassword;
    private static String ftpDirectory;
    private static boolean ftpUseFtps;
    private static String ftpProtocol;
    private static boolean ftpImplicit;

    private static boolean ownCloudAutoSendEnabled;
    private static String ownCloudServerName;
    private static String ownCloudUsername;
    private static String ownCloudPassword;
    private static String ownCloudDirectory;

    private static String customFileName;
    private static boolean isCustomFile;
    private static boolean askCustomFileNameEachTime;

    private static String gpsLoggerFolder;

    private static boolean fileNamePrefixSerial;

    private static int absoluteTimeout;

    private static boolean autoSendWhenIPressStop;

    private static boolean gDocsAutoSendEnabled;
    private static boolean dropboxAutoSendEnabled;
    private static boolean osmAutoSendEnabled;

    private static String googleDriveFolderName;

    private static boolean dontLogIfUserIsStill;

    private static boolean adjustAltitudeFromGeoIdHeight;
    private static int subtractAltitudeOffset;


    public static boolean isOsmAutoSendEnabled() {
        return osmAutoSendEnabled;
    }

    public static void setOsmAutoSendEnabled(boolean osmAutoSendEnabled) {
        AppSettings.osmAutoSendEnabled = osmAutoSendEnabled;
    }

    public static boolean isDropboxAutoSendEnabled(){
        return dropboxAutoSendEnabled;
    }

    public static void setDropboxAutoSendEnabled(boolean enabled){
        AppSettings.dropboxAutoSendEnabled = enabled;
    }

    public static boolean isGDocsAutoSendEnabled() {
        return gDocsAutoSendEnabled;
    }

    public static void setGDocsAutoSendEnabled(boolean gdocsEnabled) {
        AppSettings.gDocsAutoSendEnabled = gdocsEnabled;
    }






































    public static String getFtpServerName() {
        return ftpServerName;
    }

    public static void setFtpServerName(String ftpServerName) {
        AppSettings.ftpServerName = ftpServerName;
    }

    public static int getFtpPort() {
        return ftpPort;
    }

    public static void setFtpPort(int ftpPort) {
        AppSettings.ftpPort = ftpPort;
    }

    public static String getFtpUsername() {
        return ftpUsername;
    }

    public static void setFtpUsername(String ftpUsername) {
        AppSettings.ftpUsername = ftpUsername;
    }

    public static String getFtpPassword() {
        return ftpPassword;
    }

    public static void setFtpPassword(String ftpPassword) {
        AppSettings.ftpPassword = ftpPassword;
    }

    public static boolean FtpUseFtps() {
        return ftpUseFtps;
    }

    public static void setFtpUseFtps(boolean ftpUseFtps) {
        AppSettings.ftpUseFtps = ftpUseFtps;
    }

    public static String getFtpProtocol() {
        return ftpProtocol;
    }

    public static void setFtpProtocol(String ftpProtocol) {
        AppSettings.ftpProtocol = ftpProtocol;
    }

    public static boolean FtpImplicit() {
        return ftpImplicit;
    }

    public static void setFtpImplicit(boolean ftpImplicit) {
        AppSettings.ftpImplicit = ftpImplicit;
    }

    public static boolean isFtpAutoSendEnabled() {
        return ftpAutoSendEnabled;
    }

    public static void setFtpAutoSendEnabled(boolean ftpAutoSendEnabled) {
        AppSettings.ftpAutoSendEnabled = ftpAutoSendEnabled;
    }

    public static String getOwnCloudServerName() {
        return ownCloudServerName;
    }

    public static void setOwnCloudServerName(String ownCloudServerName) {
        AppSettings.ownCloudServerName = ownCloudServerName;
    }

    public static String getOwnCloudUsername() {
        return ownCloudUsername;
    }

    public static void setOwnCloudUsername(String ownCloudUsername) {
        AppSettings.ownCloudUsername = ownCloudUsername;
    }

    public static String getOwnCloudPassword() {
        return ownCloudPassword;
    }

    public static void setOwnCloudPassword(String ownCloudPassword) {
        AppSettings.ownCloudPassword = ownCloudPassword;
    }

    public static String getOwnCloudDirectory() { return ownCloudDirectory; }

    public static void setOwnCloudDirectory(String ownCloudDirectory) {
        AppSettings.ownCloudDirectory = ownCloudDirectory;
    }

    public static boolean isOwnCloudAutoSendEnabled() {
        return ownCloudAutoSendEnabled;
    }

    public static void setOwnCloudAutoSendEnabled(boolean ownCloudAutoSendEnabled) {
        AppSettings.ownCloudAutoSendEnabled = ownCloudAutoSendEnabled;
    }











    public static String getGpsLoggerFolder() {
        return gpsLoggerFolder;
    }

    public static void setGpsLoggerFolder(String gpsLoggerFolder) {
        AppSettings.gpsLoggerFolder = gpsLoggerFolder;
    }

    public static String getFtpDirectory() {
        return ftpDirectory;
    }

    public static void setFtpDirectory(String ftpDirectory) {
        AppSettings.ftpDirectory = ftpDirectory;
    }

    public static boolean shouldPrefixSerialToFileName() {
        return fileNamePrefixSerial;
    }

    public static void setFileNamePrefixSerial(boolean fileNamePrefixSerial) {
        AppSettings.fileNamePrefixSerial = fileNamePrefixSerial;
    }

    public static int getAbsoluteTimeout() {
        return absoluteTimeout;
    }

    public static void setAbsoluteTimeout(int absoluteTimeout) {
        AppSettings.absoluteTimeout = absoluteTimeout;
    }











    public static String getGoogleDriveFolderName() {
        return googleDriveFolderName;
    }

    public static void setGoogleDriveFolderName(String googleDriveFolderName) {
        AppSettings.googleDriveFolderName = googleDriveFolderName;
    }

    public static boolean shouldNotLogIfUserIsStill() {
        return AppSettings.dontLogIfUserIsStill;
    }

    public static void setShouldNotLogIfUserIsStill(boolean check){
        AppSettings.dontLogIfUserIsStill = check;
    }


    public static boolean shouldAdjustAltitudeFromGeoIdHeight() {
        return adjustAltitudeFromGeoIdHeight;
    }

    public static void setAdjustAltitudeFromGeoIdHeight(boolean adjustAltitudeFromGeoIdHeight) {
        AppSettings.adjustAltitudeFromGeoIdHeight = adjustAltitudeFromGeoIdHeight;
    }


    public static int getSubtractAltitudeOffset() {
        return subtractAltitudeOffset;
    }

    public static void setSubtractAltitudeOffset(int subtractAltitudeOffset) {
        AppSettings.subtractAltitudeOffset = subtractAltitudeOffset;
    }

}

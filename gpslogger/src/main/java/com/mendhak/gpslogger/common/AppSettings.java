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
    public static int getMinimumLoggingInterval() {
        return (Integer.valueOf(prefs.getString("time_before_logging", "60")));
    }

    /**
     * Sets the minimum time interval between logging points
     *
     * @param minimumSeconds - in seconds
     */
    public static void setMinimumLoggingInterval(int minimumSeconds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("time_before_logging", String.valueOf(minimumSeconds));
        editor.apply();
    }


    /**
     * The minimum distance, in meters, to have traveled before a point is recorded
     */
    public static int getMinimumDistanceInterval() {
        return (Integer.valueOf(prefs.getString("distance_before_logging", "0")));
    }

    /**
     * Sets the minimum distance to have traveled before a point is recorded
     *
     * @param distanceBeforeLogging - in meters
     */
    public static void setMinimumDistanceInMeters(int distanceBeforeLogging) {
        prefs.edit().putString("distance_before_logging", String.valueOf(distanceBeforeLogging)).apply();
    }


    /**
     * The minimum accuracy of a point before the point is recorded, in meters
     */
    public static int getMinimumAccuracy() {
        return (Integer.valueOf(prefs.getString("accuracy_before_logging", "0")));
    }


    /**
     * Whether to keep GPS on between fixes
     */
    public static boolean shouldKeepGPSOnBetweenFixes() {
        return prefs.getBoolean("keep_fix", false);
    }

    /**
     * Set whether to keep GPS on between fixes
     */
    public static void setShouldKeepGPSOnBetweenFixes(boolean keepFix) {
        prefs.edit().putBoolean("keep_fix", keepFix).apply();
    }


    /**
     * How long to keep retrying for a fix if one with the user-specified accuracy hasn't been found
     */
    public static int getLoggingRetryPeriod() {
        return (Integer.valueOf(prefs.getString("retry_time", "60")));
    }


    /**
     * Sets how long to keep trying for an accurate fix
     *
     * @param retryInterval in seconds
     */
    public static void setLoggingRetryPeriod(int retryInterval) {
        prefs.edit().putString("retry_time", String.valueOf(retryInterval)).apply();
    }

    /**
     * How long to keep retrying for an accurate point before giving up
     */
    public static int getAbsoluteTimeoutForAcquiringPosition() {
        return (Integer.valueOf(prefs.getString("absolute_timeout", "120")));
    }

    /**
     * Sets how long to keep retrying for an accurate point before giving up
     *
     * @param absoluteTimeout in seconds
     */
    public static void setAbsoluteTimeoutForAcquiringPosition(int absoluteTimeout) {
        prefs.edit().putString("absolute_timeout", String.valueOf(absoluteTimeout)).apply();
    }

    /**
     * Whether to start logging on application launch
     */
    public static boolean shouldStartLoggingOnAppLaunch() {
        return prefs.getBoolean("startonapplaunch", false);
    }

    /**
     * Whether to start logging when phone is booted up
     */
    public static boolean shouldStartLoggingOnBootup() {
        return prefs.getBoolean("startonbootup", false);
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
    public static boolean shouldDisplayImperialUnits() {
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
     *
     * @param chosenListeners a Set of listener names
     */
    public static void setChosenListeners(Set<String> chosenListeners) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("listeners", chosenListeners);
        editor.apply();
    }

    /**
     * Sets the list of location providers that the app will listen to given their array positions in {@link #GetDefaultListeners()}.
     */
    public static void setChosenListeners(Integer... listenerIndices) {
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
    public static List<String> GetDefaultListeners() {

        List<String> listeners = new ArrayList<String>();
        listeners.add("gps");
        listeners.add("network");
        listeners.add("passive");

        return listeners;
    }





    /**
     * New file creation preference:
     * onceaday - once a day,
     * customfile - custom file (static),
     * everystart - every time the service starts
     */
    static String getNewFileCreationMode() {
        return prefs.getString("new_file_creation", "onceaday");
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
    public static String getCustomFileName() {
        return prefs.getString("new_file_custom_name", "gpslogger");
    }

    /**
     * Whether to prompt for a custom file name each time logging starts, if {@link #shouldCreateCustomFile()} returns true
     */
    public static boolean shouldAskCustomFileNameEachTime() {
        return prefs.getBoolean("new_file_custom_each_time", true);
    }


    /**
     * Whether automatic sending to various targets (email,ftp, dropbox, etc) is enabled
     */
    public static boolean isAutoSendEnabled() {
        return prefs.getBoolean("autosend_enabled", false);
    }


    /**
     * The time, in minutes, before files are sent to the auto-send targets
     */
    public static Float getAutoSendInterval() {
        return Float.valueOf(prefs.getString("autosend_frequency_minutes", "60"));
    }


    /**
     * Whether to auto send to targets when logging is stopped
     */
    public static boolean shouldAutoSendOnStopLogging() {
        return prefs.getBoolean("autosend_frequency_whenstoppressed", false);
    }



    /**
     * Whether automatic sending to email is enabled
     */
    public static boolean isEmailAutoSendEnabled() {
        return prefs.getBoolean("autoemail_enabled", false);
    }


    /**
     * SMTP Server to use when sending emails
     */
    public static String getSmtpServer() {
        return prefs.getString("smtp_server", "");
    }

    /**
     * Sets SMTP Server to use when sending emails
     */
    public static void setSmtpServer(String smtpServer) {
        prefs.edit().putString("smtp_server", smtpServer).apply();
    }

    /**
     * SMTP Port to use when sending emails
     */
    public static String getSmtpPort() {
        return prefs.getString("smtp_port", "25");
    }

    public static void setSmtpPort(String port) {
        prefs.edit().putString("smtp_port", port).apply();
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
     * Sets whether SSL is enabled when sending emails
     */
    public static void setSmtpSsl(boolean smtpSsl) {
        prefs.edit().putBoolean("smtp_ssl", smtpSsl).apply();
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
     */
    public static String getSmtpSenderAddress() {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }


    public static void setDebugToFile(boolean writeToFile) {
        prefs.edit().putBoolean("debugtofile", writeToFile).apply();
    }

    /**
     * Whether to write log messages to a debuglog.txt file
     */
    public static boolean shouldDebugToFile() {
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
        return prefs.getString("opengts_accountname", "");
    }


    /**
     * Whether to auto send to Google Drive
     */
    public static boolean isGDocsAutoSendEnabled() {
        return prefs.getBoolean("gdocs_enabled", false);
    }

    /**
     * Target directory for Google Drive auto send
     */
    public static String getGoogleDriveFolderName() {
        return prefs.getString("gdocs_foldername", "GPSLogger for Android");
    }


    /**
     * Whether to auto send to Dropbox
     */
    public static boolean isDropboxAutoSendEnabled() {
        return prefs.getBoolean("dropbox_enabled", false);
    }

    public static String getDropBoxAccessKeyName() {
        return prefs.getString("DROPBOX_ACCESS_KEY", null);
    }

    public static void setDropBoxAccessKeyName(String key) {
        prefs.edit().putString("DROPBOX_ACCESS_KEY", key).apply();
    }

    public static String getDropBoxAccessSecretName() {
        return prefs.getString("DROPBOX_ACCESS_SECRET", null);
    }

    public static void setDropBoxAccessSecret(String secret) {
        prefs.edit().putString("DROPBOX_ACCESS_SECRET", secret).apply();
    }


    /**
     * Whether to auto send to OpenStreetMap
     */
    public static boolean isOsmAutoSendEnabled() {
        return prefs.getBoolean("osm_enabled", false);
    }


    /**
     * FTP Server name for auto send
     */
    public static String getFtpServerName() {
        return prefs.getString("autoftp_server", "");
    }


    /**
     * FTP Port for auto send
     */
    public static int getFtpPort() {
        return Integer.valueOf(prefs.getString("autoftp_port", "21"));
    }


    /**
     * FTP Username for auto send
     */
    public static String getFtpUsername() {
        return prefs.getString("autoftp_username", "");
    }


    /**
     * FTP Password for auto send
     */
    public static String getFtpPassword() {
        return prefs.getString("autoftp_password", "");
    }

    /**
     * Whether to use FTPS
     */
    public static boolean FtpUseFtps() {
        return prefs.getBoolean("autoftp_useftps", false);
    }


    /**
     * FTP protocol to use (SSL or TLS)
     */
    public static String getFtpProtocol() {
        return prefs.getString("autoftp_ssltls", "");
    }


    /**
     * Whether to use FTP Implicit mode for auto send
     */
    public static boolean FtpImplicit() {
        return prefs.getBoolean("autoftp_implicit", false);
    }


    /**
     * Whether to auto send to FTP target
     */
    public static boolean isFtpAutoSendEnabled() {
        return prefs.getBoolean("autoftp_enabled", false);
    }


    /**
     * FTP Directory on the server for auto send
     */
    public static String getFtpDirectory() {
        return prefs.getString("autoftp_directory", "GPSLogger");
    }


    /**
     * OwnCloud server for auto send
     */
    public static String getOwnCloudServerName() {
        return prefs.getString("owncloud_server", "");
    }


    /**
     * OwnCloud username for auto send
     */
    public static String getOwnCloudUsername() {
        return prefs.getString("owncloud_username", "");
    }


    /**
     * OwnCloud password for auto send
     */
    public static String getOwnCloudPassword() {
        return prefs.getString("owncloud_password", "");
    }


    /**
     * OwnCloud target directory for autosend
     */
    public static String getOwnCloudDirectory() {
        return prefs.getString("owncloud_directory", "/gpslogger");
    }


    /**
     * Whether to auto send to OwnCloud
     */
    public static boolean isOwnCloudAutoSendEnabled() {
        return prefs.getBoolean("owncloud_enabled", false);
    }


    /**
     * GPS Logger folder path on phone.  Falls back to {@link Utilities#GetDefaultStorageFolder(Context)} if nothing specified.
     */
    public static String getGpsLoggerFolder() {
        return prefs.getString("gpslogger_folder", Utilities.GetDefaultStorageFolder(getInstance()).getAbsolutePath());
    }


    /**
     * Whether to prefix the phone's serial number to the logging file
     */
    public static boolean shouldPrefixSerialToFileName() {
        return prefs.getBoolean("new_file_prefix_serial", false);
    }


    /**
     * Whether to detect user activity and if the user is still, pause logging
     */
    public static boolean shouldNotLogIfUserIsStill() {
        return prefs.getBoolean("activityrecognition_dontlogifstill", false);
    }


    /**
     * Whether to subtract GeoID height from the reported altitude to get Mean Sea Level altitude instead of WGS84
     */
    public static boolean shouldAdjustAltitudeFromGeoIdHeight() {
        return prefs.getBoolean("altitude_subtractgeoidheight", false);
    }


    /**
     * How much to subtract from the altitude reported
     */
    public static int getSubtractAltitudeOffset() {
        return Integer.valueOf(prefs.getString("altitude_subtractoffset", "0"));
    }


    /**
     * Whether to autosend only if wifi is enabled
     */
    public static boolean shouldAutoSendOnWifiOnly() {
        return prefs.getBoolean("autosend_wifionly", false);
    }


    /**
     * Sets preferences in a generic manner from a .properties file
     */
    public static void SetPreferenceFromProperties(Properties props) {
        for (Object key : props.keySet()) {

            SharedPreferences.Editor editor = prefs.edit();
            String value = props.getProperty(key.toString());
            tracer.info("Setting preset property: " + key.toString() + " to " + value.toString());

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
            } else if (key.equals("listeners")) {
                List<String> availableListeners = GetDefaultListeners();
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

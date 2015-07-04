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
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import de.greenrobot.event.EventBus;

import java.util.Set;

public class AppSettings extends Application {

    private static JobManager jobManager;


    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();

        Configuration config = new Configuration.Builder(getInstance()).consumerKeepAlive(60).minConsumerCount(2).build();
        jobManager = new JobManager(this, config);
    }

    public static JobManager GetJobManager(){
        return jobManager;
    }


    private static AppSettings instance;
    public AppSettings() {
        instance = this;
    }

    public static AppSettings getInstance() {
        return instance;
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
    private static Set<String> chosenListeners;
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


    /**
     * @return the useImperial
     */
    public static boolean shouldUseImperial() {
        return useImperial;
    }

    /**
     * @param useImperial the useImperial to set
     */
    static void setUseImperial(boolean useImperial) {
        AppSettings.useImperial = useImperial;
    }

    /**
     * @return the newFileOnceADay
     */
    public static boolean shouldCreateNewFileOnceADay() {
        return newFileOnceADay;
    }

    /**
     * @param newFileOnceADay the newFileOnceADay to set
     */
    static void setNewFileOnceADay(boolean newFileOnceADay) {
        AppSettings.newFileOnceADay = newFileOnceADay;
    }



    /**
     * @return the logToKml
     */
    public static boolean shouldLogToKml() {
        return logToKml;
    }

    /**
     * @param logToKml the logToKml to set
     */
    static void setLogToKml(boolean logToKml) {
        AppSettings.logToKml = logToKml;
    }

    /**
     * @return the logToGpx
     */
    public static boolean shouldLogToGpx() {
        return logToGpx;
    }

    /**
     * @param logToGpx the logToGpx to set
     */
    static void setLogToGpx(boolean logToGpx) {
        AppSettings.logToGpx = logToGpx;
    }

    public static boolean shouldLogToPlainText() {
        return logToPlainText;
    }

    static void setLogToPlainText(boolean logToPlainText) {
        AppSettings.logToPlainText = logToPlainText;
    }


    /**
     * @return the minimumSeconds
     */
    public static int getMinimumSeconds() {
        return minimumSeconds;
    }

    /**
     * @param minimumSeconds the minimumSeconds to set
     */
    static void setMinimumSeconds(int minimumSeconds) {
        AppSettings.minimumSeconds = minimumSeconds;
    }


    /**
     * @return the keepFix
     */
    public static boolean shouldkeepFix() {
        return keepFix;
    }

    /**
     * @param keepFix the keepFix to set
     */
    static void setKeepFix(boolean keepFix) {
        AppSettings.keepFix = keepFix;
    }

    /**
     * @return the retryInterval
     */
    public static int getRetryInterval() {
        return retryInterval;
    }

    /**
     * @param retryInterval the retryInterval to set
     */
    static void setRetryInterval(int retryInterval) {
        AppSettings.retryInterval = retryInterval;
    }


    /**
     * @return the minimumDistance
     */
    public static int getMinimumDistanceInMeters() {
        return minimumDistance;
    }

    /**
     * @param minimumDistance the minimumDistance to set
     */
    static void setMinimumDistanceInMeters(int minimumDistance) {
        AppSettings.minimumDistance = minimumDistance;
    }

    /**
     * @return the minimumAccuracy
     */
    public static int getMinimumAccuracyInMeters() {
        return minimumAccuracy;
    }

    /**
     * @param minimumAccuracy the minimumAccuracy to set
     */
    static void setMinimumAccuracyInMeters(int minimumAccuracy) {
        AppSettings.minimumAccuracy = minimumAccuracy;
    }


    /**
     * @return the newFileCreation
     */
    static String getNewFileCreation() {
        return newFileCreation;
    }

    /**
     * @param newFileCreation the newFileCreation to set
     */
    static void setNewFileCreation(String newFileCreation) {
        AppSettings.newFileCreation = newFileCreation;
    }


    /**
     * @return the autoSendDelay
     */
    public static Float getAutoSendDelay() {
            return autoSendDelay;
    }

    /**
     * @param autoSendDelay the autoSendDelay to set
     */
    static void setAutoSendDelay(Float autoSendDelay) {

            AppSettings.autoSendDelay = autoSendDelay;
    }

    /**
     * @return the emailAutoSendEnabled
     */
    public static boolean isEmailAutoSendEnabled() {
        return emailAutoSendEnabled;
    }

    /**
     * @param emailAutoSendEnabled the emailAutoSendEnabled to set
     */
    static void setEmailAutoSendEnabled(boolean emailAutoSendEnabled) {
        AppSettings.emailAutoSendEnabled = emailAutoSendEnabled;
    }


    static void setSmtpServer(String smtpServer) {
        AppSettings.smtpServer = smtpServer;
    }

    public static String getSmtpServer() {
        return smtpServer;
    }

    static void setSmtpPort(String smtpPort) {
        AppSettings.smtpPort = smtpPort;
    }

    public static String getSmtpPort() {
        return smtpPort;
    }

    static void setSmtpUsername(String smtpUsername) {
        AppSettings.smtpUsername = smtpUsername;
    }

    public static String getSmtpUsername() {
        return smtpUsername;
    }


    static void setSmtpPassword(String smtpPassword) {
        AppSettings.smtpPassword = smtpPassword;
    }

    public static String getSmtpPassword() {
        return smtpPassword;
    }

    static void setSmtpSsl(boolean smtpSsl) {
        AppSettings.smtpSsl = smtpSsl;
    }

    public static boolean isSmtpSsl() {
        return smtpSsl;
    }

    static void setAutoEmailTargets(String autoEmailTargets) {
        AppSettings.autoEmailTargets = autoEmailTargets;
    }

    public static String getAutoEmailTargets() {
        return autoEmailTargets;
    }

    public static boolean isDebugToFile() {
        return debugToFile;
    }

    public static void setDebugToFile(boolean debugToFile) {
        AppSettings.debugToFile = debugToFile;
    }


    public static boolean shouldSendZipFile() {
        return shouldSendZipFile;
    }

    public static void setShouldSendZipFile(boolean shouldSendZipFile) {
        AppSettings.shouldSendZipFile = shouldSendZipFile;
    }

    private static String getSmtpFrom() {
        return smtpFrom;
    }

    public static void setSmtpFrom(String smtpFrom) {
        AppSettings.smtpFrom = smtpFrom;
    }

    /**
     * Returns the from value to use when sending an email
     *
     * @return
     */
    public static String getSenderAddress() {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }

    public static boolean isAutoSendEnabled() {
        return autoSendEnabled;
    }

    public static void setAutoSendEnabled(boolean autoSendEnabled) {
        AppSettings.autoSendEnabled = autoSendEnabled;
    }

    public static boolean shouldLogToOpenGTS() {
        return logToOpenGts;
    }

    public static void setLogToOpenGts(boolean logToOpenGts) {
        AppSettings.logToOpenGts = logToOpenGts;
    }

    public static boolean isOpenGtsAutoSendEnabled() {
        return openGtsAutoSendEnabled;
    }

    public static void setOpenGtsAutoSendEnabled(boolean openGtsAutoSendEnabled) {
        AppSettings.openGtsAutoSendEnabled = openGtsAutoSendEnabled;
    }

    public static String getOpenGTSServer() {
        return openGTSServer;
    }

    public static void setOpenGTSServer(String openGTSServer) {
        AppSettings.openGTSServer = openGTSServer;
    }

    public static String getOpenGTSServerPort() {
        return openGTSServerPort;
    }

    public static void setOpenGTSServerPort(String openGTSServerPort) {
        AppSettings.openGTSServerPort = openGTSServerPort;
    }

    public static String getOpenGTSServerCommunicationMethod() {
        return openGTSServerCommunicationMethod;
    }

    public static void setOpenGTSServerCommunicationMethod(String openGTSServerCommunicationMethod) {
        AppSettings.openGTSServerCommunicationMethod = openGTSServerCommunicationMethod;
    }

    public static String getOpenGTSServerPath() {
        return openGTSServerPath;
    }

    public static void setOpenGTSServerPath(String openGTSServerPath) {
        AppSettings.openGTSServerPath = openGTSServerPath;
    }

    public static String getOpenGTSDeviceId() {
        return openGTSDeviceId;
    }

    public static void setOpenGTSDeviceId(String openGTSDeviceId) {
        AppSettings.openGTSDeviceId = openGTSDeviceId;
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

    public static String getCustomFileName() {
        return customFileName;
    }

    public static void setCustomFileName(String customFileName) {
        AppSettings.customFileName = customFileName;
    }

    public static boolean isCustomFile() {
        return isCustomFile;
    }

    public static void setCustomFile(boolean customFile) {
        AppSettings.isCustomFile = customFile;
    }

    public static boolean shouldAskCustomFileNameEachTime() { return askCustomFileNameEachTime; }

    public static void setAskCustomFileNameEachTime(boolean askEachTime) { AppSettings.askCustomFileNameEachTime = askEachTime; }

    public static boolean shouldLogToCustomUrl() {
        return logToCustomUrl;
    }

    public static void setLogToCustomUrl(boolean logToCustomUrl) {
        AppSettings.logToCustomUrl = logToCustomUrl;
    }


    public static String getCustomLoggingUrl() {
        return customLoggingUrl;
    }

    public static void setCustomLoggingUrl(String customLoggingUrl) {
        AppSettings.customLoggingUrl = customLoggingUrl;
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

    public static boolean shouldLogToNmea() {
        return logToNmea;
    }

    public static void setLogToNmea(boolean logToNmea) {
        AppSettings.logToNmea = logToNmea;
    }

    public static String getOpenGTSAccountName() {
        return openGTSAccountName;
    }

    public static void setOpenGTSAccountName(String openGTSAccountName) {
        AppSettings.openGTSAccountName = openGTSAccountName;
    }

    public static void setChosenListeners(Set<String> chosenListeners) {
        AppSettings.chosenListeners = chosenListeners;
    }

    public static Set<String> getChosenListeners() {
        return chosenListeners;
    }

    public static void setAutoSendWhenIPressStop(boolean autoSendWhenIPressStop) {
        AppSettings.autoSendWhenIPressStop = autoSendWhenIPressStop;
    }

    public static boolean shouldAutoSendWhenIPressStop() {
        return autoSendWhenIPressStop;
    }



    public static void setHideNotificationButtons(boolean hideNotificationButtons) {
        AppSettings.hideNotificationButtons = hideNotificationButtons;
    }

    public static boolean shouldHideNotificationButtons() {
        return hideNotificationButtons;
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

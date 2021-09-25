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

package com.mendhak.gpslogger.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import org.slf4j.Logger;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class PreferenceHelper {

    private static PreferenceHelper instance = null;
    private SharedPreferences prefs;
    private static final Logger LOG = Logs.of(PreferenceHelper.class);

    /**
     * Use PreferenceHelper.getInstance()
     */
    private PreferenceHelper(){

    }

    public static PreferenceHelper getInstance(){
        if(instance==null){
            instance = new PreferenceHelper();
            instance.prefs = PreferenceManager.getDefaultSharedPreferences(AppSettings.getInstance().getApplicationContext());
        }

        return instance;
    }

    /**
     * Whether to auto send to Dropbox
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_DROPBOX_ENABLED)
    public  boolean isDropboxAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_DROPBOX_ENABLED, false);
    }

    public  String getDropBoxAccessKeyName() {
        return prefs.getString(PreferenceNames.DROPBOX_ACCESS_KEY, null);
    }

    public  void setDropBoxAccessKeyName(String key) {
        prefs.edit().putString(PreferenceNames.DROPBOX_ACCESS_KEY, key).apply();
    }


    /**
     * Legacy - only used to check if user is still on Oauth1 and to upgrade them.
     * @return
     */
    public String getDropBoxOauth1Secret() {
        return prefs.getString(PreferenceNames.DROPBOX_ACCESS_SECRET, null);
    }

    public void setDropBoxOauth1Secret(String secret) {
        prefs.edit().putString(PreferenceNames.DROPBOX_ACCESS_SECRET, secret).apply();
    }




    /**
     * Whether automatic sending to email is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_EMAIL_ENABLED)
    public boolean isEmailAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_EMAIL_ENABLED, false);
    }


    /**
     * SMTP Server to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_SERVER)
    public String getSmtpServer() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_SERVER, "");
    }

    /**
     * Sets SMTP Server to use when sending emails
     */
    public void setSmtpServer(String smtpServer) {
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_SERVER, smtpServer).apply();
    }

    /**
     * SMTP Port to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_PORT)
    public String getSmtpPort() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_PORT, "25");
    }

    public void setSmtpPort(String port) {
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_PORT, port).apply();
    }

    /**
     * SMTP Username to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_USERNAME)
    public String getSmtpUsername() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_USERNAME, "");
    }

    public void setSmtpUsername(String user){
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_USERNAME, user).apply();
    }

    /**
     * SMTP Password to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_PASSWORD)
    public String getSmtpPassword() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_PASSWORD, "");
    }

    public void setSmtpPassword(String pass){
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_PASSWORD, pass).apply();
    }

    /**
     * Whether SSL is enabled when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_SSL)
    public boolean isSmtpSsl() {
        return prefs.getBoolean(PreferenceNames.EMAIL_SMTP_SSL, true);
    }

    /**
     * Sets whether SSL is enabled when sending emails
     */
    public void setSmtpSsl(boolean smtpSsl) {
        prefs.edit().putBoolean(PreferenceNames.EMAIL_SMTP_SSL, smtpSsl).apply();
    }


    /**
     * Email addresses to send to
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_TARGET)
    public String getAutoEmailTargets() {
        return prefs.getString(PreferenceNames.EMAIL_TARGET, "");
    }

    public void setAutoEmailTargets(String emailCsv) {
        prefs.edit().putString(PreferenceNames.EMAIL_TARGET, emailCsv).apply();
    }


    /**
     * SMTP from address to use
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_FROM)
    private String getSmtpFrom() {
        return prefs.getString(PreferenceNames.EMAIL_FROM, "");
    }

    public void setSmtpFrom(String from) {
        prefs.edit().putString(PreferenceNames.EMAIL_FROM, from).apply();
    }

    /**
     * The from address to use when sending an email, uses {@link #getSmtpUsername()} if {@link #getSmtpFrom()} is not specified
     */
    public String getSmtpSenderAddress() {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }


    /**
     * FTP Server name for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_SERVER)
    public String getFtpServerName() {
        return prefs.getString(PreferenceNames.FTP_SERVER, "");
    }


    /**
     * FTP Port for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PORT)
    public int getFtpPort() {
        return Strings.toInt(prefs.getString(PreferenceNames.FTP_PORT, "21"), 21);
    }

    public void setFtpPort(String port){
        prefs.edit().putString(PreferenceNames.FTP_PORT, port).apply();
    }

    /**
     * FTP Username for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_USERNAME)
    public String getFtpUsername() {
        return prefs.getString(PreferenceNames.FTP_USERNAME, "");
    }


    /**
     * FTP Password for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PASSWORD)
    public String getFtpPassword() {
        return prefs.getString(PreferenceNames.FTP_PASSWORD, "");
    }

    public void setFtpPassword(String pass){
        prefs.edit().putString(PreferenceNames.FTP_PASSWORD, pass).apply();
    }

    /**
     * Whether to use FTPS
     */
    @ProfilePreference(name= PreferenceNames.FTP_USE_FTPS)
    public boolean shouldFtpUseFtps() {
        return prefs.getBoolean(PreferenceNames.FTP_USE_FTPS, false);
    }


    /**
     * FTP protocol to use (SSL or TLS)
     */
    @ProfilePreference(name= PreferenceNames.FTP_SSLORTLS)
    public String getFtpProtocol() {
        return prefs.getString(PreferenceNames.FTP_SSLORTLS, "");
    }


    /**
     * Whether to use FTP Implicit mode for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_IMPLICIT)
    public boolean isFtpImplicit() {
        return prefs.getBoolean(PreferenceNames.FTP_IMPLICIT, false);
    }


    /**
     * Whether to auto send to FTP target
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_FTP_ENABLED)
    public boolean isFtpAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_FTP_ENABLED, false);
    }


    /**
     * FTP Directory on the server for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_DIRECTORY)
    public String getFtpDirectory() {
        return prefs.getString(PreferenceNames.FTP_DIRECTORY, "GPSLogger");
    }




    /**
     * GPS Logger folder path on phone.  Falls back to {@link Files#storageFolder(Context)} if nothing specified.
     */
    @ProfilePreference(name= PreferenceNames.GPSLOGGER_FOLDER)
    public String getGpsLoggerFolder() {
        return prefs.getString(PreferenceNames.GPSLOGGER_FOLDER, Files.storageFolder(AppSettings.getInstance().getApplicationContext()).getAbsolutePath());
    }


    /**
     * Sets GPS Logger folder path
     */
    public void setGpsLoggerFolder(String folderPath) {
        prefs.edit().putString(PreferenceNames.GPSLOGGER_FOLDER, folderPath).apply();
    }



    /**
     * The minimum seconds interval between logging points
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_INTERVAL)
    public int getMinimumLoggingInterval() {
        return Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_INTERVAL, "60"), 60);
    }

    /**
     * Sets the minimum time interval between logging points
     *
     * @param minimumSeconds - in seconds
     */
    public void setMinimumLoggingInterval(int minimumSeconds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.MINIMUM_INTERVAL, String.valueOf(minimumSeconds));
        editor.apply();
    }


    /**
     * The minimum distance, in meters, to have traveled before a point is recorded
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_DISTANCE)
    public int getMinimumDistanceInterval() {
        return (Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_DISTANCE, "0"), 0));
    }

    /**
     * Sets the minimum distance to have traveled before a point is recorded
     *
     * @param distanceBeforeLogging - in meters
     */
    public void setMinimumDistanceInMeters(int distanceBeforeLogging) {
        prefs.edit().putString(PreferenceNames.MINIMUM_DISTANCE, String.valueOf(distanceBeforeLogging)).apply();
    }


    /**
     * The minimum accuracy of a point before the point is recorded, in meters
     */
    @ProfilePreference(name= PreferenceNames.MINIMUM_ACCURACY)
    public int getMinimumAccuracy() {
        return (Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_ACCURACY, "40"), 40));
    }

    public void setMinimumAccuracy(int minimumAccuracy){
        prefs.edit().putString(PreferenceNames.MINIMUM_ACCURACY, String.valueOf(minimumAccuracy)).apply();
    }


    /**
     * Whether to keep GPS on between fixes
     */
    @ProfilePreference(name= PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES)
    public boolean shouldKeepGPSOnBetweenFixes() {
        return prefs.getBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, false);
    }

    /**
     * Set whether to keep GPS on between fixes
     */
    public void setShouldKeepGPSOnBetweenFixes(boolean keepFix) {
        prefs.edit().putBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, keepFix).apply();
    }


    /**
     * How long to keep retrying for a fix if one with the user-specified accuracy hasn't been found
     */
    @ProfilePreference(name= PreferenceNames.LOGGING_RETRY_TIME)
    public int getLoggingRetryPeriod() {
        return (Strings.toInt(prefs.getString(PreferenceNames.LOGGING_RETRY_TIME, "60"), 60));
    }


    /**
     * Sets how long to keep trying for an accurate fix
     *
     * @param retryInterval in seconds
     */
    public void setLoggingRetryPeriod(int retryInterval) {
        prefs.edit().putString(PreferenceNames.LOGGING_RETRY_TIME, String.valueOf(retryInterval)).apply();
    }

    /**
     * How long to keep retrying for an accurate point before giving up
     */
    @ProfilePreference(name= PreferenceNames.ABSOLUTE_TIMEOUT)
    public int getAbsoluteTimeoutForAcquiringPosition() {
        return (Strings.toInt(prefs.getString(PreferenceNames.ABSOLUTE_TIMEOUT, "120"), 120));
    }

    /**
     * Sets how long to keep retrying for an accurate point before giving up
     *
     * @param absoluteTimeout in seconds
     */
    public void setAbsoluteTimeoutForAcquiringPosition(int absoluteTimeout) {
        prefs.edit().putString(PreferenceNames.ABSOLUTE_TIMEOUT, String.valueOf(absoluteTimeout)).apply();
    }

    /**
     * Whether to start logging on application launch
     */
    @ProfilePreference(name= PreferenceNames.START_LOGGING_ON_APP_LAUNCH)
    public boolean shouldStartLoggingOnAppLaunch() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_APP_LAUNCH, false);
    }

    /**
     * Whether to start logging when phone is booted up
     */
    @ProfilePreference(name= PreferenceNames.START_LOGGING_ON_BOOTUP)
    public boolean shouldStartLoggingOnBootup() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_BOOTUP, false);
    }


    /**
     * Which navigation item the user selected
     */
    public int getUserSelectedNavigationItem() {
        return Strings.toInt(prefs.getString(PreferenceNames.SELECTED_NAVITEM, "0"), 0);
    }

    /**
     * Sets which navigation item the user selected
     */
    public void setUserSelectedNavigationItem(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.SELECTED_NAVITEM, String.valueOf(position));
        editor.apply();
    }

    /**
     * Whether to hide the buttons when displaying the app notification
     */
    @ProfilePreference(name= PreferenceNames.HIDE_NOTIFICATION_BUTTONS)
    public boolean shouldHideNotificationButtons() {
        return prefs.getBoolean(PreferenceNames.HIDE_NOTIFICATION_BUTTONS, false);
    }


    @ProfilePreference(name=PreferenceNames.HIDE_NOTIFICATION_FROM_STATUS_BAR)
    public boolean shouldHideNotificationFromStatusBar(){
        return prefs.getBoolean(PreferenceNames.HIDE_NOTIFICATION_FROM_STATUS_BAR, false);
    }

    /**
     * Whether to display certain values using imperial units
     */
    @ProfilePreference(name= PreferenceNames.DISPLAY_IMPERIAL)
    public boolean shouldDisplayImperialUnits() {
        return prefs.getBoolean(PreferenceNames.DISPLAY_IMPERIAL, false);
    }

    /**
     * Display format to use for lat long coordinates on screen
     * DEGREES_MINUTES_SECONDS, DEGREES_DECIMAL_MINUTES, DECIMAL_DEGREES
     */
    @ProfilePreference(name=PreferenceNames.LATLONG_DISPLAY_FORMAT)
    public PreferenceNames.DegreesDisplayFormat getDisplayLatLongFormat(){
        String chosenValue = prefs.getString(PreferenceNames.LATLONG_DISPLAY_FORMAT,"DEGREES_MINUTES_SECONDS");
        return PreferenceNames.DegreesDisplayFormat.valueOf(chosenValue);
    }

    public void setDisplayLatLongFormat(PreferenceNames.DegreesDisplayFormat displayFormat){
        prefs.edit().putString(PreferenceNames.LATLONG_DISPLAY_FORMAT, displayFormat.toString()).apply();
    }


    /**
     * Whether to log to KML file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_KML)
    public boolean shouldLogToKml() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_KML, false);
    }


    /**
     * Whether to log to GPX file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_GPX)
    public boolean shouldLogToGpx() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_GPX, true);
    }

    /**
     * Whether to log to GPX in GPX 1.0 or 1.1 format
     */
    @ProfilePreference(name= PreferenceNames.LOG_AS_GPX_11)
    public boolean shouldLogAsGpx11() {
        return prefs.getBoolean(PreferenceNames.LOG_AS_GPX_11, false);
    }


    /**
     * Whether to log to a CSV file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_CSV)
    public boolean shouldLogToCSV() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_CSV, false);
    }

    /**
     * Whether to log to a GeoJSON file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_GEOJSON)
    public boolean shouldLogToGeoJSON() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_GEOJSON, false);
    }


    /**
     * Whether to log to NMEA file
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_NMEA)
    public boolean shouldLogToNmea() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_NMEA, false);
    }


    /**
     * Whether to log to a custom URL. The app will log to the URL returned by {@link #getCustomLoggingUrl()}
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_URL)
    public boolean shouldLogToCustomUrl() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_URL, false);
    }

    @ProfilePreference(name=PreferenceNames.LOG_TO_URL_METHOD)
    public String getCustomLoggingHTTPMethod(){
        return prefs.getString(PreferenceNames.LOG_TO_URL_METHOD, "GET");
    }

    public void setCustomLoggingHTTPMethod(String method){
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_METHOD, method).apply();
    }

    @ProfilePreference(name=PreferenceNames.LOG_TO_URL_BODY)
    public String getCustomLoggingHTTPBody(){
        return prefs.getString(PreferenceNames.LOG_TO_URL_BODY,"");
    }

    @ProfilePreference(name=PreferenceNames.LOG_TO_URL_HEADERS)
    public String getCustomLoggingHTTPHeaders(){
        return prefs.getString(PreferenceNames.LOG_TO_URL_HEADERS,"");
    }

    public void setCustomLoggingHTTPHeaders(String headers){
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_HEADERS, headers).apply();
    }

    @ProfilePreference(name=PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME)
    public String getCustomLoggingBasicAuthUsername() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME, "");
    }

    public void setCustomLoggingBasicAuthUsername(String username) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME, username).apply();
    }

    @ProfilePreference(name=PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD)
    public String getCustomLoggingBasicAuthPassword() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD, "");
    }

    public void setCustomLoggingBasicAuthPassword(String password) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD, password).apply();
    }

    /**
     * The custom URL to log to.  Relevant only if {@link #shouldLogToCustomUrl()} returns true.
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_URL_PATH)
    public String getCustomLoggingUrl() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_PATH, "http://localhost/log?lat=%LAT&longitude=%LON&time=%TIME&s=%SPD");
    }

    /**
     * Sets custom URL to log to, if {@link #shouldLogToCustomUrl()} returns true.
     */
    public void setCustomLoggingUrl(String customLoggingUrl) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_PATH, customLoggingUrl).apply();
    }

    /**
     * Whether to log to OpenGTS.  See their <a href="http://opengts.sourceforge.net/OpenGTS_Config.pdf">installation guide</a>
     */
    @ProfilePreference(name= PreferenceNames.LOG_TO_OPENGTS)
    public boolean shouldLogToOpenGTS() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_OPENGTS, false);
    }




    @ProfilePreference(name=PreferenceNames.LOG_PASSIVE_LOCATIONS)
    public boolean shouldLogPassiveLocations(){
        return prefs.getBoolean(PreferenceNames.LOG_PASSIVE_LOCATIONS, false);
    }


    public void setShouldLogPassiveLocations(boolean value){
        prefs.edit().putBoolean(PreferenceNames.LOG_PASSIVE_LOCATIONS, value).apply();
    }


    @ProfilePreference(name = PreferenceNames.LOG_SATELLITE_LOCATIONS)
    public boolean shouldLogSatelliteLocations(){
        return  prefs.getBoolean(PreferenceNames.LOG_SATELLITE_LOCATIONS, true);
    }

    public void setShouldLogSatelliteLocations(boolean value){
        prefs.edit().putBoolean(PreferenceNames.LOG_SATELLITE_LOCATIONS, value).apply();
    }

    @ProfilePreference(name = PreferenceNames.LOG_NETWORK_LOCATIONS)
    public boolean shouldLogNetworkLocations(){
        return prefs.getBoolean(PreferenceNames.LOG_NETWORK_LOCATIONS, true);
    }

    public void setShouldLogNetworkLocations(boolean value){
        prefs.edit().putBoolean(PreferenceNames.LOG_NETWORK_LOCATIONS, value).apply();
    }




    /**
     * New file creation preference:
     * onceamonth - once a month,
     * onceaday - once a day,
     * customfile - custom file (static),
     * everystart - every time the service starts
     */
    @ProfilePreference(name=PreferenceNames.NEW_FILE_CREATION_MODE)
    public String getNewFileCreationMode() {
        return prefs.getString(PreferenceNames.NEW_FILE_CREATION_MODE, "onceaday");
    }


    /**
     * Whether a new file should be created daily
     */
    public boolean shouldCreateNewFileOnceADay() {
        return (getNewFileCreationMode().equals("onceaday"));
    }


    /**
     * Whether a new file should be created monthly
     */
    public boolean shouldCreateNewFileOnceAMonth() {
        return (getNewFileCreationMode().equals("onceamonth"));
    }


    /**
     * Whether only a custom file should be created
     */
    public boolean shouldCreateCustomFile() {
        return getNewFileCreationMode().equals("custom") || getNewFileCreationMode().equals("static");
    }


    /**
     * The custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name= PreferenceNames.CUSTOM_FILE_NAME)
    public String getCustomFileName() {
        return prefs.getString(PreferenceNames.CUSTOM_FILE_NAME, "gpslogger");
    }


    /**
     * Sets custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    public void setCustomFileName(String customFileName) {
        prefs.edit().putString(PreferenceNames.CUSTOM_FILE_NAME, customFileName).apply();
    }

    /**
     * Whether to prompt for a custom file name each time logging starts, if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name= PreferenceNames.ASK_CUSTOM_FILE_NAME)
    public boolean shouldAskCustomFileNameEachTime() {
        return prefs.getBoolean(PreferenceNames.ASK_CUSTOM_FILE_NAME, true);
    }

    /**
     * Whether automatic sending to various targets (email,ftp, dropbox, etc) is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ENABLED)
    public boolean isAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ENABLED, false);
    }


    /**
     * The time, in minutes, before files are sent to the auto-send targets
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_FREQUENCY)
    public int getAutoSendInterval() {
        return Math.round(Float.parseFloat(prefs.getString(PreferenceNames.AUTOSEND_FREQUENCY, "60")));
    }

    public void setAutoSendInterval(String frequency){
        prefs.edit().putString(PreferenceNames.AUTOSEND_FREQUENCY, frequency).apply();
    }


    /**
     * Whether to auto send to targets when logging is stopped
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ON_STOP)
    public boolean shouldAutoSendOnStopLogging() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ON_STOP, false);
    }

    public void setDebugToFile(boolean writeToFile) {
        prefs.edit().putBoolean(PreferenceNames.DEBUG_TO_FILE, writeToFile).apply();
    }

    /**
     * Whether to write log messages to a debuglog.txt file
     */
    public boolean shouldDebugToFile() {
        return prefs.getBoolean(PreferenceNames.DEBUG_TO_FILE, false);
    }


    /**
     * Whether to zip the files up before auto sending to targets
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_ZIP)
    public boolean shouldSendZipFile() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ZIP, true);
    }


    /**
     * Whether to auto send to OpenGTS Server
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OPENGTS_ENABLED)
    public boolean isOpenGtsAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OPENGTS_ENABLED, false);
    }


    /**
     * OpenGTS Server name
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_SERVER)
    public String getOpenGTSServer() {
        return prefs.getString(PreferenceNames.OPENGTS_SERVER, "");
    }


    /**
     * OpenGTS Server Port
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_PORT)
    public String getOpenGTSServerPort() {
        return prefs.getString(PreferenceNames.OPENGTS_PORT, "");
    }


    /**
     * Communication method when talking to OpenGTS (either UDP or HTTP)
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_PROTOCOL)
    public String getOpenGTSServerCommunicationMethod() {
        return prefs.getString(PreferenceNames.OPENGTS_PROTOCOL, "");
    }


    /**
     * OpenGTS Server Path
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_SERVER_PATH)
    public String getOpenGTSServerPath() {
        return prefs.getString(PreferenceNames.OPENGTS_SERVER_PATH, "");
    }


    /**
     * Device ID for OpenGTS communication
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_DEVICE_ID)
    public String getOpenGTSDeviceId() {
        return prefs.getString(PreferenceNames.OPENGTS_DEVICE_ID, "");
    }


    /**
     * Account name for OpenGTS communication
     */
    @ProfilePreference(name= PreferenceNames.OPENGTS_ACCOUNT_NAME)
    public String getOpenGTSAccountName() {
        return prefs.getString(PreferenceNames.OPENGTS_ACCOUNT_NAME, "");
    }




    /**
     * Sets OpenStreetMap OAuth Token for auto send
     */
    public void setOSMAccessToken(String token) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN, token).apply();
    }


    /**
     * Gets access token for OpenStreetMap auto send
     */
    public String getOSMAccessToken() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN, "");
    }


    /**
     * Sets OpenStreetMap OAuth secret for auto send
     */
    public void setOSMAccessTokenSecret(String secret) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN_SECRET, secret).apply();
    }

    /**
     * Gets access token secret for OpenStreetMap auto send
     */
    public String getOSMAccessTokenSecret() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_ACCESS_TOKEN_SECRET, "");
    }

    /**
     * Sets request token for OpenStreetMap auto send
     */
    public void setOSMRequestToken(String token) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_REQUEST_TOKEN, token).apply();
    }

    /**
     * Sets request token secret for OpenStreetMap auto send
     */
    public void setOSMRequestTokenSecret(String secret) {
        prefs.edit().putString(PreferenceNames.OPENSTREETMAP_REQUEST_TOKEN_SECRET, secret).apply();
    }

    /**
     * Description of uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name = PreferenceNames.OPENSTREETMAP_DESCRIPTION)
    public String getOSMDescription() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_DESCRIPTION, "");
    }

    /**
     * Tags associated with uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.OPENSTREETMAP_TAGS)
    public String getOSMTags() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_TAGS, "");
    }

    /**
     * Visibility of uploaded trace on OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.OPENSTREETMAP_VISIBILITY)
    public String getOSMVisibility() {
        return prefs.getString(PreferenceNames.OPENSTREETMAP_VISIBILITY, "private");
    }




    /**
     * Whether to auto send to OpenStreetMap
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OSM_ENABLED)
    public boolean isOsmAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OSM_ENABLED, false);
    }





    /**
     * OwnCloud server for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_SERVER)
    public String getOwnCloudServerName() {
        return prefs.getString(PreferenceNames.OWNCLOUD_SERVER, "");
    }


    /**
     * OwnCloud username for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_USERNAME)
    public String getOwnCloudUsername() {
        return prefs.getString(PreferenceNames.OWNCLOUD_USERNAME, "");
    }


    /**
     * OwnCloud password for auto send
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_PASSWORD)
    public String getOwnCloudPassword() {
        return prefs.getString(PreferenceNames.OWNCLOUD_PASSWORD, "");
    }


    /**
     * OwnCloud target directory for autosend
     */
    @ProfilePreference(name= PreferenceNames.OWNCLOUD_DIRECTORY)
    public String getOwnCloudDirectory() {
        return prefs.getString(PreferenceNames.OWNCLOUD_DIRECTORY, "/gpslogger");
    }


    /**
     * Whether to auto send to OwnCloud
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_OWNCLOUD_ENABLED)
    public boolean isOwnCloudAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_OWNCLOUD_ENABLED, false);
    }




    /**
     * Whether to prefix the phone's serial number to the logging file
     */
    @ProfilePreference(name= PreferenceNames.PREFIX_SERIAL_TO_FILENAME)
    public boolean shouldPrefixSerialToFileName() {
        return prefs.getBoolean(PreferenceNames.PREFIX_SERIAL_TO_FILENAME, false);
    }


    /**
     * Whether to subtract GeoID height from the reported altitude to get Mean Sea Level altitude instead of WGS84
     */
    @ProfilePreference(name= PreferenceNames.ALTITUDE_SHOULD_ADJUST)
    public boolean shouldAdjustAltitudeFromGeoIdHeight() {
        return prefs.getBoolean(PreferenceNames.ALTITUDE_SHOULD_ADJUST, false);
    }


    /**
     * How much to subtract from the altitude reported
     */
    @ProfilePreference(name= PreferenceNames.ALTITUDE_SUBTRACT_OFFSET)
    public int getSubtractAltitudeOffset() {
        return Strings.toInt(prefs.getString(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET, "0"), 0);
    }


    /**
     * Whether to autosend only if wifi is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_WIFI_ONLY)
    public boolean shouldAutoSendOnWifiOnly() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_WIFI_ONLY, false);
    }


    @ProfilePreference(name= PreferenceNames.CURRENT_PROFILE_NAME)
    public String getCurrentProfileName() {
        return prefs.getString(PreferenceNames.CURRENT_PROFILE_NAME, AppSettings.getInstance().getString(R.string.profile_default));
    }

    public void setCurrentProfileName(String profileName){
        prefs.edit().putString(PreferenceNames.CURRENT_PROFILE_NAME, profileName).apply();
    }

    /**
     * A preference to keep track of version specific changes.
     */
    @ProfilePreference(name= PreferenceNames.LAST_VERSION_SEEN_BY_USER)
    public int getLastVersionSeen(){
        return Strings.toInt(prefs.getString(PreferenceNames.LAST_VERSION_SEEN_BY_USER, "1"), 1);
    }

    public void setLastVersionSeen(int lastVersionSeen){
        prefs.edit().putString(PreferenceNames.LAST_VERSION_SEEN_BY_USER, String.valueOf(lastVersionSeen)).apply();
    }


    @ProfilePreference(name=PreferenceNames.USER_SPECIFIED_LANGUAGE)
    public String getUserSpecifiedLocale() {
        return prefs.getString(PreferenceNames.USER_SPECIFIED_LANGUAGE, "");
    }

    public void setUserSpecifiedLocale(String userSpecifiedLocale) {
        prefs.edit().putString(PreferenceNames.USER_SPECIFIED_LANGUAGE, userSpecifiedLocale).apply();
    }

    @ProfilePreference(name=PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING)
    public boolean shouldChangeFileNameDynamically() {
        return prefs.getBoolean(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING, true);
    }

    public void setShouldChangeFileNameDynamically(boolean keepChanging){
        prefs.edit().putBoolean(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING, keepChanging).apply();
    }

    public boolean isSFTPEnabled(){
        return prefs.getBoolean(PreferenceNames.SFTP_ENABLED, false);
    }

    public String getSFTPHost(){
        return prefs.getString(PreferenceNames.SFTP_HOST, "127.0.0.1");
    }

    public void setSFTPHost(String host){
        prefs.edit().putString(PreferenceNames.SFTP_HOST, host).apply();
    }

    public int getSFTPPort(){
        return Strings.toInt(prefs.getString(PreferenceNames.SFTP_PORT, "22"),22);
    }

    public void setSFTPPort(String port){
        prefs.edit().putString(PreferenceNames.SFTP_PORT, port).apply();
    }

    public String getSFTPUser(){
        return prefs.getString(PreferenceNames.SFTP_USER, "");
    }

    public void setSFTPUser(String user){
        prefs.edit().putString(PreferenceNames.SFTP_USER, user).apply();
    }

    public String getSFTPPassword(){
        return prefs.getString(PreferenceNames.SFTP_PASSWORD, "");
    }

    public void setSFTPPassword(String pass){
        prefs.edit().putString(PreferenceNames.SFTP_PASSWORD, pass).apply();
    }

    public String getSFTPPrivateKeyFilePath(){
        return prefs.getString(PreferenceNames.SFTP_PRIVATE_KEY_PATH, "");
    }

    public void setSFTPPrivateKeyFilePath(String filePath){
        prefs.edit().putString(PreferenceNames.SFTP_PRIVATE_KEY_PATH, filePath).apply();
    }

    public String getSFTPPrivateKeyPassphrase(){
        return prefs.getString(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE, "");
    }

    public void setSFTPPrivateKeyPassphrase(String pass){
        prefs.edit().putString(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE, pass).apply();
    }

    public String getSFTPKnownHostKey(){
        return prefs.getString(PreferenceNames.SFTP_KNOWN_HOST_KEY, "");
    }

    public void setSFTPKnownHostKey(String hostKey){
        prefs.edit().putString(PreferenceNames.SFTP_KNOWN_HOST_KEY, hostKey).apply();
    }

    public String getSFTPRemoteServerPath(){
        return prefs.getString(PreferenceNames.SFTP_REMOTE_SERVER_PATH, "/tmp");
    }

    public void setSFTPRemoteServerPath(String path){
        prefs.edit().putString(PreferenceNames.SFTP_REMOTE_SERVER_PATH, path).apply();
    }

    @SuppressWarnings("unchecked")
    public void savePropertiesFromPreferences(File f) throws IOException {

        Properties props = new Properties();

        Method[] methods = PreferenceHelper.class.getMethods();
        for(Method m : methods){

            Annotation a = m.getAnnotation(ProfilePreference.class);
            if(a != null){
                try {
                    Object val = m.invoke(this);

                    if(val != null){
                        props.setProperty(((ProfilePreference)a).name(),String.valueOf(val));
                        LOG.debug(((ProfilePreference) a).name() + " : " + String.valueOf(val));
                    }
                    else {
                        LOG.debug("Null value: " + ((ProfilePreference) a).name() + " is null.");
                    }

                } catch (Exception e) {
                    LOG.error("Could not save preferences to profile", e);
                }
            }
        }

        OutputStream outStream = new FileOutputStream(f);
        props.store(outStream,"Warning: This file can contain server names, passwords, email addresses and other sensitive information.");

    }


    /**
     * Sets preferences in a generic manner from a .properties file
     */

    public void setPreferenceFromPropertiesFile(File file) throws IOException {
        Properties props = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
        props.load(reader);

        for (Object key : props.keySet()) {

            SharedPreferences.Editor editor = prefs.edit();
            String value = props.getProperty(key.toString());
            LOG.info("Setting preset property: " + key.toString() + " to " + value.toString());

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
            } else {
                editor.putString(key.toString(), value);
            }
            editor.apply();
        }

    }


}

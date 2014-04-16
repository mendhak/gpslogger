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

//TODO: Simplify email logic (too many methods)
//TODO: Allow messages in IActionListener callback methods
//TODO: Handle case where a fix is not found and GPS gives up - restart alarm somehow?

package com.mendhak.gpslogger;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateFormat;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;
import com.mendhak.gpslogger.senders.AlarmReceiver;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import org.slf4j.LoggerFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GpsLoggingService extends Service implements IActionListener {
    private static NotificationManager gpsNotifyManager;
    private static int NOTIFICATION_ID = 8675309;
    private static IGpsLoggerServiceClient mainServiceClient;
    private final IBinder mBinder = new GpsLoggingBinder();
    LocationManager gpsLocationManager;
    AlarmManager nextPointAlarmManager;
    private Notification nfc = null;
    private boolean forceLogOnce = false;
    private org.slf4j.Logger tracer;
    // ---------------------------------------------------
    // Helpers and managers
    // ---------------------------------------------------
    private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    private LocationManager towerLocationManager;
    private Intent alarmIntent;

    // ---------------------------------------------------

    /**
     * Sets the activity form for this service. The activity form needs to
     * implement IGpsLoggerServiceClient.
     *
     * @param mainForm The calling client
     */
    protected static void SetServiceClient(IGpsLoggerServiceClient mainForm) {
        mainServiceClient = mainForm;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        tracer.debug(".");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Utilities.ConfigureLogbackDirectly(getApplicationContext());
        tracer = LoggerFactory.getLogger(GpsLoggingService.class.getSimpleName());

        tracer.debug(".");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        tracer.debug(".");
        HandleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        tracer.debug(".");
        HandleIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        tracer.warn("GpsLoggingService is being destroyed by Android OS.");
        mainServiceClient = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        tracer.warn("Android is low on memory.");
        super.onLowMemory();
    }

    private void HandleIntent(Intent intent) {

        tracer.debug(".");
        GetPreferences();

        if (intent != null) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                boolean stopRightNow = bundle.getBoolean("immediatestop");
                boolean startRightNow = bundle.getBoolean("immediate");
                boolean sendEmailNow = bundle.getBoolean("emailAlarm");
                boolean getNextPoint = bundle.getBoolean("getnextpoint");

                tracer.debug("stopRightNow - " + String.valueOf(stopRightNow) + ", startRightNow - "
                        + String.valueOf(startRightNow) + ", sendEmailNow - " + String.valueOf(sendEmailNow)
                        + ", getNextPoint - " + String.valueOf(getNextPoint));

                if (startRightNow) {
                    tracer.info("Intent received - Start Logging Now");
                    StartLogging();
                }

                if (stopRightNow) {
                    tracer.info("Intent received - Stop logging now");
                    StopLogging();
                }

                if (sendEmailNow) {

                    tracer.debug("Intent received - Send Email Now");

                    Session.setReadyToBeAutoSent(true);
                    AutoSendLogFile();
                }

                if (getNextPoint && Session.isStarted()) {
                    tracer.debug("Intent received - Get Next Point");
                    StartGpsManager();
                }

            }
        } else {
            // A null intent is passed in if the service has been killed and
            // restarted.
            tracer.debug("Service restarted with null intent. Start logging.");
            StartLogging();

        }
    }

    @Override
    public void OnComplete() {
        Utilities.HideProgress();
    }

    @Override
    public void OnFailure() {
        Utilities.HideProgress();
    }

    /**
     * Sets up the auto email timers based on user preferences.
     */
    public void SetupAutoSendTimers() {
        tracer.debug("Setting up autosend timers. Auto Send Enabled - " + String.valueOf(AppSettings.isAutoSendEnabled())
                + ", Auto Send Delay - " + String.valueOf(Session.getAutoSendDelay()));

        if (AppSettings.isAutoSendEnabled() && Session.getAutoSendDelay() > 0) {
            tracer.debug("Setting up autosend alarm");
            long triggerTime = System.currentTimeMillis()
                    + (long) (Session.getAutoSendDelay() * 60 * 60 * 1000);

            alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
            CancelAlarm();

            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            tracer.debug("Autosend alarm has been set");

        } else {
            if (alarmIntent != null) {
                tracer.debug("alarmIntent was null, canceling alarm");
                CancelAlarm();
            }
        }
    }


    protected void ForceAutoSendNow()
    {

        tracer.debug(".");
        if (AppSettings.isAutoSendEnabled() && Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0)
        {
            if (IsMainFormVisible())
            {
                SetStatus(R.string.autosend_sending);
//                Utilities.ShowProgress(mainServiceClient.GetActivity(), getString(R.string.autosend_sending),
//                        getString(R.string.please_wait));
            }

            tracer.info("Force emailing Log File");
            FileSenderFactory.SendFiles(getApplicationContext(), this);
        }
    }

    private void SetForceLogOnce(boolean flag) {
        forceLogOnce = flag;
    }

    private boolean ForceLogOnce() {
        return forceLogOnce;
    }

    public void LogOnce() {
        tracer.debug(".");

        SetForceLogOnce(true);

        if (Session.isStarted()) {
            StartGpsManager();
        } else {
            Session.setSinglePointMode(true);
            StartLogging();
        }
    }

    private void CancelAlarm() {
        tracer.debug(".");

        if (alarmIntent != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            tracer.debug("Pending alarm intent was null? " + String.valueOf(sender == null));
            am.cancel(sender);
        }
    }

    /**
     * Method to be called if user has chosen to auto email log files when he
     * stops logging
     */
    private void AutoSendLogFileOnStop() {
        tracer.debug("Auto send enabled: " + String.valueOf(AppSettings.isAutoSendEnabled()) + ", "
                + " auto send delay: " + String.valueOf(Session.getAutoSendDelay()));

        // autoSendDelay 0 means send it when you stop logging.
        if (AppSettings.isAutoSendEnabled() && Session.getAutoSendDelay() == 0) {
            Session.setReadyToBeAutoSent(true);
            AutoSendLogFile();
        }
    }

    /**
     * Calls the Auto Email Helper which processes the file and sends it.
     */
    private void AutoSendLogFile() {

        tracer.debug("isReadyToBeAutoSent - " + Session.isReadyToBeAutoSent());

        // Check that auto emailing is enabled, there's a valid location and
        // file name.
        if (Session.getCurrentFileName() != null && Session.getCurrentFileName().length() > 0
                && Session.isReadyToBeAutoSent() && Session.hasValidLocation()) {

            //Don't show a progress bar when auto-sending
            tracer.info("Sending Log File");

            FileSenderFactory.SendFiles(getApplicationContext(), this);
            Session.setReadyToBeAutoSent(true);
            SetupAutoSendTimers();

        }
    }

    /**
     * Gets preferences chosen by the user and populates the AppSettings object.
     * Also sets up email timers if required.
     */
    private void GetPreferences() {
        tracer.debug(".");
        Utilities.PopulateAppSettings(getApplicationContext());

        if (Session.getAutoSendDelay() != AppSettings.getAutoSendDelay()) {
            tracer.debug("Old autoSendDelay - " + String.valueOf(Session.getAutoSendDelay())
                    + "; New -" + String.valueOf(AppSettings.getAutoSendDelay()));
            Session.setAutoSendDelay(AppSettings.getAutoSendDelay());
            SetupAutoSendTimers();
        }

    }

    /**
     * Resets the form, resets file name if required, reobtains preferences
     */
    protected void StartLogging() {
        tracer.debug(".");
        Session.setAddNewTrackSegment(true);

        if (Session.isStarted()) {
            tracer.debug("Session already started, ignoring");
            return;
        }


        try {
            tracer.info("Starting GpsLoggingService in foreground");
            startForeground(NOTIFICATION_ID, new Notification());
        } catch (Exception ex) {
            tracer.error("Could not start GPSLoggingService in foreground. ", ex);
        }


        Session.setStarted(true);

        GetPreferences();
        Notify();
        ResetCurrentFileName(true);
        NotifyClientStarted();
        StartGpsManager();

    }

    /**
     * Asks the main service client to clear its form.
     */
    private void NotifyClientStarted() {
        if (IsMainFormVisible()) {
            mainServiceClient.OnStartLogging();
        }
    }

    /**
     * Stops logging, removes notification, stops GPS manager, stops email timer
     */
    public void StopLogging() {
        tracer.debug("GpsLoggingService.StopLogging");
        Session.setAddNewTrackSegment(true);

        Session.setStarted(false);
        // Email log file before setting location info to null
        AutoSendLogFileOnStop();
        CancelAlarm();
        Session.setCurrentLocationInfo(null);
        Session.setSinglePointMode(false);
        stopForeground(true);

        RemoveNotification();
        StopAlarm();
        StopGpsManager();
        NotifyClientStopped();
    }

    /**
     * Manages the notification in the status bar
     */
    private void Notify() {

        tracer.debug("GpsLoggingService.Notify");

        //Notification is necessary now to prevent service from being killed.
        gpsNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ShowNotification();
    }

    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void RemoveNotification() {
        tracer.debug("GpsLoggingService.RemoveNotification");
        try {
            if (Session.isNotificationVisible()) {
                gpsNotifyManager.cancelAll();
            }
        } catch (Exception ex) {
            tracer.error("RemoveNotification", ex);
        } finally {
            nfc = null;
            Session.setNotificationVisible(false);
        }
    }

    /**
     * Shows a notification icon in the status bar for GPS Logger
     */
    private void ShowNotification() {
        tracer.debug("GpsLoggingService.ShowNotification");
        // What happens when the notification item is clicked
        Intent contentIntent = new Intent(this, GpsMainActivity.class);

        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, contentIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

        NumberFormat nf = new DecimalFormat("###.#####");

        String contentText = getString(R.string.gpslogger_still_running);
        if (Session.hasValidLocation()) {
            contentText = DateFormat.format("HH:mm:ss", Session.getCurrentLocationInfo().getTime()) + ": ";
            contentText += "Lat: " + nf.format(Session.getCurrentLatitude()) + ", Lon: " + nf.format(Session.getCurrentLongitude());
        }

        if (nfc == null) {
            nfc = new Notification(R.drawable.gpsloggericon2, null, System.currentTimeMillis());
            nfc.flags |= Notification.FLAG_ONGOING_EVENT;
        }

        nfc.setLatestEventInfo(getApplicationContext(), getString(R.string.gpslogger_still_running), contentText, pending);

        gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
        Session.setNotificationVisible(true);
    }

    /**
     * Starts the location manager. There are two location managers - GPS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GPS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    private void StartGpsManager() {
        tracer.debug("GpsLoggingService.StartGpsManager");

        GetPreferences();

        if (gpsLocationListener == null) {
            gpsLocationListener = new GeneralLocationListener(this);
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralLocationListener(this);
        }


        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        CheckTowerAndGpsStatus();

        if (Session.isGpsEnabled() && !AppSettings.shouldPreferCellTower()) {
            tracer.info("Requesting GPS location updates");
            // gps satellite based
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 0,
                    gpsLocationListener);

            gpsLocationManager.addGpsStatusListener(gpsLocationListener);

            Session.setUsingGps(true);
        } else if (Session.isTowerEnabled()) {
            tracer.info("Requesting tower location updates");
            Session.setUsingGps(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000, 0,
                    towerLocationListener);

        } else {
            tracer.info("No provider available");
            Session.setUsingGps(false);
            SetStatus(R.string.gpsprovider_unavailable);
            SetFatalMessage(R.string.gpsprovider_unavailable);
            StopLogging();
            return;
        }

        if (mainServiceClient != null) {
            mainServiceClient.OnWaitingForLocation(true);
            Session.setWaitingForLocation(true);
        }

        SetStatus(R.string.started);
    }

    /**
     * This method is called periodically to determine whether the cell tower /
     * gps providers have been enabled, and sets class level variables to those
     * values.
     */
    private void CheckTowerAndGpsStatus() {
        Session.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        Session.setGpsEnabled(gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    /**
     * Stops the location managers
     */
    private void StopGpsManager() {

        tracer.debug("GpsLoggingService.StopGpsManager");

        if (towerLocationListener != null) {
            tracer.debug("Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gpsLocationListener != null) {
            tracer.debug("Removing gpsLocationManager updates");
            gpsLocationManager.removeUpdates(gpsLocationListener);
            gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
        }


        if (mainServiceClient != null) {
            Session.setWaitingForLocation(false);
            mainServiceClient.OnWaitingForLocation(false);
        }
        SetStatus(getString(R.string.stopped));
    }

    /**
     * Sets the current file name based on user preference.
     */
    private void ResetCurrentFileName(boolean newLogEachStart) {

        tracer.debug(".");

        /* Pick up saved settings, if any. (Saved static file) */
        String newFileName = Session.getCurrentFileName();

        /* Update the file name, if required. (New day, Re-start service) */
        if (AppSettings.isStaticFile()) {
            newFileName = AppSettings.getStaticFileName();
            Session.setCurrentFileName(AppSettings.getStaticFileName());
        } else if (AppSettings.shouldCreateNewFileOnceADay()) {
            // 20100114.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        } else if (newLogEachStart) {
            // 20100114183329.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        }

        if (IsMainFormVisible()) {
            tracer.info("File name: " + newFileName);
            mainServiceClient.onFileName(newFileName);
        }

    }

    /**
     * Gives a status message to the main service client to display
     *
     * @param status The status message
     */
    void SetStatus(String status) {
        tracer.info(status);
        if (IsMainFormVisible()) {
            mainServiceClient.OnStatusMessage(status);
        }
    }

    /**
     * Gives an error message to the main service client to display
     *
     * @param messageId ID of string to lookup
     */
    void SetFatalMessage(int messageId) {
        tracer.error(getString(messageId));
        if (IsMainFormVisible()) {
            mainServiceClient.OnFatalMessage(getString(messageId));
        }
    }

    /**
     * Gets string from given resource ID, passes to SetStatus(String)
     *
     * @param stringId ID of string to lookup
     */
    private void SetStatus(int stringId) {
        String s = getString(stringId);
        SetStatus(s);
    }

    /**
     * Notifies main form that logging has stopped
     */
    void NotifyClientStopped() {
        if (IsMainFormVisible()) {
            mainServiceClient.OnStopLogging();
        }
    }

    /**
     * Stops location manager, then starts it.
     */
    void RestartGpsManagers() {
        tracer.debug("GpsLoggingService.RestartGpsManagers");
        StopGpsManager();
        StartGpsManager();
    }

    /**
     * This event is raised when the GeneralLocationListener has a new location.
     * This method in turn updates notification, writes to file, reobtains
     * preferences, notifies main service client and resets location managers.
     *
     * @param loc Location object
     */
    void OnLocationChanged(Location loc) {
        int retryTimeout = Session.getRetryTimeout();

        if (!Session.isStarted()) {
            tracer.debug("OnLocationChanged called, but Session.isStarted is false");
            StopLogging();
            return;
        }

        tracer.debug("GpsLoggingService.OnLocationChanged");


        long currentTimeStamp = System.currentTimeMillis();

        // Wait some time even on 0 frequency so that the UI doesn't lock up

        if ((currentTimeStamp - Session.getLatestTimeStamp()) < 1000) {
            return;
        }

        // Don't do anything until the user-defined time has elapsed
        if (!ForceLogOnce() && (currentTimeStamp - Session.getLatestTimeStamp()) < (AppSettings.getMinimumSeconds() * 1000)) {
            return;
        }

        // Don't do anything until the user-defined accuracy is reached
        if (AppSettings.getMinimumAccuracyInMeters() > 0) {
            if (AppSettings.getMinimumAccuracyInMeters() < Math.abs(loc.getAccuracy())) {
                if (retryTimeout < 50) {
                    Session.setRetryTimeout(retryTimeout + 1);
                    SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached");
                    StopManagerAndResetAlarm(AppSettings.getRetryInterval());
                    return;
                } else {
                    Session.setRetryTimeout(0);
                    SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached and timeout reached");
                    StopManagerAndResetAlarm();
                    return;
                }
            }
        }

        //Don't do anything until the user-defined distance has been traversed
        if (!ForceLogOnce() && AppSettings.getMinimumDistanceInMeters() > 0 && Session.hasValidLocation()) {

            double distanceTraveled = Utilities.CalculateDistance(loc.getLatitude(), loc.getLongitude(),
                    Session.getCurrentLatitude(), Session.getCurrentLongitude());

            if (AppSettings.getMinimumDistanceInMeters() > distanceTraveled) {
                SetStatus("Only " + String.valueOf(Math.floor(distanceTraveled)) + " m traveled.");
                StopManagerAndResetAlarm();
                return;
            }

        }


        tracer.debug("Location to update: " + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getLongitude()));
        ResetCurrentFileName(false);
        Session.setLatestTimeStamp(System.currentTimeMillis());
        Session.setCurrentLocationInfo(loc);
        SetDistanceTraveled(loc);
        Notify();
        WriteToFile(loc);
        GetPreferences();
        StopManagerAndResetAlarm();
        SetForceLogOnce(false);

        if (IsMainFormVisible()) {

            mainServiceClient.OnLocationUpdate(loc);
        }

        if (Session.isSinglePointMode()) {
            tracer.debug("Single point mode - stopping logging now");
            StopLogging();
        }
    }

    private void SetDistanceTraveled(Location loc) {
        // Distance
        if (Session.getPreviousLocationInfo() == null) {
            Session.setPreviousLocationInfo(loc);
        }
        // Calculate this location and the previous location location and add to the current running total distance.
        // NOTE: Should be used in conjunction with 'distance required before logging' for more realistic values.
        double distance = Utilities.CalculateDistance(
                Session.getPreviousLatitude(),
                Session.getPreviousLongitude(),
                loc.getLatitude(),
                loc.getLongitude());
        Session.setPreviousLocationInfo(loc);
        Session.setTotalTravelled(Session.getTotalTravelled() + distance);
    }

    protected void StopManagerAndResetAlarm() {
        tracer.debug("GpsLoggingService.StopManagerAndResetAlarm");
        if (!AppSettings.shouldkeepFix()) {
            StopGpsManager();
        }
        SetAlarmForNextPoint();
    }

    protected void StopManagerAndResetAlarm(int retryInterval) {
        tracer.debug("GpsLoggingService.StopManagerAndResetAlarm_retryInterval");
        if (!AppSettings.shouldkeepFix()) {
            StopGpsManager();
        }
        SetAlarmForNextPoint(retryInterval);
    }

    private void StopAlarm() {
        tracer.debug("GpsLoggingService.StopAlarm");
        Intent i = new Intent(this, GpsLoggingService.class);
        i.putExtra("getnextpoint", true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);
    }

    private void SetAlarmForNextPoint() {

        tracer.debug("GpsLoggingService.SetAlarmForNextPoint");

        Intent i = new Intent(this, GpsLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AppSettings.getMinimumSeconds() * 1000, pi);

    }

    private void SetAlarmForNextPoint(int retryInterval) {

        tracer.debug("GpsLoggingService.SetAlarmForNextPoint_retryInterval");

        Intent i = new Intent(this, GpsLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + retryInterval * 1000, pi);

    }

    /**
     * Calls file helper to write a given location to a file.
     *
     * @param loc Location object
     */
    private void WriteToFile(Location loc) {
        tracer.debug("GpsLoggingService.WriteToFile");
        List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers(getApplicationContext());
        Session.setAddNewTrackSegment(false);
        boolean atLeastOneAnnotationSuccess = false;

        for (IFileLogger logger : loggers) {
            try {
                logger.Write(loc);
                if (Session.hasDescription()) {
                    logger.Annotate(Session.getDescription(), loc);
                    atLeastOneAnnotationSuccess = true;
                }
            } catch (Exception e) {
                SetStatus(R.string.could_not_write_to_file);
            }
        }

        if (atLeastOneAnnotationSuccess) {
            Session.clearDescription();
            if (IsMainFormVisible()) {
                mainServiceClient.OnClearAnnotation();
            }
        }
    }

    /**
     * Informs the main service client of the number of visible satellites.
     *
     * @param count Number of Satellites
     */
    void SetSatelliteInfo(int count) {
        if (IsMainFormVisible()) {
            mainServiceClient.OnSatelliteCount(count);
        }
    }

    private boolean IsMainFormVisible() {
        return mainServiceClient != null;
    }

    /**
     * Can be used from calling classes as the go-between for methods and
     * properties.
     */
    public class GpsLoggingBinder extends Binder {
        public GpsLoggingService getService() {
            tracer.debug("GpsLoggingBinder.getService");
            return GpsLoggingService.this;
        }
    }


}

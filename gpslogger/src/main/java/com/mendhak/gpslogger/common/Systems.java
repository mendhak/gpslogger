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


import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Systems {

    private static final Logger LOG = Logs.of(Systems.class);
    public final static int REQUEST_PERMISSION_CODE=2191;


    public static String getAndroidId() {
        return Settings.Secure.getString(AppSettings.getInstance().getContentResolver(),
                Settings.Secure.ANDROID_ID);

    }

    public static BatteryInfo getBatteryInfo(Context context){
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 0;

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int batteryPercentage = -1;
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (level == -1 || scale == -1) {
            batteryPercentage = 50;
        }
        else {
            batteryPercentage = (int) (((float) level / (float) scale) * 100.0f);
        }

        return new BatteryInfo(batteryPercentage, isCharging);
    }

    public static boolean isPackageInstalled(String targetPackage, Context context){
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    public static String getPackageSignature(String targetPackage, Context context) throws PackageManager.NameNotFoundException, CertificateException, NoSuchAlgorithmException {
        if(isPackageInstalled(targetPackage, context)){
            Signature sig = context.getPackageManager().getPackageInfo(targetPackage, PackageManager.GET_SIGNATURES).signatures[0];
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(sig.toByteArray()));
            String hexString = null;
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert.getEncoded());
            hexString = byte2HexFormatted(publicKey);
            return hexString;
        }
        return "";
    }

    static String byte2HexFormatted(byte[] arr) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1) h = "0" + h;
            if (l > 2) h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (i < (arr.length - 1)) str.append(':');
        }
        return str.toString();
    }


    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        // note that this may return null if no network is currently active
        return cm.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     * https://github.com/yigit/android-priority-jobqueue/blob/master/jobqueue/src/main/java/com/path/android/jobqueue/network/NetworkUtilImpl.java#L60
     */
    @TargetApi(23)
    public static boolean isDozing(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isDeviceIdleMode() &&
                    !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }


    public static boolean locationPermissionsGranted(Context context) {
        int fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);

        return fineCheck == PackageManager.PERMISSION_GRANTED && coarseCheck == PackageManager.PERMISSION_GRANTED;
    }

    public static void setLocale(String userSpecifiedLocale, Context baseContext, Resources resources) {

        if (!Strings.isNullOrEmpty(userSpecifiedLocale)) {
            LOG.debug("Setting language to " + userSpecifiedLocale);

            String language, country="";

            if(userSpecifiedLocale.contains("-")){
                language = userSpecifiedLocale.split("-")[0];
                country = userSpecifiedLocale.split("-")[1];
            }
            else {
                language = userSpecifiedLocale;
            }

            Locale locale = new Locale(language, country);
            Locale.setDefault(locale);
            resources.getConfiguration().locale = locale;
            baseContext.getResources().updateConfiguration(resources.getConfiguration(), baseContext.getResources().getDisplayMetrics());

        }
    }

    /**
     * Whether the user has allowed the permissions absolutely required to run the app.
     * Currently this is location and file storage.
     */
    public static boolean hasUserGrantedAllNecessaryPermissions(Context context){
        boolean granted = hasUserGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION, context)
                && hasUserGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)
                && hasUserGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)
                && hasUserGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE, context);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            granted = granted && hasUserGrantedPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, context);
        }

        return granted;
    }

    static boolean hasUserGrantedPermission(String permissionName, Context context){
        boolean granted = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED;
        LOG.debug("Permission " + permissionName + " : " + granted);
        return granted;
    }

    public static boolean isDarkMode(FragmentActivity activity){
        int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void setAppTheme(String appThemeSetting){

        if(appThemeSetting.equalsIgnoreCase("system")){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }
        }
        else if(appThemeSetting.equalsIgnoreCase("light")){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

    }

    /**
     * Starts a OneTimeWorkRequest with the given worker class and data map and tag. The constraints are set to
     * UNMETERED network type if the user has set the app to only send on wifi. Otherwise it is set to
     * CONNECTED. The initial delay is set to 1 second to avoid the work being enqueued immediately.
     * The backoff criteria is set to exponential with a 30 second initial delay. The tag is used to
     * uniquely identify the work request, and it replaces any existing work with the same tag.
     * @param workerClass
     * @param dataMap
     * @return
     */
    public static void startWorkManagerRequest(Class workerClass, HashMap<String, Object> dataMap, String tag) {

        androidx.work.Data data = new Data.Builder().putAll(dataMap).build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly() ? NetworkType.UNMETERED: NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(workerClass)
                .setConstraints(constraints)
                .setInitialDelay(1, java.util.concurrent.TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static void sendFileUploadedBroadcast(Context context, String[] filePaths, String senderType){
        LOG.debug("Sending a file uploaded broadcast");
        Intent sendIntent = new Intent();
        sendIntent.setAction("com.mendhak.gpslogger.EVENT");
        sendIntent.putExtra("gpsloggerevent", "fileuploaded");
        sendIntent.putExtra("filepaths", filePaths);
        sendIntent.putExtra("sendertype", senderType);
        context.sendBroadcast(sendIntent);
    }

    /**
     * Show an error notification with a warning emoji ⚠️, this is only used for important errors worth notifying the user for.
     * Such as location permissions missing, unable to write to storage.
     * @param context The application context, so that the notification service can be accessed.
     * @param message A single line message to show in the notification.
     */
    public static void showErrorNotification(Context context, String message){
        LOG.debug("Showing fatal notification");

        Intent contentIntent = new Intent(context, GpsMainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, flags);

        NotificationCompat.Builder nfc = new NotificationCompat.Builder(context.getApplicationContext(), NotificationChannelNames.GPSLOGGER_ERRORS)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                //.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.stat_sys_warning))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(context.getString(R.string.error))
                .setContentText(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(message).toString()).setBigContentTitle(context.getString(R.string.error)))
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        notificationManager.notify(NotificationChannelNames.GPSLOGGER_ERRORS_NOTIFICATION_ID, nfc.build());

    }
}

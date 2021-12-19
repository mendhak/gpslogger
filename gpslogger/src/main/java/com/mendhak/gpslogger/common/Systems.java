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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

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

}

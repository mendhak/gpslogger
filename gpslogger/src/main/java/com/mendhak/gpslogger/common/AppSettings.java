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

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;


import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

public class AppSettings extends Application {


    private static AppSettings instance;
    private static Logger LOG;


    @Override
    public void onCreate() {

        Systems.setAppTheme(PreferenceHelper.getInstance().getAppThemeSetting());
        super.onCreate();

        //Configure the slf4j logger
        Logs.configure();
        LOG = Logs.of(this.getClass());
        LOG.debug("SLF4J logging configured");

        //Configure the Event Bus
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        LOG.debug("EventBus configured");

        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(NotificationChannelNames.GPSLOGGER_DEFAULT, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);
            channel.setLockscreenVisibility(PreferenceHelper.getInstance().shouldHideNotificationFromLockScreen() ? Notification.VISIBILITY_PRIVATE : Notification.VISIBILITY_PUBLIC);

            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);

            NotificationChannel channelErrors = new NotificationChannel(NotificationChannelNames.GPSLOGGER_ERRORS, getString(R.string.error), NotificationManager.IMPORTANCE_HIGH);
            channelErrors.enableLights(true);
            channelErrors.enableVibration(true);
            channelErrors.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelErrors.setShowBadge(true);
            manager.createNotificationChannel(channelErrors);

        }
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


}

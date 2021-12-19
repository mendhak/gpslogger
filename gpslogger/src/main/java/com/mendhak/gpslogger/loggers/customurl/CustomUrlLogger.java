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

package com.mendhak.gpslogger.loggers.customurl;

import android.content.Context;
import android.location.Location;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.senders.customurl.CustomUrlManager;


public class CustomUrlLogger implements FileLogger {

    private final String name = "URL";
    private final String customLoggingUrl;
    private final int batteryLevel;
    private final String httpMethod;
    private final String httpBody;
    private final String httpHeaders;
    private final String basicAuthUsername;
    private final String basicAuthPassword;



    public CustomUrlLogger(String customLoggingUrl, Context context, String httpMethod, String httpBody, String httpHeaders, String basicAuthUsername, String basicAuthPassword) {
        this.customLoggingUrl = customLoggingUrl;
        this.batteryLevel = Systems.getBatteryLevel(context);
        this.httpMethod = httpMethod;
        this.httpBody = httpBody;
        this.httpHeaders = httpHeaders;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {

        CustomUrlManager manager = new CustomUrlManager(PreferenceHelper.getInstance());

        SerializableLocation sLoc = new SerializableLocation(loc);

        String finalUrl = manager.getFormattedTextblock(customLoggingUrl, sLoc, description,
                Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(),
                Session.getInstance().getCurrentFormattedFileName(),
                PreferenceHelper.getInstance().getCurrentProfileName(),
                Session.getInstance().getTotalTravelled());
        String finalBody = manager.getFormattedTextblock(httpBody, sLoc, description,
                Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(),
                Session.getInstance().getCurrentFormattedFileName(),
                PreferenceHelper.getInstance().getCurrentProfileName(),
                Session.getInstance().getTotalTravelled());
        String finalHeaders = manager.getFormattedTextblock(httpHeaders, sLoc, description,
                Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(),
                Session.getInstance().getCurrentFormattedFileName(),
                PreferenceHelper.getInstance().getCurrentProfileName(),
                Session.getInstance().getTotalTravelled());


        manager.sendByHttp(finalUrl,httpMethod, finalBody, finalHeaders, basicAuthUsername, basicAuthPassword);

    }


    @Override
    public String getName() {
        return name;
    }
}



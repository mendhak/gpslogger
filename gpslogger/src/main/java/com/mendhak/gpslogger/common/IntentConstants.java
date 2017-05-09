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

public class IntentConstants {
    public final static String IMMEDIATE_STOP = "immediatestop";
    public final static String IMMEDIATE_START =  "immediatestart";
    public static final String AUTOSEND_NOW = "emailAlarm";
    public static final String GET_NEXT_POINT = "getnextpoint";
    public static final String SET_DESCRIPTION = "setnextpointdescription";
    public static final String PREFER_CELLTOWER = "setprefercelltower";
    public static final String TIME_BEFORE_LOGGING = "settimebeforelogging";
    public static final String DISTANCE_BEFORE_LOGGING = "setdistancebeforelogging";
    public static final String GPS_ON_BETWEEN_FIX = "setkeepbetweenfix";
    public static final String RETRY_TIME = "setretrytime";
    public static final String ABSOLUTE_TIMEOUT = "setabsolutetimeout";
    public static final String LOG_ONCE = "logonce";
    public static final String SWITCH_PROFILE = "switchprofile";

    public static final String GET_NEXT_ACCELEROMETER = "getnextaccelerometer";
    public static final String GET_NEXT_MAGNETICFIELD = "getnextmagneticfield";
}

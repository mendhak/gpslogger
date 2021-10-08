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

package com.mendhak.gpslogger.loggers.csv;

import android.location.Location;
import androidx.annotation.Nullable;

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.loggers.Files;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;


public class CSVFileLogger implements FileLogger {

    public static class FIELDS{

        public static final String TIME = "time";
        public static final String LAT = "lat";
        public static final String LON = "lon";
        public static final String ELEVATION = "elevation";
        public static final String ACCURACY = "accuracy";
        public static final String BEARING = "bearing";
        public static final String SPEED = "speed";
        public static final String SATELLITES = "satellites";
        public static final String PROVIDER = "provider";
        public static final String HDOP = "hdop";
        public static final String VDOP = "vdop";
        public static final String PDOP = "pdop";
        public static final String GEOID_HEIGHT = "geoidheight";
        public static final String AGE_OF_DGPS_DATA = "ageofdgpsdata";
        public static final String DGPS_ID = "dgpsid";
        public static final String ACTIVITY = "activity";
        public static final String BATTERY = "battery";
        public static final String ANNOTATION = "annotation";
        public static final String TIMESTAMP_MILLIS = "timestamp_ms";
        public static final String TIME_WITH_OFFSET = "time_offset";
        public static final String DISTANCE = "distance";
        public static final String START_TIMESTAMP_MILLIS = "starttimestamp_ms";
        public static final String PROFILE_NAME = "profile_name";
    }

    private final Integer batteryLevel;
    private File file;
    protected final String name = "TXT";

    public CSVFileLogger(File file, @Nullable Integer batteryLevel) {
        this.file = file;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    public static String[] getCSVFileHeaders(){
        return new String[]{
                CSVFileLogger.FIELDS.TIME,
                CSVFileLogger.FIELDS.LAT,
                CSVFileLogger.FIELDS.LON,
                CSVFileLogger.FIELDS.ELEVATION,
                CSVFileLogger.FIELDS.ACCURACY,
                CSVFileLogger.FIELDS.BEARING,
                CSVFileLogger.FIELDS.SPEED,
                CSVFileLogger.FIELDS.SATELLITES,
                CSVFileLogger.FIELDS.PROVIDER,
                CSVFileLogger.FIELDS.HDOP,
                CSVFileLogger.FIELDS.VDOP,
                CSVFileLogger.FIELDS.PDOP,
                CSVFileLogger.FIELDS.GEOID_HEIGHT,
                CSVFileLogger.FIELDS.AGE_OF_DGPS_DATA,
                CSVFileLogger.FIELDS.DGPS_ID,
                CSVFileLogger.FIELDS.ACTIVITY,
                CSVFileLogger.FIELDS.BATTERY,
                CSVFileLogger.FIELDS.ANNOTATION,
                CSVFileLogger.FIELDS.TIMESTAMP_MILLIS,
                CSVFileLogger.FIELDS.TIME_WITH_OFFSET,
                CSVFileLogger.FIELDS.DISTANCE,
                CSVFileLogger.FIELDS.START_TIMESTAMP_MILLIS,
                CSVFileLogger.FIELDS.PROFILE_NAME
        };
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {

        if(!Files.reallyExists(file)){
            CSVFormat header = CSVFormat.DEFAULT.builder()
                    .setHeader(getCSVFileHeaders())
                    .setDelimiter(PreferenceHelper.getInstance().getCSVDelimiter())
                    .build();
            FileWriter out = new FileWriter(file);
            CSVPrinter printer = new CSVPrinter(out, header);
            printer.close();
            out.close();
        }

        CSVFormat header = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(true)
                .setDelimiter(PreferenceHelper.getInstance().getCSVDelimiter())
                .build();

        FileWriter out = new FileWriter(file, true);
        try (CSVPrinter printer = new CSVPrinter(out, header)) {
            printer.printRecord(
                    Strings.getIsoDateTime(new Date(loc.getTime())),
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.hasAltitude() ? loc.getAltitude() : "",
                    loc.hasAccuracy() ? loc.getAccuracy() : "",
                    loc.hasBearing() ? loc.getBearing() : "",
                    loc.hasSpeed() ? loc.getSpeed() : "",
                    Maths.getBundledSatelliteCount(loc),
                    loc.getProvider(),
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.HDOP))) ? loc.getExtras().getString(BundleConstants.HDOP) : "",
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.VDOP))) ? loc.getExtras().getString(BundleConstants.VDOP) : "",
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.PDOP))) ? loc.getExtras().getString(BundleConstants.PDOP) : "",
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.GEOIDHEIGHT))) ? loc.getExtras().getString(BundleConstants.GEOIDHEIGHT) : "",
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA))) ? loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA) : "",
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DGPSID))) ? loc.getExtras().getString(BundleConstants.DGPSID) : "",
                    "", //Activity detection was removed, but keeping this here for backward compatibility.
                    (batteryLevel != null) ? batteryLevel : "",
                    description,
                    loc.getTime(),
                    Strings.getIsoDateTimeWithOffset(new Date(loc.getTime())),
                    Session.getInstance().getTotalTravelled(),
                    Session.getInstance().getStartTimeStamp(),
                    PreferenceHelper.getInstance().getCurrentProfileName()
            );
        }
        out.close();

        Files.addToMediaDatabase(file, "text/csv");
    }

    @Override
    public String getName() {
        return name;
    }

}

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


    @Override
    public void annotate(String description, Location loc) throws Exception {

        if(!Files.reallyExists(file)){
            CSVFormat header = CSVFormat.DEFAULT.builder().setHeader("time", "lat", "lon", "elevation",
                    "accuracy", "bearing", "speed", "satellites", "provider", "hdop", "vdop", "pdop",
                    "geoidheight", "ageofdgpsdata", "dgpsid", "activity", "battery", "annotation",
                    "timestamp", "timewithoffset", "distance", "starttimestamp", "profilename")
                    .setDelimiter(CSVFormat.DEFAULT.getDelimiterString())
                    .build();
            FileWriter out = new FileWriter(file);
            CSVPrinter printer = new CSVPrinter(out, header);
            printer.close();
            out.close();
        }

        CSVFormat header = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(true)
                .setDelimiter(CSVFormat.DEFAULT.getDelimiterString())
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
                    (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY))) ? loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY) : "",
                    (batteryLevel != null) ? batteryLevel : "",
                    description,
                    loc.getTime()/1000,
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

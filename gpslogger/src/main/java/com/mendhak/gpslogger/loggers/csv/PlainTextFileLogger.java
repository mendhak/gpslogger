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
import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.loggers.Files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;


public class PlainTextFileLogger implements FileLogger {

    private File file;
    protected final String name = "TXT";

    public PlainTextFileLogger(File file) {
        this.file = file;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!file.exists()) {
            file.createNewFile();

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);
            String header = "time,lat,lon,elevation,accuracy,bearing,speed,satellites,provider,hdop,vdop,pdop,geoidheight,ageofdgpsdata,dgpsid,activity\n";
            output.write(header.getBytes());
            output.flush();
            output.close();

        }

        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

        String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
        String csvLine = getCsvLine(loc, dateTimeString);


        output.write(csvLine.getBytes());
        output.flush();
        output.close();
        Files.addToMediaDatabase(file, "text/csv");
    }

    String getCsvLine(Location loc, String dateTimeString) {

        String outputString = String.format(Locale.US, "%s,%f,%f,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s\n", dateTimeString,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.hasAltitude() ? loc.getAltitude() : "",
                loc.hasAccuracy() ? loc.getAccuracy() : "",
                loc.hasBearing() ? loc.getBearing() : "",
                loc.hasSpeed() ? loc.getSpeed() : "",
                Maths.getBundledSatelliteCount(loc),
                loc.getProvider(),
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("HDOP"))) ? loc.getExtras().getString("HDOP") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("VDOP"))) ? loc.getExtras().getString("VDOP") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("PDOP"))) ? loc.getExtras().getString("PDOP") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("GEOIDHEIGHT"))) ? loc.getExtras().getString("GEOIDHEIGHT") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("AGEOFDGPSDATA"))) ? loc.getExtras().getString("AGEOFDGPSDATA") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("DGPSID"))) ? loc.getExtras().getString("DGPSID") : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString("DETECTED_ACTIVITY"))) ? loc.getExtras().getString("DETECTED_ACTIVITY") : ""
        );
        return outputString;
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {

    }

    @Override
    public String getName() {
        return name;
    }

}

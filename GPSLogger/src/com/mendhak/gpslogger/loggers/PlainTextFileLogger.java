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

package com.mendhak.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

import android.location.Location;

import com.mendhak.gpslogger.common.Utilities;


/**
 * Writes a comma separated plain text file.<br/>
 * First line of file is a header with the logged fields: time,lat,lon,elevation,accuracy,bearing,speed
 *
 * @author Jeroen van Wilgenburg
 *         https://github.com/jvwilge/gpslogger/commit/a7d45bcc1d5012513ff2246022ce4da2708adf47
 */
public class PlainTextFileLogger implements IFileLogger
{

    private File file;
    private boolean useSatelliteTime;
    protected final String name = "TXT";

    public PlainTextFileLogger(File file, boolean useSatelliteTime)
    {
        this.file = file;
        this.useSatelliteTime = useSatelliteTime;
    }

    @Override
    public void Write(Location loc) throws Exception
    {
        if (!file.exists())
        {
            file.createNewFile();

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);
            String header = "time,lat,lon,elevation,accuracy,bearing,speed\n";
            output.write(header.getBytes());
            output.flush();
            output.close();
        }

        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

        Date now;

        if (useSatelliteTime)
        {
            now = new Date(loc.getTime());
        }
        else
        {
            now = new Date();
        }

        String dateTimeString = Utilities.GetIsoDateTime(now);

        String outputString = String.format(Locale.US, "%s,%f,%f,%f,%f,%f,%f\n", dateTimeString,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getAltitude(),
                loc.getAccuracy(),
                loc.getBearing(),
                loc.getSpeed());

        output.write(outputString.getBytes());
        output.flush();
        output.close();
    }

    @Override
    public void Annotate(String description, Location loc) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName()
    {
        return name;
    }

}

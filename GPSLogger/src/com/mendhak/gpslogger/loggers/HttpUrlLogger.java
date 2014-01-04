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

import android.location.Location;
import android.os.Build;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Utilities;
import org.apache.commons.net.io.Util;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpUrlLogger implements IFileLogger {

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private final String name = "URL";
    private final int satellites;

    public HttpUrlLogger(int satellites)
    {
        this.satellites = satellites;
    }

    @Override
    public void Write(Location loc) throws Exception {
        Annotate("", loc);
    }

    @Override
    public void Annotate(String description, Location loc) throws Exception {
        HttpUrlLogHandler writeHandler = new HttpUrlLogHandler(loc, description, satellites);
        Utilities.LogDebug(String.format("There are currently %s tasks waiting on the GPX10 EXECUTOR.", EXECUTOR.getQueue().size()));
        EXECUTOR.execute(writeHandler);
    }

    @Override
    public String getName() {
        return name;
    }
}

class HttpUrlLogHandler implements Runnable {

    private Location loc;
    private String annotation;
    private int satellites;

    public HttpUrlLogHandler(Location loc, String annotation, int satellites) {
        this.loc = loc;
        this.annotation = annotation;
        this.satellites = satellites;
    }

    @Override
    public void run() {
        try {
            Utilities.LogDebug("Writing HTTP URL Logger");
            HttpURLConnection conn = null;

            String logUrl = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME";

            logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(loc.getLatitude()));
            logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(loc.getLongitude()));
            logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(satellites));
            logUrl = logUrl.replaceAll("(?i)%desc", String.valueOf(annotation));
            logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(loc.getAltitude()));
            logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(loc.getAccuracy()));
            logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(loc.getBearing()));
            logUrl = logUrl.replaceAll("(?i)%prov", String.valueOf(loc.getProvider()));
            logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(loc.getSpeed()));
            logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Utilities.GetIsoDateTime(new Date(loc.getTime()))));


            Utilities.LogDebug(logUrl);


            if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(logUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            InputStream response = conn.getInputStream();
        } catch (Exception e) {
            Utilities.LogError("HttpUrlLogHandler.run", e);

        }
    }
}
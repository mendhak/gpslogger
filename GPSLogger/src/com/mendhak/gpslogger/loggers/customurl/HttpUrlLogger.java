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

package com.mendhak.gpslogger.loggers.customurl;

import android.location.Location;
import android.os.Build;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.IFileLogger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpUrlLogger implements IFileLogger {

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private final String name = "URL";
    private final int satellites;
    private final String customLoggingUrl;
    private final float batteryLevel;
    private final String androidId;

    public HttpUrlLogger(String customLoggingUrl, int satellites, float batteryLevel, String androidId)
    {
        this.satellites = satellites;
        this.customLoggingUrl = customLoggingUrl;
        this.batteryLevel = batteryLevel;
        this.androidId = androidId;
    }

    @Override
    public void Write(Location loc) throws Exception {
        if(!Session.hasDescription()){
            Annotate("", loc);
        }

    }

    @Override
    public void Annotate(String description, Location loc) throws Exception {
        HttpUrlLogHandler writeHandler = new HttpUrlLogHandler(customLoggingUrl, loc, description, satellites, batteryLevel, androidId);
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
    private String logUrl;
    private float batteryLevel;
    private String androidId;

    public HttpUrlLogHandler(String customLoggingUrl, Location loc, String annotation, int satellites, float batteryLevel, String androidId) {
        this.loc = loc;
        this.annotation = annotation;
        this.satellites = satellites;
        this.logUrl = customLoggingUrl;
        this.batteryLevel = batteryLevel;
        this.androidId = androidId;
    }

    @Override
    public void run() {
        try {
            Utilities.LogDebug("Writing HTTP URL Logger");
            HttpURLConnection conn = null;

            //String logUrl = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV
            // &spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER";

            logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(loc.getLatitude()));
            logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(loc.getLongitude()));
            logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(satellites));
            logUrl = logUrl.replaceAll("(?i)%desc", String.valueOf(URLEncoder.encode(Utilities.HtmlDecode(annotation), "UTF-8")));
            logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(loc.getAltitude()));
            logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(loc.getAccuracy()));
            logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(loc.getBearing()));
            logUrl = logUrl.replaceAll("(?i)%prov", String.valueOf(loc.getProvider()));
            logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(loc.getSpeed()));
            logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Utilities.GetIsoDateTime(new Date(loc.getTime()))));
            logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
            logUrl = logUrl.replaceAll("(?i)%aid", String.valueOf(androidId));
            logUrl = logUrl.replaceAll("(?i)%ser", String.valueOf(Utilities.GetBuildSerial()));



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
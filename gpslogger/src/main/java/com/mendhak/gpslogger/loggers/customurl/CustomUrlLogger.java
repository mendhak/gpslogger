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
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.path.android.jobqueue.JobManager;

import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomUrlLogger implements FileLogger {

    private final String name = "URL";
    private final String customLoggingUrl;
    private final float batteryLevel;
    private final String androidId;

    public CustomUrlLogger(String customLoggingUrl, float batteryLevel, String androidId) {
        this.customLoggingUrl = customLoggingUrl;
        this.batteryLevel = batteryLevel;
        this.androidId = androidId;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {

        AbstractMap.SimpleEntry<String,String> credentials = getBasicAuth(customLoggingUrl);
        String finalUrl  = removeCredentialsFromUrl(customLoggingUrl, credentials.getKey(), credentials.getValue());
        finalUrl = getFormattedUrl(finalUrl, loc, description, androidId, batteryLevel, Strings.getBuildSerial());

        JobManager jobManager = AppSettings.getJobManager();
        jobManager.addJobInBackground(new CustomUrlJob(finalUrl, credentials.getKey(), credentials.getValue(), new UploadEvents.CustomUrl()));
    }

    public String getFormattedUrl(String customLoggingUrl, Location loc, String description, String androidId, float batteryLevel, String buildSerial) throws Exception {

        String logUrl = customLoggingUrl;
        SerializableLocation sLoc = new SerializableLocation(loc);
        logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(sLoc.getLatitude()));
        logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(sLoc.getLongitude()));
        logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(sLoc.getSatelliteCount()));
        logUrl = logUrl.replaceAll("(?i)%desc", String.valueOf(URLEncoder.encode(Strings.htmlDecode(description), "UTF-8")));
        logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(sLoc.getAltitude()));
        logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(sLoc.getAccuracy()));
        logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(sLoc.getBearing()));
        logUrl = logUrl.replaceAll("(?i)%prov", String.valueOf(sLoc.getProvider()));
        logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(sLoc.getSpeed()));
        logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Strings.getIsoDateTime(new Date(sLoc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
        logUrl = logUrl.replaceAll("(?i)%aid", String.valueOf(androidId));
        logUrl = logUrl.replaceAll("(?i)%ser", String.valueOf(buildSerial));
        logUrl = logUrl.replaceAll("(?i)%act", String.valueOf(sLoc.getDetectedActivity()));

        return logUrl;
    }

    public AbstractMap.SimpleEntry<String,String> getBasicAuth(String customLoggingUrl){
        String basicUsername="", basicPassword="";
        Pattern r = Pattern.compile("(\\w+):(\\w+)@.+"); //Looking for http://username:password@example.com/....
        Matcher m =  r.matcher(customLoggingUrl);
        while(m.find()){
            basicUsername = m.group(1);
            basicPassword = m.group(2);
        }

        return new AbstractMap.SimpleEntry<>(basicUsername, basicPassword);
    }

    public String removeCredentialsFromUrl(String customLoggingUrl, String user, String pass){
        return customLoggingUrl.replace(user + ":" + pass+"@","");
    }


    @Override
    public String getName() {
        return name;
    }
}



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

package com.mendhak.gpslogger.senders.opengts;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.birbit.android.jobqueue.JobManager;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlRequest;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlWorker;
import com.mendhak.gpslogger.loggers.opengts.OpenGtsUdpJob;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.GpxReader;
import org.slf4j.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpenGTSManager extends FileSender {

    private static final Logger LOG = Logs.of(OpenGTSManager.class);
    private PreferenceHelper preferenceHelper;
    private int batteryLevel;

    public OpenGTSManager(PreferenceHelper preferenceHelper)  {
        this(preferenceHelper, 0);
    }

    public OpenGTSManager(PreferenceHelper preferenceHelper, int batteryLevel) {
        this.preferenceHelper = preferenceHelper;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public void uploadFile(List<File> files) {
        // Use only gpx
        for (File f : files) {
            if (f.getName().endsWith(".gpx")) {
                String tag = String.valueOf(Objects.hashCode(f.getName()));
                Data data = new Data.Builder()
                        .putString("gpxFilePath", f.getAbsolutePath())
                        .putString("callbackType", "opengts")
                        .build();

                OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(CustomUrlWorker.class, data);
                WorkManager.getInstance(AppSettings.getInstance())
                        .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);

            }
        }
    }

    public void sendLocations(SerializableLocation[] locations){
        if (locations.length > 0) {

            String server = preferenceHelper.getOpenGTSServer();
            int port = Integer.parseInt(preferenceHelper.getOpenGTSServerPort());
            String path = preferenceHelper.getOpenGTSServerPath();
            String deviceId = preferenceHelper.getOpenGTSDeviceId();
            String accountName = preferenceHelper.getOpenGTSAccountName();
            String communication = preferenceHelper.getOpenGTSServerCommunicationMethod();

            if(communication.equalsIgnoreCase("udp")){
                JobManager jobManager = AppSettings.getJobManager();
                jobManager.addJobInBackground(new OpenGtsUdpJob(server, port, accountName, path, deviceId, communication, locations));
            }
            else {
                sendByHttp(deviceId, accountName, locations, communication, path, server, port);
            }

        }
    }

    void sendByHttp(String deviceId, String accountName, SerializableLocation[] locations, String communication, String path, String server, int port) {

        String[] serializedRequests = new String[locations.length];
        for (int i = 0; i < locations.length; i++) {
            String finalUrl = getUrl(deviceId, accountName, locations[i], communication, path, server, port, batteryLevel );
            CustomUrlRequest request = new CustomUrlRequest(finalUrl);
            serializedRequests[i] = Strings.serializeTojson(request);
        }


        String tag = String.valueOf(Objects.hashCode(serializedRequests));
        Data data = new Data.Builder()
                .putStringArray("urlRequests", serializedRequests)
                .putString("callbackType", "opengts")
                .build();

        OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(CustomUrlWorker.class, data);
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);


    }




    /**
     * Encode a location as GPRMC string data.
     * <p/>
     * For details check org.opengts.util.Nmea0183#_parse_GPRMC(String)
     * (OpenGTS source)
     *
     * @param loc location
     * @return GPRMC data
     */
    public static String gprmcEncode(SerializableLocation loc) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("0.000000", dfs);

        String gprmc = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,",
                "$GPRMC",
                getNmeaGprmcTime(new Date(loc.getTime())),
                "A",
                getNmeaGprmcCoordinates(Math.abs(loc.getLatitude())),
                (loc.getLatitude() >= 0) ? "N" : "S",
                getNmeaGprmcCoordinates(Math.abs(loc.getLongitude())),
                (loc.getLongitude() >= 0) ? "E" : "W",
                f.format(Maths.mpsToKnots(loc.getSpeed())),
                f.format(loc.getBearing()),
                getNmeaGprmcDate(new Date(loc.getTime()))
        );

        gprmc += "*" + getNmeaChecksum(gprmc);

        return gprmc;
    }


    public static String getNmeaGprmcTime(Date dateToFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String getNmeaGprmcDate(Date dateToFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String getNmeaGprmcCoordinates(double coord) {
        // “DDDMM.MMMMM”
        int degrees = (int) coord;
        double minutes = (coord - degrees) * 60;

        DecimalFormat df = new DecimalFormat("00.00000", new DecimalFormatSymbols(Locale.US));
        StringBuilder rCoord = new StringBuilder();
        rCoord.append(degrees);
        rCoord.append(df.format(minutes));

        return rCoord.toString();
    }


    public static String getNmeaChecksum(String msg) {
        int chk = 0;
        for (int i = 1; i < msg.length(); i++) {
            chk ^= msg.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase();
        while (chk_s.length() < 2) {
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }


    public static String getUrl(String id, String accountName, SerializableLocation loc, String communication, String path, String server, int port, int batteryLevel) {
        List<AbstractMap.SimpleEntry<String,String>> qparams = new ArrayList<>();
        qparams.add(new AbstractMap.SimpleEntry<>("id", id));
        qparams.add(new AbstractMap.SimpleEntry<>("dev", id));
        if (!Strings.isNullOrEmpty(accountName)) {
            qparams.add(new AbstractMap.SimpleEntry<>("acct", accountName));
        } else {
            qparams.add(new AbstractMap.SimpleEntry<>("acct", id));
        }

        //OpenGTS 2.5.5 requires batt param or it throws exception...
        qparams.add(new AbstractMap.SimpleEntry<>("batt", String.valueOf(batteryLevel)));
        qparams.add(new AbstractMap.SimpleEntry<>("code", "0xF020"));
        qparams.add(new AbstractMap.SimpleEntry<>("alt", String.valueOf(loc.getAltitude())));
        qparams.add(new AbstractMap.SimpleEntry<>("gprmc", OpenGTSManager.gprmcEncode(loc)));

        if(path.startsWith("/")){
            path = path.replaceFirst("/","");
        }

        return String.format("%s://%s:%d/%s?%s",communication.toLowerCase(),server,port,path,getQuery(qparams));

    }

    private static String getQuery(List<AbstractMap.SimpleEntry<String, String>> params)
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry<String, String> pair : params)
        {
            if (first) {
                first = false;
            }
            else {
                result.append("&");
            }

            result.append(pair.getKey());
            result.append("=");
            result.append(pair.getValue());
        }

        return result.toString();
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServer())
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerPort())
                && Strings.toInt(preferenceHelper.getOpenGTSServerPort(), 0) != 0
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerCommunicationMethod())
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSDeviceId());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isOpenGtsAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.OPENGTS;
    }

    public List<CustomUrlRequest> getCustomUrlRequestsFromGPX(File f) {
        List<CustomUrlRequest> requests = new ArrayList<>();
        try {
            List<SerializableLocation> locations = GpxReader.getPoints(f);
            LOG.debug(locations.size() + " points were read from " + f.getName());
            for (SerializableLocation location : locations) {
                String finalUrl = getUrl(preferenceHelper.getOpenGTSDeviceId(),
                        preferenceHelper.getOpenGTSAccountName(),
                        location,
                        preferenceHelper.getOpenGTSServerCommunicationMethod(),
                        preferenceHelper.getOpenGTSServerPath(),
                        preferenceHelper.getOpenGTSServer(),
                        Integer.valueOf(preferenceHelper.getOpenGTSServerPort()),
                        batteryLevel);
                CustomUrlRequest request = new CustomUrlRequest(finalUrl);
                requests.add(request);
            }
        } catch (Exception e) {
            LOG.error("OpenGTSManager.getCustomUrlRequestsFromGPX", e);
        }
        return requests;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }
}
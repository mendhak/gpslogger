package com.mendhak.gpslogger.senders.customurl;

import android.location.Location;
import android.os.Bundle;

import com.birbit.android.jobqueue.JobManager;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlJob;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlRequest;
import com.mendhak.gpslogger.loggers.opengts.OpenGtsUdpJob;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.GpxReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CustomUrlManager extends FileSender {

    private final PreferenceHelper preferenceHelper;
    private static final Logger LOG = Logs.of(CustomUrlManager.class);

    public CustomUrlManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        //TODO: Read from CSV file.
        // Convert each line to a Serializable Location.
        // Send each location using JobManager Custom Url Job.

        for (File f : files) {
            if (f.getName().endsWith(".csv")) {
                List<SerializableLocation> locations = getLocationsFromCSV(f);
                LOG.debug(locations.size() + " points were read from " + f.getName());

                sendLocations(locations.toArray(new SerializableLocation[locations.size()]));

            }
        }
    }
    public void sendLocations(SerializableLocation[] locations){
//        if (locations.length > 0) {
//
//
//            String server = preferenceHelper.getOpenGTSServer();
//            int port = Integer.parseInt(preferenceHelper.getOpenGTSServerPort());
//            String path = preferenceHelper.getOpenGTSServerPath();
//            String deviceId = preferenceHelper.getOpenGTSDeviceId();
//            String accountName = preferenceHelper.getOpenGTSAccountName();
//            String communication = preferenceHelper.getOpenGTSServerCommunicationMethod();
//
//            if(communication.equalsIgnoreCase("udp")){
//                JobManager jobManager = AppSettings.getJobManager();
//                jobManager.addJobInBackground(new OpenGtsUdpJob(server, port, accountName, path, deviceId, communication, locations));
//            }
//            else {
//                sendByHttp(deviceId, accountName, locations, communication, path, server, port);
//            }
//
//        }
    }

    private List<SerializableLocation> getLocationsFromCSV(File f) {
        List<SerializableLocation> locations = new ArrayList<>();
        try {
            Reader in = new FileReader(f);
            CSVFormat header = CSVFormat.DEFAULT.builder().setHeader("time", "lat", "lon", "elevation",
                    "accuracy", "bearing", "speed", "satellites", "provider", "hdop", "vdop", "pdop",
                    "geoidheight", "ageofdgpsdata", "dgpsid", "activity", "battery", "annotation",
                    "timestamp", "timewithoffset", "distance", "starttimestamp", "profilename")
                    .setDelimiter(CSVFormat.DEFAULT.getDelimiterString())
                    .setSkipHeaderRecord(true)
                    .build();
            Iterable<CSVRecord> records = header.parse(in);
            for(CSVRecord record : records){
                Location csvLoc = new Location(record.get("provider"));
                csvLoc.setTime(Long.parseLong(record.get("timestamp")));
                csvLoc.setLatitude(Double.parseDouble(record.get("lat")));
                csvLoc.setLongitude(Double.parseDouble(record.get("lon")));
                csvLoc.setAltitude(Double.parseDouble(record.get("elevation")));
                csvLoc.setAccuracy(Float.parseFloat(record.get("accuracy")));
                csvLoc.setBearing(Float.parseFloat(record.get("bearing")));
                csvLoc.setSpeed(Float.parseFloat(record.get("speed")));

                Bundle b = new Bundle();
                b.putInt(BundleConstants.SATELLITES_FIX, Integer.parseInt(record.get("satellites")));
                b.putString(BundleConstants.HDOP, record.get("hdop"));
                b.putString(BundleConstants.VDOP, record.get("vdop"));
                b.putString(BundleConstants.PDOP, record.get("pdop"));

                b.putString(BundleConstants.GEOIDHEIGHT, record.get("geoidheight"));
                b.putString(BundleConstants.AGEOFDGPSDATA, record.get("ageofdgpsdata"));
                b.putString(BundleConstants.DGPSID, record.get("dgpsid"));

                csvLoc.setExtras(b);

                SerializableLocation sLock = new SerializableLocation(csvLoc);
                locations.add(sLock);
            }

        } catch (Exception e) {
            LOG.error("Could not read locations from CSV file", e);
        }
        return locations;
    }

    public String getFormattedTextblock(String customLoggingUrl, Location loc, String description, String androidId,
                                        float batteryLevel, String buildSerial, long sessionStartTimeStamp, String fileName, String profileName, double distance)
            throws Exception {

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
        logUrl = logUrl.replaceAll("(?i)%timestamp", String.valueOf(sLoc.getTime()/1000));
        logUrl = logUrl.replaceAll("(?i)%timeoffset", Strings.getIsoDateTimeWithOffset(new Date(sLoc.getTime())));
        logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Strings.getIsoDateTime(new Date(sLoc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%date", String.valueOf(Strings.getIsoCalendarDate(new Date(sLoc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%starttimestamp", String.valueOf(sessionStartTimeStamp/1000));
        logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
        logUrl = logUrl.replaceAll("(?i)%aid", String.valueOf(androidId));
        logUrl = logUrl.replaceAll("(?i)%ser", String.valueOf(buildSerial));
        logUrl = logUrl.replaceAll("(?i)%act", String.valueOf(sLoc.getDetectedActivity()));
        logUrl = logUrl.replaceAll("(?i)%filename", fileName);
        logUrl = logUrl.replaceAll("(?i)%profile",URLEncoder.encode(profileName, "UTF-8"));
        logUrl = logUrl.replaceAll("(?i)%hdop", sLoc.getHDOP());
        logUrl = logUrl.replaceAll("(?i)%vdop", sLoc.getVDOP());
        logUrl = logUrl.replaceAll("(?i)%pdop", sLoc.getPDOP());
        logUrl = logUrl.replaceAll("(?i)%dist", String.valueOf((int)distance));

        return logUrl;
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingUrl()) &&
                !Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingHTTPMethod());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isCustomURLAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.CUSTOMURL;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".csv");
    }
}

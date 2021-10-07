package com.mendhak.gpslogger.senders.customurl;

import android.location.Location;
import android.os.Bundle;

import com.birbit.android.jobqueue.JobManager;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlJob;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlRequest;
import com.mendhak.gpslogger.senders.FileSender;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;
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
        for (File f : files) {
            if (f.getName().endsWith(".csv")) {
                List<SerializableLocation> locations = getLocationsFromCSV(f);
                LOG.debug(locations.size() + " points were read from " + f.getName());

                sendLocations(locations.toArray(new SerializableLocation[locations.size()]));

            }
        }
    }

    private List<SerializableLocation> getLocationsFromCSV(File f) {
        List<SerializableLocation> locations = new ArrayList<>();
        try {
            Reader in = new FileReader(f);
            CSVFormat header = CSVFormat.DEFAULT.builder().setHeader("time", "lat", "lon", "elevation",
                    "accuracy", "bearing", "speed", "satellites", "provider", "hdop", "vdop", "pdop",
                    "geoidheight", "ageofdgpsdata", "dgpsid", "activity", "battery", "annotation",
                    "timestamp_ms", "time_offset", "distance", "starttime_ms", "profile_name")
                    .setDelimiter(CSVFormat.DEFAULT.getDelimiterString())
                    .setSkipHeaderRecord(true)
                    .build();

            Iterable<CSVRecord> records = header.parse(in);
            for(CSVRecord record : records){
                Location csvLoc = new Location(record.get("provider"));
                csvLoc.setTime(Long.parseLong(record.get("timestamp_ms")));
                csvLoc.setLatitude(Double.parseDouble(record.get("lat")));
                csvLoc.setLongitude(Double.parseDouble(record.get("lon")));

                if(!Strings.isNullOrEmpty(record.get("elevation"))){
                    csvLoc.setAltitude(Double.parseDouble(record.get("elevation")));
                }

                if(!Strings.isNullOrEmpty(record.get("accuracy"))){
                    csvLoc.setAccuracy(Float.parseFloat(record.get("accuracy")));
                }


                if(!Strings.isNullOrEmpty(record.get("bearing"))){
                    csvLoc.setBearing(Float.parseFloat(record.get("bearing")));
                }

                if(!Strings.isNullOrEmpty(record.get("speed"))){
                    csvLoc.setSpeed(Float.parseFloat(record.get("speed")));
                }

                Bundle b = new Bundle();

                if(!Strings.isNullOrEmpty(record.get("satellites"))){
                    b.putInt(BundleConstants.SATELLITES_FIX, Integer.parseInt(record.get("satellites")));
                }

                b.putString(BundleConstants.HDOP, record.get("hdop"));
                b.putString(BundleConstants.VDOP, record.get("vdop"));
                b.putString(BundleConstants.PDOP, record.get("pdop"));

                b.putString(BundleConstants.GEOIDHEIGHT, record.get("geoidheight"));
                b.putString(BundleConstants.AGEOFDGPSDATA, record.get("ageofdgpsdata"));
                b.putString(BundleConstants.DGPSID, record.get("dgpsid"));

                if(!Strings.isNullOrEmpty(record.get("battery"))){
                    b.putInt(BundleConstants.BATTERY_LEVEL, Integer.parseInt(record.get("battery")));
                }

                b.putString(BundleConstants.ANNOTATION, record.get("annotation"));
                b.putString(BundleConstants.TIME_WITH_OFFSET, record.get("time_offset"));

                if(!Strings.isNullOrEmpty(record.get("distance"))){
                    b.putDouble(BundleConstants.DISTANCE, Double.parseDouble(record.get("distance")));
                }

                if(!Strings.isNullOrEmpty(record.get("starttime_ms"))){
                    b.putLong(BundleConstants.STARTTIMESTAMP, Long.parseLong(record.get("starttime_ms")));
                }

                b.putString(BundleConstants.PROFILE_NAME, record.get("profile_name"));
                b.putString(BundleConstants.FILE_NAME, f.getName().replace(".csv",""));

                csvLoc.setExtras(b);

                SerializableLocation sLock = new SerializableLocation(csvLoc);
                locations.add(sLock);
            }

        } catch (Exception e) {
            LOG.error("Could not read locations from CSV file", e);
        }
        return locations;
    }

    private void sendLocations(SerializableLocation[] locations){
        if(locations.length > 0){

            String customLoggingUrl = preferenceHelper.getCustomLoggingUrl();
            String httpBody = preferenceHelper.getCustomLoggingHTTPBody();
            String httpHeaders = preferenceHelper.getCustomLoggingHTTPHeaders();
            String httpMethod = preferenceHelper.getCustomLoggingHTTPMethod();

            for(SerializableLocation loc: locations){

                try {
                    String finalUrl = getFormattedTextblock(customLoggingUrl, loc);
                    String finalBody = getFormattedTextblock(httpBody, loc);
                    String finalHeaders = getFormattedTextblock(httpHeaders, loc);

                    sendByHttp(finalUrl, httpMethod, finalBody, finalHeaders,
                            preferenceHelper.getCustomLoggingBasicAuthUsername(),
                            preferenceHelper.getCustomLoggingBasicAuthPassword());

                } catch (Exception e) {
                    LOG.error("Could not build the Custom URL to send", e);
                }
            }
        }

    }

    public void sendByHttp(String url, String method, String body, String headers, String username, String password){
        JobManager jobManager = AppSettings.getJobManager();
        jobManager.addJobInBackground(new CustomUrlJob(new CustomUrlRequest(url, method,
                body, headers, username, password), new UploadEvents.CustomUrl()));
    }


    private String getFormattedTextblock(String textToFormat, SerializableLocation loc) throws Exception {
        return getFormattedTextblock(textToFormat, loc, loc.getDescription(), Systems.getAndroidId(), loc.getBatteryLevel(), Strings.getBuildSerial(), loc.getStartTimeStamp(), loc.getFileName(), loc.getProfileName(), loc.getDistance());
    }

    public String getFormattedTextblock(String customLoggingUrl,
                                        SerializableLocation sLoc,
                                        String description,
                                        String androidId,
                                        float batteryLevel,
                                        String buildSerial,
                                        long sessionStartTimeStamp,
                                        String fileName,
                                        String profileName,
                                        double distance)
            throws Exception {

        String logUrl = customLoggingUrl;

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
        logUrl = logUrl.replaceAll("(?i)%act", ""); //Activity detection was removed, but keeping this here for backward compatibility.
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

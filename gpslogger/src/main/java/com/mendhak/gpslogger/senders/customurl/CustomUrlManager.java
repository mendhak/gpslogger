package com.mendhak.gpslogger.senders.customurl;

import android.content.Context;
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
import com.mendhak.gpslogger.loggers.csv.CSVFileLogger;
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
            CSVFormat header = CSVFormat.DEFAULT.builder().setHeader(
                    CSVFileLogger.getCSVFileHeaders())
                    .setDelimiter(preferenceHelper.getCSVDelimiter())
                    .setSkipHeaderRecord(true)
                    .build();

            Iterable<CSVRecord> records = header.parse(in);
            for(CSVRecord record : records){
                Location csvLoc = new Location(record.get(CSVFileLogger.FIELDS.PROVIDER));
                csvLoc.setTime(Long.parseLong(record.get(CSVFileLogger.FIELDS.TIMESTAMP_MILLIS)));
                csvLoc.setLatitude(Double.parseDouble( unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.LAT)) ));
                csvLoc.setLongitude(Double.parseDouble( unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.LON)) ));

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.ELEVATION))){
                    csvLoc.setAltitude(Double.parseDouble( unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.ELEVATION))));
                }

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.ACCURACY))){
                    csvLoc.setAccuracy(Float.parseFloat( unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.ACCURACY))));
                }


                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.BEARING))){
                    csvLoc.setBearing(Float.parseFloat( unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.BEARING))));
                }

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.SPEED))){
                    csvLoc.setSpeed(Float.parseFloat(unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.SPEED))));
                }

                Bundle b = new Bundle();

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.SATELLITES))){
                    b.putInt(BundleConstants.SATELLITES_FIX, Integer.parseInt(record.get(CSVFileLogger.FIELDS.SATELLITES)));
                }

                b.putString(BundleConstants.HDOP, unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.HDOP)));
                b.putString(BundleConstants.VDOP, unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.VDOP)));
                b.putString(BundleConstants.PDOP, unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.PDOP)));

                b.putString(BundleConstants.GEOIDHEIGHT, unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.GEOID_HEIGHT)));
                b.putString(BundleConstants.AGEOFDGPSDATA, record.get(CSVFileLogger.FIELDS.AGE_OF_DGPS_DATA));
                b.putString(BundleConstants.DGPSID, record.get(CSVFileLogger.FIELDS.DGPS_ID));

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.BATTERY))){
                    b.putInt(BundleConstants.BATTERY_LEVEL, Integer.parseInt(record.get(CSVFileLogger.FIELDS.BATTERY)));
                }

                b.putString(BundleConstants.ANNOTATION, record.get(CSVFileLogger.FIELDS.ANNOTATION));
                b.putString(BundleConstants.TIME_WITH_OFFSET, record.get(CSVFileLogger.FIELDS.TIME_WITH_OFFSET));

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.DISTANCE))){
                    b.putDouble(BundleConstants.DISTANCE, Double.parseDouble(unApplyDecimalComma(record.get(CSVFileLogger.FIELDS.DISTANCE))));
                }

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.START_TIMESTAMP_MILLIS))){
                    b.putLong(BundleConstants.STARTTIMESTAMP, Long.parseLong(record.get(CSVFileLogger.FIELDS.START_TIMESTAMP_MILLIS)));
                }

                b.putString(BundleConstants.PROFILE_NAME, record.get(CSVFileLogger.FIELDS.PROFILE_NAME));
                b.putString(BundleConstants.FILE_NAME, f.getName().replace(".csv",""));

                csvLoc.setExtras(b);

                SerializableLocation sLoc = new SerializableLocation(csvLoc);
                locations.add(sLoc);
            }

        } catch (Exception e) {
            LOG.error("Could not read locations from CSV file", e);
        }
        return locations;
    }

    /**
     * Replace commas with points, in case the CSV contained decimal commas.
     * This is necessary as all the subsequent processing expects decimals
     */
    private String unApplyDecimalComma(String recordValue) {
        return recordValue.replace(",",".");
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
        return getFormattedTextblock(textToFormat, loc, loc.getDescription(), Systems.getAndroidId(), loc.getBatteryLevel(), false, Strings.getBuildSerial(), loc.getStartTimeStamp(), loc.getFileName(), loc.getProfileName(), loc.getDistance());
    }

    public String getFormattedTextblock(String customLoggingUrl,
                                        SerializableLocation sLoc,
                                        String description,
                                        String androidId,
                                        float batteryLevel,
                                        boolean isCharging,
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

        if(!Strings.isNullOrEmpty(sLoc.getTimeWithOffset())){
            logUrl = logUrl.replaceAll("(?i)%timeoffset", sLoc.getTimeWithOffset());
        }
        else {
            logUrl = logUrl.replaceAll("(?i)%timeoffset", Strings.getIsoDateTimeWithOffset(new Date(sLoc.getTime())));
        }


        logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Strings.getIsoDateTime(new Date(sLoc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%date", String.valueOf(Strings.getIsoCalendarDate(new Date(sLoc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%starttimestamp", String.valueOf(sessionStartTimeStamp/1000));
        logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
        logUrl = logUrl.replaceAll("(?i)%charging", String.valueOf(isCharging));
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

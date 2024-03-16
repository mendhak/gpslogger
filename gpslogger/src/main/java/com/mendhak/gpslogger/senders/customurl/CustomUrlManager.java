package com.mendhak.gpslogger.senders.customurl;

import android.location.Location;
import android.os.Bundle;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.csv.CSVFileLogger;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlRequest;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlWorker;
import com.mendhak.gpslogger.senders.FileSender;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomUrlManager extends FileSender {

    private final PreferenceHelper preferenceHelper;
    private static final Logger LOG = Logs.of(CustomUrlManager.class);

    public CustomUrlManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        boolean foundFileToSend = false;
        for (File f : files) {
            if (f.getName().endsWith(".csv")) {
                foundFileToSend = true;

                String tag = String.valueOf(Objects.hashCode(f.getName()));
                Data data = new Data.Builder()
                        .putString("csvFilePath", f.getAbsolutePath())
                        .putString("callbackType", "customUrl")
                        .build();
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(preferenceHelper.shouldAutoSendOnWifiOnly() ? NetworkType.UNMETERED: NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                        .Builder(CustomUrlWorker.class)
                        .setConstraints(constraints)
                        .setInitialDelay(1, java.util.concurrent.TimeUnit.SECONDS)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                        .setInputData(data)
                        .build();
                WorkManager.getInstance(AppSettings.getInstance())
                        .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);

            }
        }

        if(!foundFileToSend){
            LOG.warn("Custom URL auto sender requires a CSV file to be present.");
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

                if(!Strings.isNullOrEmpty(record.get(CSVFileLogger.FIELDS.BATTERY_CHARGING))){
                    b.putBoolean(BundleConstants.BATTERY_CHARGING, Boolean.parseBoolean(record.get(CSVFileLogger.FIELDS.BATTERY_CHARGING)));
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



    public void sendByHttp(String url, String method, String body, String headers, String username, String password){

        CustomUrlRequest request = new CustomUrlRequest(url, method,
                body, headers, username, password);

        String serializedRequest = Strings.serializeTojson(request);

        Data data = new Data.Builder()
                .putStringArray("urlRequests", new String[]{serializedRequest})
                .putString("callbackType", "customUrl")
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(CustomUrlWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(1, java.util.concurrent.TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .setInputData(data)
                .build();
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(serializedRequest, ExistingWorkPolicy.REPLACE, workRequest);
    }

    private String getFormattedTextblock(String textToFormat, SerializableLocation loc) throws Exception {
        return getFormattedTextblock(textToFormat, loc, loc.getDescription(), Systems.getAndroidId(),
                loc.getBatteryLevel(), loc.getBatteryCharging(), Strings.getBuildSerial(),
                loc.getStartTimeStamp(), loc.getFileName(), loc.getProfileName(), loc.getDistance());
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

        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put("lat", String.valueOf(sLoc.getLatitude()));
        replacements.put("lon", String.valueOf(sLoc.getLongitude()));
        replacements.put("sat", String.valueOf(sLoc.getSatelliteCount()));
        replacements.put("desc", String.valueOf(Strings.getUrlEncodedString(Strings.htmlDecode(description))));
        replacements.put("alt", String.valueOf(sLoc.getAltitude()));
        replacements.put("acc", String.valueOf(sLoc.getAccuracy()));
        replacements.put("dir", String.valueOf(sLoc.getBearing()));
        replacements.put("prov", String.valueOf(sLoc.getProvider()));
        replacements.put("spd", String.valueOf(sLoc.getSpeed()));
        replacements.put("timestamp", String.valueOf(sLoc.getTime()/1000));

        if(!Strings.isNullOrEmpty(sLoc.getTimeWithOffset())){
            replacements.put("timeoffset", Strings.getUrlEncodedString(sLoc.getTimeWithOffset()));
        }
        else {
            replacements.put("timeoffset", Strings.getUrlEncodedString(Strings.getIsoDateTimeWithOffset(new Date(sLoc.getTime()))));
        }

        replacements.put("time", String.valueOf(Strings.getUrlEncodedString(Strings.getIsoDateTime(new Date(sLoc.getTime())))));
        replacements.put("starttimestamp", String.valueOf(sessionStartTimeStamp/1000));
        replacements.put("date", String.valueOf(Strings.getIsoCalendarDate(new Date(sLoc.getTime()))));
        replacements.put("batt", String.valueOf(batteryLevel));
        replacements.put("ischarging", String.valueOf(isCharging));
        replacements.put("aid", String.valueOf(androidId));
        replacements.put("ser", String.valueOf(buildSerial));
        replacements.put("act", ""); //Activity detection was removed, but keeping this here for backward compatibility.
        replacements.put("filename", fileName);
        replacements.put("profile", Strings.getUrlEncodedString(profileName));
        replacements.put("hdop", sLoc.getHDOP());
        replacements.put("vdop", sLoc.getVDOP());
        replacements.put("pdop", sLoc.getPDOP());
        replacements.put("dist", String.valueOf((int)distance));

        if(customLoggingUrl.toUpperCase().contains("%ALL")){
            StringBuilder sbAll = new StringBuilder();
            for (String q : replacements.keySet()){
                sbAll.append(q + "=%" + q + "&");
            }
            logUrl = logUrl.replaceAll("(?i)%ALL", sbAll.toString());
        }

        for (String m : replacements.keySet()) {
            logUrl = logUrl.replaceAll("(?i)%"+m, replacements.get(m));
        }

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

    public List<CustomUrlRequest> getCustomUrlRequestsFromCSV(File f) {
        List<SerializableLocation> locations = getLocationsFromCSV(f);
        LOG.debug(locations.size() + " points were read from " + f.getName());

        List<CustomUrlRequest> requests = new ArrayList<>();

        String customLoggingUrl = preferenceHelper.getCustomLoggingUrl();
        String httpBody = preferenceHelper.getCustomLoggingHTTPBody();
        String httpHeaders = preferenceHelper.getCustomLoggingHTTPHeaders();
        String httpMethod = preferenceHelper.getCustomLoggingHTTPMethod();

        for(SerializableLocation loc: locations){
            try {
                String finalUrl = getFormattedTextblock(customLoggingUrl, loc);
                String finalBody = getFormattedTextblock(httpBody, loc);
                String finalHeaders = getFormattedTextblock(httpHeaders, loc);

                requests.add(new CustomUrlRequest(finalUrl, httpMethod,
                        finalBody, finalHeaders, preferenceHelper.getCustomLoggingBasicAuthUsername(),
                        preferenceHelper.getCustomLoggingBasicAuthPassword()));
            } catch (Exception e) {
                LOG.error("Could not build the Custom URL to send", e);
            }
        }

        return requests;

    }

}

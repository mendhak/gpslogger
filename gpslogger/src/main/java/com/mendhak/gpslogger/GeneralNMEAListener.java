package com.mendhak.gpslogger;

import android.location.GpsStatus;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.nmea.NmeaSentence;


public class GeneralNMEAListener {

    public static void processNMEASentence(String nmeaSentence, long timestamp, GeneralLocationListener listener, GpsLoggingService loggingService){

        loggingService.onNmeaSentence(timestamp, nmeaSentence);

        if(Strings.isNullOrEmpty(nmeaSentence)){
            return;
        }

        NmeaSentence nmea = new NmeaSentence(nmeaSentence);

        if(nmea.isLocationSentence()){
            if(nmea.getLatestPdop() != null){
                listener.latestPdop = nmea.getLatestPdop();
            }

            if(nmea.getLatestHdop() != null){
                listener.latestHdop = nmea.getLatestHdop();
            }

            if(nmea.getLatestVdop() != null){
                listener.latestVdop = nmea.getLatestVdop();
            }

            if(nmea.getGeoIdHeight() != null){
                listener.geoIdHeight = nmea.getGeoIdHeight();
            }

            if(nmea.getAgeOfDgpsData() != null){
                listener.ageOfDgpsData = nmea.getAgeOfDgpsData();
            }

            if(nmea.getDgpsId() != null){
                listener.dgpsId = nmea.getDgpsId();
            }

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static class NMEAListener24 implements android.location.OnNmeaMessageListener{

        private final GeneralLocationListener listener;
        private final GpsLoggingService loggingService;

        public NMEAListener24(GeneralLocationListener listener, GpsLoggingService loggingService){
            this.listener = listener;
            this.loggingService = loggingService;
        }

        @Override
        public void onNmeaMessage(String message, long timestamp) {
            processNMEASentence(message,timestamp,listener, loggingService);
        }
    }

    public static class NMEAListenerLegacy implements GpsStatus.NmeaListener {

        private final GeneralLocationListener listener;
        private final GpsLoggingService loggingService;

        public NMEAListenerLegacy(GeneralLocationListener listener, GpsLoggingService loggingService){
            this.listener = listener;
            this.loggingService = loggingService;
        }

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {

            processNMEASentence(nmea, timestamp, listener, loggingService);

        }
    }


}

package com.mendhak.gpslogger.common.events;

import android.location.Location;

public class ServiceEvents {


    public static class StatusMessage {
        public String status;
        public StatusMessage(String message){
            this.status = message;
        }

    }

    public static class FatalMessage {
        public String message;
        public FatalMessage(String message) {
            this.message = message;
        }
    }

    public static class LocationUpdate {
        public Location location;
        public LocationUpdate(Location loc) {
            this.location = loc;
        }
    }

    public static class SatelliteCount {
        public int satelliteCount;
        public SatelliteCount(int count) {
            this.satelliteCount = count;
        }
    }

    public static class WaitingForLocation {
        public boolean waiting;
        public WaitingForLocation(boolean waiting) {
            this.waiting = waiting;
        }
    }

    public static class LocationServicesUnavailable {
    }

    public static class AnnotationStatus {
        public boolean annotationWritten;
        public AnnotationStatus(boolean written){
            this.annotationWritten = written;
        }
    }

    public static class LoggingStatus {
        public boolean loggingStarted;
        public LoggingStatus(boolean loggingStarted) {
            this.loggingStarted = loggingStarted;
        }
    }

    public static class FileNamed {
        public String newFileName;
        public FileNamed(String newFileName) {
            this.newFileName = newFileName;
        }
    }

}

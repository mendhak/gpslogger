package com.mendhak.gpslogger.common.events;

import android.location.Location;

public class ServiceEvents {


    public static class StatusMessageEvent{
        public String status;
        public StatusMessageEvent(String message){
            this.status = message;
        }

    }

    public static class FatalMessageEvent {
        public String message;
        public FatalMessageEvent(String message) {
            this.message = message;
        }
    }

    public static class LocationUpdateEvent {
        public Location location;
        public LocationUpdateEvent(Location loc) {
            this.location = loc;
        }
    }

    public static class SatelliteCountEvent {
        public int satelliteCount;
        public SatelliteCountEvent(int count) {
            this.satelliteCount = count;
        }
    }

    public static class WaitingForLocationEvent {
        public boolean waiting;
        public WaitingForLocationEvent(boolean waiting) {
            this.waiting = waiting;
        }
    }

    public static class LocationServicesUnavailableEvent {
    }

    public static class AnnotationWrittenEvent {
    }

    public static class LoggingStatusEvent {
        public boolean loggingStarted;
        public LoggingStatusEvent(boolean loggingStarted) {
            this.loggingStarted = loggingStarted;
        }
    }
}

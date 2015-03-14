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
}

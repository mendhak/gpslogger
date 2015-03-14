package com.mendhak.gpslogger.common.events;

public class ServiceEvents {
    public static class StatusMessageEvent{
        public String status;
        public StatusMessageEvent(String message){
            this.status = message;
        }

    }
}

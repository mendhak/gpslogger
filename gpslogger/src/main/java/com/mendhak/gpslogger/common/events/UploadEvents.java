package com.mendhak.gpslogger.common.events;


public class UploadEvents {

    public static class AutoEmailEvent {
        public boolean success;
        public AutoEmailEvent(boolean success){
            this.success = success;
        }
    }

    public static class CustomUrlLoggedEvent {
        public boolean success;
        public CustomUrlLoggedEvent(boolean success){
            this.success = success;
        }
    }

    public static class DropboxEvent {
        public boolean success;
        public DropboxEvent(boolean success){
            this.success = success;
        }
    }

    public static class FtpEvent {
        public boolean success;
        public FtpEvent(boolean success){
            this.success = success;
        }
    }

    public static class GDocsEvent {
        public boolean success;
        public GDocsEvent(boolean success){
            this.success = success;
        }
    }

    public static class OpenGTSLoggedEvent {
        public boolean success;
        public OpenGTSLoggedEvent(boolean success){
            this.success = success;
        }
    }

    public static class OpenStreetMapEvent {
        public boolean success;
        public OpenStreetMapEvent(boolean success){
            this.success = success;
        }
    }
}

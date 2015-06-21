package com.mendhak.gpslogger.common.events;


public class UploadEvents {

    public static class AutoEmail {
        public boolean success;
        public AutoEmail(boolean success){
            this.success = success;
        }
    }

    public static class CustomUrl {
        public boolean success;
        public CustomUrl(boolean success){
            this.success = success;
        }
    }

    public static class Dropbox {
        public boolean success;
        public Dropbox(boolean success){
            this.success = success;
        }
    }

    public static class Ftp {
        public boolean success;
        public Ftp(boolean success){
            this.success = success;
        }
    }

    public static class GDocs {
        public boolean success;
        public GDocs(boolean success){
            this.success = success;
        }
    }

    public static class OpenGTS {
        public boolean success;
        public OpenGTS(boolean success){
            this.success = success;
        }
    }

    public static class OpenStreetMap {
        public boolean success;
        public OpenStreetMap(boolean success){
            this.success = success;
        }
    }

    public static class OwnCloud {
        public boolean success;
        public OwnCloud(boolean success) { this.success = success; }
    }
}

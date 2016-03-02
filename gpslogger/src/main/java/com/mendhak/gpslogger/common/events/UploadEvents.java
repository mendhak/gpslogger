package com.mendhak.gpslogger.common.events;


import java.util.ArrayList;

public class UploadEvents {

    // baseeventclass

    // new Baseuploadvent.   b.message = "X";
    // public class AutoEmailEvent extends thebaseone {}

    static abstract class BaseUploadEvent{
        public boolean success;
        public String message;
        public Throwable throwable;


        /**
         * Convenience function, returns a succeeded event
         */
        public <T extends BaseUploadEvent> T succeeded(){
            this.success = true;
            return (T) this;
        }

        /**
         * Convenience function, returns a succes event with a message
         */
        public <T extends BaseUploadEvent> T succeeded(String message){
            this.success = true;
            this.message = message;
            return (T) this;
        }


        /**
         * Convenience function, returns a failed event
         */
        public <T extends BaseUploadEvent> T failed(){
            this.success = false;
            this.message = null;
            this.throwable = null;
            return (T)this;
        }

        /**
         * Convenience function, returns a failed event with a message and a throwable
         */
        public <T extends BaseUploadEvent> T failed(String message, Throwable throwable){
            this.success = false;
            this.message = message;
            this.throwable = throwable;
            return (T)this;
        }
    }

    public static class AutoEmail extends BaseUploadEvent  {  }


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
        public String message;
        public ArrayList<String> ftpMessages;
        public Throwable throwable;
        public Ftp(boolean success){
            this.success = success;
        }
        public Ftp(boolean success, String message, Throwable throwable){
            this.success = success;
            this.message = message;
            this.throwable = throwable;
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
        public String message;
        public Throwable throwable;
        public OwnCloud(boolean success) { this.success = success; }
        public OwnCloud(boolean success, String message, Throwable throwable){
            this.success = success;
            this.message = message;
            this.throwable = throwable;
        }
    }
}

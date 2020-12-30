/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.common.events;


import java.util.ArrayList;

public class UploadEvents {

    @SuppressWarnings("unchecked")
    public static abstract class BaseUploadEvent implements java.io.Serializable{
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
         * Convenience function, returns a success event with a message
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
         * Convenience function, returns a failed event with just a message
         */
        public <T extends BaseUploadEvent> T failed(String message){
            this.success = false;
            this.message = message;
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

    public static class AutoEmail extends BaseUploadEvent  {
        public ArrayList<String> smtpMessages;

    }


    public static class CustomUrl extends BaseUploadEvent {}

    public static class Ftp extends BaseUploadEvent {
        public ArrayList<String> ftpMessages;
    }

    public static class OpenGTS extends BaseUploadEvent {}

    public static class OpenStreetMap extends BaseUploadEvent {}

    public static class OwnCloud extends BaseUploadEvent {}

    public static class SFTP extends BaseUploadEvent {
        public String fingerprint;
        public String hostKey;
    }
}

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


import androidx.annotation.Nullable;

public class CommandEvents {
    /**
     * Requests starting or stopping the logging service.
     * Called from the fragment button click events
     */
    public static class RequestToggle {
    }

    /**
     * Requests starting the logging service
     */
    public static class RequestStartStop {
        public boolean start;
        public RequestStartStop(boolean start){
            this.start = start;
        }
    }

    /**
     * Requests to get status of Logger
     */
    public static class GetStatus {
    }


    /**
     * Requests auto sending to targets
     */
    public static class AutoSend {
        public String formattedFileName;
        public AutoSend(@Nullable String formattedFileName){
            this.formattedFileName = formattedFileName;
        }
    }

    /**
     * Set a description for the next point
     */
    public static class Annotate {
        public String annotation;
        public Annotate(String annotation) {
            this.annotation = annotation;
        }
    }

    /**
     * Log once and stop
     */
    public static class LogOnce {
    }
}

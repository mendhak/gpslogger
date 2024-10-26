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

package com.mendhak.gpslogger.common.slf4j;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


public class SessionLogcatAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Marker to indicate that this logger entry is special
     */
    public static Marker MARKER_LOCATION = MarkerFactory.getMarker("LOCATION");
    /**
     * Marker to indicate that this logger entry is for debug log files only
     */
    public static Marker MARKER_INTERNAL = MarkerFactory.getMarker("INTERNAL");
    public static FifoDeque<ILoggingEvent> Statuses = new FifoDeque<>(325);

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        //Prevents certain entries from appearing in GPS Log View screen
        if(eventObject.getLevel().toInt() < Level.INFO.toInt()){ return; }

        //Prevents certain entries from appearing in device logcat
        if(eventObject.getMarkers() != null && eventObject.getMarkers().contains(MARKER_INTERNAL)){ return; }

        Statuses.add(eventObject);
    }
}

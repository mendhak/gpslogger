package com.mendhak.gpslogger.common;

import ch.qos.logback.core.rolling.RollingFileAppender;


public class GpsRollingFileAppender<E> extends RollingFileAppender<E> {


    @Override
    protected void subAppend(E e) {

        //This extends the RollingFileAppender.
        // It checks if the user has requested a
        // debug log file and only then writes
        // to a file.
        if (AppSettings.isDebugToFile()) {
            super.subAppend(e);
        }

    }
}

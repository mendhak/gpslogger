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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Logs {

    /**
     * Returns a logger for the given class.
     */
    public static Logger of(Class<?> c){

        return getLogger(c.getSimpleName());
    }

    private static Logger getLogger(String name){
        return LoggerFactory.getLogger(name);
    }

    public static void configure() {
        try {
            // reset the default context (which may already have been initialized)
            // since we want to reconfigure it
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            lc.reset();

            //final String LOG_DIR = "/sdcard/GPSLogger";
            final String LOG_DIR = PreferenceHelper.getInstance().getGpsLoggerFolder();

            GpsRollingFileAppender<ILoggingEvent> rollingFileAppender = new GpsRollingFileAppender<>();
            rollingFileAppender.setAppend(true);
            rollingFileAppender.setContext(lc);

            // OPTIONAL: Set an active log file (separate from the rollover files).
            // If rollingPolicy.fileNamePattern already set, you don't need this.
            rollingFileAppender.setFile(LOG_DIR + "/debuglog.txt");
            rollingFileAppender.setLazy(true);

            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setFileNamePattern(LOG_DIR + "/debuglog.%d.txt");
            rollingPolicy.setMaxHistory(3);
            rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
            rollingPolicy.setContext(lc);
            rollingPolicy.start();

            rollingFileAppender.setRollingPolicy(rollingPolicy);

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setPattern("%d{HH:mm:ss} %-5p %class{0}.%method:%L - %m%n");
            encoder.setContext(lc);
            encoder.start();

            rollingFileAppender.setEncoder(encoder);
            rollingFileAppender.start();

            // setup LogcatAppender
            PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
            encoder2.setContext(lc);
            encoder2.setPattern("%method:%L - %m%n");
            encoder2.start();

            LogcatAppender logcatAppender = new LogcatAppender();
            logcatAppender.setContext(lc);
            logcatAppender.setEncoder(encoder2);
            logcatAppender.start();

            SessionLogcatAppender sessionAppender = new SessionLogcatAppender();
            sessionAppender.setContext(lc);
            sessionAppender.start();

            // add the newly created appenders to the root logger;
            // qualify Logger to disambiguate from org.slf4j.Logger
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.addAppender(rollingFileAppender);
            root.addAppender(logcatAppender);
            root.addAppender(sessionAppender);

        }
        catch(Exception ex){
              System.out.println("Could not configure logging!");
        }

    }
}

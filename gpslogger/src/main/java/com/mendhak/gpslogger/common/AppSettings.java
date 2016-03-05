/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.common;

import android.app.Application;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

public class AppSettings extends Application {

    private static JobManager jobManager;
    private static AppSettings instance;
    private static Logger tracer;


    @Override
    public void onCreate() {
        super.onCreate();

        //Configure the slf4j logger
        Logs.configure();
         tracer = Logs.of(this.getClass());
        tracer.debug("Log4J configured");

        //Configure the Event Bus
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        tracer.debug("EventBus configured");

        //Configure the Job Queue
        Configuration config = new Configuration.Builder(getInstance())
                .networkUtil(new WifiNetworkUtil(getInstance()))
                .consumerKeepAlive(60)
                .minConsumerCount(0)
                .customLogger(jobQueueLogger)
                .build();
        jobManager = new JobManager(this, config);
        tracer.debug("Job Queue configured");
    }

    /**
     * Returns a configured Job Queue Manager
     */
    public static JobManager GetJobManager() {
        return jobManager;
    }

    public AppSettings() {
        instance = this;
    }

    /**
     * Returns a singleton instance of this class
     */
    public static AppSettings getInstance() {
        return instance;
    }


    private final CustomLogger jobQueueLogger = new CustomLogger() {
        @Override
        public boolean isDebugEnabled() {
            return BuildConfig.DEBUG;
//            return false;
        }

        @Override
        public void d(String text, Object... args) {

            tracer.debug(text);
        }

        @Override
        public void e(Throwable t, String text, Object... args) {
            tracer.error(text, t);
        }

        @Override
        public void e(String text, Object... args) {

            tracer.error(text);
        }
    };



}

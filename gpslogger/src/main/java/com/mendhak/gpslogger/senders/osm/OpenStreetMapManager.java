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

package com.mendhak.gpslogger.senders.osm;

import com.birbit.android.jobqueue.CancelResult;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import org.slf4j.Logger;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthProvider;


import java.io.File;
import java.util.List;

public class OpenStreetMapManager extends FileSender {



    private static final Logger LOG = Logs.of(OpenStreetMapManager.class);
    static final String OSM_REQUESTTOKEN_URL = "https://www.openstreetmap.org/oauth/request_token";
    static final String OSM_ACCESSTOKEN_URL = "https://www.openstreetmap.org/oauth/access_token";
    static final String OSM_AUTHORIZE_URL = "https://www.openstreetmap.org/oauth/authorize";
    static final String OSM_GPSTRACE_URL = "https://www.openstreetmap.org/api/0.6/gpx/create";
    private PreferenceHelper preferenceHelper;

    public OpenStreetMapManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;

    }

    public static OAuthProvider getOSMAuthProvider() {
        return new OkHttpOAuthProvider(OSM_REQUESTTOKEN_URL, OSM_ACCESSTOKEN_URL, OSM_AUTHORIZE_URL);
    }

    public boolean isOsmAuthorized() {
        String oAuthAccessToken = preferenceHelper.getOSMAccessToken();
        return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
    }

    public static OAuthConsumer getOSMAuthConsumer() {

        OAuthConsumer consumer = null;

        try {

            consumer = new OkHttpOAuthConsumer(BuildConfig.OSM_CONSUMER_KEY, BuildConfig.OSM_CONSUMER_SECRET);


            String osmAccessToken =  PreferenceHelper.getInstance().getOSMAccessToken();
            String osmAccessTokenSecret = PreferenceHelper.getInstance().getOSMAccessTokenSecret();

            if (Strings.isNullOrEmpty(osmAccessToken) || Strings.isNullOrEmpty(osmAccessTokenSecret)) {
                return consumer;
            } else {
                consumer.setTokenWithSecret(osmAccessToken, osmAccessTokenSecret);
            }

        } catch (Exception e) {
            LOG.error("Error getting OAuth Consumer", e);
        }

        return consumer;
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            if (f.getName().contains(".gpx")) {
                uploadFile(f.getName());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return isOsmAuthorized();
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isOsmAutoSendEnabled();
    }

    public void uploadFile(String fileName) {
        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        final File chosenFile = new File(gpxFolder, fileName);
        final OAuthConsumer consumer = getOSMAuthConsumer();
        final String gpsTraceUrl = OSM_GPSTRACE_URL;


        final String description = preferenceHelper.getOSMDescription();
        final String tags = preferenceHelper.getOSMTags();
        final String visibility = preferenceHelper.getOSMVisibility();

        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new OSMJob( consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
            }
        }, TagConstraint.ANY, OSMJob.getJobTag(chosenFile));

    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }

}




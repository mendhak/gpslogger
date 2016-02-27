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

package com.mendhak.gpslogger.senders.osm;

import android.content.Context;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import java.io.File;
import java.util.List;

public class OpenStreetMapManager implements IFileSender {




    final String OSM_REQUESTTOKEN_URL = "http://www.openstreetmap.org/oauth/request_token";
    final String OSM_ACCESSTOKEN_URL = "http://www.openstreetmap.org/oauth/access_token";
    final String OSM_AUTHORIZE_URL = "http://www.openstreetmap.org/oauth/authorize";
    final String OSM_GPSTRACE_URL = "http://www.openstreetmap.org/api/0.6/gpx/create";

    public OpenStreetMapManager() {

    }

    public OAuthProvider GetOSMAuthProvider() {
        return new CommonsHttpOAuthProvider(OSM_REQUESTTOKEN_URL, OSM_ACCESSTOKEN_URL, OSM_AUTHORIZE_URL);
    }

    protected boolean IsOsmAuthorized() {
        String oAuthAccessToken = AppSettings.getOSMAccessToken();
        return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
    }

    public OAuthConsumer GetOSMAuthConsumer() {

        OAuthConsumer consumer = null;

        try {

            consumer = new CommonsHttpOAuthConsumer(BuildConfig.OSM_CONSUMER_KEY, BuildConfig.OSM_CONSUMER_SECRET);


            String osmAccessToken =  AppSettings.getOSMAccessToken();
            String osmAccessTokenSecret = AppSettings.getOSMAccessTokenSecret();

            if (osmAccessToken != null && osmAccessToken.length() > 0
                    && osmAccessTokenSecret != null
                    && osmAccessTokenSecret.length() > 0) {
                consumer.setTokenWithSecret(osmAccessToken,
                        osmAccessTokenSecret);
            }

        } catch (Exception e) {
            //Swallow the exception
        }

        return consumer;
    }

    @Override
    public void UploadFile(List<File> files) {
        for (File f : files) {
            if (f.getName().contains(".gpx")) {
                UploadFile(f.getName());
            }
        }
    }

    @Override
    public boolean IsAvailable() {
        return AppSettings.isOsmAutoSendEnabled() && IsOsmAuthorized();
    }

    public void UploadFile(String fileName) {
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        File chosenFile = new File(gpxFolder, fileName);
        OAuthConsumer consumer = GetOSMAuthConsumer();
        String gpsTraceUrl = OSM_GPSTRACE_URL;


        String description = AppSettings.getOSMDescription();
        String tags = AppSettings.getOSMTags();
        String visibility = AppSettings.getOSMVisibility();

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, OSMJob.getJobTag(chosenFile));
        jobManager.addJobInBackground(new OSMJob( consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }

}




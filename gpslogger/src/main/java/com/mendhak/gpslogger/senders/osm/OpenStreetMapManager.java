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

import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
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
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public OpenStreetMapManager() {

    }

    public OAuthProvider getOSMAuthProvider() {
        return new CommonsHttpOAuthProvider(OSM_REQUESTTOKEN_URL, OSM_ACCESSTOKEN_URL, OSM_AUTHORIZE_URL);
    }

    protected boolean isOsmAuthorized() {
        String oAuthAccessToken = preferenceHelper.getOSMAccessToken();
        return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
    }

    public OAuthConsumer getOSMAuthConsumer() {

        OAuthConsumer consumer = null;

        try {

            consumer = new CommonsHttpOAuthConsumer(BuildConfig.OSM_CONSUMER_KEY, BuildConfig.OSM_CONSUMER_SECRET);


            String osmAccessToken =  preferenceHelper.getOSMAccessToken();
            String osmAccessTokenSecret = preferenceHelper.getOSMAccessTokenSecret();

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
    public void uploadFile(List<File> files) {
        for (File f : files) {
            if (f.getName().contains(".gpx")) {
                uploadFile(f.getName());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return preferenceHelper.isOsmAutoSendEnabled() && isOsmAuthorized();
    }

    public void uploadFile(String fileName) {
        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        File chosenFile = new File(gpxFolder, fileName);
        OAuthConsumer consumer = getOSMAuthConsumer();
        String gpsTraceUrl = OSM_GPSTRACE_URL;


        String description = preferenceHelper.getOSMDescription();
        String tags = preferenceHelper.getOSMTags();
        String visibility = preferenceHelper.getOSMVisibility();

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, OSMJob.getJobTag(chosenFile));
        jobManager.addJobInBackground(new OSMJob( consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }

}




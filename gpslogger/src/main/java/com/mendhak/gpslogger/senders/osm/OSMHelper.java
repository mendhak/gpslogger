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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class OSMHelper implements IFileSender {

    Context context;

    public OSMHelper(Context context) {

        this.context = context;
    }

    public static OAuthProvider GetOSMAuthProvider(Context context) {
        return new CommonsHttpOAuthProvider(
                context.getString(R.string.osm_requesttoken_url),
                context.getString(R.string.osm_accesstoken_url),
                context.getString(R.string.osm_authorize_url));
    }

    public static boolean IsOsmAuthorized(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String oAuthAccessToken = prefs.getString("osm_accesstoken", "");

        return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
    }

    public static OAuthConsumer GetOSMAuthConsumer(Context context) {

        OAuthConsumer consumer = null;

        try {

            consumer = new CommonsHttpOAuthConsumer(
                    BuildConfig.OSM_CONSUMER_KEY,
                    BuildConfig.OSM_CONSUMER_SECRET);

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context);
            String osmAccessToken = prefs.getString("osm_accesstoken", "");
            String osmAccessTokenSecret = prefs.getString(
                    "osm_accesstokensecret", "");

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

    public void UploadFile(String fileName) {
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        File chosenFile = new File(gpxFolder, fileName);
        OAuthConsumer consumer = GetOSMAuthConsumer(context);
        String gpsTraceUrl = context.getString(R.string.osm_gpstrace_url);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String description = prefs.getString("osm_description", "");
        String tags = prefs.getString("osm_tags", "");
        String visibility = prefs.getString("osm_visibility", "private");

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, OSMJob.JOB_TAG);
        jobManager.addJobInBackground(new OSMJob( consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }

}




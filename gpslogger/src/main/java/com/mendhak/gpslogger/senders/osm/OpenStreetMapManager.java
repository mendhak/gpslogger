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

import android.content.Context;
import android.net.Uri;

import com.birbit.android.jobqueue.CancelResult;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONException;
import org.slf4j.Logger;
import java.io.File;
import java.util.List;

public class OpenStreetMapManager extends FileSender {



    private static final Logger LOG = Logs.of(OpenStreetMapManager.class);

    static final String OSM_GPSTRACE_URL = "https://www.openstreetmap.org/api/0.6/gpx/create";
    private PreferenceHelper preferenceHelper;

    public OpenStreetMapManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public static String getOpenStreetMapClientID() {
        return "IPhwuq5DbvDXtP7VUKLU2x5TLEucQLHyKez8DdNNgVM";
    }

    public static String getOpenStreetMapRedirect() {
        //Needs to match in androidmanifest.xml
        return "com.mendhak.gpslogger://oauth2openstreetmap";
    }

    public static String[] getOpenStreetMapClientScopes() {
        return new String[]{"write_gpx"};
    }

    public static AuthorizationService getAuthorizationService(Context context) {
        return new AuthorizationService(context, new AppAuthConfiguration.Builder().build());
    }

    public static AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return new AuthorizationServiceConfiguration(
                Uri.parse("https://www.openstreetmap.org/oauth2/authorize"),
                Uri.parse("https://www.openstreetmap.org/oauth2/token"),
                null,
                null
        );
    }


    public static AuthState getAuthState() {
        AuthState authState = new AuthState();
        String open_street_map_auth_state = PreferenceHelper.getInstance().getOSMAuthState();

        if (!Strings.isNullOrEmpty(open_street_map_auth_state)) {
            try {
                authState = AuthState.jsonDeserialize(open_street_map_auth_state);

            } catch (JSONException e) {
                LOG.debug(e.getMessage(), e);
            }
        }

        return authState;
    }

    public boolean isOsmAuthorized() {
        return getAuthState().isAuthorized();
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

    @Override
    public String getName() {
        return SenderNames.OPENSTREETMAP;
    }

    public void uploadFile(String fileName) {
        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        final File chosenFile = new File(gpxFolder, fileName);

        final String gpsTraceUrl = OSM_GPSTRACE_URL;


        final String description = preferenceHelper.getOSMDescription();
        final String tags = preferenceHelper.getOSMTags();
        final String visibility = preferenceHelper.getOSMVisibility();

        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new OSMJob(gpsTraceUrl, chosenFile, description, tags, visibility));
            }
        }, TagConstraint.ANY, OSMJob.getJobTag(chosenFile));

    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }

}




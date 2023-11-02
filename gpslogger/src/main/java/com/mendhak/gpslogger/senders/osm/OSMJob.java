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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import oauth.signpost.OAuthConsumer;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OSMJob extends Job {


    private static final Logger LOG = Logs.of(OSMJob.class);

    private final AtomicBoolean taskDone = new AtomicBoolean(false);
    private String openStreetMapAccessToken;
    //OAuthConsumer consumer;
    String gpsTraceUrl;
    File chosenFile;
    String description;
    String tags;
    String visibility;

    protected OSMJob(OAuthConsumer consumer, String gpsTraceUrl, File chosenFile, String description, String tags, String visibility) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(chosenFile)));

//        this.consumer = consumer;
        this.gpsTraceUrl = gpsTraceUrl;
        this.chosenFile = chosenFile;
        this.description = description;
        this.tags = tags;
        this.visibility = visibility;
    }

    @Override
    public void onAdded() {

        LOG.debug("OSM Job added");
    }

    @Override
    public void onRun() throws Throwable {

        AuthState authState = OpenStreetMapManager.getAuthState();

        if(!authState.isAuthorized()){
            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed("Could not upload to OpenStreetMap. Not authorized."));
        }

        AuthorizationService authorizationService = OpenStreetMapManager.getAuthorizationService(AppSettings.getInstance());

        // The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
        // So I can't do network calls on this thread.
        // Instead, updating a class level variable, and waiting for it afterwards.
        // https://github.com/openid/AppAuth-Android/issues/123
        authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
            @Override
            public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                if (ex != null){
                    EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed(ex.toJsonString(), ex));
                    taskDone.set(true);
                    LOG.error(ex.toJsonString(), ex);
                    return;
                }

                openStreetMapAccessToken = accessToken;
                taskDone.set(true);
            }
        });

        // Wait for the performActionWithFreshTokens.execute callback
        // (which happens on the UI thread for some reason) to complete.
        while (!taskDone.get()) {
            Thread.sleep(500);
        }

        if (Strings.isNullOrEmpty(openStreetMapAccessToken)) {
            LOG.error("Failed to fetch Access Token for OpenStreetMap. Stopping this job.");
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()),
                        (X509TrustManager) Networks.getTrustManager(AppSettings.getInstance()))
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", chosenFile.getName(), RequestBody.create(MediaType.parse("application/xml+gpx"), chosenFile))
                .addFormDataPart("description", Strings.isNullOrEmpty(description) ? "GPSLogger for Android" : description)
                .addFormDataPart("tags", tags)
                .addFormDataPart("visibility",visibility)
                .build();

        Request request = new Request.Builder()
                .url(gpsTraceUrl)
                .addHeader("Authorization", "Bearer " + openStreetMapAccessToken)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();

        if(response.isSuccessful()){
            String message = body.string();
            LOG.debug("Response from OpenStreetMap: " + message);
            LOG.info("OpenStreetMap - file uploaded");
            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().succeeded());
        }
        else {
            body.close();
            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed());
        }
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        LOG.error("Could not send to OpenStreetMap", throwable);
        EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed("Could not send to OpenStreetMap", throwable));
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.createExponentialBackoff(runCount, 3000);
    }

    public static String getJobTag(File gpxFile) {
        return "OSM" + gpxFile.getName();

    }

    @Override
    protected int getRetryLimit() {
        return 3;
    }
}

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

package com.mendhak.gpslogger.loggers.customurl;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CustomUrlJob extends Job {

    private static final Logger LOG = Logs.of(CustomUrlJob.class);

    private UploadEvents.BaseUploadEvent callbackEvent;

    private File csvFile;
    private ArrayList<CustomUrlRequest> urlRequests;

    public CustomUrlJob(ArrayList<CustomUrlRequest> urlRequests, File csvFile, UploadEvents.BaseUploadEvent callbackEvent) {
        super(new Params(1).requireNetwork().persist());

        this.callbackEvent = callbackEvent;
        this.urlRequests = urlRequests;
        this.csvFile = csvFile;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {

        boolean success = true;
        String responseError = null;
        String responseThrowableMessage = null;

        if(urlRequests != null && urlRequests.size() > 0){

            for (CustomUrlRequest urlRequest : urlRequests) {
                LOG.info("HTTP Request - " + urlRequest.getLogURL());

                OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
                okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()),
                        (X509TrustManager) Networks.getTrustManager(AppSettings.getInstance()));
                Request.Builder requestBuilder = new Request.Builder().url(urlRequest.getLogURL());

                for(Map.Entry<String,String> header : urlRequest.getHttpHeaders().entrySet()){
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }

                if ( ! urlRequest.getHttpMethod().equalsIgnoreCase("GET")) {
                    RequestBody body = RequestBody.create(null, urlRequest.getHttpBody());
                    requestBuilder = requestBuilder.method(urlRequest.getHttpMethod(),body);
                }

                Request request = requestBuilder.build();
                Response response = okBuilder.build().newCall(request).execute();

                if (response.isSuccessful()) {
                    LOG.debug("HTTP request complete with successful response code " + response);
                }
                else {
                    LOG.error("HTTP request complete with unexpected response code " + response );
                    responseError = "Unexpected code " + response;
                    responseThrowableMessage = response.body().string();
                    success = false;
                }

                response.body().close();

                if(!success){
                    break;
                }
            }
        }

        if(success){
            EventBus.getDefault().post(callbackEvent.succeeded());
        }
        else {
            EventBus.getDefault().post(callbackEvent.failed("Unexpected code " + responseError, new Throwable(responseThrowableMessage)));
        }
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        EventBus.getDefault().post(callbackEvent.failed("Could not send to custom URL", throwable));
        LOG.error("Custom URL: maximum attempts failed, giving up", throwable);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        LOG.warn(String.format("Custom URL: attempt %d failed, maximum %d attempts", runCount, maxRunCount));
        return RetryConstraint.createExponentialBackoff(runCount, 5000);
    }


    @Override
    protected int getRetryLimit() {
        return 3;
    }
}

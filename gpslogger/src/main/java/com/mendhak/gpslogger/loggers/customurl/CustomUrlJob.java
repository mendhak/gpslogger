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


import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import okhttp3.*;

import org.slf4j.Logger;
import java.io.IOException;


public class CustomUrlJob extends Job {

    private static final Logger LOG = Logs.of(CustomUrlJob.class);

    private UploadEvents.BaseUploadEvent callbackEvent;
    private CustomUrlRequest urlRequest;

    public CustomUrlJob(CustomUrlRequest urlRequest, UploadEvents.BaseUploadEvent callbackEvent) {
        super(new Params(1).requireNetwork().persist());

        this.callbackEvent = callbackEvent;
        this.urlRequest = urlRequest;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {

        LOG.debug("Sending to URL: " + urlRequest.getLogURL());

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();

        if(!Strings.isNullOrEmpty(urlRequest.getBasicAuthUsername())){
            okBuilder.authenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                    String credential = Credentials.basic(urlRequest.getBasicAuthUsername(), urlRequest.getBasicAuthPassword());
                    return response.request().newBuilder().header("Authorization", credential).build();
                }
            });
        }

        okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()));

        OkHttpClient client = okBuilder.build();

        Request request;



        if ( ! urlRequest.getHttpMethod().equalsIgnoreCase("GET")) {

            RequestBody body = RequestBody.create(null, urlRequest.getHttpBody());
            request = new Request.Builder().url(urlRequest.getLogURL()).method(urlRequest.getHttpMethod(), body).build();
        }
        else {
            request = new Request.Builder().url(urlRequest.getLogURL()).build();
        }

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            LOG.debug("Success - response code " + response);
            EventBus.getDefault().post(callbackEvent.succeeded());
        }
        else {
            LOG.error("Unexpected response code " + response );
            EventBus.getDefault().post(callbackEvent.failed("Unexpected code " + response,new Throwable(response.body().string())));
        }

        response.body().close();
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        EventBus.getDefault().post(callbackEvent.failed("Could not send to custom URL", throwable));
        LOG.error("Could not send to custom URL", throwable);
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 2;
    }
}

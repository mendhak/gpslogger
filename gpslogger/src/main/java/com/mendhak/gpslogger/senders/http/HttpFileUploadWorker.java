package com.mendhak.gpslogger.senders.http;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpFileUploadWorker extends Worker {

    private static final Logger LOG = Logs.of(HttpFileUploadWorker.class);

    public HttpFileUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString("filePath");
        if (Strings.isNullOrEmpty(filePath)) {
            LOG.error("No file path provided to HttpFileUploadWorker");
            return Result.failure();
        }

        File fileToUpload = new File(filePath);
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

        String url = preferenceHelper.getHttpFileUploadUrl();
        String method = preferenceHelper.getHttpFileUploadMethod();
        String headersString = preferenceHelper.getHttpFileUploadHeaders();
        String username = preferenceHelper.getHttpFileUploadUsername();
        String password = preferenceHelper.getHttpFileUploadPassword();
        String bodyType = preferenceHelper.getHttpFileUploadBodyType();

        try {
            LOG.info("HTTP File Uploading " + fileToUpload.getName() + " to " + url + " using " + method + " (" + bodyType + ")");

            OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
            okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()),
                    (X509TrustManager) Networks.getTrustManager(AppSettings.getInstance()));

            Request.Builder requestBuilder = new Request.Builder().url(url);

            // Add custom headers
            if (!Strings.isNullOrEmpty(headersString)) {
                String[] lines = headersString.split("\n");
                for (String line : lines) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        requestBuilder.addHeader(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            // Basic Auth
            if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
                requestBuilder.addHeader("Authorization", Credentials.basic(username, password));
            }

            RequestBody requestBody;
            if ("form-data".equalsIgnoreCase(bodyType)) {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileToUpload.getName(),
                                RequestBody.create(MediaType.parse("application/octet-stream"), fileToUpload))
                        .build();
            } else {
                // Binary stream
                requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileToUpload);
            }

            requestBuilder.method(method, requestBody);

            Request request = requestBuilder.build();
            Response response = okBuilder.build().newCall(request).execute();

            if (response.isSuccessful()) {
                LOG.debug("HTTP File Upload complete with successful response code " + response.code());
                response.close();

                EventBus.getDefault().post(new UploadEvents.HttpFileUpload().succeeded());
                Systems.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "httpfileupload");

                return Result.success();
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Empty response body";
                LOG.error("HTTP File Upload failed with code " + response.code() + ": " + errorBody);
                response.close();

                if (getRunAttemptCount() < 3) {
                    return Result.retry();
                }

                EventBus.getDefault().post(new UploadEvents.HttpFileUpload().failed("Response code " + response.code(), new Throwable(errorBody)));
                return Result.failure();
            }

        } catch (Exception e) {
            LOG.error("Exception during HTTP File Upload", e);
            if (getRunAttemptCount() < 3) {
                return Result.retry();
            }
            EventBus.getDefault().post(new UploadEvents.HttpFileUpload().failed(e.getMessage(), e));
            return Result.failure();
        }
    }
}

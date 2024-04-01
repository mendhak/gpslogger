package com.mendhak.gpslogger.loggers.customurl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.customurl.CustomUrlManager;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomUrlWorker extends Worker {

    private static final Logger LOG = Logs.of(CustomUrlWorker.class);
    public CustomUrlWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        UploadEvents.BaseUploadEvent callbackEvent = getCallbackEvent();
        CustomUrlRequest[] urlRequests = getCustomUrlRequests(getInputData());

        if(urlRequests == null || urlRequests.length == 0){
            EventBus.getDefault().post(callbackEvent.failed("Nothing to process", new Throwable("Nothing to process")));
            return Result.failure();
        }

        boolean success = true;
        String responseError = null;
        String responseThrowableMessage = null;

        for (CustomUrlRequest urlRequest : urlRequests) {
            try{
                LOG.info("HTTP Request - " + urlRequest.getLogURL());

                OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
                okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()),
                        (X509TrustManager) Networks.getTrustManager(AppSettings.getInstance()));
                Request.Builder requestBuilder = new Request.Builder().url(urlRequest.getLogURL());

                for (Map.Entry<String, String> header : urlRequest.getHttpHeaders().entrySet()) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }

                if (!urlRequest.getHttpMethod().equalsIgnoreCase("GET")) {
                    RequestBody body = RequestBody.create(null, urlRequest.getHttpBody());
                    requestBuilder = requestBuilder.method(urlRequest.getHttpMethod(), body);
                }

                Request request = requestBuilder.build();
                Response response = okBuilder.build().newCall(request).execute();

                if (response.isSuccessful()) {
                    LOG.debug("HTTP request complete with successful response code " + response);
                } else {
                    LOG.error("HTTP request complete with unexpected response code " + response);
                    responseError = "Unexpected code " + response;
                    responseThrowableMessage = response.body().string();
                    success = false;
                }

                response.body().close();

                if (!success) {
                    break;
                }
            }
            catch (Exception e) {
                LOG.error("Exception during Custom URL processing " + e);
                responseError = "Exception " + e;
                responseThrowableMessage = e.getMessage();
                success = false;
                break;
            }
        }

        if(success) {

            String gpxFilePath = getInputData().getString("gpxFilePath");
            String csvFilePath = getInputData().getString("csvFilePath");
            // Notify external listeners
            if(!Strings.isNullOrEmpty(gpxFilePath) || !Strings.isNullOrEmpty(csvFilePath)){
                String[] filePaths = new String[]{ Strings.isNullOrEmpty(gpxFilePath) ? csvFilePath : gpxFilePath };
                Systems.sendFileUploadedBroadcast(getApplicationContext(), filePaths, getInputData().getString("callbackType"));
            }

            // Notify internal listeners
            EventBus.getDefault().post(callbackEvent.succeeded());
            return Result.success();
        }
        else {
            if(getRunAttemptCount() < getRetryLimit()){
                LOG.warn(String.format("Custom URL - attempt %d of %d failed, will retry", getRunAttemptCount(), getRetryLimit()));
                return Result.retry();
            }

            EventBus.getDefault()
                    .post(callbackEvent.failed("Unexpected code " + responseError, new Throwable(responseThrowableMessage)));
            return Result.failure();
        }
    }

    private CustomUrlRequest[] getCustomUrlRequests(Data inputData) {
        CustomUrlRequest[] urlRequests = null;
        String[] serializedRequests = inputData.getStringArray("urlRequests");
        String gpxFilePath = inputData.getString("gpxFilePath");
        String csvFilePath = inputData.getString("csvFilePath");

        if(!Strings.isNullOrEmpty(gpxFilePath)){
            OpenGTSManager openGTSManager = new OpenGTSManager(PreferenceHelper.getInstance(), Systems.getBatteryInfo(AppSettings.getInstance()).BatteryLevel);
            List<CustomUrlRequest> gpxCustomUrlRequests = openGTSManager.getCustomUrlRequestsFromGPX(new File(gpxFilePath));
            urlRequests = gpxCustomUrlRequests.toArray(new CustomUrlRequest[0]);
        }
        else if(!Strings.isNullOrEmpty(csvFilePath)){
            CustomUrlManager customUrlManager = new CustomUrlManager(PreferenceHelper.getInstance());
            List<CustomUrlRequest> csvCustomUrlRequests = customUrlManager.getCustomUrlRequestsFromCSV(new File(csvFilePath));
            urlRequests = csvCustomUrlRequests.toArray(new CustomUrlRequest[0]);
        }
        else if(serializedRequests != null && serializedRequests.length > 0) {
            urlRequests = new CustomUrlRequest[serializedRequests.length];
            for (int i = 0; i < serializedRequests.length; i++) {
                urlRequests[i] = Strings.deserializeFromJson(serializedRequests[i], CustomUrlRequest.class);
            }
        }
        return urlRequests;
    }

    @NonNull
    private UploadEvents.BaseUploadEvent getCallbackEvent() {
        String callbackType = getInputData().getString("callbackType");
        UploadEvents.BaseUploadEvent callbackEvent = new UploadEvents.CustomUrl();

        if(!Strings.isNullOrEmpty(callbackType) && callbackType.equals("opengts")){
            callbackEvent = new UploadEvents.OpenGTS();
        }
        return callbackEvent;
    }

    protected int getRetryLimit() {
        return 3;
    }
}

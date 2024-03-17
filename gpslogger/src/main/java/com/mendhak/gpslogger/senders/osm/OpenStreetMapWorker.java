package com.mendhak.gpslogger.senders.osm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpenStreetMapWorker extends Worker {

    private static final Logger LOG = Logs.of(OpenStreetMapWorker.class);

    private String openStreetMapAccessToken;

    public OpenStreetMapWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String filePath = getInputData().getString("filePath");
        File fileToUpload = new File(filePath);

        final AtomicBoolean taskDone = new AtomicBoolean(false);
        AuthState authState = OpenStreetMapManager.getAuthState();
        boolean success;
        String failureMessage = "";
        Throwable failureThrowable = null;

        if(!authState.isAuthorized()){
            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed("Could not upload to OpenStreetMap. Not authorized."));
            return Result.failure();
        }

        try {
            AuthorizationService authorizationService = OpenStreetMapManager.getAuthorizationService(AppSettings.getInstance());

            // The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
            // So I can't do network calls on this thread.
            // Instead, updating a class level variable, and waiting for it afterwards.
            // https://github.com/openid/AppAuth-Android/issues/123
            authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex != null){
//                        EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed(ex.toJsonString(), ex));
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
                success = false;
                failureMessage = "Failed to fetch Access Token for OpenStreetMap.";
//                EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed("Failed to fetch Access Token for OpenStreetMap."));
//                return Result.failure();
            }
            else {

                PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
                String description = preferenceHelper.getOSMDescription();
                String tags = preferenceHelper.getOSMTags();
                String visibility = preferenceHelper.getOSMVisibility();


                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()),
                                (X509TrustManager) Networks.getTrustManager(AppSettings.getInstance()))
                        .build();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileToUpload.getName(), RequestBody.create(MediaType.parse("application/xml+gpx"), fileToUpload))
                        .addFormDataPart("description", Strings.isNullOrEmpty(description) ? "GPSLogger for Android" : description)
                        .addFormDataPart("tags", tags)
                        .addFormDataPart("visibility",visibility)
                        .build();

                Request request = new Request.Builder()
                        .url("https://www.openstreetmap.org/api/0.6/gpx/create")
                        .addHeader("Authorization", "Bearer " + openStreetMapAccessToken)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();

                if(response.isSuccessful()){
                    String message = body.string();
                    LOG.debug("Response from OpenStreetMap: " + message);
                    LOG.info("OpenStreetMap - file uploaded");
                    success = true;
                }
                else {
                    failureMessage = "Failed to upload to OpenStreetMap";
                    if(body != null){
                        failureMessage = body.string();
                    }
//                    body.close();
                    EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed());
                    success = false;

                }
            }
        }
        catch(Exception ex){
            success = false;
            failureMessage = ex.getMessage();
            failureThrowable = ex;
        }


        if(success){
            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().succeeded());
            return Result.success();
        }
        else {
            if(getRunAttemptCount() < getRetryLimit()){
                LOG.warn(String.format("OpenStreetMap Upload - attempt %d of %d failed, will retry", getRunAttemptCount(), getRetryLimit()));
                return Result.retry();
            }

            if(failureThrowable == null){
                failureThrowable = new Exception(failureMessage);
            }

            EventBus.getDefault().post(new UploadEvents.OpenStreetMap().failed(failureMessage, failureThrowable));
            return Result.failure();

        }
    }

    protected int getRetryLimit() {
        return 3;
    }
}

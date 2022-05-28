package com.mendhak.gpslogger.senders.googledrive;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.net.URLEncoder;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleDriveJob extends Job {

    private static final Logger LOG = Logs.of(GoogleDriveJob.class);
    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    String fileName;
    private Handler handler;
    private HandlerThread handlerThread;

    protected GoogleDriveJob(String fileName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(fileName)));
        this.fileName = fileName;
    }

    @Override
    public void onAdded() {
        LOG.debug("Google Drive job added");
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);
        AuthState authState = getGoogleDriveAuthState();
        LOG.debug(authState.jsonSerializeString());
        if (authState.isAuthorized()) {

            AuthorizationService authorizationService = GoogleDriveManager.getAuthorizationService(AppSettings.getInstance());

            handlerThread = new HandlerThread("HandlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());

            //The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
            // So I can't do network calls on this thread.
            //That's why I have to do a handlerThread and handler.post
            authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex != null) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(ex.toJsonString(), ex));
                        return;
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String gpsLoggerFolderId = getFileIdFromFileName(accessToken,
                                        "GPSLOGGER", null);
                                LOG.debug("GPSLogger folder ID - " + gpsLoggerFolderId);
                                if (Strings.isNullOrEmpty(gpsLoggerFolderId)) {
                                    LOG.debug("GPSLogger folder not found, will create.");
                                    gpsLoggerFolderId = createEmptyFile(accessToken, "GPSLOGGER",
                                            "application/vnd.google-apps.folder", "root");
                                }

                                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());

                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                                EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(e.getMessage(), e));
                            } finally {
                                handlerThread.quit();
                            }
                        }
                    });

                }
            });
        }
    }

    private String getFileIdFromFileName(String accessToken, String fileName, String inFolderId) throws Exception {
        String fileId = "";
        fileName = URLEncoder.encode(fileName, "UTF-8");

        String inFolderParam = "";
        if (!Strings.isNullOrEmpty(inFolderId)) {
            inFolderParam = "+and+'" + inFolderId + "'+in+parents";
        }
        String searchUrl = "https://www.googleapis.com/drive/v3/files?q=name%20%3D%20%27" + fileName + "%27%20and%20trashed%20%3D%20false" + inFolderParam;
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(searchUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        Request request = requestBuilder.build();
        Response response = client.newCall(request).execute();
        String fileMetadata = response.body().string();
        LOG.debug(fileMetadata);
        response.body().close();
        JSONObject fileMetadataJson = new JSONObject(fileMetadata);
        if (fileMetadataJson.getJSONArray("files") != null && fileMetadataJson.getJSONArray("files").length() > 0) {
            fileId = fileMetadataJson.getJSONArray("files").getJSONObject(0).get("id").toString();
            LOG.debug("Found file with ID " + fileId);
        }

        return fileId;

    }


    private String createEmptyFile(String accessToken, String fileName, String mimeType, String parentFolderId) throws Exception {

        String fileId = null;
        String createFileUrl = "https://www.googleapis.com/drive/v3/files";

        String createFilePayload = "   {\n" +
                "             \"name\": \"" + fileName + "\",\n" +
                "             \"mimeType\": \"" + mimeType + "\",\n" +
                "             \"parents\": [\"" + parentFolderId + "\"]\n" +
                "            }";


        //            OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(createFileUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), createFilePayload);
        requestBuilder = requestBuilder.method("POST", body);


        Request request = requestBuilder.build();
        Response response = client.newCall(request).execute();
        String fileMetadata = response.body().string();
        LOG.debug(fileMetadata);
        response.body().close();

        JSONObject fileMetadataJson = new JSONObject(fileMetadata);
        fileId = fileMetadataJson.getString("id");

        return fileId;
    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not upload to Google Drive", throwable));
        LOG.error("Could not upload to Google Drive", throwable);
        return RetryConstraint.CANCEL;
    }

    public static String getJobTag(String fileName) {
        return "GOOGLEDRIVE" + fileName;
    }

    AuthState getGoogleDriveAuthState() {
        AuthState authState = new AuthState();
        String google_drive_auth_state = preferenceHelper.getGoogleDriveAuthState();

        if (!Strings.isNullOrEmpty(google_drive_auth_state)) {
            try {
                authState = AuthState.jsonDeserialize(google_drive_auth_state);

            } catch (JSONException e) {
                LOG.debug(e.getMessage(), e);
            }
        }

        return authState;
    }
}

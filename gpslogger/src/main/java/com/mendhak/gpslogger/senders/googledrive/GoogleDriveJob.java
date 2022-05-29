package com.mendhak.gpslogger.senders.googledrive;

import android.os.Build;

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
import com.mendhak.gpslogger.loggers.Streams;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleDriveJob extends Job {

    private static final Logger LOG = Logs.of(GoogleDriveJob.class);
    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private String fileName;
    private final AtomicBoolean taskDone = new AtomicBoolean(false);
    private String googleDriveAccessToken;

    protected GoogleDriveJob(String fileName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(fileName)).groupBy("GoogleDrive"));
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
        AuthState authState = GoogleDriveManager.getAuthState();
        if (authState.isAuthorized()) {

            AuthorizationService authorizationService = GoogleDriveManager.getAuthorizationService(AppSettings.getInstance());

            // The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
            // So I can't do network calls on this thread.
            // Instead, updating a class level variable, and waiting for it afterwards.
            // https://github.com/openid/AppAuth-Android/issues/123
            authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex != null) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(ex.toJsonString(), ex));
                        taskDone.set(true);
                        LOG.error(ex.toJsonString(), ex);
                        return;
                    }
                    googleDriveAccessToken = accessToken;
                    taskDone.set(true);
                }
            });

            // Wait for the performActionWithFreshTokens.execute callback
            // (which happens on the UI thread for some reason) to complete.
            while (!taskDone.get()) {
                Thread.sleep(500);
            }

            if (Strings.isNullOrEmpty(googleDriveAccessToken)) {
                LOG.error("Failed to fetch Access Token for Google Drive. Stopping this job.");
                return;
            }


            try {

                // Figure out the Folder ID to upload to, from the path; recursively create if it doesn't exist.
                String folderPath = preferenceHelper.getGoogleDriveFolderPath();
                String[] pathParts = folderPath.split("/");
                String parentFolderId = null;
                String latestFolderId = null;
                for (String part : pathParts) {
                    latestFolderId = getFileIdFromFileName(googleDriveAccessToken, part, parentFolderId);
                    if (!Strings.isNullOrEmpty(latestFolderId)) {
                        LOG.debug("Folder " + part + " found, folder ID is " + latestFolderId);
                    } else {
                        LOG.debug("Folder " + part + " not found, creating.");
                        latestFolderId = createEmptyFile(googleDriveAccessToken, part,
                                "application/vnd.google-apps.folder", Strings.isNullOrEmpty(parentFolderId) ? "root" : parentFolderId);
                    }
                    parentFolderId = latestFolderId;
                }

                String gpsLoggerFolderId = latestFolderId;

                if (Strings.isNullOrEmpty(gpsLoggerFolderId)) {
                    EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not create folder"));
                    return;
                }

                // Now search for the file
                String gpxFileId = getFileIdFromFileName(googleDriveAccessToken, fileName, gpsLoggerFolderId);

                if (Strings.isNullOrEmpty(gpxFileId)) {
                    LOG.debug("Creating an empty file first.");
                    gpxFileId = createEmptyFile(googleDriveAccessToken, fileName, getMimeTypeFromFileName(fileName), gpsLoggerFolderId);

                    if (Strings.isNullOrEmpty(gpxFileId)) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not create file"));
                        return;
                    }
                }

                // Upload contents to file
                if (!Strings.isNullOrEmpty(gpxFileId)) {
                    LOG.debug("Uploading file contents");
                    updateFileContents(googleDriveAccessToken, gpxFileId, fileName);
                }

                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(e.getMessage(), e));
            }

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

    private String updateFileContents(String accessToken, String gpxFileId, String fileName) throws Exception {
        FileInputStream fis = new FileInputStream(new File(preferenceHelper.getGpsLoggerFolder(), fileName));
        String fileId = null;

        String fileUpdateUrl = "https://www.googleapis.com/upload/drive/v3/files/" + gpxFileId + "?uploadType=media";

        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(fileUpdateUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        RequestBody body = RequestBody.create(MediaType.parse(getMimeTypeFromFileName(fileName)), Streams.getByteArrayFromInputStream(fis));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            requestBuilder.addHeader("X-HTTP-Method-Override", "PATCH");
        }
        requestBuilder = requestBuilder.method("PATCH", body);

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

    @Override
    protected int getRetryLimit() {
        return 3;
    }

    private String getMimeTypeFromFileName(String fileName) {
        if (fileName.endsWith("kml")) {
            return "application/vnd.google-earth.kml+xml";
        }

        if (fileName.endsWith("gpx")) {
            return "application/gpx+xml";
        }

        if (fileName.endsWith("zip")) {
            return "application/zip";
        }

        if (fileName.endsWith("xml")) {
            return "application/xml";
        }

        if (fileName.endsWith("nmea")) {
            return "text/plain";
        }

        if (fileName.endsWith("geojson")) {
            return "application/vnd.geo+json";
        }

        return "application/vnd.google-apps.spreadsheet";
    }

}

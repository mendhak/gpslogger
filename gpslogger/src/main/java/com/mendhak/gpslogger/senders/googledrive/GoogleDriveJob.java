package com.mendhak.gpslogger.senders.googledrive;

import android.os.Build;
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
    String fileName;
    private Handler handler;
    private HandlerThread handlerThread;
    final AtomicBoolean taskDone = new AtomicBoolean(false);

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

            handlerThread = new HandlerThread("HandlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());

            // The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
            // So I can't do network calls on this thread.
            // That's why I have to do a handlerThread and handler.post
            // https://github.com/openid/AppAuth-Android/issues/123
            authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex != null) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(ex.toJsonString(), ex));
                        return;
                    }

                    GoogleDriveUploadWorkflow task = new GoogleDriveUploadWorkflow(accessToken);
                    handler.post(task);
                    // I hate what I'm having to do here.
                    // Because there is a handler.post here, which is async, execution of onRun continues and finishes.
                    // The Job Queue scheduler notices that onRun finished, so it immediately starts executing the next Google Drive Job - each one with the same problem.
                    // This results in multiple Google Drive workflow tasks attempting to create folder paths in Google Drive.
                    // And since Google Drive is happy to not enforce unique folder names, this leads to multiple copies of the same folder appearing.
                    //
                    // The solution is to do a handler.post but then wait for the Runnable to notify() back.
                    // The taskDone variable is added here just in case the Runnable posts a notify() before the while loop can get started.
                    // https://stackoverflow.com/questions/20179193/calling-wait-after-posting-a-runnable-to-ui-thread-until-completion
                    // And remember the handler is only here because the `performActionWithFreshTokens.execute` runs on the UI thread, which doesn't allow network calls, aargh!!
                    synchronized(task) {
                        while(!taskDone.get()) {
                            try {
                                task.wait();
                            } catch (InterruptedException e) {
                                LOG.warn("Exception while waiting for a Google Drive upload to complete.", e);
                            }
                        }
                    }
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

    class GoogleDriveUploadWorkflow implements Runnable {

        String accessToken;

        GoogleDriveUploadWorkflow(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public void run() {
            try {

                // Figure out the Folder ID to upload to, from the path; create if it doesn't exist.
                String folderPath = preferenceHelper.getGoogleDriveFolderPath();
                String[] pathParts = folderPath.split("/");
                String parentFolderId = null;
                String latestFolderId = null;
                for (String part : pathParts) {
                    latestFolderId = getFileIdFromFileName(accessToken, part, parentFolderId);
                    if (!Strings.isNullOrEmpty(latestFolderId)) {
                        LOG.debug("Folder " + part + " found, folder ID is " + latestFolderId);
                    } else {
                        LOG.debug("Folder " + part + " not found, creating.");
                        latestFolderId = createEmptyFile(accessToken, part,
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
                String gpxFileId = getFileIdFromFileName(accessToken, fileName, gpsLoggerFolderId);

                if (Strings.isNullOrEmpty(gpxFileId)) {
                    LOG.debug("Creating an empty file first.");
                    gpxFileId = createEmptyFile(accessToken, fileName, getMimeTypeFromFileName(fileName), gpsLoggerFolderId);

                    if (Strings.isNullOrEmpty(gpxFileId)) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not create file"));
                        return;
                    }
                }

                // Upload contents to file
                if (!Strings.isNullOrEmpty(gpxFileId)) {
                    LOG.debug("Uploading file contents");
                    updateFileContents(accessToken, gpxFileId, fileName);
                }

                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(e.getMessage(), e));
            } finally {
                handlerThread.quit();
                taskDone.set(true);
                synchronized (this){
                    notify();
                }

            }
        }
    }

}

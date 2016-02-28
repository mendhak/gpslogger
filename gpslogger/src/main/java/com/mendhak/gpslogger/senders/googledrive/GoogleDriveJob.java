package com.mendhak.gpslogger.senders.googledrive;

import android.os.Build;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class GoogleDriveJob extends Job {
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GoogleDriveJob.class.getSimpleName());
    String token;
    File gpxFile;
    String googleDriveFolderName;

    protected GoogleDriveJob(File gpxFile, String googleDriveFolderName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(gpxFile)));
        this.gpxFile = gpxFile;
        this.googleDriveFolderName = googleDriveFolderName;

    }

    public static String getJobTag(File gpxFile){
        return "GOOGLEDRIVE" + gpxFile.getName();
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
        token = manager.GetToken();

        FileInputStream fis = new FileInputStream(gpxFile);
        String fileName = gpxFile.getName();

        String gpsLoggerFolderId = getFileIdFromFileName(token, googleDriveFolderName, null);

        if (Utilities.IsNullOrEmpty(gpsLoggerFolderId)) {
            //Couldn't find folder, must create it
            gpsLoggerFolderId = createEmptyFile(token, googleDriveFolderName, "application/vnd.google-apps.folder", "root");

            if (Utilities.IsNullOrEmpty(gpsLoggerFolderId)) {
                EventBus.getDefault().post(new UploadEvents.GDocs(false));
                return;
            }
        }

        //Now search for the file
        String gpxFileId = getFileIdFromFileName(token, fileName, gpsLoggerFolderId);

        if (Utilities.IsNullOrEmpty(gpxFileId)) {
            //Create empty file first
            gpxFileId = createEmptyFile(token, fileName, getMimeTypeFromFileName(fileName), gpsLoggerFolderId);

            if (Utilities.IsNullOrEmpty(gpxFileId)) {
                EventBus.getDefault().post(new UploadEvents.GDocs(false));
                return;
            }
        }

        if (!Utilities.IsNullOrEmpty(gpxFileId)) {
            //Set file's contents
            updateFileContents(token, gpxFileId, Utilities.GetByteArrayFromInputStream(fis), fileName);
        }
        EventBus.getDefault().post(new UploadEvents.GDocs(true));
    }



    private String updateFileContents(String authToken, String gpxFileId, byte[] fileContents, String fileName) {
        HttpURLConnection conn = null;
        String fileId = null;

        String fileUpdateUrl = "https://www.googleapis.com/upload/drive/v2/files/" + gpxFileId + "?uploadType=media";

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(fileUpdateUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setRequestProperty("Content-Type", getMimeTypeFromFileName(fileName));
            conn.setRequestProperty("Content-Length", String.valueOf(fileContents.length));

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

			conn.setConnectTimeout(10000);
			conn.setReadTimeout(30000);

            DataOutputStream wr = new DataOutputStream(
                    conn.getOutputStream());
            wr.write(fileContents);
            wr.flush();
            wr.close();

            String fileMetadata = Utilities.GetStringFromInputStream(conn.getInputStream());

            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            fileId = fileMetadataJson.getString("id");
            tracer.debug("File updated : " + fileId);

        } catch (Exception e) {
            tracer.error("Could not update contents", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return fileId;
    }

    private String createEmptyFile(String authToken, String fileName, String mimeType, String parentFolderId) {

        String fileId = null;
        HttpURLConnection conn = null;

        String createFileUrl = "https://www.googleapis.com/drive/v2/files";

        String createFilePayload = "   {\n" +
                "             \"title\": \"" + fileName + "\",\n" +
                "             \"mimeType\": \"" + mimeType + "\",\n" +
                "             \"parents\": [\n" +
                "              {\n" +
                "               \"id\": \"" + parentFolderId + "\"\n" +
                "              }\n" +
                "             ]\n" +
                "            }";

        try {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(createFileUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

			conn.setConnectTimeout(10000);
			conn.setReadTimeout(30000);

            DataOutputStream wr = new DataOutputStream(
                    conn.getOutputStream());
            wr.writeBytes(createFilePayload);
            wr.flush();
            wr.close();

            fileId = null;

            String fileMetadata = Utilities.GetStringFromInputStream(conn.getInputStream());

            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            fileId = fileMetadataJson.getString("id");
            tracer.debug("File created with ID " + fileId + " of type " + mimeType);

        } catch (Exception e) {
            tracer.error("Could not create file", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

        }

        return fileId;
    }


    private String getFileIdFromFileName(String authToken, String fileName, String inFolderId) {

        HttpURLConnection conn = null;
        String fileId = "";

        try {

            fileName = URLEncoder.encode(fileName, "UTF-8");

            String inFolderParam = "";
            if(!Utilities.IsNullOrEmpty(inFolderId)){
                inFolderParam = "+and+'" + inFolderId + "'+in+parents";
            }

            //To search in a folder:
            String searchUrl = "https://www.googleapis.com/drive/v2/files?q=title%20%3D%20%27" + fileName + "%27%20and%20trashed%20%3D%20false" + inFolderParam;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(searchUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "OAuth " + authToken);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(30000);
	
            String fileMetadata = Utilities.GetStringFromInputStream(conn.getInputStream());


            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            if (fileMetadataJson.getJSONArray("items") != null && fileMetadataJson.getJSONArray("items").length() > 0) {
                fileId = fileMetadataJson.getJSONArray("items").getJSONObject(0).get("id").toString();
                tracer.debug("Found file with ID " + fileId);
            }

        } catch (Exception e) {
            tracer.error("SearchForGPSLoggerFile", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return fileId;
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

        return "application/vnd.google-apps.spreadsheet";
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.GDocs(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to Google Drive", throwable);
        return false;
    }
}

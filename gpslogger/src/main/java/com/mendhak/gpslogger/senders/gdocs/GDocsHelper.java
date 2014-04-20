/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.senders.gdocs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;


public class GDocsHelper implements IActionListener, IFileSender
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GDocsHelper.class.getSimpleName());
    Context ctx;
    IActionListener callback;


    /*
    To revoke permissions:
    (new Android)
    ./adb -e shell 'sqlite3 /data/system/users/0/accounts.db "delete from grants;"'
    or
    (old Android)
   ./adb -e shell 'sqlite3 /data/system/accounts.db "delete from grants;"'
     */

    public GDocsHelper(Context applicationContext, IActionListener callback)
    {

        this.ctx = applicationContext;
        this.callback = callback;
    }

    public static String GetOauth2Scope()
    {
        return "oauth2:https://www.googleapis.com/auth/drive.file";
    }

    /**
     * Gets the stored authToken, which may be expired
     */
    public static String GetAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDRIVE_AUTH_TOKEN", "");
    }

    /**
     * Gets the stored account name
     */
    public static String GetAccountName(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDRIVE_ACCOUNT_NAME", "");
    }

    public static void SetAccountName(Context applicationContext, String accountName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("GDRIVE_ACCOUNT_NAME", accountName);
        editor.commit();
    }

    /**
     * Saves the authToken and account name into shared preferences
     */
    public static void SaveAuthToken(Context applicationContext, String authToken)
    {
        try
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            SharedPreferences.Editor editor = prefs.edit();

            tracer.debug("Saving GDocs authToken: " + authToken);
            editor.putString("GDRIVE_AUTH_TOKEN", authToken);
            editor.commit();
        }
        catch (Exception e)
        {

            tracer.error("GDocsHelper.SaveAuthToken", e);
        }

    }

    /**
     * Removes the authToken and account name from storage
     */
    public static void ClearAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDRIVE_AUTH_TOKEN");
        editor.remove("GDRIVE_ACCOUNT_NAME");
        editor.commit();
    }


    /**
     * Returns whether the app is authorized to perform Google API operations
     *
     * @param applicationContext
     * @return
     */
    public static boolean IsLinked(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String gdocsAuthToken = prefs.getString("GDRIVE_AUTH_TOKEN", "");
        String gdocsAccount = prefs.getString("GDRIVE_ACCOUNT_NAME", "");
        return gdocsAuthToken.length() > 0 && gdocsAccount.length() > 0;
    }


    @Override
    public void UploadFile(List<File> files)
    {
        File zipFile = null;


        for (File f : files)
        {
            if (f.getName().contains(".zip"))
            {
                zipFile = f;
                break;
            }
        }

        if (zipFile != null)
        {
            UploadFile(zipFile.getName());
        }
        else
        {
            for (File f : files)
            {
                UploadFile(f.getName());
            }
        }
    }

    public void UploadFile(final String fileName)
    {

        if (!IsLinked(ctx))
        {
            callback.OnFailure();
            return;
        }

        try
        {

            File gpsDir = new File(AppSettings.getGpsLoggerFolder());
            File gpxFile = new File(gpsDir, fileName);
            FileInputStream fis = new FileInputStream(gpxFile);


            Thread t = new Thread(new GDocsUploadHandler(fis, fileName, GDocsHelper.this));
            t.start();


        }
        catch (Exception e)
        {
            callback.OnFailure();
            tracer.error("GDocsHelper.UploadFile", e);
        }
    }

    private class GDocsUploadHandler implements Runnable
    {

        String fileName;
        InputStream inputStream;
        IActionListener callback;

        GDocsUploadHandler(InputStream inputStream, String fileName,
                           IActionListener callback)
        {

            this.inputStream = inputStream;
            this.fileName = fileName;
            this.callback = callback;
        }

        @Override
        public void run()
        {
            try
            {

                String token = GoogleAuthUtil.getTokenWithNotification(ctx, GetAccountName(ctx), GetOauth2Scope(), new Bundle());
                GDocsHelper.SaveAuthToken(ctx, token);
                tracer.debug(token);

                String gpsLoggerFolderId = GetFileIdFromFileName(token, "GPSLogger For Android");

                if(Utilities.IsNullOrEmpty(gpsLoggerFolderId))
                {
                    //Couldn't find folder, must create it
                    gpsLoggerFolderId = CreateEmptyFile(token, "GPSLogger For Android", "application/vnd.google-apps.folder", "root");

                    if(Utilities.IsNullOrEmpty(gpsLoggerFolderId))
                    {
                        callback.OnFailure();
                        return;
                    }
                }


                //Now search for the file
                String gpxFileId = GetFileIdFromFileName(token,fileName);

                if(Utilities.IsNullOrEmpty(gpxFileId))
                {
                    //Create empty file first
                    gpxFileId = CreateEmptyFile(token, fileName, GetMimeTypeFromFileName(fileName), gpsLoggerFolderId);

                    if(Utilities.IsNullOrEmpty(gpxFileId))
                    {
                        callback.OnFailure();
                        return;
                    }
                }

                if(!Utilities.IsNullOrEmpty(gpxFileId))
                {
                    //Set file's contents
                    UpdateFileContents(token, gpxFileId, Utilities.GetByteArrayFromInputStream(inputStream), fileName);
                }

                callback.OnComplete();

            }
            catch (Exception e)
            {
                tracer.error("GDocsUploadHandler", e);
                callback.OnFailure();
            }


        }
    }

    private String UpdateFileContents(String authToken, String gpxFileId, byte[] fileContents, String fileName)
    {
        HttpURLConnection conn = null;
        String fileId = null;

        String fileUpdateUrl = "https://www.googleapis.com/upload/drive/v2/files/"+ gpxFileId + "?uploadType=media";

        try
        {


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(fileUpdateUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setRequestProperty("Content-Type", GetMimeTypeFromFileName(fileName));
            conn.setRequestProperty("Content-Length", String.valueOf(fileContents.length));

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(
                    conn.getOutputStream());
            wr.write(fileContents);
            wr.flush();
            wr.close();

            String fileMetadata = Utilities.GetStringFromInputStream(conn.getInputStream());

            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            fileId = fileMetadataJson.getString("id");
            tracer.debug("File updated : " + fileId);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }

        }

        return fileId;

    }

    private String CreateEmptyFile(String authToken, String fileName, String mimeType, String parentFolderId)
    {

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

        try
        {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            {
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

        }
        catch (Exception e)
        {

            System.out.println(e.getMessage());
            System.out.println(e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }

        }

        return fileId;
    }


    private String GetFileIdFromFileName(String authToken, String fileName)
    {

        HttpURLConnection conn = null;
        String fileId = "";

        try
        {

            fileName = URLEncoder.encode(fileName, "UTF-8");
            String searchUrl = "https://www.googleapis.com/drive/v2/files?q=title%20%3D%20%27" + fileName + "%27%20and%20trashed%20%3D%20false";


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            {
                //Due to a pre-froyo bug
                //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                System.setProperty("http.keepAlive", "false");
            }

            URL url = new URL(searchUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "OAuth " + authToken);

            String fileMetadata = Utilities.GetStringFromInputStream(conn.getInputStream());


            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            if(fileMetadataJson.getJSONArray("items") != null && fileMetadataJson.getJSONArray("items").length() > 0)
            {
                fileId = fileMetadataJson.getJSONArray("items").getJSONObject(0).get("id").toString();
                tracer.debug("Found file with ID " + fileId);
            }

        }
        catch (Exception e)
        {
            tracer.error("SearchForGPSLoggerFile", e);
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }

        return fileId;
    }

    private String GetMimeTypeFromFileName(String fileName){
        if(fileName.endsWith("kml")){
            return "application/vnd.google-earth.kml+xml";
        }

        if(fileName.endsWith("gpx")){
            return "application/gpx+xml";
        }

        if(fileName.endsWith("zip")){
            return "application/zip";
        }

        if(fileName.endsWith("xml")){
            return "application/xml";
        }

        if(fileName.endsWith("nmea")){
            return "application/x-nmea";
        }

        return "application/vnd.google-apps.spreadsheet";
    }




    @Override
    public boolean accept(File dir, String name)
    {
        return true;
    }

    @Override
    public void OnComplete()
    {
        callback.OnComplete();
    }

    @Override
    public void OnFailure()
    {
        callback.OnFailure();
    }
}

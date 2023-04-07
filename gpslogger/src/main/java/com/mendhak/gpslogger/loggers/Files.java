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

package com.mendhak.gpslogger.loggers;


import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Files {

    private static final Logger LOG = Logs.of(Files.class);

    /**
     * Gets the GPSLogger-specific MIME type to use for a given filename/extension
     *
     * @param fileName
     * @return
     */
    public static String getMimeTypeFromFileName(String fileName) {
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

        if (fileName.endsWith("nmea") || fileName.endsWith("txt")) {
            return "text/plain";
        }

        if (fileName.endsWith("geojson")) {
            return "application/vnd.geo+json";
        }

        if (fileName.endsWith("csv")){
            return "application/vnd.google-apps.spreadsheet";
        }

        return "application/octet-stream";

    }

    public static void addToMediaDatabase(File file, String mimeType){

        MediaScannerConnection.scanFile(AppSettings.getInstance(),
                new String[]{file.getPath()},
                new String[]{mimeType},
                null);
    }

    public static File[] fromFolder(File folder) {
        return fromFolder(folder, null);
    }

    public static File[] fromFolder(File folder, FilenameFilter filter) {

        if (folder == null || !folder.exists() || folder.listFiles() == null) {
            return new File[]{};
        } else {
            if (filter != null) {
                return folder.listFiles(filter);
            }
            return folder.listFiles();
        }
    }

    public static File storageFolder(Context context){
        File storageFolder = context.getExternalFilesDir(null);
        if(storageFolder == null){
            storageFolder = context.getFilesDir();
        }
        return storageFolder;
    }

    public static boolean hasSDCard(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return context.getExternalFilesDirs(null).length > 1;
        }else {
            File storageDir = new File("/storage");
            return  storageDir.listFiles().length > 1;
        }
    }

    public static boolean isAllowedToWriteTo(String gpsLoggerFolder) {
        return new File(gpsLoggerFolder).canWrite();
    }

    public static String getAssetFileAsString(String pathToAsset, Context context){
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(pathToAsset);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
//            Log.e(TAG, "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
//                    Log.e(TAG, "Error closing asset " + name);
                }
            }
        }

        return null;

    }

    public static File createTestFile() throws IOException {
        File gpxFolder = new File(PreferenceHelper.getInstance().getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");
        if (!testFile.exists()) {
            testFile.createNewFile();

            FileOutputStream initialWriter = new FileOutputStream(testFile, true);
            BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

            initialOutput.write("<x>This is a test file</x>".getBytes());
            initialOutput.flush();
            initialOutput.close();

            Files.addToMediaDatabase(testFile, "text/xml");
        }

        return testFile;
    }

    public static boolean reallyExists(File gpxFile) {
        // Sometimes .isFile returns false even if a file exists.
        // This guesswork tries to determine whether file exists in a few different ways.
        return gpxFile.isFile() || gpxFile.getAbsoluteFile().exists() || gpxFile.getAbsoluteFile().isFile();
    }

    public static void copyFile(File sourceLocation, File targetLocation)
            throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(sourceLocation);
        OutputStream out = new FileOutputStream(targetLocation);

        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void DownloadFromUrl(String url, File destination) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        Response response = client.newCall(request).execute();

        if(response.isSuccessful()){
            LOG.debug("Response successful");
            InputStream inputStream = response.body().byteStream();


            OutputStream outputStream = new FileOutputStream(destination);
            Streams.copyIntoStream(inputStream, outputStream);
            response.body().close();
            LOG.debug("Wrote to properties file");

        }


    }

    public static String getBaseName(String url) {
        return getBaseName(Uri.parse(url));
    }

    public static String getBaseName(Uri data) {
        if(data == null || Strings.isNullOrEmpty(data.toString()))
        {
            return "";
        }

        String baseFileName = data.getLastPathSegment();
        int pos = baseFileName.lastIndexOf(".");
        if (pos > 0 && pos < (baseFileName.length() - 1)) {
            baseFileName = baseFileName.substring(0, pos);
        }



        return baseFileName;

    }

    @SuppressWarnings({"unchecked"})
    public static void addItemToCacheFile(String item, String cacheKey, Context ctx){
        List<String> existingList = getListFromCacheFile(cacheKey, ctx);
        final LinkedHashSet<String> set = new LinkedHashSet(existingList);
        set.add(item);
        saveListToCacheFile(new ArrayList<>(set), cacheKey, ctx);
    }

    public static void saveListToCacheFile(List<String> items, String cacheKey, Context ctx){
        try
        {

            if(items.size() > 10) {
                items = new ArrayList<>(items.subList(1, 11));
            }

            File cacheFile = new File(ctx.getCacheDir(), cacheKey);
            cacheFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(cacheFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(items);
            oos.close();
            fos.close();
        }
        catch (Exception ioe)
        {
            LOG.error("Could not save items to cache");
        }
    }


    @SuppressWarnings({"unchecked"})
    public static List<String> getListFromCacheFile(String cacheKey, Context ctx){
        ArrayList<String> items = new ArrayList<>();
        try
        {
            File cacheFile = new File(ctx.getCacheDir(), cacheKey);
            cacheFile.createNewFile();

            if(cacheFile.length() > 0){
                FileInputStream fis = new FileInputStream(cacheFile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                items = (ArrayList<String>) ois.readObject();

                ois.close();
                fis.close();
            }


        }
        catch (Exception ex){
            LOG.debug("Could not retrieve from cache", ex);
        }
        return items;
    }

    /**
     * For a given File URI of the file:// format, get the corresponding Document URI in the content:// format.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static Uri getDocumentUriFromFileUri(Context context, File file) {
        String filePath = file.getAbsolutePath();
        String volumeName = getVolumeName(context, file);

        if(volumeName.equalsIgnoreCase("primary")){
            filePath = filePath.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        }
        else {
            filePath = filePath.replace("/storage/"+volumeName, "");
        }

        // Create the document URI with the content:// scheme.
        Uri documentUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", volumeName + ":" + filePath); //file.getAbsolutePath().substring(1)

        return documentUri;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static String getVolumeName(Context context, File file) {
        String volumeName = null;

        // Get the external storage volumes
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumes = storageManager.getStorageVolumes().toArray(new StorageVolume[0]);

        // Go through all the volumes, find the volume for the given file
        for (StorageVolume storageVolume : storageVolumes) {
            String path = file.getAbsolutePath();
            String volumePath = storageVolume.getDirectory().getAbsolutePath();
            if (path.startsWith(volumePath)) {
                volumeName = storageVolume.getUuid();
                break;
            }
        }

        // If the volume name is null, use the primary volume
        if (volumeName == null) {
            volumeName = "primary";
        }

        return volumeName;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    public static void setFilePathAsClickableLink(Context context, TextView txtFilename, String gpsLoggerFolder){
        txtFilename.setTextIsSelectable(true);
        txtFilename.setSelectAllOnFocus(true);

        ClickableSpan clickSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                File file = new File(gpsLoggerFolder);

                Uri convertedUri = getDocumentUriFromFileUri(context, file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri folderUri = Uri.parse(convertedUri.toString());
                intent.setDataAndType(folderUri, "vnd.android.document/directory");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri);

                context.startActivity(intent);
            }
        };

        Spannable spanText = new SpannableString(txtFilename.getText());
        //Make the folder path clickable but not the filename itself.
        spanText.setSpan(clickSpan, txtFilename.getText().toString().indexOf("\n"), txtFilename.getText().toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        txtFilename.setText(spanText, TextView.BufferType.SPANNABLE);
        txtFilename.setMovementMethod(LinkMovementMethod.getInstance());
    }
}

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.List;


public class GDocsHelper implements IFileSender {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GDocsHelper.class.getSimpleName());
    Context context;

    /*
    To revoke permissions:
    (new Android)
    ./adb -e shell 'sqlite3 /data/system/users/0/accounts.db "delete from grants;"'
    or
    (old Android)
   ./adb -e shell 'sqlite3 /data/system/accounts.db "delete from grants;"'
     */

    public GDocsHelper(Context applicationContext) {

        this.context = applicationContext;
    }

    public static String GetOauth2Scope() {
        return "oauth2:https://www.googleapis.com/auth/drive.file";
    }

    /**
     * Gets the stored authToken, which may be expired
     */
    public static String GetAuthToken(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDRIVE_AUTH_TOKEN", "");
    }

    /**
     * Gets the stored account name
     */
    public static String GetAccountName(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDRIVE_ACCOUNT_NAME", "");
    }

    public static void SetAccountName(Context applicationContext, String accountName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("GDRIVE_ACCOUNT_NAME", accountName);
        editor.apply();
    }

    /**
     * Saves the authToken and account name into shared preferences
     */
    public static void SaveAuthToken(Context applicationContext, String authToken) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            SharedPreferences.Editor editor = prefs.edit();

            tracer.debug("Saving GDocs authToken: " + authToken);
            editor.putString("GDRIVE_AUTH_TOKEN", authToken);
            editor.apply();
        } catch (Exception e) {

            tracer.error("GDocsHelper.SaveAuthToken", e);
        }
    }

    /**
     * Removes the authToken and account name from storage
     */
    public static void ClearAuthToken(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDRIVE_AUTH_TOKEN");
        editor.remove("GDRIVE_ACCOUNT_NAME");
        editor.apply();
    }


    /**
     * Returns whether the app is authorized to perform Google API operations
     *
     */
    public static boolean IsLinked(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String gdocsAuthToken = prefs.getString("GDRIVE_AUTH_TOKEN", "");
        String gdocsAccount = prefs.getString("GDRIVE_ACCOUNT_NAME", "");
        return gdocsAuthToken.length() > 0 && gdocsAccount.length() > 0;
    }

    @Override
    public void UploadFile(List<File> files) {
        for (File f : files) {
            UploadFile(f.getName());
        }
    }

    public void UploadFile(final String fileName) {
        if (!IsLinked(context)) {
            EventBus.getDefault().post(new UploadEvents.GDocs(false));
            return;
        }

        try {
            File gpsDir = new File(AppSettings.getGpsLoggerFolder());
            File gpxFile = new File(gpsDir, fileName);

            tracer.debug("Sending file to GDocs: " + fileName);
            new GDocsTokenAsyncTask(gpxFile).execute(context);
        } catch (Exception e) {
            EventBus.getDefault().post(new UploadEvents.GDocs(false));
            tracer.error("GDocsHelper.UploadFile", e);
        }
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    private class GDocsTokenAsyncTask extends AsyncTask<Context, Void, String> {

        File gpxFile;
        public GDocsTokenAsyncTask(File gpxFile){
            this.gpxFile = gpxFile;
        }

        @Override
        protected String doInBackground(Context... params) {
            try {
                return GoogleAuthUtil.getTokenWithNotification(params[0], GetAccountName(params[0]), GetOauth2Scope(), new Bundle());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String token) {
            tracer.debug("On Post Execute with token: " + token);
            GDocsHelper.SaveAuthToken(context, token);

            JobManager jobManager = AppSettings.GetJobManager();
            jobManager.addJobInBackground(new GDocsJob(token, gpxFile));
        }
    }

}

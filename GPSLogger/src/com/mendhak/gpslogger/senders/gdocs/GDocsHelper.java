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

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import java.io.*;
import java.util.List;


public class GDocsHelper implements IActionListener, IFileSender
{
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
        return  "oauth2:https://www.googleapis.com/auth/drive.file";
    }

     /**
     * Gets the stored authToken, which may be expired
     */
    public static String GetAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDOCS_AUTH_TOKEN", "");
    }

    /**
     * Gets the stored account name
     */
    public static String GetAccountName(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDOCS_ACCOUNT_NAME", "");
    }

    public static void SetAccountName(Context applicationContext, String accountName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("GDOCS_ACCOUNT_NAME", accountName);
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

            Utilities.LogDebug("Saving GDocs authToken: " + authToken);
            editor.putString("GDOCS_AUTH_TOKEN", authToken);
            editor.commit();
        }
        catch (Exception e)
        {

            Utilities.LogError("GDocsHelper.SaveAuthToken", e);
        }

    }

    /**
     * Removes the authToken and account name from storage
     *
     */
    public static void ClearAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDOCS_AUTH_TOKEN");
        editor.remove("GDOCS_ACCOUNT_NAME");
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
        String gdocsAuthToken = prefs.getString("GDOCS_AUTH_TOKEN", "");
        String gdocsAccount = prefs.getString("GDOCS_ACCOUNT_NAME", "");
        return gdocsAuthToken.length() > 0 && gdocsAccount.length() > 0;
    }


//    void RefreshToken()
//    {
//        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>()
//        {
//            @Override
//            protected String doInBackground(Void... params)
//            {
//                try
//                {
//                    // Retrieve a token for the given account and scope. It will always return either
//                    // a non-empty String or throw an exception.
//
//                    return token;
//                }
//                catch (Exception e)
//                {
//                    Utilities.LogError("RefreshToken", e);
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(String authToken)
//            {
//                if(authToken != null)
//                {
//                    SaveAuthToken(ctx, authToken);
//                    Utilities.LogDebug(authToken);
//                }
//
//            }
//        };
//        task.execute();
//    }


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

            File gpsDir = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
            File gpxFile = new File(gpsDir, fileName);
            FileInputStream fis = new FileInputStream(gpxFile);


            Thread t = new Thread(new GDocsUploadHandler(fis, fileName, GDocsHelper.this));
            t.start();


        }
        catch (Exception e)
        {
            callback.OnFailure();
            Utilities.LogError("GDocsHelper.UploadFile", e);
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
                Utilities.LogDebug(token);
            }
            catch (Exception e)
            {
                Utilities.LogError("GDocsUploadHandler.RefreshToken", e);
            }



        }
    }



        @Override
    public boolean accept(File dir, String filename)
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
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

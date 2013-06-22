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

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class GDocsHelper implements IActionListener, IFileSender
{
    Context ctx;
    IActionListener callback;
    private static final int USER_RECOVERABLE_AUTH = 5;
    private static final int ACCOUNT_PICKER = 2;

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

    /**
     * OAuth 2 scope to use
     */
    private static final String SCOPE = "oauth2:https://docs.google.com/feeds/";




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
     * @param applicationContext
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


    @Override
    public void UploadFile(List<File> files)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean accept(File dir, String filename)
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void OnComplete()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void OnFailure()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

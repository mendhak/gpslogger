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

package com.mendhak.gpslogger.senders.osm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.senders.IFileSender;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class OSMHelper implements IActionListener, IFileSender
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OSMHelper.class.getSimpleName());
    IActionListener callback;
    Context ctx;

    public OSMHelper(Context ctx, IActionListener callback)
    {

        this.ctx = ctx;
        this.callback = callback;
    }

    public static OAuthProvider GetOSMAuthProvider(Context ctx)
    {
        return new CommonsHttpOAuthProvider(
                ctx.getString(R.string.osm_requesttoken_url),
                ctx.getString(R.string.osm_accesstoken_url),
                ctx.getString(R.string.osm_authorize_url));

    }

    public static boolean IsOsmAuthorized(Context ctx)
    {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String oAuthAccessToken = prefs.getString("osm_accesstoken", "");

        return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
    }

    public static Intent GetOsmSettingsIntent(Context ctx)
    {
        Intent intentOsm;

        intentOsm = new Intent(ctx.getPackageName() + ".OSM_AUTHORIZE");
        intentOsm.setData(Uri.parse("gpslogger://authorize"));

        return intentOsm;
    }


    public static OAuthConsumer GetOSMAuthConsumer(Context ctx)
    {

        OAuthConsumer consumer = null;

        try
        {
            int osmConsumerKey = ctx.getResources().getIdentifier(
                    "osm_consumerkey", "string", ctx.getPackageName());
            int osmConsumerSecret = ctx.getResources().getIdentifier(
                    "osm_consumersecret", "string", ctx.getPackageName());
            consumer = new CommonsHttpOAuthConsumer(
                    ctx.getString(osmConsumerKey),
                    ctx.getString(osmConsumerSecret));

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            String osmAccessToken = prefs.getString("osm_accesstoken", "");
            String osmAccessTokenSecret = prefs.getString(
                    "osm_accesstokensecret", "");

            if (osmAccessToken != null && osmAccessToken.length() > 0
                    && osmAccessTokenSecret != null
                    && osmAccessTokenSecret.length() > 0)
            {
                consumer.setTokenWithSecret(osmAccessToken,
                        osmAccessTokenSecret);
            }

        }
        catch (Exception e)
        {
            //Swallow the exception
        }

        return consumer;
    }

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
        callback.OnFailure();
    }

    @Override
    public void UploadFile(List<File> files)
    {
        //Upload only GPX

        for (File f : files)
        {
            if (f.getName().contains(".gpx"))
            {
                UploadFile(f.getName());
            }

        }

    }


    public void UploadFile(String fileName)
    {
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        File chosenFile = new File(gpxFolder, fileName);
        OAuthConsumer consumer = GetOSMAuthConsumer(ctx);
        String gpsTraceUrl = ctx.getString(R.string.osm_gpstrace_url);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String description = prefs.getString("osm_description", "");
        String tags = prefs.getString("osm_tags", "");
        String visibility = prefs.getString("osm_visibility", "private");

        Thread t = new Thread(new OsmUploadHandler(this, consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
        t.start();
    }

    @Override
    public boolean accept(File dir, String name)
    {
        return name.toLowerCase().contains(".gpx");
    }


    private class OsmUploadHandler implements Runnable
    {
        OAuthConsumer consumer;
        String gpsTraceUrl;
        File chosenFile;
        String description;
        String tags;
        String visibility;
        IActionListener helper;

        public OsmUploadHandler(IActionListener helper, OAuthConsumer consumer, String gpsTraceUrl, File chosenFile, String description, String tags, String visibility)
        {
            this.consumer = consumer;
            this.gpsTraceUrl = gpsTraceUrl;
            this.chosenFile = chosenFile;
            this.description = description;
            this.tags = tags;
            this.visibility = visibility;
            this.helper = helper;
        }

        public void run()
        {
            try
            {
                HttpPost request = new HttpPost(gpsTraceUrl);

                consumer.sign(request);

                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                FileBody gpxBody = new FileBody(chosenFile);

                entity.addPart("file", gpxBody);
                if (description == null || description.length() <= 0)
                {
                    description = "GPSLogger for Android";
                }

                entity.addPart("description", new StringBody(description));
                entity.addPart("tags", new StringBody(tags));
                entity.addPart("visibility", new StringBody(visibility));

                request.setEntity(entity);
                DefaultHttpClient httpClient = new DefaultHttpClient();

                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                tracer.debug("OSM Upload - " + String.valueOf(statusCode));
                helper.OnComplete();

            }
            catch (Exception e)
            {
                helper.OnFailure();
                tracer.error("OsmUploadHelper.run", e);
            }
        }
    }

}




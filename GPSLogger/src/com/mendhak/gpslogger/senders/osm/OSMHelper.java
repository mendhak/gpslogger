package com.mendhak.gpslogger.senders.osm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
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

import java.io.File;

public class OSMHelper implements IActionListener
{

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

        if (!IsOsmAuthorized(ctx))
        {
            intentOsm = new Intent(ctx.getPackageName() + ".OSM_AUTHORIZE");
            intentOsm.setData(Uri.parse("gpslogger://authorize"));
        }
        else
        {
            intentOsm = new Intent(ctx.getPackageName() + ".OSM_SETUP");

        }

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


    public void UploadGpsTrace(String fileName)
    {

        File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
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

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
        callback.OnFailure();
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
                Utilities.LogDebug("OSM Upload - " + String.valueOf(statusCode));
                helper.OnComplete();

            }
            catch (Exception e)
            {
                helper.OnFailure();
                Utilities.LogError("OsmUploadHelper.run", e);
            }
        }
    }

}




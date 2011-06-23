package com.mendhak.gpslogger.helpers;

import java.io.File;

import oauth.signpost.OAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.Utilities;

public class OSMHelper implements IOsmHelper
{

	private GpsMainActivity mainActivity;

	
	public OSMHelper(GpsMainActivity activity)
	{
		this.mainActivity = activity;
	}
	
	public void UploadGpsTrace(String fileName)
	{
		
		File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
		File chosenFile = new File(gpxFolder, fileName);
		OAuthConsumer consumer = Utilities.GetOSMAuthConsumer(mainActivity.getBaseContext());
		String gpsTraceUrl = mainActivity.getString(R.string.osm_gpstrace_url);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity.getBaseContext());
		String description = prefs.getString("osm_description", "");
		String tags = prefs.getString("osm_tags", "");
		String visibility = prefs.getString("osm_visibility", "private");
		
		Thread t = new Thread(new OsmUploadHandler(this, consumer, gpsTraceUrl, chosenFile, description, tags, visibility));
		t.start();
	}
	
	public void OnComplete()
	{
		mainActivity.handler.post(mainActivity.updateOsmUpload);
	}
	
	
	
	
	private class OsmUploadHandler implements Runnable
	{
		OAuthConsumer consumer;
		String gpsTraceUrl;
		File chosenFile;
		String description;
		String tags;
		String visibility;
		IOsmHelper helper;
		
		public OsmUploadHandler(IOsmHelper helper, OAuthConsumer consumer, String gpsTraceUrl, File chosenFile, String description, String tags, String visibility)
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
	            if(description == null || description.length() <= 0)
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
			catch(Exception e)
			{
					Utilities.LogError("OsmUploadHelper.run", e);
			}
		}
	}

}


interface IOsmHelper
{
	public void OnComplete();
}

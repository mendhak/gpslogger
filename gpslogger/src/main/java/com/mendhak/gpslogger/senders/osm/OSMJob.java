package com.mendhak.gpslogger.senders.osm;

import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import oauth.signpost.OAuthConsumer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OSMJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OSMJob.class.getSimpleName());
    OAuthConsumer consumer;
    String gpsTraceUrl;
    File chosenFile;
    String description;
    String tags;
    String visibility;

    protected OSMJob(OAuthConsumer consumer, String gpsTraceUrl, File chosenFile, String description, String tags, String visibility) {
        super(new Params(1).requireNetwork().persist());

        this.consumer = consumer;
        this.gpsTraceUrl = gpsTraceUrl;
        this.chosenFile = chosenFile;
        this.description = description;
        this.tags = tags;
        this.visibility = visibility;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        HttpPost request = new HttpPost(gpsTraceUrl);

        consumer.sign(request);

        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        FileBody gpxBody = new FileBody(chosenFile);

        entity.addPart("file", gpxBody);
        if (description == null || description.length() <= 0) {
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
        EventBus.getDefault().post(new UploadEvents.OpenStreetMap(true));
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.OpenStreetMap(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not send to OpenStreetMap", throwable);
        return false;
    }
}

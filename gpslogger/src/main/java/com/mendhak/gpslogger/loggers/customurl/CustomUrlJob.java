package com.mendhak.gpslogger.loggers.customurl;

import android.location.Location;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public class CustomUrlJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(CustomUrlJob.class.getSimpleName());
    private SerializableLocation loc;
    private String annotation;
    private int satellites;
    private String logUrl;
    private float batteryLevel;
    private String androidId;

    public CustomUrlJob(String customLoggingUrl, Location loc, String annotation, int satellites, float batteryLevel, String androidId) {
        super(new Params(1).requireNetwork().persist());
        this.loc = new SerializableLocation(loc);
        this.annotation = annotation;
        this.satellites = satellites;
        this.logUrl = customLoggingUrl;
        this.batteryLevel = batteryLevel;
        this.androidId = androidId;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        HttpURLConnection conn;

        //String logUrl = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV
        // &spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER";

        logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(loc.getLatitude()));
        logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(loc.getLongitude()));
        logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(satellites));
        logUrl = logUrl.replaceAll("(?i)%desc", String.valueOf(URLEncoder.encode(Utilities.HtmlDecode(annotation), "UTF-8")));
        logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(loc.getAltitude()));
        logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(loc.getAccuracy()));
        logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(loc.getBearing()));
        logUrl = logUrl.replaceAll("(?i)%prov", String.valueOf(loc.getProvider()));
        logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(loc.getSpeed()));
        logUrl = logUrl.replaceAll("(?i)%time", String.valueOf(Utilities.GetIsoDateTime(new Date(loc.getTime()))));
        logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
        logUrl = logUrl.replaceAll("(?i)%aid", String.valueOf(androidId));
        logUrl = logUrl.replaceAll("(?i)%ser", String.valueOf(Utilities.GetBuildSerial()));

        tracer.debug("Sending to URL: " + logUrl);
        URL url = new URL(logUrl);

        if(url.getProtocol().equalsIgnoreCase("https")){
            HttpsURLConnection.setDefaultSSLSocketFactory(CustomUrlTrustEverything.GetSSLContextSocketFactory());
            conn = (HttpsURLConnection)url.openConnection();
            ((HttpsURLConnection)conn).setHostnameVerifier(new CustomUrlTrustEverything.VerifyEverythingHostnameVerifier());
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod("GET");

        if(conn.getResponseCode() != 200){
            tracer.error("Status code: " + String.valueOf(conn.getResponseCode()));
        } else {
            tracer.debug("Status code: " + String.valueOf(conn.getResponseCode()));
        }

        EventBus.getDefault().post(new UploadEvents.CustomUrl(true));
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.CustomUrl(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not send to custom URL", throwable);
        return true;
    }
}

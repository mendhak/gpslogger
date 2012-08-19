package com.mendhak.gpslogger.senders.opengts;

import android.content.Context;
import android.location.Location;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.OpenGTSClient;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class OpenGTSHelper implements IActionListener, IFileSender
{
    Context applicationContext;
    IActionListener callback;

    public OpenGTSHelper(Context applicationContext, IActionListener callback)
    {
        this.applicationContext = applicationContext;
        this.callback = callback;
    }

    @Override
    public void UploadFile(List<File> files)
    {
        // Use only gpx
        for(File f : files)
        {
            if(f.getName().endsWith(".gpx"))
            {
                Thread t = new Thread(new OpenGTSHandler(applicationContext, f, this));
                t.start();
            }
        }
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
    public boolean accept(File dir, String name)
    {
        return name.toLowerCase().contains(".gpx");
    }
}

class OpenGTSHandler implements Runnable
{

    List<Location> locations;
    Context applicationContext;
    File file;
    final IActionListener helper;

    public OpenGTSHandler(Context applicationContext, File file, IActionListener helper)
    {
        this.applicationContext = applicationContext;
        this.file = file;
        this.helper = helper;
    }

    public void run()
    {
        try {

            locations = getLocationsFromGPX(file);
            Utilities.LogInfo(locations.size() + " points where read from " + file.getName());

            if (locations.size() > 0) {

                String server = AppSettings.getOpenGTSServer();
                int port = Integer.parseInt(AppSettings.getOpenGTSServerPort());
                String path = AppSettings.getOpenGTSServerPath();
                String deviceId = AppSettings.getOpenGTSDeviceId();

                OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path, helper, applicationContext);
                openGTSClient.sendHTTP(deviceId, locations.toArray(new Location[0]));

            } else {
                helper.OnFailure();
            }

        } catch (Exception e) {
            Utilities.LogError("OpenGTSHandler.run", e);
            helper.OnFailure();
        }

    }

    private List<Location> getLocationsFromGPX(File f)
    {
        List<Location> locations = Collections.emptyList();
        try {
            locations = GpxReader.getPoints(f);
        } catch (Exception e){
            Utilities.LogError("OpenGTSHelper.getLocationsFromGPX", e);
        }
        return locations;
    }

}
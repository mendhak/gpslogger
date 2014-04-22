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

package com.mendhak.gpslogger.senders.opengts;

import android.content.Context;
import android.location.Location;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.OpenGTSClient;
import com.mendhak.gpslogger.senders.IFileSender;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class OpenGTSHelper implements IActionListener, IFileSender {
    Context applicationContext;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSHelper.class.getSimpleName());
    IActionListener callback;

    public OpenGTSHelper(Context applicationContext, IActionListener callback) {
        this.applicationContext = applicationContext;
        this.callback = callback;
    }

    @Override
    public void UploadFile(List<File> files) {
        // Use only gpx
        for (File f : files) {
            if (f.getName().endsWith(".gpx")) {
                Thread t = new Thread(new OpenGTSHandler(applicationContext, f, this));
                t.start();
            }
        }
    }

    public void OnComplete() {
        callback.OnComplete();
    }

    public void OnFailure() {
        callback.OnFailure();
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }
}

class OpenGTSHandler implements Runnable {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSHandler.class.getSimpleName());
    List<Location> locations;
    Context applicationContext;
    File file;
    final IActionListener helper;

    public OpenGTSHandler(Context applicationContext, File file, IActionListener helper) {
        this.applicationContext = applicationContext;
        this.file = file;
        this.helper = helper;
    }

    public void run() {
        try {

            locations = getLocationsFromGPX(file);
            tracer.info(locations.size() + " points where read from " + file.getName());

            if (locations.size() > 0) {

                String server = AppSettings.getOpenGTSServer();
                int port = Integer.parseInt(AppSettings.getOpenGTSServerPort());
                String path = AppSettings.getOpenGTSServerPath();
                String deviceId = AppSettings.getOpenGTSDeviceId();
                String accountName = AppSettings.getOpenGTSAccountName();

                OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path, helper, applicationContext);
                openGTSClient.sendHTTP(deviceId, accountName, locations.toArray(new Location[0]));

            } else {
                helper.OnFailure();
            }

        } catch (Exception e) {
            tracer.error("OpenGTSHandler.run", e);
            helper.OnFailure();
        }

    }

    private List<Location> getLocationsFromGPX(File f) {
        List<Location> locations = Collections.emptyList();
        try {
            locations = GpxReader.getPoints(f);
        } catch (Exception e) {
            tracer.error("OpenGTSHelper.getLocationsFromGPX", e);
        }
        return locations;
    }

}
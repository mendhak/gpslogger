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

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSJob;
import com.mendhak.gpslogger.senders.GpxReader;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class OpenGTSManager implements IFileSender {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSManager.class.getSimpleName());

    public OpenGTSManager() {
    }

    @Override
    public void UploadFile(List<File> files) {
        // Use only gpx
        for (File f : files) {
            if (f.getName().endsWith(".gpx")) {
                List<SerializableLocation> locations = getLocationsFromGPX(f);
                tracer.debug(locations.size() + " points were read from " + f.getName());

                if (locations.size() > 0) {

                    String server = AppSettings.getOpenGTSServer();
                    int port = Integer.parseInt(AppSettings.getOpenGTSServerPort());
                    String path = AppSettings.getOpenGTSServerPath();
                    String deviceId = AppSettings.getOpenGTSDeviceId();
                    String accountName = AppSettings.getOpenGTSAccountName();
                    String communication = AppSettings.getOpenGTSServerCommunicationMethod();

                    JobManager jobManager = AppSettings.GetJobManager();
                    jobManager.addJobInBackground(new OpenGTSJob(server, port, accountName, path, deviceId, communication, locations.toArray(new SerializableLocation[0])));
                }
            }
        }
    }

    @Override
    public boolean IsAvailable() {
        return  AppSettings.getOpenGTSServer().length() > 0
                && AppSettings.getOpenGTSServerPort().length() > 0
                && AppSettings.getOpenGTSServerCommunicationMethod().length() > 0
                && AppSettings.getOpenGTSDeviceId().length() > 0;
    }

    private List<SerializableLocation> getLocationsFromGPX(File f) {
        List<SerializableLocation> locations = Collections.emptyList();
        try {
            locations = GpxReader.getPoints(f);
        } catch (Exception e) {
            tracer.error("OpenGTSManager.getLocationsFromGPX", e);
        }
        return locations;
    }


    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }
}
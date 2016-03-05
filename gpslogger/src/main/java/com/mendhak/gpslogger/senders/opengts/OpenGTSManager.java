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
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSJob;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.GpxReader;
import com.path.android.jobqueue.JobManager;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class OpenGTSManager extends FileSender {

    private static final Logger LOG = Logs.of(OpenGTSManager.class);
    private PreferenceHelper preferenceHelper;

    public OpenGTSManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        // Use only gpx
        for (File f : files) {
            if (f.getName().endsWith(".gpx")) {
                List<SerializableLocation> locations = getLocationsFromGPX(f);
                LOG.debug(locations.size() + " points were read from " + f.getName());

                if (locations.size() > 0) {

                    String server = preferenceHelper.getOpenGTSServer();
                    int port = Integer.parseInt(preferenceHelper.getOpenGTSServerPort());
                    String path = preferenceHelper.getOpenGTSServerPath();
                    String deviceId = preferenceHelper.getOpenGTSDeviceId();
                    String accountName = preferenceHelper.getOpenGTSAccountName();
                    String communication = preferenceHelper.getOpenGTSServerCommunicationMethod();

                    JobManager jobManager = AppSettings.getJobManager();
                    jobManager.addJobInBackground(new OpenGTSJob(server, port, accountName, path, deviceId, communication, locations.toArray(new SerializableLocation[locations.size()])));
                }
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServer())
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerPort())
                && Strings.toInt(preferenceHelper.getOpenGTSServerPort(), 0) != 0
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerCommunicationMethod())
                && !Strings.isNullOrEmpty(preferenceHelper.getOpenGTSDeviceId());
    }

    @Override
    protected boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isOpenGtsAutoSendEnabled();
    }

    private List<SerializableLocation> getLocationsFromGPX(File f) {
        List<SerializableLocation> locations = Collections.emptyList();
        try {
            locations = GpxReader.getPoints(f);
        } catch (Exception e) {
            LOG.error("OpenGTSManager.getLocationsFromGPX", e);
        }
        return locations;
    }


    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".gpx");
    }
}
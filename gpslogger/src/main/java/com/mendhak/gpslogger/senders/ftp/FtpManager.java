/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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


package com.mendhak.gpslogger.senders.ftp;



import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class FtpManager extends FileSender {
    private static final Logger LOG = Logs.of(FtpManager.class);

    PreferenceHelper preferenceHelper;

    public FtpManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public void testFtp(String servername, String username, String password, String directory, int port, boolean useFtps, String protocol, boolean implicit) {

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        LOG.debug("Creating gpslogger_test.xml");
        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");

        try {
            if (!testFile.exists()) {
                testFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(testFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                initialOutput.write("<x>This is a test file</x>".getBytes());
                initialOutput.flush();
                initialOutput.close();

                Files.addToMediaDatabase(testFile, "text/xml");
            }

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed(ex.getMessage(), ex));
        }

        JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, FtpJob.getJobTag(testFile));
        jobManager.addJobInBackground(new FtpJob(servername, port, username, password, directory,
                useFtps, protocol, implicit, testFile, "gpslogger_test.txt"));
    }

    @Override
    public void uploadFile(List<File> files) {
        if (!validSettings(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(),
                preferenceHelper.getFtpPort(), preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit())) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed());
        }

        for (File f : files) {
            uploadFile(f);
        }
    }

    @Override
    public boolean isAvailable() {
        return validSettings(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpUsername(),
                preferenceHelper.getFtpPassword(), preferenceHelper.getFtpPort(), preferenceHelper.shouldFtpUseFtps(),
                preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isFtpAutoSendEnabled();
    }

    public void uploadFile(File f) {

        JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, FtpJob.getJobTag(f));
        jobManager.addJobInBackground(new FtpJob(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpPort(),
                preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(), preferenceHelper.getFtpDirectory(),
                preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit(),
                f, f.getName()));
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }


    public boolean validSettings(String servername, String username, String password, Integer port, boolean useFtps,
                                 String sslTls, boolean implicit) {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if (useFtps && (sslTls == null || sslTls.length() <= 0)) {
            retVal = false;
        }

        return retVal;
    }
}


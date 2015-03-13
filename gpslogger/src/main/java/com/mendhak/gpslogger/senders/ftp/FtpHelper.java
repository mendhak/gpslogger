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
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class FtpHelper implements IFileSender {
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(FtpHelper.class.getSimpleName());


    public FtpHelper() {
    }

    void TestFtp(String servername, String username, String password, String directory, int port, boolean useFtps, String protocol, boolean implicit) {

        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        tracer.debug("Creating gpslogger_test.xml");
        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");

        try {
            if (!testFile.exists()) {
                testFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(testFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                initialOutput.write("<x>This is a test file</x>".getBytes());
                initialOutput.flush();
                initialOutput.close();
            }

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.FtpEvent(false));
        }

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.addJobInBackground(new FtpJob(servername, port, username, password, directory,
                useFtps, protocol, implicit, testFile, "gpslogger_test.txt"));
    }

    @Override
    public void UploadFile(List<File> files) {
        if (!ValidSettings(AppSettings.getFtpServerName(), AppSettings.getFtpUsername(), AppSettings.getFtpPassword(),
                AppSettings.getFtpPort(), AppSettings.FtpUseFtps(), AppSettings.getFtpProtocol(), AppSettings.FtpImplicit())) {
            EventBus.getDefault().post(new UploadEvents.FtpEvent(false));
        }

        for (File f : files) {
            UploadFile(f);
        }
    }

    public void UploadFile(File f) {
        try {
            JobManager jobManager = AppSettings.GetJobManager();
            jobManager.addJobInBackground(new FtpJob(AppSettings.getFtpServerName(), AppSettings.getFtpPort(),
                    AppSettings.getFtpUsername(), AppSettings.getFtpPassword(), AppSettings.getFtpDirectory(),
                    AppSettings.FtpUseFtps(), AppSettings.getFtpProtocol(), AppSettings.FtpImplicit(),
                    f, f.getName()));

        } catch (Exception e) {
            tracer.error("Could not prepare file for upload.", e);
        }
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }


    public boolean ValidSettings(String servername, String username, String password, Integer port, boolean useFtps,
                                 String sslTls, boolean implicit) {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if (useFtps && (sslTls == null || sslTls.length() <= 0)) {
            retVal = false;
        }

        return retVal;
    }
}


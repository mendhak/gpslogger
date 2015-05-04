package com.mendhak.gpslogger.senders.owncloud;

import android.content.Context;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;

import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import de.greenrobot.event.EventBus;

public class OwnCloudHelper implements IFileSender
{
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudSettingsFragment.class.getSimpleName());


    public OwnCloudHelper() {
    }

    void TestOwnCloud(String servername, String username, String password, String directory, int port, boolean useHttps) {

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

                Utilities.AddFileToMediaDatabase(testFile, "text/xml");
            }

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp(false));
        }

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.addJobInBackground(new OwnCloudJob(servername, port, username, password, directory,
                useHttps, testFile, "gpslogger_test.txt"));
    }

    public boolean ValidSettings(String servername, String username, String password, Integer port, boolean useSSL,
                                 String directory) {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if (useSSL) {
            retVal = false;
        }

        return retVal;
    }

    @Override
    public void UploadFile(List<File> files) {


        JobManager jobManager = AppSettings.GetJobManager();
        for (File f : files) {
            //UploadFile(f.getName());
            jobManager.addJobInBackground(new OwnCloudJob(
                    AppSettings.getOwnCloudServerName(),
                    AppSettings.getOwnCloudPort(),
                    AppSettings.getOwnCloudUsername(),
                    AppSettings.getOwnCloudPassword(),
                    AppSettings.getOwnCloudDirectory(),
                    AppSettings.OwnCloudUseHttps(),
                    f, f.getName()));
        }
    }
/*
    public void UploadFile(String fileName) {


        jobManager.addJobInBackground(new (fileName));
    }
*/
    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    public boolean IsLinked() {
        return false;
    }
}
package com.mendhak.gpslogger.senders.owncloud;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;

import com.path.android.jobqueue.TagConstraint;
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

    void TestOwnCloud(String servername, String username, String password, String directory) {

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
            tracer.error("Error while testing ownCloud upload: "+ ex.getMessage());
        }

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, "OWNCLOUD");
        jobManager.addJobInBackground(new OwnCloudJob(servername, username, password, directory,
                testFile, "gpslogger_test.txt"));
        tracer.debug("Added background ownCloud upload job");
        jobManager.start();
    }

    public static boolean ValidSettings(
            String servername,
            String username,
            String password,
            String directory) {
        boolean retVal = servername != null && servername.length() > 0;

        return retVal;
    }

    @Override
    public void UploadFile(List<File> files)
    {
        for (File f : files) {
            UploadFile(f);
        }
    }

    public void UploadFile(File f)
    {
        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.addJobInBackground(new OwnCloudJob(
                AppSettings.getOwnCloudServerName(),
                AppSettings.getOwnCloudUsername(),
                AppSettings.getOwnCloudPassword(),
                AppSettings.getOwnCloudDirectory(),
                f, f.getName()));
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    public boolean IsLinked() {
        return false;
    }
}
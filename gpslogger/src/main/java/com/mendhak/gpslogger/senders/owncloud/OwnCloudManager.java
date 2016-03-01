package com.mendhak.gpslogger.senders.owncloud;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class OwnCloudManager extends FileSender
{
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudSettingsFragment.class.getSimpleName());
    private static PreferenceHelper preferenceHelper;

    public OwnCloudManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    void testOwnCloud(String servername, String username, String password, String directory) {

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
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
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, OwnCloudJob.getJobTag(testFile));
        jobManager.addJobInBackground(new OwnCloudJob(servername, username, password, directory,
                testFile, "gpslogger_test.txt"));
        tracer.debug("Added background ownCloud upload job");
    }

    public static boolean ValidSettings(
            String servername,
            String username,
            String password,
            String directory) {
        return !Utilities.IsNullOrEmpty(servername);

    }

    @Override
    public void uploadFile(List<File> files)
    {
        for (File f : files) {
            uploadFile(f);
        }
    }

    @Override
    public boolean isAvailable() {
        return ValidSettings(preferenceHelper.getOwnCloudServerName(),
                preferenceHelper.getOwnCloudUsername(),
                preferenceHelper.getOwnCloudPassword(),
                preferenceHelper.getOwnCloudDirectory());
    }

    @Override
    protected boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isOwnCloudAutoSendEnabled();
    }

    public void uploadFile(File f)
    {
        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, OwnCloudJob.getJobTag(f));
        jobManager.addJobInBackground(new OwnCloudJob(
                preferenceHelper.getOwnCloudServerName(),
                preferenceHelper.getOwnCloudUsername(),
                preferenceHelper.getOwnCloudPassword(),
                preferenceHelper.getOwnCloudDirectory(),
                f, f.getName()));
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }


}
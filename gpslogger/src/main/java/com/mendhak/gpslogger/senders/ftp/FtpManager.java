/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.mendhak.gpslogger.senders.ftp;



import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.CancelResult;
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

    public void testFtp(final String servername, final String username, final String password, final String directory, final int port, final boolean useFtps, final String protocol, final boolean implicit) {


        try {
            final File testFile = Files.createTestFile();

            final JobManager jobManager = AppSettings.getJobManager();

            jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
                @Override
                public void onCancelled(CancelResult cancelResult) {
                    jobManager.addJobInBackground(new FtpJob(servername, port, username, password, directory,
                            useFtps, protocol, implicit, testFile, testFile.getName()));
                }
            }, TagConstraint.ANY, FtpJob.getJobTag(testFile));

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed(ex.getMessage(), ex));
        }



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

    public void uploadFile(final File f) {

        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new FtpJob(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpPort(),
                        preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(), preferenceHelper.getFtpDirectory(),
                        preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit(),
                        f, f.getName()));
            }
        }, TagConstraint.ANY, FtpJob.getJobTag(f));

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


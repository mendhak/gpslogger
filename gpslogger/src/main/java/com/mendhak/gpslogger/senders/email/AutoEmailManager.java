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

package com.mendhak.gpslogger.senders.email;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoEmailManager extends FileSender {

    PreferenceHelper preferenceHelper;

    public AutoEmailManager(PreferenceHelper helper) {
        this.preferenceHelper = helper;
    }

    @Override
    public void uploadFile(List<File> files) {

        ArrayList<File> filesToSend = new ArrayList<>();

        //If a zip file exists, remove others
        for (File f : files) {
            filesToSend.add(f);
        }

        String subject = "GPS Log file generated at "+ Utilities.GetReadableDateTime(new Date());

        String body = "GPS Log file generated at "+ Utilities.GetReadableDateTime(new Date());

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, AutoEmailJob.getJobTag(filesToSend.toArray(new File[filesToSend.size()])));
        jobManager.addJobInBackground(new AutoEmailJob(preferenceHelper.getSmtpServer(),
                preferenceHelper.getSmtpPort(), preferenceHelper.getSmtpUsername(), preferenceHelper.getSmtpPassword(),
                preferenceHelper.isSmtpSsl(), preferenceHelper.getAutoEmailTargets(), preferenceHelper.getSmtpSenderAddress(),
                subject, body, filesToSend.toArray(new File[filesToSend.size()])));

    }

    @Override
    public boolean isAvailable() {
        return isValid( preferenceHelper.getSmtpServer(), preferenceHelper.getSmtpPort(), preferenceHelper.getSmtpUsername(), preferenceHelper.getSmtpPassword(), preferenceHelper.getAutoEmailTargets());
    }

    @Override
    protected boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isEmailAutoSendEnabled();
    }


    void sendTestEmail(String smtpServer, String smtpPort,
                       String smtpUsername, String smtpPassword, boolean smtpUseSsl,
                       String emailTarget, String fromAddress) {

        String subject = "Test Email from GPSLogger at " + Utilities.GetReadableDateTime(new Date());
        String body ="Test Email from GPSLogger at " + Utilities.GetReadableDateTime(new Date());

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.addJobInBackground(new AutoEmailJob(smtpServer,
                smtpPort, smtpUsername, smtpPassword, smtpUseSsl,
                emailTarget, fromAddress, subject, body, new File[]{}));

    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    protected boolean isValid( String server, String port, String username, String password, String target) {
                return !Utilities.IsNullOrEmpty(server) && !Utilities.IsNullOrEmpty(port) && !Utilities.IsNullOrEmpty(username) && !Utilities.IsNullOrEmpty(target);

    }
}


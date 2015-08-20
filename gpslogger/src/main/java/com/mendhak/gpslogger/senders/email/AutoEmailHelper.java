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
import android.content.Context;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoEmailHelper implements IFileSender {

    public AutoEmailHelper() {
    }

    @Override
    public void UploadFile(List<File> files) {

        ArrayList<File> filesToSend = new ArrayList<File>();

        //If a zip file exists, remove others
        for (File f : files) {
            filesToSend.add(f);
        }

        String subject = "GPS Log file generated at "+ Utilities.GetReadableDateTime(new Date());

        String body = "GPS Log file generated at "+ Utilities.GetReadableDateTime(new Date());

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, AutoEmailJob.JOB_TAG);
        jobManager.addJobInBackground(new AutoEmailJob(AppSettings.getSmtpServer(),
                AppSettings.getSmtpPort(), AppSettings.getSmtpUsername(), AppSettings.getSmtpPassword(),
                AppSettings.isSmtpSsl(), AppSettings.getAutoEmailTargets(), AppSettings.getSenderAddress(),
                subject, body, filesToSend.toArray(new File[filesToSend.size()])));

    }


    void SendTestEmail(String smtpServer, String smtpPort,
                       String smtpUsername, String smtpPassword, boolean smtpUseSsl,
                       String emailTarget, String fromAddress) {

        String subject = "Test Email from GPSLogger at " + Utilities.GetReadableDateTime(new Date());
        String body ="Test Email from GPSLogger at " + Utilities.GetReadableDateTime(new Date());

        JobManager jobManager = AppSettings.GetJobManager();
        jobManager.cancelJobsInBackground(null, TagConstraint.ANY, AutoEmailJob.JOB_TAG);
        jobManager.addJobInBackground(new AutoEmailJob(smtpServer,
                smtpPort, smtpUsername, smtpPassword, smtpUseSsl,
                emailTarget, fromAddress, subject, body, new File[]{}));

    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

}


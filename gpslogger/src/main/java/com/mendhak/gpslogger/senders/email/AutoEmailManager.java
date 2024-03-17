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

package com.mendhak.gpslogger.senders.email;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.senders.FileSender;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AutoEmailManager extends FileSender {

    PreferenceHelper preferenceHelper;

    public AutoEmailManager(PreferenceHelper helper) {
        this.preferenceHelper = helper;
    }

    @Override
    public void uploadFile(List<File> files) {

        String[] fileNames = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileNames[i] = files.get(i).getAbsolutePath();
        }

        final String subject = "GPS Log file generated at "+ Strings.getReadableDateTime(new Date());
        final String body = "GPS Log file generated at "+ Strings.getReadableDateTime(new Date());

        HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
            put("subject", subject);
            put("body", body);
            put("fileNames", fileNames);
        }};

        String tag = String.valueOf(Objects.hashCode(fileNames)) ;
        OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(AutoEmailWorker.class, dataMap);
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);

    }

    @Override
    public boolean isAvailable() {
        return isValid( preferenceHelper.getSmtpServer(), preferenceHelper.getSmtpPort(), preferenceHelper.getSmtpUsername(), preferenceHelper.getSmtpPassword(), preferenceHelper.getAutoEmailTargets());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isEmailAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.AUTOEMAIL;
    }


    public void sendTestEmail() {

        String subject = "Test Email from GPSLogger at " + Strings.getReadableDateTime(new Date());
        String body ="Test Email from GPSLogger at " + Strings.getReadableDateTime(new Date());


        HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
            put("subject", subject);
            put("body", body);
            put("fileNames", new String[]{});
        }};
        String tag = String.valueOf(Objects.hashCode(new String[]{})) ;
        OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(AutoEmailWorker.class, dataMap);
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    public boolean isValid(String server, String port, String username, String password, String target) {
                return !Strings.isNullOrEmpty(server) && !Strings.isNullOrEmpty(port) && !Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(target);

    }
}


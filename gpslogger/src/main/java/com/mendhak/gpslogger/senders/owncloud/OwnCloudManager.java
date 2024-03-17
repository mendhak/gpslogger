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

package com.mendhak.gpslogger.senders.owncloud;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.ui.fragments.settings.OwnCloudSettingsFragment;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class OwnCloudManager extends FileSender
{
    private static final Logger LOG = Logs.of(OwnCloudSettingsFragment.class);
    private PreferenceHelper preferenceHelper;

    public OwnCloudManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public void testOwnCloud() {

        try {
            final File testFile = Files.createTestFile();

            String tag = String.valueOf(Objects.hashCode(testFile));
            HashMap<String, Object> dataMap = new HashMap<String, Object>(){{
                put("filePath", testFile.getAbsolutePath());
            }};
            OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(OwnCloudWorker.class, dataMap);
            WorkManager.getInstance(AppSettings.getInstance())
                    .enqueueUniqueWork(tag, androidx.work.ExistingWorkPolicy.REPLACE, workRequest);

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed());
            LOG.error("Error while testing ownCloud upload: " + ex.getMessage());
        }

        LOG.debug("Added background ownCloud upload job");
    }

    public static boolean validSettings(
            String servername,
            String username,
            String password,
            String directory) {
        return !Strings.isNullOrEmpty(servername);

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
        return validSettings(preferenceHelper.getOwnCloudBaseUrl(),
                preferenceHelper.getOwnCloudUsername(),
                preferenceHelper.getOwnCloudPassword(),
                preferenceHelper.getOwnCloudDirectory());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isOwnCloudAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.OWNCLOUD;
    }

    public void uploadFile(final File f)
    {

        String tag = String.valueOf(Objects.hashCode(f));
        HashMap<String, Object> dataMap = new HashMap<String, Object>(){{
            put("filePath", f.getAbsolutePath());
        }};
        OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(OwnCloudWorker.class, dataMap);
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, androidx.work.ExistingWorkPolicy.REPLACE, workRequest);
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }


}
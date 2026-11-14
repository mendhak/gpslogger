/*
 * Copyright (C) 2026 Jan-NiklasB
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

package com.mendhak.gpslogger.senders.dawarich;

import androidx.work.ExistingWorkPolicy;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class DawarichManager extends FileSender {

    private static final Logger LOG = Logs.of(DawarichManager.class);

    static final String UPLOAD_WORK_NAME = "DawarichUpload";

    private final PreferenceHelper preferenceHelper;

    public DawarichManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public static String getBufferPath(String folder) {
        return folder + "/buffer.json";
    }

    // If an upload is already pending or running it sends the new locations too, so nothing new is scheduled
    public void uploadQueue() {
        if (!isAvailable()) {
            LOG.debug("Dawarich is not configured, not uploading");
            return;
        }

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put(DawarichWorker.BUFFER_PATH, getBufferPath(preferenceHelper.getGpsLoggerFolder()));
        Systems.startWorkManagerRequest(DawarichWorker.class, dataMap, UPLOAD_WORK_NAME, ExistingWorkPolicy.KEEP);
    }

    // Dawarich doesn't upload files, an auto send or manual upload sends the queued locations instead
    @Override
    public void uploadFile(List<File> files) {
        uploadQueue();
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getDawarichBaseUrl()) &&
                !Strings.isNullOrEmpty(preferenceHelper.getDawarichApikey());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isDawarichAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.DAWARICH;
    }

    @Override
    public boolean accept(File file, String s) {
        return false;
    }
}

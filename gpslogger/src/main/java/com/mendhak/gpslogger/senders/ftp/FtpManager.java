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


import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;

import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class FtpManager extends FileSender {
    private static final Logger LOG = Logs.of(FtpManager.class);

    PreferenceHelper preferenceHelper;

    public FtpManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public void testFtp() {


        try {
            final File testFile = Files.createTestFile();

            HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
                put("filePath", testFile.getAbsolutePath());
            }};

            String tag = String.valueOf(Objects.hashCode(testFile)) ;
            Systems.startWorkManagerRequest(FtpWorker.class, dataMap, tag);

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed(ex.getMessage(), ex));
        }
    }

    @Override
    public void uploadFile(List<File> files) {
        if (!validSettings(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(),
                preferenceHelper.getFtpPort(), preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit())) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed());
            return;
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

    @Override
    public String getName() {
        return SenderNames.FTP;
    }

    public void uploadFile(final File f) {

        HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
            put("filePath", f.getAbsolutePath());
        }};

        String tag = String.valueOf(Objects.hashCode(f)) ;
        Systems.startWorkManagerRequest(FtpWorker.class, dataMap, tag);
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


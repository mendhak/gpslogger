package com.mendhak.gpslogger.senders.sftp;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.senders.FileSender;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SFTPManager extends FileSender {

    private PreferenceHelper preferenceHelper;

    public SFTPManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            uploadFile(f);
        }
    }

    public void uploadFile(final File file){
        String tag = String.valueOf(Objects.hashCode(file));
        HashMap<String, Object> dataMap = new HashMap<String, Object>(){{
            put("filePath", file.getAbsolutePath());
        }};
        OneTimeWorkRequest workRequest = Systems.getBasicOneTimeWorkRequest(SFTPWorker.class, dataMap);
        WorkManager.getInstance(AppSettings.getInstance())
                .enqueueUniqueWork(tag, androidx.work.ExistingWorkPolicy.REPLACE, workRequest);
    }

    @Override
    public boolean isAvailable() {
        return validSettings(preferenceHelper.getSFTPRemoteServerPath(), preferenceHelper.getSFTPHost(),preferenceHelper.getSFTPPort(),preferenceHelper.getSFTPPrivateKeyFilePath(),
                preferenceHelper.getSFTPPrivateKeyPassphrase(),preferenceHelper.getSFTPUser(),preferenceHelper.getSFTPPassword(),preferenceHelper.getSFTPKnownHostKey());
    }

    private boolean validSettings(String sftpRemoteServerPath, String sftpHost, int sftpPort, String sftpPrivateKeyFilePath, String sftpPrivateKeyPassphrase, String sftpUser, String sftpPassword, String sftpKnownHostKey) {
        if (Strings.isNullOrEmpty(sftpRemoteServerPath)
                || Strings.isNullOrEmpty(sftpHost)
                || sftpPort <= 0
                || (Strings.isNullOrEmpty(sftpPrivateKeyFilePath) && Strings.isNullOrEmpty(sftpPassword) )){
            return false;
        }

        return true;
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isSFTPEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.SFTP;
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }
}

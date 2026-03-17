package com.mendhak.gpslogger.senders.http;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
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

public class HttpUploadManager extends FileSender {

    private static final Logger LOG = Logs.of(HttpUploadManager.class);
    private final PreferenceHelper preferenceHelper;

    public HttpUploadManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public void testHttpUpload() {
        try {
            final File testFile = Files.createTestFile();

            String tag = String.valueOf(Objects.hashCode(testFile.getAbsolutePath()));
            HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
                put("filePath", testFile.getAbsolutePath());
            }};
            Systems.startWorkManagerRequest(HttpUploadWorker.class, dataMap, tag);

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.HttpUpload().failed());
            LOG.error("Error while testing HTTP upload: " + ex.getMessage());
        }

        LOG.debug("Added background HTTP upload job");
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            String tag = String.valueOf(Objects.hashCode(f.getAbsolutePath()));
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("filePath", f.getAbsolutePath());

            Systems.startWorkManagerRequest(HttpUploadWorker.class, dataMap, tag);
        }
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getHttpUploadUrl()) &&
                !Strings.isNullOrEmpty(preferenceHelper.getHttpUploadMethod());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isHttpUploadAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.HTTPUPLOAD;
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }
}

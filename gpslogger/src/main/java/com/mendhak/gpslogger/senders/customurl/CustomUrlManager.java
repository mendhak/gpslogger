package com.mendhak.gpslogger.senders.customurl;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class CustomUrlManager extends FileSender {

    private final PreferenceHelper preferenceHelper;
    private static final Logger LOG = Logs.of(CustomUrlManager.class);

    public CustomUrlManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        //TODO: Read from CSV file.
        // Convert each line to a Serializable Location.
        // Send each location using JobManager Custom Url Job.
    }

    @Override
    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingUrl()) &&
                !Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingHTTPMethod());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isCustomURLAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.CUSTOMURL;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().contains(".csv");
    }
}

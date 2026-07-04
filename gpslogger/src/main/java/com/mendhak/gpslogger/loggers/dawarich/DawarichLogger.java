package com.mendhak.gpslogger.loggers.dawarich;

import android.location.Location;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.senders.dawarich.DawarichManager;

import java.io.IOException;

public class DawarichLogger implements FileLogger {

    private final SerializableFIFOBuffer<SerializableLocation> buffer;

    public DawarichLogger (String persistenceFilePath) {
        this.buffer = new SerializableFIFOBuffer<>(persistenceFilePath + "/buffer.json");
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        SerializableLocation sloc = new SerializableLocation(loc);

        if(!Systems.isNetworkAvailable((AppSettings.getInstance())) && preferenceHelper.shouldDawarichLoggingDiscardOfflineLocations()) return;
        this.buffer.push(sloc);
        if (!Systems.isNetworkAvailable(AppSettings.getInstance())) return;


        if (this.buffer.getSize() >= preferenceHelper.getDawarichBatchMin()) {
            DawarichManager manager = new DawarichManager(preferenceHelper);
            manager.send(buffer);
        }

    }

    @Override
    public String getName() {
        return "DAWARICH";
    }
}

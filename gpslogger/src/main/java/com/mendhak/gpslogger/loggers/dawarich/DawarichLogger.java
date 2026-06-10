package com.mendhak.gpslogger.loggers.dawarich;

import android.location.Location;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.senders.dawarich.DawarichManager;

import java.io.IOException;

public class DawarichLogger implements FileLogger {

    private final String name = "DAWARICH";
    private final String filepath;
    private final SerializableFIFOBuffer<SerializableLocation> buffer;

    public DawarichLogger (String persistenceFilePath) throws IOException {
        this.filepath = persistenceFilePath;
        this.buffer = new SerializableFIFOBuffer<SerializableLocation>(this.filepath);
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

        if (!Systems.isNetworkAvailable(AppSettings.getInstance()))
        {
            if(preferenceHelper.shouldDawarichLoggingDiscardOfflineLocations())
            {
                return;
            }

            this.buffer.push(sloc);
            return;
        }

        DawarichManager manager = new DawarichManager(preferenceHelper);

        if (buffer.getSize() > 1){
            this.buffer.push(sloc);
            manager.sendBulkData(buffer);
        }
        else {
            if (!manager.sendLocation(sloc)) {
                this.buffer.push(sloc);
            };
        }
    }

    @Override
    public String getName() {
        return name;
    }
}

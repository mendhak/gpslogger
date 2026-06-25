package com.mendhak.gpslogger.senders.dawarich;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.mendhak.gpslogger.common.SerializableFIFOBuffer;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.dropbox.DropboxWorker;
import org.slf4j.Logger;

public class DawarichWorker extends Worker {
    private static final Logger LOG = Logs.of(DawarichWorker.class);
    private SerializableFIFOBuffer buffer;
    public DawarichWorker(@NonNull Context context, @NonNull WorkerParameters workerParams, SerializableFIFOBuffer buf) {
        super(context, workerParams);
        buffer = buf;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        if (buffer.getSize() > 1) {
            // Batch upload
        }
        else {
            //Single location upload
        }
    }
}

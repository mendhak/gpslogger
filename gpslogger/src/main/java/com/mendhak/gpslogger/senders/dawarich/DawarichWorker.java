package com.mendhak.gpslogger.senders.dawarich;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.gson.reflect.TypeToken;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableFIFOBuffer;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.dropbox.DropboxWorker;
import de.greenrobot.event.EventBus;
import okhttp3.*;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

public class DawarichWorker extends Worker {
    private static final Logger LOG = Logs.of(DawarichWorker.class);
    private OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
    public DawarichWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Type dequeTyp = new TypeToken<ArrayDeque<SerializableLocation>>() {}.getType();
        ArrayDeque<SerializableLocation> buffer = Strings.deserializeFromJson(getInputData().getString("sendBuffer"), dequeTyp);
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        UploadEvents.Dawarich callbackEvent = new UploadEvents.Dawarich();
        assert buffer != null;
        callbackEvent.sendBuffer = buffer.clone();

        DawarichBatch batch = new DawarichBatch();
        while (!buffer.isEmpty()) {
            DawarichBatchLocation loc = DawarichBatchLocation.fromSerializableLocationExtended(Objects.requireNonNull(buffer.pop()), preferenceHelper);
            batch.appendLocation(loc);
        }
        try {
            String json = batch.toJSON().toString();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(
                    mediaType,
                    json
            );
            LOG.info("Sending bulk data to Dawarich");
            Request req = new Request.Builder()
                    .url(preferenceHelper.getDawarichBaseUrl() + "/api/v1/overland/batches?api_key=" + preferenceHelper.getDawarichApikey())
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = httpClient.newCall(req).execute();
            if (response.isSuccessful()) {
                LOG.info("Successfully posted bulk data to Dawarich");
                EventBus.getDefault().post(callbackEvent.succeeded());
                return Result.success();
            } else {
                ArrayList<DawarichBatchLocation> locations = batch.getLocations();
                for (DawarichBatchLocation l : locations) {
                    buffer.push(l.getSourceData());
                }
                LOG.warn("Location batch could not be send to the Dawarich server, locations have been added to the queue again, server response:{}", response.toString());
                EventBus.getDefault().post(callbackEvent.failed());
                return Result.failure();
            }
        } catch (Exception e) {
            LOG.error("Exception on location upload to Dawarich: {}", e.getMessage(), e);
            EventBus.getDefault().post(callbackEvent.failed());
            return Result.failure();
        }
    }
}

package com.mendhak.gpslogger.senders.dawarich;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableFIFOBuffer;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import de.greenrobot.event.EventBus;
import okhttp3.*;
import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Sends the queue in batches until it's empty. Locations are only removed once the server has accepted them.
 */
public class DawarichWorker extends Worker {
    private static final Logger LOG = Logs.of(DawarichWorker.class);

    static final String BUFFER_PATH = "bufferPath";

    enum Outcome { SENT, DROP, RETRY, STOP }

    public DawarichWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        String bufferPath = getInputData().getString(BUFFER_PATH);
        if (Strings.isNullOrEmpty(bufferPath)) {
            LOG.error("No Dawarich queue file given, nothing to upload");
            return Result.failure();
        }
        if (!new File(bufferPath).exists()) {
            // Don't create a queue file if Dawarich logging was never used
            return Result.success();
        }

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        SerializableFIFOBuffer<SerializableLocation> buffer = SerializableFIFOBuffer.getInstance(bufferPath);
        int batchSize = Math.max(1, preferenceHelper.getDawarichBatchMax());
        OkHttpClient httpClient = createHttpClient();

        while (!buffer.isEmpty()) {
            if (isStopped()) {
                return Result.retry();
            }

            List<SerializableLocation> locations = buffer.peek(batchSize);
            Outcome outcome;
            try {
                outcome = send(httpClient, preferenceHelper, locations);
            } catch (IOException | JSONException e) {
                LOG.warn("Could not send locations to Dawarich, will retry: {}", e.getMessage());
                EventBus.getDefault().post(new UploadEvents.Dawarich().failed(e.getMessage(), e));
                return Result.retry();
            }

            switch (outcome) {
                case SENT:
                    buffer.remove(locations.size());
                    break;
                case DROP:
                    LOG.error("Dawarich rejected {} locations as invalid, removing them from the queue", locations.size());
                    buffer.remove(locations.size());
                    EventBus.getDefault().post(new UploadEvents.Dawarich().failed());
                    break;
                case RETRY:
                    EventBus.getDefault().post(new UploadEvents.Dawarich().failed());
                    return Result.retry();
                case STOP:
                    EventBus.getDefault().post(new UploadEvents.Dawarich().failed());
                    return Result.failure();
            }
        }

        LOG.info("All queued locations sent to Dawarich");
        EventBus.getDefault().post(new UploadEvents.Dawarich().succeeded());
        return Result.success();
    }

    private Outcome send(OkHttpClient httpClient, PreferenceHelper preferenceHelper, List<SerializableLocation> locations)
            throws IOException, JSONException {
        DawarichBatch batch = new DawarichBatch();
        for (SerializableLocation location : locations) {
            try {
                batch.appendLocation(DawarichBatchLocation.fromSerializableLocationExtended(location, preferenceHelper));
            } catch (RuntimeException e) {
                LOG.error("Skipping a location that can't be converted for Dawarich", e);
            }
        }
        if (batch.getItemCount() == 0) {
            return Outcome.SENT;
        }

        LOG.info("Sending {} locations to Dawarich", batch.getItemCount());
        try (Response response = httpClient.newCall(buildRequest(preferenceHelper, batch.toJSON().toString())).execute()) {
            Outcome outcome = outcomeFor(response.code());
            if (outcome != Outcome.SENT) {
                LOG.warn("Dawarich server replied {} {}, {}", response.code(), response.message(), outcome);
            }
            return outcome;
        }
    }

    // Dawarich returns 422 when a timestamp is invalid (the whole batch is rejected)
    // and 401/402/403 for API key, account or plan problems
    static Outcome outcomeFor(int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return Outcome.SENT;
        }
        if (httpStatus == 400 || httpStatus == 422) {
            return Outcome.DROP;
        }
        if (httpStatus == 408 || httpStatus == 429 || httpStatus >= 500) {
            return Outcome.RETRY;
        }
        // Configuration problem (API key, wrong URL...), keep the locations until it's fixed
        return Outcome.STOP;
    }

    static OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    static Request buildRequest(PreferenceHelper preferenceHelper, String json) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        return new Request.Builder()
                .url(preferenceHelper.getDawarichBaseUrl() + "/api/v1/overland/batches")
                .addHeader("Authorization", "Bearer " + preferenceHelper.getDawarichApikey())
                .post(body)
                .build();
    }
}

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

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableFIFOBuffer;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import okhttp3.*;
import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public class DawarichManager extends FileSender {

    private final PreferenceHelper preferenceHelper;
    private static final Logger LOG = Logs.of(DawarichManager.class);
    private OkHttpClient httpClient = new OkHttpClient().newBuilder().build();;

    public DawarichManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    /**
     * Sends up to 10 points from the buffer as one request to the Dawarich server
     * @param buffer The buffer holding the points
     * @return True if the server returned a success message (201), false if not (in this case requeues the points
     * @throws JSONException
     * @throws IOException
     */
    public boolean sendBulkData(SerializableFIFOBuffer<SerializableLocation> buffer) throws JSONException, IOException {
        DawarichBatch batch = new DawarichBatch();
        int i = 0;
        while (i < 10 && !buffer.isEmpty()) {
            i++;
            DawarichBatchLocation loc = DawarichBatchLocation.fromSerializableLocationExtended(Objects.requireNonNull(buffer.pop()), preferenceHelper);
            batch.appendLocation(loc);
        }
        String json = batch.toJSON().toString();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(
                mediaType,
                json
        );
        Request req = new Request.Builder()
                .url(preferenceHelper.getDawarichBaseUrl() + "/api/v1/overland/batches?api_key=" + preferenceHelper.getDawarichApikey())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = httpClient.newCall(req).execute();
        if (response.isSuccessful()) return true;
        else {
            ArrayList<DawarichBatchLocation> locations = batch.getLocations();
            for (DawarichBatchLocation l : locations){
                buffer.push(l.getSourceData());
            }
            LOG.warn("Location batch could not be send to the Dawarich server, locations have been added to the queue again, server response:{}", response.toString());
            return false;
        }
    }

    public boolean sendLocation(SerializableLocation location) throws JSONException, IOException {
        DawarichBatch batch = new DawarichBatch();
        batch.appendLocation(DawarichBatchLocation.fromSerializableLocationExtended(location, preferenceHelper));
        String json = batch.toJSON().toString();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(
                mediaType,
                json
        );
        Request req = new Request.Builder()
                .url(preferenceHelper.getDawarichBaseUrl() + "/api/v1/overland/batches?api_key=" + preferenceHelper.getDawarichApikey())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = httpClient.newCall(req).execute();
        if (response.isSuccessful()) return true;
        else {
            LOG.warn("Location could not be send to the Dawarich server, location will be added to the queue again, server response:{}", response.toString());
            return false;
        }
    }

    @Override
    public void uploadFile(List<File> files) {

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

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

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DawarichManager extends FileSender {

    private final PreferenceHelper preferenceHelper;
    private static final Logger LOG = Logs.of(DawarichManager.class);
    private SerializableFIFOBuffer<SerializableLocation> buffer;

    public DawarichManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
        registerEventBus();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    public void send(SerializableFIFOBuffer<SerializableLocation> buffer) throws JSONException, IOException {
        this.buffer = buffer;
        ArrayDeque<SerializableLocation> sendBuffer = new ArrayDeque<>();
        for (int i = 0; i < preferenceHelper.getDawarichBatchMax() && !buffer.isEmpty(); i++) {
            sendBuffer.add(buffer.pop());
        }

        HashMap<String, Object> dataMap = new HashMap<String, Object>() {
            {
                put("sendBuffer", Strings.serializeTojson(sendBuffer));
            }
        };
        String tag = "DaWarIch_" + Systems.DateTimeUtil.currentDateTime();
        Systems.startWorkManagerRequest(DawarichWorker.class, dataMap, tag);
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Dawarich upload){
        if(!upload.success){
            for (int i=0; !upload.sendBuffer.isEmpty(); i++){
                try {
                    this.buffer.push(upload.sendBuffer.poll());
                } catch (Exception e) {
                    LOG.error(String.valueOf(e.getCause()));
                }
            }
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

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

package com.mendhak.gpslogger.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Generic FIFO Buffer with file persistence
 * @param <T> Serializable datatype to use
 */
public class SerializableFIFOBuffer<T extends Serializable> {

    private ArrayDeque<T> buffer = new ArrayDeque<>();
    private static final Logger LOG = Logs.of(SerializableFIFOBuffer.class);
    private final File persistenceFile;
    private static final Map<String, SerializableFIFOBuffer<? extends Serializable>> instance = new HashMap<>();


    private SerializableFIFOBuffer(String filePath) {
        persistenceFile = new File(filePath);
        try {
            if (persistenceFile.exists() && persistenceFile.isFile() && persistenceFile.length() > 0) {
                LOG.debug("Load FIFO buffer from file: {}", persistenceFile.getAbsolutePath());
                loadFromFile();
            } else {
                if (!(persistenceFile.exists() && persistenceFile.isFile())) {
                    LOG.debug("FIFO buffer file does not exist, creating new file in path: {}", persistenceFile.getAbsolutePath());
                    if (!persistenceFile.createNewFile()) {
                        // Only warn, if no exception is thrown the file exists but was falsely not detected by the if-clause
                        LOG.warn("Could not create file for FIFO buffer in path (already exists): {}", persistenceFile.getAbsolutePath());
                    }
                } else {
                    LOG.debug("Persistence file for FIFO-buffer is empty");
                }
            }
        } catch (IOException e) {
            LOG.error("Error on creation of FIFO buffer for Dawarich logging, logging will not be functional", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> SerializableFIFOBuffer<T> getInstance(String filePath) {
        if (instance.containsKey(filePath)) {
            return (SerializableFIFOBuffer<T>) instance.get(filePath);
        } else {
            SerializableFIFOBuffer<T> newInst = new SerializableFIFOBuffer<>(filePath);
            instance.put(filePath, newInst);
            return newInst;
        }
    }

    public synchronized void push(T data) {
        buffer.offer(data);
        persistBuffer();
    }

    public synchronized T pop() {
        T data = buffer.poll();
        if (data != null) {
            persistBuffer();
        }
        return data;
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public int getSize() {
        return buffer.size();
    }

    private void persistBuffer() {
        try (FileWriter fw = new FileWriter(persistenceFile)) {

            Gson gson = new Gson();
            String jsonBuffer = gson.toJson(buffer);
            fw.write(jsonBuffer);

        } catch (IOException ex) {
            LOG.error("IOException while persisting Fifo-Buffer object: {}", ex.getMessage());
        }
    }

    private void loadFromFile() {
        try (FileReader reader = new FileReader(persistenceFile)) {

            Gson gson = new Gson();
            Type dequeTyp = new TypeToken<ArrayDeque<SerializableLocation>>() {}.getType();
            buffer = gson.fromJson(reader, dequeTyp);

        } catch (IOException ex) {
            LOG.error("IOException while loading Fifo-Buffer object: {}", ex.getMessage());
        }
    }
}

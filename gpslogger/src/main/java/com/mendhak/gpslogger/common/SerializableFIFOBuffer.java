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
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * A FIFO queue that is persisted to a JSON file after every change, so it survives app restarts.
 * Use {@link #peek(int)} then {@link #remove(int)} to only drop items once they have been processed.
 */
public class SerializableFIFOBuffer<T extends Serializable> {

    private ArrayDeque<T> buffer = new ArrayDeque<>();
    private static final Logger LOG = Logs.of(SerializableFIFOBuffer.class);
    private final File persistenceFile;
    private static final Map<String, SerializableFIFOBuffer<? extends Serializable>> instance = new HashMap<>();


    SerializableFIFOBuffer(String filePath) {
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
    public static synchronized <T extends Serializable> SerializableFIFOBuffer<T> getInstance(String filePath) {
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

    public synchronized List<T> peek(int count) {
        List<T> items = new ArrayList<>();
        Iterator<T> it = buffer.iterator();
        while (it.hasNext() && items.size() < count) {
            items.add(it.next());
        }
        return items;
    }

    public synchronized void remove(int count) {
        boolean changed = false;
        for (int i = 0; i < count && !buffer.isEmpty(); i++) {
            buffer.poll();
            changed = true;
        }
        if (changed) {
            persistBuffer();
        }
    }

    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }

    public synchronized int getSize() {
        return buffer.size();
    }

    private void persistBuffer() {
        // Write to a temp file then rename, so a crash mid-write can't leave a truncated queue file
        File tempFile = new File(persistenceFile.getPath() + ".tmp");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(new Gson().toJson(buffer));
        } catch (IOException ex) {
            LOG.error("IOException while persisting Fifo-Buffer object: {}", ex.getMessage());
            return;
        }

        if (!tempFile.renameTo(persistenceFile)) {
            LOG.error("Could not replace FIFO buffer file {}", persistenceFile.getAbsolutePath());
        }
    }

    private void loadFromFile() {
        try (FileReader reader = new FileReader(persistenceFile)) {
            Type dequeTyp = new TypeToken<ArrayDeque<SerializableLocation>>() {}.getType();
            ArrayDeque<T> loaded = new Gson().fromJson(reader, dequeTyp);
            if (loaded != null) {
                buffer = loaded;
            }
        } catch (IOException ex) {
            LOG.error("IOException while loading Fifo-Buffer object: {}", ex.getMessage());
        } catch (JsonParseException ex) {
            // Keep the unreadable file for troubleshooting
            File corruptFile = new File(persistenceFile.getPath() + ".corrupt-" + System.currentTimeMillis());
            LOG.error("FIFO buffer file {} is not valid JSON, moving it to {} and starting empty",
                    persistenceFile.getAbsolutePath(), corruptFile.getName(), ex);
            if (!persistenceFile.renameTo(corruptFile)) {
                LOG.error("Could not move corrupt FIFO buffer file");
            }
        }
    }
}

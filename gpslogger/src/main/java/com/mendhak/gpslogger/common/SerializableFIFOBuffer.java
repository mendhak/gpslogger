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

import java.io.*;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Generic FIFO Buffer with file persistence
 * @param <T> Serializable datatype to use
 */
public class SerializableFIFOBuffer<T extends Serializable> {

    private Queue<T> buffer = new ArrayDeque<>();
    private final File persistenceFile;


    public SerializableFIFOBuffer(String filePath) throws IOException{
        persistenceFile = new File(filePath);
        if (persistenceFile.exists()) {
            loadFromFile();
        }
        else {
            if (!persistenceFile.createNewFile()) {
                throw new IOException("Persistence file for buffer could not be created");
            }
        }
    }

    public synchronized void push(T data) throws IOException {
        buffer.offer(data);
        persistBuffer();
    }

    public synchronized T pop() throws IOException {
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

    private void persistBuffer() throws IOException {
        try {
            FileOutputStream file = new FileOutputStream(persistenceFile);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(buffer);
            out.close();
            file.close();
        } catch (IOException ex) {
            System.err.println("IOException while persisting Fifo-Buffer object");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile() throws IOException {
        try {
            FileInputStream file = new FileInputStream(persistenceFile);
            ObjectInputStream in = new ObjectInputStream(file);
            buffer = (Queue<T>) in.readObject();
            in.close();
            file.close();
        } catch (IOException ex) {
            System.err.println("IOException while persisting Fifo-Buffer object");
        } catch (ClassNotFoundException ex) {
            System.err.println("Thw requested class to cast to wasn't found");
        }
    }
}

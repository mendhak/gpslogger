/*
 * Copyright (C) 2017 mendhak
 *
 * This file is part of gpslogger.
 *
 * gpslogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * gpslogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gpslogger.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.common.slf4j;


import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

public class LoggingOutputStream extends OutputStream {

    private static final int DEFAULT_BUFFER_LENGTH = 2048;
    private boolean hasBeenClosed = false;
    private byte[] buf;
    private int count;
    private int curBufLength;
    private Logger log;

    public LoggingOutputStream(final Logger log)
            throws IllegalArgumentException {
        if (log == null) {
            throw new IllegalArgumentException(
                    "Logger  must be not null");
        }
        this.log = log;
        curBufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[curBufLength];
        count = 0;
    }

    public void write(final int b) throws IOException {
        if (hasBeenClosed) {
            throw new IOException("The stream has been closed.");
        }
        // don't log nulls
        if (b == 0) {
            return;
        }
        // would this be writing past the buffer?
        if (count == curBufLength) {
            // grow the buffer
            final int newBufLength = curBufLength +
                    DEFAULT_BUFFER_LENGTH;
            final byte[] newBuf = new byte[newBufLength];
            System.arraycopy(buf, 0, newBuf, 0, curBufLength);
            buf = newBuf;
            curBufLength = newBufLength;
        }

        buf[count] = (byte) b;
        count++;
    }

    public void flush() {
        if (count == 0) {
            return;
        }
        final byte[] bytes = new byte[count];
        System.arraycopy(buf, 0, bytes, 0, count);
        String str = new String(bytes);
        log.debug(str);
        count = 0;
    }

    public void close() {
        flush();
        hasBeenClosed = true;
    }
}
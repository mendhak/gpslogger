/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * Initial code from Gaggle and contributed by:
 * Copyright (C) 2012 Max Kellermann <max@duempel.org>
 * distributed under GPLv2 (or higher)
 *
 * Ported to gpslogger by Marc Poulhiès <dkm@kataplop.net>
 */

package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.os.SystemClock;

import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.utils.LocationBuffer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

class CRC16CCITT {
	private static final int[] table = new int[]{
		0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
		0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
		0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
		0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
		0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
		0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
		0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
		0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
		0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
		0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
		0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
		0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
		0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
		0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
		0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
		0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
		0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
		0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
		0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
		0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
		0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
		0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
		0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
		0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
		0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
		0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
		0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
		0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
		0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
		0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
		0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
		0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
	};

	static short update(byte octet, short crc) {
		return (short)((crc << 8) ^ table[((crc >> 8) ^ octet) & 0xff]);
	}

	static short update(byte[] data, short crc) {
        for (int i = 0; i < data.length; ++i)
            crc = update(data[i], crc);
		return crc;
	}
}

/**
 * Skylines live tracking support
 * http://skylines.xcsoar.org
 * @see "http://git.xcsoar.org/cgit/master/xcsoar.git/tree/src/Tracking/SkyLines/Protocol.hpp"
 */
public class SkyLinesLogger extends AbstractLiveLogger
{
    private static final int MAGIC = 0x5df4b67b;
	private static final short TYPE_FIX = 0x3;

	private static final int FLAG_LOCATION = 0x1;
	private static final int FLAG_TRACK = 0x2;
	private static final int FLAG_GROUND_SPEED = 0x4;
	private static final int FLAG_AIRSPEED = 0x8;
	private static final int FLAG_ALTITUDE = 0x10;
	private static final int FLAG_VARIO = 0x20;
	private static final int FLAG_ENL = 0x40;

	private long key;
	private int intervalMS;
    private String servername;
    private int serverport;

	private DatagramSocket socket;
	private SocketAddress serverAddress;
	private DatagramPacket datagram;

	private long nextUpdateTime = SystemClock.elapsedRealtime();

	private Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    public static final String name = "SKYLINES";

    private static SkyLinesLogger instance = null;

    public static SkyLinesLogger getSkyLinesLogger(long key, int intervals, int minDistance, String host, int port)
            throws SocketException, UnknownHostException {

        if (instance == null || instance.key != key || instance.intervalMS != intervals ||
                !instance.servername.equals(host) ||
                instance.serverport != port)
            instance = new SkyLinesLogger(key, intervals, minDistance, host, port);

        return instance;
    }

    private SkyLinesLogger(long key, int intervals, int minDistance, String host, int port)
            throws SocketException, UnknownHostException
    {
        super(intervals,minDistance);
        Utilities.LogDebug("Skylines constructor");
        this.key = key;
        this.intervalMS = intervals * 1000;
        this.serverport = port;
        this.servername = host;

		socket = new DatagramSocket();

        InetAddress serverIP = InetAddress.getByName(host);
        serverAddress = new InetSocketAddress(serverIP, port);
    }

    private void writeHeader(DataOutputStream dos, short type)
            throws IOException {
        dos.writeInt(MAGIC);
        dos.writeShort(0); // CRC
		dos.writeShort(type);
		dos.writeLong(key);
	}

    private void calculateCRC(byte[] data) {
        short crc = CRC16CCITT.update(data, (short) 0);
        data[4] = (byte) (crc >> 8);
        data[5] = (byte) crc;
    }

    private void writeAngle(DataOutputStream dos, double value)
            throws IOException {
        dos.writeInt((int) (value * 1000000));
    }

    private void writeGeoPoint(DataOutputStream dos,
                               double latitude, double longitude)
            throws IOException {
        writeAngle(dos, latitude);
        writeAngle(dos, longitude);
    }

    private void writeFix(DataOutputStream dos,
                          long time, double latitude, double longitude,
                          int altitude, int track, int groundSpeed)
            throws IOException {
        writeHeader(dos, TYPE_FIX);
        dos.writeInt(FLAG_LOCATION | FLAG_TRACK | FLAG_GROUND_SPEED |
                FLAG_ALTITUDE);
        dos.writeInt((int)time);
        writeGeoPoint(dos, latitude, longitude);
        dos.writeInt(0); // reserved
        dos.writeShort(track);
        dos.writeShort(groundSpeed);
        dos.writeShort(0); // airspeed (unavailable)
        dos.writeShort(altitude);
        dos.writeShort(0); // vario (unavailable)
        dos.writeShort(0); // engine noise level (unavailable)
    }

    public boolean liveUpload(LocationBuffer.BufferedLocation b) throws IOException{
        sendFix(b.timems,
                b.lat, b.lon,
                b.altitude,
                b.bearing,
                b.speed);
        return true;
    }

    private void sendFix(long time, double latitude, double longitude,
                         int altitude, int track, int groundSpeed)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(48);
        DataOutputStream dos = new DataOutputStream(baos);
        writeFix(dos, time, latitude, longitude, altitude,
                track, groundSpeed);

        byte[] data = baos.toByteArray();
        assert (data.length == 48);

        calculateCRC(data);

        if (datagram == null)
            datagram = new DatagramPacket(data, data.length,
                    serverAddress);
        else
            /* reuse old object to reduce GC pressure */
            datagram.setData(data);

        socket.send(datagram);
    }

    @Override
    public void closeAfterFlush() {
        instance = null;
    }



    @Override
    public void Annotate(String description, Location loc) throws Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public String getName()
    {
        return name;
    }
}

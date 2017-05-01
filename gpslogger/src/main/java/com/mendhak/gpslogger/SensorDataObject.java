package com.mendhak.gpslogger;

import java.io.Serializable;

public class SensorDataObject implements Serializable{

    public static class Compass extends BaseInfo {
        public float deg;

        public Compass(float deg, int time_offset) {
            this.deg = deg;
            this.time_offset = time_offset;
        }
    }

    public static class Accelerometer extends BaseInfo {
        public float x;
        public float y;
        public float z;

        public Accelerometer(float x, float y, float z, int time_offset) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time_offset = time_offset;
        }
    }

    public static class Orientation extends BaseInfo {
        public float azimuth;
        public float pitch;
        public float roll;

        public Orientation(float azimuth, float pitch, float roll, int time_offset) {
            this.azimuth = azimuth;
            this.pitch = pitch;
            this.roll = roll;
            this.time_offset = time_offset;
        }
    }

    public static class BaseInfo implements Serializable{
        public int time_offset;
    }
}

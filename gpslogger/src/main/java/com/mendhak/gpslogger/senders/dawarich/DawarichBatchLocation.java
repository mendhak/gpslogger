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

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import net.openid.appauth.internal.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for generating a single batch location for being combined with others to send as batch
 * as specified in <a href="https://my.dawarich.app/api-docs/index.html">Dawarich OAS API</a>
 */
public class DawarichBatchLocation {

    //region class properties
    /**
     * Type of the location described by the object
     */
    private String locationType = "Feature";


    //region geometry
    /**
     * Type of the geometry
     */
    private String geometryType = "Point";
    /**
     * Coordinates of the location in lon,lat order
     */
    private double[] coordinates = new double[2];
    //endregion


    //region properties
    /**
     * Timestamp in ISO 8601 format<br>
     * Example value: {@code 2021-06-01T12:00:00Z}
     */
    private String timestamp;
    /**
     * Altitude in meters
     */
    private Double altitude;
    /**
     * Speed in <b>meters per second</b>
     */
    private Double speed;
    /**
     * Horizontal accuracy in meters
     */
    private Double horizontal_accuracy;
    /**
     * Vertical accuracy in meters
     */
    private Double vertical_accuracy;
    /**
     * Motion type, for example: automotive_navigation, fitness, other_navigation or other<br>
     * Example: {@code ["walking","running","driving","cycling","stationary"]}
     */
    private String[] motion;
    /**
     * Activity type, e.g.: automotive_navigation, fitness, other_navigation or other
     */
    private String activity = "unknown";
    /**
     * Desired accuracy in meters
     */
    private Double desired_accuracy;
    /**
     * The distance in meters to defer location updates
     */
    private Double deferred;
    /**
     * A significant change mode, disabled, enabled or exclusive
     */
    private String significant_change = "disabled";
    /**
     * The number of locations in the payload
     */
    private Integer locations_in_payload;
    /**
     * The device id<br>
     * Example: {@code iOS device #166}
     */
    private String device_id;
    /**
     * The device's Unique ID as set by Apple
     */
    private String unique_id;
    /**
     * The WiFi network name
     */
    private String wifi = "unknown";
    /**
     * The battery state, unknown, unplugged, charging or full
     */
    private String battery_state = "unknown";
    /**
     * The battery level percentage, from 0.00 to 1.00
     */
    private Double battery_level;
    //endregion

    //endregion

    private static SerializableLocation sourceData;

    //region getters
    public String getLocationType() {
        return locationType;
    }

    public String getGeometryType() {
        return geometryType;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public double getHorizontal_accuracy() {
        return horizontal_accuracy;
    }

    public double getVertical_accuracy() {
        return vertical_accuracy;
    }

    public String[] getMotion() {
        return motion;
    }

    public String getActivity() {
        return activity;
    }

    public double getDesired_accuracy() {
        return desired_accuracy;
    }

    public double getDeferred() {
        return deferred;
    }

    public String getSignificant_change() {
        return significant_change;
    }

    public int getLocations_in_payload() {
        return locations_in_payload;
    }

    public String getDevice_id() {
        return device_id;
    }

    public String getUnique_id() {
        return unique_id;
    }

    public String getWifi() {
        return wifi;
    }

    public String getBattery_state() {
        return battery_state;
    }

    public double getBattery_level() {
        return battery_level;
    }

    public SerializableLocation getSourceData() {return sourceData;};
    //endregion

    private static final org.slf4j.Logger LOG = Logs.of(DawarichBatchLocation.class);

    private DawarichBatchLocation (Builder builder) {
        this.locationType = builder.locationType;
        this.geometryType = builder.geometryType;
        this.coordinates = builder.coordinates;
        this.timestamp = builder.timestamp;
        this.altitude = builder.altitude;
        this.speed = builder.speed;
        this.horizontal_accuracy = builder.horizontal_accuracy;
        this.vertical_accuracy = builder.vertical_accuracy;
        this.motion = builder.motion;
        this.activity = builder.activity;
        this.desired_accuracy = builder.desired_accuracy;
        this.deferred = builder.deferred;
        this.significant_change = builder.significant_change;
        this.locations_in_payload = builder.locations_in_payload;
        this.device_id = builder.device_id;
        this.unique_id = builder.unique_id;
        this.wifi = builder.wifi;
        this.battery_state = builder.battery_state;
        this.battery_level = builder.battery_level;
    }

    public static DawarichBatchLocation fromSerializableLocation (SerializableLocation location) {
        sourceData = location;
        double[] coords = {location.getLongitude(), location.getLatitude()};
        Builder b = new Builder(coords, Strings.getIsoDateTimeWithOffset(new Date(location.getTime())))
                .withAltitude(location.getAltitude())
                .withSpeed(location.getSpeed())
                // Use accuracy in meters as hor. accuracy
                .withHorizontalAccuracy(location.getAccuracy())
                .withLocationsInPayload(1)
                .withBatteryState(location.getBatteryCharging() ? "charging" : "unplugged")
                .withBatteryLevel((double) location.getBatteryLevel() / 100);
        if (!Strings.isNullOrEmpty(location.getHDOP()) &&  !Strings.isNullOrEmpty(location.getVDOP())) {
            // Calc vert. accuracy from hor. accuracy, vdop & hdop via acc*(vdop/hdop)
            b.withVerticalAccuracy(location.getAccuracy() * (Double.parseDouble(location.getVDOP()) / Double.parseDouble(location.getHDOP())));
        }
        return b.build();
    }

    public static DawarichBatchLocation fromSerializableLocationExtended (
            SerializableLocation location,
            PreferenceHelper helper){
        double[] coords = {location.getLongitude(), location.getLatitude()};
        Builder b = new Builder(coords, Strings.getIsoDateTimeWithOffset(new Date(location.getTime())))
                .withAltitude(location.getAltitude())
                .withSpeed(location.getSpeed())
                // Use accuracy in meters as hor. accuracy
                .withHorizontalAccuracy(location.getAccuracy())
                .withLocationsInPayload(1)
                .withBatteryState(location.getBatteryCharging() ? "charging" : "unplugged")
                .withBatteryLevel((double) location.getBatteryLevel() / 100)
                .withDeferred(helper.getMinimumDistanceInterval())
                .withDesiredAccuracy(helper.getMinimumAccuracy())
                .withDeviceId(helper.getDawarichDeviceId())
                .withSignificantChange(helper.shouldLogOnlyIfSignificantMotion() ? "enabled" : "disabled");
        if (!Strings.isNullOrEmpty(location.getHDOP()) &&  !Strings.isNullOrEmpty(location.getVDOP())) {
            // Calc vert. accuracy from hor. accuracy, vdop & hdop via acc*(vdop/hdop)
            b.withVerticalAccuracy(location.getAccuracy() * (Double.parseDouble(location.getVDOP()) / Double.parseDouble(location.getHDOP())));
        }
        return b.build();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject root = new JSONObject();
        if (locationType != null) root.put("type", locationType);

        JSONObject geometry = new JSONObject();
        geometry.put("type", geometryType);
        JSONArray coords = new JSONArray();
        for (double v : coordinates) {
            coords.put(v);
        }
        geometry.put("coordinates", coords);

        JSONObject properties = new JSONObject();
        properties.put("timestamp", timestamp);
        if (altitude != null) properties.put("altitude", altitude);
        if (speed != null) properties.put("speed", speed);
        if (horizontal_accuracy != null) properties.put("horizontal_accuracy", horizontal_accuracy);
        if (vertical_accuracy != null) properties.put("vertical_accuracy", vertical_accuracy);
        if (motion != null) {
            JSONArray mot = new JSONArray();

            for (String s : motion) {
                mot.put(s);
            }
            properties.put("motion", mot);
        }
        if (activity != null) properties.put("activity", activity);
        if (desired_accuracy != null) properties.put("desired_accuracy", desired_accuracy);
        if (deferred != null) properties.put("deffered", deferred);
        if (significant_change != null) properties.put("significant_change", significant_change);
        if (locations_in_payload != null) properties.put("locations_in_payload", locations_in_payload);
        if (device_id != null) properties.put("device_id", device_id);
        if (unique_id != null) properties.put("unique_id", unique_id);
        if (wifi != null) properties.put("wifi", wifi);
        if (battery_state != null) properties.put("battery_state", battery_state);
        if (battery_level != null) properties.put("battery_level", battery_level);

        root.put("geometry", geometry);
        root.put("properties", properties);

        return root;
    }

    public static class Builder {
        /**
         * Type of the location described by the object
         */
        String locationType = "Feature";


        //region geometry
        /**
         * Type of the geometry
         */
        String geometryType = "Point";
        /**
         * Coordinates of the location in lon,lat order
         */
        final double[] coordinates;
        //endregion


        //region properties
        /**
         * Timestamp in ISO 8601 format<br>
         * Example value: {@code 2021-06-01T12:00:00Z}
         */
        final String timestamp;
        /**
         * Altitude in meters
         */
        double altitude;
        /**
         * Speed in <b>meters per second</b>
         */
        double speed;
        /**
         * Horizontal accuracy in meters
         */
        double horizontal_accuracy;
        /**
         * Vertical accuracy in meters
         */
        double vertical_accuracy;
        /**
         * Motion type, for example: automotive_navigation, fitness, other_navigation or other<br>
         * Example: {@code ["walking","running","driving","cycling","stationary"]}
         */
        String[] motion;
        /**
         * Activity type, e.g.: automotive_navigation, fitness, other_navigation or other
         */
        String activity = "unknown";
        /**
         * Desired accuracy in meters
         */
        double desired_accuracy;
        /**
         * The distance in meters to defer location updates
         */
        double deferred;
        /**
         * A significant change mode, disabled, enabled or exclusive
         */
        String significant_change = "disabled";
        /**
         * The number of locations in the payload
         */
        int locations_in_payload;
        /**
         * The device id<br>
         * Example: {@code iOS device #166}
         */
        String device_id;
        /**
         * The device's Unique ID as set by Apple
         */
        String unique_id;
        /**
         * The WiFi network name
         */
        String wifi = "unknown";
        /**
         * The battery state, unknown, unplugged, charging or full
         */
        String battery_state = "unknown";
        /**
         * The battery level percentage, from 0.00 to 1.00
         */
        double battery_level;
        //endregion

        private final String iso8601_regex = "^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?(?:Z|[+-][01]\\d:[0-5]\\d|[+-][01]\\d[0-5]\\d|[+-][01]\\d)$";
        private final Pattern iso8601_pattern = Pattern.compile(iso8601_regex);



        public Builder(double[] coordinates, String timestamp) {

            if (coordinates.length == 2) {
                this.coordinates = coordinates;
            } else {
                throw new IllegalArgumentException("Length of coordinates array is not 2");
            }

            Matcher match = iso8601_pattern.matcher(timestamp);
            if (match.matches()) {
                this.timestamp = timestamp;
            } else {
                LOG.error("Invalid timestamp format: {}", timestamp);
                throw new IllegalArgumentException("Timestamp does not match ISO 8601 requirements");
            }
        }

        public DawarichBatchLocation build() {
            return new DawarichBatchLocation(this);
        }

        public Builder withLocationType(String locationType) {
            this.locationType = locationType;
            return this;
        }

        public Builder withGeometryType(String geometryType) {
            this.geometryType = geometryType;
            return this;
        }

        public Builder withAltitude(double altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder withSpeed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder withHorizontalAccuracy(double horizontalAccuracy) {
            this.horizontal_accuracy = horizontalAccuracy;
            return this;
        }

        public Builder withVerticalAccuracy(double verticalAccuracy) {
            this.vertical_accuracy = verticalAccuracy;
            return this;
        }

        public Builder withMotion(String[] motion) {
            this.motion = motion;
            return this;
        }

        public Builder withActivity(String activity) {
            this.activity = activity;
            return this;
        }

        public Builder withDesiredAccuracy(double desiredAccuracy) {
            this.desired_accuracy = desiredAccuracy;
            return this;
        }

        public Builder withDeferred(double deferred) {
            this.deferred = deferred;
            return this;
        }

        public Builder withSignificantChange(String significantChange) {
            this.significant_change = significantChange;
            return this;
        }

        public Builder withLocationsInPayload(int locationsInPayload) {
            this.locations_in_payload = locationsInPayload;
            return this;
        }

        public Builder withDeviceId(String deviceId) {
            this.device_id = deviceId;
            return this;
        }

        public Builder withUniqueId(String uniqueId) {
            this.unique_id = uniqueId;
            return this;
        }

        public Builder withWifi(String wifi) {
            this.wifi = wifi;
            return this;
        }

        public Builder withBatteryState(String batteryState) {
            this.battery_state = batteryState;
            return this;
        }

        public Builder withBatteryLevel(double batteryLevel) {
            this.battery_level = batteryLevel;
            return this;
        }
    }
}

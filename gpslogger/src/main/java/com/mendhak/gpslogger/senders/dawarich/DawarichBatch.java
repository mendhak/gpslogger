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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class DawarichBatch {
    private ArrayList<DawarichBatchLocation> locations;
    public DawarichBatch (DawarichBatchLocation[] locations) {
        this.locations = new ArrayList<>(Arrays.asList(locations));
    }
    public DawarichBatch () {
        this.locations = new ArrayList<DawarichBatchLocation>();
    }

    public void appendLocation (DawarichBatchLocation location) {
        this.locations.add(location);
    }

    public int getItemCount() {
        return locations.size();
    }

    public ArrayList<DawarichBatchLocation> getLocations() {
        return locations;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject root = new JSONObject();
        JSONArray locations = new JSONArray();
        for (DawarichBatchLocation loc : this.locations){
            locations.put(loc);
        }
        root.put("locations", locations);
        return root;
    }
}

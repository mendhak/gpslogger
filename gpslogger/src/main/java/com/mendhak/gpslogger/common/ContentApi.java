/*
 * Copyright (C) 2016 mendhak
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


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;

public class ContentApi extends ContentProvider {

    private static final Logger LOG = Logs.of(ContentApi.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String queryType = uri.getPathSegments().get(0);
        LOG.debug(queryType);
        String result;

        switch(queryType){
            case "gpslogger_folder":
                result = preferenceHelper.getGpsLoggerFolder();
                break;
            default:
                result = "NULL";
                break;
        }


        LOG.debug(result);
        MatrixCursor matrixCursor = new MatrixCursor(new String[] { "Column1" });

        matrixCursor.newRow().add(result);
        return matrixCursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}

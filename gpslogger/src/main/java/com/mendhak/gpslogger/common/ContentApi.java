package com.mendhak.gpslogger.common;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import org.slf4j.LoggerFactory;

public class ContentApi extends ContentProvider {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(ContentApi.class.getSimpleName());

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String queryType = uri.getPathSegments().get(0);
        tracer.debug(queryType);
        String result = "";

        switch(queryType){
            case "gpslogger_folder":
                result = AppSettings.getGpsLoggerFolder();
                break;
            default:
                result = "NULL";
                break;
        }


        tracer.debug(result);
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

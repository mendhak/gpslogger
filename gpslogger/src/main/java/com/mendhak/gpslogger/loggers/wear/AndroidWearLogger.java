package com.mendhak.gpslogger.loggers.wear;


import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.*;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.FileLogger;
import org.slf4j.Logger;

import java.util.Date;

public class AndroidWearLogger implements FileLogger, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final Logger LOG = Logs.of(AndroidWearLogger.class);
    private GoogleApiClient googleClient;
    private Context context;
    Location loc;

    public AndroidWearLogger(Context context){
        this.context = context;

    }

    @Override
    public void write(Location loc) throws Exception {

        this.loc = loc;

        LOG.debug("Android wear logger - connect to device");

        // Build a new GoogleApiClient for the Wearable API
        googleClient = new GoogleApiClient.Builder(context)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();
    }

    @Override
    public void annotate(String description, Location loc) throws Exception { }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        DataMap dataMap = new DataMap();
        dataMap.putLong("timestamp", System.currentTimeMillis());

        if(loc != null){
            dataMap.putLong("fixtime", loc.getTime());
            dataMap.putString("latitude", Strings.getFormattedLatitude(loc.getLatitude()));
            dataMap.putString("longitude", Strings.getFormattedLongitude(loc.getLongitude()));
            dataMap.putDouble("altitude", loc.getAltitude());
        }

        dataMap.putBoolean("session", Session.isStarted());

        new SendToDataLayerThread("/latest_gps", dataMap).start();

    }

    @Override
    public void onConnectionSuspended(int i) {
        LOG.debug("Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LOG.debug("Connection failed");
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();

            if(googleClient.hasConnectedApi(Wearable.API)){
                PutDataMapRequest putDMR = PutDataMapRequest.create("/latest_gps");
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();

                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
                if (result.getStatus().isSuccess()) {
                    LOG.debug("DataMap: " + dataMap + " sent successfully to data layer ");
                }
                else {
                    // Log an error
                    LOG.debug("ERROR: failed to send DataMap to data layer");
                }
            }

            if (null != googleClient && googleClient.isConnected()) {
                googleClient.disconnect();
            }
        }
    }
}

//package com.mendhak.gpslogger;
//
//
//import android.util.Log;
//import com.google.android.gms.wearable.*;
//
//public class ListenerService extends WearableListenerService {
//
//    private static final String WEARABLE_DATA_PATH = "/latest_gps";
//
//    @Override
//    public void onDataChanged(DataEventBuffer dataEvents) {
//
//        DataMap dataMap;
//        for (DataEvent event : dataEvents) {
//
//            // Check the data type
//            if (event.getType() == DataEvent.TYPE_CHANGED) {
//                // Check the data path
//                String path = event.getDataItem().getUri().getPath();
//                if (path.equals(WEARABLE_DATA_PATH)) {}
//                dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
//                Log.v("GPSLOGGER", "DataMap received on watch: " + dataMap);
//            }
//        }
//    }
//}

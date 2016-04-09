package com.mendhak.gpslogger;


import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class AndroidWearListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/message_path")) {
            final String message = new String(messageEvent.getData());
            Log.v("GPSLOGGER", "Message path received on mob is: " + messageEvent.getPath());
            Log.v("GPSLOGGER", "Message received on mob is: " + message);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
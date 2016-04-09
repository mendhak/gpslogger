package com.mendhak.gpslogger;


import android.content.Intent;
import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;


public class AndroidWearListenerService extends WearableListenerService {
    private static final Logger LOG = Logs.of(AndroidWearListenerService.class);


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/message_path")) {
            final String message = new String(messageEvent.getData());
            LOG.debug("Message path received on mob is: " + messageEvent.getPath());
            LOG.debug("Message received on mob is: " + message);

            LOG.debug("Session started: " + Session.isStarted());

            Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
            if(Session.isStarted()){
                serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP,true);
            }
            else {
                serviceIntent.putExtra(IntentConstants.IMMEDIATE_START,true);
            }

            getApplicationContext().startService(serviceIntent);

        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
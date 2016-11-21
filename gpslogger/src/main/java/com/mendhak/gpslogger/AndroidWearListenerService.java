package com.mendhak.gpslogger;


import android.content.Intent;
import android.location.Location;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.wear.AndroidWearLogger;
import org.slf4j.Logger;


public class AndroidWearListenerService extends WearableListenerService {
    private static final Logger LOG = Logs.of(AndroidWearListenerService.class);
    private Session session = Session.getInstance();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/start_stop")) {
            final String message = new String(messageEvent.getData());
            LOG.debug("Message path received on mob is: " + messageEvent.getPath());
            LOG.debug("Message received on mob is: " + message);

            LOG.debug("Session started: " + session.isStarted());

            Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
            if(session.isStarted()){
                serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP,true);
            }
            else {
                serviceIntent.putExtra(IntentConstants.IMMEDIATE_START,true);
            }

            getApplicationContext().startService(serviceIntent);

        }
        else if(messageEvent.getPath().equals("/get_status")){
            LOG.debug("Get Status request from Android Wear");

            try {

                Location loc = session.getCurrentLocationInfo() == null ?  session.getPreviousLocationInfo(): session.getCurrentLocationInfo();
                AndroidWearLogger logger = new AndroidWearLogger(getApplicationContext());
                logger.write(loc);

            } catch (Exception e) {
                LOG.error("Could not send latest location info to Android Wear", e);
            }
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
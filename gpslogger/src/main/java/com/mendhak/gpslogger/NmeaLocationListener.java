package com.mendhak.gpslogger;

import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NmeaLocationListener implements OnNmeaMessageListener {

    private static GeneralLocationListener listener;

    public NmeaLocationListener(GeneralLocationListener generalLocationListener){
        listener = generalLocationListener;
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
        listener.onNmeaReceived(timestamp,message);

    }
}

package com.mendhak.gpslogger.loggers;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;

import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.utils.LocationBuffer;

import java.io.IOException;

public abstract class AbstractLiveLogger extends AbstractLogger {
    private final LocationBuffer loc_buffer = new LocationBuffer();

    private Runnable flusher;
    private Handler handler;
    private FlusherAsyncTask flushertask;

    private static String name = "AbstractLiveLogger";

    public abstract boolean liveUpload(LocationBuffer.BufferedLocation bloc) throws IOException;

    public AbstractLiveLogger(final int minsec, final int mindist){
        super(minsec, mindist);

        this.handler = new Handler();

        flusher = new Runnable() {
            @Override
            public void run() {
                execAsyncFlush();
                handler.postDelayed(flusher, minsec * 1000);
            }
        };
        flusher.run();
    }

    private class FlusherAsyncTask extends AsyncTask<LocationBuffer, Void, Void> {
        @Override
        protected Void doInBackground(LocationBuffer... buffers) {
            for (LocationBuffer buf : buffers){
                LocationBuffer.BufferedLocation b;

                Utilities.LogDebug(name + " flushing buffer (" + buf.size() + ")");
                int i = 0;

                while((b = buf.peek()) != null) {
                    try {
                        Utilities.LogDebug(name + " flushing elt " + i);
                        Utilities.LogDebug("TIME: " + b.timems);
                        if (liveUpload(b)){
                            buf.pop();
                            i++;
                        } else {
                            Utilities.LogDebug(name + " failed flush elt " + i);
                        }
                    } catch (IOException ex) {
                        Utilities.LogDebug(name + ": sending fix", ex);
                    }
                }
                Utilities.LogDebug(name + ": finished flushing " + i + " locations" );
            }
            return null;
        }
    };

//    int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    @SuppressLint("NewApi")
    private void execAsyncFlush(){
        if (flushertask == null || flushertask.getStatus() != AsyncTask.Status.RUNNING){
            Utilities.LogDebug(name + " starting flusher task");

            if (flushertask == null || flushertask.getStatus() == AsyncTask.Status.FINISHED) {
                flushertask = new FlusherAsyncTask();
            }
//            if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
//                flushertask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            } else {
                flushertask.execute(loc_buffer);
//            }
        } else {
            Utilities.LogDebug(name + " flusher task already running");
        }
    }

    @Override
    public void close() throws Exception{
        this.handler.removeCallbacks(flusher);
        execAsyncFlush();
    }

    @Override
    public void Write(Location loc) throws Exception
    {
//        final long now = SystemClock.elapsedRealtime();
//        calendar.setTimeInMillis(loc.getTime());
        SetLatestTimeStamp(System.currentTimeMillis());
        // get time from system, not location: prevent emulator problem with wrong date
        // in simulated location
        final long ms_of_day =  System.currentTimeMillis(); //loc.getTime();
//        final int second_of_day =
//                calendar.get(Calendar.HOUR_OF_DAY) * 3600
//                        + calendar.get(Calendar.MINUTE) * 60
//                        + calendar.get(Calendar.SECOND);
//        final int ms_of_day = second_of_day * 1000
//                + calendar.get(Calendar.MILLISECOND);
        loc_buffer.push(
                ms_of_day,
                loc.getLatitude(), loc.getLongitude(),
                (int)loc.getAltitude(),
                (int)loc.getBearing(),
                (int)(loc.getSpeed() / 3.6)
        );
        Utilities.LogDebug(name  + " pushed (" + loc_buffer.size() + ")");

//
//        if (now >= nextUpdateTime) {
//            new WriteAsync().execute(loc);
//            nextUpdateTime = now + intervalMS;
//        }
    }
}

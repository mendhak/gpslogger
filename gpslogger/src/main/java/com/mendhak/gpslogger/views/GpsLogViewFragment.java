package com.mendhak.gpslogger.views;


import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GpsLogViewFragment extends GenericViewFragment {

    private View rootView;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsLogViewFragment.class.getSimpleName());
    long startTime = 0;
    TextView logTextView;
    ScrollView scrollView;

    Handler timerHandler = new Handler();

    public static final GpsLogViewFragment newInstance() {
        GpsLogViewFragment fragment = new GpsLogViewFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_log_view, container, false);
        logTextView = (TextView) rootView.findViewById(R.id.logview_txtstatus);
        scrollView = (ScrollView) rootView.findViewById(R.id.logview_scrollView);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }


    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            ShowLogcatMessages();
            timerHandler.postDelayed(this, 1500);
        }
    };


    private void ShowLogcatMessages(){


        StringBuilder sb = new StringBuilder();
        for(ILoggingEvent message : SessionLogcatAppender.Statuses){

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            sb.append(sdf.format(new Date(message.getTimeStamp())));
            sb.append(" ");
            if(message.getLevel() == Level.ERROR) {
                sb.append("<font color='red'>" + message.getMessage() + "</font>");
            }
            else {
                sb.append(message.getMessage());
            }
            sb.append("<br />");
        }
        logTextView.setText(Html.fromHtml(sb.toString()));

        scrollView.fullScroll(View.FOCUS_DOWN);
    }



}

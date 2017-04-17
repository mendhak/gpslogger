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

package com.mendhak.gpslogger.ui.fragments.display;


import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import com.mendhak.gpslogger.ui.components.InteractiveScrollView;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GpsLogViewFragment extends GenericViewFragment implements CompoundButton.OnCheckedChangeListener {

    private View rootView;
    private static final Logger LOG = Logs.of(GpsLogViewFragment.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    long startTime = 0;
    TextView logTextView;
    InteractiveScrollView scrollView;
    private boolean doAutomaticScroll = true;
    private Session session = Session.getInstance();

    Handler timerHandler = new Handler();

    public static GpsLogViewFragment newInstance() {
        return new GpsLogViewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_log_view, container, false);
        logTextView = (TextView) rootView.findViewById(R.id.logview_txtstatus);
        scrollView = (InteractiveScrollView) rootView.findViewById(R.id.logview_scrollView);
        scrollView.setOnScrolledUpListener(new InteractiveScrollView.OnScrolledUpListener() {
            @Override
            public void onScrolledUp(int scrollY) {
                doAutomaticScroll=false;
            }
        });

        scrollView.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached(int scrollY) {
                doAutomaticScroll=true;
            }
        });

        CheckBox chkStartLogging = (CheckBox) rootView.findViewById(R.id.logview_startLogging);
        chkStartLogging.setChecked(session.isStarted());
        chkStartLogging.setOnCheckedChangeListener(this);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            showLogcatMessages();
            timerHandler.postDelayed(this, 1500);
        }
    };


    void showLogcatMessages(){

        CheckBox chkLocationsOnly = (CheckBox) rootView.findViewById(R.id.logview_chkLocationsOnly);

        StringBuilder sb = new StringBuilder();
        for(ILoggingEvent message : SessionLogcatAppender.Statuses){

            if(message.getMarker() == SessionLogcatAppender.MARKER_LOCATION){
                sb.append(getFormattedMessage(message.getMessage(), R.color.accentColorComplementary, message.getTimeStamp()));
            }

            else if(!chkLocationsOnly.isChecked()){
                if(message.getLevel() == Level.ERROR) {
                    sb.append(getFormattedMessage(message.getMessage(), R.color.errorColor, message.getTimeStamp()));

                }
                else if(message.getLevel() == Level.WARN){
                    sb.append(getFormattedMessage(message.getMessage(), R.color.warningColor, message.getTimeStamp()));

                }
                else {
                    sb.append(getFormattedMessage(message.getMessage(), R.color.secondaryColorText, message.getTimeStamp()));
                }
            }

        }
        logTextView.setText(Html.fromHtml(sb.toString()));

        if(doAutomaticScroll){
            scrollView.fullScroll(View.FOCUS_DOWN);
        }

    }

    private String getFormattedMessage(String message, int colorResourceId, long timeStamp){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String dateStamp = sdf.format(new Date(timeStamp)) + " ";

        String messageFormat = "%s<font color='#%s'>%s</font><br />";

        return String.format(messageFormat,
                dateStamp,
                Integer.toHexString(ContextCompat.getColor(rootView.getContext(), colorResourceId)).substring(2), message);

    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        if(compoundButton.getId() == R.id.logview_startLogging){
            requestToggleLogging();
        }
    }
}

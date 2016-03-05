package com.mendhak.gpslogger.views;


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
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GpsLogViewFragment extends GenericViewFragment implements CompoundButton.OnCheckedChangeListener {

    private View rootView;
    private static final Logger LOG = Logs.of(GpsLogViewFragment.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    long startTime = 0;
    TextView logTextView;
    ScrollView scrollView;

    Handler timerHandler = new Handler();

    public static GpsLogViewFragment newInstance() {
        return new GpsLogViewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_log_view, container, false);
        logTextView = (TextView) rootView.findViewById(R.id.logview_txtstatus);
        scrollView = (ScrollView) rootView.findViewById(R.id.logview_scrollView);

        CheckBox chkDebugFile = (CheckBox) rootView.findViewById(R.id.logview_chkDebugFile);
        chkDebugFile.setChecked(preferenceHelper.shouldDebugToFile());
        chkDebugFile.setOnCheckedChangeListener(this);
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
        CheckBox chkAutoScroll = (CheckBox) rootView.findViewById(R.id.logview_chkAutoScroll);


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

        if(chkAutoScroll.isChecked()){
            scrollView.fullScroll(View.FOCUS_DOWN);
        }

    }

    private String getFormattedMessage(String message, int colorResourceId, long timeStamp){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String dateStamp = sdf.format(new Date(timeStamp)) + " ";

        String messageFormat = "%s<font color='#%s'>%s</font><br />";

        return String.format(messageFormat,
                dateStamp,
                Integer.toHexString(ContextCompat.getColor(getActivity(), colorResourceId)).substring(2), message);

    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        if(compoundButton.getId() == R.id.logview_chkDebugFile){
            preferenceHelper.setDebugToFile(checked);

            if(checked){
                Toast.makeText(getActivity(), R.string.debuglog_summary, Toast.LENGTH_LONG).show();
            }
        }
    }
}

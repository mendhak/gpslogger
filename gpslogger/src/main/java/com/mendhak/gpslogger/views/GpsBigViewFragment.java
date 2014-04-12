package com.mendhak.gpslogger.views;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Session;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

public class GpsBigViewFragment extends GenericViewFragment implements View.OnTouchListener {

    View rootView;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsBigViewFragment.class.getSimpleName());


    public static final GpsBigViewFragment newInstance() {
        GpsBigViewFragment fragment = new GpsBigViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_big_view, container, false);

        TextView txtLat = (TextView)rootView.findViewById(R.id.bigview_text_lat);
        txtLat.setOnTouchListener(this);

        TextView txtLong = (TextView)rootView.findViewById(R.id.bigview_text_long);
        txtLong.setOnTouchListener(this);

        SetLocation(Session.getCurrentLocationInfo());

        if(Session.isStarted()){
            Toast.makeText(getActivity().getApplicationContext(), R.string.bigview_taptotoggle, Toast.LENGTH_SHORT).show();
        }


        return rootView;
    }

    @Override
    public void SetLocation(Location locationInfo) {
        TextView txtLat = (TextView)rootView.findViewById(R.id.bigview_text_lat);
        TextView txtLong = (TextView)rootView.findViewById(R.id.bigview_text_long);

        if(locationInfo != null){
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(6);

            txtLat.setText(String.valueOf(nf.format(locationInfo.getLatitude())));

            txtLong.setText(String.valueOf(nf.format(locationInfo.getLongitude())));
        }
        else if (Session.isStarted())
        {
            txtLat.setText("...");
        }
        else {
            txtLat.setText(R.string.bigview_taptotoggle);
        }
    }

    @Override
    public void SetSatelliteCount(int count) {

    }

    @Override
    public void SetLoggingStarted() {
        TextView txtLat = (TextView)rootView.findViewById(R.id.bigview_text_lat);
        TextView txtLong = (TextView)rootView.findViewById(R.id.bigview_text_long);
        txtLat.setText("");
        txtLong.setText("");
    }

    @Override
    public void SetLoggingStopped() {

    }

    @Override
    public void SetStatusMessage(String message) {

    }

    @Override
    public void SetFatalMessage(String message) {

    }

    @Override
    public void OnFileNameChange(String newFileName) {

    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
        {
            tracer.debug("Big frame - onTouch event");
            requestToggleLogging();
            return true;
        }

        return false;

    }
}

package com.mendhak.gpslogger.views;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Session;
import org.w3c.dom.Text;

/**
 * Created by mendhak on 31/03/14.
 */
public class GpsLegacyFragment extends Fragment implements View.OnClickListener {

    View rootView;
    IGpsLegacyFragmentListener callback;

    @Override
    public void onClick(View view) {
        callback.OnGpsLegacyButtonClick();
    }

    public void onTextUpdate(String message){
        TextView txt = (TextView)rootView.findViewById(R.id.textViewLocation);
        txt.setText(message);
    }

    public void setCurrentlyLogging(boolean loggingStatus) {
        TextView txt = (TextView)rootView.findViewById(R.id.textViewLocation);
        txt.setText("Started logging " + String.valueOf(loggingStatus));
    }

    public void setLocationInfo(Location currentLocationInfo) {
        if(currentLocationInfo != null){
            TextView txt = (TextView)rootView.findViewById(R.id.textViewLocation);
            txt.setText(String.valueOf(currentLocationInfo.getAccuracy()));
        }

    }

    public interface IGpsLegacyFragmentListener{
        public void OnNewGpsLegacyMessage(String message);


        public void OnGpsLegacyButtonClick();
    }

    public static GpsLegacyFragment newInstance() {
        return new GpsLegacyFragment();
    }

    public GpsLegacyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);


        setCurrentlyLogging(Session.isStarted());
        setLocationInfo(Session.getCurrentLocationInfo());

        Button btnStart = (Button) rootView.findViewById(R.id.buttonStart);
        btnStart.setText("Found it");
        btnStart.setOnClickListener(this);

//            textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(getArguments().getString("parent_message"));

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            callback = (IGpsLegacyFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }

        callback.OnNewGpsLegacyMessage("Well well well");



    }

}

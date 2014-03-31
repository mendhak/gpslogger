package com.mendhak.gpslogger.views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mendhak.gpslogger.R;

/**
 * Created by mendhak on 31/03/14.
 */
public class GpsLegacyFragment extends Fragment {

    IGpsLegacyFragmentListener callback;

    public interface IGpsLegacyFragmentListener{
        public void OnNewGpsLegacyMessage(String message);
    }

    public static GpsLegacyFragment newInstance() {
        return new GpsLegacyFragment();
    }

    public GpsLegacyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView;
        TextView textView;
        rootView = inflater.inflate(R.layout.fragment_gps_main, container, false);
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

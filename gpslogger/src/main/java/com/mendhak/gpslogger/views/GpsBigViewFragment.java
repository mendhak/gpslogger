/*******************************************************************************
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
 ******************************************************************************/

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

        TextView txtLat = (TextView) rootView.findViewById(R.id.bigview_text_lat);
        txtLat.setOnTouchListener(this);

        TextView txtLong = (TextView) rootView.findViewById(R.id.bigview_text_long);
        txtLong.setOnTouchListener(this);

        SetLocation(Session.getCurrentLocationInfo());

        if (Session.isStarted()) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.bigview_taptotoggle, Toast.LENGTH_SHORT).show();
        }


        return rootView;
    }

    @Override
    public void SetLocation(Location locationInfo) {
        TextView txtLat = (TextView) rootView.findViewById(R.id.bigview_text_lat);
        TextView txtLong = (TextView) rootView.findViewById(R.id.bigview_text_long);

        if (locationInfo != null) {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(6);

            txtLat.setText(String.valueOf(nf.format(locationInfo.getLatitude())));

            txtLong.setText(String.valueOf(nf.format(locationInfo.getLongitude())));
        } else if (Session.isStarted()) {
            txtLat.setText("...");
        } else {
            txtLat.setText(R.string.bigview_taptotoggle);
        }
    }

    @Override
    public void SetSatelliteCount(int count) {

    }

    @Override
    public void SetLoggingStarted() {
        TextView txtLat = (TextView) rootView.findViewById(R.id.bigview_text_lat);
        TextView txtLong = (TextView) rootView.findViewById(R.id.bigview_text_long);
        txtLat.setText("");
        txtLong.setText("");
    }

    @Override
    public void SetLoggingStopped() {

    }

    @Override
    public void OnWaitingForLocation(boolean inProgress) {

    }

    @Override
    public void OnFileNameChange(String newFileName) {

    }

    @Override
    public void OnNmeaSentence(long timestamp, String nmeaSentence) {

    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            tracer.debug("Big frame - onTouch event");
            requestToggleLogging();
            return true;
        }

        return false;

    }
}

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

package com.mendhak.gpslogger.ui.fragments.display;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.ServiceEvents;


public class GpsBigViewFragment extends GenericViewFragment implements View.OnTouchListener {

    View rootView;

    public static GpsBigViewFragment newInstance() {
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

        displayLocationInfo(Session.getCurrentLocationInfo());

        if (Session.isStarted()) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.bigview_taptotoggle, Toast.LENGTH_SHORT).show();
        }


        return rootView;
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationUpdate){
        displayLocationInfo(locationUpdate.location);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        if(loggingStatus.loggingStarted){
            TextView txtLat = (TextView) rootView.findViewById(R.id.bigview_text_lat);
            TextView txtLong = (TextView) rootView.findViewById(R.id.bigview_text_long);
            txtLat.setText("");
            txtLong.setText("");
        }
    }

    public void displayLocationInfo(Location locationInfo){

        TextView txtLat = (TextView) rootView.findViewById(R.id.bigview_text_lat);
        TextView txtLong = (TextView) rootView.findViewById(R.id.bigview_text_long);

        if (locationInfo != null) {
            txtLat.setText(String.valueOf(Strings.getFormattedLatitude(locationInfo.getLatitude())));

            txtLong.setText(String.valueOf(Strings.getFormattedLongitude(locationInfo.getLongitude())));
        } else if (Session.isStarted()) {
            txtLat.setText("...");
        } else {
            txtLat.setText(R.string.bigview_taptotoggle);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            requestToggleLogging();
            return true;
        }

        return false;

    }
}

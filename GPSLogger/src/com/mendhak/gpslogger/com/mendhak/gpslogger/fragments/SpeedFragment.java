/*
 *    This file is part of GPSLogger for Android.
 *
 *    Copyright Marc Poulhiès <dkm@kataplop.net>
 *
 *    GPSLogger for Android is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    GPSLogger for Android is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.com.mendhak.gpslogger.fragments;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import net.kataplop.gpslogger.R;


public class SpeedFragment extends SherlockFragment implements  IWidgetFragment {
    private TextView tvSpeed;
    private TextView tvUnit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.speed_fragment, container, false);
        tvSpeed = (TextView) v.findViewById(R.id.big_speed);
        tvUnit = (TextView) v.findViewById(R.id.big_speed_unit);

        return v;
    }

    @Override
    public String getTitle() {
        // FIXME move to strings

        return "speed";
    }

    @Override
    public void onLocationChanged(final Location loc) {
        Utilities.LogDebug("SpeedFragment.onLocationChanged");

        if (loc == null) {
            return;
        }

        setSpeed(loc);
    }

    private void setSpeed(final Location loc) {
        String unit;
        if (AppSettings.shouldUseImperial()) {
            tvUnit.setText(getString(R.string.miles_per_hour));
        } else {
            tvUnit.setText(R.string.kilometers_per_hour);
        }

        if (loc.hasSpeed()) {
            float speed = loc.getSpeed();

            if (AppSettings.shouldUseImperial()) {
                speed = speed * 0.6818f;
            } else {
                speed = speed * 3.6f;
            }

            final String sspeed = Integer.toString((int)speed);

            tvSpeed.setText(sspeed);
        } else {
            tvSpeed.setText(R.string.not_applicable);
        }
    }

    @Override
    public void setSatelliteInfo(int number) {
    }

    @Override
    public void setStatus(final String message) {
    }

    @Override
    public void clear() {
        tvSpeed.setText("-");
    }
}
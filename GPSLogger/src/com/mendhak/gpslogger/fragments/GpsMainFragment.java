/*
 *    This file is part of GPSLogger for Android.
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
package com.mendhak.gpslogger.fragments;

import com.actionbarsherlock.app.SherlockFragment;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import net.kataplop.gpslogger.R;

import java.util.Date;

public class GpsMainFragment extends SherlockFragment implements IWidgetFragment {

    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvDateTime;
    private TextView tvAltitude;
    private TextView txtSpeed;
    private TextView txtSatellites;
    private TextView txtDirection;
    private TextView txtAccuracy;
    private TextView txtTravelled;
    private TextView tvStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.main, container, false);
        tvStatus = (TextView) v.findViewById(R.id.textStatus);

        tvLatitude = (TextView) v.findViewById(R.id.txtLatitude);
        tvLongitude = (TextView) v.findViewById(R.id.txtLongitude);
        tvDateTime = (TextView) v.findViewById(R.id.txtDateTimeAndProvider);

        tvAltitude = (TextView) v.findViewById(R.id.txtAltitude);

        txtSpeed = (TextView) v.findViewById(R.id.txtSpeed);

        txtSatellites = (TextView) v.findViewById(R.id.txtSatellites);
        txtDirection = (TextView) v.findViewById(R.id.txtDirection);
        txtAccuracy = (TextView) v.findViewById(R.id.txtAccuracy);
        txtTravelled = (TextView) v.findViewById(R.id.txtDistanceTravelled);

        return v;
    }

    @Override
    public String getTitle() {
        return "main";
    }

    @Override
    public void onLocationChanged(Location loc){
        displayLocationInfo(loc);
    }

    @Override
    public void setSatelliteInfo(int number){
        txtSatellites.setText(String.valueOf(number));
    }

    @Override
    public void setStatus(String message) {
        tvStatus.setText(message);
    }

    @Override
    public void clear() {
        tvLatitude.setText("");
        tvLongitude.setText("");
        tvDateTime.setText("");
        tvAltitude.setText("");
        txtSpeed.setText("");
        txtSatellites.setText("");
        txtDirection.setText("");
        txtAccuracy.setText("");
        txtTravelled.setText("");
    }

    private void displayLocationInfo(Location loc) {
        Utilities.LogDebug("GpsMainActivity.DisplayLocationInfo");

        if (loc == null) {
            return;
        }

        String providerName = loc.getProvider();

        if (providerName.equalsIgnoreCase("gps")) {
            providerName = getString(R.string.providername_gps);
        } else {
            providerName = getString(R.string.providername_celltower);
        }

        tvDateTime.setText(new Date(Session.getLatestTimeStamp()).toLocaleString()
                + getString(R.string.providername_using, providerName));
        tvLatitude.setText(String.valueOf(loc.getLatitude()));
        tvLongitude.setText(String.valueOf(loc.getLongitude()));

        if (loc.hasAltitude()) {

            double altitude = loc.getAltitude();

            if (AppSettings.shouldUseImperial()) {
                tvAltitude.setText(String.valueOf(Utilities.MetersToFeet(altitude))
                        + getString(R.string.feet));
            } else {
                tvAltitude.setText(String.valueOf(altitude) + getString(R.string.meters));
            }

        } else {
            tvAltitude.setText(R.string.not_applicable);
        }

        if (loc.hasSpeed()) {

            float speed = loc.getSpeed();
            String unit;
            if (AppSettings.shouldUseImperial()) {
                if (speed > 1.47) {
                    speed = speed * 0.6818f;
                    unit = getString(R.string.miles_per_hour);

                } else {
                    speed = Utilities.MetersToFeet(speed);
                    unit = getString(R.string.feet_per_second);
                }
            } else {
                if (speed > 0.277) {
                    speed = speed * 3.6f;
                    unit = getString(R.string.kilometers_per_hour);
                } else {
                    unit = getString(R.string.meters_per_second);
                }
            }

            txtSpeed.setText(String.valueOf(speed) + unit);

        } else {
            txtSpeed.setText(R.string.not_applicable);
        }

        if (loc.hasBearing()) {

            float bearingDegrees = loc.getBearing();
            String direction;

            direction = Utilities.GetBearingDescription(bearingDegrees, getSherlockActivity());

            txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                    + getString(R.string.degree_symbol) + ")");
        } else {
            txtDirection.setText(R.string.not_applicable);
        }

        if (!Session.isUsingGps()) {
            txtSatellites.setText(R.string.not_applicable);
            Session.setSatelliteCount(0);
        }

        if (loc.hasAccuracy()) {

            float accuracy = loc.getAccuracy();

            if (AppSettings.shouldUseImperial()) {
                txtAccuracy.setText(getString(R.string.accuracy_within,
                        String.valueOf(Utilities.MetersToFeet(accuracy)), getString(R.string.feet)));

            } else {
                txtAccuracy.setText(getString(R.string.accuracy_within, String.valueOf(accuracy),
                        getString(R.string.meters)));
            }

        } else {
            txtAccuracy.setText(R.string.not_applicable);
        }

        String distanceUnit;
        double distanceValue = Session.getTotalTravelled();
        if (AppSettings.shouldUseImperial()) {
            distanceUnit = getString(R.string.feet);
            distanceValue = Utilities.MetersToFeet(distanceValue);
            // When it passes more than 1 kilometer, convert to miles.
            if (distanceValue > 3281) {
                distanceUnit = getString(R.string.miles);
                distanceValue = distanceValue / 5280;
            }
        } else {
            distanceUnit = getString(R.string.meters);
            if (distanceValue > 1000) {
                distanceUnit = getString(R.string.kilometers);
                distanceValue = distanceValue / 1000;
            }
        }

        txtTravelled.setText(String.valueOf(Math.round(distanceValue)) + " " + distanceUnit +
                " (" + Session.getNumLegs() + " points)");

    }
}
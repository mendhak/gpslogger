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

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.dd.processbutton.iml.ActionProcessButton;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class GpsDetailedViewFragment extends GenericViewFragment {


    private View rootView;
    private ActionProcessButton actionButton;
    private static final Logger LOG = Logs.of(GpsDetailedViewFragment.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private Session session = Session.getInstance();

    public static GpsDetailedViewFragment newInstance() {

        GpsDetailedViewFragment fragment = new GpsDetailedViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_detailed_view, container, false);

        actionButton = (ActionProcessButton)rootView.findViewById(R.id.btnActionProcess);
        actionButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.accentColor ));

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestToggleLogging();
            }
        });


        if (session.hasValidLocation()) {
            displayLocationInfo(session.getCurrentLocationInfo());
        }

        showPreferencesAndMessages();

        return rootView;
    }


    private void setActionButtonStart(){
        actionButton.setText(R.string.btn_start_logging);
        actionButton.setBackgroundColor( ContextCompat.getColor(getActivity(), R.color.accentColor));
        actionButton.setAlpha(0.8f);
    }

    private void setActionButtonStop(){
        actionButton.setText(R.string.btn_stop_logging);
        actionButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.accentColorComplementary));
        actionButton.setAlpha(0.8f);
    }

    @Override
    public void onStart() {

        setActionButtonStop();
        super.onStart();
    }

    @Override
    public void onResume() {

        if(session.isStarted()){
            setActionButtonStop();
        }
        else {
            setActionButtonStart();
        }

        showPreferencesAndMessages();
        super.onResume();
    }

    /**
     * Displays a human readable summary of the preferences chosen by the user
     * on the main form
     */
    private void showPreferencesAndMessages() {

        try {
            TextView txtLoggingTo = (TextView) rootView.findViewById(R.id.detailedview_loggingto_text);
            TextView txtFrequency = (TextView) rootView.findViewById(R.id.detailedview_frequency_text);
            TextView txtDistance = (TextView) rootView.findViewById(R.id.detailedview_distance_text);
            TextView txtAutoEmail = (TextView) rootView.findViewById(R.id.detailedview_autosend_text);

            List<FileLogger> loggers = FileLoggerFactory.getFileLoggers(getActivity().getApplicationContext());

            if (loggers.size() > 0) {

                StringBuilder enabledLoggers = new StringBuilder();

                for(FileLogger l : loggers){
                    if(!Strings.isNullOrEmpty(l.getName())){
                        enabledLoggers.append(l.getName() + " ");
                    }
                }

                if (preferenceHelper.shouldLogToNmea()) {
                    enabledLoggers.append("NMEA ");
                }

                txtLoggingTo.setText(enabledLoggers.toString());

            } else {

                txtLoggingTo.setText(R.string.summary_loggingto_screen);

            }

            if (preferenceHelper.getMinimumLoggingInterval() > 0) {
                String descriptiveTime = Strings.getDescriptiveDurationString(preferenceHelper.getMinimumLoggingInterval(),
                        getActivity().getApplicationContext());

                txtFrequency.setText(descriptiveTime);
            } else {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            txtDistance.setText(Strings.getDistanceDisplay(getActivity(), preferenceHelper.getMinimumDistanceInterval(), preferenceHelper.shouldDisplayImperialUnits(), true));


            if (preferenceHelper.isAutoSendEnabled() && preferenceHelper.getAutoSendInterval() > 0) {
                String autoEmailDisplay = String.format(getString(R.string.autosend_frequency_display), String.valueOf(preferenceHelper.getAutoSendInterval()));
                txtAutoEmail.setText(autoEmailDisplay);
            }


            showCurrentFileName(Strings.getFormattedFileName());


            TextView txtTargets = (TextView) rootView.findViewById(R.id.detailedview_autosendtargets_text);

            if(preferenceHelper.isAutoSendEnabled()){
                StringBuilder sb = new StringBuilder();
                if (FileSenderFactory.getEmailSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.autoemail_title)).append("\n");
                }

                if (FileSenderFactory.getFtpSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.autoftp_setup_title)).append("\n");
                }

                if(FileSenderFactory.getSFTPSender().isAutoSendAvailable()){
                    sb.append(getString(R.string.sftp_setup_title)).append("\n");
                }


                if (FileSenderFactory.getOsmSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.osm_setup_title)).append("\n");
                }

                if (FileSenderFactory.getDropBoxSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.dropbox_setup_title)).append("\n");
                }

                if (FileSenderFactory.getOpenGTSSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.opengts_setup_title)).append("\n");
                }

                if (FileSenderFactory.getOwnCloudSender().isAutoSendAvailable()) {
                    sb.append(getString(R.string.owncloud_setup_title)).append("\n");
                }

                txtTargets.setText(sb.toString());
            }
            else {
                txtTargets.setText("");
            }



        } catch (Exception ex) {
            LOG.error("showPreferencesAndMessages " + ex.getMessage(), ex);
        }


    }

    public void showCurrentFileName(String newFileName) {

        TextView txtFilename = (TextView) rootView.findViewById(R.id.detailedview_file_text);
        txtFilename.setTextIsSelectable(true);
        txtFilename.setSelectAllOnFocus(true);

        txtFilename.setText(Strings.getFormattedFileName() + "\n (" + preferenceHelper.getGpsLoggerFolder() + ")");

    }


    public void setSatelliteCount(int count) {
        TextView txtSatellites = (TextView) rootView.findViewById(R.id.detailedview_satellites_text);
        txtSatellites.setText(String.valueOf(count));
    }

    private void clearDisplay() {
        TextView tvLatitude = (TextView) rootView.findViewById(R.id.detailedview_lat_text);
        TextView tvLongitude = (TextView) rootView.findViewById(R.id.detailedview_lon_text);
        TextView tvDateTime = (TextView) rootView.findViewById(R.id.detailedview_datetime_text);

        TextView tvAltitude = (TextView) rootView.findViewById(R.id.detailedview_altitude_text);

        TextView txtSpeed = (TextView) rootView.findViewById(R.id.detailedview_speed_text);

        TextView txtSatellites = (TextView) rootView.findViewById(R.id.detailedview_satellites_text);
        TextView txtDirection = (TextView) rootView.findViewById(R.id.detailedview_direction_text);
        TextView txtAccuracy = (TextView) rootView.findViewById(R.id.detailedview_accuracy_text);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.detailedview_travelled_text);
        TextView txtTime = (TextView) rootView.findViewById(R.id.detailedview_duration_text);

        TextView txtStill = (TextView) rootView.findViewById(R.id.detailedview_activity_text);

        tvLatitude.setText("");
        tvLongitude.setText("");
        tvDateTime.setText("");
        tvAltitude.setText("");
        txtSpeed.setText("");
        txtSatellites.setText("");
        txtAccuracy.setText("");
        txtDirection.setText("");
        txtTravelled.setText("");
        txtTime.setText("");
        txtStill.setText("");


    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationEvent){
        displayLocationInfo(locationEvent.location);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.SatellitesVisible satellitesVisible){
        setSatelliteCount(satellitesVisible.satelliteCount);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        if(loggingStatus.loggingStarted){
            setActionButtonStop();
            showPreferencesAndMessages();
            clearDisplay();
        }
        else {
            setActionButtonStart();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.FileNamed fileNamed){
        showCurrentFileName(fileNamed.newFileName);
    }

    public void displayLocationInfo(Location locationInfo){
        if (locationInfo == null) {
            return;
        }

        showPreferencesAndMessages();

        TextView tvLatitude = (TextView) rootView.findViewById(R.id.detailedview_lat_text);
        TextView tvLongitude = (TextView) rootView.findViewById(R.id.detailedview_lon_text);
        TextView tvDateTime = (TextView) rootView.findViewById(R.id.detailedview_datetime_text);

        TextView tvAltitude = (TextView) rootView.findViewById(R.id.detailedview_altitude_text);

        TextView txtSpeed = (TextView) rootView.findViewById(R.id.detailedview_speed_text);

        TextView txtSatellites = (TextView) rootView.findViewById(R.id.detailedview_satellites_text);
        TextView txtDirection = (TextView) rootView.findViewById(R.id.detailedview_direction_text);
        TextView txtAccuracy = (TextView) rootView.findViewById(R.id.detailedview_accuracy_text);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.detailedview_travelled_text);
        TextView txtTime = (TextView) rootView.findViewById(R.id.detailedview_duration_text);
        String providerName = locationInfo.getProvider();
        if (providerName.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            providerName = getString(R.string.providername_gps);
        } else {
            providerName = getString(R.string.providername_celltower);
        }

        tvDateTime.setText(android.text.format.DateFormat.getDateFormat(getActivity()).format(new Date(session.getLatestTimeStamp()))
                + " " + new SimpleDateFormat("HH:mm:ss").format(new Date(session.getLatestTimeStamp()))
                + " - " + providerName);

        tvLatitude.setText(String.valueOf(Strings.getFormattedLatitude(locationInfo.getLatitude())));
        tvLongitude.setText(String.valueOf(Strings.getFormattedLongitude(locationInfo.getLongitude())));

        if (locationInfo.hasAltitude()) {
            tvAltitude.setText(Strings.getDistanceDisplay(getActivity(), locationInfo.getAltitude(), preferenceHelper.shouldDisplayImperialUnits(), false));
        } else {
            tvAltitude.setText(R.string.not_applicable);
        }

        if (locationInfo.hasSpeed()) {
            txtSpeed.setText(Strings.getSpeedDisplay(getActivity(), locationInfo.getSpeed(), preferenceHelper.shouldDisplayImperialUnits()));

        } else {
            txtSpeed.setText(R.string.not_applicable);
        }

        if (locationInfo.hasBearing()) {

            float bearingDegrees = locationInfo.getBearing();
            String direction;

            direction = Strings.getBearingDescription(bearingDegrees, getActivity().getApplicationContext());

            txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                    + getString(R.string.degree_symbol) + ")");
        } else {
            txtDirection.setText(R.string.not_applicable);
        }

        if (!session.isUsingGps()) {
            txtSatellites.setText(R.string.not_applicable);
        }

        if (locationInfo.hasAccuracy()) {

            float accuracy = locationInfo.getAccuracy();
            txtAccuracy.setText(getString(R.string.accuracy_within, Strings.getDistanceDisplay(getActivity(), accuracy, preferenceHelper.shouldDisplayImperialUnits(), true), ""));

        } else {
            txtAccuracy.setText(R.string.not_applicable);
        }

        double distanceValue = session.getTotalTravelled();
        txtTravelled.setText(Strings.getDistanceDisplay(getActivity(), distanceValue, preferenceHelper.shouldDisplayImperialUnits(), true) + " (" + session.getNumLegs() + " points)");

        long startTime = session.getStartTimeStamp();
        Date d = new Date(startTime);
        long currentTime = System.currentTimeMillis();

        String duration = Strings.getDescriptiveDurationString((int) (currentTime - startTime) / 1000, getActivity());

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext());
        txtTime.setText(duration + " (started at " + dateFormat.format(d) + " " + timeFormat.format(d) + ")");



    }



}

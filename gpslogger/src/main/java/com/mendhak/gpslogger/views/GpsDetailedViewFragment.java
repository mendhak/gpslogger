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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.dd.processbutton.iml.ActionProcessButton;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import org.slf4j.LoggerFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;


public class GpsDetailedViewFragment extends GenericViewFragment {


    private View rootView;
    private ActionProcessButton actionButton;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsDetailedViewFragment.class.getSimpleName());

    public static final GpsDetailedViewFragment newInstance() {

        GpsDetailedViewFragment fragment = new GpsDetailedViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: Inflates the detailed layout

        rootView = inflater.inflate(R.layout.fragment_detailed_view, container, false);

        actionButton = (ActionProcessButton)rootView.findViewById(R.id.btnActionProcess);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColor));

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestToggleLogging();
            }
        });


        if (Session.hasValidLocation()) {
            DisplayLocationInfo(Session.getCurrentLocationInfo());
        }

        showPreferencesSummary();

        return rootView;
    }


    private void setActionButtonStart(){
        actionButton.setText(R.string.btn_start_logging);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColor));
        actionButton.setAlpha(0.8f);
    }

    private void setActionButtonStop(){
        actionButton.setText(R.string.btn_stop_logging);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColorComplementary));
        actionButton.setAlpha(0.8f);
    }

    @Override
    public void onStart() {

        setActionButtonStop();
        super.onStart();
    }

    @Override
    public void onResume() {

        if(Session.isStarted()){
            setActionButtonStop();
        }
        else {
            setActionButtonStart();
        }

        showPreferencesSummary();
        super.onResume();
    }

    /**
     * Displays a human readable summary of the preferences chosen by the user
     * on the main form
     */
    private void showPreferencesSummary() {

        try {
            TextView txtLoggingTo = (TextView) rootView.findViewById(R.id.detailedview_loggingto_text);
            TextView txtFrequency = (TextView) rootView.findViewById(R.id.detailedview_frequency_text);
            TextView txtDistance = (TextView) rootView.findViewById(R.id.detailedview_distance_text);
            TextView txtAutoEmail = (TextView) rootView.findViewById(R.id.detailedview_autosend_text);

            List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers(getActivity().getApplicationContext());

            if (loggers.size() > 0) {

                ListIterator<IFileLogger> li = loggers.listIterator();
                String logTo = li.next().getName();
                while (li.hasNext()) {
                    logTo += ", " + li.next().getName();
                }

                if (AppSettings.shouldLogToNmea()) {
                    logTo += ", NMEA";
                }

                txtLoggingTo.setText(logTo);

            } else {

                txtLoggingTo.setText(R.string.summary_loggingto_screen);

            }

            if (AppSettings.getMinimumSeconds() > 0) {
                String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(),
                        getActivity().getApplicationContext());

                txtFrequency.setText(descriptiveTime);
            } else {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            if (AppSettings.getMinimumDistanceInMeters() > 0) {
                txtDistance.setText(Utilities.GetDistanceDisplay(getActivity(), AppSettings.getMinimumDistanceInMeters(), AppSettings.shouldUseImperial()));
            } else {
                txtDistance.setText(R.string.summary_dist_regardless);
            }

            if (AppSettings.isAutoSendEnabled() && AppSettings.getAutoSendDelay() > 0) {
                String autoEmailDisplay = String.format(getString(R.string.autosend_frequency_display), AppSettings.getAutoSendDelay().intValue());

                txtAutoEmail.setText(autoEmailDisplay);
            }


            showCurrentFileName(Session.getCurrentFileName());


            TextView txtTargets = (TextView) rootView.findViewById(R.id.detailedview_autosendtargets_text);

            if(AppSettings.isAutoSendEnabled()){
                StringBuilder sb = new StringBuilder();
                if (AppSettings.isEmailAutoSendEnabled() && Utilities.IsEmailSetup()) {
                    sb.append(getString(R.string.autoemail_title)).append("\n");
                }

                if (AppSettings.isFtpAutoSendEnabled() && Utilities.IsFtpSetup()) {
                    sb.append(getString(R.string.autoftp_setup_title)).append("\n");
                }

                if (AppSettings.isGDocsAutoSendEnabled() && GDocsHelper.IsLinked(getActivity().getApplicationContext())) {
                    sb.append(getString(R.string.gdocs_setup_title)).append("\n");
                }

                if (AppSettings.isOsmAutoSendEnabled() && OSMHelper.IsOsmAuthorized(getActivity().getApplicationContext())) {
                    sb.append(getString(R.string.osm_setup_title)).append("\n");
                }

                if (AppSettings.isDropboxAutoSendEnabled() && Utilities.IsDropBoxSetup(getActivity().getApplicationContext())) {
                    sb.append(getString(R.string.dropbox_setup_title)).append("\n");
                }

                if (AppSettings.isOpenGtsAutoSendEnabled() && Utilities.IsOpenGTSSetup()) {
                    sb.append(getString(R.string.opengts_setup_title)).append("\n");
                }

                txtTargets.setText(sb.toString());
            }
            else {
                txtTargets.setText("");
            }



        } catch (Exception ex) {
            tracer.error("showPreferencesSummary", ex);
        }


    }

    public void showCurrentFileName(String newFileName) {
        if (newFileName == null || newFileName.length() <= 0) {
            return;
        }

        TextView txtFilename = (TextView) rootView.findViewById(R.id.detailedview_file_text);
        txtFilename.setText(Session.getCurrentFileName() + "\n (" + AppSettings.getGpsLoggerFolder() + ")");

        Utilities.SetFileExplorerLink(txtFilename,
                Html.fromHtml( Session.getCurrentFileName() + "<br /> (" + "<font color='blue'><u>" +  AppSettings.getGpsLoggerFolder() + "</u></font>" + ")"),
                AppSettings.getGpsLoggerFolder(),
                getActivity().getApplicationContext());
    }


    public void SetSatelliteCount(int count) {
        TextView txtSatellites = (TextView) rootView.findViewById(R.id.detailedview_satellites_text);
        txtSatellites.setText(String.valueOf(count));
    }

    private void ClearDisplay() {
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


    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.StatusMessage event){
        TextView txtStatus = (TextView) rootView.findViewById(R.id.detailedview_txtstatus);
        txtStatus.setText(event.status);
        showPreferencesSummary();
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.FatalMessage event){
        TextView txtStatus = (TextView) rootView.findViewById(R.id.detailedview_txtstatus);
        txtStatus.setText(event.message);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationEvent){
        DisplayLocationInfo(locationEvent.location);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.SatelliteCount satelliteCount){
        SetSatelliteCount(satelliteCount.satelliteCount);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        if(loggingStatus.loggingStarted){
            setActionButtonStop();
            showPreferencesSummary();
            ClearDisplay();
        }
        else {
            setActionButtonStart();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.FileNamed fileNamed){
        showCurrentFileName(fileNamed.newFileName);
    }

    public void DisplayLocationInfo(Location locationInfo){
        if (locationInfo == null) {
            return;
        }

        showPreferencesSummary();

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
        if (providerName.equalsIgnoreCase("gps")) {
            providerName = getString(R.string.providername_gps);
        } else {
            providerName = getString(R.string.providername_celltower);
        }

        tvDateTime.setText(new Date(Session.getLatestTimeStamp()).toLocaleString() + " - " + providerName);

        NumberFormat nf = NumberFormat.getInstance();


        nf.setMaximumFractionDigits(6);
        tvLatitude.setText(String.valueOf(nf.format(locationInfo.getLatitude())));
        tvLongitude.setText(String.valueOf(nf.format(locationInfo.getLongitude())));

        nf.setMaximumFractionDigits(3);

        if (locationInfo.hasAltitude()) {
            tvAltitude.setText(Utilities.GetDistanceDisplay(getActivity(), locationInfo.getAltitude(), AppSettings.shouldUseImperial()));
        } else {
            tvAltitude.setText(R.string.not_applicable);
        }

        if (locationInfo.hasSpeed()) {
            txtSpeed.setText(Utilities.GetSpeedDisplay(getActivity(), locationInfo.getSpeed(), AppSettings.shouldUseImperial()));

        } else {
            txtSpeed.setText(R.string.not_applicable);
        }

        if (locationInfo.hasBearing()) {

            float bearingDegrees = locationInfo.getBearing();
            String direction;

            direction = Utilities.GetBearingDescription(bearingDegrees, getActivity().getApplicationContext());

            txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                    + getString(R.string.degree_symbol) + ")");
        } else {
            txtDirection.setText(R.string.not_applicable);
        }

        if (!Session.isUsingGps()) {
            txtSatellites.setText(R.string.not_applicable);
        }

        if (locationInfo.hasAccuracy()) {

            float accuracy = locationInfo.getAccuracy();
            txtAccuracy.setText(getString(R.string.accuracy_within, Utilities.GetDistanceDisplay(getActivity(), accuracy, AppSettings.shouldUseImperial()), ""));

        } else {
            txtAccuracy.setText(R.string.not_applicable);
        }

        double distanceValue = Session.getTotalTravelled();
        txtTravelled.setText(Utilities.GetDistanceDisplay(getActivity(), distanceValue, AppSettings.shouldUseImperial()) + " (" + Session.getNumLegs() + " points)");

        long startTime = Session.getStartTimeStamp();
        Date d = new Date(startTime);
        long currentTime = System.currentTimeMillis();

        String duration = Utilities.GetDescriptiveTimeString((int)(currentTime - startTime)/1000, getActivity());

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext());
        txtTime.setText(duration + " (started at " + dateFormat.format(d) + " " + timeFormat.format(d) + ")");
    }

}

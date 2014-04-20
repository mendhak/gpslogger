package com.mendhak.gpslogger.views;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.IFileLogger;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import com.mendhak.gpslogger.views.component.ToggleComponent;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;


public class GpsDetailedViewFragment extends GenericViewFragment {

    private ToggleComponent toggleComponent;
    private View rootView;
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

        // Toggle the play and pause views.
        toggleComponent = ToggleComponent.getBuilder()
                .addOnView(rootView.findViewById(R.id.detailedview_play))
                .addOffView(rootView.findViewById(R.id.detailedview_stop))
                .setDefaultState(!Session.isStarted())
                .addHandler(new ToggleComponent.ToggleHandler() {
                    @Override
                    public void onStatusChange(boolean status) {
                        if (status) {
                            requestStartLogging();
                        } else {
                            requestStopLogging();
                        }
                    }
                })
                .build();

        if (Session.hasValidLocation()) {
            SetLocation(Session.getCurrentLocationInfo());
        }

        showPreferencesSummary();

        return rootView;
    }

    @Override
    public void onStart() {

        toggleComponent.SetEnabled(!Session.isStarted());
        super.onStart();
    }

    @Override
    public void onResume() {

        toggleComponent.SetEnabled(!Session.isStarted());
        showPreferencesSummary();
        super.onResume();
    }

    @Override
    public void SetLocation(Location locationInfo) {
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

        tvDateTime.setText(new Date(Session.getLatestTimeStamp()).toLocaleString()
                + "\n" + getString(R.string.providername_using, providerName));

        NumberFormat nf = NumberFormat.getInstance();


        nf.setMaximumFractionDigits(6);
        tvLatitude.setText(String.valueOf(nf.format(locationInfo.getLatitude())));
        tvLongitude.setText(String.valueOf(nf.format(locationInfo.getLongitude())));

        nf.setMaximumFractionDigits(3);

        if (locationInfo.hasAltitude()) {

            double altitude = locationInfo.getAltitude();

            if (AppSettings.shouldUseImperial()) {
                tvAltitude.setText(nf.format(Utilities.MetersToFeet(altitude))
                        + getString(R.string.feet));
            } else {
                tvAltitude.setText(nf.format(altitude) + getString(R.string.meters));
            }

        } else {
            tvAltitude.setText(R.string.not_applicable);
        }

        if (locationInfo.hasSpeed()) {

            float speed = locationInfo.getSpeed();
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

            txtSpeed.setText(String.valueOf(nf.format(speed)) + unit);

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

            if (AppSettings.shouldUseImperial()) {
                txtAccuracy.setText(getString(R.string.accuracy_within,
                        nf.format(Utilities.MetersToFeet(accuracy)), getString(R.string.feet)));

            } else {
                txtAccuracy.setText(getString(R.string.accuracy_within, nf.format(accuracy),
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

        txtTravelled.setText(nf.format(distanceValue) + " " + distanceUnit +
                " (" + Session.getNumLegs() + " points)");

        long startTime = Session.getStartTimeStamp();
        Date d = new Date(startTime);
        long currentTime = System.currentTimeMillis();
        String duration = getInterval(startTime, currentTime);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity().getApplicationContext());
        txtTime.setText(duration + " (started at " + dateFormat.format(d) + " " + timeFormat.format(d) + ")");

    }

    private String getInterval(long startTime, long endTime) {
        StringBuffer sb = new StringBuffer();
        long diff = endTime - startTime;
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        if (diffDays > 0) {
            sb.append(diffDays + " days ");
        }
        if (diffHours > 0) {
            sb.append(String.format("%02d", diffHours) + ":");
        }
        sb.append(String.format("%02d", diffMinutes) + ":");
        sb.append(String.format("%02d", diffSeconds));
        return sb.toString();
    }


    /**
     * Displays a human readable summary of the preferences chosen by the user
     * on the main form
     */
    private void showPreferencesSummary() {
        tracer.debug("GpsDetailedViewFragment.showPreferencesSummary");
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
                if (AppSettings.shouldUseImperial()) {
                    int minimumDistanceInFeet = Utilities.MetersToFeet(AppSettings.getMinimumDistanceInMeters());
                    txtDistance.setText(((minimumDistanceInFeet == 1)
                            ? getString(R.string.foot)
                            : String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
                } else {
                    txtDistance.setText(((AppSettings.getMinimumDistanceInMeters() == 1)
                            ? getString(R.string.meter)
                            : String.valueOf(AppSettings.getMinimumDistanceInMeters()) + getString(R.string.meters)));
                }
            } else {
                txtDistance.setText(R.string.summary_dist_regardless);
            }


            if (AppSettings.isAutoSendEnabled()) {
                String autoEmailResx;

                if (AppSettings.getAutoSendDelay() == 0) {
                    autoEmailResx = "autoemail_frequency_whenistop";
                } else {

                    autoEmailResx = "autoemail_frequency_"
                            + String.valueOf(AppSettings.getAutoSendDelay()).replace(".", "");
                }

                String autoEmailDesc = getString(getResources().getIdentifier(autoEmailResx, "string", getActivity().getPackageName()));

                txtAutoEmail.setText(autoEmailDesc);
            }


            showCurrentFileName(Session.getCurrentFileName());


            StringBuilder sb = new StringBuilder();
            if (Utilities.IsEmailSetup()) {
                sb.append("Email\n");
            }

            if (Utilities.IsFtpSetup()) {
                sb.append("FTP\n");
            }

            if (GDocsHelper.IsLinked(getActivity().getApplicationContext())) {
                sb.append("Google Docs\n");
            }

            if (OSMHelper.IsOsmAuthorized(getActivity().getApplicationContext())) {
                sb.append("OpenStreetMap\n");
            }

            if (Utilities.IsDropBoxSetup(getActivity().getApplicationContext())) {
                sb.append("Dropbox\n");
            }

            if (Utilities.IsOpenGTSSetup()) {
                sb.append("OpenGTS\n");
            }

            TextView txtTargets = (TextView) rootView.findViewById(R.id.detailedview_autosendtargets_text);
            txtTargets.setText(sb.toString());


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

    }


    @Override
    public void SetSatelliteCount(int count) {

        TextView txtSatellites = (TextView) rootView.findViewById(R.id.detailedview_satellites_text);
        txtSatellites.setText(String.valueOf(count));
    }

    @Override
    public void SetLoggingStarted() {
        toggleComponent.SetEnabled(false);
        showPreferencesSummary();
        ClearDisplay();
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

    @Override
    public void SetLoggingStopped() {
        toggleComponent.SetEnabled(true);
    }

    @Override
    public void SetStatusMessage(String message) {

        TextView txtStatus = (TextView) rootView.findViewById(R.id.detailedview_txtstatus);

        txtStatus.setText(message);
        showPreferencesSummary();
    }

    @Override
    public void SetFatalMessage(String message) {
        TextView txtStatus = (TextView) rootView.findViewById(R.id.detailedview_txtstatus);

        txtStatus.setText(message);
    }

    @Override
    public void OnFileNameChange(String newFileName) {
        showCurrentFileName(newFileName);
    }

    @Override
    public void OnNmeaSentence(long timestamp, String nmeaSentence) {

    }
}

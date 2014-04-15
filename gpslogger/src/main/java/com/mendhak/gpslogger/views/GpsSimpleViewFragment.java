package com.mendhak.gpslogger.views;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.views.component.ToggleComponent;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

public class GpsSimpleViewFragment extends GenericViewFragment implements View.OnClickListener {

    Context context;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsSimpleViewFragment.class.getSimpleName());


    private View rootView;
    private ToggleComponent toggleComponent;

    public GpsSimpleViewFragment() {

    }

    public static final GpsSimpleViewFragment newInstance() {

        GpsSimpleViewFragment fragment = new GpsSimpleViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: Inflates the simple layout

        rootView = inflater.inflate(R.layout.fragment_simple_view, container, false);

        setImageTooltips();


        // Toggle the play and pause.
        toggleComponent = ToggleComponent.getBuilder()
                .addOnView(rootView.findViewById(R.id.simple_play))
                .addOffView(rootView.findViewById(R.id.simple_stop))
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


        if (getActivity() != null) {
            this.context = getActivity().getApplicationContext();

        }


        return rootView;
    }

    private void setImageTooltips() {
        ImageView imgSatellites = (ImageView) rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        imgSatellites.setOnClickListener(this);

        TextView txtAccuracyIcon = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracyIcon);
        txtAccuracyIcon.setOnClickListener(this);

        ImageView imgElevation = (ImageView) rootView.findViewById(R.id.simpleview_imgAltitude);
        imgElevation.setOnClickListener(this);

        ImageView imgBearing = (ImageView) rootView.findViewById(R.id.simpleview_imgDirection);
        imgBearing.setOnClickListener(this);

        ImageView imgDuration = (ImageView) rootView.findViewById(R.id.simpleview_imgDuration);
        imgDuration.setOnClickListener(this);

        ImageView imgSpeed = (ImageView) rootView.findViewById(R.id.simpleview_imgSpeed);
        imgSpeed.setOnClickListener(this);

        ImageView imgDistance = (ImageView) rootView.findViewById(R.id.simpleview_distance);
        imgDistance.setOnClickListener(this);

        ImageView imgPoints = (ImageView) rootView.findViewById(R.id.simpleview_points);
        imgPoints.setOnClickListener(this);

    }

    @Override
    public void onStart() {

        toggleComponent.SetEnabled(!Session.isStarted());
        super.onResume();
    }

    @Override
    public void onResume() {

        toggleComponent.SetEnabled(!Session.isStarted());
        super.onResume();
    }

    @Override
    public void onPause() {

        super.onPause();
    }


    @Override
    public void SetLocation(Location locationInfo) {

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);

        EditText txtLatitude = (EditText) rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText(String.valueOf(nf.format(locationInfo.getLatitude())));

        EditText txtLongitude = (EditText) rootView.findViewById(R.id.simple_lon_text);
        txtLongitude.setText(String.valueOf(nf.format(locationInfo.getLongitude())));

        nf.setMaximumFractionDigits(3);

        if (locationInfo.hasAccuracy()) {

            TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
            float accuracy = locationInfo.getAccuracy();

            if (AppSettings.shouldUseImperial()) {
                txtAccuracy.setText(nf.format(Utilities.MetersToFeet(accuracy)) + getString(R.string.feet));

            } else {
                txtAccuracy.setText(nf.format(accuracy) + getString(R.string.meters));
            }


            if (accuracy > 500) {
                txtAccuracy.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }

            if (accuracy > 900) {
                txtAccuracy.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                txtAccuracy.setTextColor(getResources().getColor(android.R.color.black));
            }


        }

        if (locationInfo.hasAltitude()) {

            TextView txtAltitude = (TextView) rootView.findViewById(R.id.simpleview_txtAltitude);

            if (AppSettings.shouldUseImperial()) {
                txtAltitude.setText(nf.format(Utilities.MetersToFeet(locationInfo.getAltitude()))
                        + getString(R.string.feet));
            } else {
                txtAltitude.setText(nf.format(locationInfo.getAltitude()) + getString(R.string.meters));
            }


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

            TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
            txtSpeed.setText(String.valueOf(nf.format(speed)) + unit);

        }

        if (locationInfo.hasBearing()) {

            ImageView imgDirection = (ImageView) rootView.findViewById(R.id.simpleview_imgDirection);
            imgDirection.setRotation(locationInfo.getBearing());

            TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
            txtDirection.setText(String.valueOf(Math.round(locationInfo.getBearing())) + getString(R.string.degree_symbol));

        }

        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);

        long startTime = Session.getStartTimeStamp();
        long currentTime = System.currentTimeMillis();
        String duration = getInterval(startTime, currentTime);

        txtDuration.setText(duration);


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


        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        nf.setMaximumFractionDigits(1);
        txtTravelled.setText(nf.format(distanceValue) + " " + distanceUnit);
        txtPoints.setText(Session.getNumLegs() + " " +getString(R.string.points));

        String providerName = locationInfo.getProvider();
        if (!providerName.equalsIgnoreCase("gps")) {
            TextView txtSatelliteCount = (TextView) rootView.findViewById(R.id.simpleview_txtSatelliteCount);
            txtSatelliteCount.setText("-");
        }

    }

    private void clearLocationDisplay() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);

        EditText txtLatitude = (EditText) rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText("-");

        EditText txtLongitude = (EditText) rootView.findViewById(R.id.simple_lon_text);
        txtLongitude.setText("-");


        TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
        txtAccuracy.setText("-");
        txtAccuracy.setTextColor(getResources().getColor(android.R.color.black));


        TextView txtAltitude = (TextView) rootView.findViewById(R.id.simpleview_txtAltitude);
        txtAltitude.setText("-");

        TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
        txtDirection.setText("-");

        TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
        txtSpeed.setText("-");


        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);
        txtDuration.setText("-");

        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        txtPoints.setText("");
        txtTravelled.setText("");
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

    @Override
    public void SetSatelliteCount(int count) {
        TextView txtSatelliteCount = (TextView) rootView.findViewById(R.id.simpleview_txtSatelliteCount);


        AlphaAnimation fadeIn = new AlphaAnimation(0.6f, 1.0f);
        fadeIn.setDuration(1200);
        fadeIn.setFillAfter(true);
        txtSatelliteCount.startAnimation(fadeIn);
        txtSatelliteCount.setText(String.valueOf(count));
    }

    @Override
    public void SetLoggingStarted() {
        tracer.debug("GpsSimpleViewFragment.SetLoggingStarted");
        clearLocationDisplay();
        toggleComponent.SetEnabled(false);
    }

    @Override
    public void SetLoggingStopped() {
        TextView txtSatelliteCount = (TextView) rootView.findViewById(R.id.simpleview_txtSatelliteCount);
        txtSatelliteCount.setText("-");

        toggleComponent.SetEnabled(true);
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
    public void onClick(View view) {
        Toast toast = new Toast(getActivity());
        switch(view.getId()){
            case R.id.simpleview_imgSatelliteCount:
                toast = getToast(R.string.txt_satellites);
                break;
            case R.id.simpleview_txtAccuracyIcon:
                toast = getToast(R.string.txt_accuracy);
                break;

            case R.id.simpleview_imgAltitude:
                toast = getToast(R.string.txt_altitude);
                break;

            case R.id.simpleview_imgDirection:
                toast = getToast(R.string.txt_direction);
                break;

            case R.id.simpleview_imgDuration:
                toast = getToast(R.string.txt_travel_duration);
                break;

            case R.id.simpleview_imgSpeed:
                toast = getToast(R.string.txt_speed);
                break;

            case R.id.simpleview_distance:
                toast = getToast(R.string.txt_travel_distance);
                break;

            case R.id.simpleview_points:
                toast = getToast(R.string.txt_number_of_points);
                break;

        }

        int location[]=new int[2];
        view.getLocationOnScreen(location);
        toast.setGravity(Gravity.TOP|Gravity.LEFT, location[0], location[1]);
        toast.show();
    }

    private Toast getToast(int stringResourceId) {
        return Toast.makeText(getActivity(), getString(stringResourceId).replace(":",""), Toast.LENGTH_SHORT);

    }
}

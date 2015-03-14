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

import android.content.Context;
import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.dd.processbutton.iml.ActionProcessButton;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import org.slf4j.LoggerFactory;
import java.text.NumberFormat;

public class GpsSimpleViewFragment extends GenericViewFragment implements View.OnClickListener {

    Context context;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsSimpleViewFragment.class.getSimpleName());


    private View rootView;
    private ActionProcessButton actionButton;

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
        showPreferencesSummary();

        actionButton = (ActionProcessButton)rootView.findViewById(R.id.btnActionProcess);
        actionButton.setMode(ActionProcessButton.Mode.ENDLESS);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColor));

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Session.isStarted()){
                    requestStopLogging();
                }
                else {
                    requestStartLogging();
                }
            }
        });


        if (Session.hasValidLocation()) {
            SetLocation(Session.getCurrentLocationInfo());
        }


        if (getActivity() != null) {
            this.context = getActivity().getApplicationContext();

        }


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

    private void showPreferencesSummary() {
        showCurrentFileName(Session.getCurrentFileName());


        ImageView imgGpx = (ImageView) rootView.findViewById(R.id.simpleview_imgGpx);
        ImageView imgKml = (ImageView) rootView.findViewById(R.id.simpleview_imgKml);
        ImageView imgCsv = (ImageView) rootView.findViewById(R.id.simpleview_imgCsv);
        ImageView imgNmea = (ImageView) rootView.findViewById(R.id.simpleview_imgNmea);
        ImageView imgLink = (ImageView) rootView.findViewById(R.id.simpleview_imgLink);

        if (AppSettings.shouldLogToGpx()) {

            imgGpx.setVisibility(View.VISIBLE);
        } else {
            imgGpx.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToKml()) {

            imgKml.setVisibility(View.VISIBLE);
        } else {
            imgKml.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToNmea()) {
            imgNmea.setVisibility(View.VISIBLE);
        } else {
            imgNmea.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToPlainText()) {

            imgCsv.setVisibility(View.VISIBLE);
        } else {
            imgCsv.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToCustomUrl()) {
            imgLink.setVisibility(View.VISIBLE);
        } else {
            imgLink.setVisibility(View.GONE);
        }

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml()
                && !AppSettings.shouldLogToPlainText()) {
            showCurrentFileName(null);
        }

    }

    private void showCurrentFileName(String newFileName) {
        TextView txtFilename = (TextView) rootView.findViewById(R.id.simpleview_txtfilepath);
        if (newFileName == null || newFileName.length() <= 0) {
            txtFilename.setText("");
            txtFilename.setVisibility(View.INVISIBLE);
            return;
        }

        txtFilename.setVisibility(View.VISIBLE);
        txtFilename.setText(Html.fromHtml("<em>" + AppSettings.getGpsLoggerFolder() + "/<strong><br />" + Session.getCurrentFileName() + "</strong></em>"));

        Utilities.SetFileExplorerLink(txtFilename,
                Html.fromHtml("<em><font color='blue'><u>" + AppSettings.getGpsLoggerFolder() + "</u></font>" + "/<strong><br />" + Session.getCurrentFileName() + "</strong></em>" ),
                AppSettings.getGpsLoggerFolder(),
                getActivity().getApplicationContext());

    }

    private enum IconColorIndicator {
        Good,
        Warning,
        Bad,
        Inactive
    }

    private void ClearColor(ImageView imgView){
        SetColor(imgView, IconColorIndicator.Inactive);
    }

    private void SetColor(ImageView imgView, IconColorIndicator colorIndicator ){
        imgView.clearColorFilter();

        if(colorIndicator == IconColorIndicator.Inactive){
            return;
        }

        int color = -1;
        switch(colorIndicator){
            case Bad:
                color = Color.parseColor("#FFEEEE");
                break;
            case Good:
                color = getResources().getColor(R.color.accentColor);
                break;
            case Warning:
                color = Color.parseColor("#D4FFA300");
                break;
        }

        imgView.setColorFilter(color);

    }

    private void setImageTooltips() {
        ImageView imgSatellites = (ImageView) rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        imgSatellites.setOnClickListener(this);

        ImageView imgAccuracy = (ImageView) rootView.findViewById(R.id.simpleview_imgAccuracy);
        imgAccuracy.setOnClickListener(this);

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

        ImageView imgLink = (ImageView) rootView.findViewById(R.id.simpleview_imgLink);
        imgLink.setOnClickListener(this);

    }

    @Override
    public void onStart() {

        setActionButtonStop();
        super.onStart();
    }

    @Override
    public void onResume() {
        showPreferencesSummary();

        if(Session.isStarted()){
            setActionButtonStop();
        }
        else {
            setActionButtonStart();
        }
        super.onResume();
    }

    @Override
    public void onPause() {

        super.onPause();
    }


    @Override
    public void SetLocation(Location locationInfo) {

        showPreferencesSummary();

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);

        EditText txtLatitude = (EditText) rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText(String.valueOf(nf.format(locationInfo.getLatitude())) + ", " + String.valueOf(nf.format(locationInfo.getLongitude())));


        nf.setMaximumFractionDigits(3);


        ImageView imgAccuracy = (ImageView) rootView.findViewById(R.id.simpleview_imgAccuracy);
        ClearColor(imgAccuracy);

        if (locationInfo.hasAccuracy()) {

            TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
            float accuracy = locationInfo.getAccuracy();
            txtAccuracy.setText(Utilities.GetDistanceDisplay(getActivity(), accuracy, AppSettings.shouldUseImperial()));

            if (accuracy > 500) {
                SetColor(imgAccuracy, IconColorIndicator.Warning);
            }

            if (accuracy > 900) {
                SetColor(imgAccuracy, IconColorIndicator.Bad);
            } else {
                SetColor(imgAccuracy, IconColorIndicator.Good);
            }
        }

        ImageView imgAltitude = (ImageView)rootView.findViewById(R.id.simpleview_imgAltitude);
        ClearColor(imgAltitude);

        if (locationInfo.hasAltitude()) {
            SetColor(imgAltitude, IconColorIndicator.Good);
            TextView txtAltitude = (TextView) rootView.findViewById(R.id.simpleview_txtAltitude);

            txtAltitude.setText(Utilities.GetDistanceDisplay(getActivity(), locationInfo.getAltitude(), AppSettings.shouldUseImperial()));
        }

        ImageView imgSpeed = (ImageView)rootView.findViewById(R.id.simpleview_imgSpeed);
        ClearColor(imgSpeed);

        if (locationInfo.hasSpeed()) {

            SetColor(imgSpeed, IconColorIndicator.Good);

            TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
            txtSpeed.setText(Utilities.GetSpeedDisplay(getActivity(),locationInfo.getSpeed(),AppSettings.shouldUseImperial()));
        }

        ImageView imgDirection = (ImageView) rootView.findViewById(R.id.simpleview_imgDirection);
        ClearColor(imgDirection);

        if (locationInfo.hasBearing()) {
            SetColor(imgDirection, IconColorIndicator.Good);
            imgDirection.setRotation(locationInfo.getBearing());

            TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
            txtDirection.setText(String.valueOf(Math.round(locationInfo.getBearing())) + getString(R.string.degree_symbol));
        }

        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);

        long startTime = Session.getStartTimeStamp();
        long currentTime = System.currentTimeMillis();

        txtDuration.setText(Utilities.GetTimeDisplay(getActivity(), currentTime-startTime));

        double distanceValue = Session.getTotalTravelled();

        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        txtTravelled.setText(Utilities.GetDistanceDisplay(getActivity(), distanceValue, AppSettings.shouldUseImperial()));
        txtPoints.setText(Session.getNumLegs() + " " + getString(R.string.points));

        String providerName = locationInfo.getProvider();
        if (!providerName.equalsIgnoreCase("gps")) {
            SetSatelliteCount(-1);
        }

    }

    private void clearLocationDisplay() {

        EditText txtLatitude = (EditText) rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText("");

        ImageView imgAccuracy = (ImageView)rootView.findViewById(R.id.simpleview_imgAccuracy);
        ClearColor(imgAccuracy);

        TextView txtAccuracy = (TextView) rootView.findViewById(R.id.simpleview_txtAccuracy);
        txtAccuracy.setText("");
        txtAccuracy.setTextColor(getResources().getColor(android.R.color.black));

        ImageView imgAltitude = (ImageView)rootView.findViewById(R.id.simpleview_imgAltitude);
        ClearColor(imgAltitude);

        TextView txtAltitude = (TextView) rootView.findViewById(R.id.simpleview_txtAltitude);
        txtAltitude.setText("");

        ImageView imgDirection = (ImageView)rootView.findViewById(R.id.simpleview_imgDirection);
        ClearColor(imgDirection);

        TextView txtDirection = (TextView) rootView.findViewById(R.id.simpleview_txtDirection);
        txtDirection.setText("");

        ImageView imgSpeed = (ImageView)rootView.findViewById(R.id.simpleview_imgSpeed);
        ClearColor(imgSpeed);

        TextView txtSpeed = (TextView) rootView.findViewById(R.id.simpleview_txtSpeed);
        txtSpeed.setText("");


        TextView txtDuration = (TextView) rootView.findViewById(R.id.simpleview_txtDuration);
        txtDuration.setText("");

        TextView txtPoints = (TextView) rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = (TextView) rootView.findViewById(R.id.simpleview_txtDistance);

        txtPoints.setText("");
        txtTravelled.setText("");
    }



    @Override
    public void SetSatelliteCount(int count) {
        ImageView imgSatelliteCount = (ImageView) rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        TextView txtSatelliteCount = (TextView) rootView.findViewById(R.id.simpleview_txtSatelliteCount);

        if(count > -1) {
            SetColor(imgSatelliteCount, IconColorIndicator.Good);

            AlphaAnimation fadeIn = new AlphaAnimation(0.6f, 1.0f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);
            txtSatelliteCount.startAnimation(fadeIn);
            txtSatelliteCount.setText(String.valueOf(count));
        }
        else {
            ClearColor(imgSatelliteCount);
            txtSatelliteCount.setText("");
        }

    }

    @Override
    public void SetLoggingStarted() {
        tracer.debug("GpsSimpleViewFragment.SetLoggingStarted");
        showPreferencesSummary();
        clearLocationDisplay();

        tracer.debug(".");
        setActionButtonStop();
    }

    @Override
    public void SetLoggingStopped() {

        tracer.debug(".");
        SetSatelliteCount(-1);

        setActionButtonStart();
    }

    @Override
    public void OnWaitingForLocation(boolean inProgress) {

        tracer.debug(inProgress + "");

        if(!Session.isStarted()){
            actionButton.setProgress(0);
            setActionButtonStart();
            return;
        }

        if(inProgress){
            actionButton.setProgress(1);
            setActionButtonStop();
        }
        else {
            actionButton.setProgress(0);
            setActionButtonStop();
        }
    }


    @Override
    public void OnFileNameChange(String newFileName) {
        showCurrentFileName(newFileName);
    }

    @Override
    public void OnNmeaSentence(long timestamp, String nmeaSentence) {

    }

    @Override
    public void onClick(View view) {
        Toast toast = new Toast(getActivity());
        switch (view.getId()) {
            case R.id.simpleview_imgSatelliteCount:
                toast = getToast(R.string.txt_satellites);
                break;
            case R.id.simpleview_imgAccuracy:
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

            case R.id.simpleview_imgLink:
                toast = getToast(AppSettings.getCustomLoggingUrl());
                break;

        }

        int location[] = new int[2];
        view.getLocationOnScreen(location);
        toast.setGravity(Gravity.TOP | Gravity.LEFT, location[0], location[1]);
        toast.show();
    }

    private Toast getToast(String message) {
        return Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
    }

    private Toast getToast(int stringResourceId) {
        return getToast(getString(stringResourceId).replace(":", ""));
    }
}

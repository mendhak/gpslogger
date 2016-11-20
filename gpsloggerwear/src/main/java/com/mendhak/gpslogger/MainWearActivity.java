package com.mendhak.gpslogger;

import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.*;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainWearActivity extends WearableActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient googleClient;
    private ImageView img;
    private boolean sessionStarted;

    private BoxInsetLayout mContainerView;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
//        setAmbientEnabled();

        // Build a new GoogleApiClient for the Wearable API
        googleClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        img = (ImageView)findViewById(R.id.btnStartStop);
        img.setOnClickListener(this);
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("GPSLOGGER", "Connecting to listener client");
        googleClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

//    @Override
//    public void onEnterAmbient(Bundle ambientDetails) {
//        super.onEnterAmbient(ambientDetails);
//        updateDisplay();
//    }
//
//    @Override
//    public void onUpdateAmbient() {
//        super.onUpdateAmbient();
//        updateDisplay();
//    }
//
//    @Override
//    public void onExitAmbient() {
//        updateDisplay();
//        super.onExitAmbient();
//    }
//
//    private void updateDisplay() {
//        if (isAmbient()) {
//            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
//            mClockView.setVisibility(View.VISIBLE);
//
//            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
//        } else {
//            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
//            mClockView.setVisibility(View.GONE);
//        }
//    }



    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {

        Log.v("GPSLOGGER", "OnConnected");
        Wearable.DataApi.addListener(googleClient, this);

        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/get_status", "").start();
    }



    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/latest_gps") == 0) {
                    final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    handler.post(new UpdateUI(dataMap));
                }
            }
        }

    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.btnStartStop){
            new SendToDataLayerThread("/start_stop", "").start();

            if(!sessionStarted){
                sessionStarted = true;
                //clear the text while waiting for new location update
                imageViewAnimatedChange("STARTED");
                new UpdateUI(null).run();
            }
            else {
                sessionStarted = false;
                //Stop but keep text on screen
                imageViewAnimatedChange("STOPPED");
            }
        }
    }


    private void imageViewAnimatedChange( String buttonState ) {

        final ImageView v =  (ImageView)findViewById(R.id.btnStartStop);
        final int new_image = (buttonState.equals("STOPPED")) ? android.R.drawable.ic_media_play: android.R.drawable.ic_menu_close_clear_cancel;

        final Animation anim_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageResource(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }


    private void textViewAnimatedChange(final TextView v, final String new_text) {
        final Animation anim_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        anim_out.setDuration(500);
        final Animation anim_in  = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        anim_in.setDuration(20);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setText(new_text);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }
    private class UpdateUI implements Runnable {

        private DataMap dataMap;
        public UpdateUI(DataMap dataMap){
            this.dataMap = dataMap;
        }

        @Override
        public void run() {

            TextView txtLatitude = (TextView) findViewById(R.id.txtLatitude);
            TextView txtLongitude = (TextView)findViewById(R.id.txtLongitude);
            TextView txtFixTime = (TextView)findViewById(R.id.txtFixTime);

            if(dataMap == null){
                txtLatitude.setText("");
                txtLongitude.setText("");
                txtFixTime.setText("");
                return;
            }

            if(dataMap.containsKey("latitude")){
                textViewAnimatedChange(txtLatitude, dataMap.getString("latitude"));
            }

            if(dataMap.containsKey("longitude")){
                textViewAnimatedChange(txtLongitude, dataMap.getString("longitude"));
            }

            if(dataMap.containsKey("fixtime")){

                String dateString = new SimpleDateFormat("HH:mm:ss").format(new Date(dataMap.getLong("fixtime")));
                textViewAnimatedChange(txtFixTime, "(@" + dateString + ")");
            }

            sessionStarted = dataMap.getBoolean("session", false);
            img.setImageResource(android.R.drawable.ic_media_play);
            if(sessionStarted){
                img.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            }

        }
    }


    private class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String path, String msg) {
            this.path = path;
            this.message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("GPSLOGGER", "Request: {" + path+ "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }
}

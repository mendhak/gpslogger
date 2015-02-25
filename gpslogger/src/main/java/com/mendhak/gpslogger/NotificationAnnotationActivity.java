package com.mendhak.gpslogger;

import android.app.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class NotificationAnnotationActivity extends Activity {

    //Called from the 'Annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tracer = LoggerFactory.getLogger(GpsLoggingService.class.getSimpleName());

        MaterialDialog alertDialog = new MaterialDialog.Builder(this)
                .title(R.string.add_description)
                .customView(R.layout.alertview, false)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        finish();
                    }

                    @Override
                    public void onPositive(MaterialDialog dialog) {

                        EditText userInput = (EditText) dialog.getCustomView().findViewById(R.id.alert_user_input);
                        tracer.info("Notification annotation: " + userInput.getText().toString());

                        Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
                        serviceIntent.putExtra("setnextpointdescription", userInput.getText().toString());
                        getApplicationContext().startService(serviceIntent);

                        finish();
                    }
                }).cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                }).build();
        TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
        tvMessage.setText(R.string.letters_numbers);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            super.finish();
        }

        return super.onKeyDown(keyCode, event);
    }
}

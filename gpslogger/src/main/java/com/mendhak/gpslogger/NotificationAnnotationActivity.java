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

package com.mendhak.gpslogger;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.common.events.CommandEvents;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

public class NotificationAnnotationActivity extends Activity {

    //Called from the 'Annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tracer = LoggerFactory.getLogger(NotificationAnnotationActivity.class.getSimpleName());

        MaterialDialog alertDialog = new MaterialDialog.Builder(this)
                .title(R.string.add_description)
                .customView(R.layout.alertview, false)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        finish();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        EditText userInput = (EditText) materialDialog.getCustomView().findViewById(R.id.alert_user_input);
                        tracer.info("Annotation from notification: " + userInput.getText().toString());

                        EventBus.getDefault().postSticky(new CommandEvents.Annotate(userInput.getText().toString()));

                        Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
                        getApplicationContext().startService(serviceIntent);

                        finish();
                    }
                })
               .cancelListener(new DialogInterface.OnCancelListener() {
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

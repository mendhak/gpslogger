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

package com.mendhak.gpslogger;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.KeyEvent;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;

public class NotificationAnnotationActivity extends AppCompatActivity {

    //Called from the 'annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private static final Logger LOG = Logs.of(NotificationAnnotationActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MaterialDialog.Builder(this)
                .title(R.string.add_description)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(R.string.cancel)
                .autoDismiss(true)
                .canceledOnTouchOutside(true)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .input(getString(R.string.letters_numbers), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog materialDialog, @NonNull CharSequence input) {

                        LOG.info("Annotation from notification: " + input.toString());
                        Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
                        serviceIntent.putExtra(IntentConstants.SET_DESCRIPTION, input.toString());
                        ContextCompat.startForegroundService(getApplicationContext(),  serviceIntent);
                        materialDialog.dismiss();
                        finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.hide();
                        materialDialog.dismiss();
                        finish();
                    }
                })
                .show();
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

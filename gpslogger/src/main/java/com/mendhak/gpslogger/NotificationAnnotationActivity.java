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

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.ui.Dialogs;
import org.slf4j.Logger;

import eltos.simpledialogfragment.SimpleDialog;

public class NotificationAnnotationActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    //Called from the 'annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private static final Logger LOG = Logs.of(NotificationAnnotationActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        Dialogs.autoSuggestDialog(NotificationAnnotationActivity.this, "annotations",
                getString(R.string.add_description), getString(R.string.letters_numbers), "");

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

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
         if(dialogTag.equalsIgnoreCase("annotations") && which == BUTTON_POSITIVE){
            String enteredText = extras.getString("annotations");
            //Replace all whitespace and newlines, with single space
            enteredText = enteredText.replaceAll("\\s+"," ");
            LOG.info("Notification Annotation entered : " + enteredText);
            Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
            serviceIntent.putExtra(IntentConstants.SET_DESCRIPTION, enteredText);
            ContextCompat.startForegroundService(getApplicationContext(),  serviceIntent);
            finish();
            return true;
        }

        return false;
    }
}

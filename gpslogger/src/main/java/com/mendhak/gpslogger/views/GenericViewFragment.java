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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 */
public abstract class GenericViewFragment extends PermissionedFragment  {

    private org.slf4j.Logger tracer;
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracer = LoggerFactory.getLogger(GenericViewFragment.class.getSimpleName());
        registerEventBus();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationServicesUnavailable locationServicesUnavailable) {
        new MaterialDialog.Builder(getActivity())
                //.title("Location services unavailable")
                .content(R.string.gpsprovider_unavailable)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getActivity().startActivity(settingsIntent);
                    }
                })
                .show();
    }


    @AskPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void requestToggleLogging() {

        if (Session.isStarted()) {
            toggleLogging();
            return;
        }

        if (preferenceHelper.shouldCreateCustomFile() && preferenceHelper.shouldAskCustomFileNameEachTime()) {

            MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.alertview, true)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            String chosenFileName = preferenceHelper.getCustomFileName();
                            EditText userInput = (EditText) materialDialog.getCustomView().findViewById(R.id.alert_user_input);

                            if (!Utilities.IsNullOrEmpty(userInput.getText().toString()) && !userInput.getText().toString().equalsIgnoreCase(chosenFileName)) {
                                preferenceHelper.setCustomFileName(userInput.getText().toString());
                            }
                            toggleLogging();
                        }
                    })
                    .keyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                toggleLogging();
                                dialog.dismiss();
                            }
                            return true;
                        }
                    })
                    .build();

            EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
            userInput.setText(preferenceHelper.getCustomFileName());
            TextView tvMessage = (TextView) alertDialog.getCustomView().findViewById(R.id.alert_user_message);
            tvMessage.setText(R.string.new_file_custom_message);
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();

        } else {
            toggleLogging();
        }
    }

    public void toggleLogging() {
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }

}

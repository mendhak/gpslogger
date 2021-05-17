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

package com.mendhak.gpslogger.ui.fragments.display;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 */
public abstract class GenericViewFragment extends Fragment {

    private static final Logger LOG = Logs.of(GenericViewFragment.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private Session session = Session.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                        if(getActivity() != null){
                            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getActivity().startActivity(settingsIntent);
                        }

                    }
                })
                .show();
    }


    public void requestToggleLogging() {

        if(!Systems.locationPermissionsGranted(getActivity())){
            Dialogs.alert(getString(R.string.gpslogger_permissions_rationale_title),
                    getString(R.string.gpslogger_permissions_permanently_denied), getActivity());
            return;
        }

        if (session.isStarted()) {
            toggleLogging();
            return;
        }

        if(! Files.isAllowedToWriteTo(preferenceHelper.getGpsLoggerFolder())){
            Dialogs.alert(getString(R.string.error),getString(R.string.pref_logging_file_no_permissions) + "<br />" + preferenceHelper.getGpsLoggerFolder(), getActivity());
            return;
        }

        if (preferenceHelper.shouldCreateCustomFile() && preferenceHelper.shouldAskCustomFileNameEachTime()) {

            Dialogs.autoCompleteText(getActivity(), "customfilename",
                    getString(R.string.new_file_custom_title), "gpslogger",
                    preferenceHelper.getCustomFileName(), new Dialogs.AutoCompleteCallback() {

                        @Override
                        public void messageBoxResult(int which, MaterialDialog dialog, String enteredText) {

                            if(which == Dialogs.AutoCompleteCallback.CANCEL){
                                return;
                            }

                            String originalFileName = preferenceHelper.getCustomFileName();

                            if(!originalFileName.equalsIgnoreCase(enteredText)){
                                preferenceHelper.setCustomFileName(enteredText);
                            }

                            toggleLogging();
                        }
                    });


        } else {
            if(!session.isStarted()){
                Intent serviceIntent = new Intent(getActivity().getApplicationContext(), GpsLoggingService.class);
                ContextCompat.startForegroundService(getActivity().getApplicationContext(), serviceIntent);
            }
            toggleLogging();
        }
    }

    public void toggleLogging() {
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }

}

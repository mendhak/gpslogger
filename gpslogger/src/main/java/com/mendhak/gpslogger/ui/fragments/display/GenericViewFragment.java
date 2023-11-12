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

import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;


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
        //Literally keeping this here only because the eventbus requries at least one onEvent method in this class.
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

        ArrayList<FormElement> formElements = new ArrayList<>();

        //If the user needs to be prompted about custom file name, build some form elements for it.
        if (preferenceHelper.shouldCreateCustomFile() && preferenceHelper.shouldAskCustomFileNameEachTime()) {
            formElements.add(Hint.plain(R.string.new_file_custom_title));
            formElements.add(Hint.plain(R.string.new_file_custom_message));

            final List<String> cachedList = Files.getListFromCacheFile(PreferenceNames.CUSTOM_FILE_NAME, getActivity());
            formElements.add(Input.plain(PreferenceNames.CUSTOM_FILE_NAME)
                    .required()
                    // Don't allow *, &, %, / or \ in the file name.
                    .validatePattern("^[^*/\\\\]+$", "Invalid file name")
                    .suggest(new ArrayList<>(cachedList))
                    .text(preferenceHelper.getCustomFileName())
            );

        }

        //If the user needs to be prompted about OpenStreetMap settings, build some form elements for it.
        if(preferenceHelper.isAutoSendEnabled()
                && preferenceHelper.isOsmAutoSendEnabled()
                && FileSenderFactory.getOsmSender().isAutoSendAvailable()){
            formElements.add(Hint.plain(R.string.osm_setup_title));
            formElements.addAll(Dialogs.getOpenStreetMapFormElementsForDialog(preferenceHelper));
        }

        //If the user needs to be prompted about any of the above, it's time to show a dialog
        //The result is handled in GPSMainactivity onResult.
        if(formElements.size() > 0){

            SimpleFormDialog.build()
                    .fields(formElements.toArray(new FormElement[0]))
                    .pos(R.string.btn_start_logging)
                    .show(getActivity(), "shouldpromptbeforelogging");
        }
        //Else it's just normal logging.
        else {
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

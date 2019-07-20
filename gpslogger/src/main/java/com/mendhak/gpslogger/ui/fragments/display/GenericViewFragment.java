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
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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


            final List<String> cachedList = Files.getListFromCacheFile("customfilename", getActivity());
            final LinkedHashSet<String> set = new LinkedHashSet(cachedList);

            final MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.custom_autocomplete_view, true)
                    .negativeText(R.string.cancel)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            AutoCompleteTextView autoComplete = materialDialog.getCustomView().findViewById(R.id.custom_autocomplete);
                            String originalFileName = preferenceHelper.getCustomFileName();
                            String selectedFileName = autoComplete.getText().toString();

                            if (!Strings.isNullOrEmpty(selectedFileName) && !selectedFileName.equalsIgnoreCase(originalFileName)) {

                                if(set.add(selectedFileName)){
                                    Files.saveListToCacheFile(new ArrayList<>(set), "customfilename", getActivity());
                                }

                                preferenceHelper.setCustomFileName(selectedFileName);
                            }
                            toggleLogging();
                            materialDialog.dismiss();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .build();


            String[] arr = set.toArray(new String[set.size()]);



            final AutoCompleteTextView customFileName = (AutoCompleteTextView) alertDialog.getCustomView().findViewById(R.id.custom_autocomplete);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, arr);
            customFileName.setAdapter(adapter);
            customFileName.setHint("gpslogger");
            customFileName.append(preferenceHelper.getCustomFileName());

            customFileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        alertDialog.getActionButton(DialogAction.POSITIVE).callOnClick();

                    }
                    return false;
                }
            });

            customFileName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        customFileName.showDropDown();
                        customFileName.requestFocus();
                    }
                }
            });

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

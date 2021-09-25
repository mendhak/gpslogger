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

//https://www.dropbox.com/developers/start/setup#android

package com.mendhak.gpslogger.ui.fragments.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.dropbox.DropBoxManager;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;
import java.io.File;


public class DropboxAuthorizationFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(DropboxAuthorizationFragment.class);
    DropBoxManager manager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference pref = findPreference("dropbox_resetauth");

        manager = new DropBoxManager(PreferenceHelper.getInstance());

        if (manager.isLinked()) {
            pref.setTitle(R.string.osm_resetauth);
            pref.setSummary(R.string.dropbox_unauthorize_description);
        } else {
            pref.setTitle(R.string.osm_lbl_authorize);
            pref.setSummary(R.string.dropbox_authorize_description);
        }

        pref.setOnPreferenceClickListener(this);
        findPreference("dropbox_test_upload").setOnPreferenceClickListener(this);
        registerEventBus();

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dropboxsettings, rootKey);
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            if (manager.finishAuthorization()) {
                startActivity(new Intent(getActivity(), GpsMainActivity.class));
                getActivity().finish();
            }
        } catch (Exception e) {
            Dialogs.alert(getString(R.string.error), getString(R.string.dropbox_couldnotauthorize),
                    getActivity());
            LOG.error(".", e);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase("dropbox_test_upload")){
            uploadTestFile();
        }

        else {
            // This logs you out if you're logged in, or vice versa
            if (manager.isLinked()) {
                manager.unLink();
                startActivity(new Intent(getActivity(), GpsMainActivity.class));
                getActivity().finish();
            } else {
                try {
                    manager.startAuthentication(this.getActivity());
                } catch (Exception e) {
                    LOG.error(".", e);
                }
            }
        }

        return true;

    }

    private void uploadTestFile() {
        Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.please_wait));

        try {
            File testFile = Files.createTestFile();
            manager.uploadFile(testFile.getName());

        } catch (Exception ex) {
            LOG.error("Could not create local test file", ex);
            EventBus.getDefault().post(new UploadEvents.Dropbox().failed("Could not create local test file", ex));
        }

    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Dropbox d){
        LOG.debug("Dropbox Event completed, success: " + d.success);
        Dialogs.hideProgress();
        if(!d.success){
            Dialogs.showError(getString(R.string.sorry), "Could not upload to Dropbox", d.message, d.throwable,(FragmentActivity) getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), "", getActivity());
        }
    }
}

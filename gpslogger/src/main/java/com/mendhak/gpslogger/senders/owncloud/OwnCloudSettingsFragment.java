package com.mendhak.gpslogger.senders.owncloud;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.views.component.CustomSwitchPreference;

import de.greenrobot.event.EventBus;

import org.slf4j.LoggerFactory;

public class OwnCloudSettingsFragment
        extends PreferenceValidationFragment implements Preference.OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudSettingsFragment.class.getSimpleName());

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.owncloudsettings);

        Preference testOwnCloud = findPreference("owncloud_test");
        testOwnCloud.setOnPreferenceClickListener(this);
        RegisterEventBus();
    }

    @Override
    public void onDestroy() {

        UnregisterEventBus();
        super.onDestroy();
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void UnregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public boolean IsValid() {
        CustomSwitchPreference chkEnabled = (CustomSwitchPreference) findPreference("owncloud_enabled");
        MaterialEditTextPreference txtServer = (MaterialEditTextPreference) findPreference("owncloud_server");
        MaterialEditTextPreference txtUserName = (MaterialEditTextPreference) findPreference("owncloud_username");
        MaterialEditTextPreference txtPort = (MaterialEditTextPreference) findPreference("owncloud_port");
        return !chkEnabled.isChecked() || txtServer.getText() != null
                && txtServer.getText().length() > 0 && txtUserName.getText() != null
                && txtUserName.getText().length() > 0 && txtPort.getText() != null
                && txtPort.getText().length() > 0;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        OwnCloudHelper helper = new OwnCloudHelper();

        MaterialEditTextPreference servernamePreference = (MaterialEditTextPreference) findPreference("owncloud_server");
        MaterialEditTextPreference usernamePreference = (MaterialEditTextPreference) findPreference("owncloud_username");
        MaterialEditTextPreference passwordPreference = (MaterialEditTextPreference) findPreference("owncloud_password");
        MaterialEditTextPreference portPreference = (MaterialEditTextPreference) findPreference("owncloud_port");
        CustomSwitchPreference useHttpsPreference = (CustomSwitchPreference) findPreference("owncloud_usehttps");
        MaterialEditTextPreference directoryPreference = (MaterialEditTextPreference) findPreference("owncloud_directory");

        if (!helper.ValidSettings(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                Integer.valueOf(portPreference.getText()), useHttpsPreference.isChecked(), directoryPreference.getText())) {
            Utilities.MsgBox(getString(R.string.owncloud_invalid_settings),
                    getString(R.string.owncloud_invalid_summary),
                    getActivity());
            return false;
        }

        Utilities.ShowProgress(getActivity(), getString(R.string.owncloud_testing),
                getString(R.string.please_wait));


        helper.TestOwnCloud(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                directoryPreference.getText(), Integer.valueOf(portPreference.getText()), useHttpsPreference.isChecked());

        return true;
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OwnCloud o){
        tracer.debug("OwnCloud Event completed, success: " + o.success);
        Utilities.HideProgress();
        if(!o.success){
            Utilities.MsgBox(getString(R.string.sorry), "OwnCloud Test Failed", getActivity());
        }
        else {
            Utilities.MsgBox(getString(R.string.success), "OwnCloud Test Succeeded", getActivity());
        }
    }
}


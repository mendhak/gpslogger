package com.mendhak.gpslogger.senders.owncloud;


import android.Manifest;
import android.os.Bundle;
import android.preference.Preference;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.canelmas.let.AskPermission;
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
        return !chkEnabled.isChecked() || (
                txtServer.getText() != null && txtServer.getText().length() > 0 &&
                txtUserName.getText() != null && txtUserName.getText().length() > 0
        );
    }


    @Override
    @AskPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {

        MaterialEditTextPreference servernamePreference = (MaterialEditTextPreference) findPreference("owncloud_server");
        MaterialEditTextPreference usernamePreference = (MaterialEditTextPreference) findPreference("owncloud_username");
        MaterialEditTextPreference passwordPreference = (MaterialEditTextPreference) findPreference("owncloud_password");
        MaterialEditTextPreference directoryPreference = (MaterialEditTextPreference) findPreference("owncloud_directory");

        if (!OwnCloudHelper.ValidSettings(
                servernamePreference.getText(),
                usernamePreference.getText(),
                passwordPreference.getText(),
                directoryPreference.getText())) {
            Utilities.MsgBox(getString(R.string.owncloud_invalid_settings),
                    getString(R.string.owncloud_invalid_summary),
                    getActivity());
            return false;
        }

        Utilities.ShowProgress(getActivity(), getString(R.string.owncloud_testing), getString(R.string.please_wait));
        OwnCloudHelper helper = new OwnCloudHelper();
        helper.TestOwnCloud(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                directoryPreference.getText());

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


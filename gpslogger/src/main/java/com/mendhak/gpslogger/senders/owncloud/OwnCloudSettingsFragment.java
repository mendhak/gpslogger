package com.mendhak.gpslogger.senders.owncloud;


import android.Manifest;
import android.os.Bundle;
import android.preference.Preference;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.CustomSwitchPreference;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import com.mendhak.gpslogger.ui.fragments.settings.OwnCloudManager;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

public class OwnCloudSettingsFragment
        extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator {

    private static final Logger LOG = Logs.of(OwnCloudSettingsFragment.class);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.owncloudsettings);

        Preference testOwnCloud = findPreference("owncloud_test");
        testOwnCloud.setOnPreferenceClickListener(this);
        registerEventBus();
    }

    @Override
    public void onDestroy() {

        unregisterEventBus();
        super.onDestroy();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public boolean isValid() {
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

        if (!OwnCloudManager.validSettings(
                servernamePreference.getText(),
                usernamePreference.getText(),
                passwordPreference.getText(),
                directoryPreference.getText())) {
            Dialogs.alert(getString(R.string.owncloud_invalid_settings),
                    getString(R.string.owncloud_invalid_summary),
                    getActivity());
            return false;
        }

        Dialogs.progress(getActivity(), getString(R.string.owncloud_testing), getString(R.string.please_wait));
        OwnCloudManager helper = new OwnCloudManager(PreferenceHelper.getInstance());
        helper.testOwnCloud(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                directoryPreference.getText());

        return true;
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OwnCloud o){
        LOG.debug("OwnCloud Event completed, success: " + o.success);

        Dialogs.hideProgress();
        if(!o.success){
            Dialogs.error(getString(R.string.sorry), "OwnCloud Test Failed", o.message, o.throwable, getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), "OwnCloud Test Succeeded", getActivity());
        }
    }
}


/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.ui.fragments.settings;

import android.Manifest;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.ftp.FtpManager;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.CustomSwitchPreference;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

public class FtpFragment
        extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator {
    private static final Logger LOG = Logs.of(FtpFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.autoftpsettings);

        Preference testFtp = findPreference("autoftp_test");
        testFtp.setOnPreferenceClickListener(this);
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
    @AskPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {

        FtpManager helper = new FtpManager(preferenceHelper);

        MaterialEditTextPreference servernamePreference = (MaterialEditTextPreference) findPreference("autoftp_server");
        MaterialEditTextPreference usernamePreference = (MaterialEditTextPreference) findPreference("autoftp_username");
        MaterialEditTextPreference passwordPreference = (MaterialEditTextPreference) findPreference("autoftp_password");
        MaterialEditTextPreference portPreference = (MaterialEditTextPreference) findPreference("autoftp_port");
        CustomSwitchPreference useFtpsPreference = (CustomSwitchPreference) findPreference("autoftp_useftps");
        MaterialListPreference sslTlsPreference = (MaterialListPreference) findPreference("autoftp_ssltls");
        CustomSwitchPreference implicitPreference = (CustomSwitchPreference) findPreference("autoftp_implicit");
        MaterialEditTextPreference directoryPreference = (MaterialEditTextPreference) findPreference("autoftp_directory");

        if (!helper.validSettings(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                Strings.toInt(portPreference.getText(), 21),
                 useFtpsPreference.isChecked(), sslTlsPreference.getValue(),
                implicitPreference.isChecked())) {
            Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                    getString(R.string.autoftp_invalid_summary),
                    getActivity());
            return false;
        }

        Dialogs.progress(getActivity(), getString(R.string.autoftp_testing),
                getString(R.string.please_wait));


        helper.testFtp(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                directoryPreference.getText(), Strings.toInt(portPreference.getText(), 21), useFtpsPreference.isChecked(),
                sslTlsPreference.getValue(), implicitPreference.isChecked());

        return true;
    }


    @Override
    public boolean isValid() {
        FtpManager manager = new FtpManager(preferenceHelper);

        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp o){
            LOG.debug("FTP Event completed, success: " + o.success);
            Dialogs.hideProgress();
            if(!o.success){
                String ftpMessages = (o.ftpMessages == null) ? "" : TextUtils.join("",o.ftpMessages);
                Dialogs.error(getString(R.string.sorry), "FTP Test Failed", o.message + "\r\n" + ftpMessages, o.throwable, getActivity());
            }
            else {
                Dialogs.alert(getString(R.string.success), "FTP Test Succeeded", getActivity());
            }
    }
}
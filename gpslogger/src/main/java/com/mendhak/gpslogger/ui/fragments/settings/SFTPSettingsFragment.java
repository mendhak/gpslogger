package com.mendhak.gpslogger.ui.fragments.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.core.content.ContextCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.sftp.SFTPManager;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;

public class SFTPSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator, Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(SFTPSettingsFragment.class);
    SFTPManager manager;
    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("on create");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sftpsettings);

        manager = new SFTPManager(preferenceHelper);

        findPreference("sftp_validateserver").setOnPreferenceClickListener(this);
        findPreference("sftp_reset_authorisation").setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setOnPreferenceChangeListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setSummary(preferenceHelper.getSFTPPrivateKeyFilePath());
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
    public boolean onPreferenceClick(Preference preference) {


        if(preference.getKey().equals("sftp_validateserver")) {
            uploadTestFile();
        }
        else if (preference.getKey().equals("sftp_reset_authorisation")){
            preferenceHelper.setSFTPKnownHostKey("");
            preferenceHelper.setSFTPPrivateKeyFilePath("");
            getActivity().finish();
        }

        return false;
    }

    private void uploadTestFile() {
        Dialogs.progress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));

        File testFile = null;
        try {
            testFile = Files.createTestFile();
        } catch (Exception ex) {
            LOG.error("Could not create local test file", ex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed("Could not create local test file", ex));
        }

        manager.uploadFile(testFile);
    }

    @Override
    public boolean isValid() {
        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(final UploadEvents.SFTP o){
        LOG.debug("SFTP Event completed, success: " + o.success);

        Dialogs.hideProgress();
        if(!o.success){
            if( !Strings.isNullOrEmpty(o.hostKey)){
                LOG.debug("SFTP HostKey " + o.hostKey);
                LOG.debug("SFTP Fingerprint " + o.fingerprint);
                String codeGreen = Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.accentColorComplementary)).substring(2);
                String promptMessage = String.format("Fingerprint: <br /><font color='#%s' face='monospace'>%s</font> <br /><br /> Host Key: <br /><font color='#%s' face='monospace'>%s</font>",
                                                        codeGreen, o.fingerprint, codeGreen, o.hostKey);

                Dialogs.alert(getString(R.string.sftp_validate_accept_host_key), promptMessage , getActivity(), true, new Dialogs.MessageBoxCallback() {
                    @Override
                    public void messageBoxResult(int which) {
                        if(which==Dialogs.MessageBoxCallback.OK){
                            preferenceHelper.setSFTPKnownHostKey(o.hostKey);
                            uploadTestFile();
                        }
                    }
                });
            }
            else {
                Dialogs.error(getString(R.string.sorry), "SFTP Test Failed", o.message , o.throwable, getActivity());
            }
        }
        else {
            Dialogs.alert(getString(R.string.success), "SFTP Test Succeeded", getActivity());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if(preference.getKey().equals(PreferenceNames.SFTP_PRIVATE_KEY_PATH)){
            findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setSummary(newValue.toString());
            return true;
        }


        return false;
    }
}

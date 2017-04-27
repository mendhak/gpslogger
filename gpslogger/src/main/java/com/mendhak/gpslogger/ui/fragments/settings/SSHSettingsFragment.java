package com.mendhak.gpslogger.ui.fragments.settings;

import android.Manifest;
import android.os.Bundle;
import android.preference.Preference;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.ssh.SSHManager;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class SSHSettingsFragment extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator {

    private static final Logger LOG = Logs.of(SSHSettingsFragment.class);
    SSHManager manager;


    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("on create");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sshsettings);

        manager = new SSHManager(PreferenceHelper.getInstance());

        findPreference("ssh_validateserver").setOnPreferenceClickListener(this);
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

        if(preference.getKey().equals("ssh_validateserver")) {

            uploadTestFile();
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
            EventBus.getDefault().post(new UploadEvents.SSH().failed("Could not create local test file", ex));
        }

        manager.uploadFile(testFile);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.SSH o){
        LOG.debug("SSH Event completed, success: " + o.success);
        LOG.debug("SSH HostKey " + o.hostKey);
        LOG.debug("SSH Fingerprint " + o.fingerprint);

        Dialogs.hideProgress();
        if(!o.success){
            if(o.message.contains("reject HostKey")){
                Dialogs.alert("Do you accept the following host?", "Fingerprint: <code>" + o.fingerprint +"</code>", getActivity(), new Dialogs.MessageBoxCallback() {
                    @Override
                    public void messageBoxResult(int which) {
                        if(which==Dialogs.MessageBoxCallback.OK){
                            uploadTestFile();
                        }
                    }
                });
            }
            else {
                Dialogs.error(getString(R.string.sorry), "SSH Test Failed", o.message, o.throwable, getActivity());
            }


        }
        else {
            Dialogs.alert(getString(R.string.success), "SSH Test Succeeded", getActivity());
        }
    }
}

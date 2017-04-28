package com.mendhak.gpslogger.ui.fragments.settings;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.text.TextUtils;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.ssh.SSHManager;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;

public class SSHSettingsFragment extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator {

    private static final Logger LOG = Logs.of(SSHSettingsFragment.class);
    SSHManager manager;
    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static int NONONSENSE_DIRPICKER_ACTIVITYID = 929292;


    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("on create");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sshsettings);

        manager = new SSHManager(preferenceHelper);

        findPreference("ssh_validateserver").setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SSH_PRIVATE_KEY_PATH).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SSH_PRIVATE_KEY_PATH).setSummary(preferenceHelper.getSSHPrivateKeyFilePath());
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

        if (preference.getKey().equals(PreferenceNames.SSH_PRIVATE_KEY_PATH)) {

            Intent i = new Intent(getActivity(), FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);

            String filePath = preferenceHelper.getSSHPrivateKeyFilePath();

            if(Strings.isNullOrEmpty(filePath)){
                filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }

            i.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
            startActivityForResult(i, NONONSENSE_DIRPICKER_ACTIVITYID);

            return true;
        }
        else if(preference.getKey().equals("ssh_validateserver")) {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NONONSENSE_DIRPICKER_ACTIVITYID && resultCode == Activity.RESULT_OK) {

            Uri uri = data.getData();
            LOG.debug("Private key chosen - " + uri.getPath());

            preferenceHelper.setSSHPrivateKeyFilePath(uri.getPath());
            findPreference(PreferenceNames.SSH_PRIVATE_KEY_PATH).setSummary(uri.getPath());

        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @EventBusHook
    public void onEventMainThread(final UploadEvents.SSH o){
        LOG.debug("SSH Event completed, success: " + o.success);

        Dialogs.hideProgress();
        if(!o.success){
            if(o.message.contains("reject HostKey") || o.message.contains("HostKey has been changed")){
                LOG.debug("SSH HostKey " + o.hostKey);
                LOG.debug("SSH Fingerprint " + o.fingerprint);

                Dialogs.alert("Do you accept the following host?", "Fingerprint: <code>" + o.fingerprint +"</code>", getActivity(), true, new Dialogs.MessageBoxCallback() {
                    @Override
                    public void messageBoxResult(int which) {
                        if(which==Dialogs.MessageBoxCallback.OK){
                            preferenceHelper.setSSHKnownHostKey(o.hostKey);
                            uploadTestFile();
                        }
                    }
                });
            }
            else {
                String sshMessages = (o.sshMessages == null) ? "" : TextUtils.join(" ",o.sshMessages);
                Dialogs.error(getString(R.string.sorry), "SSH Test Failed", o.message + "\r\n" + sshMessages, o.throwable, getActivity());
            }
        }
        else {
            Dialogs.alert(getString(R.string.success), "SSH Test Succeeded", getActivity());
        }
    }
}

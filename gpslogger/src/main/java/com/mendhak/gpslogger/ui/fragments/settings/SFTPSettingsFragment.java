package com.mendhak.gpslogger.ui.fragments.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.support.v4.content.ContextCompat;
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
import com.mendhak.gpslogger.senders.sftp.SFTPManager;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;

public class SFTPSettingsFragment extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener, PreferenceValidator {

    private static final Logger LOG = Logs.of(SFTPSettingsFragment.class);
    SFTPManager manager;
    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static int NONONSENSE_DIRPICKER_ACTIVITYID = 929292;


    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("on create");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sftpsettings);

        manager = new SFTPManager(preferenceHelper);

        findPreference("sftp_validateserver").setOnPreferenceClickListener(this);
        findPreference("sftp_reset_authorisation").setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setOnPreferenceClickListener(this);
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
    @AskPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals(PreferenceNames.SFTP_PRIVATE_KEY_PATH)) {

            Intent i = new Intent(getActivity(), FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);

            String filePath = preferenceHelper.getSFTPPrivateKeyFilePath();

            if(Strings.isNullOrEmpty(filePath)){
                filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }

            i.putExtra(FilePickerActivity.EXTRA_START_PATH, filePath);
            startActivityForResult(i, NONONSENSE_DIRPICKER_ACTIVITYID);

            return true;
        }
        else if(preference.getKey().equals("sftp_validateserver")) {
            uploadTestFile();
        }
        else if (preference.getKey().equals("sftp_reset_authorisation")){
            preferenceHelper.setSFTPKnownHostKey("");
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NONONSENSE_DIRPICKER_ACTIVITYID && resultCode == Activity.RESULT_OK) {

            Uri uri = data.getData();
            LOG.debug("Private key chosen - " + uri.getPath());

            preferenceHelper.setSFTPPrivateKeyFilePath(uri.getPath());
            findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setSummary(uri.getPath());

        }
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

                Dialogs.alert("Accept this host key?", promptMessage , getActivity(), true, new Dialogs.MessageBoxCallback() {
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
}

package com.mendhak.gpslogger.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.preference.Preference;
import android.provider.Settings;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.codekidlabs.storagechooser.StorageChooser;
import com.mendhak.gpslogger.BuildConfig;
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
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import org.slf4j.Logger;

import java.io.File;

public class SFTPSettingsFragment extends PreferenceFragmentCompat
        implements

        Preference.OnPreferenceClickListener,
        PreferenceValidator,
        Preference.OnPreferenceChangeListener,
        SimpleDialog.OnDialogResultListener {

    private static final Logger LOG = Logs.of(SFTPSettingsFragment.class);
    SFTPManager manager;
    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("on create");
        super.onCreate(savedInstanceState);

        manager = new SFTPManager(preferenceHelper);

        findPreference(PreferenceNames.SFTP_HOST).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_HOST).setSummary(preferenceHelper.getSFTPHost());

        findPreference(PreferenceNames.SFTP_PORT).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PORT).setSummary(String.valueOf(preferenceHelper.getSFTPPort()));

        findPreference(PreferenceNames.SFTP_REMOTE_SERVER_PATH).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_REMOTE_SERVER_PATH).setSummary(preferenceHelper.getSFTPRemoteServerPath());

        findPreference(PreferenceNames.SFTP_USER).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_USER).setSummary(preferenceHelper.getSFTPUser());

        findPreference(PreferenceNames.SFTP_PASSWORD).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PASSWORD).setSummary(preferenceHelper.getSFTPPassword().replaceAll(".","*"));

        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE).setSummary(preferenceHelper.getSFTPPrivateKeyPassphrase().replaceAll(".","*"));

        findPreference("sftp_validateserver").setOnPreferenceClickListener(this);
        findPreference("sftp_reset_authorisation").setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setSummary(preferenceHelper.getSFTPPrivateKeyFilePath());
        registerEventBus();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        LOG.debug("onCreatePreferences");
        setPreferencesFromResource(R.xml.sftpsettings, rootKey);
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
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_PRIVATE_KEY_PATH)){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                Dialogs.alert(getString(R.string.error),getString(R.string.gpslogger_custom_path_need_permission),getActivity(), which -> {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
                });
                return false;
            }

            StorageChooser chooser = Dialogs.filePicker(getActivity(), getActivity().getFragmentManager());
            chooser.setOnSelectListener(path -> {
                LOG.debug(path);
                findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PATH).setSummary(path);
                preferenceHelper.setSFTPPrivateKeyFilePath(path);
            });
            chooser.show();
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_HOST)){
            SimpleFormDialog.build().title(R.string.autoopengts_server_summary)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_HOST).required().text(preferenceHelper.getSFTPHost())
                    )
                    .show(this, PreferenceNames.SFTP_HOST);
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_PORT)){
            SimpleFormDialog.build().title(R.string.autoftp_port)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_PORT).required().text(String.valueOf(preferenceHelper.getSFTPPort())).inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show( this, PreferenceNames.SFTP_PORT);
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_REMOTE_SERVER_PATH)){
            SimpleFormDialog.build().title(R.string.autoftp_directory)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_REMOTE_SERVER_PATH).required().text(preferenceHelper.getSFTPRemoteServerPath())
                    ).show(this, PreferenceNames.SFTP_REMOTE_SERVER_PATH);
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_USER)){
            SimpleFormDialog.build().title(R.string.autoftp_username)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_USER).required().text(preferenceHelper.getSFTPUser())
                    ).show(this, PreferenceNames.SFTP_USER);
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_PASSWORD)){
            SimpleFormDialog.build().title(R.string.autoftp_password)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_PASSWORD).text(preferenceHelper.getSFTPPassword()).showPasswordToggle().inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    ).show(this, PreferenceNames.SFTP_PASSWORD);
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE)){
            SimpleFormDialog.build().title(R.string.sftp_private_key_passphrase)
                    .fields(
                            Input.plain(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE).text(preferenceHelper.getSFTPPrivateKeyPassphrase()).showPasswordToggle().inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    ).show(this, PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE);
        }

        return false;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_HOST)){
            if (which == BUTTON_POSITIVE) {
                String sftpHost = extras.getCharSequence(PreferenceNames.SFTP_HOST).toString();
                preferenceHelper.setSFTPHost(sftpHost);
                findPreference(PreferenceNames.SFTP_HOST).setSummary(sftpHost);
                return true;
            }
        }
        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_PORT)){
            if (which == BUTTON_POSITIVE) {
                String sftpPort = extras.getString(PreferenceNames.SFTP_PORT);
                preferenceHelper.setSFTPPort(sftpPort);
                findPreference(PreferenceNames.SFTP_PORT).setSummary(String.valueOf(sftpPort));
                return true;
            }
        }
        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_REMOTE_SERVER_PATH)){
            if (which==BUTTON_POSITIVE) {
                String remoteServerPath = extras.getString(PreferenceNames.SFTP_REMOTE_SERVER_PATH);
                preferenceHelper.setSFTPRemoteServerPath(remoteServerPath);
                findPreference(PreferenceNames.SFTP_REMOTE_SERVER_PATH).setSummary(remoteServerPath);
            }
        }
        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_USER)){
            if (which==BUTTON_POSITIVE) {
                String sftpUser = extras.getString(PreferenceNames.SFTP_USER);
                preferenceHelper.setSFTPUser(sftpUser);
                findPreference(PreferenceNames.SFTP_USER).setSummary(sftpUser);
            }
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_PASSWORD)){
            if (which==BUTTON_POSITIVE) {
                String sftpPassword = extras.getString(PreferenceNames.SFTP_PASSWORD);
                preferenceHelper.setSFTPPassword(sftpPassword);
                findPreference(PreferenceNames.SFTP_PASSWORD).setSummary(sftpPassword.replaceAll(".","*"));
            }
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE)){
            if (which==BUTTON_POSITIVE) {
                String privKeyPass = extras.getString(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE);
                preferenceHelper.setSFTPPrivateKeyPassphrase(privKeyPass);
                findPreference(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE).setSummary(privKeyPass.replaceAll(".","*"));
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 55 && resultCode == Activity.RESULT_OK) {

            String folderLocation = data.getExtras().getString("data");
            LOG.debug( folderLocation );

        }
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

package com.mendhak.gpslogger.ui.fragments.settings;

import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.http.HttpFileUploadManager;
import com.mendhak.gpslogger.ui.Dialogs;

import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

public class HttpFileUploadSettingsFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator,
        Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(HttpFileUploadSettingsFragment.class);
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference urlPreference = findPreference(PreferenceNames.HTTPUPLOAD_URL);
        urlPreference.setSummary(preferenceHelper.getHttpFileUploadUrl());
        urlPreference.setOnPreferenceClickListener(this);

        Preference bodyTypePreference = findPreference(PreferenceNames.HTTPUPLOAD_BODY_TYPE);
        bodyTypePreference.setSummary(getBodyTypeDisplay(preferenceHelper.getHttpFileUploadBodyType()));
        bodyTypePreference.setOnPreferenceClickListener(this);

        Preference methodPreference = findPreference(PreferenceNames.HTTPUPLOAD_METHOD);
        methodPreference.setSummary(preferenceHelper.getHttpFileUploadMethod());
        methodPreference.setOnPreferenceClickListener(this);

        Preference headersPreference = findPreference(PreferenceNames.HTTPUPLOAD_HEADERS);
        headersPreference.setSummary(preferenceHelper.getHttpFileUploadHeaders());
        headersPreference.setOnPreferenceClickListener(this);

        Preference authPreference = findPreference("httpupload_basicauth");
        updateAuthSummary(authPreference);
        authPreference.setOnPreferenceClickListener(this);

        findPreference("httpupload_test").setOnPreferenceClickListener(this);

        updateMethodPreferenceState();
        registerEventBus();
    }

    private void updateAuthSummary(Preference authPreference) {
        String username = preferenceHelper.getHttpFileUploadUsername();
        if (!Strings.isNullOrEmpty(username)) {
            authPreference.setSummary(username + ":***");
        } else {
            authPreference.setSummary("");
        }
    }

    private String getBodyTypeDisplay(String bodyType) {
        String[] entries = getResources().getStringArray(R.array.http_body_types);
        String[] values = getResources().getStringArray(R.array.http_body_type_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(bodyType)) {
                return entries[i];
            }
        }
        return entries[0];
    }

    private void updateMethodPreferenceState() {
        Preference methodPreference = findPreference(PreferenceNames.HTTPUPLOAD_METHOD);
        if ("form-data".equals(preferenceHelper.getHttpFileUploadBodyType())) {
            preferenceHelper.setHttpFileUploadMethod("POST");
            methodPreference.setSummary("POST");
            methodPreference.setEnabled(false);
        } else {
            methodPreference.setEnabled(true);
            methodPreference.setSummary(preferenceHelper.getHttpFileUploadMethod());
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.httpuploadsettings, rootKey);
    }

    @Override
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            // Safe to ignore
        }
    }

    @Override
    public boolean isValid() {
        HttpFileUploadManager manager = new HttpFileUploadManager(preferenceHelper);
        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceNames.HTTPUPLOAD_URL)) {
            SimpleFormDialog.build()
                    .title("URL")
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.HTTPUPLOAD_URL)
                                    .text(preferenceHelper.getHttpFileUploadUrl())
                                    .required()
                    )
                    .show(this, PreferenceNames.HTTPUPLOAD_URL);
            return true;
        }

        if (preference.getKey().equals(PreferenceNames.HTTPUPLOAD_METHOD)) {
            SimpleListDialog.build()
                    .title(R.string.customurl_http_method)
                    .items(getActivity(), R.array.http_methods)
                    .choiceMode(SimpleListDialog.SINGLE_CHOICE_DIRECT)
                    .show(this, PreferenceNames.HTTPUPLOAD_METHOD);
            return true;
        }

        if (preference.getKey().equals(PreferenceNames.HTTPUPLOAD_BODY_TYPE)) {
            SimpleListDialog.build()
                    .title(R.string.http_file_upload_body_type)
                    .items(getActivity(), R.array.http_body_types)
                    .choiceMode(SimpleListDialog.SINGLE_CHOICE_DIRECT)
                    .show(this, PreferenceNames.HTTPUPLOAD_BODY_TYPE);
            return true;
        }

        if (preference.getKey().equals(PreferenceNames.HTTPUPLOAD_HEADERS)) {
            SimpleFormDialog.build()
                    .title(R.string.customurl_http_headers)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.HTTPUPLOAD_HEADERS)
                                    .text(preferenceHelper.getHttpFileUploadHeaders())
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                    )
                    .show(this, PreferenceNames.HTTPUPLOAD_HEADERS);
            return true;
        }

        if (preference.getKey().equals("httpupload_basicauth")) {
            SimpleFormDialog.build()
                    .title("Basic Authentication")
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.HTTPUPLOAD_BASICAUTH_USERNAME)
                                    .text(preferenceHelper.getHttpFileUploadUsername())
                                    .hint(R.string.autoftp_username),
                            Input.plain(PreferenceNames.HTTPUPLOAD_BASICAUTH_PASSWORD)
                                    .text(preferenceHelper.getHttpFileUploadPassword())
                                    .hint(R.string.autoftp_password)
                                    .showPasswordToggle()
                                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    )
                    .show(this, "httpupload_basicauth");
            return true;
        }

        if (preference.getKey().equals("httpupload_test")) {
            HttpFileUploadManager manager = new HttpFileUploadManager(preferenceHelper);
            if (!manager.isAvailable()) {
                Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                        getString(R.string.autoftp_invalid_summary),
                        getActivity());
                return false;
            }

            Dialogs.progress((FragmentActivity) getActivity(), "Testing HTTP File Upload");
            manager.testHttpFileUpload();
            return true;
        }

        return false;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.HttpFileUpload o) {
        LOG.debug("HTTP File Upload Event completed, success: " + o.success);
        Dialogs.hideProgress();
        if (!o.success) {
            Dialogs.showError(getString(R.string.sorry), "HTTP File Upload Test Failed", o.message, o.throwable, (FragmentActivity) getActivity());
        } else {
            Dialogs.alert(getString(R.string.success), "HTTP File Upload Test Succeeded", getActivity());
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which != BUTTON_POSITIVE) {
            return true;
        }

        if (dialogTag.equals(PreferenceNames.HTTPUPLOAD_URL)) {
            String url = extras.getString(PreferenceNames.HTTPUPLOAD_URL);
            preferenceHelper.setHttpFileUploadUrl(url);
            findPreference(PreferenceNames.HTTPUPLOAD_URL).setSummary(url);
            return true;
        }

        if (dialogTag.equals(PreferenceNames.HTTPUPLOAD_METHOD)) {
            String method = extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL);
            preferenceHelper.setHttpFileUploadMethod(method);
            findPreference(PreferenceNames.HTTPUPLOAD_METHOD).setSummary(method);
            return true;
        }

        if (dialogTag.equals(PreferenceNames.HTTPUPLOAD_BODY_TYPE)) {
            String label = extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL);
            String[] entries = getResources().getStringArray(R.array.http_body_types);
            String[] values = getResources().getStringArray(R.array.http_body_type_values);

            String value = values[0];
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].equals(label)) {
                    value = values[i];
                    break;
                }
            }

            preferenceHelper.setHttpFileUploadBodyType(value);
            findPreference(PreferenceNames.HTTPUPLOAD_BODY_TYPE).setSummary(getBodyTypeDisplay(value));
            updateMethodPreferenceState();
            return true;
        }

        if (dialogTag.equals(PreferenceNames.HTTPUPLOAD_HEADERS)) {
            String headers = extras.getString(PreferenceNames.HTTPUPLOAD_HEADERS);
            preferenceHelper.setHttpFileUploadHeaders(headers);
            findPreference(PreferenceNames.HTTPUPLOAD_HEADERS).setSummary(headers);
            return true;
        }

        if (dialogTag.equals("httpupload_basicauth")) {
            String username = extras.getString(PreferenceNames.HTTPUPLOAD_BASICAUTH_USERNAME);
            String password = extras.getString(PreferenceNames.HTTPUPLOAD_BASICAUTH_PASSWORD);
            preferenceHelper.setHttpFileUploadUsername(username);
            preferenceHelper.setHttpFileUploadPassword(password);
            updateAuthSummary(findPreference("httpupload_basicauth"));
            return true;
        }

        return false;
    }
}

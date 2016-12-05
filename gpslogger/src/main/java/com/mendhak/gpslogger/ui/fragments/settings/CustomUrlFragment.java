/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.mendhak.gpslogger.ui.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.Html;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.CertificateValidationException;
import com.mendhak.gpslogger.common.Networks;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.customurl.LocalX509TrustManager;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

public class CustomUrlFragment extends PermissionedPreferenceFragment implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(CustomUrlFragment.class);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customurlsettings);

        EditTextPreference urlPathPreference = (EditTextPreference)findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);


        String legend1 = MessageFormat.format("{0} %LAT\n{1} %LON\n{2} %DESC\n{3} %SAT\n{4} %ALT",
                getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                getString(R.string.txt_satellites), getString(R.string.txt_altitude));

        Preference urlLegendPreference1 = (Preference)findPreference("customurl_legend_1");
        urlLegendPreference1.setSummary(legend1);

        String legend2 = MessageFormat.format("{0} %SPD\n{1} %ACC\n{2} %DIR\n{3} %PROV",
                getString(R.string.txt_speed),
                getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider)
                );

        Preference urlLegendPreference2 = (Preference)findPreference("customurl_legend_2");
        urlLegendPreference2.setSummary(legend2);

        String legend3 = MessageFormat.format("{0} %TIME\n{1} %BATT\n{2} %AID\n{3} %SER\n{4} %ACT",
                getString(R.string.txt_time_isoformat), "Battery:", "Android ID:", "Serial:", getString(R.string.txt_activity)
                );

        Preference urlLegendPreference3 = (Preference)findPreference("customurl_legend_3");
        urlLegendPreference3.setSummary(legend3);

        Preference getCustomCert = (Preference)findPreference("customurl_getcustomsslcert");
        getCustomCert.setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_PATH)){
            preference.setSummary(newValue.toString());
        }
        return true;
    }


    @Override
    public boolean isValid() {
        return true;
    }


    public Handler postValidationHandler = new Handler();

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("customurl_getcustomsslcert")){
            Dialogs.progress(getActivity(),getString(R.string.please_wait), getString(R.string.please_wait));
            new Thread(fetchCert).start();
            return true;
        }
        return false;
    }

    public void onCertificateFetched(Exception e, boolean isValid){

        Dialogs.hideProgress();

        if(!isValid){
            final CertificateValidationException cve = Networks.extractCertificateValidationException(e);

            if(cve != null){
                LOG.debug(cve.getCertificate().getIssuerDN().getName());
                try {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("<br /><br /><strong>").append("Subject: ").append("</strong>").append(cve.getCertificate().getSubjectDN().getName());
                    sb.append("<br /><br /><strong>").append("Issuer: ").append("</strong>").append(cve.getCertificate().getIssuerDN().getName());
                    sb.append("<br /><br /><strong>").append("Fingerprint: ").append("</strong>").append(DigestUtils.shaHex(cve.getCertificate().getEncoded()));
                    sb.append("<br /><br /><strong>").append("Issued on: ").append("</strong>").append(cve.getCertificate().getNotBefore());
                    sb.append("<br /><br /><strong>").append("Expires on: ").append("</strong>").append(cve.getCertificate().getNotBefore());

                    new MaterialDialog.Builder(getActivity())
                            .title(cve.getMessage())
                            .content(Html.fromHtml("Add this certificate to local keystore?<br />" + sb.toString()))
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        Networks.addCertToKnownServersStore(cve.getCertificate(), getActivity().getApplicationContext());
                                    } catch (Exception e) {
                                        LOG.error("Could not add to the keystore", e);
                                    }

                                    dialog.dismiss();
                                }
                            }).show();

                } catch (Exception e1) {
                    LOG.error("Could not get fingerprint of certificate", e1);
                }

            }
            else {
                LOG.error("Error while attempting to fetch server certificate", e);
                Dialogs.error(getString(R.string.error), "Error while attempting to fetch server certificate", e.getMessage(),e,getActivity());
            }
        }
        else {
            Dialogs.alert(getString(R.string.success),getString(R.string.success),getActivity());
        }
    }

    private Runnable fetchCert = new Runnable() {
        @Override
        public void run() {
            try {

                OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
                okBuilder.sslSocketFactory(Networks.getSocketFactory(getActivity()));
                OkHttpClient client = okBuilder.build();
                Request request = new Request.Builder().url(PreferenceHelper.getInstance().getCustomLoggingUrl()).build();
                Response resp = client.newCall(request).execute();
                LOG.debug("Page responded with HTTP " + Integer.toString(resp.code()));
                resp.body().close();
                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onCertificateFetched(null, true);
                    }
                });

            }
            catch (final Exception e) {

                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onCertificateFetched(e, false);
                    }
                });
            }

        }
    };

}

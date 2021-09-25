package com.mendhak.gpslogger.ui.components;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.ui.Dialogs;

import org.slf4j.Logger;

import java.security.cert.X509Certificate;

import eltos.simpledialogfragment.SimpleDialog;

public class SimpleTLSValidationDialog extends SimpleDialog<SimpleTLSValidationDialog> {

    private static final Logger LOG = Logs.of(SimpleTLSValidationDialog.class);

    public static SimpleTLSValidationDialog build(){
        return new SimpleTLSValidationDialog()
                .title(R.string.error)
                .pos(R.string.ok)
                .neg(R.string.cancel);
    }

    @Override
    protected boolean callResultListener(int which, @Nullable Bundle extras) {
        if(which == OnDialogResultListener.BUTTON_POSITIVE){
            try {
                X509Certificate cert = (X509Certificate) getArgs().getBundle("SimpleDialog.bundle").getSerializable("CERT");
                Networks.addCertToKnownServersStore(cert, getActivity());
                Dialogs.alert("", getString(R.string.restart_required), getActivity());
            } catch (Exception e) {
                LOG.error("Could not add to the keystore", e);
                Dialogs.showError(getString(R.string.error), e.getMessage(), e.getMessage(), e, getActivity());
            }
            return true;
        }
        return super.callResultListener(which, extras);
    }
}
package com.mendhak.gpslogger.ui.components;


import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.mendhak.gpslogger.R;
import eltos.simpledialogfragment.SimpleDialog;

public class SimpleErrorDialog extends SimpleDialog<SimpleErrorDialog> {

    public static SimpleErrorDialog build(){
        return new SimpleErrorDialog()
                .title(R.string.error)
                .neut(R.string.copy);
    }

    @Override
    protected boolean callResultListener(int which, @Nullable Bundle extras) {
        if (which == OnDialogResultListener.BUTTON_NEUTRAL) {
            String plainMessage = Html.fromHtml(getMessage()).toString();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("GPSLogger error message", plainMessage);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getContext(), "Error message copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.callResultListener(which, extras);
    }
}
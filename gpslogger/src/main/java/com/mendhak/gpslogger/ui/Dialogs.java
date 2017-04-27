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

package com.mendhak.gpslogger.ui;


import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.util.Arrays;

public class Dialogs {
    private static MaterialDialog pd;

    protected static String getFormattedErrorMessageForDisplay(String message, Throwable throwable) {
        String html = "";
        if(!Strings.isNullOrEmpty(message)){
            html += "<b>" + message.replace("\r\n","<br />").replace("\n","<br />") + "</b> <br /><br />";
        }

        if(throwable != null){
            if(!Strings.isNullOrEmpty(throwable.getMessage())){
                html += throwable.getMessage().replace("\r\n","<br />") + "<br />";
            }
        }
       return html;
    }

    protected static String getFormattedErrorMessageForPlainText(String message, Throwable throwable){

        StringBuilder sb = new StringBuilder();
        if(!Strings.isNullOrEmpty(message)){
            sb.append(message).append("\r\n");
        }

        if(throwable != null){
            if(!Strings.isNullOrEmpty(throwable.getMessage())){
                sb.append(throwable.getMessage()).append("\r\n");
            }

            if(throwable.getStackTrace().length > 0) {
                sb.append(Arrays.toString(throwable.getStackTrace()));
            }
        }

        return sb.toString();

    }

    public static void error(String title, final String friendlyMessage, final String errorMessage, final Throwable throwable, final Context context){

        final String messageFormatted = getFormattedErrorMessageForDisplay(errorMessage, throwable);

        MaterialDialog alertDialog = new MaterialDialog.Builder(context)
                .title(title)
                .customView(R.layout.error_alertview, true)
                .autoDismiss(false)
                .negativeText("Copy")
                .positiveText(R.string.ok)
                .neutralText("Details")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {

                        final ExpandableTextView expTv1 = (ExpandableTextView) materialDialog.getCustomView().findViewById(R.id.error_expand_text_view);
                        expTv1.findViewById(R.id.expand_collapse).performClick();

                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Gpslogger error message", getFormattedErrorMessageForPlainText(errorMessage, throwable));
                        clipboard.setPrimaryClip(clip);

                        materialDialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .build();


        final ExpandableTextView expTv1 = (ExpandableTextView) alertDialog.getCustomView().findViewById(R.id.error_expand_text_view);
        expTv1.setText(Html.fromHtml(messageFormatted));
        TextView tv = (TextView) expTv1.findViewById(R.id.expandable_text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvFriendly = (TextView)alertDialog.getCustomView().findViewById(R.id.error_friendly_message);
        tvFriendly.setText(friendlyMessage);

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context The calling class, such as GpsMainActivity.this or
     *                  mainActivity.
     */
    public static void alert(String title, String message, Context context) {
        alert(title, message, context, false, null);
    }



    public static void alert(String title, String message, Context context, final MessageBoxCallback msgCallback){
        alert(title, message, context, false, msgCallback);
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context   The calling class, such as GpsMainActivity.this or
     *                    mainActivity.
     * @param msgCallback An object which implements IHasACallBack so that the
     *                    click event can call the callback method.
     */
    public static void alert(String title, String message, Context context, boolean includeCancel, final MessageBoxCallback msgCallback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(title)
                .content(Html.fromHtml(message))
                .positiveText(R.string.ok)

                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (msgCallback != null) {
                            msgCallback.messageBoxResult(MessageBoxCallback.OK);
                        }
                    }
                })
                ;

        if(includeCancel){
            builder.negativeText(R.string.cancel);
            builder.onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                    if (msgCallback != null) {
                        msgCallback.messageBoxResult(MessageBoxCallback.CANCEL);
                    }
                }
            });
        }


         MaterialDialog alertDialog = builder.build();

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }

    }

    public static void progress(Context context, String title, String message) {
        if (context != null) {

            pd = new MaterialDialog.Builder(context)
                    .title(title)
                    .content(message)
                    .progress(true, 0)
                    .show();
        }
    }

    public static void hideProgress() {
        if (pd != null) {
            pd.dismiss();
        }
    }

    public interface MessageBoxCallback {

        int CANCEL = 0;
        int OK = 1;

        void messageBoxResult(int which);
    }
}

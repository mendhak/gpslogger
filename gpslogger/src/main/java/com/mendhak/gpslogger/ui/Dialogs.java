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
import androidx.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.Files;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Dialogs {
    private static MaterialDialog pd;

    protected static String getFormattedErrorMessageForDisplay(String message, Throwable throwable) {
        StringBuilder html = new StringBuilder();
        if(!Strings.isNullOrEmpty(message)){
            html.append("<b>").append(message.replace("\r\n","<br />")
                    .replace("\n","<br />")).append("</b> <br /><br />");
        }

        while(throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())){
            html.append(throwable.getMessage().replace("\r\n","<br />"))
                    .append("<br /><br />");
            throwable=throwable.getCause();
        }

       return html.toString();
    }

    protected static String getFormattedErrorMessageForPlainText(String message, Throwable throwable){

        StringBuilder sb = new StringBuilder();
        if(!Strings.isNullOrEmpty(message)){
            sb.append(message).append("\r\n");
        }

        while (throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())) {
            sb.append("\r\n\r\n").append(throwable.getMessage()).append("\r\n");
            if (throwable.getStackTrace().length > 0) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                sb.append(sw.toString());
            }
            throwable = throwable.getCause();
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
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Gpslogger error message", getFormattedErrorMessageForPlainText(friendlyMessage, throwable));
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


    /**
     * Text input dialog, with auto complete entries stored in cache.
     * Invokes callback with user entry afterwards, only if OK is pressed
     * Dismisses dialog if no text is entered
     * @param cacheKey  the unique cache key for this dialog's entries
     * @param title the title of the dialog box
     * @param hint the hint to show if text is empty
     * @param text the text to set in the text box
     * @param callback the callback to invoke after user presses OK
     */
    public static void autoCompleteText(final Context ctx, final String cacheKey, String title,
                                        String hint, String text,
                                        final AutoCompleteCallback callback) {

        final List<String> cachedList = Files.getListFromCacheFile(cacheKey, ctx);
        final LinkedHashSet<String> set = new LinkedHashSet(cachedList);

        final MaterialDialog alertDialog = new MaterialDialog.Builder(ctx)
                .title(title)
                .customView(R.layout.custom_autocomplete_view, true)
                .negativeText(R.string.cancel)
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {

                        AutoCompleteTextView autoComplete = materialDialog.getCustomView().findViewById(R.id.custom_autocomplete);
                        String enteredText = autoComplete.getText().toString();

                        if(Strings.isNullOrEmpty(enteredText)){
                            materialDialog.dismiss();
                            return;
                        }
                        else if(set.add(enteredText)){
                            Files.saveListToCacheFile(new ArrayList<>(set), cacheKey, ctx);
                        }

                        callback.messageBoxResult(MessageBoxCallback.OK, materialDialog, enteredText);
                        materialDialog.dismiss();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        callback.messageBoxResult(MessageBoxCallback.CANCEL, materialDialog, "");
                        materialDialog.dismiss();
                    }
                })
                .build();

        String[] arr = set.toArray(new String[set.size()]);

        final AutoCompleteTextView customAutocomplete = (AutoCompleteTextView) alertDialog.getCustomView().findViewById(R.id.custom_autocomplete);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_dropdown_item_1line, arr);
        customAutocomplete.setAdapter(adapter);
        customAutocomplete.setHint(hint);
        customAutocomplete.append(text);

        // set keyboard done as dialog positive
        customAutocomplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    alertDialog.getActionButton(DialogAction.POSITIVE).callOnClick();

                }
                return false;
            }
        });

        // show autosuggest dropdown even if empty
        customAutocomplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    customAutocomplete.showDropDown();
                    customAutocomplete.requestFocus();
                }
            }
        });

        // show keyboard
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();

    }

    public interface MessageBoxCallback {

        int CANCEL = 0;
        int OK = 1;

        void messageBoxResult(int which);
    }

    public interface AutoCompleteCallback{
        int CANCEL = 0;
        int OK = 1;

        void messageBoxResult(int which, MaterialDialog dialog, String enteredText);
    }
}

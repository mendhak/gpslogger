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
import android.content.res.Configuration;

import androidx.fragment.app.FragmentActivity;

import com.codekidlabs.storagechooser.StorageChooser;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.components.SimpleErrorDialog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.SimpleProgressDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class Dialogs {
    private static SimpleDialog simpleProgress;

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

    public static StorageChooser directoryChooser(Activity activity){
        return storageChooser(StorageChooser.DIRECTORY_CHOOSER, (FragmentActivity) activity);
    }

    public static StorageChooser filePicker(FragmentActivity activity){
        return storageChooser(StorageChooser.FILE_PICKER, (FragmentActivity) activity);
    }

    private static StorageChooser storageChooser(String chooserType, FragmentActivity activity){
        com.codekidlabs.storagechooser.Content scContent = new com.codekidlabs.storagechooser.Content();
        scContent.setCreateLabel(activity.getString(R.string.storage_chooser_create_label));
        scContent.setInternalStorageText(activity.getString(R.string.storage_chooser_internal_storage_text));
        scContent.setCancelLabel(activity.getString(R.string.cancel));
        scContent.setSelectLabel(activity.getString(R.string.storage_chooser_select_folder));
        scContent.setOverviewHeading(activity.getString(R.string.storage_chooser_overview_heading));
        scContent.setNewFolderLabel(activity.getString(R.string.storage_chooser_new_folder_label));
        scContent.setFreeSpaceText("%s " + activity.getString(R.string.storage_chooser_free_space_text));
        scContent.setTextfieldErrorText(activity.getString(R.string.storage_chooser_text_field_error));
        scContent.setTextfieldHintText(activity.getString(R.string.storage_chooser_text_field_hint));
        scContent.setFolderErrorToastText(activity.getString(R.string.pref_logging_file_no_permissions));


        StorageChooser.Theme scTheme = new StorageChooser.Theme(activity.getApplicationContext());

        if(Systems.isDarkMode(activity)){
            scTheme.setScheme(activity.getResources().getIntArray(com.codekidlabs.storagechooser.R.array.default_dark));
        }
        else {
            int[] myScheme = scTheme.getDefaultScheme();
            myScheme[StorageChooser.Theme.OVERVIEW_HEADER_INDEX] = activity.getResources().getColor(R.color.accentColor);
            myScheme[StorageChooser.Theme.SEC_ADDRESS_BAR_BG] = activity.getResources().getColor(R.color.accentColor);
            myScheme[StorageChooser.Theme.SEC_FOLDER_TINT_INDEX] = activity.getResources().getColor(R.color.primaryColor);
            scTheme.setScheme(myScheme);
        }

        boolean showOverview = Files.hasSDCard(activity);

        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(activity)
                .withFragmentManager(activity.getFragmentManager())
                .withMemoryBar(true)  //Just a bit fancy, a bar.
                .allowCustomPath(true) //If false, defaults to /storage/path. If true, lets user pick a subfolder.
                .hideFreeSpaceLabel(false) //Shows the "MiB" remaining
                .skipOverview(!showOverview) //Always show the storage chooser. Maybe this should be smarter?
                .setTheme(scTheme) //Make it bluish
                .withContent(scContent) //Localizations
                .disableMultiSelect() //Only allow one thing to be chosen
                .allowAddFolder(true) //Let user create a folder using the + icon at the top
                .setType(chooserType) //File picker or folder picker
                .build();

        return chooser;
    }

    public static void showError(String title, final String friendlyMessage, final String errorMessage, final Throwable throwable, final FragmentActivity activity){
        SimpleErrorDialog.build().title(friendlyMessage).msgHtml(getFormattedErrorMessageForDisplay(errorMessage,throwable)).show(activity);
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param activity The calling class, such as GpsMainActivity.this or
     *                  mainActivity.
     */
    public static void alert(String title, String message, Activity activity) {
        SimpleDialog.build().title(title).msgHtml(message).show((FragmentActivity) activity);
    }

    public static void progress(FragmentActivity activity, String title){
        simpleProgress = SimpleProgressDialog.bar().title(title).neut(R.string.hide);
        simpleProgress.show(activity);
    }

    public static void hideProgress() {
        if(simpleProgress!=null){
            simpleProgress.dismiss();
        }
    }

    public static void autoSuggestDialog(final FragmentActivity activity, final String cacheKey,
                                         String title, String hint, String text){

        final List<String> cachedList = Files.getListFromCacheFile(cacheKey, activity);

        SimpleFormDialog.build()
                .fields(
                        Input.plain(cacheKey)
                                .suggest(new ArrayList<>(cachedList))
                                .hint(hint)
                                .text(text)
                )
                .title(title)
                .show(activity, cacheKey);
    }

}
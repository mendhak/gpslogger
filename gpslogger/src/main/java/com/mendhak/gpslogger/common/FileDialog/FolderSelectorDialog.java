package com.mendhak.gpslogger.common.FileDialog;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 * https://github.com/afollestad/material-dialogs/blob/master/sample/src/main/java/com/afollestad/materialdialogssample/FolderSelectorDialog.java
 */
public class FolderSelectorDialog extends DialogFragment implements MaterialDialog.ListCallback {

    private File parentFolder;
    private File[] parentContents;
    private boolean canGoUp = true;
    private FolderSelectCallback mCallback;


    private final MaterialDialog.SingleButtonCallback positiveCallback = new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
            materialDialog.dismiss();
            mCallback.onFolderSelection(parentFolder);
        }
    };

    private final MaterialDialog.SingleButtonCallback negativeCallback = new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
            materialDialog.dismiss();
        }
    };



    public static interface FolderSelectCallback {
        void onFolderSelection(File folder);
    }

    public FolderSelectorDialog(File defaultStorageFolder) {
        parentFolder = defaultStorageFolder;
        parentContents = listFiles();
    }

    public void SetCallback(FolderSelectCallback callback){
        this.mCallback = callback;
    }

    String[] getContentsArray() {
        String[] results = new String[parentContents.length + (canGoUp ? 1 : 0)];
        if (canGoUp) results[0] = "︽";
        for (int i = 0; i < parentContents.length; i++)
            results[canGoUp ? i + 1 : i] = parentContents[i].getName();
        return results;
    }

    File[] listFiles() {
        File[] contents = parentFolder.listFiles();
        List<File> results = new ArrayList<>();
        if(contents != null){
            for (File fi : contents) {
                if (fi.isDirectory() && fi.canWrite()) results.add(fi);
            }
        }

        Collections.sort(results, new FolderSorter());
        return results.toArray(new File[results.size()]);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(parentFolder.getAbsolutePath())
                .items(getContentsArray())
                .itemsCallback(this)
                .onPositive(positiveCallback)
                .onNegative(negativeCallback)
                .autoDismiss(false)
                .positiveText(R.string.ok)
                .negativeText(android.R.string.cancel)
                .build();
    }

    @Override
    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
        if (canGoUp && i == 0) {
            parentFolder = parentFolder.getParentFile();
            canGoUp = parentFolder.getParent() != null && parentFolder.getParentFile().canRead();
        } else {
            parentFolder = parentContents[canGoUp ? i - 1 : i];
            canGoUp = true;
        }

        parentContents = listFiles();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        dialog.setTitle(parentFolder.getAbsolutePath());
        dialog.setItems(getContentsArray());
    }


    public void show(Activity context) {
        show(context.getFragmentManager(), "FOLDER_SELECTOR");
    }


    private static class FolderSorter implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
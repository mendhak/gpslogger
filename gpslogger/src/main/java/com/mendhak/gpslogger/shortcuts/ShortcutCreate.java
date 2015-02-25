/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.shortcuts;

import android.app.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;

public class ShortcutCreate extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final CharSequence[] items = {getString(R.string.shortcut_start), getString(R.string.shortcut_stop)};

        new MaterialDialog.Builder(this)
                .title(R.string.shortcut_pickaction)
                .items(items)
                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int item, CharSequence charSequence) {
                        Intent shortcutIntent;
                        String shortcutLabel;

                        if (item == 0) {
                            shortcutIntent = new Intent(getApplicationContext(), ShortcutStart.class);
                            shortcutLabel = getString(R.string.shortcut_start);
                        } else {
                            shortcutIntent = new Intent(getApplicationContext(), ShortcutStop.class);
                            shortcutLabel = getString(R.string.shortcut_stop);
                        }

                        Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext
                                (getApplicationContext(), R.drawable.gpsloggericon2);
                        Intent intent = new Intent();
                        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutLabel);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                        setResult(RESULT_OK, intent);

                        finish();
                    }
                }).show();


    }
}
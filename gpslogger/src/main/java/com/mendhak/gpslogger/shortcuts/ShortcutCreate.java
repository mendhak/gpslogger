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

package com.mendhak.gpslogger.shortcuts;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.mendhak.gpslogger.R;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

public class ShortcutCreate extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final String[] items = {getString(R.string.shortcut_start), getString(R.string.shortcut_stop)};

        SimpleListDialog.build()
                .title(R.string.shortcut_pickaction)
                .items(items, new long[] {0L, 1L})
                .choiceMode(CustomListDialog.SINGLE_CHOICE)
                .show(this, "SHORTCUT_CREATE");


    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase("SHORTCUT_CREATE") && which == BUTTON_POSITIVE){
            long id = extras.getLong(SimpleListDialog.SELECTED_SINGLE_ID);
            Intent shortcutIntent;
            String shortcutLabel;
            int shortcutIcon;

            if (id == 0) {
                shortcutIntent = new Intent(getApplicationContext(), ShortcutStart.class);
                shortcutLabel = getString(R.string.shortcut_start);
                shortcutIcon = R.drawable.gps_shortcut_start;

            } else {
                shortcutIntent = new Intent(getApplicationContext(), ShortcutStop.class);
                shortcutLabel = getString(R.string.shortcut_stop);
                shortcutIcon = R.drawable.gps_shortcut_stop;
            }

            Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext
                    (getApplicationContext(), shortcutIcon);
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutLabel);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            setResult(RESULT_OK, intent);

            finish();
            return true;
        }
        return false;
    }
}
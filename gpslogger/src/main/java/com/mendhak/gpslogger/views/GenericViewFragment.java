/*******************************************************************************
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
 ******************************************************************************/

package com.mendhak.gpslogger.views;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import de.greenrobot.event.EventBus;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 */
public abstract class GenericViewFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RegisterEventBus();
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void UnregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public void onDestroy() {
        UnregisterEventBus();
        super.onDestroy();
    }



    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationServicesUnavailable locationServicesUnavailable){
        new MaterialDialog.Builder(getActivity())
                //.title("Location services unavailable")
                .content(R.string.gpsprovider_unavailable)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getActivity().startActivity(settingsIntent);
                    }
                })
                .show();
    }


    public void RequestToggleLogging(){

        if(Session.isStarted()){
            ToggleLogging();
            return;
        }

        if(AppSettings.isCustomFile()  && AppSettings.shouldAskCustomFileNameEachTime()){

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.alertview, true)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String chosenFileName = AppSettings.getCustomFileName();
                            EditText userInput = (EditText) dialog.getCustomView().findViewById(R.id.alert_user_input);

                            if (!Utilities.IsNullOrEmpty(userInput.getText().toString()) && !userInput.getText().toString().equalsIgnoreCase(chosenFileName)) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("new_file_custom_name", userInput.getText().toString());
                                editor.apply();
                            }
                            ToggleLogging();
                        }
                    })
                    .keyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                ToggleLogging();
                                dialog.dismiss();
                            }
                            return true;
                        }
                    })
                    .build();

            EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
            userInput.setText(AppSettings.getCustomFileName());
            TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
            tvMessage.setText(R.string.new_file_custom_message);
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();

        }
        else {
            ToggleLogging();
        }
    }

    public void ToggleLogging(){
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }


}

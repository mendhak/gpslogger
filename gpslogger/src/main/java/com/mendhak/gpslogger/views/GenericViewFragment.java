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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import de.greenrobot.event.EventBus;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 */
public abstract class GenericViewFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    // Mechanism to talk back to parent
    protected IGpsViewCallback gpsCallback;

    public abstract void SetLocation(Location locationInfo);

    public abstract void SetSatelliteCount(int count);

    public abstract void SetLoggingStarted();

    public abstract void SetLoggingStopped();

    public abstract void OnWaitingForLocation(boolean inProgress);

    public abstract void OnFileNameChange(String newFileName);

    public abstract void OnNmeaSentence(long timestamp, String nmeaSentence);


    public void OnLocationServicesUnavailable() {

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


    protected void requestStartLogging() {
        if (gpsCallback != null) {
            gpsCallback.onRequestStartLogging();
        }
    }

    protected void requestStopLogging() {
        if (gpsCallback != null) {
            gpsCallback.onRequestStopLogging();
        }
    }

    protected void requestToggleLogging() {
        if (gpsCallback != null) {
            gpsCallback.onRequestToggleLogging();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IGpsViewCallback) {
            gpsCallback = (IGpsViewCallback) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (gpsCallback != null) {
            gpsCallback = null;
        }
    }




    /**
     * Interface used by the different fragments to communicate with the parent activity
     * which should implement this interface.
     */
    public static interface IGpsViewCallback {
        public void onRequestStartLogging();

        public void onRequestStopLogging();

        public void onRequestToggleLogging();
    }
}

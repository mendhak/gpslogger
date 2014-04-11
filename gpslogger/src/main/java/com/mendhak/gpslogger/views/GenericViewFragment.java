package com.mendhak.gpslogger.views;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 *
 * Created by oceanebelle on 04/04/14.
 */
public abstract class GenericViewFragment extends Fragment {
    // Mechanism to talk back to parent
    protected IGpsViewCallback gpsCallback;

    public abstract void SetLocation(Location locationInfo);
    public abstract void SetSatelliteCount(int count);
    public abstract void SetLoggingStarted();
    public abstract void SetLoggingStopped();
    public abstract void SetStatusMessage(String message);
    public abstract void SetFatalMessage(String message);
    public abstract void OnFileNameChange(String newFileName);

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
     * Created by oceanebelle on 04/04/14.
     */
    public static interface IGpsViewCallback {
        public void onRequestStartLogging();
        public void onRequestStopLogging();
        public void onRequestToggleLogging();
    }
}

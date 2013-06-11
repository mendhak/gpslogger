/*
 *    This file is part of GPSLogger for Android.
 *
 *    Copyright Marc Poulhiès <dkm@kataplop.net>
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

package com.mendhak.gpslogger.fragments;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import net.kataplop.gpslogger.R;

import java.text.DecimalFormatSymbols;


public class GlideRatioFragment extends SherlockFragment implements  IWidgetFragment {
    private TextView tvGlide;
    private Location lastLoc;

    private static String TAG = "GlideRationFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.glideratio_fragment, container, false);
        tvGlide = (TextView) v.findViewById(R.id.big_glideratio);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        lastLoc = null;
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        IWidgetContainer c = (IWidgetContainer) getActivity();
        c.setTitle(getTitle(), this);
    }

    @Override
    public String getTitle() {
        return getString(R.string.glideratio);
    }

    @Override
    public void onLocationChanged(final Location loc) {
        Utilities.LogDebug("GlideRatioFragment.onLocationChanged");

        if (loc == null) {
            return;
        }

        setGlideRatio(loc);
    }

    private void setGlideRatio(final Location loc) {
        final boolean hasAlt = loc.hasAltitude();
        final boolean hasPrev = (lastLoc != null);

        final long timeDiff = hasPrev ? loc.getTime() - lastLoc.getTime() : 0;
        final double distDiff = hasPrev ? Utilities.CalculateDistance(
                lastLoc.getLatitude(),
                lastLoc.getLongitude(),
                loc.getLatitude(),
                loc.getLongitude()) : 0;
        final double altDiff = hasPrev && hasAlt ? lastLoc.getAltitude() - loc.getAltitude() : 0;

        Utilities.LogDebug(TAG + " hasprev:" + hasPrev);
        if (!hasAlt){
            Utilities.LogDebug(TAG + " hasAlt:" + hasAlt + " => skipping");
            return;
        }

        if (!hasPrev){
            Utilities.LogDebug(TAG + " no previous loc, storing");
            lastLoc = loc;
            return;
        }

        // last loc is too old for glide ratio computation
        if (timeDiff > 3000) {
            Utilities.LogDebug(TAG + " timeDiff too large:" + timeDiff);
            lastLoc = loc;
            return;
        }

        String t;
        // we're going up !
        if (altDiff < 0) {
            t = DecimalFormatSymbols.getInstance().getInfinity();
        } else {
            int glide = (int) (distDiff / altDiff);
            if (glide > 20){
                t = "20+";
            } else {
                t = Integer.toString(glide);
            }
        }

        tvGlide.setText(t);
        Utilities.LogDebug(TAG + " setting glide ratio " + t);
        lastLoc = loc;
    }

    @Override
    public void setSatelliteInfo(int number) {
    }

    @Override
    public void setStatus(final String message) {
    }

    @Override
    public void clear() {
        tvGlide.setText("-");
    }
}
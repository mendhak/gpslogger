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

package com.mendhak.gpslogger.views.component;

import android.view.View;

/**
 * Toggles two views. Handles the hiding and showing of two views.
 * Notifies interested handlers of the current state.
 */
public class ToggleComponent {
    private View on;
    private View off;
    private ToggleHandler handler;
    private boolean enabled;

    ToggleComponent() {
    }

    public void SetEnabled(boolean enabled) {

        if (enabled) {
            on.setVisibility(View.VISIBLE);
            off.setVisibility(View.GONE);
        } else {
            on.setVisibility(View.GONE);
            off.setVisibility(View.VISIBLE);
        }
    }

    public static ToggleBuilder getBuilder() {
        return new ToggleBuilder(new ToggleComponent());
    }

    public static class ToggleBuilder {
        private final ToggleComponent toggleComponent;
        private boolean built;

        public ToggleBuilder(ToggleComponent toggleComponent) {
            this.toggleComponent = toggleComponent;
        }

        public ToggleBuilder addOnView(View view) {
            ensureNotBuilt();
            if (this.toggleComponent.on != null || view == null) {
                throw new IllegalStateException("On View already set or invalid view");
            }
            this.toggleComponent.on = view;
            return this;
        }

        public ToggleBuilder addOffView(View view) {
            ensureNotBuilt();
            if (this.toggleComponent.off != null || view == null) {
                throw new IllegalStateException("Off View already set or invalid view");
            }
            this.toggleComponent.off = view;
            return this;
        }

        public ToggleBuilder addHandler(ToggleHandler handler) {
            ensureNotBuilt();
            if (this.toggleComponent.handler != null) throw new IllegalStateException("Handler already set");
            this.toggleComponent.handler = handler;
            return this;
        }

        public ToggleBuilder setDefaultState(boolean defaultState) {
            ensureNotBuilt();
            this.toggleComponent.enabled = defaultState;
            return this;
        }

        private void ensureNotBuilt() {
            if (this.built) {
                throw new IllegalStateException("Cannot set properties on built object.");
            }
        }

        public ToggleComponent build() {
            this.toggleComponent.initialiseView(this.toggleComponent.on, this.toggleComponent.off);
            this.built = true;
            return this.toggleComponent;
        }
    }

    public interface ToggleHandler {
        void onStatusChange(boolean status);
    }

    private void initialiseView(View viewOn, View viewOff) {

        SetEnabled(this.enabled);

        viewOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = true;
                on.setVisibility(View.GONE);
                off.setVisibility(View.VISIBLE);
                handler.onStatusChange(enabled);
            }
        });

        viewOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                enabled = false;
                off.setVisibility(View.GONE);
                on.setVisibility(View.VISIBLE);
                handler.onStatusChange(enabled);
            }
        });
    }
}

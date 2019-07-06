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

package com.mendhak.gpslogger.common.events;

public class ProfileEvents {

    /**
     * Requests saving the current profile and loading this new one.
     */
    public static class SwitchToProfile {
        public String newProfileName;

        public SwitchToProfile(String newProfileName){
            this.newProfileName = newProfileName;
        }
    }

    /**
     * Requests the creation of a new profile name.
     */
    public static class CreateNewProfile {
        public String newProfileName;
        public CreateNewProfile(String newProfileName) {
            this.newProfileName = newProfileName;
        }
    }

    /**
     * Requests a .properties file download, profile creation and switching to new profile
     */
    public static class DownloadProfileFromUrl{
        public String profileUrl;
        public DownloadProfileFromUrl(String profileUrl) { this.profileUrl = profileUrl; }
    }

    /**
     * Requests deletion of this profile
     */
    public static class DeleteProfile {
        public String profileName;
        public DeleteProfile(String profileNameToDelete) {
            this.profileName = profileNameToDelete;
        }
    }
}

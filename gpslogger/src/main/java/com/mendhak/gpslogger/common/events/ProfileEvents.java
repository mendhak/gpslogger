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
     * Requests deletion of this profile
     */
    public static class DeleteProfile {
        public String profileName;
        public DeleteProfile(String profileNameToDelete) {
            this.profileName = profileNameToDelete;
        }
    }
}

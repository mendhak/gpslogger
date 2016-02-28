package com.mendhak.gpslogger.common;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.PreferenceNames;

public class PreferenceHelper {

    private static PreferenceHelper instance = null;
    private SharedPreferences prefs;

    private PreferenceHelper(){

    }

    public static PreferenceHelper getInstance(){
        if(instance==null){
            instance = new PreferenceHelper();
            instance.prefs = PreferenceManager.getDefaultSharedPreferences(AppSettings.getInstance().getApplicationContext());
        }

        return instance;
    }

    /**
     * Whether to auto send to Dropbox
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_DROPBOX_ENABLED)
    public  boolean isDropboxAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_DROPBOX_ENABLED, false);
    }

    public  String getDropBoxAccessKeyName() {
        return prefs.getString(PreferenceNames.DROPBOX_ACCESS_KEY, null);
    }

    public  void setDropBoxAccessKeyName(String key) {
        prefs.edit().putString(PreferenceNames.DROPBOX_ACCESS_KEY, key).apply();
    }

    public  String getDropBoxAccessSecretName() {
        return prefs.getString(PreferenceNames.DROPBOX_ACCESS_SECRET, null);
    }

    public  void setDropBoxAccessSecret(String secret) {
        prefs.edit().putString(PreferenceNames.DROPBOX_ACCESS_SECRET, secret).apply();
    }

}

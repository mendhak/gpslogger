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





    /**
     * Whether automatic sending to email is enabled
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_EMAIL_ENABLED)
    public boolean isEmailAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_EMAIL_ENABLED, false);
    }


    /**
     * SMTP Server to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_SERVER)
    public String getSmtpServer() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_SERVER, "");
    }

    /**
     * Sets SMTP Server to use when sending emails
     */
    public void setSmtpServer(String smtpServer) {
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_SERVER, smtpServer).apply();
    }

    /**
     * SMTP Port to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_PORT)
    public String getSmtpPort() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_PORT, "25");
    }

    public void setSmtpPort(String port) {
        prefs.edit().putString(PreferenceNames.EMAIL_SMTP_PORT, port).apply();
    }

    /**
     * SMTP Username to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_USERNAME)
    public String getSmtpUsername() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_USERNAME, "");
    }


    /**
     * SMTP Password to use when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_PASSWORD)
    public String getSmtpPassword() {
        return prefs.getString(PreferenceNames.EMAIL_SMTP_PASSWORD, "");
    }

    /**
     * Whether SSL is enabled when sending emails
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_SMTP_SSL)
    public boolean isSmtpSsl() {
        return prefs.getBoolean(PreferenceNames.EMAIL_SMTP_SSL, true);
    }

    /**
     * Sets whether SSL is enabled when sending emails
     */
    public void setSmtpSsl(boolean smtpSsl) {
        prefs.edit().putBoolean(PreferenceNames.EMAIL_SMTP_SSL, smtpSsl).apply();
    }


    /**
     * Email addresses to send to
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_TARGET)
    public String getAutoEmailTargets() {
        return prefs.getString(PreferenceNames.EMAIL_TARGET, "");
    }


    /**
     * SMTP from address to use
     */
    @ProfilePreference(name= PreferenceNames.EMAIL_FROM)
    private String getSmtpFrom() {
        return prefs.getString(PreferenceNames.EMAIL_FROM, "");
    }

    /**
     * The from address to use when sending an email, uses {@link #getSmtpUsername()} if {@link #getSmtpFrom()} is not specified
     */
    public String getSmtpSenderAddress() {
        if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
            return getSmtpFrom();
        }

        return getSmtpUsername();
    }


}

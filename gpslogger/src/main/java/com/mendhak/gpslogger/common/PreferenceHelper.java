package com.mendhak.gpslogger.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.PreferenceNames;

public class PreferenceHelper {

    private static PreferenceHelper instance = null;
    private SharedPreferences prefs;

    /**
     * Use PreferenceHelper.getInstance()
     */
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



    /**
     * FTP Server name for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_SERVER)
    public String getFtpServerName() {
        return prefs.getString(PreferenceNames.FTP_SERVER, "");
    }


    /**
     * FTP Port for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PORT)
    public int getFtpPort() {
        return Utilities.parseWithDefault(prefs.getString(PreferenceNames.FTP_PORT, "21"), 21);
    }


    /**
     * FTP Username for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_USERNAME)
    public String getFtpUsername() {
        return prefs.getString(PreferenceNames.FTP_USERNAME, "");
    }


    /**
     * FTP Password for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_PASSWORD)
    public String getFtpPassword() {
        return prefs.getString(PreferenceNames.FTP_PASSWORD, "");
    }

    /**
     * Whether to use FTPS
     */
    @ProfilePreference(name= PreferenceNames.FTP_USE_FTPS)
    public boolean FtpUseFtps() {
        return prefs.getBoolean(PreferenceNames.FTP_USE_FTPS, false);
    }


    /**
     * FTP protocol to use (SSL or TLS)
     */
    @ProfilePreference(name= PreferenceNames.FTP_SSLORTLS)
    public String getFtpProtocol() {
        return prefs.getString(PreferenceNames.FTP_SSLORTLS, "");
    }


    /**
     * Whether to use FTP Implicit mode for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_IMPLICIT)
    public boolean FtpImplicit() {
        return prefs.getBoolean(PreferenceNames.FTP_IMPLICIT, false);
    }


    /**
     * Whether to auto send to FTP target
     */
    @ProfilePreference(name= PreferenceNames.AUTOSEND_FTP_ENABLED)
    public boolean isFtpAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_FTP_ENABLED, false);
    }


    /**
     * FTP Directory on the server for auto send
     */
    @ProfilePreference(name= PreferenceNames.FTP_DIRECTORY)
    public String getFtpDirectory() {
        return prefs.getString(PreferenceNames.FTP_DIRECTORY, "GPSLogger");
    }




    /**
     * GPS Logger folder path on phone.  Falls back to {@link Utilities#GetDefaultStorageFolder(Context)} if nothing specified.
     */
    @ProfilePreference(name= PreferenceNames.GPSLOGGER_FOLDER)
    public String getGpsLoggerFolder() {
        return prefs.getString(PreferenceNames.GPSLOGGER_FOLDER, Utilities.GetDefaultStorageFolder(AppSettings.getInstance().getApplicationContext()).getAbsolutePath());
    }


    /**
     * Sets GPS Logger folder path
     */
    public void setGpsLoggerFolder(String folderPath) {
        prefs.edit().putString(PreferenceNames.GPSLOGGER_FOLDER, folderPath).apply();
    }


}

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

package com.mendhak.gpslogger.common;


public  class PreferenceNames {
    public static final String MINIMUM_INTERVAL ="time_before_logging";
    public static final String MINIMUM_DISTANCE = "distance_before_logging";
    public static final String MINIMUM_ACCURACY = "accuracy_before_logging";
    public static final String KEEP_GPS_ON_BETWEEN_FIXES = "keep_fix";
    public static final String LOGGING_RETRY_TIME = "retry_time";
    public static final String ABSOLUTE_TIMEOUT = "absolute_timeout";
    public static final String START_LOGGING_ON_APP_LAUNCH = "startonapplaunch";
    public static final String START_LOGGING_ON_BOOTUP = "startonbootup";
    public static final String LOG_TO_KML = "log_kml";
    public static final String LOG_TO_GPX = "log_gpx";
    public static final String LOG_AS_GPX_11 = "log_gpx_11";
    public static final String LOG_TO_CSV = "log_plain_text";
    public static final String LOG_TO_CSV_DELIMITER = "log_plain_text_csv_delimiter";
    public static final String LOG_TO_CSV_DECIMAL_COMMA = "log_plain_text_csv_decimal_comma";
    public static final String LOG_TO_GEOJSON = "log_geojson";
    public static final String LOG_TO_NMEA = "log_nmea";
    public static final String LOG_TO_URL = "log_customurl_enabled";
    public static final String LOG_TO_URL_PATH = "log_customurl_url";
    public static final String LOG_TO_URL_BODY = "log_customurl_body";
    public static final String LOG_TO_URL_HEADERS = "log_customurl_headers";
    public static final String LOG_TO_URL_METHOD = "log_customurl_method";
    public static final String LOG_TO_URL_BASICAUTH_USERNAME = "log_customurl_basicauth_username";
    public static final String LOG_TO_URL_BASICAUTH_PASSWORD = "log_customurl_basicauth_password";
    public static final String AUTOSEND_CUSTOMURL_ENABLED = "autocustomurl_enabled";
    public static final String LOG_TO_OPENGTS = "log_opengts";
    public static final String LOG_PASSIVE_LOCATIONS="log_passive_locations";
    public static final String LOG_SATELLITE_LOCATIONS = "log_satellite_locations";
    public static final String LOG_NETWORK_LOCATIONS="log_network_locations";
    public static final String NEW_FILE_CREATION_MODE = "new_file_creation";
    public static final String CUSTOM_FILE_NAME = "new_file_custom_name";
    public static final String CUSTOM_FILE_NAME_KEEP_CHANGING = "new_file_custom_keep_changing";
    public static final String ASK_CUSTOM_FILE_NAME = "new_file_custom_each_time";
    public static final String AUTOSEND_ENABLED = "autosend_enabled";
    public static final String AUTOSEND_FREQUENCY = "autosend_frequency_minutes";
    public static final String AUTOSEND_ON_STOP = "autosend_frequency_whenstoppressed";
    public static final String AUTOSEND_EMAIL_ENABLED = "autoemail_enabled";
    public static final String AUTOSEND_ZIP = "autosend_sendzip";
    public static final String AUTOSEND_OPENGTS_ENABLED = "autoopengts_enabled";
    public static final String EMAIL_SMTP_SERVER = "smtp_server";
    public static final String EMAIL_SMTP_PORT = "smtp_port";
    public static final String EMAIL_SMTP_USERNAME = "smtp_username";
    public static final String EMAIL_SMTP_PASSWORD = "smtp_password";

    public static final String EMAIL_SMTP_SSL = "smtp_ssl";
    public static final String EMAIL_TARGET = "autoemail_target";
    public static final String EMAIL_FROM = "smtp_from";
    public static final String DEBUG_TO_FILE = "debugtofile";
    public static final String OPENGTS_SERVER = "opengts_server";
    public static final String OPENGTS_PORT = "opengts_server_port";
    public static final String OPENGTS_PROTOCOL = "opengts_server_communication_method";
    public static final String OPENGTS_SERVER_PATH = "autoopengts_server_path";
    public static final String OPENGTS_DEVICE_ID = "opengts_device_id";
    public static final String OPENGTS_ACCOUNT_NAME = "opengts_accountname";
    public static final String HIDE_NOTIFICATION_BUTTONS = "hide_notification_buttons";
    public static final String HIDE_NOTIFICATION_FROM_STATUS_BAR = "hide_notification_from_status_bar";
    public static final String DISPLAY_IMPERIAL = "useImperial";

    public static final String OPENSTREETMAP_ACCESS_TOKEN = "osm_accesstoken";
    public static final String OPENSTREETMAP_ACCESS_TOKEN_SECRET = "osm_accesstokensecret";
    public static final String OPENSTREETMAP_REQUEST_TOKEN = "osm_requesttoken";
    public static final String OPENSTREETMAP_REQUEST_TOKEN_SECRET = "osm_requesttokensecret";
    public static final String OPENSTREETMAP_DESCRIPTION = "osm_description";
    public static final String OPENSTREETMAP_TAGS = "osm_tags";
    public static final String OPENSTREETMAP_VISIBILITY = "osm_visibility";
    public static final String AUTOSEND_DROPBOX_ENABLED = "dropbox_enabled";
    public static final String DROPBOX_ACCESS_KEY = "DROPBOX_ACCESS_KEY";
    public static final String DROPBOX_ACCESS_SECRET = "DROPBOX_ACCESS_SECRET";
    public static final String AUTOSEND_OSM_ENABLED = "osm_enabled";
    public static final String FTP_SERVER = "autoftp_server";
    public static final String FTP_PORT = "autoftp_port";
    public static final String FTP_USERNAME = "autoftp_username";
    public static final String FTP_PASSWORD = "autoftp_password";
    public static final String FTP_USE_FTPS = "autoftp_useftps";
    public static final String FTP_SSLORTLS = "autoftp_ssltls";
    public static final String FTP_IMPLICIT = "autoftp_implicit";
    public static final String AUTOSEND_FTP_ENABLED = "autoftp_enabled";
    public static final String FTP_DIRECTORY = "autoftp_directory";
    public static final String OWNCLOUD_BASE_URL = "owncloud_server";
    public static final String OWNCLOUD_USERNAME = "owncloud_username";
    public static final String OWNCLOUD_PASSWORD = "owncloud_password";
    public static final String OWNCLOUD_DIRECTORY = "owncloud_directory";
    public static final String AUTOSEND_OWNCLOUD_ENABLED = "owncloud_enabled";
    public static final String GPSLOGGER_FOLDER = "gpslogger_folder";
    public static final String PREFIX_SERIAL_TO_FILENAME = "new_file_prefix_serial";

    public static final String ALTITUDE_SUBTRACT_OFFSET = "altitude_subtractoffset";
    public static final String ALTITUDE_SHOULD_ADJUST = "altitude_subtractgeoidheight";
    public static final String AUTOSEND_WIFI_ONLY = "autosend_wifionly";
    public static final String CURRENT_PROFILE_NAME = "current_profile_name";
    public static final String SELECTED_NAVITEM = "selected_navitem";

    public static final String LAST_VERSION_SEEN_BY_USER = "last_version_seen";
    public static final String USER_SPECIFIED_LANGUAGE = "user_specified_locale";

    public static final String LATLONG_DISPLAY_FORMAT="latlong_display_format";
    public static final String APP_THEME_SETTING = "app_theme_setting";
    public static final String LOGGING_WRITE_TIME_WITH_OFFSET = "file_logging_write_time_with_offset";



    public static enum DegreesDisplayFormat {
        DEGREES_MINUTES_SECONDS, DEGREES_DECIMAL_MINUTES, DECIMAL_DEGREES
    }

    public static final String SFTP_ENABLED ="sftp_enabled";
    public static final String SFTP_HOST ="sftp_host";
    public static final String SFTP_PORT ="sftp_port";
    public static final String SFTP_USER ="sftp_user";
    public static final String SFTP_PASSWORD ="sftp_password";
    public static final String SFTP_PRIVATE_KEY_PATH ="sftp_private_key_path";
    public static final String SFTP_PRIVATE_KEY_PASSPHRASE ="sftp_private_key_passphrase";
    public static final String SFTP_KNOWN_HOST_KEY ="sftp_known_host_key";
    public static final String SFTP_REMOTE_SERVER_PATH ="sftp_remote_server_path";


}

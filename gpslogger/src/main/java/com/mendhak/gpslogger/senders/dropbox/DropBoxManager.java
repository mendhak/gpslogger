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

package com.mendhak.gpslogger.senders.dropbox;


import android.content.Context;
import com.dropbox.core.*;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class DropBoxManager extends FileSender {

    private static final Logger LOG = Logs.of(DropBoxManager.class);
    private final PreferenceHelper preferenceHelper;

    public DropBoxManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    /**
     * Whether the user has authorized GPSLogger with DropBox
     *
     * @return True/False
     */
    public boolean isLinked() {
        return !Strings.isNullOrEmpty(preferenceHelper.getDropboxLongLivedAccessKey()) ||
                !Strings.isNullOrEmpty(preferenceHelper.getDropboxRefreshToken());
    }

    public boolean finishAuthorization() {
        if(!isLinked()){
            DbxCredential dbxCredential = Auth.getDbxCredential();
            if(dbxCredential != null){
                preferenceHelper.setDropboxRefreshToken(dbxCredential.toString());
                return true;
            }
        }

        return false;
    }

    public void startAuthentication(Context context) {
        DbxRequestConfig dbxRequestConfig =  DbxRequestConfig.newBuilder("gpslogger").build();
        Auth.startOAuth2PKCE(context, "0unjsn38gpe3rwv", dbxRequestConfig);
    }

    public void unLink() {
        //Not used anymore but delete this if existing Long Lived Token users are clearing authorization
        preferenceHelper.setDropboxLongLivedAccessKey(null);
        preferenceHelper.setDropboxRefreshToken(null);
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            LOG.debug(f.getName());
            uploadFile(f.getName());
        }
    }

    @Override
    public boolean isAvailable() {
        return isLinked();
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return  preferenceHelper.isDropboxAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.DROPBOX;
    }

    public void uploadFile(final String fileName) {

        HashMap<String, Object> dataMap = new HashMap<String, Object>(){{
            put("fileName", fileName);
        }};
        String tag = String.valueOf(Objects.hashCode(fileName));
        Systems.startWorkManagerRequest(DropboxWorker.class, dataMap, tag);

    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

}





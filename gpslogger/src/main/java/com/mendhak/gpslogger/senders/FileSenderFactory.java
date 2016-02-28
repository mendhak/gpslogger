/*
*    This file is part of GPSLogger for Android.
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

package com.mendhak.gpslogger.senders;

import android.content.Context;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropBoxManager;
import com.mendhak.gpslogger.senders.email.AutoEmailManager;
import com.mendhak.gpslogger.senders.ftp.FtpManager;
import com.mendhak.gpslogger.senders.googledrive.GoogleDriveManager;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;
import com.mendhak.gpslogger.senders.osm.OpenStreetMapManager;
import com.mendhak.gpslogger.senders.owncloud.OwnCloudManager;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSenderFactory {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(FileSenderFactory.class.getSimpleName());
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public static IFileSender GetOsmSender() {
        return new OpenStreetMapManager();
    }

    public static IFileSender GetDropBoxSender() {
        return new DropBoxManager(PreferenceHelper.getInstance());
    }

    public static IFileSender GetGDocsSender() {
        return new GoogleDriveManager(PreferenceHelper.getInstance());
    }

    public static IFileSender GetEmailSender() {
        return new AutoEmailManager(PreferenceHelper.getInstance());
    }

    public static IFileSender GetOpenGTSSender() {
        return new OpenGTSManager();
    }

    public static IFileSender GetFtpSender() {
        return new FtpManager(preferenceHelper);
    }

    public static IFileSender GetOwnCloudSender() {
        return new OwnCloudManager();
    }

    public static void SendFiles(Context applicationContext, final String fileToSend) {

        tracer.info("Sending file " + fileToSend);

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

        if (Utilities.GetFilesInFolder(gpxFolder).length < 1) {
            tracer.warn("No files found to send.");
            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(Utilities.GetFilesInFolder(gpxFolder, new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(fileToSend) && !s.contains("zip");
            }
        })));

        List<File> zipFiles = new ArrayList<>();

        if (files.size() == 0) {
            tracer.warn("No files found to send after filtering.");
            return;
        }

        if (AppSettings.shouldSendZipFile()) {
            File zipFile = new File(gpxFolder.getPath(), fileToSend + ".zip");
            ArrayList<String> filePaths = new ArrayList<>();

            for (File f : files) {
                filePaths.add(f.getAbsolutePath());
            }

            tracer.info("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.zipFiles();

            zipFiles.clear();
            zipFiles.add(zipFile);
        }

        List<IFileSender> senders = GetFileSenders();

        for (IFileSender sender : senders) {
            tracer.debug("Sender: " + sender.getClass().getName());
            //Special case for OSM Uploader
            if(!sender.accept(null, ".zip")){
                sender.uploadFile(files);
                continue;
            }

            if(AppSettings.shouldSendZipFile()){
                sender.uploadFile(zipFiles);
            } else {
                sender.uploadFile(files);
            }

        }
    }


    private static List<IFileSender> GetFileSenders() {

        List<IFileSender> senders = new ArrayList<>();


        if(GetGDocsSender().isAvailable()){
            senders.add(GetGDocsSender());
        }

        if(GetOsmSender().isAvailable()){
            senders.add(GetOsmSender());
        }

        if(GetEmailSender().isAvailable()){
            senders.add(GetEmailSender());
        }

        if(GetDropBoxSender().isAvailable()){
            senders.add(GetDropBoxSender());
        }

        if(GetOpenGTSSender().isAvailable()){
            senders.add(GetOpenGTSSender());
        }

        if(GetFtpSender().isAvailable()){
            senders.add(GetFtpSender());
        }

        if(GetOwnCloudSender().isAvailable()){
            senders.add(GetOwnCloudSender());
        }

        return senders;

    }
}

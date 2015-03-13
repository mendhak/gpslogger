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
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailHelper;
import com.mendhak.gpslogger.senders.ftp.FtpHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.opengts.OpenGTSHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSenderFactory {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(FileSenderFactory.class.getSimpleName());

    public static IFileSender GetOsmSender(Context applicationContext) {
        return new OSMHelper(applicationContext);
    }

    public static IFileSender GetDropBoxSender(Context applicationContext) {
        return new DropBoxHelper(applicationContext);
    }

    public static IFileSender GetGDocsSender(Context applicationContext) {
        return new GDocsHelper(applicationContext);
    }

    public static IFileSender GetEmailSender(Context applicationContext) {
        return new AutoEmailHelper();
    }

    public static IFileSender GetOpenGTSSender(Context applicationContext) {
        return new OpenGTSHelper();
    }

    public static IFileSender GetFtpSender(Context applicationContext) {
        return new FtpHelper();
    }

    public static void SendFiles(Context applicationContext, IActionListener callback) {

        final String currentFileName = Session.getCurrentFileName();
        tracer.info("Sending file " + currentFileName);

        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());

        if (Utilities.GetFilesInFolder(gpxFolder).length < 1) {
            callback.OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(Utilities.GetFilesInFolder(gpxFolder, new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0) {
            callback.OnFailure();
            return;
        }

        if (AppSettings.shouldSendZipFile()) {
            File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");
            ArrayList<String> filePaths = new ArrayList<String>();

            for (File f : files) {
                filePaths.add(f.getAbsolutePath());
            }

            tracer.info("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.Zip();

            files.clear();
            files.add(zipFile);
        }

        List<IFileSender> senders = GetFileSenders(applicationContext);

        for (IFileSender sender : senders) {
            sender.UploadFile(files);
        }
    }


    public static List<IFileSender> GetFileSenders(Context applicationContext) {
        List<IFileSender> senders = new ArrayList<IFileSender>();

        if (GDocsHelper.IsLinked(applicationContext)) {
            tracer.debug("Google Docs Sender picked");
            senders.add(new GDocsHelper(applicationContext));
        }

        if (OSMHelper.IsOsmAuthorized(applicationContext)) {
            tracer.debug("OSM Sender picked");
            senders.add(new OSMHelper(applicationContext));
        }

        if (AppSettings.isAutoEmailEnabled()) {
            tracer.debug("Email Sender picked");
            senders.add(new AutoEmailHelper());
        }

        DropBoxHelper dh = new DropBoxHelper(applicationContext);

        if (dh.IsLinked()) {
            tracer.debug("DropBox Sender picked");
            senders.add(dh);
        }

        if (AppSettings.isAutoOpenGTSEnabled()) {
            tracer.debug("OpenGTS Sender picked");
            senders.add(new OpenGTSHelper());
        }

        if (AppSettings.isAutoFtpEnabled()) {
            tracer.debug("FTP Sender picked");
            senders.add(new FtpHelper());
        }

        return senders;

    }
}

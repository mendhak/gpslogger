package com.mendhak.gpslogger.senders;

import android.content.Context;
import android.os.Environment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSenderFactory
{

    public static IFileSender GetOsmSender(Context applicationContext, IActionListener callback)
    {
        return new OSMHelper(applicationContext,  callback);
    }

    public static IFileSender GetDropBoxSender(Context applicationContext, IActionListener callback)
    {
        return new DropBoxHelper(applicationContext, callback);
    }
    
    public static IFileSender GetGDocsSender(Context applicationContext, IActionListener callback)
    {
        return new GDocsHelper(applicationContext, callback);
    }
    
    public static IFileSender GetEmailSender(IActionListener callback)
    {
        return new AutoEmailHelper(callback);
    }

    public static void SendFiles(Context applicationContext, IActionListener callback)
    {


        final String currentFileName = Session.getCurrentFileName();

        File gpxFolder = new File(Environment.getExternalStorageDirectory(),
                "GPSLogger");

        if (!gpxFolder.exists())
        {
            callback.OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(gpxFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0)
        {
            callback.OnFailure();
            return;
        }

        if (AppSettings.shouldSendZipFile())
        {
            File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");
            ArrayList<String> filePaths = new ArrayList<String>();

            for (File f : files)
            {
                filePaths.add(f.getAbsolutePath());
            }

            Utilities.LogInfo("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.Zip();

            //files.clear();
            files.add(zipFile);
        }

        List<IFileSender> senders = GetFileSenders(applicationContext, callback);

        for(IFileSender sender : senders)
        {
            sender.UploadFile(files);
        }
    }



    public static List<IFileSender> GetFileSenders(Context applicationContext, IActionListener callback)
    {
        List<IFileSender> senders = new ArrayList<IFileSender>();

        if(GDocsHelper.IsLinked(applicationContext))
        {
            senders.add(new GDocsHelper(applicationContext, callback));
        }

        if(OSMHelper.IsOsmAuthorized(applicationContext))
        {
            senders.add(new OSMHelper(applicationContext, callback));
        }

        if(AppSettings.isAutoEmailEnabled())
        {
            senders.add(new AutoEmailHelper(callback));
        }

        DropBoxHelper dh = new DropBoxHelper(applicationContext, callback);

        if(dh.IsLinked())
        {
            senders.add(dh);
        }

        return senders;

    }
}

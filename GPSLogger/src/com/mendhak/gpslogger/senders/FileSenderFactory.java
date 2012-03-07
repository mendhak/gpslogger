package com.mendhak.gpslogger.senders;

import android.content.Context;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

import java.util.List;

public class FileSenderFactory
{
    public static List<IFileSender> GetSenders()
    {
       return null;
    }
    
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
}

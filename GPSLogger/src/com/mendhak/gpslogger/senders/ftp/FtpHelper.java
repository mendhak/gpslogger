/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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


package com.mendhak.gpslogger.senders.ftp;


import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;

import java.io.*;
import java.util.Date;
import java.util.List;

public class FtpHelper implements IFileSender
{
    IActionListener callback;

    public FtpHelper(IActionListener callback)
    {
        this.callback = callback;
    }

    void TestFtp(String servername, String username, String password, int port, boolean useFtps, String protocol, boolean implicit)
    {
        String data = "GPSLogger for Android, test file.  Generated at " + (new Date()).toLocaleString() + "\r\n";
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());

        Thread t = new Thread(new FtpUploadHandler(callback, servername, port, username, password,
                useFtps, protocol, implicit, in, "gpslogger_test.txt"));
        t.start();
    }

    @Override
    public void UploadFile(List<File> files)
    {


        if (!ValidSettings(AppSettings.getFtpServerName(), AppSettings.getFtpUsername(), AppSettings.getFtpPassword(),
                AppSettings.getFtpPort(), AppSettings.FtpUseFtps(), AppSettings.getFtpProtocol(), AppSettings.FtpImplicit()))
        {
            callback.OnFailure();
        }

        File zipFile = null;

        //Only upload a zip file if it's present
        for (File f : files)
        {
            if (f.getName().contains(".zip"))
            {
                zipFile = f;
                break;
            }
        }

        if (zipFile != null)
        {
            UploadFile(zipFile);
        }
        else
        {
            for (File f : files)
            {
                UploadFile(f);
            }
        }

    }

    public boolean acceptZip() {
        return true;
    }

    public void UploadFile(File f)
    {
        try
        {
            FileInputStream fis = new FileInputStream(f);
            Thread t = new Thread(new FtpUploadHandler(callback, AppSettings.getFtpServerName(), AppSettings.getFtpPort(),
                    AppSettings.getFtpUsername(), AppSettings.getFtpPassword(),
                    AppSettings.FtpUseFtps(), AppSettings.getFtpProtocol(), AppSettings.FtpImplicit(),
                    fis, f.getName()));
            t.start();
        }
        catch (Exception e)
        {
            Utilities.LogError("Could not prepare file for upload.", e);
        }
    }

    @Override
    public boolean accept(File file, String s)
    {
        return true;
    }


    public boolean ValidSettings(String servername, String username, String password, Integer port, boolean useFtps,
                                 String sslTls, boolean implicit)
    {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if (useFtps && (sslTls == null || sslTls.length() <= 0))
        {
            retVal = false;
        }

        return retVal;
    }
}

class FtpUploadHandler implements Runnable
{

    IActionListener helper;
    String server;
    int port;
    String username;
    String password;
    boolean useFtps;
    String protocol;
    boolean implicit;
    InputStream inputStream;
    String fileName;

    public FtpUploadHandler(IActionListener helper, String server, int port, String username,
                            String password, boolean useFtps, String protocol, boolean implicit,
                            InputStream inputStream, String fileName)
    {
        this.helper = helper;
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useFtps = useFtps;
        this.protocol = protocol;
        this.implicit = implicit;
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    @Override
    public void run()
    {
        if (Ftp.Upload(server, username, password, port, useFtps, protocol, implicit, inputStream, fileName))
        {
            helper.OnComplete();
        }
        else
        {
            helper.OnFailure();
        }
    }
}

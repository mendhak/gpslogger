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


import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.senders.IFileSender;
import java.io.ByteArrayInputStream;
import java.io.File;
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

        Thread t = new Thread(new TestFtpHandler(callback, servername, port, username, password,
                useFtps, protocol, implicit));
        t.start();
    }

    @Override
    public void UploadFile(List<File> files)
    {

    }

    @Override
    public boolean accept(File file, String s)
    {
        return false;
    }


    public boolean ValidSettings(String servername, String username, String password, Integer port, boolean useFtps,
                                 String sslTls, boolean implicit)
    {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if(useFtps && (sslTls == null || sslTls.length() <= 0))
        {
            retVal = false;
        }

        return retVal;
    }
}

class TestFtpHandler implements Runnable
{

    IActionListener helper;
    String server;
    int port;
    String username;
    String password;
    boolean useFtps;
    String protocol;
    boolean implicit;

    public TestFtpHandler(IActionListener helper, String server, int port, String username,
                          String password, boolean useFtps, String protocol, boolean implicit)
    {
        this.helper = helper;
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useFtps = useFtps;
        this.protocol = protocol;
        this.implicit = implicit;
    }

    @Override
    public void run()
    {
        String data = "GPSLogger for Android, test file.  Generated at " + (new Date()).toLocaleString() + "\r\n";
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());

        if(Ftp.Upload(server, username,  password,  port, useFtps, protocol,  implicit, in, "gpslogger_test.txt"))
        {
            helper.OnComplete();
        }
        else
        {
            helper.OnFailure();
        }
    }
}

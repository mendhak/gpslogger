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

import android.util.Log;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.senders.IFileSender;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FtpHelper implements IFileSender
{
    IActionListener callback;

    public FtpHelper(IActionListener callback)
    {
        this.callback = callback;
    }

    void TestFtp()
    {
        Thread t = new Thread(new TestFtpHandler(callback));
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


}

class TestFtpHandler implements Runnable
{

    IActionListener helper;

    public TestFtpHandler(IActionListener helper)
    {
        this.helper = helper;
    }


    @Override
    public void run()
    {

        boolean useFtps = false;
        FTPClient client = null;
        String protocol = "SSL";

        //If implicit = true, set port to 990. If false, set to 21.  If useFtps = false, set to 21.
        boolean implicit = true;
        int port = 21;

        try
        {
            Log.v("FTPTEST", "Connecting...");

            if(useFtps)
            {
                client = new FTPSClient(protocol, implicit);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(null, null);
                KeyManager km = kmf.getKeyManagers()[0];
                ((FTPSClient) client).setKeyManager(km);

            }
            else
            {
                client = new FTPClient();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        try {
            client.connect("ftp.secureftp-test.com", port);

            //
            // When login success the login method returns true.
            //
            if (client.login("test", "test"))
            {
                Log.v("FTPTEST", "logged in");
                client.enterLocalPassiveMode();
//                String data = "test data";
//                ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
//                boolean result = client.storeFile("test2.txt", in);
//                in.close();
//                if (result)
//                {
//                    Log.v("FTPTEST", "succeeded in writing file");
//                }
//                else
//                {
//                    Log.v("FTPTEST", "failed to write file");
//                }

                helper.OnComplete();
            }
            else
            {
                Log.e("FTPTEST", "Login fail...");
                helper.OnFailure();
            }

        } catch (IOException e) {
            Log.e("FTPTEST", e.getMessage());
            e.printStackTrace();
            helper.OnFailure();
        } finally {
            try {
                //
                // Closes the connection to the FTP server
                //
                Log.v("FTPTEST", "Logging out...");
                client.logout();
                Log.v("FTPTEST", "Disconnecting...");
                client.disconnect();
            } catch (IOException e) {
                Log.e("FTPTEST", e.getMessage());
                e.printStackTrace();

            }
        }
    }
}

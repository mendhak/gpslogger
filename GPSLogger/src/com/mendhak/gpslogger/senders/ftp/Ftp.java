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


import com.mendhak.gpslogger.common.Utilities;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;

public class Ftp
{
    public static boolean Upload(String server, String username, String password, int port,
                                 boolean useFtps, String protocol, boolean implicit,
                                 InputStream inputStream, String fileName)
    {
        FTPClient client = null;

        try
        {
            if (useFtps)
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


        try
        {
            Utilities.LogDebug("Connecting to FTP");
            client.connect(server, port);

            Utilities.LogDebug("Logging in to FTP server");
            if (client.login(username, password))
            {
                client.enterLocalPassiveMode();

                Utilities.LogDebug("Uploading file to FTP server");
                boolean result = client.storeFile(fileName, inputStream);
                inputStream.close();
                if (result)
                {
                    Utilities.LogDebug("Successfully FTPd file");
                }
                else
                {
                    Utilities.LogDebug("Failed to FTP file");
                }

            }
            else
            {
                Utilities.LogDebug("Could not log in to FTP server");
                return false;
            }

        }
        catch (Exception e)
        {
            Utilities.LogError("Could not connect or upload to FTP server.", e);
        }
        finally
        {
            try
            {
                Utilities.LogDebug("Logging out of FTP server");
                client.logout();

                Utilities.LogDebug("Disconnecting from FTP server");
                client.disconnect();
            }
            catch (Exception e)
            {
                Utilities.LogError("Could not logout or disconnect", e);
            }
        }

        return true;
    }
}

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


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.io.InputStream;

public class Ftp {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(Ftp.class.getSimpleName());

    public static boolean Upload(String server, String username, String password, String directory, int port,
                                 boolean useFtps, String protocol, boolean implicit,
                                 InputStream inputStream, String fileName) {
        FTPClient client = null;

        try {
            if (useFtps) {
                client = new FTPSClient(protocol, implicit);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(null, null);
                KeyManager km = kmf.getKeyManagers()[0];
                ((FTPSClient) client).setKeyManager(km);
            } else {
                client = new FTPClient();
            }

        } catch (Exception e) {
            tracer.error("Could not create FTP Client", e);
            return false;
        }


        try {
            tracer.debug("Connecting to FTP");
            client.connect(server, port);
            showServerReply(client);

            tracer.debug("Logging in to FTP server");
            if (client.login(username, password)) {
                client.enterLocalPassiveMode();
                showServerReply(client);

                tracer.debug("Uploading file to FTP server " + server);

                tracer.debug("Checking for FTP directory " + directory);
                FTPFile[] existingDirectory = client.listFiles(directory);
                showServerReply(client);

                if (existingDirectory.length <= 0) {
                    tracer.debug("Attempting to create FTP directory " + directory);
                    //client.makeDirectory(directory);
                    ftpCreateDirectoryTree(client, directory);
                    showServerReply(client);

                }

                client.changeWorkingDirectory(directory);
                boolean result = client.storeFile(fileName, inputStream);
                inputStream.close();
                showServerReply(client);
                if (result) {
                    tracer.debug("Successfully FTPd file " + fileName);
                } else {
                    tracer.debug("Failed to FTP file " + fileName);
                    return false;
                }

            } else {
                tracer.debug("Could not log in to FTP server");
                return false;
            }

        } catch (Exception e) {
            tracer.error("Could not connect or upload to FTP server.", e);
            return false;
        } finally {
            try {
                tracer.debug("Logging out of FTP server");
                client.logout();
                showServerReply(client);

                tracer.debug("Disconnecting from FTP server");
                client.disconnect();
                showServerReply(client);
            } catch (Exception e) {
                tracer.error("Could not logout or disconnect", e);
                return false;
            }
        }

        return true;
    }


    private static void ftpCreateDirectoryTree(FTPClient client, String dirTree) throws IOException {

        boolean dirExists = true;

        //tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
        String[] directories = dirTree.split("/");
        for (String dir : directories) {
            if (dir.length() > 0) {
                if (dirExists) {
                    dirExists = client.changeWorkingDirectory(dir);
                    showServerReply(client);
                }
                if (!dirExists) {
                    client.makeDirectory(dir);
                    showServerReply(client);
                    client.changeWorkingDirectory(dir);
                    showServerReply(client);
                }
            }
        }
    }

    private static void showServerReply(FTPClient client) {
        String[] replies = client.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                tracer.debug("FTP SERVER: " + aReply);
            }
        }
    }
}

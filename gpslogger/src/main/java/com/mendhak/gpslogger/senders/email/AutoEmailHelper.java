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

package com.mendhak.gpslogger.senders.email;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoEmailHelper implements IActionListener, IFileSender
{

    IActionListener callback;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoEmailHelper.class.getSimpleName());

    public AutoEmailHelper(IActionListener callback)
    {
        this.callback = callback;
    }

    @Override
    public void UploadFile(List<File> files)
    {

        ArrayList<File> filesToSend = new ArrayList<File>();

        //If a zip file exists, remove others
        for (File f : files)
        {
            filesToSend.add(f);

            if (f.getName().contains(".zip"))
            {
                filesToSend.clear();
                filesToSend.add(f);
                break;
            }
        }


        Thread t = new Thread(new AutoSendHandler(filesToSend.toArray(new File[filesToSend.size()]), this));
        t.start();
    }


    void SendTestEmail(String smtpServer, String smtpPort,
                       String smtpUsername, String smtpPassword, boolean smtpUseSsl,
                       String emailTarget, String fromAddress, IActionListener helper)
    {

        Thread t = new Thread(new TestEmailHandler(helper, smtpServer,
                smtpPort, smtpUsername, smtpPassword, smtpUseSsl, emailTarget, fromAddress));
        t.start();
    }


    public void OnComplete()
    {
        // This was a success
        tracer.info("Email sent");

        callback.OnComplete();
    }

    public void OnFailure()
    {
        callback.OnFailure();
    }

    @Override
    public boolean accept(File dir, String name)
    {
        return name.toLowerCase().endsWith(".zip")
                || name.toLowerCase().endsWith(".gpx")
                || name.toLowerCase().endsWith(".kml");
    }

}

class AutoSendHandler implements Runnable
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoSendHandler.class.getSimpleName());
    File[] files;
    private final IActionListener helper;

    public AutoSendHandler(File[] files, IActionListener helper)
    {
        this.files = files;
        this.helper = helper;
    }

    public void run()
    {
        try
        {
            Mail m = new Mail(AppSettings.getSmtpUsername(),
                    AppSettings.getSmtpPassword());

            String csvEmailTargets = AppSettings.getAutoEmailTargets();
            String[] toArr = csvEmailTargets.split(",");

            m.setTo(toArr);
            m.setFrom(AppSettings.getSenderAddress());
            m.setSubject("GPS Log file generated at "
                    + Utilities.GetReadableDateTime(new Date()));
            m.setBody("GPS Log file generated at "
                    + Utilities.GetReadableDateTime(new Date()));

            m.setPort(AppSettings.getSmtpPort());
            m.setSecurePort(AppSettings.getSmtpPort());
            m.setSmtpHost(AppSettings.getSmtpServer());
            m.setSsl(AppSettings.isSmtpSsl());

            for (File f : files)
            {
                m.addAttachment(f.getName(), f.getAbsolutePath());
            }

            tracer.info("Sending email...");

            if (m.send())
            {
                helper.OnComplete();
            }
            else
            {
                helper.OnFailure();
            }
        }
        catch (Exception e)
        {
            helper.OnFailure();
            tracer.error("AutoSendHandler.run", e);
        }

    }

}

class TestEmailHandler implements Runnable
{
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(TestEmailHandler.class.getSimpleName());
    String smtpServer;
    String smtpPort;
    String smtpUsername;
    String smtpPassword;
    boolean smtpUseSsl;
    String csvEmailTargets;
    IActionListener helper;
    String fromAddress;

    public TestEmailHandler(IActionListener helper, String smtpServer,
                            String smtpPort, String smtpUsername, String smtpPassword,
                            boolean smtpUseSsl, String csvEmailTargets, String fromAddress)
    {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpPassword = smtpPassword;
        this.smtpUsername = smtpUsername;
        this.smtpUseSsl = smtpUseSsl;
        this.csvEmailTargets = csvEmailTargets;
        this.helper = helper;
        this.fromAddress = fromAddress;
    }

    public void run()
    {
        try
        {

            Mail m = new Mail(smtpUsername, smtpPassword);

            String[] toArr = csvEmailTargets.split(",");
            m.setTo(toArr);

            if (fromAddress != null && fromAddress.length() > 0)
            {
                m.setFrom(fromAddress);
            }
            else
            {
                m.setFrom(smtpUsername);
            }


            m.setSubject("Test Email from GPSLogger at "
                    + Utilities.GetReadableDateTime(new Date()));
            m.setBody("Test Email from GPSLogger at "
                    + Utilities.GetReadableDateTime(new Date()));

            m.setPort(smtpPort);
            m.setSecurePort(smtpPort);
            m.setSmtpHost(smtpServer);
            m.setSsl(smtpUseSsl);
            m.setDebuggable(true);

            tracer.info("Sending email...");
            if (m.send())
            {
                helper.OnComplete();
            }
            else
            {
                helper.OnFailure();
            }
        }
        catch (Exception e)
        {
            helper.OnFailure();
            tracer.error("AutoSendHandler.run", e);
        }

    }
}

/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.senders.email;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.loggers.Streams;
import com.mendhak.gpslogger.common.network.LocalX509TrustManager;
import de.greenrobot.event.EventBus;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.smtp.*;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


public class AutoEmailJob extends Job {

    private static final Logger LOG = Logs.of(AutoEmailJob.class);
    String smtpServer;
    String smtpPort;
    String smtpUsername;
    String smtpPassword;
    boolean smtpUseSsl;
    String csvEmailTargets;
    String fromAddress;
    String subject;
    String body;
    File[] files;
    static ArrayList<String> smtpServerResponses;
    static UploadEvents.AutoEmail smtpFailureEvent;



    protected AutoEmailJob(String smtpServer,
                           String smtpPort, String smtpUsername, String smtpPassword,
                           boolean smtpUseSsl, String csvEmailTargets, String fromAddress,
                            String subject, String body, File[] files) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(files)));
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpPassword = smtpPassword;
        this.smtpUsername = smtpUsername;
        this.smtpUseSsl = smtpUseSsl;
        this.csvEmailTargets = csvEmailTargets;
        this.fromAddress = fromAddress;
        this.subject = subject;
        this.body = body;
        this.files = files;

        smtpServerResponses = new ArrayList<>();
        smtpFailureEvent = null;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        int port = Strings.toInt(smtpPort,25);

        if (Strings.isNullOrEmpty(fromAddress)) {
            fromAddress = smtpUsername;
        }

        AuthenticatingSMTPClient client = new AuthenticatingSMTPClient();

        try {

            client.addProtocolCommandListener(new ProtocolCommandListener() {
                @Override
                public void protocolCommandSent(ProtocolCommandEvent event) {
                    LOG.debug(event.getMessage());
                    smtpServerResponses.add(event.getMessage());
                }

                @Override
                public void protocolReplyReceived(ProtocolCommandEvent event) {
                    LOG.debug(event.getMessage());
                    smtpServerResponses.add(event.getMessage());
                }
            });

            if(smtpUseSsl){
                client = new AuthenticatingSMTPClient("TLS",true);
            }


            // optionally set a timeout to have a faster feedback on errors
            client.setDefaultTimeout(10 * 1000);
            checkReply(client);
            LOG.debug("Connecting to SMTP Server");
            client.connect(smtpServer, port);
            checkReply(client);
            // you say ehlo  and you specify the host you are connecting from, could be anything
            client.ehlo("localhost");
            checkReply(client);
            // if your host accepts STARTTLS, we're good everything will be encrypted, otherwise we're done here
            LOG.debug("Checking TLS...");

            client.setTrustManager(new LocalX509TrustManager(Networks.getKnownServersStore(AppSettings.getInstance())));
            if(!smtpUseSsl && client.execTLS()){
                client.ehlo("localhost");
            }

            client.auth(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN, smtpUsername, smtpPassword);
            checkReply(client);

            client.setSender(fromAddress);
            checkReply(client);

            String target = csvEmailTargets.split(",")[0];
            client.addRecipient(target);

            SimpleSMTPHeader header = new SimpleSMTPHeader(fromAddress, target, subject);

            //Multiple email targets?
            for (String ccTarget : csvEmailTargets.split(",")) {
                if (!ccTarget.equalsIgnoreCase(target)) {
                    header.addCC(ccTarget);
                    client.addRecipient(ccTarget);
                }
            }

            checkReply(client);

            Writer writer = client.sendMessageData();

            if (writer != null) {

                // Regular email with just a body
                if (files == null || files.length == 0) {
                    header.addHeaderField("Content-Type", "text/plain; charset=UTF-8");
                    writer.write(header.toString());
                    writer.write(body);
                }
                // Attach files in a multipart way
                else {
                    String boundary = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 9);
                    header.addHeaderField("Content-Type", "multipart/mixed; boundary=" + boundary);
                    writer.write(header.toString());

                    writer.write("--" + boundary + "\n");
                    writer.write("Content-Type: text/plain; charset=UTF-8" + "\n\n");
                    writer.write(body);
                    writer.write("\n");

                    attachFilesToWriter(writer, boundary, files);
                    writer.write("--" + boundary + "--\n\n");
                }


                writer.close();
                if (!client.completePendingCommand()) {// failure
                    smtpFailureEvent = new UploadEvents.AutoEmail().failed("Failure to send the email");
                }
                else {
                    LOG.info("Email - file sent");
                    EventBus.getDefault().post(new UploadEvents.AutoEmail().succeeded());
                }
            }
            else {
                smtpFailureEvent = new UploadEvents.AutoEmail().failed("Failure to send the email");
            }

        }
        catch (Exception e) {
            LOG.error("Could not send email ", e);
            smtpFailureEvent = new UploadEvents.AutoEmail().failed("Could not send email " + e.getMessage() , e);
        }
        finally {
            try{
                client.logout();
                client.disconnect();
            }
            catch (Exception ignored) {
            }

            if(smtpFailureEvent != null){
                smtpFailureEvent.smtpMessages = smtpServerResponses;
                if(smtpFailureEvent.smtpMessages.isEmpty()){
                    smtpFailureEvent.smtpMessages = new ArrayList<>(Arrays.asList(client.getReplyStrings()));
                }
                EventBus.getDefault().post(smtpFailureEvent);
            }
        }

    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        LOG.debug("Email job cancelled");
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        LOG.error("Could not send email", throwable);
        return RetryConstraint.CANCEL;
    }


    /**
     * Append the given attachments to the message which is being written by the given writer.
     *
     * @param boundary separates each file attachment
     */
    private static void attachFilesToWriter(Writer writer, String boundary, File[] files) throws IOException {
        for (File f : files) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) f.length());
            FileInputStream inputStream = new FileInputStream(f);
            Streams.copyIntoStream(inputStream, outputStream);

            writer.write("--" + boundary + "\n");
            writer.write("Content-Type: application/" + Files.getMimeTypeFromFileName(f.getName()) + "; name=\"" + f.getName() + "\"\n");
            writer.write("Content-Disposition: attachment; filename=\"" + f.getName() + "\"\n");
            writer.write("Content-Transfer-Encoding: base64\n\n");
            String encodedFile = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
            writer.write(encodedFile);
            writer.write("\n");
        }
    }


    private static void checkReply(SMTPClient sc) throws Exception {
        if (SMTPReply.isNegativeTransient(sc.getReplyCode())) {
            throw new Exception("Transient SMTP error " +  sc.getReplyString());
        } else if (SMTPReply.isNegativePermanent(sc.getReplyCode())) {
            throw new Exception("Permanent SMTP error " +  sc.getReplyString());
        }
    }


    public static String getJobTag(File[] files) {
        StringBuilder sb = new StringBuilder();
        for(File f : files){
            sb.append(f.getName()).append(".");
        }
        return "EMAIL" + sb.toString();

    }
}

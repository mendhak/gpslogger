package com.mendhak.gpslogger.senders.email;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.LocalX509TrustManager;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.loggers.Streams;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SimpleSMTPHeader;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import de.greenrobot.event.EventBus;

public class AutoEmailWorker extends Worker {

    private static final Logger LOG = Logs.of(AutoEmailWorker.class);
    public AutoEmailWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String smtpServer = preferenceHelper.getSmtpServer();
        String smtpPort = preferenceHelper.getSmtpPort();
        String smtpPassword = preferenceHelper.getSmtpPassword();
        String smtpUsername = preferenceHelper.getSmtpUsername();
        boolean smtpUseSsl = preferenceHelper.isSmtpSsl();
        String csvEmailTargets = preferenceHelper.getAutoEmailTargets();
        String fromAddress = preferenceHelper.getSmtpSenderAddress();

        String subject = getInputData().getString("subject");
        String body = getInputData().getString("body");
        String[] fileNames = getInputData().getStringArray("fileNames");

        File[] files = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            files[i] = new File(fileNames[i]);
        }

        int port = Strings.toInt(smtpPort,25);

        if (Strings.isNullOrEmpty(fromAddress)) {
            fromAddress = smtpUsername;
        }

        AuthenticatingSMTPClient client = new AuthenticatingSMTPClient();

        ArrayList<String> smtpServerResponses = new ArrayList<>();
        UploadEvents.AutoEmail smtpFailureEvent = null;

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
                return Result.failure();
            }
        }

        Systems.sendFileUploadedBroadcast(getApplicationContext(), fileNames, "email");

        return Result.success();
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
}

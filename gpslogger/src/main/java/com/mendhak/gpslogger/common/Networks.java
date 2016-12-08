/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of gpslogger.
 *
 * gpslogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * gpslogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gpslogger.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.common;


import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.Html;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class Networks {

    private static final Logger LOG = Logs.of(Networks.class);

    static String LOCAL_TRUSTSTORE_FILENAME = "knownservers.bks";
    static String LOCAL_TRUSTSTORE_PASSWORD = "politelemon";

    public static KeyStore getKnownServersStore(Context context)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        KeyStore mKnownServersStore = KeyStore.getInstance(KeyStore.getDefaultType());
        File localTrustStoreFile = new File(Files.storageFolder(context), LOCAL_TRUSTSTORE_FILENAME);

        LOG.debug("Getting local truststore - " + localTrustStoreFile.getAbsolutePath());
        if (localTrustStoreFile.exists()) {
            InputStream in = new FileInputStream(localTrustStoreFile);
            try {
                mKnownServersStore.load(in, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
            } finally {
                in.close();
            }
        } else {
            // next is necessary to initialize an empty KeyStore instance
            mKnownServersStore.load(null, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        }

        return mKnownServersStore;
    }


    public static void addCertToKnownServersStore(Certificate cert, Context context)
            throws  KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        File localTrustStoreFile = new File(Files.storageFolder(context), LOCAL_TRUSTSTORE_FILENAME);

        KeyStore knownServers = Networks.getKnownServersStore(context);
        LOG.debug("Adding certificate - HashCode: " + cert.hashCode());
        knownServers.setCertificateEntry(Integer.toString(cert.hashCode()), cert);

        FileOutputStream fos = null;

        try {
            //fos = context.openFileOutput(localTrustStoreFile.getName(), Context.MODE_PRIVATE);
            fos = new FileOutputStream(localTrustStoreFile);
            knownServers.store(fos, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        }
        catch(Exception e)
        {
            LOG.error("Could not save certificate", e);
        }
        finally {
            fos.close();
        }
    }

    public static CertificateValidationException extractCertificateValidationException(Exception e) {

        if (e == null) { return null ; }

        CertificateValidationException result = null;

        if (e instanceof CertificateValidationException) {
            return (CertificateValidationException)e;
        }
        Throwable cause = e.getCause();
        Throwable previousCause = null;
        while (cause != null && cause != previousCause && !(cause instanceof CertificateValidationException)) {
            previousCause = cause;
            cause = cause.getCause();
        }
        if (cause != null && cause instanceof CertificateValidationException) {
            result = (CertificateValidationException)cause;
        }
        return result;
    }

    public static SSLSocketFactory getSocketFactory(Context context){
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            LocalX509TrustManager atm = null;

            atm = new LocalX509TrustManager(getKnownServersStore(context));

            TrustManager[] tms = new TrustManager[] { atm };
            sslContext.init(null, tms, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            LOG.error("Could not get SSL Socket factory ", e);
        }

        return null;
    }

    public static void performCertificateValidationWorkflow(Context context, String host, int port, ServerType serverType) {
        Handler postValidationHandler = new Handler();
        Dialogs.progress(context, context.getString(R.string.please_wait), context.getString(R.string.please_wait));
        new Thread(new CertificateFetcher(context, host, port, serverType, postValidationHandler)).start();
    }

    private static void onCertificateFetched(final Context context, Exception e, boolean isValid) {

        Dialogs.hideProgress();

        if (!isValid) {
            final CertificateValidationException cve = Networks.extractCertificateValidationException(e);

            if (cve != null) {
                LOG.debug("Untrusted certificate found, " + cve.getCertificate().toString());
                try {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cve.getMessage());
                    sb.append("<br /><br /><strong>").append("Subject: ").append("</strong>").append(cve.getCertificate().getSubjectDN().getName());
                    sb.append("<br /><br /><strong>").append("Issuer: ").append("</strong>").append(cve.getCertificate().getIssuerDN().getName());
                    sb.append("<br /><br /><strong>").append("Fingerprint: ").append("</strong>").append(DigestUtils.shaHex(cve.getCertificate().getEncoded()));
                    sb.append("<br /><br /><strong>").append("Issued on: ").append("</strong>").append(cve.getCertificate().getNotBefore());
                    sb.append("<br /><br /><strong>").append("Expires on: ").append("</strong>").append(cve.getCertificate().getNotBefore());

                    new MaterialDialog.Builder(context)
                            .title("Add this certificate to local keystore?")
                            .content(Html.fromHtml(sb.toString()))
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        Networks.addCertToKnownServersStore(cve.getCertificate(), context.getApplicationContext());
                                        Dialogs.alert("",context.getString(R.string.restart_required),context);
                                    } catch (Exception e) {
                                        LOG.error("Could not add to the keystore", e);
                                    }

                                    dialog.dismiss();
                                }
                            }).show();

                } catch (Exception e1) {
                    LOG.error("Could not get fingerprint of certificate", e1);
                }

            } else {
                LOG.error("Error while attempting to fetch server certificate", e);
                Dialogs.error(context.getString(R.string.error), "Error while attempting to fetch server certificate", e!= null ? e.getMessage(): "", e, context);
            }
        } else {
            Dialogs.alert(context.getString(R.string.success), "The certificate is valid or in the local keystore." , context);
        }
    }

    public enum ServerType {
        HTTPS,
        FTP,
        SMTP
    }

    private static class CertificateFetcher implements Runnable {

        Handler postValidationHandler;
        Context context;
        String host;
        int port;
        ServerType serverType;

        CertificateFetcher(Context context, String host, int port, ServerType serverType, Handler postValidationHandler) {
            this.context = context;
            this.host = host;
            this.port = port;
            this.serverType = serverType;
            this.postValidationHandler = postValidationHandler;

        }

        @Override
        public void run() {
            try {

                LOG.debug("Beginning certificate validation - will connect directly to {} port {}", host, String.valueOf(port));

                if (serverType == ServerType.HTTPS) {
                    LOG.debug("HTTPS type server, attempting SSL handshake");
                    connectToSSLSocket(null);
                    postValidationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            onCertificateFetched(context, null, true);
                        }
                    });
                } else {

                    String command = "", regexToMatch = "";
                    if (serverType == ServerType.FTP) {
                        LOG.debug("FTP type server");
                        command = "AUTH SSL\r\n";
                        regexToMatch = "(?:234.*)";
                    } else if (serverType == ServerType.SMTP) {
                        LOG.debug("SMTP type server");
                        command = "STARTTLS\r\n";
                        regexToMatch = "(?i:220 .* Ready.*)";
                    }

                    try {
                        LOG.debug("Trying handshake first in case the socket is SSL/TLS only");
                        connectToSSLSocket(null);
                        postValidationHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onCertificateFetched(context, null, true);
                            }
                        });
                    } catch (Exception e) {

                        if (Networks.extractCertificateValidationException(e) != null) {
                            throw e;
                        }

                        LOG.debug("Direct connection failed or no certificate was presented", e);

                        LOG.debug("Attempting to connect over plain socket");
                        Socket plainSocket = new Socket(host, port);
                        plainSocket.setSoTimeout(30000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(plainSocket.getInputStream()));
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(plainSocket.getOutputStream()));
                        String line;

                        if(serverType==ServerType.SMTP){
                            LOG.debug("CLIENT: EHLO localhost");
                            writer.write("EHLO localhost\r\n");
                            writer.flush();
                            line = reader.readLine();
                            LOG.debug("SERVER: " + line);
                        }

                        LOG.debug("CLIENT: " + command);
                        LOG.debug("(Expecting {} in response)", regexToMatch);
                        writer.write(command);
                        writer.flush();
                        while ((line = reader.readLine()) != null) {
                            LOG.debug("SERVER: " + line);
                            if (line.matches(regexToMatch)) {
                                LOG.debug("Elevating socket and attempting handshake");
                                connectToSSLSocket(plainSocket);
                                postValidationHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onCertificateFetched(context, null, true);
                                    }
                                });
                                return;
                            }
                        }

                        LOG.debug("No certificates found.  Giving up.");
                        postValidationHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onCertificateFetched(context, null, false);
                            }
                        });
                    }

                }

            } catch (final Exception e) {

                LOG.debug("",e);
                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onCertificateFetched(context, e, false);
                    }
                });
            }

        }

        private void connectToSSLSocket(Socket plainSocket) throws IOException {
            SSLSocketFactory factory = Networks.getSocketFactory(context);
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            if(plainSocket!=null){
                socket = (SSLSocket)factory.createSocket(plainSocket,host,port,true);
            }

            if(serverType == ServerType.SMTP){
                socket.setUseClientMode(true);
                socket.setNeedClientAuth(true);
            }

            socket.setSoTimeout(5000);
            LOG.debug("Starting handshake...");
            socket.startHandshake();
            SSLSession session = socket.getSession();
            Certificate[] servercerts = session.getPeerCertificates();

        }
    }


}

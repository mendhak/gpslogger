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

package com.mendhak.gpslogger.common.network;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.Html;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.ui.Dialogs;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.cert.Certificate;

public class CertificateValidationWorkflow implements Runnable {

    Handler postValidationHandler;
    Context context;
    String host;
    int port;
    ServerType serverType;

    private static final Logger LOG = Logs.of(CertificateValidationWorkflow.class);

    CertificateValidationWorkflow(Context context, String host, int port, ServerType serverType, Handler postValidationHandler) {
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

            try {
                LOG.debug("Trying handshake first in case the socket is SSL/TLS only");
                connectToSSLSocket(null);
                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onWorkflowFinished(context, null, true);
                    }
                });
            } catch (final Exception e) {

                if (Networks.extractCertificateValidationException(e) != null) {
                    throw e;
                }

                LOG.debug("Direct connection failed or no certificate was presented", e);

                if(serverType== ServerType.HTTPS){
                    postValidationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            onWorkflowFinished(context, e, false);
                        }
                    });
                    return;
                }

                LOG.debug("Now attempting to connect over plain socket");
                Socket plainSocket = new Socket(host, port);
                plainSocket.setSoTimeout(30000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(plainSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(plainSocket.getOutputStream()));
                String line;

                if (serverType == ServerType.SMTP) {
                    LOG.debug("CLIENT: EHLO localhost");
                    writer.write("EHLO localhost\r\n");
                    writer.flush();
                    line = reader.readLine();
                    LOG.debug("SERVER: " + line);
                }

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

                LOG.debug("CLIENT: " + command);
                LOG.debug("(Expecting regex {} in response)", regexToMatch);
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
                                onWorkflowFinished(context, null, true);
                            }
                        });
                        return;
                    }
                }

                LOG.debug("No certificates found.  Giving up.");
                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onWorkflowFinished(context, null, false);
                    }
                });
            }

        } catch (final Exception e) {

            LOG.debug("",e);
            postValidationHandler.post(new Runnable() {
                @Override
                public void run() {
                    onWorkflowFinished(context, e, false);
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

    private static void onWorkflowFinished(final Context context, Exception e, boolean isValid) {

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
                    sb.append("<br /><br /><strong>").append("Expires on: ").append("</strong>").append(cve.getCertificate().getNotAfter());

                    new MaterialDialog.Builder(context)
                            .title(R.string.ssl_certificate_add_to_keystore)
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
            Dialogs.alert(context.getString(R.string.success), context.getString(R.string.ssl_certificate_is_valid) , context);
        }
    }
}

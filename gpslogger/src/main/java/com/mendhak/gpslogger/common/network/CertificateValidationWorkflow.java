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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.FragmentActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.SimpleTLSValidationDialog;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.List;

public class CertificateValidationWorkflow implements Runnable {

    Handler postValidationHandler;
    Activity activity;
    String host;
    int port;
    ServerType serverType;

    private static final Logger LOG = Logs.of(CertificateValidationWorkflow.class);

    CertificateValidationWorkflow(Activity activity, String host, int port, ServerType serverType, Handler postValidationHandler) {
        this.activity = activity;
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
                        onWorkflowFinished(activity, null, true);
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
                            onWorkflowFinished(activity, e, false);
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
                                onWorkflowFinished(activity, null, true);
                            }
                        });
                        return;
                    }
                }

                LOG.debug("No certificates found.  Giving up.");
                postValidationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onWorkflowFinished(activity, null, false);
                    }
                });
            }

        } catch (final Exception e) {

            LOG.debug("",e);
            postValidationHandler.post(new Runnable() {
                @Override
                public void run() {
                    onWorkflowFinished(activity, e, false);
                }
            });
        }

    }

    private void connectToSSLSocket(Socket plainSocket) throws IOException {
        SSLSocketFactory factory = Networks.getSocketFactory(activity);
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

    private static void onWorkflowFinished(final Activity activity, Exception e, boolean isValid) {

        Dialogs.hideProgress();

        if (!isValid) {
            final CertificateValidationException cve = Networks.extractCertificateValidationException(e);

            if (cve != null) {
                LOG.debug("Untrusted certificate found, " + cve.getCertificate().toString());
                try {

                    StringBuilder sans = new StringBuilder();
                    if(cve.getCertificate().getSubjectAlternativeNames() != null && cve.getCertificate().getSubjectAlternativeNames().size() > 0){
                        for(List item : cve.getCertificate().getSubjectAlternativeNames()){
                            if((int)item.get(0) == 2 || (int)item.get(0) == 7){ //Alt Name type DNS or IP
                                sans.append(String.format("<br /><font face='monospace'>%s</font>", item.get(1).toString()));
                            }
                        }
                    }

                    final StringBuilder sb = new StringBuilder();
                    sb.append(cve.getMessage());
                    String msgformat = "<br /><br /><strong>%s: </strong><font face='monospace'>%s</font>";
                    sb.append(String.format(msgformat, "Subject", cve.getCertificate().getSubjectDN().getName()));
                    if(sans.length() > 0) { sb.append(String.format(msgformat, "Subject Alternative Names",sans)); }
                    sb.append(String.format(msgformat,"Issuer", cve.getCertificate().getIssuerDN().getName()));
                    sb.append(String.format(msgformat,"Fingerprint", DigestUtils.shaHex(cve.getCertificate().getEncoded())));
                    sb.append(String.format(msgformat,"Issued on",cve.getCertificate().getNotBefore()));
                    sb.append(String.format(msgformat,"Expires on",cve.getCertificate().getNotAfter()));

                    Bundle b = new Bundle();
                    b.putSerializable("CERT",cve.getCertificate());

                    SimpleTLSValidationDialog.build()
                            .title(R.string.ssl_certificate_add_to_keystore)
                            .pos(R.string.ok)
                            .neg(R.string.cancel)
                            .msgHtml(sb.toString())
                            .extra(b)
                            .show((FragmentActivity)activity);

                } catch (Exception e1) {
                    LOG.error("Could not get fingerprint of certificate", e1);
                }

            } else {
                LOG.error("Error while attempting to fetch server certificate", e);
                Dialogs.showError(activity.getString(R.string.error), "Error while attempting to fetch server certificate", e!= null ? e.getMessage(): "", e, (FragmentActivity) activity);
            }
        } else {
            Dialogs.alert(activity.getString(R.string.success), activity.getString(R.string.ssl_certificate_is_valid) , activity);
        }
    }
}

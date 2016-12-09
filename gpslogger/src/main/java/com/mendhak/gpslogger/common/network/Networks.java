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
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import org.slf4j.Logger;
import javax.net.ssl.*;
import java.io.*;
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

    public static void beginCertificateValidationWorkflow(Context context, String host, int port, ServerType serverType) {
        Handler postValidationHandler = new Handler();
        Dialogs.progress(context, context.getString(R.string.please_wait), context.getString(R.string.please_wait));
        new Thread(new CertificateValidationWorkflow(context, host, port, serverType, postValidationHandler)).start();
    }


}

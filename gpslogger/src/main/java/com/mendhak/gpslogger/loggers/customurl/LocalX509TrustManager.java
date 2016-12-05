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

package com.mendhak.gpslogger.loggers.customurl;


import com.mendhak.gpslogger.common.CertificateValidationException;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.slf4j.Logger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author David A. Velasco
 * @author Mendhak
 */
public class LocalX509TrustManager implements X509TrustManager {

    private static final Logger LOG = Logs.of(LocalX509TrustManager.class);

    private X509TrustManager standardTrustManager = null;
    private KeyStore knownServersKeyStore;

    /**
     * Constructor for LocalX509TrustManager
     *
     * @param  knownServersKeyStore    Local certificates store with server certificates explicitly trusted by the user.
     * @throws CertStoreException       When no default X509TrustManager instance was found in the system.
     */
    public LocalX509TrustManager(KeyStore knownServersKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException, CertStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore)null);
        standardTrustManager = findX509TrustManager(factory);

        this.knownServersKeyStore = knownServersKeyStore;
    }



    /**
     * Locates the first X509TrustManager provided by a given TrustManagerFactory
     * @param factory               TrustManagerFactory to inspect in the search for a X509TrustManager
     * @return                      The first X509TrustManager found in factory.
     * @throws CertStoreException   When no X509TrustManager instance was found in factory
     */
    private X509TrustManager findX509TrustManager(TrustManagerFactory factory) throws CertStoreException {
        TrustManager tms[] = factory.getTrustManagers();
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager) {
                return (X509TrustManager) tms[i];
            }
        }
        return null;
    }


    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],
     *      String authType)
     */
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        standardTrustManager.checkClientTrusted(certificates, authType);
    }


    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
     *      String authType)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {

        if (!isKnownServer(certificates[0])) {

            try {
                certificates[0].checkValidity();
            }
            catch (CertificateExpiredException c) {
                throw new CertificateValidationException(certificates[0], "Certificate is expired", c);
            }
            catch (CertificateNotYetValidException c) {
                throw new CertificateValidationException(certificates[0], "Certificates is not yet valid", c);
            }

            try {
                standardTrustManager.checkServerTrusted(certificates, authType);
            }
            catch (CertificateException c) {
                Throwable cause = c.getCause();
                Throwable previousCause = null;
                while (cause != null && cause != previousCause && !(cause instanceof CertPathValidatorException)) {
                    previousCause = cause;
                    cause = cause.getCause();
                }

                if (cause != null && cause instanceof CertPathValidatorException) {
                    throw new CertificateValidationException(certificates[0], "Certificate path validation error", c);
                }
                else {
                    throw new CertificateValidationException(certificates[0], "Certificate is not valid, unknown reason", c);
                }
            }

        }
    }


    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return standardTrustManager.getAcceptedIssuers();
    }


    public boolean isKnownServer(X509Certificate cert) {
        try {
            LOG.debug("Checking for certificate - HashCode: " + cert.hashCode() + " " + Boolean.toString(knownServersKeyStore.isCertificateEntry(Integer.toString(cert.hashCode()))));
            return (knownServersKeyStore.isCertificateEntry(Integer.toString(cert.hashCode())));
        }
        catch (KeyStoreException e) {
            LOG.error("Fail while checking certificate in the known-servers store",e);
            return false;
        }
    }




}
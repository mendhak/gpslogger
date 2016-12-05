///*
// * Copyright (C) 2016 mendhak
// *
// * This file is part of GPSLogger for Android.
// *
// * GPSLogger for Android is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 2 of the License, or
// * (at your option) any later version.
// *
// * GPSLogger for Android is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package com.mendhak.gpslogger.senders.owncloud;
//
///* ownCloud Android Library is available under MIT license
// *   Copyright (C) 2015 ownCloud Inc.
// *
// *   Permission is hereby granted, free of charge, to any person obtaining a copy
// *   of this software and associated documentation files (the "Software"), to deal
// *   in the Software without restriction, including without limitation the rights
// *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// *   copies of the Software, and to permit persons to whom the Software is
// *   furnished to do so, subject to the following conditions:
// *
// *   The above copyright notice and this permission notice shall be included in
// *   all copies or substantial portions of the Software.
// *
// *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
// *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
// *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// *   THE SOFTWARE.
// *
// */
//
//import com.mendhak.gpslogger.common.AppSettings;
//import com.mendhak.gpslogger.common.Networks;
//import com.mendhak.gpslogger.loggers.customurl.LocalX509TrustManager;
//import com.owncloud.android.lib.common.network.AdvancedSslSocketFactory;
//import org.apache.commons.httpclient.ConnectTimeoutException;
//import org.apache.commons.httpclient.params.HttpConnectionParams;
//import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
//
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManager;
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.net.UnknownHostException;
//import java.security.GeneralSecurityException;
//
//
///**
// * SelfSignedConfidentSslSocketFactory allows to create SSL {@link Socket}s
// * that accepts self-signed server certificates.
// *
// * WARNING: this SHOULD NOT be used in productive environments.
// *
// * @author David A. Velasco
// * @author Mendhak
// *
// */
//
//public class SelfSignedConfidentSslSocketFactory implements SecureProtocolSocketFactory {
//
//
//    //private SSLContext mSslContext = null;
//    private AdvancedSslSocketFactory wrappedSslSocketFactory = null;
//
//
//    /**
//     * Constructor for SelfSignedConfidentSslSocketFactory.
//     * @throws GeneralSecurityException
//     */
//    public SelfSignedConfidentSslSocketFactory() throws GeneralSecurityException, IOException {
//        SSLContext sslContext = SSLContext.getInstance("TLS");
//        sslContext.init(
//                null,
//                new TrustManager[] { new LocalX509TrustManager(Networks.getKnownServersStore(AppSettings.getInstance())) },
//                null
//        );
//        wrappedSslSocketFactory = new AdvancedSslSocketFactory(sslContext, null, null);
//    }
//
//
//    /**
//     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
//     */
//    @Override
//    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
//        return wrappedSslSocketFactory.createSocket(host, port);
//    }
//
//    /**
//     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
//     */
//    @Override
//    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
//            throws IOException, UnknownHostException {
//        return wrappedSslSocketFactory.createSocket(host, port, clientHost, clientPort);
//    }
//
//    /**
//     * Attempts to get a new socket connection to the given host within the given time limit.
//     *
//     * @param host 			The host name/IP
//     * @param port 			The port on the host
//     * @param localAddress 	The local host name/IP to bind the socket to
//     * @param localPort 	The port on the local machine
//     * @param params 		{@link HttpConnectionParams} HTTP connection parameters.
//     *
//     * @return Socket 		A new socket
//     *
//     * @throws IOException if an I/O error occurs while creating the socket
//     * @throws UnknownHostException if the IP address of the host cannot be determined
//     */
//    @Override
//    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
//                               HttpConnectionParams params) throws IOException, UnknownHostException,
//            ConnectTimeoutException {
//
//        return wrappedSslSocketFactory.createSocket(host, port, localAddress, localPort, params);
//    }
//
//    /**
//     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
//     */
//    @Override
//    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
//            throws IOException, UnknownHostException {
//        return wrappedSslSocketFactory.createSocket(socket, host, port, autoClose);
//    }
//
//}
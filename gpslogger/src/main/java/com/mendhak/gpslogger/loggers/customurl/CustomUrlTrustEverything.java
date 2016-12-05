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
//package com.mendhak.gpslogger.loggers.customurl;
//
//import javax.net.ssl.*;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//
////This class allows HttpsUrlConnection to trust self signed certificates
//public class CustomUrlTrustEverything {
//
//    public static SSLSocketFactory getSSLContextSocketFactory() {
//        TrustManager[] trustManager = new TrustManager[] {new CustomUrlTrustEverything.TrustEverythingTrustManager()};
//
//        SSLContext sslContext;
//        try {
//            sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManager, new java.security.SecureRandom());
//            return sslContext.getSocketFactory();
//        }
//        catch (NoSuchAlgorithmException | KeyManagementException ignored) {}
//
//        return null;
//
//    }
//
//    public static class TrustEverythingTrustManager implements X509TrustManager {
//        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//            return new java.security.cert.X509Certificate[]{};
//        }
//
//        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {   }
//
//        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {   }
//    }
//
//    public static class VerifyEverythingHostnameVerifier implements HostnameVerifier {
//
//        public boolean verify(String string, SSLSession sslSession) {
//            return true;
//        }
//    }
//
//
//}

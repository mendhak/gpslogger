package com.mendhak.gpslogger.loggers.customurl;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

//This class allows HttpsUrlConnection to trust self signed certificates
public class CustomUrlTrustEverything {

    public static SSLSocketFactory GetSSLContextSocketFactory() {
        TrustManager[] trustManager = new TrustManager[] {new CustomUrlTrustEverything.TrustEverythingTrustManager()};

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManager, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        }
        catch (NoSuchAlgorithmException | KeyManagementException ignored) {}

        return null;

    }

    public static class TrustEverythingTrustManager implements X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {   }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {   }
    }

    public static class VerifyEverythingHostnameVerifier implements HostnameVerifier {

        public boolean verify(String string, SSLSession sslSession) {
            return true;
        }
    }


}

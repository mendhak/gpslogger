package com.mendhak.gpslogger.common.network;

import java.net.URI;

/**
 * Checks whether a URL points at the local machine or a private network, based
 * only on the literal host text. Hostnames such as example.com are not resolved
 * via DNS and therefore return false.
 *
 * Used by CustomUrlManager to decide whether WorkManager needs network
 * connectivity (internal URLs can run offline).
 *
 * Internal means localhost, IPv4 loopback (127.0.0.0/8, not part of RFC 1918)
 * or the three private ranges from RFC 1918:
 * https://datatracker.ietf.org/doc/html/rfc1918
 * (10.0.0.0/8, 172.16.0.0/12 and 192.168.0.0/16).
 */
public final class InternalAddresses {

    private static final long NOT_IPV4 = -1L;

    private InternalAddresses() {
    }

    /**
     * Returns true if the host component of the URL is an internal address.
     */
    public static boolean isInternalUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String host;
        try {
            host = new URI(url).getHost();
        } catch (Exception e) {
            return false;
        }

        if (host == null || host.isEmpty()) {
            return false;
        }
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }
        return isPrivateIpv4(host);
    }

    static boolean isPrivateIpv4(String host) {
        long ip = parseIpv4(host);
        if (ip == NOT_IPV4) {
            return false;
        }
        int first = (int) ((ip >>> 24) & 0xFF);
        int second = (int) ((ip >>> 16) & 0xFF);

        return first == 127                                   // loopback
                || first == 10                                // RFC 1918: 10.0.0.0/8
                || (first == 172 && second >= 16 && second <= 31) // RFC 1918: 172.16.0.0/12
                || (first == 192 && second == 168);           // RFC 1918: 192.168.0.0/16
    }

    private static long parseIpv4(String host) {
        long packed = 0;
        int octet = 0;
        int digits = 0;
        int dots = 0;

        for (int i = 0, length = host.length(); i < length; i++) {
            char c = host.charAt(i);
            if (c == '.') {
                if (digits == 0 || ++dots > 3) {
                    return NOT_IPV4; // reject empty octets such as "10..0.1" or trailing dots
                }
                packed = (packed << 8) | octet;
                octet = 0;
                digits = 0;
            } else if (c >= '0' && c <= '9') {
                if (++digits > 3) {
                    return NOT_IPV4; // octets have at most 3 digits, e.g. reject "0010.0.0.1"
                }
                octet = octet * 10 + (c - '0');
                if (octet > 255) {
                    return NOT_IPV4; // octet out of range, e.g. reject "256.0.0.1"
                }
            } else {
                return NOT_IPV4;
            }
        }

        if (dots != 3 || digits == 0) {
            return NOT_IPV4; // need exactly four non-empty octets
        }
        return (packed << 8) | octet;
    }
}

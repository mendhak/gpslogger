package com.mendhak.gpslogger.common.network;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InternalAddressesTest {

    @Test
    public void isInternalUrl_WhenNullOrEmpty_ReturnsFalse() {
        assertThat("Null URL is not internal", InternalAddresses.isInternalUrl(null), is(false));
        assertThat("Empty URL is not internal", InternalAddresses.isInternalUrl(""), is(false));
    }

    @Test
    public void isInternalUrl_WhenInternalAddress_ReturnsTrue() {
        assertThat("localhost is internal", InternalAddresses.isInternalUrl("http://localhost/test"), is(true));
        assertThat("127.0.0.1 is internal", InternalAddresses.isInternalUrl("http://127.0.0.1/test"), is(true));
        assertThat("10.0.0.0/8 is internal", InternalAddresses.isInternalUrl("http://10.0.0.1/test"), is(true));
        assertThat("172.16.5.2 is internal", InternalAddresses.isInternalUrl("http://172.16.5.2/test"), is(true));
        assertThat("172.16.0.0/12 is internal", InternalAddresses.isInternalUrl("http://172.31.255.255/test"), is(true));
        assertThat("192.168.0.0/16 is internal", InternalAddresses.isInternalUrl("http://192.168.1.65:8000/test"), is(true));
    }

    @Test
    public void isInternalUrl_WhenPublicAddressOrHostname_ReturnsFalse() {
        assertThat("Public address is not internal", InternalAddresses.isInternalUrl("http://8.8.8.8/test"), is(false));
        assertThat("Outside 172.16.0.0/12 is not internal", InternalAddresses.isInternalUrl("http://172.32.0.1/test"), is(false));
        assertThat("Hostname is not resolved, so not internal", InternalAddresses.isInternalUrl("http://example.com/test"), is(false));
    }

    @Test
    public void isPrivateIpv4_WhenValidPrivateAddress_ReturnsTrue() {
        assertThat("Loopback is private", InternalAddresses.isPrivateIpv4("127.0.0.1"), is(true));
        assertThat("10.0.0.0/8 is private", InternalAddresses.isPrivateIpv4("10.0.0.1"), is(true));
        assertThat("172.16.0.0/12 is private", InternalAddresses.isPrivateIpv4("172.16.5.2"), is(true));
        assertThat("192.168.0.0/16 is private", InternalAddresses.isPrivateIpv4("192.168.1.1"), is(true));
    }

    @Test
    public void isPrivateIpv4_WhenMalformedLiteral_ReturnsFalse() {
        assertThat("Empty octet is rejected", InternalAddresses.isPrivateIpv4("10..0.1"), is(false));
        assertThat("Trailing dot is rejected", InternalAddresses.isPrivateIpv4("10.0.0."), is(false));
        assertThat("Leading dot is rejected", InternalAddresses.isPrivateIpv4(".10.0.0"), is(false));
        assertThat("Only dots is rejected", InternalAddresses.isPrivateIpv4("10..."), is(false));
        assertThat("Octet out of range is rejected", InternalAddresses.isPrivateIpv4("10.0.0.256"), is(false));
        assertThat("Too few octets is rejected", InternalAddresses.isPrivateIpv4("10.0.0"), is(false));
        assertThat("Too many octets is rejected", InternalAddresses.isPrivateIpv4("10.0.0.1.5"), is(false));
        assertThat("Empty string is rejected", InternalAddresses.isPrivateIpv4(""), is(false));
    }

    @Test
    public void isPrivateIpv4_WhenOctetHasTooManyDigits_ReturnsFalse() {
        assertThat("Zero-padded first octet is rejected", InternalAddresses.isPrivateIpv4("0010.0.0.1"), is(false));
        assertThat("Zero-padded middle octet is rejected", InternalAddresses.isPrivateIpv4("172.0016.0.1"), is(false));
        assertThat("Zero-padded last octet is rejected", InternalAddresses.isPrivateIpv4("192.168.1.0001"), is(false));
        assertThat("Four-digit octet is rejected", InternalAddresses.isPrivateIpv4("1270.0.0.1"), is(false));
    }
}

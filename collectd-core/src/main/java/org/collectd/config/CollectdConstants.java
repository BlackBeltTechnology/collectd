package org.collectd.config;

/**
 * Collectd constants. More details: https://collectd.org/wiki/index.php/Binary_protocol
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class CollectdConstants {

    /**
     * Default IPv4 multicast address of Collectd server.
     */
    public static final String DEFAULT_IPV4_ADDRESS = "239.192.74.66";

    /**
     * Default IPv6 multicast address of Collectd server.
     */
    public static final String DEFAULT_IPV6_ADDRESS = "ff18::efc0:4a42";

    /**
     * Default UDP port of Collectd server.
     */
    public static final int DEFAULT_UDP_PORT = 25826;

    /**
     * Default packet size. In versions 4.0 through 4.7, the receive buffer had a fixed size of 1024 bytes. When longer packets are received, the trailing data
     * is simply ignored. Since version 4.8, the buffer size can be configured.
     */
    public static final int DEFAULT_PACKET_SIZE = 1024;

    /**
     * Maximum buffer size.
     */
    public static final int MAX_PACKET_SIZE = 1452;
}

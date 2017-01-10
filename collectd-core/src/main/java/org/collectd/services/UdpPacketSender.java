package org.collectd.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.collectd.config.CollectdConstants;
import org.collectd.model.Notification;
import org.collectd.model.PluginData;
import org.collectd.model.Values;
import org.collectd.protocol.UdpBufferWriter;

/**
 * UDP packet writer for Collectd.
 */
@Slf4j
public class UdpPacketSender {

    private UdpBufferWriter writer;

    private final InetSocketAddress server;

    private DatagramSocket socket;
    private MulticastSocket mcast;

    private String client;

    private final AtomicLong bytesSent = new AtomicLong(0L);
    private final AtomicInteger packetsSent = new AtomicInteger(0);

    /**
     * Create new UDP packet writer instance. Default packet size is used.
     *
     * @param server Collectd server address
     */
    public UdpPacketSender(final InetSocketAddress server) {
        this(server, null, CollectdConstants.DEFAULT_PACKET_SIZE);
    }

    /**
     * Create new UDP packet writer instance.
     *
     * @param server Collectd server address
     * @param clientHost client hostname
     * @param packetSize packet size
     */
    public UdpPacketSender(final InetSocketAddress server, final String clientHost, final int packetSize) {
        this.server = server;
        this.client = clientHost;
        writer = new UdpBufferWriter(packetSize);
    }

    /**
     * Write value list. Signature and encrypted parts are not supported yet.
     *
     * @param values numeric value list
     * @throws IOException unable to write value to output stream
     */
    public void send(final Values values) throws IOException {
        setDefaults(values);

        final byte[] bufferToSend = writer.checkSpace(values);
        if (bufferToSend != null) {
            flush(bufferToSend);
        }

        writer.writeValuesPart(values);
    }

    /**
     * Write notification. Signature and encrypted parts are not supported yet.
     *
     * @param notification notification
     * @throws IOException unable to write value to output stream
     */
    public void send(final Notification notification) throws IOException {
        setDefaults(notification);

        final byte[] bufferToSend = writer.checkSpace(notification);
        if (bufferToSend != null) {
            flush(bufferToSend);
        }

        writer.writeNotificationPart(notification);
    }

    /**
     * Get number of bytes sent to Collectd server.
     * 
     * @return number of sent bytes
     */
    public long getBytesSent() {
        return bytesSent.longValue();
    }

    /**
     * Get number of packets sent to Collectd server.
     * 
     * @return number of sent packets
     */
    public int getPacketsSent() {
        return packetsSent.intValue();
    }

    /**
     * Flush buffer.
     * 
     * @throws IOException unable to write buffer
     */
    public void flush() throws IOException {
        flush(writer.getBuffer());
    }

    private void flush(final byte[] buffer) throws IOException {
        final int length = buffer != null ? buffer.length : 0;
        if (length == 0) {
            return;
        }

        bytesSent.addAndGet(length);
        packetsSent.incrementAndGet();

        if (log.isDebugEnabled()) {
            log.debug("Sending UDP packet, buffer length: " + length);
        }
        if (log.isTraceEnabled()) {
            log.trace("Destination host: " + server.getHostString());
            log.trace("Destination port: " + server.getPort());
            log.trace("Buffer data: " + Arrays.toString(buffer));
        }

        try {
            final DatagramPacket packet = new DatagramPacket(buffer, length, server);
            if (server.getAddress().isMulticastAddress()) {
                getMulticastSocket().send(packet);
            } else {
                getSocket().send(packet);
            }
        } catch (IllegalArgumentException ex) {
            log.debug("Unable to send metrics", ex);
        }
    }

    private String getClient() {
        if (client == null) {
            try {
                client = InetAddress.getLocalHost().getHostName();
            } catch (IOException ex) {
                log.error("Unable to get host name", ex);
                client = "unknown";
            }
        }
        return client;
    }

    private void setDefaults(final PluginData data) {
        if (data.getHost() == null) {
            data.setHost(getClient());
        }
        if (data.getTime() <= 0) {
            data.setTime(System.currentTimeMillis());
        }
    }

    private DatagramSocket getSocket() throws SocketException {
        if (socket == null) {
            socket = new DatagramSocket();
        }
        return socket;
    }

    private MulticastSocket getMulticastSocket() throws IOException {
        if (mcast == null) {
            mcast = new MulticastSocket();
            mcast.setTimeToLive(1);
        }
        return mcast;
    }
}

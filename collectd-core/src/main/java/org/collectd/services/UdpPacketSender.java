package org.collectd.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Arrays;
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
     * @param valueList numeric value list
     * @throws IOException unable to write value to output stream
     */
    public void send(final Values valueList) throws IOException {
        setDefaults(valueList);
        
        writer.writeKeyParts(valueList);
        writer.writeValuesPart(valueList.getItems(), valueList.getInterval());

        flush();
    }

    /**
     * Write notification. Signature and encrypted parts are not supported yet.
     *
     * @param notification notification
     * @throws IOException unable to write value to output stream
     */
    public void send(final Notification notification) throws IOException {
        setDefaults(notification);
        
        writer.writeKeyParts(notification);
        writer.writeNotificationPart(notification.getSeverity(), notification.getMessage());

        flush();
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

    private void flush() throws IOException {
        final byte[] buffer = writer.getBuffer();
        final int length = buffer.length;
        if (length == 0) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Sending UDP packet, buffer length: " + length);
        }
        if (log.isTraceEnabled()) {
            log.trace("Destination host: " + server.getHostString());
            log.trace("Destination port: " + server.getPort());
            log.trace("Buffer data: " + Arrays.toString(buffer));
        }

        final DatagramPacket packet = new DatagramPacket(buffer, length, server);
        if (server.getAddress().isMulticastAddress()) {
            getMulticastSocket().send(packet);
        } else {
            getSocket().send(packet);
        }
    }
}

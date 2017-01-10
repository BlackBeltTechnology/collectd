package org.collectd.osgi.services.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.collectd.config.CollectdConstants;
import org.collectd.model.Notification;
import org.collectd.model.Values;
import org.collectd.osgi.services.CollectdSender;
import org.collectd.services.UdpPacketSender;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Collectd UDP sender service.
 */
@Component(immediate = true, property = CollectdSender.PROTOCOL_KEY + "=udp")
@Slf4j
public class UdpCollectdSender implements CollectdSender {

    @SuppressWarnings("checkstyle:JavadocMethod")
    @ObjectClassDefinition(name = "Collectd sender configuration")
    public @interface Config {

        @AttributeDefinition(required = false, name = "Collectd server host name")
        String stats_collectd_host() default CollectdConstants.DEFAULT_IPV4_ADDRESS;

        @AttributeDefinition(required = false, name = "Collectd server port number")
        int stats_collectd_port() default CollectdConstants.DEFAULT_UDP_PORT;

        @AttributeDefinition(required = false, name = "Client hostname")
        String stats_collectd_clientHost();

        @AttributeDefinition(required = false, name = "Packet size")
        int stats_collectd_packetSize() default CollectdConstants.DEFAULT_PACKET_SIZE;
    }

    private UdpPacketSender sender;
    
    /**
     * Initialize OSGi component.
     * 
     * @param config configuration options
     */
    @Activate
    @Modified
    public void startOsgiComponent(final Config config) {
        final InetSocketAddress server = new InetSocketAddress(config.stats_collectd_host(), config.stats_collectd_port());
        final int packetSize = config.stats_collectd_packetSize();
        final String clientHost = config.stats_collectd_clientHost();
        sender = new UdpPacketSender(server, clientHost, packetSize);
    }

    /**
     * Cleanup OSGi component.
     */
    @Deactivate
    public void stopOsgiComponent() {
        if (sender != null) {
            try {
                sender.flush();
            } catch (IOException ex) {
                log.error("Unable to flush buffer", ex);
            }
        }
        sender = null;
    }

    /**
     * Send numeric values (metrics) to Collectd.
     * 
     * @param values numeric values
     */
    @Override
    public void send(final Values values) {
        try {
            sender.send(values);
        } catch (IOException ex) {
            log.error("Unable to send value list", ex);
        }
    }

    /**
     * Send notification to Collectd.
     * 
     * @param notification notification
     */
    @Override
    public void send(final Notification notification) {
        try {
            sender.send(notification);
        } catch (IOException ex) {
            log.error("Unable to send notifitcation", ex);
        }
    }
}

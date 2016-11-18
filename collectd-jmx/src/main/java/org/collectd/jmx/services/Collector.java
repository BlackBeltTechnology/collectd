package org.collectd.jmx.services;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.collectd.jmx.xml.ns.definition.Jmx;
import org.collectd.model.PluginData;

/**
 * Collect JMX metrics for sending to Collectd.
 */
public class Collector {

    private MBeanServerConnection connection;

    /**
     * Create new local JMX collector instance.
     */
    public Collector() {
        configure(ManagementFactory.getPlatformMBeanServer());
    }

    /**
     * Create new remote JMX collector instance.
     *
     * @param url JMX URL
     * @throws IOException JMX connection failure
     */
    public Collector(final String url) throws IOException {
        final JMXServiceURL serviceUrl = new JMXServiceURL(url.indexOf('/') == -1 ? "service:jmx:rmi:///jndi/rmi://" + url + "/jmxrmi" : url);
        final JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        configure(connector.getMBeanServerConnection());
    }

    private void configure(final MBeanServerConnection connection) {
        this.connection = connection;
    }

    /**
     * Collect metrics and return data in Collectd structure.
     *
     * @param jmx JMX configuration
     * @return metrics data
     */
    public List<PluginData> collect(final Jmx jmx) {
        return null;
    }
}

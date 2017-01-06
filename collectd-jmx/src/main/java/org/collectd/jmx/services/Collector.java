package org.collectd.jmx.services;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import lombok.extern.slf4j.Slf4j;
import org.collectd.jmx.internal.Controller;
import org.collectd.jmx.xml.ns.definition.Jmx;
import org.collectd.jmx.xml.ns.definition.MBeanAttributeType;
import org.collectd.jmx.xml.ns.definition.MBeanType;
import org.collectd.jmx.xml.ns.definition.MBeansType;
import org.collectd.model.ValueType;
import org.collectd.model.Values;
import org.collectd.services.UdpPacketSender;

/**
 * Collect JMX metrics for sending to Collectd.
 */
@Slf4j
public class Collector implements Runnable {

    private final JMXServiceURL serviceUrl;
    private final Controller.Config config;

    private Collection<Jmx> jmxList;
    private UdpPacketSender packetSender;

    private transient MBeanServerConnection connection;
    private String instance;

    private static final String RUNTIME_NAME = "java.lang:type=Runtime";

    /**
     * Create new collector instance.
     *
     * @param config configuration
     * @param jmxList JMX definitions
     */
    public Collector(final Controller.Config config, final Collection<Jmx> jmxList) {
        Objects.requireNonNull(config, "Missing configuration");
        Objects.requireNonNull(jmxList, "Missing JMX configuration");

        this.config = config;
        this.jmxList = jmxList;

        final InetSocketAddress destination = new InetSocketAddress(config.getHost(), config.getPort());
        this.packetSender = new UdpPacketSender(destination, config.getClient(), config.getPacketSize());

        final String jmxUrl = config.getJmxUrl();
        try {
            serviceUrl = jmxUrl != null ? new JMXServiceURL(jmxUrl.indexOf('/') == -1 ? "service:jmx:rmi:///jndi/rmi://" + jmxUrl + "/jmxrmi" : jmxUrl) : null;
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid JMX url", ex);
        }

        if (config.getInstance() != null) {
            instance = config.getInstance();
        } else {
            try {
                instance = (String) getAttribute(new ObjectName(RUNTIME_NAME), "Name", false);
            } catch (JMException ex) {
                log.warn("Unable to get instance name", ex);
            }
            if (instance == null) {
                instance = ManagementFactory.getRuntimeMXBean().getName();
            }
        }
    }

    private MBeanServerConnection getConnection() {
        try {
            if (connection == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Connecting to JMX service: " + serviceUrl);
                }
                connection = serviceUrl != null ? JMXConnectorFactory.connect(serviceUrl).getMBeanServerConnection() : ManagementFactory.getPlatformMBeanServer();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("JMX connection failed", ex);
        }
        return connection;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            final Collection<Values> data = collectData();

            for (final Values values : data) {
                try {
                    packetSender.send(values);
                } catch (IOException ex) {
                    log.error("Unable to send metrics", ex);
                }
            }
        } catch (RuntimeException ex2) {
            log.error("Failed to send metrics", ex2);
        }
    }

    /**
     * Shutdown controller. Shutdown process flushes Collectd packet sender.
     */
    public void shutdown() {
        try {
            packetSender.flush();
        } catch (IOException ex) {
            log.error("Unable to flush UDP packet sender", ex);
        }
    }

    /**
     * Collect metrics and return data in Collectd structure.
     *
     * @return metrics data
     */
    private Collection<Values> collectData() {
        final List<Values> data = new LinkedList<>();

        for (final Jmx jmx : jmxList) {
            for (final MBeansType mbeans : jmx.getMbeans()) {
                final String plugin = mbeans.getName();
                if (log.isTraceEnabled()) {
                    log.trace("Collectding data for plugin '" + plugin + "'");
                }

                for (final MBeanType mbean : mbeans.getMbeen()) {
                    try {
                        data.addAll(getMetrics(plugin, mbean, true));
                    } catch (InstanceNotFoundException ex) {
                        if (log.isDebugEnabled()) {
                            log.debug("MBean not found" + mbean.getName(), ex);
                        }
                    } catch (JMException ex) {
                        log.error("Unable to get metrics for " + mbean.getName(), ex);
                    }
                }
            }
        }

        return data;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Collection<Values> getMetrics(final String plugin, final MBeanType mbean, final boolean retry) throws JMException {
        final ObjectName name = new ObjectName(mbean.getName());
        if (log.isTraceEnabled()) {
            log.trace("  - reading MBean: " + name);
        }

        final Collection<ObjectName> objectNames;
        if (name.isPattern()) {
            try {
                final MBeanServerConnection conn = getConnection();
                objectNames = conn.queryNames(name, null);
            } catch (IOException ex) {
                if (retry) {
                    log.error("Failed to get object names, retrying...", ex);
                    return getMetrics(plugin, mbean, false);
                } else {
                    throw new IllegalStateException("Failed to get object names", ex);
                }
            }
        } else {
            objectNames = Arrays.asList(name);
        }

        final Collection<Values> valueList = new LinkedList<>();
        for (final ObjectName objectName : objectNames) {
            if (mbean.getAttributes().isEmpty()) {
                // get all numeric attributes (including composite attributes) as GAUGE
                try {
                    final MBeanInfo info = getConnection().getMBeanInfo(objectName);
                    for (final MBeanAttributeInfo attribute : info.getAttributes()) {
                        if (!attribute.isReadable()) {
                            // attribute is not readable
                            continue;
                        }

                        // MBean is a search pattern
                        final String mbeanInstanceName = name.isPattern() ? getMBeanName(objectName) : mbean.getName();

                        final String attrName = attribute.getName();
                        try {
                            final Object attr = getAttribute(objectName, attrName, true);

                            if (attr instanceof CompositeData) {
                                final CompositeData data = (CompositeData) attr;
                                for (final String key : data.getCompositeType().keySet()) {
                                    final Object value = data.get(key);
                                    if (value instanceof Number) {
                                        addNumericValue(valueList, plugin, mbean.getType(), mbeanInstanceName + "_" + attrName + "_" + key, (Number) value);
                                    }
                                }
                            } else if (attr instanceof Number) {
                                addNumericValue(valueList, plugin, mbean.getType(), mbeanInstanceName + "_" + attrName, (Number) attr);
                            }
                        } catch (AttributeNotFoundException ex) {
                            log.warn("Unable to get attribute", ex);
                        }
                    }
                } catch (IOException ex) {
                    log.error("Unable to get MBean info", ex);
                }
            } else {
                final Values values = new Values();
                values.setHost(config.getClient());
                values.setPlugin(plugin);
                values.setPluginInstance(instance);
                values.setInterval(config.getInterval());

                final Values.ValueHolder[] items = new Values.ValueHolder[mbean.getAttributes().size()];

                int index = 0;
                for (final Iterator<MBeanAttributeType> it = mbean.getAttributes().iterator(); it.hasNext(); index++) {
                    final MBeanAttributeType mbeanAttribute = it.next();

                    values.setType(mbean.getType());

                    if (name.isPattern()) {
                        // MBean is a search pattern
                        final String mbeanInstanceName = getMBeanName(objectName);

                        values.setTypeInstance(mbean.getTypeInstance() != null ? mbean.getTypeInstance() + "-" + mbeanInstanceName : mbeanInstanceName);
                    } else if (mbeanAttribute.getTypeInstance() != null) {
                        // type instance is specified (for MBean attribute)
                        values.setTypeInstance(mbeanAttribute.getTypeInstance());
                    } else if (mbean.getTypeInstance() != null) {
                        // type instance is specified (for MBean)
                        values.setTypeInstance(mbean.getTypeInstance());
                    } else if (mbeanAttribute.getComposite() != null) {
                        // composite and no type instance is specified
                        values.setTypeInstance(mbeanAttribute.getComposite());
                    } else {
                        // attribute name otherwise
                        values.setTypeInstance(mbeanAttribute.getName());
                    }

                    Values.ValueHolder holder = null;
                    try {
                        holder = getAttrbituteMetrics(objectName, mbeanAttribute);
                    } catch (AttributeNotFoundException ex) {
                        log.warn("Unable to get attribute", ex);
                    }

                    // set numeric value 0 if attribute not found or failed to get value
                    items[index] = holder != null ? holder : new Values.ValueHolder(ValueType.valueOf(mbeanAttribute.getType().value()), 0);
                }

                values.getItems().addAll(Arrays.asList(items));
                valueList.add(values);
            }
        }

        return valueList;
    }

    private void addNumericValue(final Collection<Values> valueList, final String plugin, final String type, final String typeInstance, final Number value) {
        final Values values = new Values();
        values.setHost(config.getClient());
        values.setPlugin(plugin);
        values.setPluginInstance(instance);
        values.setInterval(config.getInterval());
        values.setType(type);
        values.setTypeInstance(typeInstance);
        values.getItems().add(new Values.ValueHolder(ValueType.GAUGE, value));

        valueList.add(values);
    }

    private Values.ValueHolder getAttrbituteMetrics(final ObjectName objectName, final MBeanAttributeType mbeanAttribute) throws JMException {
        final String attrName = mbeanAttribute.getName();
        final Object attr = getAttribute(objectName, attrName, true);

        final Object data;
        if (mbeanAttribute.getComposite() != null) {
            if (attr instanceof CompositeData) {
                final CompositeData compositeData = (CompositeData) attr;
                data = compositeData.get(mbeanAttribute.getComposite());
            } else {
                log.warn("Composite data expected for MBean " + objectName + ", attribute " + attrName);
                return null;
            }
        } else {
            data = attr;
        }

        final ValueType type = ValueType.valueOf(mbeanAttribute.getType().value());
        if (data instanceof Number) {
            if (log.isTraceEnabled()) {
                log.trace("    - value of attribute '" + attrName + "': " + data + (mbeanAttribute.getComposite() != null ? " " + mbeanAttribute.getComposite() : ""));
            }
            return new Values.ValueHolder(type, (Number) data);
        } else {
            log.warn("Invalid numeric data for MBean " + objectName + ", attribute " + attrName);
            return null;
        }
    }

    private String getMBeanName(final ObjectName objectName) {
        return objectName.getKeyProperty("name");
    }

    private Object getAttribute(final ObjectName name, final String attribute, final boolean retry) throws JMException {
        try {
            return getConnection().getAttribute(name, attribute);
        } catch (IOException ex) {
            if (retry) {
                log.error("Failed to get attribute, retrying...", ex);
                return getAttribute(name, attribute, false);
            } else {
                throw new IllegalStateException("Failed to get attribute", ex);
            }
        }
    }
}

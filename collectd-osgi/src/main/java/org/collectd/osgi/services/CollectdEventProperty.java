package org.collectd.osgi.services;

/**
 * OSGi event properties for submitting data to Collectd.
 */
public enum CollectdEventProperty {
    /**
     * The name of the host to associate with subsequent data values.
     */
    HOST("host"),
    /**
     * The plugin name to associate with subsequent data values, e.g. "cpu".
     */
    PLUGIN("plugin"),
    /**
     * The plugin instance name to associate with subsequent data values, e.g. "1".
     */
    PLUGIN_INSTANCE("pluginInstance"),
    /**
     * The type name to associate with subsequent data values, e.g. "cpu".
     */
    TYPE("type"),
    /**
     * The type instance name to associate with subsequent data values, e.g. "idle".
     */
    TYPE_INSTANCE("typeInstance"),
    /**
     * Interval used to set the "step" when creating new RRDs unless rrdtool plugin forces StepSize. Also used to detect values that have timed out.
     */
    INTERVAL("interval"),
    /**
     * Numeric data values.
     */
    VALUES("values"),
    /**
     * Type of numeric values, one of COUNTER, GAUGE, DERIVE, ABSOLUTE.
     */
    VALUE_TYPE("valueType"),
    /**
     * Message part of the notification.
     */
    MESSAGE("message"),
    /**
     * Severity part of the notification.
     */
    SEVERITY("severity");
    
    /**
     * OSGi event topic. Submit event to that topic using OSGi event admin service. Data should be 
     */
    public static final String EVENT_TOPIC = "org/collectd/Event/SEND";

    private final String propertyName;

    private CollectdEventProperty(final String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Get OSGi event property name for the given collectd data part.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }
}

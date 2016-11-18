package org.collectd.osgi.services;

import org.collectd.model.Notification;
import org.collectd.model.Values;

/**
 * Send data to Collectd.
 */
public interface CollectdSender {

    /**
     * Protocol for sending data, used as OSGi component property key.
     */
    String PROTOCOL_KEY = "protocol";

    /**
     * Send numeric values to Collectd.
     *
     * @param values numeric values
     */
    void send(Values values);

    /**
     * Send notification to Collectd.
     *
     * @param notification notification
     */
    void send(Notification notification);
}

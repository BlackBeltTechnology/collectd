package org.collectd.model;

/**
 * Notification data type.
 */
@lombok.Getter
@lombok.Setter
@lombok.ToString
public class Notification extends PluginData {

    private Severity severity;
    private String message;
}

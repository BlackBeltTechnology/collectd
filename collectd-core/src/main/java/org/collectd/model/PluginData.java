package org.collectd.model;

/**
 * Common data type for collectd.
 */
@lombok.Getter
@lombok.Setter
public abstract class PluginData {

    private String host;
    private long time;
    private String plugin;
    private String pluginInstance;
    private String type;
    private String typeInstance;

    private boolean sign;
    private boolean encrypt;
}

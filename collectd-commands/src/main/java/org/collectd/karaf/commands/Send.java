package org.collectd.karaf.commands;

import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.collectd.model.ValueType;
import org.collectd.model.Values;
import org.collectd.osgi.services.CollectdSender;

/**
 * Submit a collectd packet.
 */
@Command(scope = "collectd", name = "send", description = "Submit a Collectd packet.")
@Service
public class Send implements Action {

    @Argument(index = 0, name = "values", description = "Values", required = true, multiValued = true)
    private List<Number> values;

    @Option(name = "--plugin", description = "Plugin", required = true, multiValued = false)
    private String plugin;

    @Option(name = "--pluginInstance", description = "Plugin instance", required = false, multiValued = false)
    private String pluginInstance;

    @Option(name = "--type", description = "Type", required = false, multiValued = false)
    private String type;

    @Option(name = "--typeInstance", description = "Type", required = false, multiValued = false)
    private String typeInstance;

    @Option(name = "--valueType", description = "Value type", required = false, multiValued = false)
    private ValueType valueType = ValueType.GAUGE;

    @Option(name = "--interval", description = "Interval", required = false, multiValued = false)
    private Long interval;

    @Reference
    private CollectdSender sender;

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Object execute() {
        final Values valueList = new Values();

        valueList.setPlugin(plugin);
        valueList.setPluginInstance(pluginInstance);
        valueList.setType(type);
        valueList.setTypeInstance(typeInstance);

        for (final Number number : values) {
            final Values.ValueHolder valueHolder = new Values.ValueHolder();
            valueHolder.setValue(number);
            valueHolder.setType(valueType);
            valueList.getItems().add(valueHolder);
        }
        valueList.setInterval(interval);

        sender.send(valueList);
        return null;
    }
}

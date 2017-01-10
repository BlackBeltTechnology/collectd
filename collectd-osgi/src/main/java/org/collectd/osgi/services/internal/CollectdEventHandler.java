package org.collectd.osgi.services.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.collectd.model.Notification;
import org.collectd.model.Severity;
import org.collectd.model.ValueType;
import org.collectd.model.Values;
import org.collectd.osgi.services.CollectdEventProperty;
import org.collectd.osgi.services.CollectdSender;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@Component(immediate = true, property = {
    EventConstants.EVENT_TOPIC + "=" + CollectdEventProperty.EVENT_TOPIC,
    EventConstants.EVENT_DELIVERY + "=" + EventConstants.DELIVERY_ASYNC_UNORDERED})
@Slf4j
public class CollectdEventHandler implements EventHandler {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    private List<CollectdSender> senders = new ArrayList<>();

    @Override
    public void handleEvent(final Event event) {
        if (log.isTraceEnabled()) {
            log.trace("Collectd event received, sending data to all destinations");
        }
        
        if (event.containsProperty(CollectdEventProperty.VALUES.getPropertyName())) {
            processValuesEvent(event);
        } else if (event.containsProperty(CollectdEventProperty.MESSAGE.getPropertyName())) {
            processNotificationEvent(event);
        } else {
            log.warn("Event contains no numeric values nor notification");
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void processValuesEvent(final Event event) {
        final Values values = new Values();

        values.setHost((String) event.getProperty(CollectdEventProperty.HOST.getPropertyName()));
        values.setPlugin((String) event.getProperty(CollectdEventProperty.PLUGIN.getPropertyName()));
        values.setPluginInstance((String) event.getProperty(CollectdEventProperty.PLUGIN_INSTANCE.getPropertyName()));
        values.setType((String) event.getProperty(CollectdEventProperty.TYPE.getPropertyName()));
        values.setTypeInstance((String) event.getProperty(CollectdEventProperty.TYPE_INSTANCE.getPropertyName()));

        final String valueTypeProperty = (String) event.getProperty(CollectdEventProperty.VALUE_TYPE.getPropertyName());
        final ValueType valueType = ValueType.valueOf(valueTypeProperty);

        final String intervalProperty = (String) event.getProperty(CollectdEventProperty.INTERVAL.getPropertyName());
        if (intervalProperty != null) {
            values.setInterval(Long.parseLong(intervalProperty));
        }

        final Object valuesProperty = event.getProperty(CollectdEventProperty.VALUES.getPropertyName());
        if (valuesProperty instanceof Number) {
            final Values.ValueHolder valueHolder = new Values.ValueHolder(valueType, (Number) valuesProperty);
            values.getItems().add(valueHolder);
        } else if (valuesProperty instanceof Collection) {
            for (final Number n : (Collection<Number>) valuesProperty) {
                final Values.ValueHolder valueHolder = new Values.ValueHolder(valueType, n);
                values.getItems().add(valueHolder);
            }
        }

        sendValues(values);
    }

    private void processNotificationEvent(final Event event) {
        final Notification notification = new Notification();
        
        notification.setHost((String) event.getProperty(CollectdEventProperty.HOST.getPropertyName()));
        notification.setPlugin((String) event.getProperty(CollectdEventProperty.PLUGIN.getPropertyName()));
        notification.setPluginInstance((String) event.getProperty(CollectdEventProperty.PLUGIN_INSTANCE.getPropertyName()));
        notification.setType((String) event.getProperty(CollectdEventProperty.TYPE.getPropertyName()));
        notification.setTypeInstance((String) event.getProperty(CollectdEventProperty.TYPE_INSTANCE.getPropertyName()));

        notification.setMessage((String) event.getProperty(CollectdEventProperty.MESSAGE.getPropertyName()));
        notification.setSeverity(Severity.valueOf((String) event.getProperty(CollectdEventProperty.SEVERITY.getPropertyName())));

        sendNotification(notification);
    }

    private void sendValues(final Values values) {
        for (final CollectdSender sender : senders) {
            sender.send(values);
        }
    }

    private void sendNotification(final Notification notification) {
        for (final CollectdSender sender : senders) {
            sender.send(notification);
        }
    }
}

package org.collectd.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Numeric values data type.
 */
@lombok.Getter
@lombok.Setter
@lombok.ToString(callSuper = true)
public class Values extends PluginData {

    /**
     * Numeric values (items).
     */
    private final Collection<ValueHolder> items = new ArrayList<>();

    /**
     * Interval used to set the "step" when creating new RRDs unless rrdtool plugin forces StepSize.
     */
    private Long interval;

    /**
     * Numeric value item (type - value pair).
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.ToString
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValueHolder {

        /**
         * Type of numeric value.
         */
        private ValueType type;

        /**
         * Numeric value.
         */
        private Number value;
    }
}

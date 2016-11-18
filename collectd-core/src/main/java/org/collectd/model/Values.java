package org.collectd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Numeric values data type.
 */
@lombok.Getter
@lombok.Setter
@lombok.ToString
public class Values extends PluginData {

    /**
     * Numeric values (items).
     */
    private final List<ValueHolder> items = new ArrayList<>();

    /**
     * Interval used to set the "step" when creating new RRDs unless rrdtool plugin forces StepSize.
     */
    private Long interval;

    /**
     * Numeric value item (type - value pair).
     */
    @lombok.Getter
    @lombok.Setter
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

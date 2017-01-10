package org.collectd.model;

/**
 * Numeric value data types. More details: https://collectd.org/wiki/index.php/Data_source
 */
public enum ValueType {
    /**
     * These data sources behave exactly like DERIVE data sources in the normal case. Their behavior differs when new value less than old value.
     */
    COUNTER((byte) 0x00),
    /**
     * A GAUGE value is simply stored as-is. This is the right choice for values which may increase as well as decrease, such as temperatures or the amount of
     * memory used.
     */
    GAUGE((byte) 0x01),
    /**
     * These data sources assume that the change of the value is interesting, i.e. the derivative.
     */
    DERIVE((byte) 0x02),
    /**
     * This is probably the most exotic type: It is intended for counters which are reset upon reading.
     */
    ABSOLUTE((byte) 0x03);

    private final byte code;

    ValueType(final byte code) {
        this.code = code;
    }

    /**
     * Get data type code.
     *
     * @return code for binary protocol
     */
    public byte getCode() {
        return code;
    }
}

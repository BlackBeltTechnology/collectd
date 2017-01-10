package org.collectd.protocol;

/**
 * Collectd packet part types.
 */
public enum PacketPartType {
    /**
     * The name of the host to associate with subsequent data values.
     */
    HOST((short) 0x0000),
    /**
     * The timestamp to associate with subsequent data values, unix time format (seconds since epoch).
     */
    TIME((short) 0x0001),
    /**
     * The timestamp to associate with subsequent data values. Time is defined in 2^–30 seconds since epoch. New in Version 5.0.
     */
    TIME_high_resolution((short) 0x0008),
    /**
     * The plugin name to associate with subsequent data values, e.g. "cpu".
     */
    PLUGIN((short) 0x0002),
    /**
     * The plugin instance name to associate with subsequent data values, e.g. "1".
     */
    PLUGIN_INSTANCE((short) 0x0003),
    /**
     * The type name to associate with subsequent data values, e.g. "cpu".
     */
    TYPE((short) 0x0004),
    /**
     * The type instance name to associate with subsequent data values, e.g. "idle".
     */
    TYPE_INSTANCE((short) 0x0005),
    /**
     * Numeric data values.
     */
    VALUES((short) 0x0006),
    /**
     * Interval used to set the "step" when creating new RRDs unless rrdtool plugin forces StepSize. Also used to detect values that have timed out.
     */
    INTERVAL((short) 0x0007),
    /**
     * The interval in which subsequent data values are collected. The interval is given in 2^–30 seconds. New in Version 5.0.
     */
    INTERVAL_high_resolution((short) 0x0009),
    /**
     * Message part of the notification.
     */
    MESSAGE((short) 0x0100),
    /**
     * Severity part of the notification.
     */
    SEVERITY((short) 0x0101),
    /**
     * Signature (HMAC-SHA-256).
     */
    SIGNATURE((short) 0x0200),
    /**
     * Encrypted content (AES-256/OFB/SHA-1).
     */
    ENCRYPTED_CONTENT((short) 0x0201);

    private final short code;

    PacketPartType(final short code) {
        this.code = code;
    }

    public short getCode() {
        return code;
    }
}

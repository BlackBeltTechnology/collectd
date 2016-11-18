package org.collectd.model;

/**
 * Severity data types.
 */
public enum Severity {
    /**
     * Failure.
     */
    FAILURE(1),
    /**
     * Warning.
     */
    WARNING(2),
    /**
     * Okay.
     */
    OKAY(4);

    private final int code;

    Severity(final int code) {
        this.code = code;
    }

    /**
     * Get code of severity.
     *
     * @return code for binary protocol
     */
    public int getCode() {
        return code;
    }
}

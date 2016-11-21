package org.collectd.protocol;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.collectd.config.CollectdConstants;
import org.collectd.model.PluginData;
import org.collectd.model.Severity;
import org.collectd.model.ValueType;
import org.collectd.model.Values;

/**
 * UDP packet writer.
 */
@Slf4j
public class UdpBufferWriter {

    private final ByteArrayOutputStream bos;
    private final DataOutputStream os;

    private static final int UINT8_LEN = 1;
    private static final int UINT16_LEN = UINT8_LEN * 2;
    private static final int UINT32_LEN = UINT16_LEN * 2;
    private static final int UINT64_LEN = UINT32_LEN * 2;
    private static final int HEADER_LEN = UINT16_LEN * 2;

    /**
     * Create new UDP packet writer instance. Default packet size is used.
     */
    public UdpBufferWriter() {
        this(CollectdConstants.DEFAULT_PACKET_SIZE);
    }

    /**
     * Create new UDP packet writer instance.
     *
     * @param packetSize packet size
     */
    public UdpBufferWriter(final int packetSize) {
        bos = new ByteArrayOutputStream(packetSize);
        os = new DataOutputStream(bos);
    }

    /**
     * Get buffer content as byte array and reset it.
     *
     * @throws IOException unable to write value to output stream
     */
    public synchronized byte[] getBuffer() throws IOException {
        os.flush();
        final byte[] buffer;
        synchronized (this) {
            buffer = bos.toByteArray();
            bos.reset();
        }

        return buffer;
    }

    /**
     * Write common collectd key (identifier) parts to buffer.
     *
     * @param data plugin data
     * @throws IOException unable to write value to output stream
     */
    public void writeKeyParts(final PluginData data) throws IOException {
        writeStringPart(PacketPartType.HOST.getCode(), data.getHost());
        writeNumberPart(PacketPartType.TIME.getCode(), data.getTime() / 1000);
        writeStringPart(PacketPartType.PLUGIN.getCode(), data.getPlugin());
        if (data.getPluginInstance() != null) {
            writeStringPart(PacketPartType.PLUGIN_INSTANCE.getCode(), data.getPluginInstance());
        }
        if (data.getType() != null) {
            writeStringPart(PacketPartType.TYPE.getCode(), data.getType());
        }
        if (data.getTypeInstance() != null) {
            writeStringPart(PacketPartType.TYPE_INSTANCE.getCode(), data.getTypeInstance());
        }
    }

    /**
     * Write numeric values to buffer.
     *
     * @param values numeric values
     * @param interval interval
     * @throws IOException unable to write value to output stream
     */
    @SuppressFBWarnings("DB_DUPLICATE_SWITCH_CLAUSES")
    public void writeValuesPart(final Collection<Values.ValueHolder> values, final Long interval) throws IOException {
        final int num = values.size();
        final int len = HEADER_LEN + UINT16_LEN + num * (UINT8_LEN + UINT64_LEN);

        final byte[] types = new byte[num];
        int idx = 0;
        for (final Iterator<Values.ValueHolder> it = values.iterator(); it.hasNext(); idx++) {
            final Values.ValueHolder holder = it.next();
            final Number value = holder.getValue();

            if (holder.getType() == null) {
                if (value instanceof Double) {
                    types[idx] = ValueType.GAUGE.getCode();
                    holder.setType(ValueType.GAUGE);
                } else {
                    types[idx] = ValueType.COUNTER.getCode();
                    holder.setType(ValueType.COUNTER);
                }
            } else {
                types[idx] = holder.getType().getCode();
            }
        }

        writeHeader(PacketPartType.VALUES.getCode(), len);
        writeShortValue(num);
        os.write(types);

        for (final Values.ValueHolder holder : values) {
            final Number value = holder.getValue();
            final ValueType type = holder.getType();

            switch (type) {
                case COUNTER:
                case ABSOLUTE: {
                    // unsigned, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                case GAUGE: {
                    // little-endian
                    writeDoubleValue(value.doubleValue());
                    break;
                }
                case DERIVE: {
                    // signed, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                default: {
                    log.warn("Unsupported numeric value type: " + type);
                }
            }
        }

        if (interval != null) {
            writeNumberPart(PacketPartType.INTERVAL.getCode(), interval);
        }
    }

    /**
     * Write notification to buffer.
     *
     * @param severity severity
     * @param message message
     * @throws IOException unable to write value to output stream
     */
    public void writeNotificationPart(final Severity severity, final String message) throws IOException {
        if (severity != null) {
            writeNumberPart(PacketPartType.SEVERITY.getCode(), severity.getCode());
        }

        writeStringPart(PacketPartType.MESSAGE.getCode(), message);
    }

    private void writeHeader(final short type, final int len) throws IOException {
        writeShortValue(type);
        writeShortValue(len);
    }

    private void writeShortValue(final int val) throws IOException {
        os.writeShort(val);
    }

    /**
     * Write long or date (epoch) value.
     *
     * @param val long or epoch value
     * @throws IOException unable to write value to output stream
     */
    private void writeLongOrDateValue(final long val) throws IOException {
        os.writeLong(val);
    }

    private void writeDoubleValue(final double val) throws IOException {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(val);
        os.write(bb.array());
    }

    private void writeStringValue(final String val, final boolean addNullByte) throws IOException {
        os.write(val.getBytes("UTF-8"));
        if (addNullByte) {
            os.write('\0');
        }
    }

    private void writeStringPart(final short type, final String val) throws IOException {
        if (val == null || val.length() == 0) {
            return;
        }
        final int len = HEADER_LEN + val.length() + 1;
        writeHeader(type, len);
        writeStringValue(val, true);
    }

    private void writeNumberPart(final short type, final long val) throws IOException {
        final int len = HEADER_LEN + UINT64_LEN;
        writeHeader(type, len);
        writeLongOrDateValue(val);
    }
}

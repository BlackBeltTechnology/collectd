package org.collectd.protocol;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.collectd.config.CollectdConstants;
import org.collectd.model.Notification;
import org.collectd.model.PluginData;
import org.collectd.model.ValueType;
import org.collectd.model.Values;

/**
 * UDP packet writer.
 */
@Slf4j
public class UdpBufferWriter {

    private final ByteArrayOutputStream bos;
    private final DataOutputStream os;
    
    private final int packetSize;

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
        this.packetSize = packetSize;
        bos = new ByteArrayOutputStream(packetSize);
        os = new DataOutputStream(bos);
    }

    /**
     * Get buffer content as byte array and reset it.
     *
     * @throws IOException unable to write value to output stream
     */
    public synchronized byte[] getBuffer() {
        final byte[] buffer;
        synchronized (this) {
            buffer = bos.toByteArray();
            bos.reset();
        }

        return buffer;
    }

    /**
     * Check if free buffer space is enough for numeric values.
     * 
     * @param values numeric values
     * @return buffer to send if flushed, null otherwise
     * @throws IOException unable to check buffer size or flush buffer
     */
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    public byte[] checkSpace(final Values values) throws IOException {
        final int length = getKeyPartsLength(values) + getValuesPartLength(values);
        if (length > packetSize) {
            throw new IllegalArgumentException("Values data size is greater than maximum packet size: " + packetSize);
        }
        
        if (bos.size() + length > packetSize) {
            return getBuffer();
        } else {
            return null;
        }
    }

    /**
     * Check if free buffer space is enough for notification.
     * 
     * @param notification notification
     * @return buffer to send if flushed, null otherwise
     * @throws IOException unable to check buffer size or flush buffer
     */
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    public byte[] checkSpace(final Notification notification) throws IOException {
        final int length = getKeyPartsLength(notification) + getNotificationPartLength(notification);
        if (length > packetSize) {
            throw new IllegalArgumentException("Notification size is greater than maximum packet size: " + packetSize);
        }
        
        if (bos.size() + length > packetSize) {
            return getBuffer();
        } else {
            return null;
        }
    }

    private int getKeyPartsLength(final PluginData data) {
        return getStringPartLength(data.getHost())
                + getNumberPartLength(data.getTime() / 1000)
                + getStringPartLength(data.getPlugin())
                + getStringPartLength(data.getPluginInstance())
                + getStringPartLength(data.getType())
                + getStringPartLength(data.getTypeInstance());
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
        writeStringPart(PacketPartType.PLUGIN_INSTANCE.getCode(), data.getPluginInstance());
        writeStringPart(PacketPartType.TYPE.getCode(), data.getType());
        writeStringPart(PacketPartType.TYPE_INSTANCE.getCode(), data.getTypeInstance());
    }

    private int getValuesPartLength(final Values values) {
        final int num = values.getItems().size();
        return num > 0 ? HEADER_LEN + UINT16_LEN + num * (UINT8_LEN + UINT64_LEN) : 0;
    }

    /**
     * Write numeric values to buffer.
     *
     * @param values numeric values
     * @throws IOException unable to write value to output stream
     */
    @SuppressFBWarnings("DB_DUPLICATE_SWITCH_CLAUSES")
    public void writeValuesPart(final Values values) throws IOException {
        final int num = values.getItems().size();
        if (num == 0) {
            return;
        }

        writeKeyParts(values);
        final int len = getValuesPartLength(values);

        final byte[] types = new byte[num];
        int idx = 0;
        for (final Iterator<Values.ValueHolder> it = values.getItems().iterator(); it.hasNext(); idx++) {
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

        for (final Values.ValueHolder holder : values.getItems()) {
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

        if (values.getInterval() != null) {
            writeNumberPart(PacketPartType.INTERVAL.getCode(), values.getInterval());
        }
    }

    private int getNotificationPartLength(final Notification notification) {
        return (notification.getSeverity() != null ? getNumberPartLength(notification.getSeverity().getCode()) : 0) + getStringPartLength(notification.getMessage());
    }

    /**
     * Write notification to buffer.
     *
     * @param notification notification
     * @throws IOException unable to write value to output stream
     */
    public void writeNotificationPart(final Notification notification) throws IOException {
        writeKeyParts(notification);

        if (notification.getSeverity() != null) {
            writeNumberPart(PacketPartType.SEVERITY.getCode(), notification.getSeverity().getCode());
        }

        writeStringPart(PacketPartType.MESSAGE.getCode(), notification.getMessage());
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

    private int getStringPartLength(final String val) {
        return val != null && val.length() > 0 ? HEADER_LEN + val.length() + 1 : 0;
    }

    private void writeStringPart(final short type, final String val) throws IOException {
        if (val == null || val.length() == 0) {
            return;
        }
        final int len = getStringPartLength(val);
        writeHeader(type, len);
        writeStringValue(val, true);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private int getNumberPartLength(final long val) {
        return HEADER_LEN + UINT64_LEN;
    }

    private void writeNumberPart(final short type, final long val) throws IOException {
        final int len = getNumberPartLength(val);
        writeHeader(type, len);
        writeLongOrDateValue(val);
    }
}

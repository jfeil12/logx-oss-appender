package org.logx.core;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志载荷清洗工具：
 * - 去除除换行/制表外的控制字符
 * - 限制最大字节数，超限截断
 * - 返回清洗结果并在需要时可记录告警
 */
public final class LogPayloadSanitizer {

    private static final AtomicLong sanitizedCount = new AtomicLong(0);
    private static final AtomicLong truncatedCount = new AtomicLong(0);

    private LogPayloadSanitizer() {
    }

    public static SanitizedPayload sanitize(String input, int maxBytes) {
        if (input == null) {
            return new SanitizedPayload(new byte[0], false, false, 0);
        }

        StringBuilder sb = new StringBuilder();
        boolean sanitized = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\n' || c == '\t' || !Character.isISOControl(c)) {
                sb.append(c);
            } else {
                sanitized = true;
            }
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        int originalBytes = bytes.length;
        boolean truncated = false;
        if (bytes.length > maxBytes) {
            byte[] truncatedBytes = new byte[maxBytes];
            System.arraycopy(bytes, 0, truncatedBytes, 0, maxBytes);
            bytes = truncatedBytes;
            truncated = true;
        }

        if (sanitized) {
            sanitizedCount.incrementAndGet();
        }
        if (truncated) {
            truncatedCount.incrementAndGet();
        }

        return new SanitizedPayload(bytes, sanitized, truncated, originalBytes);
    }

    public static final class SanitizedPayload {
        public final byte[] bytes;
        public final boolean sanitized;
        public final boolean truncated;
        public final int originalBytes;

        SanitizedPayload(byte[] bytes, boolean sanitized, boolean truncated, int originalBytes) {
            this.bytes = bytes;
            this.sanitized = sanitized;
            this.truncated = truncated;
            this.originalBytes = originalBytes;
        }
    }
}

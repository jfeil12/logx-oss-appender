package org.logx.config.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.logx.config.properties.LogxOssProperties;

public final class ConfigValidationUtils {

    private ConfigValidationUtils() {
    }

    public static void validateRequiredStorage(LogxOssProperties properties, Consumer<String> logger) {
        if (properties == null || properties.getStorage() == null) {
            throw new IllegalStateException("Storage configuration is missing");
        }

        List<String> missing = new ArrayList<>();
        if (isBlank(properties.getStorage().getEndpoint())) {
            missing.add("logx.oss.storage.endpoint");
        }
        if (isBlank(properties.getStorage().getRegion())) {
            missing.add("logx.oss.storage.region");
        }
        if (isBlank(properties.getStorage().getAccessKeyId())) {
            missing.add("logx.oss.storage.accessKeyId");
        }
        if (isBlank(properties.getStorage().getAccessKeySecret())) {
            missing.add("logx.oss.storage.accessKeySecret");
        }
        if (isBlank(properties.getStorage().getBucket())) {
            missing.add("logx.oss.storage.bucket");
        }

        if (!missing.isEmpty()) {
            String message = "Missing required OSS configuration keys: " + String.join(", ", missing);
            if (logger != null) {
                logger.accept(message);
            }
            throw new IllegalStateException(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

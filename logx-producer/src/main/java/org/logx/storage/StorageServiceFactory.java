package org.logx.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.ServiceLoader;

public class StorageServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceFactory.class);

    public static StorageService createStorageService(StorageConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("StorageConfig cannot be null");
        }

        String ossType = config.getOssType();
        if (ossType == null || ossType.trim().isEmpty()) {
            throw new IllegalArgumentException("ossType is required but was null or empty. " +
                    "Please set logx.oss.storage.ossType property or LOGX_OSS_STORAGE_OSS_TYPE environment variable");
        }

        // Normalize ossType to uppercase
        ossType = ossType.trim().toUpperCase();

        ProtocolType protocol;
        try {
            StorageOssType ossTypeEnum = StorageOssType.valueOf(ossType);
            protocol = ossTypeEnum.getProtocolType();
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown OSS type: {}, trying to parse as protocol type", ossType);
            try {
                protocol = ProtocolType.fromValue(ossType);
            } catch (IllegalArgumentException e2) {
                throw new IllegalStateException("Invalid OSS type or protocol: " + ossType +
                        ". Valid values are: SF_S3, SF_OSS, AWS_S3, ALIYUN_OSS, TENCENT_COS, HUAWEI_OBS, MINIO, GENERIC_S3", e2);
            }
        }

        ServiceLoader<StorageService> loader = ServiceLoader.load(StorageService.class);
        Iterator<StorageService> iterator = loader.iterator();
        Set<String> loadedClasses = new HashSet<>();

        while (iterator.hasNext()) {
            try {
                StorageService service = iterator.next();
                String serviceClassName = service.getClass().getName();

                if (loadedClasses.contains(serviceClassName)) {
                    continue; // Skip already processed service implementation
                }
                loadedClasses.add(serviceClassName);
                
                if (service.supportsProtocol(protocol)) {
                    try {
                        Method initializeMethod = service.getClass().getMethod("initialize", StorageConfig.class);
                        initializeMethod.invoke(service, config);
                    } catch (Exception e) {
                        logger.error("Failed to initialize storage service: {}", e);
                    }
                    return service;
                }
            } catch (Exception e) {
                logger.error("Failed to load storage service: {}", e);
            }
        }

        throw new IllegalStateException("No storage service found for protocol type: " + protocol +
                ". Please ensure the appropriate adapter module is in the classpath.");
    }
}

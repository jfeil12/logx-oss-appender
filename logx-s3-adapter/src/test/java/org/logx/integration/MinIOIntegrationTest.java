package org.logx.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.properties.LogxOssProperties;
import org.logx.fallback.ObjectNameGenerator;
import org.logx.storage.StorageConfig;
import org.logx.storage.s3.S3StorageServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class MinIOIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MinIOIntegrationTest.class);

    private S3StorageServiceAdapter storageService;

    @BeforeEach
    void setUp() throws Exception {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("minio-test.properties")) {
            if (is == null) {
                fail("Could not find minio-test.properties");
            }
            properties.load(is);
        }

        String endpoint = getProperty(properties, "logx.oss.storage.endpoint", "http://localhost:9000");
        String region = getProperty(properties, "logx.oss.storage.region", "US");
        String accessKeyId = getProperty(properties, "logx.oss.storage.accessKeyId", "minioadmin");
        String accessKeySecret = getProperty(properties, "logx.oss.storage.accessKeySecret", "minioadmin");
        String bucket = getProperty(properties, "logx.oss.storage.bucket", "logx-test-bucket");
        String keyPrefix = getProperty(properties, "logx.oss.storage.keyPrefix", "logx/");

        LogxOssProperties logxOssProperties = new LogxOssProperties();
        logxOssProperties.getStorage().setOssType("S3");
        logxOssProperties.getStorage().setEndpoint(endpoint);
        logxOssProperties.getStorage().setRegion(region);
        logxOssProperties.getStorage().setAccessKeyId(accessKeyId);
        logxOssProperties.getStorage().setAccessKeySecret(accessKeySecret);
        logxOssProperties.getStorage().setBucket(bucket);
        logxOssProperties.getStorage().setKeyPrefix(keyPrefix);
        logxOssProperties.getStorage().setPathStyleAccess(true);

        StorageConfig config = new StorageConfig(logxOssProperties);

        storageService = new S3StorageServiceAdapter();
        storageService.initialize(config);
    }

    private String getProperty(Properties properties, String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }
        return defaultValue;
    }

    @AfterEach
    void tearDown() {
        if (storageService != null) {
            storageService.close();
        }
    }

    @Test
    void shouldUploadLogsToMinIO() throws Exception {
        String testContent = "MinIO Integration Test - " + System.currentTimeMillis() + "\n";
        byte[] data = testContent.getBytes(StandardCharsets.UTF_8);
        String objectKey = ObjectNameGenerator.generateObjectName(storageService.getKeyPrefix());

        CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);

        assertThatCode(() -> uploadFuture.get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldUploadMultipleFiles() throws Exception {
        for (int i = 0; i < 5; i++) {
            String testContent = String.format("Test file #%d - %d\n", i, System.currentTimeMillis());
            byte[] data = testContent.getBytes(StandardCharsets.UTF_8);
            String objectKey = ObjectNameGenerator.generateObjectName(storageService.getKeyPrefix());

            CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);
            uploadFuture.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldHandleLargeFile() throws Exception {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append(String.format("Line %04d: This is a test log message.\n", i));
        }

        byte[] data = content.toString().getBytes(StandardCharsets.UTF_8);
        String objectKey = ObjectNameGenerator.generateObjectName(storageService.getKeyPrefix());

        CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);

        assertThatCode(() -> uploadFuture.get(30, TimeUnit.SECONDS))
                .doesNotThrowAnyException();
    }
}

package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.logx.config.properties.LogxOssProperties;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageConfig;
import static org.junit.jupiter.api.Assertions.*;

public class S3StorageAdapterTest {

    private StorageConfig config;

    @BeforeEach
    public void setUp() {
        LogxOssProperties properties = new LogxOssProperties();
        properties.getStorage().setOssType("S3");
        properties.getStorage().setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        properties.getStorage().setRegion("cn-hangzhou");
        properties.getStorage().setAccessKeyId("test-access-key-id");
        properties.getStorage().setAccessKeySecret("test-access-key-secret");
        properties.getStorage().setBucket("test-bucket");
        config = new StorageConfig(properties);
    }

    @Test
    public void testS3StorageAdapterCreationWithEndpoint() {
        assertDoesNotThrow(() -> {
            S3StorageServiceAdapter adapter = new S3StorageServiceAdapter();
            adapter.initialize(config);
            assertNotNull(adapter);
            assertEquals(ProtocolType.S3, adapter.getProtocolType());
            assertEquals("test-bucket", adapter.getBucketName());
            adapter.close();
        });
    }

    @Test
    public void testS3StorageAdapterCreationWithoutEndpoint() {
        LogxOssProperties properties = new LogxOssProperties();
        properties.getStorage().setOssType("S3");
        properties.getStorage().setRegion("us-east-1");
        properties.getStorage().setAccessKeyId("test-access-key-id");
        properties.getStorage().setAccessKeySecret("test-access-key-secret");
        properties.getStorage().setBucket("test-bucket");
        StorageConfig configWithoutEndpoint = new StorageConfig(properties);

        assertDoesNotThrow(() -> {
            S3StorageServiceAdapter adapter = new S3StorageServiceAdapter();
            adapter.initialize(configWithoutEndpoint);
            assertNotNull(adapter);
            assertEquals(ProtocolType.S3, adapter.getProtocolType());
            assertEquals("test-bucket", adapter.getBucketName());
            adapter.close();
        });
    }

    @Test
    public void testS3StorageAdapterCreationWithInvalidEndpoint() {
        LogxOssProperties properties = new LogxOssProperties();
        properties.getStorage().setOssType("S3");
        properties.getStorage().setEndpoint("invalid-url");
        properties.getStorage().setRegion("cn-hangzhou");
        properties.getStorage().setAccessKeyId("test-access-key-id");
        properties.getStorage().setAccessKeySecret("test-access-key-secret");
        properties.getStorage().setBucket("test-bucket");
        StorageConfig configWithInvalidEndpoint = new StorageConfig(properties);

        assertDoesNotThrow(() -> {
            S3StorageServiceAdapter adapter = new S3StorageServiceAdapter();
            adapter.initialize(configWithInvalidEndpoint);
            assertNotNull(adapter);
            adapter.close();
        });
    }
}
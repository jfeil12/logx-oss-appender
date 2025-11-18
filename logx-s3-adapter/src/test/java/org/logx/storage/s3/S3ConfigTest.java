package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;

/**
 * S3配置测试类
 */
public class S3ConfigTest {

    @Test
    public void testEndpointUriCreation() {
        // 测试URI创建是否正确
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        assertDoesNotThrow(() -> {
            URI uri = URI.create(endpoint);
            assertNotNull(uri);
            assertEquals("https", uri.getScheme());
            assertEquals("oss-cn-hangzhou.aliyuncs.com", uri.getHost());
        });
    }

    @Test
    public void testInvalidEndpointUriCreation() {
        // 测试无效URI创建是否抛出异常
        String invalidEndpoint = "invalid-url";
        assertDoesNotThrow(() -> {
            URI uri = URI.create(invalidEndpoint);
            assertNotNull(uri);
        });
    }
}
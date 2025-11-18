package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.logx.storage.StorageConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * S3StorageAdapter简单测试类
 */
public class SimpleS3StorageAdapterTest {

    @Test
    public void testS3StorageAdapterClassExists() {
        // 简单测试类是否存在
        assertNotNull(S3StorageAdapter.class);
    }
}
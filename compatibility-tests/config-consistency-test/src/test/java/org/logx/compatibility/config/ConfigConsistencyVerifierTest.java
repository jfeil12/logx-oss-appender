package org.logx.compatibility.config;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * 配置一致性验证工具测试
 */
public class ConfigConsistencyVerifierTest {

    @Test
    public void testConfigConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建真实配置
        Map<String, String> logbackConfig = createRealConfig();
        Map<String, String> log4j2Config = createRealConfig();
        Map<String, String> log4j1Config = createRealConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告一致性
        assertTrue("配置应该是一致的", report.isConsistent());
    }

    @Test
    public void testEnvironmentVariableConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建真实环境变量
        Map<String, String> envVars = createRealEnvironmentVariables();
        
        // 验证环境变量一致性
        ConfigConsistencyReport report = verifier.verifyEnvironmentVariableConsistency(envVars);
        
        // 验证报告一致性
        assertTrue("环境变量应该是一致的", report.isConsistent());
    }

    @Test
    public void testValidationMechanismConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 验证配置验证机制一致性
        ConfigConsistencyReport report = verifier.verifyValidationMechanismConsistency();
        
        // 验证报告一致性
        assertTrue("配置验证机制应该是一致的", report.isConsistent());
    }

    @Test
    public void testErrorHandlingConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 验证错误处理一致性
        ConfigConsistencyReport report = verifier.verifyErrorHandlingConsistency();
        
        // 验证报告一致性
        assertTrue("错误处理应该是一致的", report.isConsistent());
    }

    @Test
    public void testConfigInconsistencyDetection() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建不一致的配置
        Map<String, String> logbackConfig = createRealConfig();
        Map<String, String> log4j2Config = createRealConfig();
        Map<String, String> log4j1Config = createInconsistentRealConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告不一致性 (Log4j1缺少9个参数)
        assertFalse("配置应该是不一致的", report.isConsistent());
        assertEquals("应该检测到不一致的参数", 9, report.getInconsistentParameters().size());
    }

    @Test
    public void testEnvironmentVariableInconsistencyDetection() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建不一致的环境变量
        Map<String, String> envVars = createInconsistentEnvironmentVariables();
        
        // 验证环境变量一致性
        ConfigConsistencyReport report = verifier.verifyEnvironmentVariableConsistency(envVars);
        
        // 验证报告不一致性 (缺少5个环境变量)
        assertFalse("环境变量应该是不一致的", report.isConsistent());
        assertEquals("应该检测到不一致的环境变量", 5, report.getInconsistentParameters().size());
    }

    @Test
    public void testReportContentVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建真实配置
        Map<String, String> logbackConfig = createRealConfig();
        Map<String, String> log4j2Config = createRealConfig();
        Map<String, String> log4j1Config = createRealConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告内容 (12个参数 × 3个框架 = 36个一致的参数)
        assertEquals("应该有36个一致的参数", 36, report.getConsistentParameters().size());
        assertEquals("应该没有不一致的参数", 0, report.getInconsistentParameters().size());
    }

    private Map<String, String> createRealConfig() {
        Map<String, String> config = new HashMap<>();
        // 使用真实的MinIO配置（符合minio/README-MINIO.md规范）
        // 存储配置
        config.put("logx.oss.storage.bucket", "logx-test-bucket");
        config.put("logx.oss.storage.keyPrefix", "logx/");
        config.put("logx.oss.storage.region", "US");
        config.put("logx.oss.storage.accessKeyId", "minioadmin");
        config.put("logx.oss.storage.accessKeySecret", "minioadmin");
        config.put("logx.oss.storage.endpoint", "http://localhost:9000");
        config.put("logx.oss.storage.ossType", "S3");
        config.put("logx.oss.storage.pathStyleAccess", "true");
        config.put("logx.oss.storage.enableSsl", "false");
        config.put("logx.oss.storage.maxConnections", "50");
        config.put("logx.oss.storage.connectTimeout", "30000");
        config.put("logx.oss.storage.readTimeout", "60000");
        return config;
    }

    private Map<String, String> createInconsistentRealConfig() {
        Map<String, String> config = new HashMap<>();
        // 缺少一些关键配置参数（使用新的两层结构）
        config.put("logx.oss.storage.bucket", "logx-test-bucket");
        config.put("logx.oss.storage.keyPrefix", "logx/");
        // 缺少 logx.oss.storage.region
        config.put("logx.oss.storage.accessKeyId", "minioadmin");
        // 缺少 logx.oss.storage.accessKeySecret
        // 缺少其他参数
        return config;
    }

    private Map<String, String> createRealEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        // 存储配置环境变量
        envVars.put("LOGX_OSS_STORAGE_ENDPOINT", "http://localhost:9000");
        envVars.put("LOGX_OSS_STORAGE_REGION", "US");
        envVars.put("LOGX_OSS_STORAGE_ACCESS_KEY_ID", "minioadmin");
        envVars.put("LOGX_OSS_STORAGE_ACCESS_KEY_SECRET", "minioadmin");
        envVars.put("LOGX_OSS_STORAGE_BUCKET", "logx-test-bucket");
        envVars.put("LOGX_OSS_STORAGE_KEY_PREFIX", "logx/");
        envVars.put("LOGX_OSS_STORAGE_OSS_TYPE", "S3");
        // 引擎配置环境变量
        envVars.put("LOGX_OSS_ENGINE_MAX_UPLOAD_SIZE_MB", "20");
        return envVars;
    }

    private Map<String, String> createInconsistentEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        // 使用新的两层结构环境变量
        envVars.put("LOGX_OSS_STORAGE_BUCKET", "logx-test-bucket");
        envVars.put("LOGX_OSS_STORAGE_KEY_PREFIX", "logx/");
        // 缺少 LOGX_OSS_STORAGE_REGION
        envVars.put("LOGX_OSS_STORAGE_ACCESS_KEY_ID", "minioadmin");
        // 缺少 LOGX_OSS_STORAGE_ACCESS_KEY_SECRET
        // 缺少其他参数
        return envVars;
    }
}
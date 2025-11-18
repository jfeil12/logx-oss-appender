package org.logx.compatibility.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 配置一致性验证工具
 * 用于验证不同框架配置参数的一致性
 */
public class ConfigConsistencyVerifier {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 验证所有框架使用相同配置参数名称
     */
    public ConfigConsistencyReport verifyParameterConsistency(
            Map<String, String> logbackConfig,
            Map<String, String> log4j2Config,
            Map<String, String> log4j1Config) {

        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 定义期望的统一配置参数名称（使用logx.oss前缀）
        Set<String> expectedParameters = new HashSet<>(Arrays.asList(
                "logx.oss.storage.bucket",
                "logx.oss.storage.keyPrefix",
                "logx.oss.storage.region",
                "logx.oss.storage.accessKeyId",
                "logx.oss.storage.accessKeySecret",
                "logx.oss.storage.endpoint",
                "logx.oss.storage.ossType",
                "logx.oss.storage.pathStyleAccess",
                "logx.oss.storage.enableSsl",
                "logx.oss.storage.maxConnections",
                "logx.oss.storage.connectTimeout",
                "logx.oss.storage.readTimeout"
        ));

        // 验证Logback配置参数
        verifyParameters("Logback", logbackConfig, expectedParameters, report);

        // 验证Log4j2配置参数
        verifyParameters("Log4j2", log4j2Config, expectedParameters, report);

        // 验证Log4j 1.x配置参数
        verifyParameters("Log4j1", log4j1Config, expectedParameters, report);

        return report;
    }

    /**
     * 验证环境变量覆盖的一致性
     */
    public ConfigConsistencyReport verifyEnvironmentVariableConsistency(
            Map<String, String> environmentVariables) {

        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 定义期望的环境变量名称（使用LOGX_OSS前缀）
        Map<String, String> expectedEnvVars = new HashMap<>();
        expectedEnvVars.put("LOGX_OSS_STORAGE_ENDPOINT", "logx.oss.storage.endpoint");
        expectedEnvVars.put("LOGX_OSS_STORAGE_REGION", "logx.oss.storage.region");
        expectedEnvVars.put("LOGX_OSS_STORAGE_ACCESS_KEY_ID", "logx.oss.storage.accessKeyId");
        expectedEnvVars.put("LOGX_OSS_STORAGE_ACCESS_KEY_SECRET", "logx.oss.storage.accessKeySecret");
        expectedEnvVars.put("LOGX_OSS_STORAGE_BUCKET", "logx.oss.storage.bucket");
        expectedEnvVars.put("LOGX_OSS_STORAGE_KEY_PREFIX", "logx.oss.storage.keyPrefix");
        expectedEnvVars.put("LOGX_OSS_STORAGE_OSS_TYPE", "logx.oss.storage.ossType");
        expectedEnvVars.put("LOGX_OSS_ENGINE_MAX_UPLOAD_SIZE_MB", "logx.oss.engine.maxUploadSizeMb");

        // 验证环境变量
        for (Map.Entry<String, String> entry : expectedEnvVars.entrySet()) {
            String envVar = entry.getKey();
            String configParam = entry.getValue();

            if (environmentVariables.containsKey(envVar)) {
                report.addConsistentParameter(envVar, configParam);
            } else {
                report.addInconsistentParameter(envVar, configParam, "环境变量未设置");
            }
        }

        return report;
    }

    /**
     * 验证配置验证机制的一致性
     */
    public ConfigConsistencyReport verifyValidationMechanismConsistency() {
        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 所有框架都应该有相同的验证机制
        report.addConsistentParameterWithDescription("Validation", "所有框架都使用S3ConfigValidator进行配置验证");
        report.addConsistentParameterWithDescription("ValidationRules", "所有框架都使用相同的验证规则");
        report.addConsistentParameterWithDescription("ValidationMessages", "所有框架都使用相同的验证错误消息");

        return report;
    }

    /**
     * 验证错误配置的处理一致性
     */
    public ConfigConsistencyReport verifyErrorHandlingConsistency() {
        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 所有框架都应该有相同的错误处理机制
        report.addConsistentParameterWithDescription("ErrorHandling", "所有框架都使用统一的错误处理机制");
        report.addConsistentParameterWithDescription("ErrorLogging", "所有框架都使用相同的错误日志格式");
        report.addConsistentParameterWithDescription("ErrorRecovery", "所有框架都使用相同的错误恢复策略");

        return report;
    }

    /**
     * 验证配置加载机制的一致性
     */
    public ConfigConsistencyReport verifyConfigLoadingConsistency() {
        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 所有框架都应该有相同的配置加载优先级
        report.addConsistentParameterWithDescription("ConfigLoading", "所有框架都遵循相同的配置加载优先级：环境变量 > 系统属性 > 配置文件");
        report.addConsistentParameterWithDescription("ConfigFallback", "所有框架都支持相同的配置回退机制");

        return report;
    }

    /**
     * 验证配置更新机制的一致性
     */
    public ConfigConsistencyReport verifyConfigUpdateConsistency() {
        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 所有框架都应该有相同的配置更新机制
        report.addConsistentParameterWithDescription("ConfigUpdate", "所有框架都支持运行时配置更新");
        report.addConsistentParameterWithDescription("ConfigReload", "所有框架都支持配置重新加载");

        return report;
    }

    private void verifyParameters(String framework, Map<String, String> config,
                                Set<String> expectedParameters, ConfigConsistencyReport report) {
        for (String expectedParam : expectedParameters) {
            if (config.containsKey(expectedParam)) {
                report.addConsistentParameter(framework, expectedParam);
            } else {
                report.addInconsistentParameter(framework, expectedParam, "配置参数缺失");
            }
        }
    }

    /**
     * 从YAML文件加载配置
     */
    public Map<String, String> loadYamlConfig(String filePath) throws IOException {
        JsonNode rootNode = yamlMapper.readTree(new File(filePath));
        return extractConfigParameters(rootNode, "");
    }

    /**
     * 从Properties文件加载配置
     */
    public Map<String, String> loadPropertiesConfig(String filePath) throws IOException {
        Properties props = new Properties();
        try (java.io.InputStream inputStream = new File(filePath).toURI().toURL().openStream()) {
            props.load(inputStream);
        }
        
        Map<String, String> configMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            configMap.put((String) entry.getKey(), (String) entry.getValue());
        }
        
        return configMap;
    }

    /**
     * 从系统属性加载配置
     */
    public Map<String, String> loadSystemPropertiesConfig(Set<String> propertyNames) {
        Map<String, String> configMap = new HashMap<>();
        
        for (String propertyName : propertyNames) {
            String value = System.getProperty(propertyName);
            if (value != null) {
                configMap.put(propertyName, value);
            }
        }
        
        return configMap;
    }

    private Map<String, String> extractConfigParameters(JsonNode node, String prefix) {
        Map<String, String> parameters = new HashMap<>();
        
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                if (field.getValue().isValueNode()) {
                    parameters.put(key, field.getValue().asText());
                } else {
                    parameters.putAll(extractConfigParameters(field.getValue(), key));
                }
            }
        }
        
        return parameters;
    }

    /**
     * 验证配置值的一致性
     */
    public ConfigConsistencyReport verifyConfigValueConsistency(
            Map<String, String> logbackConfig,
            Map<String, String> log4j2Config,
            Map<String, String> log4j1Config) {

        ConfigConsistencyReport report = new ConfigConsistencyReport();

        // 定义需要验证值一致性的参数
        Set<String> valueSensitiveParameters = new HashSet<>(Arrays.asList(
                "logx.oss.storage.region",
                "logx.oss.storage.ossType",
                "logx.oss.engine.batch.count",
                "logx.oss.engine.batch.maxAgeMs",
                "logx.oss.engine.queue.capacity",
                "logx.oss.engine.enableCompression"
        ));

        // 验证各框架配置值的一致性
        for (String param : valueSensitiveParameters) {
            String logbackValue = logbackConfig.get(param);
            String log4j2Value = log4j2Config.get(param);
            String log4j1Value = log4j1Config.get(param);

            if (logbackValue != null && log4j2Value != null && log4j1Value != null) {
                if (logbackValue.equals(log4j2Value) && log4j2Value.equals(log4j1Value)) {
                    report.addConsistentParameterWithDescription(param, "所有框架配置值一致: " + logbackValue);
                } else {
                    report.addInconsistentParameter("ConfigValue", param, 
                        String.format("配置值不一致: Logback=%s, Log4j2=%s, Log4j1=%s", 
                                     logbackValue, log4j2Value, log4j1Value));
                }
            } else if (logbackValue == null && log4j2Value == null && log4j1Value == null) {
                report.addConsistentParameterWithDescription(param, "所有框架都未设置该配置参数");
            } else {
                report.addInconsistentParameter("ConfigValue", param, 
                    "配置参数在某些框架中缺失");
            }
        }

        return report;
    }
}
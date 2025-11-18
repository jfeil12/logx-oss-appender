package org.logx.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.logx.config.exception.MissingPlaceholderException;
import org.logx.config.properties.LogxOssProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一配置管理器
 * <p>
 * 支持多种配置源的优先级读取，按以下顺序：
 * <ol>
 * <li>JVM系统属性 (-Dkey=value)</li>
 * <li>环境变量 (KEY=value)</li>
 * <li>配置文件属性 (logx.properties)</li>
 * <li>代码默认值</li>
 * </ol>
 * <p>
 * 支持配置键的命名规范转换，自动处理不同格式之间的映射。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String DEFAULT_CONFIG_FILE = "logx.properties";

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private Properties fileProperties;
    // 配置文件路径
    private String configFilePath;

    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 构造配置管理器
     */
    public ConfigManager() {
        try {
            this.configFilePath = DEFAULT_CONFIG_FILE;
            loadFileProperties();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ConfigManager", e);
        }
    }

    /**
     * 构造配置管理器，指定配置文件路径
     *
     * @param configFilePath
     *            配置文件路径
     */
    public ConfigManager(String configFilePath) {
        try {
            this.configFilePath = configFilePath;
            loadFileProperties(configFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ConfigManager with config file: " + configFilePath, e);
        }
    }

    /**
     * 获取配置值，按优先级顺序查找
     * <p>
     * 支持多种命名格式：
     * <ul>
     * <li>JVM系统属性：支持点号格式（logx.oss.storage.endpoint）和大写下划线格式（LOGX_OSS_STORAGE_ENDPOINT），点号格式优先</li>
     * <li>环境变量：只支持大写下划线格式（LOGX_OSS_STORAGE_ENDPOINT）</li>
     * <li>配置文件：标准点号格式（logx.oss.storage.endpoint）</li>
     * </ul>
     * <p>
     * 优先级顺序（从高到低）：
     * <ol>
     * <li>JVM系统属性</li>
     * <li>环境变量</li>
     * <li>配置文件属性</li>
     * </ol>
     * <p>
     * 如果所有配置源都找不到，返回null。默认值处理由调用方（如ConfigFactory）负责。
     *
     * @param key
     *            配置键（使用点号格式，如 logx.oss.storage.endpoint）
     *
     * @return 配置值，如果不存在返回null
     */
    public String getProperty(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        // 检查缓存
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }

        // 优先级1: JVM系统属性
        String value = getSystemProperty(key);
        if (value != null) {
            value = ConfigManager.resolvePlaceholders(value);
            configCache.put(key, value);
            return value;
        }

        // 优先级2: 环境变量 (支持不同命名格式)
        value = getEnvironmentVariable(key);
        if (value != null) {
            value = ConfigManager.resolvePlaceholders(value);
            configCache.put(key, value);
            return value;
        }

        // 优先级3: 配置文件属性
        value = getFileProperty(key);
        if (value != null) {
            value = ConfigManager.resolvePlaceholders(value);
            configCache.put(key, value);
            return value;
        }

        // 未找到配置，返回null
        return null;
    }

    /**
     * 获取配置值，如果不存在返回默认值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 配置值或默认值
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取高优先级配置值（不包括ConfigManager默认值）
     * <p>
     * 只查找JVM系统属性、环境变量和配置文件，不使用ConfigManager的默认值
     * <p>
     * 用于支持XML中的默认值语法 ${ENV:-xmlDefault}，确保XML默认值优先于ConfigManager默认值
     *
     * @param key
     *            配置键（使用点号格式，如 logx.oss.storage.endpoint）
     *
     * @return 高优先级配置值，如果不存在返回null
     */
    public String getPropertyWithoutDefaults(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        // 优先级1: JVM系统属性
        String value = getSystemProperty(key);
        if (value != null) {
            return value;
        }

        // 优先级2: 环境变量
        value = getEnvironmentVariable(key);
        if (value != null) {
            return value;
        }

        // 优先级3: 配置文件属性
        value = getFileProperty(key);
        if (value != null) {
            return value;
        }

        return null;
    }

    /**
     * 获取整数配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 整数配置值
     *
     * @throws NumberFormatException
     *             如果配置值不是有效整数
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for key '" + key + "': " + value, e);
        }
    }

    /**
     * 获取长整数配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 长整数配置值
     *
     * @throws NumberFormatException
     *             如果配置值不是有效长整数
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long value for key '" + key + "': " + value, e);
        }
    }

    /**
     * 获取布尔配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 布尔配置值
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmedValue = value.trim().toLowerCase(java.util.Locale.ENGLISH);
        return "true".equals(trimmedValue) || "yes".equals(trimmedValue) || "1".equals(trimmedValue);
    }

    public LogxOssProperties getLogxOssProperties() {
        LogxOssConfigResolver resolver = new LogxOssConfigResolver(this);
        return resolver.resolve();
    }

    /**
     * 清除配置缓存
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 重新加载配置文件
     */
    public void reload() {
        clearCache();
        loadFileProperties(this.configFilePath);
    }

    /**
     * 重新加载指定配置文件
     *
     * @param configFilePath
     *            配置文件路径
     */
    public void reload(String configFilePath) {
        clearCache();
        loadFileProperties(configFilePath);
    }

    /**
     * 获取所有配置的副本
     *
     * @return 配置映射的副本
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> allProperties = new HashMap<>();

        // 添加文件属性
        if (fileProperties != null) {
            for (String key : fileProperties.stringPropertyNames()) {
                allProperties.put(key, fileProperties.getProperty(key));
            }
        }

        // 添加环境变量 (只添加已知的配置键)
        for (String key : allProperties.keySet()) {
            String envValue = getEnvironmentVariable(key);
            if (envValue != null) {
                allProperties.put(key, envValue);
            }
        }

        // 添加所有系统属性
        Properties systemProperties = System.getProperties();
        for (String key : systemProperties.stringPropertyNames()) {
            allProperties.put(key, systemProperties.getProperty(key));
        }

        return allProperties;
    }

    /**
     * 从JVM系统属性获取值，支持两种命名风格
     * <p>
     * 支持的格式（按优先级）：
     * <ul>
     * <li>logx.oss.storage.accessKeyId （标准点号格式，优先）</li>
     * <li>LOGX_OSS_STORAGE_ACCESS_KEY_ID （大写下划线格式，支持驼峰转换）</li>
     * </ul>
     *
     * @param key
     *            配置键
     *
     * @return JVM系统属性值，如果不存在返回null
     */
    private String getSystemProperty(String key) {
        // 1. 先查找原始键（标准点号格式：logx.oss.storage.accessKeyId）
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }

        // 2. 转换为大写下划线格式，支持驼峰命名（LOGX_OSS_STORAGE_ACCESS_KEY_ID）
        String upperKey = toEnvironmentVariableFormat(key);
        value = System.getProperty(upperKey);
        if (value != null) {
            return value;
        }

        return null;
    }

    /**
     * 从配置文件获取值
     *
     * @param key
     *            配置键
     *
     * @return 配置文件属性值，如果不存在返回null
     */
    private String getFileProperty(String key) {
        if (fileProperties == null) {
            return null;
        }

        return fileProperties.getProperty(key);
    }

    /**
     * 从环境变量获取值
     * <p>
     * 只支持大写下划线格式，因为大多数shell（如bash）不支持点号作为环境变量名
     * <p>
     * 转换规则：处理驼峰命名 + 大写 + 点号转下划线
     * <p>
     * 示例：
     * <ul>
     * <li>logx.oss.storage.endpoint → LOGX_OSS_STORAGE_ENDPOINT</li>
     * <li>logx.oss.storage.accessKeyId → LOGX_OSS_STORAGE_ACCESS_KEY_ID</li>
     * <li>logx.oss.engine.batch.count → LOGX_OSS_ENGINE_BATCH_COUNT</li>
     * </ul>
     *
     * @param key
     *            配置键
     *
     * @return 环境变量值，如果不存在返回null
     */
    private String getEnvironmentVariable(String key) {
        // 转换为大写下划线格式，支持驼峰命名
        String envKey = toEnvironmentVariableFormat(key);
        return System.getenv(envKey);
    }

    /**
     * 将配置键转换为环境变量格式
     * <p>
     * 转换规则：
     * <ol>
     * <li>处理驼峰命名：在小写字母后紧跟大写字母的位置插入下划线</li>
     * <li>全部转大写</li>
     * <li>点号替换为下划线</li>
     * </ol>
     *
     * @param key
     *            配置键（如 logx.oss.storage.accessKeyId）
     *
     * @return 环境变量格式（如 LOGX_OSS_STORAGE_ACCESS_KEY_ID）
     */
    private String toEnvironmentVariableFormat(String key) {
        // 1. 处理驼峰：在小写字母后紧跟大写字母的位置插入下划线
        String result = key.replaceAll("([a-z])([A-Z])", "$1_$2");
        // 2. 转大写
        result = result.toUpperCase(java.util.Locale.ENGLISH);
        // 3. 点号替换为下划线
        result = result.replace('.', '_');
        return result;
    }

    /**
     * 加载默认配置文件
     */
    private void loadFileProperties() {
        loadFileProperties(DEFAULT_CONFIG_FILE);
    }

    /**
     * 加载指定配置文件
     * <p>
     * 对于默认配置文件（logx.properties），按以下优先级加载：
     * <ol>
     * <li>/app/deploy/conf/logx.properties - 生产环境配置目录（最高优先级）</li>
     * <li>classpath:logx.properties - 类路径下的配置文件</li>
     * <li>./logx.properties - 当前目录下的配置文件</li>
     * </ol>
     * 对于自定义配置文件路径，保持原有加载逻辑。
     *
     * @param configFilePath
     *            配置文件路径
     */
    private void loadFileProperties(String configFilePath) {
        fileProperties = new Properties();

        // 如果是默认配置文件，优先从生产环境目录加载
        if (DEFAULT_CONFIG_FILE.equals(configFilePath)) {
            String productionConfigPath = "/app/deploy/conf/" + DEFAULT_CONFIG_FILE;
            try (FileInputStream input = new FileInputStream(productionConfigPath)) {
                fileProperties.load(input);
                logger.info("配置文件加载成功: {}", productionConfigPath);
                return;
            } catch (IOException e) {
                // 文件不存在，继续尝试其他路径
            }
        }

        // 尝试从类路径加载
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
            if (input != null) {
                fileProperties.load(input);
                logger.info("配置文件加载成功: classpath:{}", configFilePath);
                return;
            }
        } catch (IOException e) {
            // 忽略，继续尝试其他方式
        }

        // 尝试从文件系统加载
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            fileProperties.load(input);
            logger.info("配置文件加载成功: {}", configFilePath);
        } catch (IOException e) {
            // 配置文件不存在或无法读取，使用空属性
            logger.warn("未找到配置文件: {}，将使用空配置", configFilePath);
            fileProperties = new Properties();
        }
    }

    /**
     * 解析包含变量占位符的字符串
     * <p>
     * 支持的格式：
     * <ul>
     * <li>${ENV_VAR:-default} - bash风格，使用:-作为分隔符</li>
     * <li>${ENV_VAR:default} - 简化风格，使用:作为分隔符</li>
     * <li>${ENV_VAR} - 只有变量名，无默认值</li>
     * </ul>
     * <p>
     * 解析优先级：JVM系统属性 > 环境变量 > 默认值
     *
     * @param value
     *            可能包含占位符的字符串
     *
     * @return 解析后的字符串，如果无法解析返回原始值
     */
    public static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        String result = value;
        int startIndex = 0;

        while (true) {
            int placeholderStart = result.indexOf("${", startIndex);
            if (placeholderStart == -1) {
                break;
            }

            int placeholderEnd = result.indexOf("}", placeholderStart);
            if (placeholderEnd == -1) {
                break;
            }

            String placeholder = result.substring(placeholderStart + 2, placeholderEnd);
            String resolvedValue = resolveSinglePlaceholder(placeholder);

            if (resolvedValue == null) {
                throw new MissingPlaceholderException("Cannot resolve required placeholder ${" + placeholder + "}");
            }

            result = result.substring(0, placeholderStart) + resolvedValue + result.substring(placeholderEnd + 1);
            startIndex = placeholderStart + resolvedValue.length();
        }

        return result;
    }

    /**
     * 批量解析Map中所有值的占位符
     * <p>
     * 遍历Map中的所有条目，对每个值调用 {@link #resolvePlaceholders(String)} 进行占位符解析，
     * 原Map会被就地修改。
     * <p>
     * 支持的占位符格式：
     * <ul>
     * <li>${ENV_VAR:-default} - bash风格，使用:-作为分隔符</li>
     * <li>${ENV_VAR:default} - 简化风格，使用:作为分隔符</li>
     * <li>${ENV_VAR} - 只有变量名，无默认值</li>
     * </ul>
     *
     * @param configMap
     *            包含可能含有占位符的配置Map，将被就地修改
     */
    public static void resolveMapPlaceholders(Map<String, String> configMap) {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String resolvedValue = resolvePlaceholders(entry.getValue());
            resolved.put(entry.getKey(), resolvedValue);
        }
        configMap.clear();
        configMap.putAll(resolved);
    }

    /**
     * 解析单个占位符
     *
     * @param placeholder
     *            占位符内容（不包括${}）
     *
     * @return 解析后的值，如果无法解析返回null
     */
    private static String resolveSinglePlaceholder(String placeholder) {
        String varName;
        String defaultValue = null;

        // 支持:-语法（bash风格）
        int separatorIndex = placeholder.indexOf(":-");
        if (separatorIndex > 0) {
            varName = placeholder.substring(0, separatorIndex);
            defaultValue = placeholder.substring(separatorIndex + 2);
        } else {
            // 支持:语法（简化风格）
            separatorIndex = placeholder.indexOf(':');
            if (separatorIndex > 0) {
                varName = placeholder.substring(0, separatorIndex);
                defaultValue = placeholder.substring(separatorIndex + 1);
            } else {
                varName = placeholder;
            }
        }

        // 优先级1: JVM系统属性（支持点号格式和大写下划线格式）
        String value = System.getProperty(varName);
        if (value != null) {
            return value;
        }

        String upperVarName = varName.toUpperCase(java.util.Locale.ENGLISH).replace('.', '_');
        value = System.getProperty(upperVarName);
        if (value != null) {
            return value;
        }

        // 优先级2: 环境变量（只支持大写下划线格式）
        value = System.getenv(upperVarName);
        if (value != null) {
            return value;
        }

        // 优先级3: 返回默认值
        return defaultValue;
    }
}

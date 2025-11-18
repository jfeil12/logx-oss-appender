package org.logx.config.validator;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置验证器工厂
 * <p>
 * 提供统一的配置验证器获取和管理机制。
 *
 * @author OSS Appender Team
 *
 * @since 1.1.0
 */
public class ConfigValidatorFactory {

    private final Map<Class<?>, ConfigValidator> validators = new ConcurrentHashMap<>();

    private ConfigValidatorFactory() {
        // 注册默认验证器
        // 注意：具体的验证器实现已移到适配器模块中
    }

    /**
     * 获取工厂实例
     *
     * @return 工厂实例
     */
    public static ConfigValidatorFactory getInstance() {
        // 返回工厂的副本以避免内部表示暴露
        return new ConfigValidatorFactory();
    }

    /**
     * 注册配置验证器
     *
     * @param configType
     *            配置类型
     * @param validator
     *            验证器实例
     * @param <T>
     *            配置类型
     */
    public <T> void registerValidator(Class<T> configType, ConfigValidator validator) {
        validators.put(configType, validator);
    }

    /**
     * 获取配置验证器
     *
     * @param configType
     *            配置类型
     * @param <T>
     *            配置类型
     *
     * @return 配置验证器，如果未找到则返回null
     */
    public <T> ConfigValidator getValidator(Class<T> configType) {
        return validators.get(configType);
    }

    /**
     * 获取配置验证器，如果未找到则抛出异常
     *
     * @param configType
     *            配置类型
     * @param <T>
     *            配置类型
     *
     * @return 配置验证器
     *
     * @throws IllegalArgumentException
     *             如果未找到对应的验证器
     */
    public <T> ConfigValidator getValidatorOrThrow(Class<T> configType) {
        ConfigValidator validator = getValidator(configType);
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for config type: " + configType.getSimpleName());
        }
        return validator;
    }

    /**
     * 验证配置
     *
     * @param config
     *            配置对象
     *
     * @return 验证结果
     */
    public ConfigValidator.ValidationResult validate(Object config) {
        return validate(config, ConfigValidator.ValidationOptions.defaultOptions());
    }

    /**
     * 验证配置
     *
     * @param config
     *            配置对象
     * @param options
     *            验证选项
     *
     * @return 验证结果
     */
    public ConfigValidator.ValidationResult validate(Object config, ConfigValidator.ValidationOptions options) {
        if (config == null) {
            List<ConfigValidator.ValidationError> errors = new ArrayList<>();
            errors.add(new ConfigValidator.ValidationError("config", "Configuration object is null",
                            "Provide a valid configuration object", ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
            return ConfigValidator.ValidationResult.invalid(errors);
        }

        ConfigValidator validator = getValidator(config.getClass());
        if (validator == null) {
            List<ConfigValidator.ValidationError> errors = new ArrayList<>();
            errors.add(new ConfigValidator.ValidationError("config", "No validator found for config type: " + config.getClass().getSimpleName(),
                            "Register a validator for this config type", ConfigValidator.ErrorType.BUSINESS_LOGIC_ERROR));
            return ConfigValidator.ValidationResult.invalid(errors);
        }

        return validator.validate(config, options);
    }
    
    /**
     * 仅供测试使用：清理所有注册的验证器
     */
    public void clearNonDefaultValidators() {
        // 清理所有验证器
        validators.clear();
    }
}
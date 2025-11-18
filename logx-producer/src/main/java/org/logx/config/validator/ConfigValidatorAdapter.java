package org.logx.config.validator;

import java.util.List;
import java.util.ArrayList;

/**
 * 通用配置验证器适配器
 * <p>
 * 提供适配器模式，便于为不同类型的配置对象创建验证器。
 *
 * @param <T>
 *            配置对象类型
 *
 * @author OSS Appender Team
 *
 * @since 1.1.0
 */
public abstract class ConfigValidatorAdapter<T> implements ConfigValidator {

    private final Class<T> configType;

    protected ConfigValidatorAdapter(Class<T> configType) {
        this.configType = configType;
    }

    @Override
    public final ValidationResult validate(Object config) {
        return validate(config, ValidationOptions.defaultOptions());
    }

    @Override
    public final ValidationResult validate(Object config, ValidationOptions options) {
        if (config == null) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("config", "Configuration object is null", "Provide a valid configuration object",
                            ErrorType.REQUIRED_FIELD_MISSING));
            return ValidationResult.invalid(errors);
        }

        if (!configType.isInstance(config)) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("config",
                    "Expected configuration type: " + configType.getSimpleName() + ", but got: "
                            + config.getClass().getSimpleName(),
                    "Provide a configuration object of type: " + configType.getSimpleName(),
                    ErrorType.BUSINESS_LOGIC_ERROR));
            return ValidationResult.invalid(errors);
        }

        T typedConfig = configType.cast(config);
        return validateConfig(typedConfig, options);
    }

    /**
     * 验证特定类型的配置对象
     *
     * @param config
     *            配置对象
     * @param options
     *            验证选项
     *
     * @return 验证结果
     */
    protected abstract ValidationResult validateConfig(T config, ValidationOptions options);

    /**
     * 获取配置类型
     *
     * @return 配置类型
     */
    public Class<T> getConfigType() {
        return configType;
    }
}
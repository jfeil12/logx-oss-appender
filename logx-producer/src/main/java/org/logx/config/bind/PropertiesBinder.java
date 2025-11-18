package org.logx.config.bind;

import org.logx.config.env.ConfigurationEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PropertiesBinder {

    private final ConfigurationEnvironment environment;

    public PropertiesBinder(ConfigurationEnvironment environment) {
        this.environment = environment;
    }

    public void bind(String prefix, Object target) {
        try {
            bind(prefix, target, target.getClass());
        } catch (Exception e) {
            throw new RuntimeException("Could not bind properties: " + e.getMessage(), e);
        }
    }

    private void bind(String prefix, Object target, Class<?> type) throws Exception {
        for (Field field : type.getDeclaredFields()) {
            String propertyName = prefix + "." + field.getName();
            String value = environment.getProperty(propertyName);

            if (value != null) {
                setField(target, field, value);
            } else if (isComplexType(field.getType())) {
                Object nestedTarget = getField(target, field);
                if (nestedTarget == null) {
                    nestedTarget = field.getType().newInstance();
                    setField(target, field, nestedTarget);
                }
                bind(propertyName, nestedTarget, field.getType());
            }
        }
    }

    private void setField(Object target, Field field, Object value) throws Exception {
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        Method setter = findMethod(target.getClass(), setterName, field.getType());
        if (setter != null) {
            Object convertedValue = convert(value.toString(), field.getType());
            setter.invoke(target, convertedValue);
        }
    }

    private Object getField(Object target, Field field) throws Exception {
        String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        Method getter = findMethod(target.getClass(), getterName);
        if (getter != null) {
            return getter.invoke(target);
        }
        return null;
    }

    private Method findMethod(Class<?> type, String name, Class<?>... paramTypes) {
        try {
            return type.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean isComplexType(Class<?> type) {
        return !type.isPrimitive() && !type.getName().startsWith("java.lang");
    }

    private Object convert(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }
}

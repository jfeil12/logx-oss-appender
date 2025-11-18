package org.logx.config.env;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfigurationEnvironment {
    private final List<PropertySource> propertySources = new CopyOnWriteArrayList<>();

    public ConfigurationEnvironment() {
        this.propertySources.add(new SystemPropertiesPropertySource());
        this.propertySources.add(new EnvironmentVariablesPropertySource());
    }

    public void addFirst(PropertySource source) {
        this.propertySources.add(0, source);
    }

    public String getProperty(String key) {
        for (PropertySource source : this.propertySources) {
            Object value = source.getProperty(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}

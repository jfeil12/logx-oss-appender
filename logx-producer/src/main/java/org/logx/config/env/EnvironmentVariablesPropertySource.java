package org.logx.config.env;

import java.util.regex.Pattern;

public class EnvironmentVariablesPropertySource implements PropertySource {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    @Override
    public Object getProperty(String name) {
        String envName = DOT_PATTERN.matcher(name).replaceAll("_").toUpperCase();
        return System.getenv(envName);
    }
}

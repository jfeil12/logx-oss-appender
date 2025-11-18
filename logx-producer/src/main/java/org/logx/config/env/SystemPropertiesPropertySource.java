package org.logx.config.env;

public class SystemPropertiesPropertySource implements PropertySource {

    @Override
    public Object getProperty(String name) {
        return System.getProperty(name);
    }
}

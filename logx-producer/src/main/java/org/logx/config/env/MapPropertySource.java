package org.logx.config.env;

import java.util.Map;

public class MapPropertySource implements PropertySource {

    private final Map<String, Object> source;

    public MapPropertySource(Map<String, Object> source) {
        this.source = source;
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}

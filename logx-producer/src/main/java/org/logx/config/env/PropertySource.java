package org.logx.config.env;

public interface PropertySource {
    Object getProperty(String name);
}

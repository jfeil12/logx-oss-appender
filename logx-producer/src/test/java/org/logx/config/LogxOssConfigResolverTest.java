package org.logx.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logx.config.properties.LogxOssProperties;

class LogxOssConfigResolverTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("logx.oss.engine.payloadMaxBytes");
    }

    @Test
    @DisplayName("应该将payloadMaxBytes配置注入到Engine属性")
    void shouldInjectPayloadMaxBytesIntoEngineProperties() {
        System.setProperty("logx.oss.engine.payloadMaxBytes", "4096");

        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        assertThat(properties.getEngine().getPayloadMaxBytes()).isEqualTo(4096);
    }
}

package org.logx.compatibility.jdk21;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JDK 21兼容性测试应用
 * 用于验证OSS Appender在JDK 21环境中的兼容性
 */
@SpringBootApplication
public class Jdk21CompatibilityTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(Jdk21CompatibilityTestApplication.class, args);
    }
}
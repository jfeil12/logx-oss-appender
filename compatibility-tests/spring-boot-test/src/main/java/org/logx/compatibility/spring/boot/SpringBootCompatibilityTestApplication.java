package org.logx.compatibility.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot兼容性测试应用
 * 用于验证OSS Appender在Spring Boot环境中的兼容性
 */
@SpringBootApplication
public class SpringBootCompatibilityTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCompatibilityTestApplication.class, args);
    }
}
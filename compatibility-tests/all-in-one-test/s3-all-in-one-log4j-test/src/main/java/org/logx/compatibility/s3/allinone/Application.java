package org.logx.compatibility.s3.allinone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.log4j.Logger;

@SpringBootApplication
@RestController
public class Application {

    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        logger.info("S3 All-in-One Log4j Test Application started successfully");

        // Trigger some test logs
        logger.info("This is an info message that should go to OSS");
        logger.warn("This is a warning message that should go to OSS");
        logger.error("This is an error message that should go to OSS");
    }

    @GetMapping("/test")
    public String testLogging() {
        logger.info("Received test request at " + System.currentTimeMillis());
        logger.debug("Debug message for testing LogX configuration");

        return "Test logs generated. Check OSS for the logs.";
    }
}
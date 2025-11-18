# All-in-One Integration Tests

This directory contains tests for the All-in-One packages:
1. s3-all-in-one-test - Tests for s3-logback-oss-appender
2. s3-all-in-one-log4j-test - Tests for s3-log4j-oss-appender
3. s3-all-in-one-log4j2-test - Tests for s3-log4j2-oss-appender

Each package is a Fat JAR that includes all necessary dependencies for logging to S3-compatible storage services.

## Test Structure

Each test uses the All-in-One JAR file directly rather than Maven dependencies, simulating how non-Maven users would integrate the packages.

## Running Tests

1. Build the All-in-One packages first:
   ```bash
   mvn clean package -pl all-in-one/s3-logback-oss-appender
   mvn clean package -pl all-in-one/s3-log4j-oss-appender
   mvn clean package -pl all-in-one/s3-log4j2-oss-appender
   ```

2. Copy the JAR files to the lib directories:
   ```bash
   cp all-in-one/s3-logback-oss-appender/target/s3-logback-oss-appender-1.0.0-SNAPSHOT.jar compatibility-tests/all-in-one-test/s3-all-in-one-test/lib/
   cp all-in-one/s3-log4j-oss-appender/target/s3-log4j-oss-appender-1.0.0-SNAPSHOT.jar compatibility-tests/all-in-one-test/s3-all-in-one-log4j-test/lib/
   cp all-in-one/s3-log4j2-oss-appender/target/s3-log4j2-oss-appender-1.0.0-SNAPSHOT.jar compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test/lib/
   ```

3. Run the tests individually:
   ```bash
   cd compatibility-tests/all-in-one-test/s3-all-in-one-test
   java -cp "lib/*:src/main/resources:." org.logx.compatibility.s3.allinone.S3AllInOneCompatibilityTestApplication
   ```
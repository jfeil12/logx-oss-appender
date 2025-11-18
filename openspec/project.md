# Project Context

## Purpose
Deliver a high-throughput, low-latency logging pipeline for Java applications that batches log events and uploads them asynchronously to S3-compatible object storage services such as Alibaba Cloud OSS, AWS S3, Tencent COS, MinIO, and SF OSS. The project ships production-ready appenders for Logback, Log4j, and Log4j2 plus a shared producer/adapter layer so teams can adopt managed cloud storage without rewriting their logging stack.

## Tech Stack
- Java 8 multi-module Maven build (parent `pom.xml` + logx-producer, logx-s3-adapter, log4j/log4j2/logback appenders, compatibility test suite, all-in-one fat JARs)
- Log frameworks: Logback 1.x, Log4j 1.x (1.2.17), Log4j2 2.x
- Async queue + batching engine in `logx-producer`
- S3/OSS adapter built on AWS S3 compatible APIs; validated against Alibaba OSS, Tencent COS, MinIO, SF OSS
- Testing stack: JUnit 5, Mockito, AssertJ, dedicated compatibility-tests modules
- Tooling: Maven Surefire, SpotBugs, formatter (Google Java Style), OWASP Dependency Check, MinIO for integration tests

## Project Conventions

### Code Style
Follow Google Java Style (see `docs/architecture/coding-standards.md`). All control statements require braces, no trailing inline comments, doc/comments appear on the line above code. Use PascalCase for classes, lowerCamelCase for methods/fields, and UPPER_SNAKE_CASE for constants. Keep methods cohesive and prefer builder/config objects over long parameter lists.

### Architecture Patterns
- Modularized parent POM orchestrating appenders (`log4j*`, `logback`), producer, and storage adapter.
- Producer implements asynchronous queue, backpressure, and batch-flush policies; appenders only translate framework log events into producer payloads.
- Storage adapter abstracts S3-compatible vendors via a single configuration namespace; shared defaults (region, batch sizes) live in docs.
- Samples/configurations live under `src/main/resources/examples` per module plus an all-in-one distribution for non-Maven users.

### Testing Strategy
- Unit/integration tests written with JUnit 5 + Mockito + AssertJ; mirror production packages under `src/test/java`.
- Compatibility suites (`compatibility-tests/*`) run cross-framework scenarios (Spring Boot, MVC, JSP/Servlet, multi-framework coexistence, config consistency) often against a MinIO sandbox.
- Core batching, retry, and async edge cases need coverage plus negative paths for storage failures.
- CI guidance: `mvn test`, targeted `mvn -Dtest=ClassName test`, and SpotBugs/dependency check profiles before release.

### Git Workflow
Use Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `chore:`). Prefer feature branches per change, open PRs with summary, affected modules, rationale, testing evidence, and perf/security considerations. Do not begin implementation work until related OpenSpec change proposals are reviewed.

## Domain Context
- Domain: reliable log fan-out from JVM apps to cloud object storage (OSS/S3/COS/MinIO). Key concerns are batching, durability, and keeping application threads free.
- Append-only object storage uploads must respect max object size, regional endpoints, and vendor-specific creds; configs are standardized so switching storage backends is low-friction.
- All-in-one fat JARs serve operations teams that cannot manage Maven dependencies but still need the same behavior.

## Important Constraints
- Java 8 baseline (no newer language features); artifacts target both on-prem and cloud JVMs.
- Throughput targets require default batch tuning (8,192 records, 10 MB) and queue backpressure; keep implementations non-blocking on logging threads.
- No secrets committed; use env variables/example properties. Ensure alignment with docs/DECISIONS defaults (region, retries, etc.).
- Avoid multi-framework coupling: each appender must remain framework-idiomatic while delegating shared logic to producer/adapter.

## External Dependencies
- Cloud object storage APIs: Alibaba Cloud OSS, AWS S3, Tencent COS, MinIO/S3-compatible endpoints, SF OSS.
- Build/test tooling: Maven Central dependencies (Jackson, AWS/Ali OSS SDKs), MinIO server for integration validation, SpotBugs/OWASP scans in CI.
- Deployment artifacts consumed by downstream JVM applications via Maven coordinates or all-in-one shaded JARs.

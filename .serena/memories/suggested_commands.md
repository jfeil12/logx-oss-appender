Key commands:
- `mvn clean install` – build all modules with formatting, SpotBugs, and tests
- `mvn test` or `mvn -Dtest=ClassName test` – run all or targeted unit tests
- `mvn -pl logx-producer -am install` – build a specific module plus deps
- `mvn spotbugs:check` – static analysis
- `mvn -Psecurity org.owasp:dependency-check-maven:check` – dependency vulnerability scan
- Compatibility tests: from `compatibility-tests`, run `mvn clean test -Pcompatibility-tests`
- Format/style validation: `mvn formatter:validate`
- Packaging all-in-one artifacts: run module-specific `mvn package`
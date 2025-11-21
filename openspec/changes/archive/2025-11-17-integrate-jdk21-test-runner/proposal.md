# Change: 在测试运行器中集成JDK 21兼容性测试

## Why
当前JDK 21兼容性测试模块(`compatibility-tests/jdk21-test`)是独立运行的，无法通过统一的测试运行器自动执行。为了确保每次发布前都能验证JDK 21的兼容性，需要将其集成到现有的`compatibility-tests/test-runner`中，实现一键运行所有兼容性测试，包括JDK 21特定测试。

## What Changes
- 在测试运行器中添加JDK 21兼容性测试模块支持
- 更新测试运行器的模块列表以包含jdk21-test
- 确保JDK 21测试可以与现有兼容性测试一起顺序执行

## Impact
- Affected specs: test-runner (需要更新测试运行器规范以包含新的测试模块)
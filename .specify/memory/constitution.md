<!--
═══════════════════════════════════════════════════════════════════════════════
SYNC IMPACT REPORT
═══════════════════════════════════════════════════════════════════════════════
Version Change: NEW → 1.0.0 (Initial ratification)

Modified Principles:
- All principles are newly defined (no previous version)

Added Sections:
- Core Principles (5 principles)
- 开发规范约束 (Development Standards Constraints)
- 治理与合规 (Governance and Compliance)

Removed Sections:
- None (initial version)

Templates Requiring Updates:
✅ plan-template.md - Constitution Check section reviewed (generic, no changes needed)
✅ spec-template.md - No constitution-specific constraints found
✅ tasks-template.md - No constitution-specific task types needed
⚠️ No commands directory found (.specify/templates/commands) - skipped

Follow-up TODOs:
- None (all placeholders filled)
═══════════════════════════════════════════════════════════════════════════════
-->

# LogX OSS Appender Constitution

## Core Principles

### I. 高性能异步处理 (High-Performance Async Processing)
所有日志处理必须使用异步队列，禁止阻塞业务线程。使用LMAX Disruptor实现低延迟队列，目标吞吐量100,000条日志/秒，平均延迟<3ms。队列内存占用必须<512MB，通过批处理、GZIP压缩和数据分片优化性能。

**理由**: 日志上传组件不能影响业务应用性能。异步处理确保日志记录操作的零阻塞，批处理和压缩优化网络传输效率，LMAX Disruptor提供高吞吐量和低延迟的队列实现。

### II. 日志零丢失保证 (Zero Log Loss Guarantee)
系统必须确保在高吞吐量负载下日志数据完整性。实现失败重试机制（指数退避，最多3次）、兜底文件机制（网络异常时本地缓存）和优雅关闭保护（30秒超时）。任何情况下不得因系统异常导致日志丢失。

**理由**: 日志数据对故障排查、审计和合规至关重要。零丢失保证确保在网络故障、系统崩溃等极端情况下日志数据的完整性和可追溯性。

### III. 多框架统一接口 (Multi-Framework Unified Interface)
支持Log4j 1.x、Log4j2、Logback三大日志框架，使用统一的配置键前缀`logx.oss.*`。所有框架适配器必须继承对应框架的基础类（AppenderSkeleton、AbstractAppender、AppenderBase），确保框架兼容性和一致的使用体验。

**理由**: Java生态系统中日志框架多样化，企业应用常使用不同框架。统一接口降低学习成本，简化运维配置，提升用户体验。

### IV. 多云存储适配 (Multi-Cloud Storage Adaptability)
通过Java SPI机制实现存储适配器的运行时动态加载，支持AWS S3、阿里云OSS、腾讯云COS、MinIO、SF OSS等S3兼容存储服务。存储后端可通过`ossType`配置参数运行时切换，实现零数据丢失的存储后端切换。

**理由**: 企业需要灵活选择云服务商或自建存储，避免供应商锁定。SPI机制实现低侵入性扩展，按需引入存储适配器减少依赖冲突。

### V. 企业级可靠性 (Enterprise-Grade Reliability)
采用固定线程池和低优先级调度保护应用资源，CPU让出机制防止影响业务性能。提供全面的错误处理、参数验证和配置兼容性检查。支持环境变量和配置文件的灵活配置方式，敏感信息必须使用环境变量。

**理由**: 日志组件作为基础设施，必须确保系统稳定性和资源可控性。固定线程池和低优先级调度避免资源无限扩张，环境变量配置提升安全性。

## 开发规范约束

### 代码质量强制要求
- **MUST** 使用中文沟通，包括代码说明、进度报告、提交信息、测试输出等（技术关键词、API名称、配置键名除外）
- **MUST** 注释在代码上一行，禁止尾行注释
- **MUST** 所有if/for/while语句使用大括号，即使只有一行代码
- **MUST** 禁止使用System.out.println，必须使用SLF4J日志框架
- **SHOULD** 优先使用import语句而非完全限定类名

**理由**: 统一的编码规范确保代码一致性和可维护性。中文沟通适应本地化需求，强制大括号避免潜在bug，SLF4J提供统一日志抽象。

### 测试覆盖要求
- **MUST** 单元测试覆盖率达到90%+，使用JUnit 5 + AssertJ + Mockito
- **MUST** 集成测试使用真实MinIO环境验证存储上传功能
- **MUST** 兼容性测试覆盖所有支持的日志框架组合（Log4j 1.x、Log4j2、Logback）
- **MUST** 性能测试验证核心指标：吞吐量100,000条/秒、延迟<3ms、内存<512MB

**理由**: 高覆盖率测试确保代码质量和回归预防。真实环境集成测试验证端到端功能，性能测试保证核心指标达标。

### 架构设计原则
- **MUST** 遵循分层抽象架构：核心引擎(logx-producer) → 框架适配器 → 存储适配器
- **MUST** 模块化设计：清晰的模块依赖关系，避免循环依赖
- **SHOULD** 提供All-in-One集成包：为非Maven用户提供开箱即用的Fat JAR方案（~25MB）

**理由**: 分层架构实现关注点分离和职责清晰。模块化设计降低耦合度，提升可扩展性。All-in-One包简化非Maven用户的集成复杂度。

## 治理与合规

### 修改流程
所有代码修改必须通过Pull Request，确保：
- 代码审查通过，符合编码标准文档（docs/architecture/coding-standards.md）
- 相关测试通过，覆盖率达到90%+要求
- 文档同步更新，保持README、API文档、架构文档的一致性
- 性能测试验证，核心性能指标不得降级

**审查重点**:
- 是否违反核心原则（高性能、零丢失、统一接口、多云适配、可靠性）
- 是否引入不必要的复杂性或外部依赖
- 是否影响现有功能的向后兼容性

### 版本管理
采用语义化版本控制(MAJOR.MINOR.PATCH)：
- **MAJOR**: 不兼容的API变更或架构重大调整（如移除核心接口、更改配置键结构）
- **MINOR**: 新功能添加或向后兼容的性能优化（如新增存储适配器、性能调优）
- **PATCH**: Bug修复或文档改进（如修复内存泄漏、更新配置示例）

当前版本：1.0.0-SNAPSHOT

### 合规检查
- **配置一致性**: 所有框架使用相同的配置键结构（logx.oss.storage.*、logx.oss.engine.*）
- **性能基准**: 每次发布前验证核心性能指标（吞吐量、延迟、内存占用）
- **安全审查**: 密钥管理使用环境变量、网络传输使用HTTPS、数据压缩使用GZIP
- **兼容性测试**: 确保支持Java 8+、Log4j 1.2.17+、Log4j2 2.22.1+、Logback 1.2.13+

### 宪章修订
宪章修订需要：
1. 提出修订提案，说明修改原因和影响范围
2. 更新受影响的模板文件（plan-template.md、spec-template.md、tasks-template.md）
3. 增量更新版本号（参见版本管理规则）
4. 在Sync Impact Report中记录所有变更

本宪章优先级高于所有其他开发实践文档。如有冲突，以本宪章为准。

**Version**: 1.0.0 | **Ratified**: 2025-01-16 | **Last Amended**: 2025-01-16

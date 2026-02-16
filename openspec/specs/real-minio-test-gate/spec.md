# real-minio-test-gate Specification

## Purpose
定义基于真实 MinIO 环境的集成门禁要求，确保前置检查、全量链路覆盖与通过标准被严格执行。

## Requirements
### Requirement: 集成门禁必须基于真实 MinIO 环境执行
门禁流程 MUST 在真实 MinIO 环境下执行集成验证，不得以 mock 或替代实现作为通过依据。

#### Scenario: 环境未指向真实 MinIO
- **WHEN** 验证流程检测到未连接真实 MinIO 环境
- **THEN** 系统 MUST 直接判定门禁失败
- **AND** 系统 MUST 输出环境不满足要求的原因说明

### Requirement: 执行集成验证前必须完成 MinIO 前置可用性检查
在运行全量测试前，系统 MUST 对 MinIO 连接、凭据与目标桶可用性进行前置检查；任一检查失败时 SHALL 快速失败并停止后续验证。

#### Scenario: MinIO 连接检查失败
- **WHEN** 系统执行 MinIO 前置检查时发现 endpoint 不可达或认证失败
- **THEN** 系统 MUST 立即终止后续测试执行
- **AND** 系统 MUST 输出具体失败检查项与诊断信息

### Requirement: 门禁必须覆盖项目要求的全量集成链路
门禁流程 MUST 执行项目定义的完整集成测试范围（包括兼容性测试集合及项目要求的扩展链路），不得以部分模块通过替代全量通过结论。

#### Scenario: 仅执行了部分测试模块
- **WHEN** 验证流程检测到仅执行子集测试而非全量链路
- **THEN** 系统 MUST 判定门禁失败
- **AND** 系统 MUST 标识缺失的测试范围

### Requirement: 全量验证通过才可判定门禁通过
只有在全量集成链路全部通过且无关键步骤缺失时，系统 SHALL 生成门禁通过结论；否则 MUST 给出失败结论。

#### Scenario: 全量测试执行完成但存在失败项
- **WHEN** 全量验证执行结束且任一测试失败或关键模块未执行
- **THEN** 系统 MUST 输出门禁失败结论
- **AND** 系统 MUST 提供失败模块与日志定位信息

#### Scenario: 全量测试全部通过
- **WHEN** 全量验证执行结束且所有必选测试全部通过
- **THEN** 系统 SHALL 输出门禁通过结论
- **AND** 系统 MUST 提供通过判定依据与结果摘要

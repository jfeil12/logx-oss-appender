# oss-logging-safety Specification

## Purpose
TBD - created by archiving change address-pr2-review-findings. Update Purpose after archive.
## Requirements
### Requirement: 分片上传不得阻塞消费者线程
批处理队列 SHALL 将分片上传交给有界异步执行器（可复用消费线程池，但不得在当前消费线程内阻塞），并设置有界超时（`storage.uploadTimeoutMs`，默认 30s / 30000ms），确保存储阻塞无法超过配置时限。

#### Scenario: 分片上传时后端卡顿
- **GIVEN** 批次超过分片阈值触发分片，且存储 Future 在上传超时时间（与正常上传共享的 `storage.uploadTimeoutMs`，默认 30s / 30000ms）内未完成
- **WHEN** 消费者线程调度分片上传
- **THEN** 上传必须在有界执行器中异步运行，可复用消费线程池，但不得在当前消费线程内同步等待
- **AND** 消费者线程最多等待配置的超时，然后判定批次失败
- **AND** 该批次必须交给重试/回退逻辑，而非无限自旋

### Requirement: 占位符缺失时必须快速失败
配置占位符 `${KEY}` 无默认值时 SHALL 视为必填；若系统属性与环境变量均缺失，应用 SHALL 带明确信息终止启动。

#### Scenario: 无默认值的访问密钥缺失
- **GIVEN** 配置项 `logx.oss.storage.accessKeyId=${LOGX_ACCESS_KEY}` 且变量未设置
- **WHEN** 执行 `ConfigManager.resolvePlaceholders`
- **THEN** 必须抛出配置异常指出缺失占位符
- **AND** 禁止静默替换为空字符串
- **AND** 在 `applyXmlConfig` 之后的启动自检阶段，若最终配置中该字段仍为空，系统 SHALL 记录告警并抛出配置异常终止启动

### Requirement: 丢弃路径诊断需脱敏
异步队列丢弃消息（如 `blockOnFull=false`）时，诊断日志 SHALL 去除原始载荷大小和队列内部细节，仅输出聚合计数与清洗后的事件标识，并做限频。

#### Scenario: 队列满且启用丢弃
- **WHEN** 队列无法入队载荷并决定丢弃
- **THEN** 系统必须输出一条结构化警告，包含丢弃计数与哈希批次 ID
- **AND** 日志不得包含原始载荷字节、事件内容或完整队列状态

### Requirement: 写入存储前必须清洗载荷
各框架适配器在将编码后的日志交给异步引擎前 SHALL 进行清洗，去除 `\n`/`\t` 之外的控制字符、强制最大载荷大小，并转义可能导致下游日志注入的序列。

#### Scenario: 日志事件含控制字符
- **WHEN** Logback/Log4j/Log4j2 appender 编码事件时遇到未转义的控制字符或超大内容
- **THEN** 清洗器必须移除/转义非法字符，并截断或拒绝超出配置字节上限的载荷
- **AND** 必须输出指标/日志，提示该事件已被清洗或丢弃


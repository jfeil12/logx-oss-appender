# 设计：修复 PR #2 安全审查发现的问题

## 背景
PR #2 引入了较大的异步日志引擎，审查指出三类具体风险：
1. `EnhancedDisruptorBatchingQueue` 在消费者线程同步上传分片（`future.get(30s)`），后端存储卡顿会阻塞出队，形成 DoS。
2. `ConfigManager.resolvePlaceholders` 将未解析的 `${VAR}` 静默替换为 `""`，掩盖缺失的凭据/端点。
3. 丢弃路径诊断日志输出原始消息大小和队列内部状态，各 appender 将未清洗的编码输出直接写入对象存储，缺乏大小/字符约束，存在泄露与注入风险。

## 方案
### 1. 分片上传异步化并感知背压
- 将分片上传迁移到有界执行器中异步处理，可复用消费线程池实例，但不得在当前消费线程内同步等待。
- 每个上传返回 `CompletableFuture`，用 `CompletableFuture.allOf` 聚合并设置可配置超时（`storage.uploadTimeoutMs`，默认 30s / 30000ms，与正常上传共享），消费线程最多等待该上限。
- 超时或批量失败时取消在途上传，记录错误指标，并将批次交给重试/回退队列，而非自旋阻塞。
- 暴露队列健康指标（丢弃批次、分片重试）以便观察。

### 2. 严格占位符校验
- `${VAR}` 无默认值时视为必填；若 JVM 属性与环境变量均缺失，抛出 `MissingPlaceholderException` 终止启动。
- 保持 `${VAR:-default}` / `${VAR:default}` 语义，但禁止静默生成空字符串。
- 在 `applyXmlConfig` 之后执行集中校验：若最终配置中的必填字段（endpoint/accessKeyId/accessKeySecret/bucket 等）为空，记录警告并抛出配置异常，让问题在启动阶段暴露。
- 启动时输出清晰日志，指明缺失键及设置方式。

### 3. 安全诊断与载荷清洗
- 将 `logger.error("[DATA_LOSS_ALERT] ... payload length ... queue status ...")` 替换为结构化指标日志，移除载荷长度与队列内部细节，仅保留哈希/ID 与计数器，并做限频。
- 引入共享的 `LogPayloadSanitizer`：
  - 入队前强制最大载荷长度（超限截断或丢弃并打点）；
  - 去除除 `\n`/`\t` 外的控制字符；
  - 转义可导致下游日志注入的换行/控制序列。
- 为各框架添加对应测试，验证清洗后的输出与安全的丢弃日志。

上述措施聚焦审查问题，避免影响无关行为。

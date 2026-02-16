---
name: logx-minio-verify
description: 代码编写完成后执行全量集成验证（严格全量：MinIO + compatibility-tests + jdk21-test）
license: MIT
compatibility: Requires Java + Maven + Docker (or local MinIO).
metadata:
  author: logx-project
  version: "1.0"
---

## 目的

统一“编码完成后的验证门禁”。

**硬规则**：只要涉及代码改动，完成实现后必须执行本技能；未通过则视为变更未完成。

## 输入

- `quick`：快速验证（MinIO 健康 + `MinIOIntegrationTest`）
- `full`：严格全量验证（默认，必须用于最终验收）

## 执行

```bash
bash scripts/integration-verify.sh <quick|full>
```

建议：
- 开发中可先跑 `quick`
- 提交前/合并前必须跑 `full`

## 通过标准

- `quick`：MinIO 可用 + `MinIOIntegrationTest` 通过
- `full`：
  - MinIO 前置检查通过（endpoint、凭据、桶）
  - `compatibility-tests/test-runner` 通过
  - `jdk21-test` 覆盖并通过

任一失败即 FAIL。

## 输出产物

脚本会写入：

`compatibility-tests/target/integration-verify/summary-<timestamp>.md`

并同时生成详细日志：

`compatibility-tests/target/integration-verify/run-<timestamp>.log`

## 失败处置

优先查看失败模板中的：
1. 失败阶段
2. 失败命令
3. 建议动作

然后根据 summary/report 路径回溯日志定位。

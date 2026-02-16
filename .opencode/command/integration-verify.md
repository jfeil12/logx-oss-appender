---
description: 一键验证全量集成测试链路（严格全量，含 MinIO 与 jdk21-test）
---

执行仓库的全量集成验证（推荐入口）。

> 与 `/minio-verify` 等价，作为语义化主入口。

## 输入

`/integration-verify [quick|full]`

- 默认 `full`
- `quick`：MinIO 可用性 + `MinIOIntegrationTest`
- `full`：在 `quick` 基础上追加 `compatibility-tests/test-runner` 与 `jdk21-test`

## 执行命令

```bash
bash scripts/integration-verify.sh <mode>
```

## 成功判定

- `quick`：MinIO 可用且 `MinIOIntegrationTest` 通过
- `full`：MinIO 可用且 `test-runner`、`jdk21-test` 全通过
- 任一必选步骤失败或缺失：FAIL

## 输出要求

脚本会输出标准化摘要并写入：

`compatibility-tests/target/integration-verify/summary-<timestamp>.md`

---
description: 一键验证全量集成测试链路（严格全量，含 MinIO 与 jdk21-test）
---

执行仓库的全量集成验证。

> 历史兼容入口：`/minio-verify`
> 推荐入口：`/integration-verify`

## 输入

`/minio-verify [quick|full]`

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

摘要必须包含：
- 模式（quick/full）
- 云环境识别结果
- 9001 端口转发地址（若可推断）
- MinIO 来源（已运行/自动拉起）
- MinIOIntegrationTest / test-runner / jdk21-test 结果
- bucket 校验结果
- 总结论 PASS/FAIL

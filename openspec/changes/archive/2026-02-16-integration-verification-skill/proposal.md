## Why

当前研发流程中，“代码编写完成”缺少统一、可执行、可追溯的集成验证门禁，容易出现未完成全量验证就进入交付的情况。项目虽然已有 compatibility-tests 与 MinIO 真实环境测试能力，但尚未被技能与流程强制绑定，执行结果不稳定，因此需要将其固化为标准完成条件。

## What Changes

- 新增“编码完成后触发验证”的统一技能化门禁规则，明确触发时机、执行入口和失败即阻断机制。
- 将“真实 MinIO 环境下的完整集成/兼容性验证（包含项目要求的全量链路）”纳入必过验收条件。
- 统一本地与 CI 的验证执行约定（前置环境检查、测试命令、结果判定与输出），确保可复现。
- 明确变更完成定义：未通过规定的集成验证，不可视为变更完成。

## Capabilities

### New Capabilities

- `post-code-integration-verification`: 定义代码完成后的统一集成验证门禁能力，包括触发条件、执行步骤、通过/失败判定和结果产物。
- `real-minio-test-gate`: 定义基于真实 MinIO 环境的集成验证要求，覆盖环境可用性检查、全量测试执行与通过标准。

### Modified Capabilities

- 无（当前不修改 `openspec/specs/` 下已有 capability 的既有要求）。

## Impact

- 影响 OpenSpec 变更与实施流程：后续 specs/design/tasks 将把“验证门禁”作为实施与验收主线。
- 预期影响仓库内技能与流程文档（如 `.opencode/skills/`、`AGENTS.md`、测试说明文档等）以反映新规则。
- 影响测试执行规范：需要真实 MinIO 测试环境与完整集成链路作为交付前置条件。
- 不引入对外 API 破坏性变更。

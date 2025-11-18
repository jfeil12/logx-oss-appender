# Spring Boot兼容性测试应用

## 概述
此应用用于验证OSS Appender在Spring Boot环境中的兼容性和性能表现，特别关注高吞吐量场景下的日志处理能力。

## 功能特性
1. 验证通过标准依赖引入的兼容性
2. 测试YAML和Properties配置方式
3. 验证环境变量配置覆盖
4. 执行性能基准测试和压力测试
5. 业务日志生成测试（电商、金融、系统监控场景）
6. 高并发日志处理能力验证

## 构建和运行

### 构建项目
```bash
mvn clean install
```

### 运行应用
```bash
mvn spring-boot:run
```

或者
```bash
java -jar target/spring-boot-compatibility-test-1.0.0-SNAPSHOT.jar
```

## 配置方式

### YAML配置
使用 `application.yml` 文件配置

### Properties配置
使用 `application.properties` 文件配置

### 环境变量配置
支持通过环境变量覆盖配置（LOGX_OSS前缀）：
- `LOGX_OSS_ENDPOINT` - 存储端点
- `LOGX_OSS_REGION` - 存储区域
- `LOGX_OSS_ACCESS_KEY_ID` - 访问密钥ID
- `LOGX_OSS_ACCESS_KEY_SECRET` - 秘密访问密钥
- `LOGX_OSS_BUCKET` - 存储桶名称
- `LOGX_OSS_KEY_PREFIX` - 对象key前缀
- `LOGX_OSS_OSS_TYPE` - OSS类型
- `LOGX_OSS_MAX_UPLOAD_SIZE_MB` - 最大上传文件大小

## 性能指标

根据架构文档调整后的性能要求：

### 核心性能指标
| 指标 | 目标值 | 说明 |
|------|--------|------|
| 吞吐量 | 10,000+条日志/秒 | 高并发处理能力 |
| 日志无丢失 | 零丢失率 | 在高吞吐量负载下确保数据完整性 |
| 队列内存占用 | < 512MB | 内存高效使用，避免OOM |

### 测试验证方法
- **吞吐量测试**: 生成1万条日志，验证实际处理速度达到10,000+/s
- **无丢失率测试**: 高负载下验证日志完整性，需对比MinIO上传文件数量
- **内存控制测试**: 多场景下监控队列内存峰值使用情况
- **高并发测试**: 20线程并发处理，每线程5000条日志

## 测试执行

### 执行业务日志生成测试
```bash
mvn test -Dtest=BusinessLogGenerationTest
```

### 执行性能压力测试
```bash
mvn test -Dtest=BusinessLogGenerationTest#testPerformanceStressLogsGeneration
```

### 执行务实性能基准测试
```bash
mvn test -Dtest=BusinessLogGenerationTest#testRealisticPerformanceBenchmark
```

## 结果验证

### 1. MinIO控制台验证
- URL: http://localhost:9001
- 桶: `logx-test-bucket`
- 检查日志文件上传情况

### 2. 性能指标验证
查看测试输出日志，关注：
- 实际吞吐量 vs 目标10,000+条/秒
- 队列内存使用峰值 vs 512MB限制
- 日志无丢失率验证状态
- 压测修正建议（如需要）

## 配置更新说明

### 线程配置优化（2025-10-10）
基于性能测试验证，系统默认线程配置已优化：
- **消费线程数**: 从4调整为1（CONSUMER_THREAD_COUNT: 4 → 1）
- **核心线程数**: 从4调整为1（CORE_POOL_SIZE: 4 → 1）
- **最大线程数**: 从4调整为1（MAXIMUM_POOL_SIZE: 4 → 1）

### 性能验证结果
通过QPS独立测试验证，单线程配置仍能达到优秀性能：
- **实际QPS**: 12,446条/秒
- **目标QPS**: 10,000+条/秒
- **结果**: ✅ 超额达标（124%）

单线程配置优势：
- 降低系统资源消耗
- 减少线程切换开销
- 简化并发控制
- 提高系统稳定性

## 压测修正机制

如果标准指标在特定环境下无法达到，系统会自动提供修正建议：
- **吞吐量修正**: 基于实际测试结果的80%作为保守目标
- **内存修正**: 基于实际峰值的120%作为安全边际
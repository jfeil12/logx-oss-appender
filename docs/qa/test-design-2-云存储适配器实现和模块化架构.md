# 云存储适配器实现和模块化架构测试设计

## 1. 概述

本文档为故事点2"云存储适配器实现和模块化架构"提供全面的测试设计，覆盖核心抽象层、存储适配器层和框架适配器层的测试场景。测试设计包括功能测试、集成测试、性能测试、兼容性测试和异常处理测试等方面，并使用Given-When-Then格式描述测试场景。

## 2. 测试范围和层次

### 2.1 测试层次
- **核心抽象层**: StorageService接口、StorageServiceFactory、StorageConfig等
- **存储适配器层**: S3StorageAdapter、SfOssStorageAdapter及相关服务类
- **框架适配器层**: Log4j、Log4j2、Logback适配器与存储服务的集成

### 2.2 测试类型
- **功能测试**: 验证各层功能实现是否符合需求
- **集成测试**: 验证各模块间的集成和交互
- **性能测试**: 验证系统在高负载下的性能表现
- **兼容性测试**: 验证不同云存储服务的兼容性
- **异常处理测试**: 验证系统在异常情况下的处理能力

## 3. 核心抽象层测试场景

### 3.1 StorageService接口测试

#### 场景1: StorageService接口方法定义
**Given**: 已定义StorageService接口
**When**: 检查接口方法
**Then**: 接口应包含putObject、getBackendType、getBucketName、close和supportsBackend方法

#### 场景2: StorageService继承StorageInterface
**Given**: 已定义StorageService接口
**When**: 检查接口继承关系
**Then**: StorageService应继承StorageInterface

### 3.2 StorageConfig配置类测试

#### 场景1: StorageConfig创建和配置
**Given**: 提供必要的配置参数
**When**: 创建StorageConfig实例
**Then**: 应成功创建配置对象，且所有必需字段正确设置

#### 场景2: StorageConfig验证
**Given**: 创建StorageConfig实例
**When**: 调用validateConfig方法
**Then**: 当所有必需字段有效时应不抛出异常，当字段无效时应抛出IllegalArgumentException

#### 场景3: StorageConfig自动检测后端类型
**Given**: 提供不同endpoint的StorageConfig
**When**: 调用detectBackendType方法
**Then**: 应根据endpoint正确识别后端类型(S3或SF_OSS)

#### 场景4: StorageConfig Builder模式
**Given**: 使用Builder模式构建StorageConfig
**When**: 链式调用各配置方法
**Then**: 应能正确构建配置对象，且各字段值正确

### 3.3 StorageServiceFactory工厂类测试

#### 场景1: 根据配置创建S3存储服务
**Given**: 提供S3后端类型的StorageConfig配置
**When**: 调用createStorageService方法
**Then**: 应返回S3StorageService实例

#### 场景2: 根据配置创建SF OSS存储服务
**Given**: 提供SF_OSS后端类型的StorageConfig配置
**When**: 调用createStorageService方法
**Then**: 应返回SfOssStorageService实例

#### 场景3: 未找到合适存储服务
**Given**: 提供不支持的后端类型的StorageConfig配置
**When**: 调用createStorageService方法
**Then**: 应抛出IllegalStateException异常

#### 场景4: 根据后端类型和配置创建存储服务
**Given**: 提供后端类型和StorageConfig配置
**When**: 调用重载的createStorageService方法
**Then**: 应返回对应后端类型的存储服务实例

## 4. 存储适配器层测试场景

### 4.1 S3存储适配器测试

#### 场景1: S3StorageAdapter创建
**Given**: 提供有效的S3配置参数
**When**: 创建S3StorageAdapter实例
**Then**: 应成功创建适配器实例，且配置正确应用

#### 场景2: S3标准上传
**Given**: S3StorageAdapter已创建，且有小于5MB的数据
**When**: 调用putObject方法上传数据
**Then**: 应成功上传数据，且使用标准上传方式

#### 场景3: S3分片上传
**Given**: S3StorageAdapter已创建，且有大于5MB的数据
**When**: 调用putObject方法上传数据
**Then**: 应成功上传数据，且使用分片上传方式

#### 场景4: S3上传参数验证
**Given**: S3StorageAdapter已创建
**When**: 调用putObject方法，传入null或空的key或data参数
**Then**: 应抛出IllegalArgumentException异常

#### 场景5: S3适配器关闭
**Given**: S3StorageAdapter已创建
**When**: 调用close方法
**Then**: 应正确释放资源，且不抛出异常

#### 场景6: S3适配器元数据获取
**Given**: S3StorageAdapter已创建
**When**: 调用getBackendType和getBucketName方法
**Then**: 应返回正确的后端类型"S3"和存储桶名称

#### 场景7: S3重试机制
**Given**: S3StorageAdapter已创建，且模拟可重试的AWS异常
**When**: 调用putObject方法上传数据
**Then**: 应按配置进行重试，且在达到最大重试次数后抛出异常

#### 场景8: S3不可重试异常
**Given**: S3StorageAdapter已创建，且模拟不可重试的AWS异常
**When**: 调用putObject方法上传数据
**Then**: 应立即抛出异常，不进行重试

### 4.2 SF OSS存储适配器测试

#### 场景1: SfOssStorageAdapter创建
**Given**: 提供有效的SF OSS配置参数
**When**: 创建SfOssStorageAdapter实例
**Then**: 应成功创建适配器实例，且配置正确应用

#### 场景2: SF OSS上传
**Given**: SfOssStorageAdapter已创建，且有任意大小的数据
**When**: 调用putObject方法上传数据
**Then**: 应成功上传数据

#### 场景3: SF OSS上传参数验证
**Given**: SfOssStorageAdapter已创建
**When**: 调用putObject方法，传入null或空的key或data参数
**Then**: 应抛出IllegalArgumentException异常

#### 场景4: SF OSS适配器关闭
**Given**: SfOssStorageAdapter已创建
**When**: 调用close方法
**Then**: 应正确释放资源，且不抛出异常

#### 场景5: SF OSS适配器元数据获取
**Given**: SfOssStorageAdapter已创建
**When**: 调用getBackendType和getBucketName方法
**Then**: 应返回正确的后端类型"SF_OSS"和存储桶名称

#### 场景6: SF OSS重试机制
**Given**: SfOssStorageAdapter已创建，且模拟可重试的SF OSS异常
**When**: 调用putObject方法上传数据
**Then**: 应按配置进行重试，且在达到最大重试次数后抛出异常

#### 场景7: SF OSS不可重试异常
**Given**: SfOssStorageAdapter已创建，且模拟不可重试的SF OSS异常
**When**: 调用putObject方法上传数据
**Then**: 应立即抛出异常，不进行重试

### 4.3 存储服务类测试

#### 场景1: S3StorageService创建
**Given**: 提供有效的S3配置参数
**When**: 创建S3StorageService实例
**Then**: 应成功创建服务实例，且内部S3StorageAdapter正确初始化

#### 场景2: S3StorageService后端支持检查
**Given**: S3StorageService实例已创建
**When**: 调用supportsBackend方法，传入不同后端类型
**Then**: 对于S3兼容的后端类型应返回true，其他类型返回false

#### 场景3: SfOssStorageService创建
**Given**: 提供有效的SF OSS配置参数
**When**: 创建SfOssStorageService实例
**Then**: 应成功创建服务实例，且内部SfOssStorageAdapter正确初始化

#### 场景4: SfOssStorageService后端支持检查
**Given**: SfOssStorageService实例已创建
**When**: 调用supportsBackend方法，传入不同后端类型
**Then**: 对于SF_OSS后端类型应返回true，其他类型返回false

## 5. 框架适配器层测试场景

### 5.1 Log4j适配器集成测试

#### 场景1: Log4j适配器配置S3后端
**Given**: Log4j配置中指定S3后端类型和相关参数
**When**: 初始化Log4j适配器
**Then**: 应成功创建S3StorageService并正确配置

#### 场景2: Log4j适配器配置SF OSS后端
**Given**: Log4j配置中指定SF OSS后端类型和相关参数
**When**: 初始化Log4j适配器
**Then**: 应成功创建SfOssStorageService并正确配置

#### 场景3: Log4j适配器日志上传
**Given**: Log4j适配器已正确配置并初始化
**When**: 记录日志消息
**Then**: 日志应通过相应的存储服务成功上传到云存储

#### 场景4: Log4j适配器后端类型自动检测
**Given**: Log4j配置中未指定后端类型，但提供了endpoint
**When**: 初始化Log4j适配器
**Then**: 应根据endpoint自动检测并配置相应的存储服务

### 5.2 Log4j2适配器集成测试

#### 场景1: Log4j2适配器配置S3后端
**Given**: Log4j2配置中指定S3后端类型和相关参数
**When**: 初始化Log4j2适配器
**Then**: 应成功创建S3StorageService并正确配置

#### 场景2: Log4j2适配器配置SF OSS后端
**Given**: Log4j2配置中指定SF OSS后端类型和相关参数
**When**: 初始化Log4j2适配器
**Then**: 应成功创建SfOssStorageService并正确配置

#### 场景3: Log4j2适配器日志上传
**Given**: Log4j2适配器已正确配置并初始化
**When**: 记录日志消息
**Then**: 日志应通过相应的存储服务成功上传到云存储

#### 场景4: Log4j2适配器后端类型自动检测
**Given**: Log4j2配置中未指定后端类型，但提供了endpoint
**When**: 初始化Log4j2适配器
**Then**: 应根据endpoint自动检测并配置相应的存储服务

### 5.3 Logback适配器集成测试

#### 场景1: Logback适配器配置S3后端
**Given**: Logback配置中指定S3后端类型和相关参数
**When**: 初始化Logback适配器
**Then**: 应成功创建S3StorageService并正确配置

#### 场景2: Logback适配器配置SF OSS后端
**Given**: Logback配置中指定SF OSS后端类型和相关参数
**When**: 初始化Logback适配器
**Then**: 应成功创建SfOssStorageService并正确配置

#### 场景3: Logback适配器日志上传
**Given**: Logback适配器已正确配置并初始化
**When**: 记录日志消息
**Then**: 日志应通过相应的存储服务成功上传到云存储

#### 场景4: Logback适配器后端类型自动检测
**Given**: Logback配置中未指定后端类型，但提供了endpoint
**When**: 初始化Logback适配器
**Then**: 应根据endpoint自动检测并配置相应的存储服务

## 6. 集成测试场景

### 6.1 存储服务工厂与适配器集成

#### 场景1: Java SPI机制加载存储服务
**Given**: classpath中包含S3和SF OSS适配器
**When**: StorageServiceFactory创建存储服务
**Then**: 应通过Java SPI机制正确加载并返回相应的存储服务实例

#### 场景2: 多种存储服务共存
**Given**: classpath中同时包含S3和SF OSS适配器
**When**: 根据不同配置创建存储服务
**Then**: 应能正确区分并返回对应的存储服务实例

### 6.2 框架适配器与存储服务集成

#### 场景1: 完整日志处理流程
**Given**: 框架适配器已配置并连接到云存储服务
**When**: 应用程序记录日志消息
**Then**: 日志应经过框架适配器、Disruptor队列、批处理引擎，最终上传到云存储

#### 场景2: 多框架适配器并存
**Given**: 同一应用中同时使用Log4j、Log4j2和Logback适配器
**When**: 各框架分别记录日志消息
**Then**: 各框架的日志应能通过各自的配置正确上传到指定的云存储

## 7. 性能测试场景

### 7.1 存储适配器性能测试

#### 场景1: S3标准上传性能
**Given**: S3StorageAdapter已配置，有小于5MB的测试数据
**When**: 并发执行多次putObject操作
**Then**: 应满足性能指标要求(如平均响应时间<100ms，吞吐量>1000 ops/sec)

#### 场景2: S3分片上传性能
**Given**: S3StorageAdapter已配置，有大于5MB的测试数据
**When**: 并发执行多次putObject操作
**Then**: 应满足性能指标要求(如平均响应时间<500ms，吞吐量>100 ops/sec)

#### 场景3: SF OSS上传性能
**Given**: SfOssStorageAdapter已配置，有任意大小的测试数据
**When**: 并发执行多次putObject操作
**Then**: 应满足性能指标要求(如平均响应时间<100ms，吞吐量>1000 ops/sec)

### 7.2 框架适配器性能测试

#### 场景1: Log4j适配器端到端性能
**Given**: Log4j适配器已配置并连接到云存储服务
**When**: 并发执行大量日志记录操作
**Then**: 应满足性能指标要求(如平均延迟<1ms，吞吐量>1万条/秒)

#### 场景2: Log4j2适配器端到端性能
**Given**: Log4j2适配器已配置并连接到云存储服务
**When**: 并发执行大量日志记录操作
**Then**: 应满足性能指标要求(如平均延迟<1ms，吞吐量>1万条/秒)

#### 场景3: Logback适配器端到端性能
**Given**: Logback适配器已配置并连接到云存储服务
**When**: 并发执行大量日志记录操作
**Then**: 应满足性能指标要求(如平均延迟<1ms，吞吐量>1万条/秒)

## 8. 兼容性测试场景

### 8.1 S3兼容存储服务测试

#### 场景1: AWS S3兼容性
**Given**: 配置指向AWS S3服务
**When**: 使用S3StorageAdapter上传数据
**Then**: 应能成功上传数据到AWS S3

#### 场景2: 阿里云OSS兼容性
**Given**: 配置指向阿里云OSS服务
**When**: 使用S3StorageAdapter上传数据
**Then**: 应能成功上传数据到阿里云OSS

#### 场景3: 腾讯云COS兼容性
**Given**: 配置指向腾讯云COS服务
**When**: 使用S3StorageAdapter上传数据
**Then**: 应能成功上传数据到腾讯云COS

#### 场景4: MinIO兼容性
**Given**: 配置指向MinIO服务
**When**: 使用S3StorageAdapter上传数据
**Then**: 应能成功上传数据到MinIO

### 8.2 不同框架版本兼容性

#### 场景1: Log4j 1.x兼容性
**Given**: 使用Log4j 1.x版本
**When**: 配置并使用Log4j适配器
**Then**: 应能正常工作并上传日志

#### 场景2: Log4j2兼容性
**Given**: 使用Log4j2版本
**When**: 配置并使用Log4j2适配器
**Then**: 应能正常工作并上传日志

#### 场景3: Logback兼容性
**Given**: 使用Logback版本
**When**: 配置并使用Logback适配器
**Then**: 应能正常工作并上传日志

## 9. 异常处理测试场景

### 9.1 存储适配器异常处理

#### 场景1: S3网络异常处理
**Given**: S3StorageAdapter已配置，但网络连接中断
**When**: 调用putObject方法上传数据
**Then**: 应按重试策略进行重试，最终抛出包含详细错误信息的异常

#### 场景2: S3认证异常处理
**Given**: S3StorageAdapter已配置，但访问密钥错误
**When**: 调用putObject方法上传数据
**Then**: 应立即抛出认证异常，不进行重试

#### 场景3: S3存储桶不存在异常处理
**Given**: S3StorageAdapter已配置，但指定的存储桶不存在
**When**: 调用putObject方法上传数据
**Then**: 应立即抛出存储桶不存在异常，不进行重试

#### 场景4: SF OSS网络异常处理
**Given**: SfOssStorageAdapter已配置，但网络连接中断
**When**: 调用putObject方法上传数据
**Then**: 应按重试策略进行重试，最终抛出包含详细错误信息的异常

#### 场景5: SF OSS认证异常处理
**Given**: SfOssStorageAdapter已配置，但访问密钥错误
**When**: 调用putObject方法上传数据
**Then**: 应立即抛出认证异常，不进行重试

### 9.2 框架适配器异常处理

#### 场景1: 框架适配器配置异常处理
**Given**: 框架适配器配置缺失必需参数
**When**: 初始化框架适配器
**Then**: 应抛出配置异常，并提供清晰的错误信息

#### 场景2: 框架适配器运行时异常处理
**Given**: 框架适配器运行时发生存储服务异常
**When**: 记录日志消息
**Then**: 应记录详细的错误日志，但不影响应用程序正常运行

## 10. 安全测试场景

### 10.1 配置信息安全

#### 场景1: 敏感信息掩码
**Given**: StorageConfig包含访问密钥等敏感信息
**When**: 调用toString方法输出配置信息
**Then**: 敏感信息应被掩码显示，如accessKeyId显示为"te****id"

### 10.2 访问控制测试

#### 场景1: 权限不足访问
**Given**: 配置的访问密钥权限不足
**When**: 尝试上传对象到存储桶
**Then**: 应抛出权限不足异常，并记录详细错误信息
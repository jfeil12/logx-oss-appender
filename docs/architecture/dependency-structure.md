# 组件依赖关系

## 正确的依赖结构

```
logx-producer (核心)
    ↓
log4j-oss-appender
log4j2-oss-appender
logback-oss-appender
```

三个适配器都直接依赖于核心模块，彼此之间没有依赖关系。
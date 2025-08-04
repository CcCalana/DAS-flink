# DAS-Flink STA/LTA 事件检测系统

## 概述

本系统在原有的DAS-Flink降噪框架基础上，集成了STA/LTA（Short-Term Average / Long-Term Average）事件检测算法，提供了完整的地震事件实时检测流程。

## 系统架构

### 核心组件

```
Kafka数据源 → 数据预处理 → 多算法并行处理 → 事件检测 → 结果输出
     ↓              ↓              ↓           ↓         ↓
  JSON解析    →   数据验证    →   降噪算法   →  STA/LTA  → 告警/统计
```

### 新增组件说明

1. **STALTADetector** (`algorithm/STALTADetector.java`)
   - 实现经典的STA/LTA事件检测算法
   - 支持自适应参数调整
   - 提供递归实现以提高计算效率

2. **EventDetectionResult** (`model/EventDetectionResult.java`)
   - 扩展的结果模型，包含降噪和事件检测信息
   - 集成信号质量评估
   - 提供事件统计和摘要功能

3. **EventDetectionProcessor** (`process/EventDetectionProcessor.java`)
   - 集成降噪和事件检测的完整处理器
   - 支持多种降噪算法组合
   - 实时性能监控和统计

4. **EventDetectionJob** (`EventDetectionJob.java`)
   - 新的主作业类，专门用于事件检测
   - 支持多算法并行处理
   - 提供实时告警和统计聚合

## STA/LTA算法原理

### 数学基础

1. **短期平均（STA）**：
   ```
   STA(n) = (1/N_s) * Σ(k=n-N_s+1 to n) |x(k)|²
   ```
   - N_s：短期窗口长度（通常1-5秒）
   - 反映信号的瞬时能量变化

2. **长期平均（LTA）**：
   ```
   LTA(n) = (1/N_l) * Σ(k=n-N_l+1 to n) |x(k)|²
   ```
   - N_l：长期窗口长度（通常10-60秒）
   - 反映背景噪声水平

3. **检测比值**：
   ```
   R(n) = STA(n) / LTA(n)
   ```
   - 当R(n) > 阈值时，检测到事件

### 递归实现优化

为提高计算效率，采用递归形式：
```
STA(n) = α_s * |x(n)|² + (1-α_s) * STA(n-1)
LTA(n) = α_l * |x(n)|² + (1-α_l) * LTA(n-1)
```
其中：α_s = 1/N_s, α_l = 1/N_l

## 使用方法

### 1. 编译项目

```bash
mvn clean package -DskipTests
```

### 2. 运行事件检测作业

```bash
# 基本运行
$FLINK_HOME/bin/flink run \
  -c com.zjujzl.das.EventDetectionJob \
  target/DAS-flink-1.0-SNAPSHOT.jar \
  --bootstrap "localhost:9092" \
  --topic "das-stream" \
  --group "event_detection_group"

# 带参数配置
$FLINK_HOME/bin/flink run \
  -c com.zjujzl.das.EventDetectionJob \
  target/DAS-flink-1.0-SNAPSHOT.jar \
  --bootstrap "localhost:9092" \
  --topic "das-stream" \
  --group "event_detection_group" \
  --high-quality-filter true \
  --event-aggregation true \
  --aggregation-window 60 \
  --quality-threshold 0.7
```

### 3. 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `bootstrap` | 必需 | Kafka集群地址 |
| `topic` | 必需 | Kafka主题名称 |
| `group` | `flink_event_detection_group` | 消费者组ID |
| `high-quality-filter` | `true` | 是否启用高质量事件过滤 |
| `event-aggregation` | `true` | 是否启用事件聚合统计 |
| `aggregation-window` | `60` | 聚合窗口大小（秒） |
| `quality-threshold` | `0.6` | 信号质量阈值 |

## 算法配置

### STA/LTA参数

可以通过修改`EventDetectionProcessor`构造函数来调整参数：

```java
// 自定义参数示例
new EventDetectionProcessor(
    "A",           // 算法类型
    false,          // 不使用自适应检测
    1.5,            // STA窗口：1.5秒
    20.0,           // LTA窗口：20秒
    3.5,            // 触发阈值：3.5
    2.0,            // 结束阈值：2.0
    0.8             // 最小事件长度：0.8秒
)
```

### 降噪算法组合

系统支持四种预定义的算法组合：

- **算法A**：空间平均 → 移动微分 → 频域去噪
- **算法B**：小波去噪 → 空间平均 → 频域去噪
- **算法C**：EMD分解 → 主成分重构 → SVD滤波 → 频域去噪
- **算法D**：SVD滤波 → 小波去噪 → 空间平均

## 输出格式

### 事件告警输出

```
[ALERT] Station: STA001 | Algorithm: A | Events: 3 | MaxRatio: 5.67 | Quality: 0.85 | Time: 30.000s | MostSignificant: [12.34s-15.67s, Ratio=5.67]
```

### 统计聚合输出

```
[STATS] Station: STA001 | Window: [2024-01-01 10:00:00, 2024-01-01 10:01:00) | Records: 25 | TotalEvents: 8 | MaxRatio: 6.23 | AvgQuality: 0.78 | HighQuality: 15 | AvgTime: 145.67ms
```

### 监控信息输出

```
[MONITOR] Station: STA001 | Processed: 100 | Algorithm: A | LastResult: Station: STA001 | Events: 2 | MaxRatio: 4.12 | Quality: 0.82 | Time: 156ms
```

## 性能优化

### 1. 并行度配置

```bash
# 设置并行度
$FLINK_HOME/bin/flink run -p 4 ...
```

### 2. 内存配置

```bash
# 增加TaskManager内存
export FLINK_TM_HEAP=2g
```

### 3. Kafka配置优化

```properties
# 增加批处理大小
batch.size=32768
linger.ms=10
compression.type=lz4
```

## 监控和调试

### 1. Flink Web UI

访问 `http://localhost:8081` 查看作业运行状态、吞吐量和延迟指标。

### 2. 日志配置

在`log4j.properties`中添加：

```properties
logger.das.name = com.zjujzl.das
logger.das.level = INFO
logger.das.appenderRef.console.ref = ConsoleAppender
```

### 3. 关键指标

- **处理延迟**：每条记录的处理时间
- **事件检测率**：单位时间内检测到的事件数量
- **信号质量分布**：不同质量等级的信号比例
- **算法性能对比**：不同算法的处理时间和检测效果

## 故障排除

### 常见问题

1. **内存不足**
   - 增加TaskManager堆内存
   - 调整并行度
   - 优化算法参数

2. **处理延迟过高**
   - 检查Kafka消费延迟
   - 优化算法实现
   - 增加并行度

3. **事件检测效果不佳**
   - 调整STA/LTA参数
   - 检查信号质量
   - 尝试不同的降噪算法组合

### 调试技巧

1. **启用详细日志**：
   ```java
   System.setProperty("flink.log.level", "DEBUG");
   ```

2. **添加性能监控**：
   ```java
   // 在处理函数中添加计时器
   long start = System.currentTimeMillis();
   // ... 处理逻辑
   long duration = System.currentTimeMillis() - start;
   ```

3. **数据采样分析**：
   ```java
   // 定期输出中间结果
   if (recordCount % 1000 == 0) {
       System.out.println("Sample data: " + Arrays.toString(signal));
   }
   ```

## 扩展开发

### 添加新的检测算法

1. 在`algorithm`包中实现新算法
2. 在`EventDetectionProcessor`中添加新的处理分支
3. 更新`EventDetectionJob`以支持新算法

### 自定义事件过滤器

```java
public class CustomEventFilter implements FilterFunction<EventDetectionResult> {
    @Override
    public boolean filter(EventDetectionResult result) throws Exception {
        // 自定义过滤逻辑
        return result.maxRatio > customThreshold;
    }
}
```

### 集成外部告警系统

```java
public class ExternalAlertSink extends RichSinkFunction<EventDetectionResult> {
    @Override
    public void invoke(EventDetectionResult result, Context context) throws Exception {
        // 发送到外部系统（如邮件、短信、Webhook等）
    }
}
```

## 版本历史

- **v1.0**: 基础降噪框架
- **v1.1**: 集成STA/LTA事件检测
- **v1.2**: 添加自适应参数调整
- **v1.3**: 优化性能和内存使用

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 创建Pull Request

## 许可证

本项目采用MIT许可证，详见LICENSE文件。
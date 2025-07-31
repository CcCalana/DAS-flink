# DAS-Flink 部署和使用指南

## 目录
- [系统要求](#系统要求)
- [环境准备](#环境准备)
- [安装部署](#安装部署)
- [配置说明](#配置说明)
- [运行指南](#运行指南)
- [监控和调试](#监控和调试)
- [性能调优](#性能调优)
- [故障排除](#故障排除)
- [生产环境部署](#生产环境部署)

## 系统要求

### 硬件要求
- **CPU**: 最少4核，推荐8核或更多
- **内存**: 最少8GB，推荐16GB或更多
- **存储**: 最少50GB可用空间
- **网络**: 千兆以太网

### 软件要求
- **操作系统**: 
  - Linux: Ubuntu 18.04+, CentOS 7+, RHEL 7+
  - Windows: Windows 10, Windows Server 2016+
  - macOS: 10.14+
- **Java**: OpenJDK 8 或 Oracle JDK 8+
- **Apache Flink**: 1.14.0 或更高版本
- **Apache Kafka**: 2.8.0 或更高版本
- **Maven**: 3.6.0 或更高版本

## 环境准备

### 1. Java 环境配置

```bash
# 检查Java版本
java -version

# 设置JAVA_HOME环境变量
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Flink 集群安装

```bash
# 下载Flink
wget https://archive.apache.org/dist/flink/flink-1.17.1/flink-1.17.1-bin-scala_2.12.tgz
tar -xzf flink-1.17.1-bin-scala_2.12.tgz
cd flink-1.17.1

# 设置FLINK_HOME
export FLINK_HOME=$(pwd)
export PATH=$FLINK_HOME/bin:$PATH

# 启动Flink集群
./bin/start-cluster.sh

# 验证Flink Web UI
# 访问 http://localhost:8081
```

### 3. Kafka 集群安装

```bash
# 下载Kafka
wget https://archive.apache.org/dist/kafka/2.8.2/kafka_2.13-2.8.2.tgz
tar -xzf kafka_2.13-2.8.2.tgz
cd kafka_2.13-2.8.2

# 启动Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties &

# 启动Kafka
bin/kafka-server-start.sh config/server.properties &

# 创建必要的主题
bin/kafka-topics.sh --create --topic seismic-data --bootstrap-server localhost:9092 --partitions 4 --replication-factor 1
bin/kafka-topics.sh --create --topic event-detection-results --bootstrap-server localhost:9092 --partitions 4 --replication-factor 1
bin/kafka-topics.sh --create --topic high-quality-events --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1
bin/kafka-topics.sh --create --topic detection-statistics --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1
```

## 安装部署

### 1. 获取源代码

```bash
# 克隆项目
git clone <repository-url>
cd DAS-flink
```

### 2. 编译项目

```bash
# 清理并编译
mvn clean package -DskipTests

# 或者包含测试
mvn clean package
```

### 3. 验证编译结果

```bash
# 检查生成的JAR文件
ls -la target/DAS-flink-1.0-SNAPSHOT.jar

# 运行示例测试
java -cp target/DAS-flink-1.0-SNAPSHOT.jar com.zjujzl.das.example.STALTAExample
```

## 配置说明

### 1. STA/LTA 参数配置

编辑 `src/main/resources/stalte-config.properties`：

```properties
# 基础检测参数
default.sta.length.sec=2.0
default.lta.length.sec=30.0
default.threshold.on=3.0
default.threshold.off=1.5

# 针对不同地震类型的优化参数
# P波检测
p.wave.sta.length.sec=0.5
p.wave.lta.length.sec=10.0
p.wave.threshold.on=4.0

# S波检测
s.wave.sta.length.sec=1.0
s.wave.lta.length.sec=15.0
s.wave.threshold.on=3.5
```

### 2. Flink 作业配置

```bash
# 设置并行度
export FLINK_PARALLELISM=4

# 设置检查点间隔
export CHECKPOINT_INTERVAL=30000

# 设置内存配置
export FLINK_TM_HEAP=2g
export FLINK_JM_HEAP=1g
```

### 3. Kafka 连接配置

```properties
# Kafka配置
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=seismic-data
kafka.output.topic=event-detection-results
kafka.group.id=das-event-detection
```

## 运行指南

### 1. 使用启动脚本（推荐）

```bash
# Windows
scripts\run-event-detection.bat

# Linux/macOS
chmod +x scripts/run-event-detection.sh
./scripts/run-event-detection.sh
```

### 2. 手动启动事件检测作业

```bash
flink run \
  --class com.zjujzl.das.EventDetectionJob \
  --parallelism 4 \
  --checkpointing-interval 30000 \
  target/DAS-flink-1.0-SNAPSHOT.jar \
  --kafka.bootstrap.servers localhost:9092 \
  --kafka.input.topic seismic-data \
  --kafka.output.topic event-detection-results \
  --kafka.group.id das-event-detection
```

### 3. 启动原始级联去噪作业

```bash
flink run \
  --class com.zjujzl.das.KafkaCascadeJob \
  --parallelism 4 \
  target/DAS-flink-1.0-SNAPSHOT.jar \
  --kafka.bootstrap.servers localhost:9092 \
  --kafka.input.topic seismic-data \
  --kafka.output.topic denoised-results
```

### 4. 运行示例和测试

```bash
# 运行STA/LTA示例
java -cp target/DAS-flink-1.0-SNAPSHOT.jar com.zjujzl.das.example.STALTAExample

# 运行单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=STALTADetectorTest
```

## 监控和调试

### 1. Flink Web UI 监控

访问 `http://localhost:8081` 查看：
- 作业状态和性能指标
- 任务并行度和资源使用
- 检查点状态
- 异常和错误日志

### 2. Kafka 监控

```bash
# 查看主题列表
kafka-topics.sh --list --bootstrap-server localhost:9092

# 查看主题详情
kafka-topics.sh --describe --topic seismic-data --bootstrap-server localhost:9092

# 监控消费者组
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group das-event-detection

# 查看消息
kafka-console-consumer.sh --topic event-detection-results --bootstrap-server localhost:9092 --from-beginning
```

### 3. 日志配置

编辑 `src/main/resources/logback.xml`：

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.zjujzl.das" level="INFO"/>
    <logger name="org.apache.flink" level="WARN"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### 4. 性能指标收集

```bash
# 启用JVM指标收集
export FLINK_ENV_JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

# 使用JConsole连接监控
jconsole localhost:9999
```

## 性能调优

### 1. Flink 性能调优

```bash
# 调整TaskManager内存
echo "taskmanager.memory.process.size: 4g" >> $FLINK_HOME/conf/flink-conf.yaml

# 调整网络缓冲区
echo "taskmanager.network.memory.fraction: 0.2" >> $FLINK_HOME/conf/flink-conf.yaml

# 启用增量检查点
echo "state.backend.incremental: true" >> $FLINK_HOME/conf/flink-conf.yaml
```

### 2. STA/LTA 算法调优

```properties
# 针对高频数据的优化
performance.max.batch.size=2000
performance.parallel.processing.enabled=true
performance.parallel.threads=8

# 内存使用优化
performance.memory.threshold=0.7
performance.processing.timeout.ms=3000
```

### 3. Kafka 性能调优

```properties
# server.properties
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
num.partitions=8
default.replication.factor=3
```

## 故障排除

### 常见问题和解决方案

#### 1. 内存不足错误

**问题**: `java.lang.OutOfMemoryError`

**解决方案**:
```bash
# 增加JVM堆内存
export FLINK_TM_HEAP=4g
export FLINK_JM_HEAP=2g

# 或在启动时指定
flink run -Dtaskmanager.memory.process.size=4g ...
```

#### 2. Kafka 连接失败

**问题**: `org.apache.kafka.common.errors.TimeoutException`

**解决方案**:
```bash
# 检查Kafka服务状态
sudo systemctl status kafka

# 检查网络连接
telnet localhost 9092

# 检查防火墙设置
sudo ufw status
```

#### 3. 检查点失败

**问题**: `Checkpoint expired before completing`

**解决方案**:
```yaml
# 增加检查点超时时间
execution.checkpointing.timeout: 600000
execution.checkpointing.max-concurrent-checkpoints: 1
```

#### 4. 序列化错误

**问题**: `java.io.NotSerializableException`

**解决方案**:
- 确保所有自定义类实现 `Serializable` 接口
- 使用 Flink 的 `TypeInformation` 显式指定类型

### 调试技巧

```bash
# 启用详细日志
export FLINK_LOG_LEVEL=DEBUG

# 查看Flink日志
tail -f $FLINK_HOME/log/flink-*.log

# 查看作业异常
flink list -r  # 查看运行中的作业
flink cancel <job-id>  # 取消作业
```

## 生产环境部署

### 1. 高可用配置

```yaml
# flink-conf.yaml
high-availability: zookeeper
high-availability.zookeeper.quorum: zk1:2181,zk2:2181,zk3:2181
high-availability.storageDir: hdfs://namenode:port/flink/ha/
high-availability.cluster-id: /default
```

### 2. 状态后端配置

```yaml
# 使用RocksDB状态后端
state.backend: rocksdb
state.checkpoints.dir: hdfs://namenode:port/flink/checkpoints
state.savepoints.dir: hdfs://namenode:port/flink/savepoints
```

### 3. 安全配置

```yaml
# 启用SSL
security.ssl.enabled: true
security.ssl.keystore: /path/to/keystore.jks
security.ssl.truststore: /path/to/truststore.jks
```

### 4. 监控集成

```yaml
# 集成Prometheus监控
metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
metrics.reporter.prom.port: 9249
```

### 5. 部署脚本示例

```bash
#!/bin/bash
# production-deploy.sh

set -e

# 环境变量
export FLINK_HOME=/opt/flink
export KAFKA_HOME=/opt/kafka
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk

# 停止现有作业
echo "Stopping existing jobs..."
flink list | grep "EventDetectionJob" | awk '{print $4}' | xargs -r flink cancel

# 部署新版本
echo "Deploying new version..."
cp target/DAS-flink-1.0-SNAPSHOT.jar /opt/das-flink/

# 启动作业
echo "Starting EventDetectionJob..."
flink run \
  --detached \
  --class com.zjujzl.das.EventDetectionJob \
  --parallelism 8 \
  --checkpointing-interval 30000 \
  /opt/das-flink/DAS-flink-1.0-SNAPSHOT.jar \
  --kafka.bootstrap.servers kafka1:9092,kafka2:9092,kafka3:9092 \
  --kafka.input.topic seismic-data \
  --kafka.output.topic event-detection-results \
  --kafka.group.id das-event-detection-prod

echo "Deployment completed successfully!"
```

### 6. 健康检查

```bash
#!/bin/bash
# health-check.sh

# 检查Flink集群状态
flink_status=$(curl -s http://localhost:8081/overview | jq -r '."taskmanagers"')
if [ "$flink_status" -lt 1 ]; then
    echo "ERROR: Flink cluster is not healthy"
    exit 1
fi

# 检查作业状态
job_status=$(flink list | grep "EventDetectionJob" | awk '{print $2}')
if [ "$job_status" != "RUNNING" ]; then
    echo "ERROR: EventDetectionJob is not running"
    exit 1
fi

# 检查Kafka连接
kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to Kafka"
    exit 1
fi

echo "All systems healthy"
exit 0
```

## 总结

本指南涵盖了 DAS-Flink 框架的完整部署和使用流程。在生产环境中部署时，请特别注意：

1. **资源规划**: 根据数据量和处理需求合理配置硬件资源
2. **高可用性**: 配置 Flink 和 Kafka 的高可用模式
3. **监控告警**: 建立完善的监控和告警机制
4. **备份恢复**: 定期备份检查点和配置文件
5. **安全防护**: 启用认证、授权和加密功能

如有问题，请参考 [故障排除](#故障排除) 部分或联系技术支持团队。
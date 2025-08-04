# Configuration Guide

## Overview

This guide provides comprehensive information about configuring the DAS-Flink framework for optimal performance in different environments and use cases.

## Configuration Files

### 1. STA/LTA Configuration (`stalte-config.properties`)

The main configuration file for STA/LTA event detection parameters.

**Location**: `src/main/resources/stalte-config.properties`

#### Basic Parameters

```properties
# Default STA/LTA parameters
default.sta.length.sec=2.0
default.lta.length.sec=30.0
default.threshold.on=3.0
default.threshold.off=1.5
default.min.event.length.sec=1.0

# Sampling rate (Hz)
default.sampling.rate=1000.0
```

#### Algorithm-Specific Configuration

```properties
# Algorithm A: Spatial → Moving Diff → Frequency
algorithm.A.sta.length.sec=1.5
algorithm.A.lta.length.sec=25.0
algorithm.A.threshold.on=2.8
algorithm.A.threshold.off=1.4

# Algorithm B: Wavelet → Spatial → Frequency
algorithm.B.sta.length.sec=2.5
algorithm.B.lta.length.sec=35.0
algorithm.B.threshold.on=3.2
algorithm.B.threshold.off=1.6

# Algorithm C: EMD → PCA → SVD → Frequency
algorithm.C.sta.length.sec=3.0
algorithm.C.lta.length.sec=40.0
algorithm.C.threshold.on=3.5
algorithm.C.threshold.off=1.8

# Algorithm D: SVD → Wavelet → Spatial
algorithm.D.sta.length.sec=2.0
algorithm.D.lta.length.sec=30.0
algorithm.D.threshold.on=3.0
algorithm.D.threshold.off=1.5
```

#### Seismology-Specific Configuration

```properties
# P-wave detection
p.wave.sta.length.sec=0.5
p.wave.lta.length.sec=10.0
p.wave.threshold.on=4.0
p.wave.threshold.off=2.0
p.wave.min.event.length.sec=0.3

# S-wave detection
s.wave.sta.length.sec=1.0
s.wave.lta.length.sec=15.0
s.wave.threshold.on=3.5
s.wave.threshold.off=1.8
s.wave.min.event.length.sec=0.5

# Regional earthquakes
regional.sta.length.sec=2.0
regional.lta.length.sec=30.0
regional.threshold.on=3.0
regional.threshold.off=1.5
regional.min.event.length.sec=1.0

# Teleseismic events
teleseismic.sta.length.sec=5.0
teleseismic.lta.length.sec=60.0
teleseismic.threshold.on=2.5
teleseismic.threshold.off=1.2
teleseismic.min.event.length.sec=10.0

# Local events
local.sta.length.sec=1.0
local.lta.length.sec=20.0
local.threshold.on=3.5
local.threshold.off=1.8
local.min.event.length.sec=0.5
```

#### Environmental Configuration

```properties
# Urban environment (high noise)
env.urban.sta.length.sec=3.0
env.urban.lta.length.sec=45.0
env.urban.threshold.on=4.0
env.urban.threshold.off=2.0

# Rural environment (low noise)
env.rural.sta.length.sec=2.0
env.rural.lta.length.sec=35.0
env.rural.threshold.on=3.0
env.rural.threshold.off=1.5

# Marine environment
env.marine.sta.length.sec=2.5
env.marine.lta.length.sec=40.0
env.marine.threshold.on=3.2
env.marine.threshold.off=1.6
```

#### Performance Configuration

```properties
# Performance settings
performance.max.batch.size=1000
performance.parallel.processing.enabled=true
performance.parallel.threads=4
performance.memory.optimization=true
performance.cache.enabled=true
performance.cache.size=10000
```

#### Monitoring and Debugging

```properties
# Debug settings
debug.verbose.logging=false
debug.save.detection.results=false
debug.results.save.path=/tmp/stalte-results

# Monitoring settings
monitoring.performance.enabled=true
monitoring.stats.interval.sec=60
monitoring.memory.tracking=true
```

#### Kafka Configuration

```properties
# Kafka topics
kafka.input.topic=seismic-data
kafka.output.topic=event-detection-results
kafka.high.quality.topic=high-quality-events
kafka.stats.topic=detection-statistics

# Kafka consumer settings
kafka.consumer.group.id=das-event-detection
kafka.consumer.auto.offset.reset=latest
kafka.consumer.enable.auto.commit=true
kafka.consumer.auto.commit.interval.ms=1000

# Kafka producer settings
kafka.producer.acks=1
kafka.producer.retries=3
kafka.producer.batch.size=16384
kafka.producer.linger.ms=10
kafka.producer.compression.type=lz4
```

#### Alert Configuration

```properties
# Alert thresholds
alert.high.quality.threshold=0.8
alert.event.density.threshold=10
alert.max.ratio.threshold=10.0

# Email alerts
alert.email.enabled=false
alert.email.recipients=admin@example.com
alert.email.smtp.host=smtp.example.com
alert.email.smtp.port=587
alert.email.smtp.username=alerts@example.com
alert.email.smtp.password=password

# Webhook alerts
alert.webhook.enabled=false
alert.webhook.url=https://hooks.example.com/alerts
alert.webhook.timeout.ms=5000
```

### 2. Flink Configuration

#### Job Configuration

```bash
# Environment variables
export FLINK_PARALLELISM=4
export CHECKPOINT_INTERVAL=30000
export FLINK_TM_HEAP=2g
export FLINK_JM_HEAP=1g
```

#### Command Line Parameters

```bash
# Event Detection Job
flink run \
  --class com.zjujzl.das.EventDetectionJob \
  --parallelism 4 \
  --checkpointing-interval 30000 \
  target/das-flink-1.0-SNAPSHOT.jar \
  --kafka.bootstrap.servers localhost:9092 \
  --kafka.input.topic seismic-data \
  --kafka.output.topic event-detection-results \
  --kafka.group.id das-event-detection \
  --algorithm A \
  --adaptive.detection true
```

#### flink-conf.yaml Settings

```yaml
# Resource configuration
taskmanager.memory.process.size: 2g
jobmanager.memory.process.size: 1g
taskmanager.numberOfTaskSlots: 4

# Checkpointing
state.backend: filesystem
state.checkpoints.dir: file:///tmp/flink-checkpoints
state.savepoints.dir: file:///tmp/flink-savepoints
execution.checkpointing.interval: 30s
execution.checkpointing.min-pause: 10s
execution.checkpointing.timeout: 10min

# Network
taskmanager.network.memory.fraction: 0.1
taskmanager.network.memory.min: 64mb
taskmanager.network.memory.max: 1gb

# Parallelism
parallelism.default: 4

# Restart strategy
restart-strategy: fixed-delay
restart-strategy.fixed-delay.attempts: 3
restart-strategy.fixed-delay.delay: 10s
```

### 3. Algorithm-Specific Configuration

#### Spatial Averaging

```properties
# Spatial averaging parameters
spatial.averaging.window.size=5
spatial.averaging.adaptive.enabled=true
spatial.averaging.adaptive.threshold=0.1
spatial.averaging.adaptive.max.window=15
```

#### Wavelet Denoising

```properties
# Wavelet parameters
wavelet.type=db4
wavelet.decomposition.levels=4
wavelet.threshold.method=soft
wavelet.threshold.value=auto
wavelet.boundary.condition=symmetric
```

#### FDDA+ Configuration

```properties
# Frequency domain parameters
fdda.window.size=1024
fdda.overlap.ratio=0.5
fdda.min.filter.value=0.1
fdda.noise.estimation.method=median
fdda.frequency.bands=10
```

#### SVD Filter

```properties
# SVD parameters
svd.window.size=64
svd.rank=10
svd.threshold=0.01
svd.auto.rank.selection=true
```

#### EMD Configuration

```properties
# EMD parameters
emd.max.imfs=8
emd.stop.criterion=0.01
emd.max.iterations=100
emd.boundary.condition=mirror
```

## Environment-Specific Configurations

### Development Environment

```properties
# Development settings
log.level=DEBUG
monitoring.enabled=true
debug.save.results=true
performance.optimization=false
kafka.auto.offset.reset=earliest
```

### Production Environment

```properties
# Production settings
log.level=INFO
monitoring.enabled=true
debug.save.results=false
performance.optimization=true
kafka.auto.offset.reset=latest
alert.email.enabled=true
```

### Testing Environment

```properties
# Testing settings
log.level=WARN
monitoring.enabled=false
debug.save.results=true
performance.optimization=false
kafka.auto.offset.reset=earliest
```

## Performance Tuning

### Memory Optimization

```properties
# Memory settings
jvm.heap.size=4g
jvm.gc.type=G1GC
jvm.gc.max.pause.ms=200
flink.memory.fraction=0.7
```

### CPU Optimization

```properties
# CPU settings
parallelism=8
thread.pool.size=16
cpu.affinity.enabled=true
```

### Network Optimization

```properties
# Network settings
network.buffer.size=32kb
network.buffers.per.channel=2
network.compression.enabled=true
```

### Disk I/O Optimization

```properties
# Disk settings
checkpoint.compression=true
state.backend.incremental=true
rocksdb.block.cache.size=256mb
```

## Monitoring Configuration

### Metrics Collection

```properties
# Metrics settings
metrics.enabled=true
metrics.interval.sec=30
metrics.reporters=prometheus,jmx

# Prometheus metrics
metrics.reporter.prometheus.class=org.apache.flink.metrics.prometheus.PrometheusReporter
metrics.reporter.prometheus.port=9249

# JMX metrics
metrics.reporter.jmx.class=org.apache.flink.metrics.jmx.JMXReporter
metrics.reporter.jmx.port=8789
```

### Logging Configuration

```xml
<!-- logback.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/das-flink.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/das-flink.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.zjujzl.das" level="INFO"/>
    <logger name="org.apache.flink" level="WARN"/>
    <logger name="org.apache.kafka" level="WARN"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## Security Configuration

### Kafka Security

```properties
# SSL configuration
security.protocol=SSL
ssl.truststore.location=/path/to/kafka.client.truststore.jks
ssl.truststore.password=password
ssl.keystore.location=/path/to/kafka.client.keystore.jks
ssl.keystore.password=password
ssl.key.password=password

# SASL configuration
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="password";
```

### Flink Security

```yaml
# Security settings in flink-conf.yaml
security.ssl.enabled: true
security.ssl.keystore: /path/to/keystore.jks
security.ssl.keystore-password: password
security.ssl.truststore: /path/to/truststore.jks
security.ssl.truststore-password: password
```

## Configuration Validation

### Parameter Validation Rules

```properties
# Validation rules
validation.sta.length.min=0.1
validation.sta.length.max=10.0
validation.lta.length.min=5.0
validation.lta.length.max=300.0
validation.threshold.on.min=1.0
validation.threshold.on.max=20.0
validation.threshold.off.min=0.5
validation.threshold.off.max=10.0
```

### Configuration Testing

```bash
# Test configuration
java -cp target/das-flink-1.0-SNAPSHOT.jar \
  com.zjujzl.das.config.ConfigValidator \
  --config src/main/resources/stalte-config.properties
```

## Troubleshooting

### Common Configuration Issues

1. **Memory Issues**:
   ```properties
   # Increase heap size
   jvm.heap.size=8g
   # Enable memory monitoring
   monitoring.memory.tracking=true
   ```

2. **Performance Issues**:
   ```properties
   # Increase parallelism
   parallelism=8
   # Enable performance optimization
   performance.optimization=true
   ```

3. **Kafka Connection Issues**:
   ```properties
   # Increase timeouts
   kafka.consumer.session.timeout.ms=30000
   kafka.consumer.heartbeat.interval.ms=3000
   ```

### Configuration Debugging

```properties
# Enable configuration debugging
debug.config.validation=true
debug.config.logging=true
log.level=DEBUG
```

## Best Practices

### Configuration Management

1. **Environment Separation**: Use different configurations for dev/test/prod
2. **Version Control**: Track configuration changes in version control
3. **Documentation**: Document all configuration changes
4. **Validation**: Always validate configurations before deployment

### Performance Optimization

1. **Profiling**: Profile your specific workload to optimize parameters
2. **Monitoring**: Continuously monitor performance metrics
3. **Testing**: Test configuration changes in non-production environments
4. **Gradual Changes**: Make incremental configuration adjustments

### Security Considerations

1. **Encryption**: Use SSL/TLS for all network communications
2. **Authentication**: Implement proper authentication mechanisms
3. **Authorization**: Use role-based access control
4. **Secrets Management**: Store sensitive configuration in secure vaults

## Configuration Templates

### Quick Start Template

```properties
# Minimal configuration for getting started
default.sta.length.sec=2.0
default.lta.length.sec=30.0
default.threshold.on=3.0
default.threshold.off=1.5
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=seismic-data
kafka.output.topic=event-detection-results
```

### High-Performance Template

```properties
# Optimized for high-throughput processing
performance.parallel.processing.enabled=true
performance.parallel.threads=8
performance.max.batch.size=5000
performance.memory.optimization=true
kafka.producer.batch.size=65536
kafka.producer.linger.ms=50
kafka.producer.compression.type=lz4
```

### High-Sensitivity Template

```properties
# Optimized for detecting small events
default.sta.length.sec=1.0
default.lta.length.sec=20.0
default.threshold.on=2.0
default.threshold.off=1.2
default.min.event.length.sec=0.3
spatial.averaging.window.size=3
wavelet.decomposition.levels=6
```

## Migration Guide

### Upgrading from Previous Versions

1. **Backup Current Configuration**: Save existing configuration files
2. **Review Changes**: Check changelog for configuration changes
3. **Update Parameters**: Migrate old parameters to new format
4. **Test Configuration**: Validate new configuration in test environment
5. **Deploy Gradually**: Roll out configuration changes incrementally

### Configuration Schema Changes

- **v1.0 → v1.1**: Added adaptive detection parameters
- **v1.1 → v1.2**: Restructured algorithm-specific configurations
- **v1.2 → v1.3**: Added environment-specific templates

## Support and Resources

### Documentation
- [API Documentation](../api/README.md)
- [Algorithm Documentation](../algorithms/README.md)
- [Deployment Guide](../DEPLOYMENT_GUIDE.md)

### Community
- GitHub Issues: Report configuration problems
- Discussion Forum: Ask configuration questions
- Wiki: Community-contributed configuration examples

### Professional Support
- Enterprise Support: Commercial configuration assistance
- Training: Configuration best practices training
- Consulting: Custom configuration optimization
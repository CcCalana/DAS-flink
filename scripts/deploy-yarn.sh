#!/bin/bash

# DAS-Flink Yarn部署脚本
# 适用于生产环境的Yarn集群部署

set -e

# 配置参数
FLINK_HOME=${FLINK_HOME:-"/opt/flink"}
HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-"/etc/hadoop/conf"}
YARN_CONF_DIR=${YARN_CONF_DIR:-"/etc/hadoop/conf"}
JAR_PATH="target/das-flink-1.0-SNAPSHOT.jar"
JOB_NAME="DAS-Event-Detection"

# Kafka配置
KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP:-"localhost:9092"}
KAFKA_TOPIC=${KAFKA_TOPIC:-"das-seismic-data"}
KAFKA_GROUP=${KAFKA_GROUP:-"flink_event_detection_group"}

# Flink作业配置
PARALLELISM=${PARALLELISM:-4}
CHECKPOINT_INTERVAL=${CHECKPOINT_INTERVAL:-60000}
STATE_BACKEND=${STATE_BACKEND:-"filesystem"}
CHECKPOINT_DIR=${CHECKPOINT_DIR:-"hdfs://namenode:9000/flink/checkpoints"}

# 资源配置
JOB_MANAGER_MEMORY=${JOB_MANAGER_MEMORY:-"1024m"}
TASK_MANAGER_MEMORY=${TASK_MANAGER_MEMORY:-"2048m"}
TASK_MANAGER_SLOTS=${TASK_MANAGER_SLOTS:-2}
NUM_TASK_MANAGERS=${NUM_TASK_MANAGERS:-2}

# 检测参数
HIGH_QUALITY_FILTER=${HIGH_QUALITY_FILTER:-true}
EVENT_AGGREGATION=${EVENT_AGGREGATION:-true}
AGGREGATION_WINDOW=${AGGREGATION_WINDOW:-60}
QUALITY_THRESHOLD=${QUALITY_THRESHOLD:-0.6}

echo "=== DAS-Flink Yarn部署脚本 ==="
echo "Flink Home: $FLINK_HOME"
echo "Hadoop Conf: $HADOOP_CONF_DIR"
echo "JAR Path: $JAR_PATH"
echo "Job Name: $JOB_NAME"
echo "Kafka Bootstrap: $KAFKA_BOOTSTRAP"
echo "Kafka Topic: $KAFKA_TOPIC"
echo "Parallelism: $PARALLELISM"
echo "JobManager Memory: $JOB_MANAGER_MEMORY"
echo "TaskManager Memory: $TASK_MANAGER_MEMORY"
echo "TaskManager Slots: $TASK_MANAGER_SLOTS"
echo "Number of TaskManagers: $NUM_TASK_MANAGERS"
echo

# 检查必要的环境
if [ ! -f "$JAR_PATH" ]; then
    echo "错误: JAR文件不存在: $JAR_PATH"
    echo "请先运行: mvn clean package"
    exit 1
fi

if [ ! -d "$FLINK_HOME" ]; then
    echo "错误: Flink目录不存在: $FLINK_HOME"
    echo "请设置正确的FLINK_HOME环境变量"
    exit 1
fi

if [ ! -d "$HADOOP_CONF_DIR" ]; then
    echo "错误: Hadoop配置目录不存在: $HADOOP_CONF_DIR"
    echo "请设置正确的HADOOP_CONF_DIR环境变量"
    exit 1
fi

# 设置环境变量
export HADOOP_CONF_DIR
export YARN_CONF_DIR
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$FLINK_HOME/lib/*

echo "正在提交Flink作业到Yarn集群..."

# 构建Flink命令
FLINK_CMD="$FLINK_HOME/bin/flink run-application \
    -t yarn-application \
    -Dyarn.application.name=$JOB_NAME \
    -Djobmanager.memory.process.size=$JOB_MANAGER_MEMORY \
    -Dtaskmanager.memory.process.size=$TASK_MANAGER_MEMORY \
    -Dtaskmanager.numberOfTaskSlots=$TASK_MANAGER_SLOTS \
    -Dparallelism.default=$PARALLELISM \
    -Dyarn.application.queue=default \
    -Dexecution.checkpointing.interval=$CHECKPOINT_INTERVAL \
    -Dstate.backend=$STATE_BACKEND \
    -Dstate.checkpoints.dir=$CHECKPOINT_DIR \
    -Drestart-strategy=fixed-delay \
    -Drestart-strategy.fixed-delay.attempts=3 \
    -Drestart-strategy.fixed-delay.delay=10s \
    -Denv.java.opts.jobmanager='-XX:+UseG1GC -XX:+PrintGCDetails' \
    -Denv.java.opts.taskmanager='-XX:+UseG1GC -XX:+PrintGCDetails' \
    $JAR_PATH \
    --bootstrap $KAFKA_BOOTSTRAP \
    --topic $KAFKA_TOPIC \
    --group $KAFKA_GROUP \
    --high-quality-filter $HIGH_QUALITY_FILTER \
    --event-aggregation $EVENT_AGGREGATION \
    --aggregation-window $AGGREGATION_WINDOW \
    --quality-threshold $QUALITY_THRESHOLD"

echo "执行命令:"
echo "$FLINK_CMD"
echo

# 执行命令
eval $FLINK_CMD

if [ $? -eq 0 ]; then
    echo
    echo "=== 作业提交成功 ==="
    echo "作业名称: $JOB_NAME"
    echo "可以通过以下方式监控作业:"
    echo "1. Yarn Web UI: http://resourcemanager:8088"
    echo "2. Flink Web UI: 通过Yarn应用页面的链接访问"
    echo "3. 命令行: $FLINK_HOME/bin/flink list -t yarn-application"
    echo
    echo "停止作业:"
    echo "$FLINK_HOME/bin/flink cancel -t yarn-application <job-id>"
    echo
else
    echo "错误: 作业提交失败"
    exit 1
fi
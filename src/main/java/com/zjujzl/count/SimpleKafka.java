package com.zjujzl.count;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * 运行示例：
 * flink run -t yarn-session \
 *   -Dyarn.application.id=<appId> \
 *   -c com.example.das.SimpleKafkaPrintJob \
 *   target/das-job-1.0.jar \
 *   --bootstrap cbdb-coordinator:9092,cbdb-datanode01:9093,cbdb-datanode02:9094 \
 *   --topic das-stream \
 *   --group test_print
 */
public class SimpleKafka {

    public static void main(String[] args) throws Exception {

        ParameterTool params = ParameterTool.fromArgs(args);
        String bootstrap = params.getRequired("bootstrap");
        String topic     = params.getRequired("topic");
        String groupId   = params.get("group", "flink_print_demo");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setAutoWatermarkInterval(0);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrap)
                .setTopics(topic)
                .setGroupId(groupId)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setStartingOffsets(OffsetsInitializer.earliest())
                .build();

        env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-source")
                .name("KafkaRawStream")
                .print()
                .setParallelism(1)
                .name("PrintSink");

        env.execute("Simple Kafka → Print");
    }
}
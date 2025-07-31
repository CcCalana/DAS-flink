package com.zjujzl.count;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleKafkaJsonToPojo {

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        String bootstrap = params.getRequired("bootstrap");
        String topic = params.getRequired("topic");
        String groupId = params.get("group", "flink_debug_group");

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
                .fromSource(source, WatermarkStrategy.noWatermarks(), "KafkaRawStream")
                .map(new JsonToPojoMapper())
                .name("JsonToPojo")
                .print()
                .setParallelism(1)
                .name("PrintSink");

        env.execute("Kafka JSON → POJO Debug");
    }

    public static class JsonToPojoMapper implements MapFunction<String, SeismicRecord> {
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        public SeismicRecord map(String json) throws Exception {
            SeismicRecord record = mapper.readValue(json, SeismicRecord.class);
            System.out.println("Parsed Record: " + record);
            return record;
        }
    }

    public static class SeismicRecord {
        public String network;
        public String station;
        public String location;
        public String channel;
        public double starttime;
        public double endtime;
        public double sampling_rate;
        public int[] data;
        public String idas_version;
        public int measure_length;
        public double geo_lat;

        @Override
        public String toString() {
            return String.format("SeismicRecord{station=%s, starttime=%.3f, length=%d}",
                    station, starttime, data != null ? data.length : 0);
        }
    }
}

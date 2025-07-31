// DasFlinkJob.java – Flink 1.17.1 DataStream API (Java)
// ======================================================
// * Source : Kafka topic "das-stream" (JSON)
// * Window : 1‑minute Tumbling → RMS per (station, channel)
// * Sink   : Print to stdout (for initial test)
//
// Build & Run (YARN‑Session):
// ------------------------------------------------------
// mvn clean package -DskipTests
//
// $FLINK_HOME/bin/flink run -t yarn-session  \
//   -Dyarn.application.id=application_..._0001 \
//   -c com.example.das.DasFlinkJob target/das-job-1.0.jar \
//   --bootstrap "cbdb-coordinator:9092,cbdb-datanode01:9093,cbdb-datanode02:9094" \
//   --group flink_das_demo
//
// Kafka connector dependency must be provided (lib or shaded).
// -----------------------------------------------------------

package com.zjujzl.count;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.Iterator;
import java.util.Properties;

public class DasFlinkJob {

    // ------------------------- POJO -------------------------
    public static class Sample {
        public String station;
        public String channel;
        public double ts;   // epoch 秒
        public int    val;

        // Flink POJO 需要无参构造器
        public Sample() {}
        public Sample(String sta, String cha, double ts, int v) {
            this.station = sta;
            this.channel = cha;
            this.ts = ts;
            this.val = v;
        }
    }

    // --------------------- FlatMap：JSON → Sample ---------------------
    public static class ExplodeFunction implements FlatMapFunction<String, Sample> {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        @Override
        public void flatMap(String json, Collector<Sample> out) throws Exception {
            JsonNode n = MAPPER.readTree(json);
            String sta = n.get("station").asText();
            String cha = n.get("channel").asText();
            double fs  = n.get("sampling_rate").asDouble();
            double t0  = n.get("starttime").asDouble();
            JsonNode arr = n.get("data");
            int idx = 0;
            for (Iterator<JsonNode> it = arr.elements(); it.hasNext(); ) {
                int v = it.next().asInt();
                double ts = t0 + idx / fs;
                out.collect(new Sample(sta, cha, ts, v));
                idx++;
            }
        }
    }

    // --------------------- Window → RMS ---------------------
    public static class RmsWindowFunction extends ProcessWindowFunction<Sample, String, Tuple2<String,String>, TimeWindow> {
        @Override
        public void process(Tuple2<String, String> key, Context ctx, Iterable<Sample> iterable, Collector<String> out) {
            double sumsq = 0;
            int count = 0;
            for (Sample s : iterable) {
                sumsq += s.val * (double) s.val;
                count++;
            }
            double rms = count == 0 ? 0 : Math.sqrt(sumsq / count);
            out.collect(String.format("%s.%s  window[%s - %s)  rms=%.3f  n=%d",
                    key.f0, key.f1,
                    new java.sql.Timestamp(ctx.window().getStart()),
                    new java.sql.Timestamp(ctx.window().getEnd()),
                    rms, count));
        }
    }

    // ---------------------------- MAIN ----------------------------
    public static void main(String[] args) throws Exception {
        System.out.println("RAW: " + java.util.Arrays.toString(args));
        System.out.println(ParameterTool.fromArgs(args).toMap());
        ParameterTool pt = ParameterTool.fromArgs(args);
        String bootstrap = pt.getRequired("bootstrap");
        String groupId   = pt.get("group",  "flink_das_demo");
        String topic     = pt.get("topic",  "das-stream");

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", bootstrap);
        kafkaProps.setProperty("group.id", groupId);


        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka Source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setProperties(kafkaProps)
                .setTopics(topic)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        WatermarkStrategy<String> noWm = WatermarkStrategy.noWatermarks();

        DataStream<String> jsonStream = env.fromSource(source, noWm, "kafka-source");

        // JSON → Sample 并加事件时间水位线
        DataStream<Sample> samples = jsonStream
                .flatMap(new ExplodeFunction())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Sample>forBoundedOutOfOrderness(Duration.ofSeconds(3))
                                .withTimestampAssigner((SerializableTimestampAssigner<Sample>) (s, ts) -> (long) (s.ts * 1000)))
                .returns(Types.POJO(Sample.class));

        // keyBy station+channel
        KeyedStream<Sample, Tuple2<String,String>> keyed = samples
                .keyBy(s -> Tuple2.of(s.station, s.channel), Types.TUPLE(Types.STRING, Types.STRING));

        // 1-min RMS
        SingleOutputStreamOperator<String> rms = keyed
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .process(new RmsWindowFunction())
                .returns(Types.STRING);

        rms.addSink(new PrintSinkFunction<>());

        env.execute("das_rms_job");
    }
}
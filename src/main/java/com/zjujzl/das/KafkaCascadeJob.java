package com.zjujzl.das;

import com.zjujzl.das.algorithm.*;
import com.zjujzl.das.model.DenoiseResult;
import com.zjujzl.das.model.EvaluationMetric;
import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.profile.NoiseProfiler;
import com.zjujzl.das.process.CascadeDenoiser;
import com.zjujzl.das.util.NoiseMetrics;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class KafkaCascadeJob {

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
                .setValueOnlyDeserializer(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                .setStartingOffsets(OffsetsInitializer.earliest())
                .build();

        DataStream<SeismicRecord> raw = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "KafkaRawStream")
                .map(new JsonToPojoMapper())
                .name("JsonToPojo")
                .filter(record -> record != null && record.data != null && record.data.length > 0)
                .name("FilterValidRecords");

        // 创建四个算法分支
        DataStream<DenoiseResult> resultsA = raw
                .keyBy(r -> r.station)
                .process(new CascadeDenoiser("A"))
                .name("cascade-A");

        DataStream<DenoiseResult> resultsB = raw
                .keyBy(r -> r.station)
                .process(new CascadeDenoiser("B"))
                .name("cascade-B");

        DataStream<DenoiseResult> resultsC = raw
                .keyBy(r -> r.station)
                .process(new CascadeDenoiser("C"))
                .name("cascade-C");

        DataStream<DenoiseResult> resultsD = raw
                .keyBy(r -> r.station)
                .process(new CascadeDenoiser("D"))
                .name("cascade-D");

        // 评估各分支算法与 A 的差异
        resultsA.connect(resultsB)
                .keyBy(r -> r.signal.station, r -> r.signal.station)
                .process(new PerformanceEvaluator("A", "B"))
                .name("Eval-A-B")
                .print();

        resultsA.connect(resultsC)
                .keyBy(r -> r.signal.station, r -> r.signal.station)
                .process(new PerformanceEvaluator("A", "C"))
                .name("Eval-A-C")
                .print();

        resultsA.connect(resultsD)
                .keyBy(r -> r.signal.station, r -> r.signal.station)
                .process(new PerformanceEvaluator("A", "D"))
                .name("Eval-A-D")
                .print();

        env.execute("Kafka JSON Cascade Eval Job");
    }

    public static class JsonToPojoMapper implements MapFunction<String, SeismicRecord> {
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        public SeismicRecord map(String json) throws Exception {
            try {
                if (json == null || json.trim().isEmpty()) {
                    System.err.println("WARN: Received null or empty JSON string");
                    return null;
                }
                
                SeismicRecord record = mapper.readValue(json, SeismicRecord.class);
                
                // 验证必要字段
                if (record.station == null || record.data == null || record.data.length == 0) {
                    System.err.println("WARN: Invalid record: station=" + record.station + 
                                     ", data length=" + (record.data != null ? record.data.length : 0));
                    return null;
                }
                
                System.out.println("DEBUG: Parsed Record: " + record);
                return record;
            } catch (Exception e) {
                System.err.println("ERROR: Failed to parse JSON: " + json + ", error: " + e.getMessage());
                return null; // 返回null，让过滤器处理
            }
        }
    }

    public static class PerformanceEvaluator extends CoProcessFunction<DenoiseResult, DenoiseResult, EvaluationMetric> {
        private final String baseAlg;
        private final String compareAlg;
        private transient MapState<String, DenoiseResult> resultBuffer;
        private transient MapState<String, Long> timestampBuffer;
        private static final long TIMEOUT_MS = 60000; // 1分钟超时

        public PerformanceEvaluator(String baseAlg, String compareAlg) {
            this.baseAlg = baseAlg;
            this.compareAlg = compareAlg;
        }

        @Override
        public void open(Configuration parameters) {
            MapStateDescriptor<String, DenoiseResult> resultDescriptor =
                    new MapStateDescriptor<>("results", String.class, DenoiseResult.class);
            resultBuffer = getRuntimeContext().getMapState(resultDescriptor);
            
            MapStateDescriptor<String, Long> timestampDescriptor =
                    new MapStateDescriptor<>("timestamps", String.class, Long.class);
            timestampBuffer = getRuntimeContext().getMapState(timestampDescriptor);
        }

        @Override
        public void processElement1(DenoiseResult result, Context ctx, Collector<EvaluationMetric> out) throws Exception {
            handle(result, ctx, out);
        }

        @Override
        public void processElement2(DenoiseResult result, Context ctx, Collector<EvaluationMetric> out) throws Exception {
            handle(result, ctx, out);
        }

        private void handle(DenoiseResult result, Context ctx, Collector<EvaluationMetric> out) throws Exception {
            try {
                String key = result.signal.station + "_" + result.algorithmId;
                long currentTime = ctx.timestamp() != null ? ctx.timestamp() : System.currentTimeMillis();
                
                resultBuffer.put(result.algorithmId, result);
                timestampBuffer.put(result.algorithmId, currentTime);
                
                // 清理超时的状态
                cleanupExpiredState(currentTime);

                if (resultBuffer.contains(baseAlg) && resultBuffer.contains(compareAlg)) {
                    DenoiseResult base = resultBuffer.get(baseAlg);
                    DenoiseResult cmp = resultBuffer.get(compareAlg);
                    
                    if (base != null && cmp != null && base.denoised != null && cmp.denoised != null) {
                        double[] raw = Arrays.stream(base.signal.data).asDoubleStream().toArray();

                        out.collect(new EvaluationMetric(base.signal.station, base.algorithmId,
                                NoiseMetrics.snrImprovement(raw, base.denoised),
                                NoiseMetrics.waveformDistortion(raw, base.denoised),
                                NoiseMetrics.featureCorrelation(raw, base.denoised),
                                base.processingTime));

                        out.collect(new EvaluationMetric(cmp.signal.station, cmp.algorithmId,
                                NoiseMetrics.snrImprovement(raw, cmp.denoised),
                                NoiseMetrics.waveformDistortion(raw, cmp.denoised),
                                NoiseMetrics.featureCorrelation(raw, cmp.denoised),
                                cmp.processingTime));
                    }

                    resultBuffer.clear();
                    timestampBuffer.clear();
                }
            } catch (Exception e) {
                System.err.println("ERROR: Error processing evaluation for algorithms " + baseAlg + " and " + compareAlg + ": " + e.getMessage());
            }
        }
        
        private void cleanupExpiredState(long currentTime) throws Exception {
            Iterator<Map.Entry<String, Long>> iterator = timestampBuffer.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (currentTime - entry.getValue() > TIMEOUT_MS) {
                    resultBuffer.remove(entry.getKey());
                    iterator.remove();
                    System.out.println("DEBUG: Cleaned up expired state for algorithm: " + entry.getKey());
                }
            }
        }
    }
}

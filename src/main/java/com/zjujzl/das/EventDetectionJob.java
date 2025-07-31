package com.zjujzl.das;

import com.zjujzl.das.model.EventDetectionResult;
import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.process.EventDetectionProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DAS实时事件检测作业
 * 集成降噪算法和STA/LTA事件检测，提供完整的地震事件检测流程
 * 功能特性：
 * 1. 多算法并行降噪处理
 * 2. STA/LTA实时事件检测
 * 3. 事件质量评估和过滤
 * 4. 实时统计和监控
 * 5. 高质量事件告警
 */
public class EventDetectionJob {
    
    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        ParameterTool params = ParameterTool.fromArgs(args);
        String bootstrap = params.getRequired("bootstrap");
        String topic = params.getRequired("topic");
        String groupId = params.get("group", "flink_event_detection_group");
        
        // 可配置的检测参数
        boolean enableHighQualityFilter = params.getBoolean("high-quality-filter", true);
        boolean enableEventAggregation = params.getBoolean("event-aggregation", true);
        int aggregationWindowSec = params.getInt("aggregation-window", 60);
        double qualityThreshold = params.getDouble("quality-threshold", 0.6);
        
        System.out.println("=== DAS Event Detection Job Starting ===");
        System.out.println("Bootstrap: " + bootstrap);
        System.out.println("Topic: " + topic);
        System.out.println("Group ID: " + groupId);
        System.out.println("High Quality Filter: " + enableHighQualityFilter);
        System.out.println("Event Aggregation: " + enableEventAggregation);
        
        // 设置Flink执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setAutoWatermarkInterval(0);
        
        // 配置Kafka源
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrap)
                .setTopics(topic)
                .setGroupId(groupId)
                .setValueOnlyDeserializer(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                .setStartingOffsets(OffsetsInitializer.latest())
                .build();
        
        // 原始数据流
        DataStream<SeismicRecord> rawStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "KafkaSource")
                .map(new JsonToSeismicRecordMapper())
                .filter(new ValidRecordFilter());
        
        // 使用智能路由进行事件检测（替代多分支并行处理）
        // SmartRouter会根据信号特征自动选择最优的降噪算法组合
        DataStream<EventDetectionResult> smartDetection = rawStream
                .keyBy(r -> r.station)
                .process(new EventDetectionProcessor("D")); // 使用D分支，内部调用SmartRouter
        
        // 智能检测结果
        DataStream<EventDetectionResult> allDetections = smartDetection;
        
        // 高质量事件过滤
        DataStream<EventDetectionResult> highQualityEvents = allDetections;
        if (enableHighQualityFilter) {
            highQualityEvents = allDetections
                    .filter(new HighQualityEventFilter(qualityThreshold));
        }
        
        // 实时事件输出
        highQualityEvents
                .process(new EventAlertProcessor())
                .print("ALERT");
        
        // 事件聚合统计（可选）
        if (enableEventAggregation) {
            allDetections
                    .keyBy(r -> r.originalSignal.station)
                    .window(TumblingProcessingTimeWindows.of(Time.seconds(aggregationWindowSec)))
                    .process(new EventAggregationProcessor())
                    .print("STATS");
        }
        
        // 所有检测结果输出（用于调试和监控）
        allDetections
                .process(new DetectionMonitorProcessor())
                .print("DETECTION");
        
        // 启动作业
        System.out.println("=== Starting Event Detection Job ===");
        env.execute("DAS Event Detection Job");
    }
    
    /**
     * JSON到SeismicRecord的映射器
     */
    public static class JsonToSeismicRecordMapper implements MapFunction<String, SeismicRecord> {
        private static final ObjectMapper mapper = new ObjectMapper();
        
        @Override
        public SeismicRecord map(String json) throws Exception {
            try {
                if (json == null || json.trim().isEmpty()) {
                    return null;
                }
                
                SeismicRecord record = mapper.readValue(json, SeismicRecord.class);
                
                // 基本验证
                if (record.station == null || record.data == null || record.data.length == 0) {
                    System.err.println("WARN: Invalid record structure");
                    return null;
                }
                
                return record;
            } catch (Exception e) {
                System.err.println("ERROR: Failed to parse JSON: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * 有效记录过滤器
     */
    public static class ValidRecordFilter implements FilterFunction<SeismicRecord> {
        @Override
        public boolean filter(SeismicRecord record) throws Exception {
            return record != null && 
                   record.station != null && 
                   record.data != null && 
                   record.data.length >= 100 &&  // 至少100个采样点
                   record.sampling_rate > 0;
        }
    }
    
    /**
     * 高质量事件过滤器
     */
    public static class HighQualityEventFilter implements FilterFunction<EventDetectionResult> {
        private final double qualityThreshold;
        
        public HighQualityEventFilter(double qualityThreshold) {
            this.qualityThreshold = qualityThreshold;
        }
        
        @Override
        public boolean filter(EventDetectionResult result) throws Exception {
            return result.hasSignificantEvents && 
                   result.signalQuality >= qualityThreshold &&
                   result.maxRatio > 2.5;
        }
    }
    
    /**
     * 事件告警处理器
     */
    public static class EventAlertProcessor extends ProcessFunction<EventDetectionResult, String> {
        @Override
        public void processElement(EventDetectionResult result, Context ctx, Collector<String> out) {
            if (result.hasSignificantEvents) {
                String alert = String.format(
                    "[ALERT] Station: %s | Algorithm: %s | Events: %d | MaxRatio: %.2f | Quality: %.2f | Time: %.3fs",
                    result.originalSignal.station,
                    result.denoisingAlgorithm,
                    result.totalEvents,
                    result.maxRatio,
                    result.signalQuality,
                    (result.originalSignal.endtime - result.originalSignal.starttime)
                );
                
                // 添加最显著事件的详细信息
                com.zjujzl.das.algorithm.STALTADetector.EventWindow mostSignificant = result.getMostSignificantEvent();
                if (mostSignificant != null) {
                    alert += String.format(
                        " | MostSignificant: [%.2fs-%.2fs, Ratio=%.2f]",
                        mostSignificant.startTime,
                        mostSignificant.endTime,
                        mostSignificant.maxRatio
                    );
                }
                
                out.collect(alert);
            }
        }
    }
    
    /**
     * 事件聚合处理器
     */
    public static class EventAggregationProcessor 
            extends ProcessWindowFunction<EventDetectionResult, String, String, TimeWindow> {
        
        @Override
        public void process(String station, Context context, 
                          Iterable<EventDetectionResult> elements, 
                          Collector<String> out) {
            
            List<EventDetectionResult> results = new ArrayList<>();
            elements.forEach(results::add);
            
            if (results.isEmpty()) {
                return;
            }
            
            // 统计信息
            int totalEvents = 0;
            double maxRatio = 0.0;
            double avgQuality = 0.0;
            long totalProcessingTime = 0;
            int highQualityCount = 0;
            
            for (EventDetectionResult result : results) {
                totalEvents += result.totalEvents;
                maxRatio = Math.max(maxRatio, result.maxRatio);
                avgQuality += result.signalQuality;
                totalProcessingTime += result.getTotalProcessingTime();
                if (result.isHighQualityDetection()) {
                    highQualityCount++;
                }
            }
            
            avgQuality /= results.size();
            double avgProcessingTime = (double) totalProcessingTime / results.size();
            
            String summary = String.format(
                "[STATS] Station: %s | Window: %s | Records: %d | TotalEvents: %d | MaxRatio: %.2f | AvgQuality: %.2f | HighQuality: %d | AvgTime: %.2fms",
                station,
                context.window().toString(),
                results.size(),
                totalEvents,
                maxRatio,
                avgQuality,
                highQualityCount,
                avgProcessingTime
            );
            
            out.collect(summary);
        }
    }
    
    /**
     * 检测监控处理器
     */
    public static class DetectionMonitorProcessor extends ProcessFunction<EventDetectionResult, String> {
        private transient MapState<String, Long> stationCounters;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            stationCounters = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("stationCounters", String.class, Long.class));
        }
        
        @Override
        public void processElement(EventDetectionResult result, Context ctx, Collector<String> out) throws Exception {
            String station = result.originalSignal.station;
            Long count = stationCounters.get(station);
            if (count == null) {
                count = 0L;
            }
            count++;
            stationCounters.put(station, count);
            
            // 每处理100条记录输出一次监控信息
            if (count % 100 == 0) {
                String monitor = String.format(
                    "[MONITOR] Station: %s | Processed: %d | Algorithm: %s | LastResult: %s",
                    station, count, result.denoisingAlgorithm, result.getCompactSummary()
                );
                out.collect(monitor);
            }
        }
    }
}
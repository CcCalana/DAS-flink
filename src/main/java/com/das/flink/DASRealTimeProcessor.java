package com.das.flink;

import com.das.flink.algorithm.*;
import com.das.flink.algorithm.DetectionEvent;
import com.das.flink.entity.SeismicRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * DAS实时数据处理主类
 * 从Kafka消费DAS数据流，进行实时地震事件检测和分析
 * 
 * @author DAS-Flink Team
 * @version 1.0
 */
public class DASRealTimeProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DASRealTimeProcessor.class);
    
    // 默认配置参数
    private static final String DEFAULT_KAFKA_BROKERS = "localhost:9092";
    private static final String DEFAULT_KAFKA_TOPIC = "das-seismic-data";
    private static final String DEFAULT_CONSUMER_GROUP = "das-flink-processor";
    private static final int DEFAULT_WINDOW_SIZE_SECONDS = 10;
    private static final int DEFAULT_SLIDE_SIZE_SECONDS = 1;
    private static final double DEFAULT_STA_WINDOW_SECONDS = 1.0;
    private static final double DEFAULT_LTA_WINDOW_SECONDS = 30.0;
    private static final double DEFAULT_TRIGGER_THRESHOLD = 3.0;
    private static final double DEFAULT_DETRIGGER_THRESHOLD = 1.5;
    
    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        ParameterTool params = ParameterTool.fromArgs(args);
        
        // 获取配置参数
        String kafkaBrokers = params.get("kafka.brokers", DEFAULT_KAFKA_BROKERS);
        String kafkaTopic = params.get("kafka.topic", DEFAULT_KAFKA_TOPIC);
        String consumerGroup = params.get("kafka.consumer.group", DEFAULT_CONSUMER_GROUP);
        int windowSize = params.getInt("window.size.seconds", DEFAULT_WINDOW_SIZE_SECONDS);
        int slideSize = params.getInt("slide.size.seconds", DEFAULT_SLIDE_SIZE_SECONDS);
        double staWindow = params.getDouble("sta.window.seconds", DEFAULT_STA_WINDOW_SECONDS);
        double ltaWindow = params.getDouble("lta.window.seconds", DEFAULT_LTA_WINDOW_SECONDS);
        double triggerThreshold = params.getDouble("trigger.threshold", DEFAULT_TRIGGER_THRESHOLD);
        double detriggerThreshold = params.getDouble("detrigger.threshold", DEFAULT_DETRIGGER_THRESHOLD);
        
        logger.info("启动DAS实时数据处理器...");
        logger.info("Kafka配置: brokers={}, topic={}, group={}", kafkaBrokers, kafkaTopic, consumerGroup);
        logger.info("处理参数: windowSize={}s, slideSize={}s, STA={}s, LTA={}s, trigger={}, detrigger={}", 
                   windowSize, slideSize, staWindow, ltaWindow, triggerThreshold, detriggerThreshold);
        
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度和检查点
        env.setParallelism(4);
        env.enableCheckpointing(5000); // 5秒检查点间隔
        
        // 配置Kafka源
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBrokers)
                .setTopics(kafkaTopic)
                .setGroupId(consumerGroup)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
        
        // 创建数据流
        DataStream<String> rawDataStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka DAS Source"
        );
        
        // 解析DAS数据
        DataStream<SeismicRecord> seismicStream = rawDataStream
                .map(new DASDataParser())
                .name("Parse DAS Data");
        
        // 创建自适应STA/LTA检测器
        AdaptiveSTALTA detector = new AdaptiveSTALTA();
        
        // 多通道检测器
        MultiChannelDetector multiChannelDetector = new MultiChannelDetector();
        
        // 应用滑动窗口进行实时检测
        DataStream<DetectionEvent> detectionStream = seismicStream
                .keyBy(record -> record.getChannelId())
                .window(SlidingProcessingTimeWindows.of(
                        Time.seconds(windowSize),
                        Time.seconds(slideSize)
                ))
                .process(new SeismicDetectionProcessor(detector, multiChannelDetector))
                .name("Seismic Detection");
        
        // 过滤和输出检测结果
        detectionStream
                .filter(event -> event.isValid())
                .map(event -> {
                    logger.info("检测到地震事件: 通道={}, 时间={}, 强度={}, 置信度={}", 
                               event.getChannelId(), event.getTimestamp(), 
                               event.getIntensityScore(), event.getConfidence());
                    return event.toString();
                })
                .name("Event Output")
                .print();
        
        // 启动作业
        logger.info("开始执行DAS实时数据处理作业...");
        env.execute("DAS Real-Time Seismic Detection");
    }
    
    /**
     * DAS数据解析器
     * 将Kafka中的原始字符串数据解析为SeismicRecord对象
     */
    public static class DASDataParser implements MapFunction<String, SeismicRecord> {
        
        @Override
        public SeismicRecord map(String rawData) throws Exception {
            try {
                // 假设数据格式为: timestamp,channelId,amplitude,frequency
                // 实际项目中需要根据具体的DAS数据格式进行调整
                String[] fields = rawData.split(",");
                
                if (fields.length >= 4) {
                    long timestamp = Long.parseLong(fields[0]);
                    String channelId = fields[1];
                    double amplitude = Double.parseDouble(fields[2]);
                    double frequency = Double.parseDouble(fields[3]);
                    
                    // 创建模拟的数据数组
                    float[] data = new float[]{(float)amplitude};
                    return new SeismicRecord(channelId, (float)frequency, data);
                } else {
                    logger.warn("数据格式不正确: {}", rawData);
                    return null;
                }
            } catch (Exception e) {
                logger.error("解析DAS数据失败: {}", rawData, e);
                return null;
            }
        }
    }
    
    /**
     * 地震检测处理器
     * 在窗口内应用STA/LTA算法和多通道检测
     */
    public static class SeismicDetectionProcessor 
            extends ProcessWindowFunction<SeismicRecord, DetectionEvent, String, TimeWindow> {
        
        private final AdaptiveSTALTA detector;
        private final MultiChannelDetector multiChannelDetector;
        
        public SeismicDetectionProcessor(AdaptiveSTALTA detector, MultiChannelDetector multiChannelDetector) {
            this.detector = detector;
            this.multiChannelDetector = multiChannelDetector;
        }
        
        @Override
        public void process(String channelId, Context context, 
                          Iterable<SeismicRecord> elements, 
                          Collector<DetectionEvent> out) throws Exception {
            
            // 收集窗口内的数据
            java.util.List<SeismicRecord> records = new java.util.ArrayList<>();
            for (SeismicRecord record : elements) {
                if (record != null) {
                    records.add(record);
                }
            }
            
            if (records.isEmpty()) {
                return;
            }
            
            // 转换为振幅数组
            double[] amplitudes = new double[records.size()];
            for (int i = 0; i < records.size(); i++) {
                float[] data = records.get(i).getData();
                if (data != null && data.length > 0) {
                    // 计算RMS振幅
                    double sum = 0.0;
                    for (float value : data) {
                        sum += value * value;
                    }
                    amplitudes[i] = Math.sqrt(sum / data.length);
                } else {
                    amplitudes[i] = 0.0;
                }
            }
            
            // 创建模拟的地震记录用于检测
            if (!records.isEmpty()) {
                SeismicRecord firstRecord = records.get(0);
                
                // 设置必要的字段以避免null值
                if (firstRecord.getNetwork() == null) {
                    firstRecord.setNetwork("DAS");
                }
                if (firstRecord.getStation() == null) {
                    firstRecord.setStation("ST01");
                }
                if (firstRecord.getChannel() == null) {
                    firstRecord.setChannel(firstRecord.getChannelId());
                }
                
                // 使用自适应STA/LTA检测器
                AdaptiveSTALTA.AdaptiveSTALTAResult adaptiveResult = detector.computeAdaptive(firstRecord);
                
                if (adaptiveResult.hasEvents()) {
                    // 创建STALTAResult用于多通道检测
                    STALTAResult result = new STALTAResult(
                        firstRecord.getChannelId(),
                        firstRecord.getData().length,
                        10, // STA长度
                        100 // LTA长度
                    );
                    result.setTimestamp(System.currentTimeMillis());
                    result.setRatios(adaptiveResult.getRatios());
                    result.calculateStatistics();
                    
                    // 多通道检测增强
                    List<DetectionEvent> enhancedEvents = multiChannelDetector.processChannelResult(result);
                    
                    // 输出所有增强的检测事件
                    for (DetectionEvent event : enhancedEvents) {
                        out.collect(event);
                    }
                }
            }
        }
    }
}
package com.zjujzl.das.example;

import com.zjujzl.das.algorithm.STALTADetector;
import com.zjujzl.das.config.EventDetectionConfig;
import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.model.EventDetectionResult;
import com.zjujzl.das.process.EventDetectionProcessor;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.Random;

/**
 * STA/LTA事件检测示例
 * 演示如何使用新的事件检测功能
 */
public class STALTAExample {
    
    public static void main(String[] args) {
        System.out.println("=== DAS STA/LTA Event Detection Example ===");
        
        // 示例1：基本STA/LTA检测
        basicSTALTAExample();
        
        // 示例2：自适应参数检测
        adaptiveDetectionExample();
        
        // 示例3：不同算法配置对比
        algorithmComparisonExample();
        
        // 示例4：模拟地震信号检测
        simulatedSeismicDetectionExample();
        
        System.out.println("\n=== Example completed ===");
    }
    
    /**
     * 示例1：基本STA/LTA检测
     */
    private static void basicSTALTAExample() {
        System.out.println("\n--- Example 1: Basic STA/LTA Detection ---");
        
        // 生成测试信号：背景噪声 + 事件信号
        double[] signal = generateTestSignal(1000, 100.0, true);
        double samplingRate = 100.0;
        
        // 执行STA/LTA检测
        STALTADetector.DetectionResult result = STALTADetector.detect(
            signal, samplingRate, 2.0, 20.0, 3.0, 1.5, 1.0
        );
        
        // 输出结果
        System.out.println("Signal length: " + signal.length + " samples");
        System.out.println("Events detected: " + result.totalEvents);
        System.out.println("Max STA/LTA ratio: " + String.format("%.2f", result.maxRatio));
        System.out.println("Detection summary: " + STALTADetector.getDetectionSummary(result, samplingRate));
        
        // 显示事件详情
        for (int i = 0; i < result.events.size(); i++) {
            STALTADetector.EventWindow event = result.events.get(i);
            System.out.println(String.format(
                "  Event %d: %.2fs - %.2fs (duration: %.2fs, max ratio: %.2f)",
                i + 1, event.startTime, event.endTime, event.duration, event.maxRatio
            ));
        }
    }
    
    /**
     * 示例2：自适应参数检测
     */
    private static void adaptiveDetectionExample() {
        System.out.println("\n--- Example 2: Adaptive Parameter Detection ---");
        
        // 生成不同特征的测试信号
        double[] quietSignal = generateTestSignal(800, 50.0, false);  // 安静环境
        double[] noisySignal = generateTestSignal(1200, 200.0, true); // 噪声环境
        
        double samplingRate = 100.0;
        
        // 对比固定参数和自适应参数
        System.out.println("\nQuiet signal detection:");
        compareDetectionMethods(quietSignal, samplingRate);
        
        System.out.println("\nNoisy signal detection:");
        compareDetectionMethods(noisySignal, samplingRate);
    }
    
    /**
     * 示例3：不同算法配置对比
     */
    private static void algorithmComparisonExample() {
        System.out.println("\n--- Example 3: Algorithm Configuration Comparison ---");
        
        // 生成测试信号
        double[] signal = generateTestSignal(1500, 150.0, true);
        double samplingRate = 100.0;
        
        // 测试不同算法的配置
        String[] algorithms = {"A", "B", "C", "D"};
        
        for (String algorithm : algorithms) {
            EventDetectionConfig.STALTAParams params = EventDetectionConfig.getAlgorithmParams(algorithm);
            
            STALTADetector.DetectionResult result = STALTADetector.detect(
                signal, samplingRate,
                params.staLengthSec, params.ltaLengthSec,
                params.thresholdOn, params.thresholdOff,
                EventDetectionConfig.DEFAULT_MIN_EVENT_LENGTH_SEC
            );
            
            System.out.println(String.format(
                "Algorithm %s: %s -> Events: %d, Max Ratio: %.2f",
                algorithm, params.toString(), result.totalEvents, result.maxRatio
            ));
        }
    }
    
    /**
     * 示例4：模拟地震信号检测
     */
    private static void simulatedSeismicDetectionExample() {
        System.out.println("\n--- Example 4: Simulated Seismic Signal Detection ---");
        
        // 创建模拟地震记录
        SeismicRecord record = createSimulatedSeismicRecord();
        
        // 使用EventDetectionProcessor进行完整的处理
        EventDetectionProcessor processor = new EventDetectionProcessor("A");
        
        // 模拟处理过程
        TestCollector collector = new TestCollector();
        
        try {
            // 注意：这里简化了Flink的Context，实际使用中需要完整的Flink环境
            // processor.processElement(record, null, collector);
            
            // 直接使用STALTADetector进行检测演示
            double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
            STALTADetector.DetectionResult result = STALTADetector.adaptiveDetect(signal, record.sampling_rate);
            
            System.out.println("Simulated seismic record:");
            System.out.println("  Station: " + record.station);
            System.out.println("  Sampling rate: " + record.sampling_rate + " Hz");
            System.out.println("  Signal length: " + record.data.length + " samples");
            System.out.println("  Duration: " + String.format("%.2f", record.data.length / record.sampling_rate) + " seconds");
            System.out.println("\nDetection results:");
            System.out.println("  Events detected: " + result.totalEvents);
            System.out.println("  Max STA/LTA ratio: " + String.format("%.2f", result.maxRatio));
            System.out.println("  " + STALTADetector.getDetectionSummary(result, record.sampling_rate));
            
        } catch (Exception e) {
            System.err.println("Error in processing: " + e.getMessage());
        }
    }
    
    /**
     * 对比固定参数和自适应参数的检测效果
     */
    private static void compareDetectionMethods(double[] signal, double samplingRate) {
        // 固定参数检测
        STALTADetector.DetectionResult fixedResult = STALTADetector.detect(
            signal, samplingRate, 2.0, 30.0, 3.0, 1.5, 1.0
        );
        
        // 自适应参数检测
        STALTADetector.DetectionResult adaptiveResult = STALTADetector.adaptiveDetect(
            signal, samplingRate
        );
        
        System.out.println(String.format(
            "  Fixed params:    Events: %d, Max Ratio: %.2f",
            fixedResult.totalEvents, fixedResult.maxRatio
        ));
        System.out.println(String.format(
            "  Adaptive params: Events: %d, Max Ratio: %.2f",
            adaptiveResult.totalEvents, adaptiveResult.maxRatio
        ));
    }
    
    /**
     * 生成测试信号
     */
    private static double[] generateTestSignal(int length, double samplingRate, boolean includeEvent) {
        Random random = new Random(42); // 固定种子以获得可重复的结果
        double[] signal = new double[length];
        
        // 生成背景噪声
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian() * 10.0; // 标准差为10的高斯噪声
        }
        
        // 添加事件信号
        if (includeEvent) {
            // 在信号中间添加一个模拟地震事件
            int eventStart = length / 3;
            int eventLength = (int) (3.0 * samplingRate); // 3秒事件
            
            for (int i = 0; i < eventLength && (eventStart + i) < length; i++) {
                double t = i / samplingRate;
                // 模拟地震波形：衰减的正弦波
                double amplitude = 100.0 * Math.exp(-t / 2.0); // 指数衰减
                double frequency = 5.0; // 5Hz主频
                signal[eventStart + i] += amplitude * Math.sin(2 * Math.PI * frequency * t);
            }
            
            // 添加第二个较小的事件
            int event2Start = 2 * length / 3;
            int event2Length = (int) (1.5 * samplingRate); // 1.5秒事件
            
            for (int i = 0; i < event2Length && (event2Start + i) < length; i++) {
                double t = i / samplingRate;
                double amplitude = 50.0 * Math.exp(-t / 1.0);
                double frequency = 8.0; // 8Hz主频
                signal[event2Start + i] += amplitude * Math.sin(2 * Math.PI * frequency * t);
            }
        }
        
        return signal;
    }
    
    /**
     * 创建模拟地震记录
     */
    private static SeismicRecord createSimulatedSeismicRecord() {
        SeismicRecord record = new SeismicRecord();
        record.network = "TEST";
        record.station = "STA001";
        record.location = "00";
        record.channel = "HHZ";
        record.starttime = System.currentTimeMillis() / 1000.0;
        record.sampling_rate = 100.0;
        record.measure_length = 2000;
        
        // 生成模拟数据
        double[] signal = generateTestSignal(2000, 100.0, true);
        record.data = Arrays.stream(signal).mapToInt(d -> (int) Math.round(d)).toArray();
        record.endtime = record.starttime + record.data.length / record.sampling_rate;
        
        return record;
    }
    
    /**
     * 测试用的Collector实现
     */
    private static class TestCollector implements Collector<EventDetectionResult> {
        @Override
        public void collect(EventDetectionResult record) {
            System.out.println("Collected result: " + record.toString());
        }
        
        @Override
        public void close() {
            // 测试实现，无需操作
        }
    }
}
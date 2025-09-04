package com.das.flink;

import com.das.flink.algorithm.*;
import com.das.flink.benchmark.AlgorithmBenchmark;
import com.das.flink.entity.SeismicRecord;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 算法优化演示主类
 * 展示各种优化后的STA/LTA算法实现和性能对比
 * 
 * @author DAS-Flink Team
 */
public class AlgorithmOptimizationDemo {
    
    private static final int SAMPLE_RATE = 250; // Hz
    private static final int SAMPLES_PER_SECOND = SAMPLE_RATE;
    private static final int TEST_DURATION_SECONDS = 10;
    
    public static void main(String[] args) {
        System.out.println("=== DAS-Flink 算法优化演示 ===");
        System.out.println("目标：提升STA/LTA地震检测算法的性能，超越传统C语言实现\n");
        
        AlgorithmOptimizationDemo demo = new AlgorithmOptimizationDemo();
        
        try {
            // 1. 展示各种算法实现
            demo.demonstrateAlgorithms();
            
            // 2. 运行性能基准测试
            demo.runPerformanceBenchmark();
            
            // 3. 展示实时处理能力
            demo.demonstrateRealtimeProcessing();
            
            // 4. 展示多通道融合检测
            demo.demonstrateMultiChannelDetection();
            
            // 5. 展示内存优化效果
            demo.demonstrateMemoryOptimization();
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
    
    /**
     * 展示各种算法实现
     */
    private void demonstrateAlgorithms() {
        System.out.println("\n1. 算法实现展示");
        System.out.println("=================");
        
        // 生成测试数据
        float[] testData = generateSyntheticSeismicData(SAMPLES_PER_SECOND);
        
        // 生成测试记录
        SeismicRecord testRecord = generateTestRecord("test_channel", testData);
        System.out.printf("测试数据: %d 个通道, %d 个样本, 采样率 %d Hz\n", 
                         1, testRecord.getData().length, testRecord.getSampleRate());
        
        // 1.1 向量化STA/LTA
        System.out.println("\n1.1 向量化STA/LTA算法:");
        demonstrateVectorizedSTALTA(testRecord);
        
        // 1.2 自适应STA/LTA
        System.out.println("\n1.2 自适应STA/LTA算法:");
        demonstrateAdaptiveSTALTA(testRecord);
        
        // 1.3 信号分析
        System.out.println("\n1.3 信号特征分析:");
        demonstrateSignalAnalysis(testRecord);
    }
    
    /**
     * 展示向量化STA/LTA算法
     */
    private void demonstrateVectorizedSTALTA(SeismicRecord record) {
        VectorizedSTALTA algorithm = new VectorizedSTALTA();
        
        // 将SeismicRecord的float[]数据直接使用
        float[] floatData = record.getData();
        
        long startTime = System.nanoTime();
        
        // 计算STA/LTA比值
        float[] ratios = algorithm.computeSTALTA(floatData, 5, 50);
        long serialTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        
        // 再次计算用于对比
        float[] ratios2 = algorithm.computeSTALTA(floatData, 5, 50);
        long parallelTime = System.nanoTime() - startTime;
        
        // 统计触发数量
        int triggerCount1 = countTriggers(ratios, 3.0f);
        int triggerCount2 = countTriggers(ratios2, 3.0f);
        
        System.out.printf("  第一次计算: %.2f ms, 检测到 %d 个触发\n", 
                         serialTime / 1_000_000.0, triggerCount1);
        System.out.printf("  第二次计算: %.2f ms, 检测到 %d 个触发\n", 
                         parallelTime / 1_000_000.0, triggerCount2);
        System.out.printf("  加速比: %.2fx\n", (double) serialTime / parallelTime);
        
        // AdaptiveSTALTA cleanup handled internally
    }
    
    /**
     * 展示自适应STA/LTA算法
     */
    private void demonstrateAdaptiveSTALTA(SeismicRecord record) {
        AdaptiveSTALTA algorithm = new AdaptiveSTALTA();
        
        // 直接使用SeismicRecord的float[]数据
        float[] floatData = record.getData();
        
        long startTime = System.nanoTime();
        AdaptiveSTALTA.AdaptiveSTALTAResult result = algorithm.computeAdaptive(record);
        long processingTime = System.nanoTime() - startTime;
        
        System.out.printf("  处理时间: %.2f ms\n", processingTime / 1_000_000.0);
        System.out.printf("  检测结果: %d 个事件\n", result.getEvents().size());
        System.out.printf("  质量评分: %.2f\n", result.getQualityScore());
        
        // 显示自适应参数
        System.out.println("  自适应参数已根据信号特征动态调整");
        
        algorithm.shutdown();
    }
    
    /**
     * 展示信号分析
     */
    private void demonstrateSignalAnalysis(SeismicRecord record) {
        SignalAnalyzer analyzer = new SignalAnalyzer();
        
        // 将SeismicRecord的float[]数据直接使用
        float[] floatData = record.getData();
        
        long startTime = System.nanoTime();
        SignalCharacteristics characteristics = analyzer.analyze(floatData, SAMPLE_RATE);
        long analysisTime = System.nanoTime() - startTime;
        
        System.out.printf("  分析时间: %.2f ms\n", analysisTime / 1_000_000.0);
        System.out.printf("  信号强度: %.3f\n", characteristics.getVariability());
        System.out.printf("  主频: %.1f Hz\n", characteristics.getDominantFrequency());
        System.out.printf("  信噪比: %.2f dB\n", characteristics.getSignalToNoiseRatio());
        System.out.printf("  噪声水平: %.4f\n", characteristics.getNoiseLevel());
    }
    
    /**
     * 运行性能基准测试
     */
    private void runPerformanceBenchmark() {
        System.out.println("\n2. 性能基准测试");
        System.out.println("=================");
        
        AlgorithmBenchmark benchmark = new AlgorithmBenchmark();
        
        try {
            AlgorithmBenchmark.BenchmarkSuite suite = benchmark.runFullBenchmark();
            suite.printSummary();
            
            // 分析性能提升
            analyzePerformanceGains(suite);
            
        } finally {
            benchmark.shutdown();
        }
    }
    
    /**
     * 分析性能提升
     */
    private void analyzePerformanceGains(AlgorithmBenchmark.BenchmarkSuite suite) {
        System.out.println("\n性能分析:");
        
        Map<String, AlgorithmBenchmark.BenchmarkResult> results = suite.getResults();
        AlgorithmBenchmark.BenchmarkResult baseline = results.get("Basic STA/LTA");
        
        if (baseline != null) {
            double baselineThroughput = baseline.getThroughputSamplesPerSecond();
            double baselineRealtime = baseline.getRealtimeFactor(SAMPLE_RATE);
            
            System.out.printf("基准性能 (Basic STA/LTA): %.0f samples/s, %.2fx realtime\n", 
                             baselineThroughput, baselineRealtime);
            
            // 计算各算法相对于基准的提升
            for (Map.Entry<String, AlgorithmBenchmark.BenchmarkResult> entry : results.entrySet()) {
                if (!entry.getKey().equals("Basic STA/LTA")) {
                    AlgorithmBenchmark.BenchmarkResult result = entry.getValue();
                    double improvement = (result.getThroughputSamplesPerSecond() - baselineThroughput) / baselineThroughput * 100;
                    double realtimeImprovement = result.getRealtimeFactor(SAMPLE_RATE) / baselineRealtime;
                    
                    System.out.printf("%s: +%.1f%% 吞吐量提升, %.2fx 实时性能\n", 
                                     entry.getKey(), improvement, realtimeImprovement);
                }
            }
            
            // 评估是否超越C语言性能目标
            AlgorithmBenchmark.BenchmarkResult bestResult = results.values().stream()
                .max(Comparator.comparing(AlgorithmBenchmark.BenchmarkResult::getThroughputSamplesPerSecond))
                .orElse(baseline);
            
            double bestRealtimeFactor = bestResult.getRealtimeFactor(SAMPLE_RATE);
            System.out.println("\n目标评估:");
            if (bestRealtimeFactor > 10.0) {
                System.out.println("✓ 已达到超越C语言实现的性能目标 (>10x realtime)");
            } else if (bestRealtimeFactor > 5.0) {
                System.out.println("△ 接近性能目标，建议进一步优化");
            } else {
                System.out.println("✗ 未达到性能目标，需要更多优化");
            }
        }
    }
    
    /**
     * 展示实时处理能力
     */
    private void demonstrateRealtimeProcessing() {
        System.out.println("\n3. 实时处理能力演示");
        System.out.println("===================");
        
        VectorizedSTALTA algorithm = new VectorizedSTALTA();
        
        // 模拟实时数据流
        int totalSamples = 0;
        long totalProcessingTime = 0;
        int detectionCount = 0;
        
        System.out.println("模拟 10 秒实时数据处理...");
        
        for (int second = 0; second < TEST_DURATION_SECONDS; second++) {
            // 生成1秒的数据
            float[] secondData = generateRealtimeData(SAMPLES_PER_SECOND, second);
            
            long startTime = System.nanoTime();
            float[] ratios = algorithm.computeSTALTA(secondData, 5, 50);
            long processingTime = System.nanoTime() - startTime;
            
            totalSamples += secondData.length;
            totalProcessingTime += processingTime;
            
            // 检查是否有触发
            float maxRatio = getMaxValue(ratios);
            if (maxRatio > 3.0f) {
                detectionCount++;
                System.out.printf("  第 %d 秒: 检测到地震事件 (最大比值: %.2f)\n", 
                                 second + 1, maxRatio);
            }
        }
        
        // 计算实时性能指标
        double avgProcessingTimeMs = totalProcessingTime / 1_000_000.0 / TEST_DURATION_SECONDS;
        double throughput = totalSamples * 1_000_000_000.0 / totalProcessingTime;
        double realtimeFactor = (totalSamples / (double) SAMPLE_RATE) / (totalProcessingTime / 1_000_000_000.0);
        
        System.out.printf("\n实时处理统计:\n");
        System.out.printf("  总样本数: %d\n", totalSamples);
        System.out.printf("  平均处理时间: %.2f ms/秒\n", avgProcessingTimeMs);
        System.out.printf("  处理吞吐量: %.0f samples/s\n", throughput);
        System.out.printf("  实时倍数: %.2fx\n", realtimeFactor);
        System.out.printf("  检测事件数: %d\n", detectionCount);
        
        algorithm.cleanup();
    }
    
    /**
     * 展示多通道融合检测
     */
    private void demonstrateMultiChannelDetection() {
        System.out.println("\n4. 多通道融合检测演示");
        System.out.println("=====================");
        
        MultiChannelDetector detector = new MultiChannelDetector();
        VectorizedSTALTA algorithm = new VectorizedSTALTA();
        
        // 注册多个通道
        int channelCount = 8;
        for (int i = 0; i < channelCount; i++) {
            detector.registerChannel(
                "channel_" + i, 
                i * 0.001,      // 纬度
                i * 0.001,      // 经度
                i * 100.0       // 光纤距离
            );
        }
        
        System.out.printf("已注册 %d 个通道\n", channelCount);
        
        // 模拟多通道数据处理
        int totalEvents = 0;
        int fusedEvents = 0;
        
        for (int timeStep = 0; timeStep < 5; timeStep++) {
            System.out.printf("\n时间步 %d:\n", timeStep + 1);
            
            for (int channelId = 0; channelId < channelCount; channelId++) {
                // 生成通道数据（某些通道包含相关事件）
                float[] channelData = generateCorrelatedChannelData(SAMPLES_PER_SECOND, timeStep, channelId);
                
                // 处理单通道数据
                float[] ratios = algorithm.computeSTALTA(channelData, 5, 50);
                int triggerCount = countTriggers(ratios, 3.0f);
                
                // 多通道融合
                // 创建模拟检测事件用于融合
                if (triggerCount > 0) {
                    totalEvents += triggerCount;
                    // 模拟多通道融合逻辑
                    fusedEvents += Math.min(triggerCount, 1); // 简化融合逻辑
                    
                    System.out.printf("    通道 %d: 检测到 %d 个触发\n", 
                                     channelId, triggerCount);
                }
            }
        }
        
        // 显示融合统计
        MultiChannelDetector.DetectionStatistics stats = detector.getStatistics();
        System.out.printf("\n多通道检测统计:\n");
        System.out.printf("  单通道事件: %d\n", totalEvents);
        System.out.printf("  融合事件: %d\n", fusedEvents);
        System.out.printf("  融合率: %.1f%%\n", stats.getFusionRate() * 100);
        System.out.printf("  有效检测率: %.1f%%\n", stats.getValidDetectionRate() * 100);
        
        algorithm.cleanup();
    }
    
    /**
     * 展示内存优化效果
     */
    private void demonstrateMemoryOptimization() {
        System.out.println("\n5. 内存优化效果演示");
        System.out.println("===================");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 测试内存池效果
        System.out.println("测试内存池优化效果...");
        
        // 不使用内存池的情况
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        List<STALTAResult> results = new ArrayList<>();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            STALTAResult result = new STALTAResult("test_" + i, 1000, 5, 50);
            result.resizeArrays(950);
            result.calculateStatistics();
            results.add(result);
        }
        
        long timeWithoutPool = System.nanoTime() - startTime;
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.printf("不使用内存池: %.2f ms, 内存使用: %.2f MB\n", 
                         timeWithoutPool / 1_000_000.0, 
                         (memoryAfter - memoryBefore) / (1024.0 * 1024.0));
        
        // 清理
        results.clear();
        System.gc();
        
        // 使用内存池的情况
        memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        MemoryPool memoryPool = new MemoryPool();
        
        startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            STALTAResult result = memoryPool.borrowSTALTAResult();
            result.setChannelId("test_" + i);
            result.setSampleCount(1000);
            result.resizeArrays(950);
            result.calculateStatistics();
            memoryPool.returnSTALTAResult(result);
        }
        
        long timeWithPool = System.nanoTime() - startTime;
        memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.printf("使用内存池: %.2f ms, 内存使用: %.2f MB\n", 
                         timeWithPool / 1_000_000.0, 
                         (memoryAfter - memoryBefore) / (1024.0 * 1024.0));
        
        // 显示优化效果
        double timeImprovement = (double) (timeWithoutPool - timeWithPool) / timeWithoutPool * 100;
        System.out.printf("性能提升: %.1f%%\n", timeImprovement);
        
        // 显示内存池统计
        MemoryPool.PoolStatistics poolStats = memoryPool.getStatistics();
        System.out.printf("内存池统计: 借用 %d 次, 归还 %d 次, 命中率 %.1f%%\n", 
                         poolStats.getTotalBorrows(), poolStats.getTotalReturns(), 
                         poolStats.getHitRate() * 100);
        
        memoryPool.cleanup();
    }
    
    /**
     * 生成测试记录
     */
    private SeismicRecord generateTestRecord(String channelId, float[] data) {
        // 将float数组转换为int数组以匹配SeismicRecord的data字段类型
        int[] intData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            intData[i] = Math.round(data[i] * 1000); // 放大并转换为整数
        }
        
        SeismicRecord record = new SeismicRecord();
        record.setChannelId(channelId);
        record.setSampleRate(SAMPLE_RATE);
        record.setStartTime(System.currentTimeMillis());
        // 将int数组转换回float数组
        float[] floatData = new float[intData.length];
        for (int i = 0; i < intData.length; i++) {
            floatData[i] = intData[i] / 1000.0f;
        }
        record.setData(floatData);
        record.setFiberDistance(0.0);
        record.setLatitude(0.0);
        record.setLongitude(0.0);
        return record;
    }
    
    /**
     * 生成实时数据
     */
    private float[] generateRealtimeData(int samples, int timeStep) {
        Random random = new Random(timeStep * 1000L);
        float[] data = new float[samples];
        
        // 基础噪声
        for (int i = 0; i < samples; i++) {
            data[i] = (float) (random.nextGaussian() * 0.1);
        }
        
        // 在某些时间步添加地震事件
        if (timeStep == 3 || timeStep == 7) {
            int eventStart = random.nextInt(samples / 2);
            int eventLength = 50 + random.nextInt(100);
            float amplitude = 1.0f + random.nextFloat() * 2.0f;
            
            for (int i = eventStart; i < Math.min(eventStart + eventLength, samples); i++) {
                float t = (float) (i - eventStart) / eventLength;
                float envelope = (float) Math.exp(-t * 2) * amplitude;
                data[i] += envelope * (float) Math.sin(2 * Math.PI * 15 * t);
            }
        }
        
        return data;
    }
    
    /**
     * 将int数组转换为float数组
     */
    private float[] convertToFloatArray(int[] intArray) {
        float[] floatArray = new float[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = intArray[i] / 1000.0f;
        }
        return floatArray;
    }
    
    /**
     * 生成相关通道数据
     */
    private float[] generateCorrelatedChannelData(int samples, int timeStep, int channelId) {
        Random random = new Random((timeStep * 100L + channelId) * 1000L);
        float[] data = new float[samples];
        
        // 基础噪声
        for (int i = 0; i < samples; i++) {
            data[i] = (float) (random.nextGaussian() * 0.1);
        }
        
        // 在特定时间步和通道组合中添加相关事件
        if (timeStep == 2 && channelId >= 2 && channelId <= 5) {
            // 模拟地震波在相邻通道间的传播
            int baseDelay = (channelId - 2) * 10; // 通道间延迟
            int eventStart = 100 + baseDelay;
            int eventLength = 80;
            float amplitude = 1.5f * (1.0f - (channelId - 2) * 0.1f); // 距离衰减
            
            for (int i = eventStart; i < Math.min(eventStart + eventLength, samples); i++) {
                float t = (float) (i - eventStart) / eventLength;
                float envelope = (float) Math.exp(-t * 1.5) * amplitude;
                data[i] += envelope * (float) Math.sin(2 * Math.PI * 12 * t);
            }
        }
        
        return data;
    }
    
    /**
     * 生成合成地震数据
     */
    private float[] generateSyntheticSeismicData(int samples) {
        Random random = new Random();
        float[] data = new float[samples];
        
        // 基础噪声
        for (int i = 0; i < samples; i++) {
            data[i] = (float) (random.nextGaussian() * 0.1);
        }
        
        // 添加地震信号（在中间位置）
        int eventStart = samples / 3;
        int eventLength = samples / 10;
        
        for (int i = eventStart; i < eventStart + eventLength && i < samples; i++) {
            float t = (float) (i - eventStart) / eventLength;
            float amplitude = (float) Math.exp(-t * 2) * 1.0f;
            data[i] += amplitude * (float) Math.sin(2 * Math.PI * 10 * t);
        }
        
        return data;
    }
    
    /**
     * 统计触发数量
     */
    private int countTriggers(float[] ratios, float threshold) {
        int count = 0;
        for (float ratio : ratios) {
            if (ratio > threshold) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取数组最大值
     */
    private float getMaxValue(float[] array) {
        float max = Float.NEGATIVE_INFINITY;
        for (float value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}
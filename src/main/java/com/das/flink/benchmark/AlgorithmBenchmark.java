package com.das.flink.benchmark;

import com.das.flink.algorithm.*;
import com.das.flink.entity.SeismicRecord;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 算法性能基准测试类
 * 用于评估和对比不同STA/LTA算法实现的性能
 * 
 * @author DAS-Flink Team
 */
public class AlgorithmBenchmark {
    
    private final ExecutorService executorService;
    private final MemoryPool memoryPool;
    private final Random random;
    
    // 测试配置
    private final int warmupIterations;
    private final int benchmarkIterations;
    private final int threadCount;
    
    // 统计信息
    private final AtomicLong totalProcessedSamples;
    private final AtomicLong totalProcessingTimeNs;
    private final AtomicReference<BenchmarkResult> lastResult;
    
    public AlgorithmBenchmark() {
        this(4, 10, 100);
    }
    
    public AlgorithmBenchmark(int threadCount, int warmupIterations, int benchmarkIterations) {
        this.threadCount = threadCount;
        this.warmupIterations = warmupIterations;
        this.benchmarkIterations = benchmarkIterations;
        
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.memoryPool = new MemoryPool();
        this.random = new Random(42); // 固定种子确保可重复性
        
        this.totalProcessedSamples = new AtomicLong(0);
        this.totalProcessingTimeNs = new AtomicLong(0);
        this.lastResult = new AtomicReference<>();
    }
    
    /**
     * 运行完整的基准测试套件
     */
    public BenchmarkSuite runFullBenchmark() {
        System.out.println("开始运行算法性能基准测试...");
        
        BenchmarkSuite suite = new BenchmarkSuite();
        
        // 1. 基础STA/LTA算法测试
        System.out.println("\n=== 基础STA/LTA算法测试 ===");
        BenchmarkResult basicResult = benchmarkBasicSTALTA();
        suite.addResult("Basic STA/LTA", basicResult);
        
        // 2. 向量化STA/LTA算法测试
        System.out.println("\n=== 向量化STA/LTA算法测试 ===");
        BenchmarkResult vectorizedResult = benchmarkVectorizedSTALTA();
        suite.addResult("Vectorized STA/LTA", vectorizedResult);
        
        // 3. 自适应STA/LTA算法测试
        System.out.println("\n=== 自适应STA/LTA算法测试 ===");
        BenchmarkResult adaptiveResult = benchmarkAdaptiveSTALTA();
        suite.addResult("Adaptive STA/LTA", adaptiveResult);
        
        // 4. 多通道融合检测测试
        System.out.println("\n=== 多通道融合检测测试 ===");
        BenchmarkResult multiChannelResult = benchmarkMultiChannelDetection();
        suite.addResult("Multi-Channel Detection", multiChannelResult);
        
        // 5. 并发性能测试
        System.out.println("\n=== 并发性能测试 ===");
        BenchmarkResult concurrentResult = benchmarkConcurrentProcessing();
        suite.addResult("Concurrent Processing", concurrentResult);
        
        // 6. 内存使用测试
        System.out.println("\n=== 内存使用测试 ===");
        MemoryBenchmarkResult memoryResult = benchmarkMemoryUsage();
        suite.setMemoryResult(memoryResult);
        
        System.out.println("\n基准测试完成！");
        return suite;
    }
    
    /**
     * 基础STA/LTA算法基准测试
     */
    public BenchmarkResult benchmarkBasicSTALTA() {
        // 创建测试数据
        List<SeismicRecord> testData = generateTestData(1000, 250, 1000);
        
        // 预热
        for (int i = 0; i < warmupIterations; i++) {
            processWithBasicSTALTA(testData.get(i % testData.size()));
        }
        
        // 基准测试
        long startTime = System.nanoTime();
        long totalSamples = 0;
        int detectionCount = 0;
        
        for (int i = 0; i < benchmarkIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            STALTAResult result = processWithBasicSTALTA(record);
            
            totalSamples += record.getData().length;
            if (result.hasTriggers()) {
                detectionCount++;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;
        
        return new BenchmarkResult(
            "Basic STA/LTA",
            benchmarkIterations,
            totalSamples,
            totalTimeNs,
            detectionCount,
            calculateThroughput(totalSamples, totalTimeNs),
            calculateLatency(totalTimeNs, benchmarkIterations)
        );
    }
    
    /**
     * 向量化STA/LTA算法基准测试
     */
    public BenchmarkResult benchmarkVectorizedSTALTA() {
        List<SeismicRecord> testData = generateTestData(1000, 250, 1000);
        VectorizedSTALTA vectorizedAlgorithm = new VectorizedSTALTA();
        
        // 预热
        for (int i = 0; i < warmupIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            float[] ratios = vectorizedAlgorithm.computeSTALTA(record.getData(), 5, 50);
            int triggerCount = countTriggers(ratios, 3.0f);
        }
        
        // 基准测试
        long startTime = System.nanoTime();
        long totalSamples = 0;
        int detectionCount = 0;
        
        for (int i = 0; i < benchmarkIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            float[] ratios = vectorizedAlgorithm.computeSTALTA(record.getData(), 5, 50);
            int triggerCount = countTriggers(ratios, 3.0f);
            
            totalSamples += record.getData().length;
            detectionCount += triggerCount;
        }
        
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;
        
        vectorizedAlgorithm.shutdown();
        
        return new BenchmarkResult(
            "Vectorized STA/LTA",
            benchmarkIterations,
            totalSamples,
            totalTimeNs,
            detectionCount,
            calculateThroughput(totalSamples, totalTimeNs),
            calculateLatency(totalTimeNs, benchmarkIterations)
        );
    }
    
    /**
     * 自适应STA/LTA算法基准测试
     */
    public BenchmarkResult benchmarkAdaptiveSTALTA() {
        List<SeismicRecord> testData = generateTestData(1000, 250, 1000);
        AdaptiveSTALTA adaptiveAlgorithm = new AdaptiveSTALTA();
        
        // 预热
        for (int i = 0; i < warmupIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            adaptiveAlgorithm.computeAdaptive(record);
        }
        
        // 基准测试
        long startTime = System.nanoTime();
        long totalSamples = 0;
        int detectionCount = 0;
        
        for (int i = 0; i < benchmarkIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            AdaptiveSTALTA.AdaptiveSTALTAResult result = adaptiveAlgorithm.computeAdaptive(record);
            
            totalSamples += record.getData().length;
            if (!result.getEvents().isEmpty()) {
                detectionCount++;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;
        
        adaptiveAlgorithm.shutdown();
        
        return new BenchmarkResult(
            "Adaptive STA/LTA",
            benchmarkIterations,
            totalSamples,
            totalTimeNs,
            detectionCount,
            calculateThroughput(totalSamples, totalTimeNs),
            calculateLatency(totalTimeNs, benchmarkIterations)
        );
    }
    
    /**
     * 多通道融合检测基准测试
     */
    public BenchmarkResult benchmarkMultiChannelDetection() {
        MultiChannelDetector detector = new MultiChannelDetector();
        
        // 注册测试通道
        for (int i = 0; i < 10; i++) {
            detector.registerChannel("channel_" + i, i * 0.001, i * 0.001, i * 100.0);
        }
        
        List<SeismicRecord> testData = generateTestData(1000, 250, 1000);
        VectorizedSTALTA algorithm = new VectorizedSTALTA();
        
        // 预热
        for (int i = 0; i < warmupIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            float[] ratios = algorithm.computeSTALTA(record.getData(), 5, 50);
            int triggerCount = countTriggers(ratios, 3.0f);
        }
        
        // 基准测试
        long startTime = System.nanoTime();
        long totalSamples = 0;
        int detectionCount = 0;
        
        for (int i = 0; i < benchmarkIterations; i++) {
            SeismicRecord record = testData.get(i % testData.size());
            float[] ratios = algorithm.computeSTALTA(record.getData(), 5, 50);
            int triggerCount = countTriggers(ratios, 3.0f);
            
            totalSamples += record.getData().length;
            detectionCount += triggerCount;
        }
        
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;
        
        algorithm.cleanup();
        
        return new BenchmarkResult(
            "Multi-Channel Detection",
            benchmarkIterations,
            totalSamples,
            totalTimeNs,
            detectionCount,
            calculateThroughput(totalSamples, totalTimeNs),
            calculateLatency(totalTimeNs, benchmarkIterations)
        );
    }
    
    /**
     * 并发处理基准测试
     */
    public BenchmarkResult benchmarkConcurrentProcessing() {
        List<SeismicRecord> testData = generateTestData(1000, 250, 1000);
        int tasksPerThread = benchmarkIterations / threadCount;
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong totalSamples = new AtomicLong(0);
        AtomicLong detectionCount = new AtomicLong(0);
        
        long startTime = System.nanoTime();
        
        // 启动并发任务
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    VectorizedSTALTA algorithm = new VectorizedSTALTA();
                    
                    for (int i = 0; i < tasksPerThread; i++) {
                        SeismicRecord record = testData.get((threadId * tasksPerThread + i) % testData.size());
                        float[] ratios = algorithm.computeSTALTA(record.getData(), 5, 50);
                        int triggerCount = countTriggers(ratios, 3.0f);
                        
                        totalSamples.addAndGet(record.getData().length);
                        detectionCount.addAndGet(triggerCount);
                    }
                    
                    algorithm.shutdown();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;
        
        return new BenchmarkResult(
            "Concurrent Processing",
            benchmarkIterations,
            totalSamples.get(),
            totalTimeNs,
            (int) detectionCount.get(),
            calculateThroughput(totalSamples.get(), totalTimeNs),
            calculateLatency(totalTimeNs, benchmarkIterations)
        );
    }
    
    /**
     * 内存使用基准测试
     */
    public MemoryBenchmarkResult benchmarkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // 强制垃圾回收
        System.gc();
        Thread.yield();
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 创建大量对象进行测试
        List<SeismicRecord> testData = generateTestData(100, 250, 5000);
        VectorizedSTALTA algorithm = new VectorizedSTALTA();
        List<float[]> results = new ArrayList<>();
        
        long peakMemory = initialMemory;
        
        for (SeismicRecord record : testData) {
            float[] ratios = algorithm.computeSTALTA(record.getData(), 5, 50);
            results.add(ratios);
            
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            peakMemory = Math.max(peakMemory, currentMemory);
        }
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 清理并测量内存释放
        algorithm.shutdown();
        results.clear();
        System.gc();
        Thread.yield();
        
        long afterCleanupMemory = runtime.totalMemory() - runtime.freeMemory();
        
        return new MemoryBenchmarkResult(
            initialMemory,
            peakMemory,
            finalMemory,
            afterCleanupMemory,
            testData.size()
        );
    }
    
    /**
     * 基础STA/LTA处理（简化实现）
     */
    private STALTAResult processWithBasicSTALTA(SeismicRecord record) {
        float[] data = record.getData();
        int staLength = 5;
        int ltaLength = 50;
        float threshold = 3.0f;
        
        STALTAResult result = memoryPool.borrowSTALTAResult();
        result.setChannelId(record.getChannelId());
        result.setSampleCount(data.length);
        result.setStaLength(staLength);
        result.setLtaLength(ltaLength);
        result.setThreshold(threshold);
        
        if (data.length < ltaLength) {
            return result;
        }
        
        int resultLength = data.length - ltaLength + 1;
        result.resizeArrays(resultLength);
        
        float[] ratios = result.getRatios();
        boolean[] triggers = result.getTriggers();
        
        // 简化的STA/LTA计算
        for (int i = ltaLength - 1; i < data.length; i++) {
            // 计算STA
            float staSum = 0.0f;
            for (int j = i - staLength + 1; j <= i; j++) {
                staSum += data[j] * data[j];
            }
            float sta = staSum / staLength;
            
            // 计算LTA
            float ltaSum = 0.0f;
            for (int j = i - ltaLength + 1; j <= i; j++) {
                ltaSum += data[j] * data[j];
            }
            float lta = ltaSum / ltaLength;
            
            // 计算比值
            int resultIndex = i - ltaLength + 1;
            ratios[resultIndex] = lta > 0 ? sta / lta : 0.0f;
            triggers[resultIndex] = ratios[resultIndex] > threshold;
        }
        
        result.calculateStatistics();
        return result;
    }
    
    /**
     * 生成测试数据
     */
    private List<SeismicRecord> generateTestData(int count, int sampleRate, int samplesPerRecord) {
        List<SeismicRecord> testData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            SeismicRecord record = new SeismicRecord();
            record.setChannelId("test_channel_" + i);
            record.setSampleRate(sampleRate);
            record.setStartTime(System.currentTimeMillis());
            
            // 生成合成地震数据
            float[] data = generateSyntheticSeismicData(samplesPerRecord, i);
            record.setData(data);
            
            testData.add(record);
        }
        
        return testData;
    }
    
    /**
     * 生成合成地震数据
     */
    private float[] generateSyntheticSeismicData(int length, int seed) {
        Random rng = new Random(seed);
        float[] data = new float[length];
        
        // 基础噪声
        for (int i = 0; i < length; i++) {
            data[i] = (float) (rng.nextGaussian() * 0.1);
        }
        
        // 添加地震信号（10%概率）
        if (rng.nextFloat() < 0.1) {
            int eventStart = rng.nextInt(length / 2);
            int eventLength = 50 + rng.nextInt(100);
            float amplitude = 0.5f + rng.nextFloat() * 1.5f;
            
            for (int i = eventStart; i < Math.min(eventStart + eventLength, length); i++) {
                float t = (float) (i - eventStart) / eventLength;
                float envelope = (float) Math.exp(-t * 3) * amplitude;
                data[i] += envelope * (float) Math.sin(2 * Math.PI * 10 * t);
            }
        }
        
        return data;
    }
    
    /**
     * 计算吞吐量（样本/秒）
     */
    private double calculateThroughput(long samples, long timeNs) {
        return samples * 1_000_000_000.0 / timeNs;
    }
    
    /**
     * 计算平均延迟（毫秒）
     */
    private double calculateLatency(long totalTimeNs, int iterations) {
        return (totalTimeNs / 1_000_000.0) / iterations;
    }
    
    /**
     * 关闭基准测试器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        memoryPool.cleanup();
    }
    
    /**
     * 计算触发器数量
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
     * 主方法 - 运行完整的基准测试套件
     */
    public static void main(String[] args) {
        System.out.println("=== DAS-Flink 算法性能基准测试 ===");
        System.out.println("开始运行性能基准测试...");
        
        AlgorithmBenchmark benchmark = new AlgorithmBenchmark();
        
        try {
            // 运行完整的基准测试套件
            BenchmarkSuite suite = benchmark.runFullBenchmark();
            
            // 打印结果摘要
            suite.printSummary();
            
        } catch (Exception e) {
            System.err.println("基准测试执行失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            benchmark.shutdown();
        }
        
        System.out.println("基准测试完成!");
    }
    
    /**
     * 基准测试结果类
     */
    public static class BenchmarkResult {
        private final String algorithmName;
        private final int iterations;
        private final long totalSamples;
        private final long totalTimeNs;
        private final int detectionCount;
        private final double throughputSamplesPerSecond;
        private final double averageLatencyMs;
        
        public BenchmarkResult(String algorithmName, int iterations, long totalSamples, 
                             long totalTimeNs, int detectionCount, 
                             double throughputSamplesPerSecond, double averageLatencyMs) {
            this.algorithmName = algorithmName;
            this.iterations = iterations;
            this.totalSamples = totalSamples;
            this.totalTimeNs = totalTimeNs;
            this.detectionCount = detectionCount;
            this.throughputSamplesPerSecond = throughputSamplesPerSecond;
            this.averageLatencyMs = averageLatencyMs;
        }
        
        public double getRealtimeFactor(float sampleRate) {
            double dataTimeSeconds = totalSamples / (double) sampleRate;
            double processingTimeSeconds = totalTimeNs / 1_000_000_000.0;
            return dataTimeSeconds / processingTimeSeconds;
        }
        
        // Getters
        public String getAlgorithmName() { return algorithmName; }
        public int getIterations() { return iterations; }
        public long getTotalSamples() { return totalSamples; }
        public long getTotalTimeNs() { return totalTimeNs; }
        public int getDetectionCount() { return detectionCount; }
        public double getThroughputSamplesPerSecond() { return throughputSamplesPerSecond; }
        public double getAverageLatencyMs() { return averageLatencyMs; }
        
        @Override
        public String toString() {
            return String.format(
                "%s: %.0f samples/s, %.2fms latency, %d detections, %.2fx realtime",
                algorithmName, throughputSamplesPerSecond, averageLatencyMs, 
                detectionCount, getRealtimeFactor(250.0f)
            );
        }
    }
    
    /**
     * 内存基准测试结果类
     */
    public static class MemoryBenchmarkResult {
        private final long initialMemoryBytes;
        private final long peakMemoryBytes;
        private final long finalMemoryBytes;
        private final long afterCleanupMemoryBytes;
        private final int objectCount;
        
        public MemoryBenchmarkResult(long initialMemoryBytes, long peakMemoryBytes, 
                                   long finalMemoryBytes, long afterCleanupMemoryBytes, 
                                   int objectCount) {
            this.initialMemoryBytes = initialMemoryBytes;
            this.peakMemoryBytes = peakMemoryBytes;
            this.finalMemoryBytes = finalMemoryBytes;
            this.afterCleanupMemoryBytes = afterCleanupMemoryBytes;
            this.objectCount = objectCount;
        }
        
        public long getMemoryUsedBytes() {
            return peakMemoryBytes - initialMemoryBytes;
        }
        
        public long getMemoryPerObjectBytes() {
            return objectCount > 0 ? getMemoryUsedBytes() / objectCount : 0;
        }
        
        public double getMemoryUsedMB() {
            return getMemoryUsedBytes() / (1024.0 * 1024.0);
        }
        
        // Getters
        public long getInitialMemoryBytes() { return initialMemoryBytes; }
        public long getPeakMemoryBytes() { return peakMemoryBytes; }
        public long getFinalMemoryBytes() { return finalMemoryBytes; }
        public long getAfterCleanupMemoryBytes() { return afterCleanupMemoryBytes; }
        public int getObjectCount() { return objectCount; }
        
        @Override
        public String toString() {
            return String.format(
                "Memory: %.2fMB used, %d bytes/object, %.2fMB peak",
                getMemoryUsedMB(), getMemoryPerObjectBytes(), peakMemoryBytes / (1024.0 * 1024.0)
            );
        }
    }
    
    /**
     * 基准测试套件类
     */
    public static class BenchmarkSuite {
        private final Map<String, BenchmarkResult> results;
        private MemoryBenchmarkResult memoryResult;
        private final long timestamp;
        
        public BenchmarkSuite() {
            this.results = new LinkedHashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addResult(String name, BenchmarkResult result) {
            results.put(name, result);
        }
        
        public void setMemoryResult(MemoryBenchmarkResult memoryResult) {
            this.memoryResult = memoryResult;
        }
        
        public void printSummary() {
            System.out.println("\n=== 基准测试结果汇总 ===");
            System.out.println("测试时间: " + new Date(timestamp));
            System.out.println();
            
            for (Map.Entry<String, BenchmarkResult> entry : results.entrySet()) {
                System.out.println(entry.getValue());
            }
            
            if (memoryResult != null) {
                System.out.println(memoryResult);
            }
            
            // 性能对比
            System.out.println("\n=== 性能对比 ===");
            BenchmarkResult baseline = results.get("Basic STA/LTA");
            if (baseline != null) {
                for (Map.Entry<String, BenchmarkResult> entry : results.entrySet()) {
                    if (!entry.getKey().equals("Basic STA/LTA")) {
                        BenchmarkResult result = entry.getValue();
                        double speedup = result.getThroughputSamplesPerSecond() / baseline.getThroughputSamplesPerSecond();
                        System.out.printf("%s: %.2fx speedup\n", entry.getKey(), speedup);
                    }
                }
            }
        }
        
        public Map<String, BenchmarkResult> getResults() { return results; }
        public MemoryBenchmarkResult getMemoryResult() { return memoryResult; }
        public long getTimestamp() { return timestamp; }
    }
}
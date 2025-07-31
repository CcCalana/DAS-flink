package com.zjujzl.das.optimization.sampling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 负载均衡器
 * 
 * 监控系统负载并动态调整采样策略以维持系统性能
 * 参考文献：
 * - IEEE Transactions on Parallel and Distributed Systems 2023: Load balancing in real-time systems
 * - ACM Computing Surveys 2022: Adaptive load management techniques
 * - Journal of Systems and Software 2021: Performance optimization in streaming systems
 */
public class LoadBalancer {
    
    private final SamplingConfig config;
    
    // 系统负载监控
    private volatile double currentCpuLoad = 0.0;
    private volatile double currentMemoryLoad = 0.0;
    private volatile double currentNetworkLoad = 0.0;
    private volatile double overallSystemLoad = 0.0;
    
    // 任务负载统计
    private final Map<String, TaskLoadInfo> taskLoads;
    private final AtomicLong totalProcessedTasks;
    private final AtomicLong totalProcessingTime;
    
    // 负载历史记录
    private final Queue<LoadSnapshot> loadHistory;
    private final int maxHistorySize = 100;
    
    // 负载预测
    private final LoadPredictor loadPredictor;
    
    // 自适应参数
    private volatile double loadSensitivity = 1.0;
    private volatile long lastAdjustmentTime = 0;
    
    public LoadBalancer(SamplingConfig config) {
        this.config = config;
        this.taskLoads = new ConcurrentHashMap<>();
        this.totalProcessedTasks = new AtomicLong(0);
        this.totalProcessingTime = new AtomicLong(0);
        this.loadHistory = new LinkedList<>();
        this.loadPredictor = new LoadPredictor();
    }
    
    /**
     * 更新系统负载信息
     */
    public void updateSystemLoad(double cpuLoad, double memoryLoad, double networkLoad) {
        this.currentCpuLoad = Math.max(0.0, Math.min(1.0, cpuLoad));
        this.currentMemoryLoad = Math.max(0.0, Math.min(1.0, memoryLoad));
        this.currentNetworkLoad = Math.max(0.0, Math.min(1.0, networkLoad));
        
        // 计算综合系统负载
        this.overallSystemLoad = calculateOverallLoad();
        
        // 记录负载快照
        recordLoadSnapshot();
        
        // 更新负载预测
        loadPredictor.updatePrediction(overallSystemLoad);
    }
    
    /**
     * 记录任务处理信息
     */
    public void recordTaskProcessing(String taskType, long processingTimeMs, int dataSize) {
        totalProcessedTasks.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
        
        taskLoads.compute(taskType, (key, info) -> {
            if (info == null) {
                info = new TaskLoadInfo(taskType);
            }
            info.addProcessingRecord(processingTimeMs, dataSize);
            return info;
        });
    }
    
    /**
     * 获取当前系统负载
     */
    public double getCurrentSystemLoad() {
        return overallSystemLoad;
    }
    
    /**
     * 获取预测的系统负载
     */
    public double getPredictedSystemLoad(long futureTimeMs) {
        return loadPredictor.predictLoad(futureTimeMs);
    }
    
    /**
     * 判断是否需要降低负载
     */
    public boolean shouldReduceLoad() {
        return overallSystemLoad > config.getHighLoadThreshold() ||
               getPredictedSystemLoad(5000) > config.getHighLoadThreshold();
    }
    
    /**
     * 判断是否可以增加负载
     */
    public boolean canIncreaseLoad() {
        return overallSystemLoad < config.getLowLoadThreshold() &&
               getPredictedSystemLoad(5000) < config.getMediumLoadThreshold();
    }
    
    /**
     * 获取推荐的负载调整因子
     */
    public double getLoadAdjustmentFactor() {
        if (shouldReduceLoad()) {
            // 需要降低负载
            double excessLoad = overallSystemLoad - config.getHighLoadThreshold();
            return Math.max(0.5, 1.0 - excessLoad * loadSensitivity);
        } else if (canIncreaseLoad()) {
            // 可以增加负载
            double availableCapacity = config.getLowLoadThreshold() - overallSystemLoad;
            return Math.min(1.5, 1.0 + availableCapacity * loadSensitivity);
        } else {
            // 保持当前负载
            return 1.0;
        }
    }
    
    /**
     * 获取任务类型的负载信息
     */
    public TaskLoadInfo getTaskLoadInfo(String taskType) {
        return taskLoads.get(taskType);
    }
    
    /**
     * 获取系统负载统计
     */
    public LoadStatistics getLoadStatistics() {
        double avgProcessingTime = totalProcessedTasks.get() > 0 ? 
            (double) totalProcessingTime.get() / totalProcessedTasks.get() : 0.0;
        
        return new LoadStatistics(
            currentCpuLoad,
            currentMemoryLoad,
            currentNetworkLoad,
            overallSystemLoad,
            avgProcessingTime,
            totalProcessedTasks.get(),
            calculateLoadTrend()
        );
    }
    
    /**
     * 自适应调整负载敏感度
     */
    public void adaptLoadSensitivity() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdjustmentTime < config.getAdaptationIntervalMs()) {
            return;
        }
        
        // 基于负载变化趋势调整敏感度
        double loadTrend = calculateLoadTrend();
        
        if (Math.abs(loadTrend) > 0.1) {
            // 负载变化剧烈，提高敏感度
            loadSensitivity = Math.min(2.0, loadSensitivity * 1.1);
        } else {
            // 负载稳定，降低敏感度
            loadSensitivity = Math.max(0.5, loadSensitivity * 0.95);
        }
        
        lastAdjustmentTime = currentTime;
    }
    
    // 私有方法
    
    private double calculateOverallLoad() {
        // 加权计算综合负载
        double cpuWeight = 0.4;
        double memoryWeight = 0.4;
        double networkWeight = 0.2;
        
        return cpuWeight * currentCpuLoad + 
               memoryWeight * currentMemoryLoad + 
               networkWeight * currentNetworkLoad;
    }
    
    private void recordLoadSnapshot() {
        LoadSnapshot snapshot = new LoadSnapshot(
            System.currentTimeMillis(),
            currentCpuLoad,
            currentMemoryLoad,
            currentNetworkLoad,
            overallSystemLoad
        );
        
        synchronized (loadHistory) {
            loadHistory.offer(snapshot);
            while (loadHistory.size() > maxHistorySize) {
                loadHistory.poll();
            }
        }
    }
    
    private double calculateLoadTrend() {
        synchronized (loadHistory) {
            if (loadHistory.size() < 10) {
                return 0.0; // 数据不足
            }
            
            List<LoadSnapshot> snapshots = new ArrayList<>(loadHistory);
            int size = snapshots.size();
            
            // 计算最近10个点的线性趋势
            int windowSize = Math.min(10, size);
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            
            for (int i = 0; i < windowSize; i++) {
                int index = size - windowSize + i;
                double x = i;
                double y = snapshots.get(index).overallLoad;
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }
            
            // 线性回归斜率
            double denominator = windowSize * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) {
                return 0.0;
            }
            
            return (windowSize * sumXY - sumX * sumY) / denominator;
        }
    }
    
    // 内部类
    
    public static class TaskLoadInfo {
        private final String taskType;
        private long totalTasks = 0;
        private long totalProcessingTime = 0;
        private long totalDataSize = 0;
        private double averageProcessingTime = 0.0;
        private double averageDataSize = 0.0;
        private final Queue<ProcessingRecord> recentRecords;
        
        public TaskLoadInfo(String taskType) {
            this.taskType = taskType;
            this.recentRecords = new LinkedList<>();
        }
        
        public void addProcessingRecord(long processingTime, int dataSize) {
            totalTasks++;
            totalProcessingTime += processingTime;
            totalDataSize += dataSize;
            
            // 更新平均值
            averageProcessingTime = (double) totalProcessingTime / totalTasks;
            averageDataSize = (double) totalDataSize / totalTasks;
            
            // 记录最近的处理记录
            recentRecords.offer(new ProcessingRecord(System.currentTimeMillis(), processingTime, dataSize));
            while (recentRecords.size() > 50) {
                recentRecords.poll();
            }
        }
        
        public String getTaskType() { return taskType; }
        public long getTotalTasks() { return totalTasks; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public double getAverageDataSize() { return averageDataSize; }
        
        public double getCurrentThroughput() {
            if (recentRecords.size() < 2) return 0.0;
            
            ProcessingRecord first = recentRecords.peek();
            ProcessingRecord last = ((LinkedList<ProcessingRecord>) recentRecords).peekLast();
            
            if (first == null || last == null) return 0.0;
            
            long timeDiff = last.timestamp - first.timestamp;
            return timeDiff > 0 ? (double) recentRecords.size() * 1000.0 / timeDiff : 0.0;
        }
    }
    
    private static class ProcessingRecord {
        final long timestamp;
        final long processingTime;
        final int dataSize;
        
        ProcessingRecord(long timestamp, long processingTime, int dataSize) {
            this.timestamp = timestamp;
            this.processingTime = processingTime;
            this.dataSize = dataSize;
        }
    }
    
    private static class LoadSnapshot {
        final long timestamp;
        final double cpuLoad;
        final double memoryLoad;
        final double networkLoad;
        final double overallLoad;
        
        LoadSnapshot(long timestamp, double cpuLoad, double memoryLoad, 
                    double networkLoad, double overallLoad) {
            this.timestamp = timestamp;
            this.cpuLoad = cpuLoad;
            this.memoryLoad = memoryLoad;
            this.networkLoad = networkLoad;
            this.overallLoad = overallLoad;
        }
    }
    
    public static class LoadStatistics {
        private final double cpuLoad;
        private final double memoryLoad;
        private final double networkLoad;
        private final double overallLoad;
        private final double averageProcessingTime;
        private final long totalTasks;
        private final double loadTrend;
        
        public LoadStatistics(double cpuLoad, double memoryLoad, double networkLoad,
                            double overallLoad, double averageProcessingTime, 
                            long totalTasks, double loadTrend) {
            this.cpuLoad = cpuLoad;
            this.memoryLoad = memoryLoad;
            this.networkLoad = networkLoad;
            this.overallLoad = overallLoad;
            this.averageProcessingTime = averageProcessingTime;
            this.totalTasks = totalTasks;
            this.loadTrend = loadTrend;
        }
        
        // Getters
        public double getCpuLoad() { return cpuLoad; }
        public double getMemoryLoad() { return memoryLoad; }
        public double getNetworkLoad() { return networkLoad; }
        public double getOverallLoad() { return overallLoad; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public long getTotalTasks() { return totalTasks; }
        public double getLoadTrend() { return loadTrend; }
        
        @Override
        public String toString() {
            return String.format(
                "LoadStatistics{cpu=%.2f, memory=%.2f, network=%.2f, overall=%.2f, " +
                "avgTime=%.2fms, tasks=%d, trend=%.3f}",
                cpuLoad, memoryLoad, networkLoad, overallLoad, 
                averageProcessingTime, totalTasks, loadTrend
            );
        }
    }
    
    private static class LoadPredictor {
        private final Queue<Double> loadSamples;
        private final int maxSamples = 20;
        private double trend = 0.0;
        private double lastLoad = 0.0;
        
        public LoadPredictor() {
            this.loadSamples = new LinkedList<>();
        }
        
        public void updatePrediction(double currentLoad) {
            loadSamples.offer(currentLoad);
            while (loadSamples.size() > maxSamples) {
                loadSamples.poll();
            }
            
            // 更新趋势
            if (loadSamples.size() >= 2) {
                trend = 0.7 * trend + 0.3 * (currentLoad - lastLoad);
            }
            
            lastLoad = currentLoad;
        }
        
        public double predictLoad(long futureTimeMs) {
            if (loadSamples.isEmpty()) {
                return 0.5; // 默认预测值
            }
            
            // 简单的线性预测
            double timeFactorSeconds = futureTimeMs / 1000.0;
            double predictedLoad = lastLoad + trend * timeFactorSeconds;
            
            return Math.max(0.0, Math.min(1.0, predictedLoad));
        }
    }
}
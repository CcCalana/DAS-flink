package com.zjujzl.das.model;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DAS性能指标类
 * 用于记录和分析DAS流处理系统的性能表现
 */
public class PerformanceMetrics {
    // 基础性能指标
    private long totalSamplesProcessed;
    private long totalProcessingTimeMs;
    private double averageLatencyMs;
    private double throughputSamplesPerSecond;
    private double peakThroughputSamplesPerSecond;
    
    // 检测性能指标
    private int totalEventsDetected;
    private double detectionAccuracy;
    private double falsePositiveRate;
    private double falseNegativeRate;
    private double averageEventConfidence;
    
    // 系统资源指标
    private double cpuUsagePercent;
    private double memoryUsageMB;
    private double networkBandwidthMbps;
    private double diskIORate;
    
    // 质量指标
    private double dataQualityScore;
    private double processingEfficiencyScore;
    private int droppedSamples;
    private int corruptedSamples;
    
    // 时间戳
    private Instant measurementStartTime;
    private Instant measurementEndTime;
    
    // 扩展指标
    private Map<String, Double> customMetrics;
    private List<Double> latencyDistribution;
    
    public PerformanceMetrics() {
        this.customMetrics = new HashMap<>();
        this.latencyDistribution = new ArrayList<>();
        this.measurementStartTime = Instant.now();
    }
    
    // Getters
    public long getTotalSamplesProcessed() { return totalSamplesProcessed; }
    public long getTotalProcessingTimeMs() { return totalProcessingTimeMs; }
    public double getAverageLatencyMs() { return averageLatencyMs; }
    public double getThroughputSamplesPerSecond() { return throughputSamplesPerSecond; }
    public double getPeakThroughputSamplesPerSecond() { return peakThroughputSamplesPerSecond; }
    public int getTotalEventsDetected() { return totalEventsDetected; }
    public double getDetectionAccuracy() { return detectionAccuracy; }
    public double getFalsePositiveRate() { return falsePositiveRate; }
    public double getFalseNegativeRate() { return falseNegativeRate; }
    public double getAverageEventConfidence() { return averageEventConfidence; }
    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public double getMemoryUsageMB() { return memoryUsageMB; }
    public double getNetworkBandwidthMbps() { return networkBandwidthMbps; }
    public double getDiskIORate() { return diskIORate; }
    public double getDataQualityScore() { return dataQualityScore; }
    public double getProcessingEfficiencyScore() { return processingEfficiencyScore; }
    public int getDroppedSamples() { return droppedSamples; }
    public int getCorruptedSamples() { return corruptedSamples; }
    public Instant getMeasurementStartTime() { return measurementStartTime; }
    public Instant getMeasurementEndTime() { return measurementEndTime; }
    public Map<String, Double> getCustomMetrics() { return customMetrics; }
    public List<Double> getLatencyDistribution() { return latencyDistribution; }
    
    // Setters
    public void setTotalSamplesProcessed(long totalSamplesProcessed) { this.totalSamplesProcessed = totalSamplesProcessed; }
    public void setTotalProcessingTimeMs(long totalProcessingTimeMs) { this.totalProcessingTimeMs = totalProcessingTimeMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }
    public void setThroughputSamplesPerSecond(double throughputSamplesPerSecond) { this.throughputSamplesPerSecond = throughputSamplesPerSecond; }
    public void setPeakThroughputSamplesPerSecond(double peakThroughputSamplesPerSecond) { this.peakThroughputSamplesPerSecond = peakThroughputSamplesPerSecond; }
    public void setTotalEventsDetected(int totalEventsDetected) { this.totalEventsDetected = totalEventsDetected; }
    public void setDetectionAccuracy(double detectionAccuracy) { this.detectionAccuracy = detectionAccuracy; }
    public void setFalsePositiveRate(double falsePositiveRate) { this.falsePositiveRate = falsePositiveRate; }
    public void setFalseNegativeRate(double falseNegativeRate) { this.falseNegativeRate = falseNegativeRate; }
    public void setAverageEventConfidence(double averageEventConfidence) { this.averageEventConfidence = averageEventConfidence; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public void setMemoryUsageMB(double memoryUsageMB) { this.memoryUsageMB = memoryUsageMB; }
    public void setNetworkBandwidthMbps(double networkBandwidthMbps) { this.networkBandwidthMbps = networkBandwidthMbps; }
    public void setDiskIORate(double diskIORate) { this.diskIORate = diskIORate; }
    public void setDataQualityScore(double dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public void setProcessingEfficiencyScore(double processingEfficiencyScore) { this.processingEfficiencyScore = processingEfficiencyScore; }
    public void setDroppedSamples(int droppedSamples) { this.droppedSamples = droppedSamples; }
    public void setCorruptedSamples(int corruptedSamples) { this.corruptedSamples = corruptedSamples; }
    public void setMeasurementStartTime(Instant measurementStartTime) { this.measurementStartTime = measurementStartTime; }
    public void setMeasurementEndTime(Instant measurementEndTime) { this.measurementEndTime = measurementEndTime; }
    
    // 辅助方法
    public void addCustomMetric(String name, double value) {
        customMetrics.put(name, value);
    }
    
    public void addLatencyMeasurement(double latencyMs) {
        latencyDistribution.add(latencyMs);
    }
    
    public double calculateOverallScore() {
        double accuracyScore = detectionAccuracy * 0.3;
        double performanceScore = Math.min(throughputSamplesPerSecond / 10000.0, 1.0) * 0.3;
        double qualityScore = dataQualityScore * 0.2;
        double efficiencyScore = processingEfficiencyScore * 0.2;
        
        return accuracyScore + performanceScore + qualityScore + efficiencyScore;
    }
    
    public long getMeasurementDurationMs() {
        if (measurementEndTime != null && measurementStartTime != null) {
            return measurementEndTime.toEpochMilli() - measurementStartTime.toEpochMilli();
        }
        return 0;
    }
    
    public double getLatencyPercentile(double percentile) {
        if (latencyDistribution.isEmpty()) return 0.0;
        
        List<Double> sorted = new ArrayList<>(latencyDistribution);
        Collections.sort(sorted);
        
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        
        return sorted.get(index);
    }
    
    public void finalizeMeasurement() {
        this.measurementEndTime = Instant.now();
        
        // 计算平均延迟
        if (!latencyDistribution.isEmpty()) {
            this.averageLatencyMs = latencyDistribution.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        }
        
        // 计算处理效率分数
        if (totalProcessingTimeMs > 0 && totalSamplesProcessed > 0) {
            double samplesPerMs = (double) totalSamplesProcessed / totalProcessingTimeMs;
            this.processingEfficiencyScore = Math.min(samplesPerMs / 100.0, 1.0);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{samples=%d, latency=%.2fms, throughput=%.2f/s, accuracy=%.2f%%, score=%.2f}",
            totalSamplesProcessed, averageLatencyMs, throughputSamplesPerSecond, 
            detectionAccuracy * 100, calculateOverallScore()
        );
    }
}
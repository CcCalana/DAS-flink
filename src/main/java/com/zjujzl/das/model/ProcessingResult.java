package com.zjujzl.das.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理结果类
 * 用于表示DAS数据处理的结果
 */
public class ProcessingResult {
    public enum BenchmarkMethod {
        TRADITIONAL_STA_LTA,           // 传统STA/LTA方法
        BASIC_STREAM_PROCESSING,       // 基础流处理
        OPTIMIZED_STREAM_PROCESSING    // 优化流处理框架
    }
    
    private BenchmarkMethod method;
    private DASStreamData inputData;
    private List<DetectedEvent> detectedEvents;
    private Instant startTime;
    private Instant endTime;
    private long processingTimeMs;
    private double qualityScore;
    private String error;
    private Map<String, Double> componentLatencies;
    private Map<String, Object> processingMetadata;
    private double cpuUsage;
    private double memoryUsage;
    private double throughput;
    private boolean processingSuccess;
    
    public ProcessingResult() {
        this.detectedEvents = new ArrayList<>();
        this.componentLatencies = new HashMap<>();
        this.processingMetadata = new HashMap<>();
        this.startTime = Instant.now();
        this.endTime = Instant.now();
        this.processingTimeMs = 0;
        this.qualityScore = 0.0;
        this.cpuUsage = 0.0;
        this.memoryUsage = 0.0;
        this.throughput = 0.0;
        this.processingSuccess = false;
    }
    
    public ProcessingResult(BenchmarkMethod method, DASStreamData inputData) {
        this();
        this.method = method;
        this.inputData = inputData;
    }
    
    // Getters and Setters
    public BenchmarkMethod getMethod() { return method; }
    public void setMethod(BenchmarkMethod method) { this.method = method; }
    
    public DASStreamData getInputData() { return inputData; }
    public void setInputData(DASStreamData inputData) { this.inputData = inputData; }
    
    public List<DetectedEvent> getDetectedEvents() { return new ArrayList<>(detectedEvents); }
    public void setDetectedEvents(List<DetectedEvent> detectedEvents) { 
        this.detectedEvents = new ArrayList<>(detectedEvents); 
    }
    public void addDetectedEvent(DetectedEvent event) { this.detectedEvents.add(event); }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public Map<String, Double> getComponentLatencies() { return new HashMap<>(componentLatencies); }
    public void setComponentLatencies(Map<String, Double> componentLatencies) { 
        this.componentLatencies = new HashMap<>(componentLatencies); 
    }
    public void addComponentLatency(String component, double latency) {
        this.componentLatencies.put(component, latency);
    }
    
    public Map<String, Object> getProcessingMetadata() { return new HashMap<>(processingMetadata); }
    public void setProcessingMetadata(Map<String, Object> processingMetadata) { 
        this.processingMetadata = new HashMap<>(processingMetadata); 
    }
    public void addMetadata(String key, Object value) {
        this.processingMetadata.put(key, value);
    }
    
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public double getThroughput() { return throughput; }
    public void setThroughput(double throughput) { this.throughput = throughput; }
    
    public boolean isProcessingSuccess() { return processingSuccess; }
    public void setProcessingSuccess(boolean processingSuccess) { this.processingSuccess = processingSuccess; }
    
    // 计算处理延迟
    public long getProcessingLatencyMs() {
        if (startTime != null && endTime != null) {
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
        return processingTimeMs;
    }
    
    // 计算事件检测率
    public double getEventDetectionRate() {
        if (inputData == null) return 0.0;
        long durationMs = inputData.getDurationMs();
        if (durationMs <= 0) return 0.0;
        return (double) detectedEvents.size() / (durationMs / 1000.0); // 事件/秒
    }
    
    // 计算平均事件置信度
    public double getAverageEventConfidence() {
        if (detectedEvents.isEmpty()) return 0.0;
        return detectedEvents.stream()
                .mapToDouble(DetectedEvent::getConfidence)
                .average()
                .orElse(0.0);
    }
    
    // 获取高置信度事件数量
    public int getHighConfidenceEventCount(double threshold) {
        return (int) detectedEvents.stream()
                .filter(event -> event.getConfidence() >= threshold)
                .count();
    }
    
    // 计算处理效率分数
    public double getProcessingEfficiencyScore() {
        if (processingTimeMs <= 0 || inputData == null) return 0.0;
        
        double dataSize = inputData.getDataSizeGB();
        double processingTimeSeconds = processingTimeMs / 1000.0;
        double baseScore = dataSize / processingTimeSeconds; // GB/s
        
        // 考虑质量分数和成功率
        double qualityFactor = qualityScore;
        double successFactor = processingSuccess ? 1.0 : 0.5;
        
        return baseScore * qualityFactor * successFactor;
    }
    
    // 验证结果完整性
    public boolean isResultValid() {
        return processingSuccess && 
               error == null && 
               inputData != null && 
               processingTimeMs >= 0 && 
               qualityScore >= 0.0 && qualityScore <= 1.0;
    }
    
    // 创建错误结果
    public static ProcessingResult createErrorResult(BenchmarkMethod method, DASStreamData inputData, 
                                                   String errorMessage) {
        ProcessingResult result = new ProcessingResult(method, inputData);
        result.setError(errorMessage);
        result.setProcessingSuccess(false);
        result.setEndTime(Instant.now());
        return result;
    }
    
    // 创建成功结果
    public static ProcessingResult createSuccessResult(BenchmarkMethod method, DASStreamData inputData,
                                                      List<DetectedEvent> events, double qualityScore) {
        ProcessingResult result = new ProcessingResult(method, inputData);
        result.setDetectedEvents(events);
        result.setQualityScore(qualityScore);
        result.setProcessingSuccess(true);
        result.setEndTime(Instant.now());
        result.setProcessingTimeMs(result.getProcessingLatencyMs());
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("ProcessingResult{method=%s, events=%d, latency=%dms, " +
                           "quality=%.2f, success=%s, error=%s}",
                           method, detectedEvents.size(), getProcessingLatencyMs(),
                           qualityScore, processingSuccess, error);
    }
}
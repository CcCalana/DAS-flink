package com.das.flink.algorithm;

import java.util.Arrays;

/**
 * STA/LTA算法计算结果类
 * 存储短时平均/长时平均比值计算的结果和相关统计信息
 * 
 * @author DAS-Flink Team
 */
public class STALTAResult {
    
    private String channelId;
    private long timestamp;
    private int sampleCount;
    private float sampleRate;
    
    // STA/LTA参数
    private int staLength;
    private int ltaLength;
    private float threshold;
    
    // 计算结果
    private float[] ratios;           // STA/LTA比值数组
    private float[] staValues;        // STA值数组
    private float[] ltaValues;        // LTA值数组
    private boolean[] triggers;       // 触发标记数组
    
    // 统计信息
    private float maxRatio;
    private float minRatio;
    private float averageRatio;
    private float variance;
    private int maxRatioIndex;
    private int triggerCount;
    private long processingTimeNs;    // 处理耗时（纳秒）
    
    // 检测结果
    private DetectionEvent[] events;  // 检测到的事件
    private int eventCount;
    
    // 质量评估
    private float signalToNoiseRatio;
    private float confidence;
    private QualityLevel qualityLevel;
    
    public STALTAResult() {
        this.channelId = "";
        this.timestamp = System.currentTimeMillis();
        this.sampleCount = 0;
        this.sampleRate = 250.0f;
        this.staLength = 5;
        this.ltaLength = 50;
        this.threshold = 3.0f;
        this.ratios = new float[0];
        this.staValues = new float[0];
        this.ltaValues = new float[0];
        this.triggers = new boolean[0];
        this.maxRatio = 0.0f;
        this.minRatio = Float.MAX_VALUE;
        this.averageRatio = 0.0f;
        this.variance = 0.0f;
        this.maxRatioIndex = -1;
        this.triggerCount = 0;
        this.processingTimeNs = 0L;
        this.events = new DetectionEvent[0];
        this.eventCount = 0;
        this.signalToNoiseRatio = 0.0f;
        this.confidence = 0.0f;
        this.qualityLevel = QualityLevel.LOW;
    }
    
    public STALTAResult(String channelId, int sampleCount, int staLength, int ltaLength) {
        this();
        this.channelId = channelId;
        this.sampleCount = sampleCount;
        this.staLength = staLength;
        this.ltaLength = ltaLength;
        
        // 初始化数组
        int resultLength = Math.max(0, sampleCount - ltaLength + 1);
        this.ratios = new float[resultLength];
        this.staValues = new float[resultLength];
        this.ltaValues = new float[resultLength];
        this.triggers = new boolean[resultLength];
    }
    
    /**
     * 重置结果对象（用于对象池）
     */
    public void reset() {
        this.channelId = "";
        this.timestamp = System.currentTimeMillis();
        this.sampleCount = 0;
        this.maxRatio = 0.0f;
        this.minRatio = Float.MAX_VALUE;
        this.averageRatio = 0.0f;
        this.variance = 0.0f;
        this.maxRatioIndex = -1;
        this.triggerCount = 0;
        this.processingTimeNs = 0L;
        this.eventCount = 0;
        this.signalToNoiseRatio = 0.0f;
        this.confidence = 0.0f;
        this.qualityLevel = QualityLevel.LOW;
        
        // 清空数组内容但保持引用
        if (ratios != null) Arrays.fill(ratios, 0.0f);
        if (staValues != null) Arrays.fill(staValues, 0.0f);
        if (ltaValues != null) Arrays.fill(ltaValues, 0.0f);
        if (triggers != null) Arrays.fill(triggers, false);
    }
    
    /**
     * 调整数组大小
     */
    public void resizeArrays(int newSize) {
        if (newSize <= 0) return;
        
        this.ratios = new float[newSize];
        this.staValues = new float[newSize];
        this.ltaValues = new float[newSize];
        this.triggers = new boolean[newSize];
    }
    
    /**
     * 计算统计信息
     */
    public void calculateStatistics() {
        if (ratios == null || ratios.length == 0) return;
        
        float sum = 0.0f;
        float sumSquares = 0.0f;
        this.maxRatio = Float.MIN_VALUE;
        this.minRatio = Float.MAX_VALUE;
        this.maxRatioIndex = -1;
        this.triggerCount = 0;
        
        for (int i = 0; i < ratios.length; i++) {
            float ratio = ratios[i];
            sum += ratio;
            sumSquares += ratio * ratio;
            
            if (ratio > maxRatio) {
                maxRatio = ratio;
                maxRatioIndex = i;
            }
            if (ratio < minRatio) {
                minRatio = ratio;
            }
            
            if (triggers != null && i < triggers.length && triggers[i]) {
                triggerCount++;
            }
        }
        
        this.averageRatio = sum / ratios.length;
        this.variance = (sumSquares / ratios.length) - (averageRatio * averageRatio);
        
        // 计算信噪比和置信度
        calculateQualityMetrics();
    }
    
    /**
     * 计算质量指标
     */
    private void calculateQualityMetrics() {
        if (ratios == null || ratios.length == 0) return;
        
        // 计算信噪比（基于方差和平均值）
        float noiseLevel = (float) Math.sqrt(variance);
        this.signalToNoiseRatio = noiseLevel > 0 ? averageRatio / noiseLevel : 0.0f;
        
        // 计算置信度（基于最大比值、触发数量和一致性）
        float ratioConfidence = Math.min(1.0f, maxRatio / (threshold * 2.0f));
        float triggerConfidence = Math.min(1.0f, triggerCount / (ratios.length * 0.1f));
        float consistencyConfidence = signalToNoiseRatio > 2.0f ? 1.0f : signalToNoiseRatio / 2.0f;
        
        this.confidence = (ratioConfidence * 0.4f + triggerConfidence * 0.3f + consistencyConfidence * 0.3f);
        
        // 确定质量等级
        if (confidence > 0.8f && maxRatio > threshold * 2.0f) {
            qualityLevel = QualityLevel.HIGH;
        } else if (confidence > 0.5f && maxRatio > threshold * 1.5f) {
            qualityLevel = QualityLevel.MEDIUM;
        } else {
            qualityLevel = QualityLevel.LOW;
        }
    }
    
    /**
     * 添加检测事件
     */
    public void addEvent(DetectionEvent event) {
        if (event == null) return;
        
        // 扩展事件数组
        if (events == null || eventCount >= events.length) {
            int newSize = events == null ? 4 : events.length * 2;
            DetectionEvent[] newEvents = new DetectionEvent[newSize];
            if (events != null) {
                System.arraycopy(events, 0, newEvents, 0, eventCount);
            }
            events = newEvents;
        }
        
        events[eventCount++] = event;
    }
    
    /**
     * 获取有效事件列表
     */
    public DetectionEvent[] getValidEvents() {
        if (events == null || eventCount == 0) {
            return new DetectionEvent[0];
        }
        
        // 统计有效事件数量
        int validCount = 0;
        for (int i = 0; i < eventCount; i++) {
            if (events[i] != null && events[i].isValid()) {
                validCount++;
            }
        }
        
        // 创建有效事件数组
        DetectionEvent[] validEvents = new DetectionEvent[validCount];
        int index = 0;
        for (int i = 0; i < eventCount; i++) {
            if (events[i] != null && events[i].isValid()) {
                validEvents[index++] = events[i];
            }
        }
        
        return validEvents;
    }
    
    /**
     * 获取处理性能指标
     */
    public PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(
            processingTimeNs,
            sampleCount,
            sampleRate,
            ratios != null ? ratios.length : 0
        );
    }
    
    /**
     * 检查是否有触发
     */
    public boolean hasTriggers() {
        return triggerCount > 0;
    }
    
    /**
     * 获取触发时间点（样本索引）
     */
    public int[] getTriggerIndices() {
        if (triggers == null || triggerCount == 0) {
            return new int[0];
        }
        
        int[] indices = new int[triggerCount];
        int index = 0;
        for (int i = 0; i < triggers.length; i++) {
            if (triggers[i]) {
                indices[index++] = i;
            }
        }
        
        return indices;
    }
    
    /**
     * 转换为JSON字符串（简化版）
     */
    public String toJson() {
        return String.format(
            "{\"channelId\":\"%s\",\"timestamp\":%d,\"sampleCount\":%d," +
            "\"maxRatio\":%.3f,\"averageRatio\":%.3f,\"triggerCount\":%d," +
            "\"confidence\":%.3f,\"qualityLevel\":\"%s\",\"eventCount\":%d,\"processingTimeNs\":%d}",
            channelId, timestamp, sampleCount, maxRatio, averageRatio, triggerCount,
            confidence, qualityLevel, eventCount, processingTimeNs
        );
    }
    
    // Getters and Setters
    
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
    
    public float getSampleRate() { return sampleRate; }
    public void setSampleRate(float sampleRate) { this.sampleRate = sampleRate; }
    
    public int getStaLength() { return staLength; }
    public void setStaLength(int staLength) { this.staLength = staLength; }
    
    public int getLtaLength() { return ltaLength; }
    public void setLtaLength(int ltaLength) { this.ltaLength = ltaLength; }
    
    public float getThreshold() { return threshold; }
    public void setThreshold(float threshold) { this.threshold = threshold; }
    
    public float[] getRatios() { return ratios; }
    public void setRatios(float[] ratios) { this.ratios = ratios; }
    
    public float[] getStaValues() { return staValues; }
    public void setStaValues(float[] staValues) { this.staValues = staValues; }
    
    public float[] getLtaValues() { return ltaValues; }
    public void setLtaValues(float[] ltaValues) { this.ltaValues = ltaValues; }
    
    public boolean[] getTriggers() { return triggers; }
    public void setTriggers(boolean[] triggers) { this.triggers = triggers; }
    
    public float getMaxRatio() { return maxRatio; }
    public void setMaxRatio(float maxRatio) { this.maxRatio = maxRatio; }
    
    public float getMinRatio() { return minRatio; }
    public void setMinRatio(float minRatio) { this.minRatio = minRatio; }
    
    public float getAverageRatio() { return averageRatio; }
    public void setAverageRatio(float averageRatio) { this.averageRatio = averageRatio; }
    
    public float getVariance() { return variance; }
    public void setVariance(float variance) { this.variance = variance; }
    
    public int getMaxRatioIndex() { return maxRatioIndex; }
    public void setMaxRatioIndex(int maxRatioIndex) { this.maxRatioIndex = maxRatioIndex; }
    
    public int getTriggerCount() { return triggerCount; }
    public void setTriggerCount(int triggerCount) { this.triggerCount = triggerCount; }
    
    public long getProcessingTimeNs() { return processingTimeNs; }
    public void setProcessingTimeNs(long processingTimeNs) { this.processingTimeNs = processingTimeNs; }
    
    public DetectionEvent[] getEvents() { return events; }
    public void setEvents(DetectionEvent[] events) { this.events = events; }
    
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    
    public float getSignalToNoiseRatio() { return signalToNoiseRatio; }
    public void setSignalToNoiseRatio(float signalToNoiseRatio) { this.signalToNoiseRatio = signalToNoiseRatio; }
    
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    
    public QualityLevel getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(QualityLevel qualityLevel) { this.qualityLevel = qualityLevel; }
    
    @Override
    public String toString() {
        return String.format(
            "STALTAResult{channel='%s', samples=%d, maxRatio=%.2f, triggers=%d, confidence=%.2f, quality=%s, events=%d}",
            channelId, sampleCount, maxRatio, triggerCount, confidence, qualityLevel, eventCount
        );
    }
    
    /**
     * 质量等级枚举
     */
    public enum QualityLevel {
        HIGH,    // 高质量：信号清晰，检测可靠
        MEDIUM,  // 中等质量：信号较好，检测基本可靠
        LOW      // 低质量：信号较差，检测不够可靠
    }
    
    /**
     * 性能指标内部类
     */
    public static class PerformanceMetrics {
        private final long processingTimeNs;
        private final int sampleCount;
        private final float sampleRate;
        private final int resultCount;
        
        public PerformanceMetrics(long processingTimeNs, int sampleCount, float sampleRate, int resultCount) {
            this.processingTimeNs = processingTimeNs;
            this.sampleCount = sampleCount;
            this.sampleRate = sampleRate;
            this.resultCount = resultCount;
        }
        
        public double getProcessingTimeMs() {
            return processingTimeNs / 1_000_000.0;
        }
        
        public double getThroughputSamplesPerSecond() {
            if (processingTimeNs <= 0) return 0.0;
            return sampleCount * 1_000_000_000.0 / processingTimeNs;
        }
        
        public double getRealtimeFactor() {
            if (sampleRate <= 0 || processingTimeNs <= 0) return 0.0;
            double dataTimeMs = sampleCount * 1000.0 / sampleRate;
            double processingTimeMs = processingTimeNs / 1_000_000.0;
            return dataTimeMs / processingTimeMs;
        }
        
        public long getProcessingTimeNs() { return processingTimeNs; }
        public int getSampleCount() { return sampleCount; }
        public float getSampleRate() { return sampleRate; }
        public int getResultCount() { return resultCount; }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceMetrics{processingTime=%.2fms, throughput=%.0f samples/s, realtimeFactor=%.2fx}",
                getProcessingTimeMs(), getThroughputSamplesPerSecond(), getRealtimeFactor()
            );
        }
    }
}
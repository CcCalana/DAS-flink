package com.das.flink.algorithm;

import java.util.Objects;

/**
 * 地震检测事件类
 * 存储STA/LTA算法检测到的地震事件信息
 * 
 * @author DAS-Flink Team
 */
public class DetectionEvent {
    
    private String eventId;
    private String channelId;
    private long startTime;
    private long endTime;
    private int duration; // 样本数
    private float maxRatio;
    private int maxRatioIndex;
    private float averageRatio;
    private float confidence;
    private EventType eventType;
    private EventQuality quality;
    private long timestamp;
    
    // 空间信息
    private double latitude;
    private double longitude;
    private float depth;
    
    // 频率特征
    private float dominantFrequency;
    private float bandwidth;
    
    // 统计信息
    private int triggerCount;
    private float[] ratioHistory;
    
    public DetectionEvent() {
        this.eventId = "";
        this.channelId = "";
        this.startTime = 0L;
        this.endTime = 0L;
        this.duration = 0;
        this.maxRatio = 0.0f;
        this.maxRatioIndex = -1;
        this.averageRatio = 0.0f;
        this.confidence = 0.0f;
        this.eventType = EventType.UNKNOWN;
        this.quality = EventQuality.LOW;
        this.timestamp = System.currentTimeMillis();
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.depth = 0.0f;
        this.dominantFrequency = 0.0f;
        this.bandwidth = 0.0f;
        this.triggerCount = 0;
        this.ratioHistory = new float[0];
    }
    
    public DetectionEvent(String eventId, String channelId, long startTime) {
        this();
        this.eventId = eventId;
        this.channelId = channelId;
        this.startTime = startTime;
    }
    
    /**
     * 重置事件对象（用于对象池）
     */
    public void reset() {
        this.eventId = "";
        this.channelId = "";
        this.startTime = 0L;
        this.endTime = 0L;
        this.duration = 0;
        this.maxRatio = 0.0f;
        this.maxRatioIndex = -1;
        this.averageRatio = 0.0f;
        this.confidence = 0.0f;
        this.eventType = EventType.UNKNOWN;
        this.quality = EventQuality.LOW;
        this.timestamp = System.currentTimeMillis();
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.depth = 0.0f;
        this.dominantFrequency = 0.0f;
        this.bandwidth = 0.0f;
        this.triggerCount = 0;
        this.ratioHistory = new float[0];
    }
    
    /**
     * 计算事件持续时间（毫秒）
     */
    public long getDurationMs() {
        return endTime - startTime;
    }
    
    /**
     * 计算事件强度分数
     */
    public float getIntensityScore() {
        // 综合考虑最大比值、持续时间和置信度
        float ratioScore = Math.min(1.0f, maxRatio / 10.0f); // 归一化到0-1
        float durationScore = Math.min(1.0f, duration / 100.0f); // 归一化到0-1
        
        return (ratioScore * 0.5f + durationScore * 0.3f + confidence * 0.2f);
    }
    
    /**
     * 判断是否为有效事件
     */
    public boolean isValid() {
        return maxRatio > 1.5f && 
               duration > 2 && 
               getDurationMs() > 100 && // 至少100ms
               confidence > 0.3f;
    }
    
    /**
     * 更新事件质量评估
     */
    public void updateQuality() {
        float intensityScore = getIntensityScore();
        
        if (intensityScore > 0.8f && confidence > 0.8f) {
            quality = EventQuality.HIGH;
        } else if (intensityScore > 0.5f && confidence > 0.5f) {
            quality = EventQuality.MEDIUM;
        } else {
            quality = EventQuality.LOW;
        }
    }
    
    /**
     * 设置比值历史记录
     */
    public void setRatioHistory(float[] ratios, int startIndex, int endIndex) {
        int length = endIndex - startIndex + 1;
        this.ratioHistory = new float[length];
        System.arraycopy(ratios, startIndex, this.ratioHistory, 0, length);
        
        // 计算平均比值
        float sum = 0.0f;
        for (float ratio : this.ratioHistory) {
            sum += ratio;
        }
        this.averageRatio = sum / this.ratioHistory.length;
    }
    
    /**
     * 计算事件的峰值时间
     */
    public long getPeakTime() {
        if (maxRatioIndex >= 0 && ratioHistory.length > 0) {
            // 基于采样率估算峰值时间
            float sampleRate = 250.0f; // 默认采样率
            long offsetMs = (long) (maxRatioIndex * 1000.0f / sampleRate);
            return startTime + offsetMs;
        }
        return startTime + (endTime - startTime) / 2; // 中点时间
    }
    
    /**
     * 合并另一个检测事件（用于多通道融合）
     */
    public void mergeWith(DetectionEvent other) {
        if (other == null) return;
        
        // 扩展时间范围
        this.startTime = Math.min(this.startTime, other.startTime);
        this.endTime = Math.max(this.endTime, other.endTime);
        
        // 更新最大比值
        if (other.maxRatio > this.maxRatio) {
            this.maxRatio = other.maxRatio;
            this.maxRatioIndex = other.maxRatioIndex;
        }
        
        // 更新置信度（加权平均）
        this.confidence = (this.confidence + other.confidence) / 2.0f;
        
        // 增加触发计数
        this.triggerCount += other.triggerCount;
        
        // 更新质量
        updateQuality();
    }
    
    // Getters and Setters
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public float getMaxRatio() { return maxRatio; }
    public void setMaxRatio(float maxRatio) { this.maxRatio = maxRatio; }
    
    public int getMaxRatioIndex() { return maxRatioIndex; }
    public void setMaxRatioIndex(int maxRatioIndex) { this.maxRatioIndex = maxRatioIndex; }
    
    public float getAverageRatio() { return averageRatio; }
    public void setAverageRatio(float averageRatio) { this.averageRatio = averageRatio; }
    
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public EventQuality getQuality() { return quality; }
    public void setQuality(EventQuality quality) { this.quality = quality; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public float getDepth() { return depth; }
    public void setDepth(float depth) { this.depth = depth; }
    
    public float getDominantFrequency() { return dominantFrequency; }
    public void setDominantFrequency(float dominantFrequency) { this.dominantFrequency = dominantFrequency; }
    
    public float getBandwidth() { return bandwidth; }
    public void setBandwidth(float bandwidth) { this.bandwidth = bandwidth; }
    
    public int getTriggerCount() { return triggerCount; }
    public void setTriggerCount(int triggerCount) { this.triggerCount = triggerCount; }
    
    public float[] getRatioHistory() { return ratioHistory; }
    public void setRatioHistory(float[] ratioHistory) { this.ratioHistory = ratioHistory; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectionEvent that = (DetectionEvent) o;
        return Objects.equals(eventId, that.eventId) &&
               Objects.equals(channelId, that.channelId) &&
               startTime == that.startTime;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, channelId, startTime);
    }
    
    @Override
    public String toString() {
        return String.format(
            "DetectionEvent{id='%s', channel='%s', time=%d-%d, maxRatio=%.2f, confidence=%.2f, quality=%s}",
            eventId, channelId, startTime, endTime, maxRatio, confidence, quality
        );
    }
    
    /**
     * 转换为JSON字符串（简化版）
     */
    public String toJson() {
        return String.format(
            "{\"eventId\":\"%s\",\"channelId\":\"%s\",\"startTime\":%d,\"endTime\":%d," +
            "\"maxRatio\":%.3f,\"confidence\":%.3f,\"quality\":\"%s\",\"type\":\"%s\"}",
            eventId, channelId, startTime, endTime, maxRatio, confidence, quality, eventType
        );
    }
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        EARTHQUAKE,     // 地震事件
        NOISE,          // 噪声事件
        CALIBRATION,    // 校准信号
        TELESEISMIC,    // 远震事件
        LOCAL,          // 本地事件
        REGIONAL,       // 区域事件
        UNKNOWN         // 未知类型
    }
    
    /**
     * 事件质量枚举
     */
    public enum EventQuality {
        HIGH,    // 高质量：信噪比高，特征明显
        MEDIUM,  // 中等质量：信噪比适中
        LOW      // 低质量：信噪比低，可能为噪声
    }
}
package com.zjujzl.das.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 检测到的地震事件类
 * 用于表示通过STA/LTA算法检测到的地震事件
 */
public class DetectedEvent {
    public enum EventType {
        EARTHQUAKE("地震"),
        NOISE("噪声"),
        TELESEISMIC("远震"),
        LOCAL("局部事件"),
        UNKNOWN("未知");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    private Instant startTime;
    private Instant endTime;
    private int channel;
    private double confidence;
    private EventType eventType;
    private Map<String, Object> attributes;
    private double amplitude;
    private double frequency;
    private double staLtaRatio;
    private double signalToNoiseRatio;
    private String detectionMethod;
    private double latitude;
    private double longitude;
    private double depth;
    private double magnitude;
    private boolean validated;
    
    public DetectedEvent() {
        this.startTime = Instant.now();
        this.endTime = Instant.now();
        this.channel = -1;
        this.confidence = 0.0;
        this.eventType = EventType.UNKNOWN;
        this.attributes = new HashMap<>();
        this.amplitude = 0.0;
        this.frequency = 0.0;
        this.staLtaRatio = 0.0;
        this.signalToNoiseRatio = 0.0;
        this.detectionMethod = "STA/LTA";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.depth = 0.0;
        this.magnitude = 0.0;
        this.validated = false;
    }
    
    public DetectedEvent(Instant startTime, Instant endTime, int channel, double confidence) {
        this();
        this.startTime = startTime;
        this.endTime = endTime;
        this.channel = channel;
        this.confidence = confidence;
    }
    
    public DetectedEvent(long startTimeMs, long endTimeMs, int channel, double confidence) {
        this(Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs), channel, confidence);
    }
    
    // Getters and Setters
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public int getChannel() { return channel; }
    public void setChannel(int channel) { this.channel = channel; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = Math.max(0.0, Math.min(1.0, confidence)); }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = new HashMap<>(attributes); }
    public void addAttribute(String key, Object value) { this.attributes.put(key, value); }
    public Object getAttribute(String key) { return attributes.get(key); }
    
    public double getAmplitude() { return amplitude; }
    public void setAmplitude(double amplitude) { this.amplitude = amplitude; }
    
    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
    
    public double getStaLtaRatio() { return staLtaRatio; }
    public void setStaLtaRatio(double staLtaRatio) { this.staLtaRatio = staLtaRatio; }
    
    public double getSignalToNoiseRatio() { return signalToNoiseRatio; }
    public void setSignalToNoiseRatio(double signalToNoiseRatio) { this.signalToNoiseRatio = signalToNoiseRatio; }
    
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }
    
    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }
    
    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }
    
    // 计算事件持续时间（毫秒）
    public long getDurationMs() {
        if (startTime == null || endTime == null) return 0;
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    // 计算事件持续时间（秒）
    public double getDurationSeconds() {
        return getDurationMs() / 1000.0;
    }
    
    // 检查事件是否有效
    public boolean isValid() {
        return startTime != null && 
               endTime != null && 
               startTime.isBefore(endTime) && 
               channel >= 0 && 
               confidence >= 0.0 && confidence <= 1.0;
    }
    
    // 检查是否为高置信度事件
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    // 检查事件是否与另一个事件重叠
    public boolean overlapsWith(DetectedEvent other) {
        if (other == null || this.channel != other.channel) return false;
        
        return !this.endTime.isBefore(other.startTime) && 
               !other.endTime.isBefore(this.startTime);
    }
    
    // 计算与另一个事件的重叠时间（毫秒）
    public long getOverlapMs(DetectedEvent other) {
        if (!overlapsWith(other)) return 0;
        
        Instant overlapStart = this.startTime.isAfter(other.startTime) ? this.startTime : other.startTime;
        Instant overlapEnd = this.endTime.isBefore(other.endTime) ? this.endTime : other.endTime;
        
        return overlapEnd.toEpochMilli() - overlapStart.toEpochMilli();
    }
    
    // 计算重叠比例
    public double getOverlapRatio(DetectedEvent other) {
        long overlapMs = getOverlapMs(other);
        if (overlapMs <= 0) return 0.0;
        
        long thisDuration = this.getDurationMs();
        long otherDuration = other.getDurationMs();
        long minDuration = Math.min(thisDuration, otherDuration);
        
        return minDuration > 0 ? (double) overlapMs / minDuration : 0.0;
    }
    
    // 合并两个事件（用于去重）
    public DetectedEvent mergeWith(DetectedEvent other) {
        if (other == null || this.channel != other.channel) return this;
        
        DetectedEvent merged = new DetectedEvent();
        merged.setChannel(this.channel);
        merged.setStartTime(this.startTime.isBefore(other.startTime) ? this.startTime : other.startTime);
        merged.setEndTime(this.endTime.isAfter(other.endTime) ? this.endTime : other.endTime);
        merged.setConfidence(Math.max(this.confidence, other.confidence));
        merged.setEventType(this.confidence >= other.confidence ? this.eventType : other.eventType);
        merged.setAmplitude(Math.max(this.amplitude, other.amplitude));
        merged.setStaLtaRatio(Math.max(this.staLtaRatio, other.staLtaRatio));
        merged.setSignalToNoiseRatio(Math.max(this.signalToNoiseRatio, other.signalToNoiseRatio));
        merged.setDetectionMethod(this.detectionMethod + "+" + other.detectionMethod);
        
        // 合并属性
        Map<String, Object> mergedAttributes = new HashMap<>(this.attributes);
        mergedAttributes.putAll(other.attributes);
        merged.setAttributes(mergedAttributes);
        
        return merged;
    }
    
    // 创建事件副本
    public DetectedEvent copy() {
        DetectedEvent copy = new DetectedEvent(startTime, endTime, channel, confidence);
        copy.setEventType(eventType);
        copy.setAttributes(attributes);
        copy.setAmplitude(amplitude);
        copy.setFrequency(frequency);
        copy.setStaLtaRatio(staLtaRatio);
        copy.setSignalToNoiseRatio(signalToNoiseRatio);
        copy.setDetectionMethod(detectionMethod);
        copy.setLatitude(latitude);
        copy.setLongitude(longitude);
        copy.setDepth(depth);
        copy.setMagnitude(magnitude);
        copy.setValidated(validated);
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DetectedEvent that = (DetectedEvent) obj;
        return channel == that.channel &&
               Double.compare(that.confidence, confidence) == 0 &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               eventType == that.eventType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, channel, confidence, eventType);
    }
    
    @Override
    public String toString() {
        return String.format("DetectedEvent{channel=%d, time=%s-%s, confidence=%.3f, " +
                           "type=%s, amplitude=%.2f, staLta=%.2f, validated=%s}",
                           channel, startTime, endTime, confidence, eventType,
                           amplitude, staLtaRatio, validated);
    }
}
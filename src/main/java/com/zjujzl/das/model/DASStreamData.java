package com.zjujzl.das.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DAS流数据结构
 * 用于表示分布式光纤声学传感器的实时数据流
 */
public class DASStreamData {
    private final int channelCount;
    private final double samplingRate; // Hz
    private final long timestamp;
    private final Instant startTime;
    private final Instant endTime;
    private final Map<Integer, double[]> channelData;
    private final Map<String, Object> metadata;
    private final double dataQuality;
    private final String dataSource;
    
    public DASStreamData() {
        this.channelCount = 0;
        this.samplingRate = 1000.0;
        this.timestamp = System.currentTimeMillis();
        this.startTime = Instant.now();
        this.endTime = Instant.now();
        this.channelData = new HashMap<>();
        this.metadata = new HashMap<>();
        this.dataQuality = 1.0;
        this.dataSource = "synthetic";
    }
    
    public DASStreamData(int channelCount, double samplingRate, Map<Integer, double[]> channelData) {
        this.channelCount = channelCount;
        this.samplingRate = samplingRate;
        this.timestamp = System.currentTimeMillis();
        this.startTime = Instant.now();
        this.endTime = Instant.now();
        this.channelData = new HashMap<>(channelData);
        this.metadata = new HashMap<>();
        this.dataQuality = 1.0;
        this.dataSource = "real";
    }
    
    public DASStreamData(int channelCount, double samplingRate, Map<Integer, double[]> channelData,
                        Instant startTime, Instant endTime, Map<String, Object> metadata,
                        double dataQuality, String dataSource) {
        this.channelCount = channelCount;
        this.samplingRate = samplingRate;
        this.timestamp = System.currentTimeMillis();
        this.startTime = startTime;
        this.endTime = endTime;
        this.channelData = new HashMap<>(channelData);
        this.metadata = new HashMap<>(metadata);
        this.dataQuality = dataQuality;
        this.dataSource = dataSource;
    }
    
    // Getters
    public int getChannelCount() { return channelCount; }
    public double getSamplingRate() { return samplingRate; }
    public long getTimestamp() { return timestamp; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Map<Integer, double[]> getChannelData() { return new HashMap<>(channelData); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public double getDataQuality() { return dataQuality; }
    public String getDataSource() { return dataSource; }
    
    // 获取指定通道的数据
    public double[] getChannelData(int channel) {
        return channelData.get(channel);
    }
    
    // 设置通道数据
    public void setChannelData(int channel, double[] data) {
        channelData.put(channel, data);
    }
    
    // 获取数据持续时间（毫秒）
    public long getDurationMs() {
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    // 获取数据大小估算（字节）
    public long getDataSize() {
        long totalSamples = 0;
        for (double[] data : channelData.values()) {
            totalSamples += data.length;
        }
        return totalSamples * 8; // 每个double 8字节
    }
    
    // 获取数据大小（GB）
    public double getDataSizeGB() {
        return getDataSize() / (1024.0 * 1024.0 * 1024.0);
    }
    
    // 获取数据传输速率（字节/秒）
    public double getDataRate() {
        long durationMs = getDurationMs();
        if (durationMs <= 0) return 0.0;
        return (double) getDataSize() / (durationMs / 1000.0);
    }
    
    // 获取数据传输速率（Mbps）
    public double getDataRateMbps() {
        return getDataRate() * 8.0 / (1024.0 * 1024.0);
    }
    
    // 添加元数据
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    // 获取元数据
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    // 检查数据完整性
    public boolean isDataComplete() {
        if (channelData.isEmpty()) return false;
        
        int expectedSamples = (int) (getDurationMs() * samplingRate / 1000.0);
        for (double[] data : channelData.values()) {
            if (data == null || data.length < expectedSamples * 0.95) {
                return false;
            }
        }
        return true;
    }
    
    // 创建数据副本
    public DASStreamData copy() {
        Map<Integer, double[]> copiedData = new HashMap<>();
        for (Map.Entry<Integer, double[]> entry : channelData.entrySet()) {
            double[] originalData = entry.getValue();
            double[] copiedArray = new double[originalData.length];
            System.arraycopy(originalData, 0, copiedArray, 0, originalData.length);
            copiedData.put(entry.getKey(), copiedArray);
        }
        
        return new DASStreamData(channelCount, samplingRate, copiedData,
                                startTime, endTime, metadata, dataQuality, dataSource);
    }
    
    @Override
    public String toString() {
        return String.format("DASStreamData{channels=%d, samplingRate=%.1fHz, duration=%dms, " +
                           "dataSize=%.2fMB, quality=%.2f, source=%s}",
                           channelCount, samplingRate, getDurationMs(),
                           getDataSize() / (1024.0 * 1024.0), dataQuality, dataSource);
    }
}
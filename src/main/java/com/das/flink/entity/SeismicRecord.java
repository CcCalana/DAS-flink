package com.das.flink.entity;

import java.util.Arrays;

/**
 * 地震记录实体类
 * 用于优化算法演示的地震数据结构
 * 
 * @author DAS-Flink Team
 */
public class SeismicRecord {
    
    private String channelId;
    private float sampleRate;
    private long startTime;
    private long endTime;
    private float[] data;
    private int dataLength;
    
    // DAS特有字段
    private int channelNumber;
    private double fiberDistance;
    private double gaugeLength;
    private double strainRate;
    
    // 地理位置信息
    private double latitude;
    private double longitude;
    private double elevation;
    
    // 元数据
    private String network;
    private String station;
    private String location;
    private String channel;
    
    public SeismicRecord() {
    }
    
    public SeismicRecord(String channelId, float sampleRate, float[] data) {
        this.channelId = channelId;
        this.sampleRate = sampleRate;
        this.data = data;
        this.dataLength = data != null ? data.length : 0;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + (long)(dataLength * 1000.0f / sampleRate);
    }
    
    // Getters and Setters
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public float getSampleRate() {
        return sampleRate;
    }
    
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public float[] getData() {
        return data;
    }
    
    public void setData(float[] data) {
        this.data = data;
        this.dataLength = data != null ? data.length : 0;
        if (this.sampleRate > 0 && this.dataLength > 0) {
            this.endTime = this.startTime + (long)(dataLength * 1000.0f / sampleRate);
        }
    }
    
    public int getDataLength() {
        return dataLength;
    }
    
    public int getChannelNumber() {
        return channelNumber;
    }
    
    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }
    
    public double getFiberDistance() {
        return fiberDistance;
    }
    
    public void setFiberDistance(double fiberDistance) {
        this.fiberDistance = fiberDistance;
    }
    
    public double getGaugeLength() {
        return gaugeLength;
    }
    
    public void setGaugeLength(double gaugeLength) {
        this.gaugeLength = gaugeLength;
    }
    
    public double getStrainRate() {
        return strainRate;
    }
    
    public void setStrainRate(double strainRate) {
        this.strainRate = strainRate;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getElevation() {
        return elevation;
    }
    
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    
    public String getNetwork() {
        return network;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }
    
    public String getStation() {
        return station;
    }
    
    public void setStation(String station) {
        this.station = station;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    /**
     * 获取持续时间（秒）
     */
    public double getDurationSeconds() {
        if (sampleRate > 0 && dataLength > 0) {
            return dataLength / (double) sampleRate;
        }
        return 0.0;
    }
    
    /**
     * 获取时间戳（毫秒）
     */
    public long getTimestamp() {
        return startTime;
    }
    
    /**
     * 检查数据有效性
     */
    public boolean isValid() {
        return channelId != null && 
               data != null && 
               data.length > 0 && 
               sampleRate > 0;
    }
    
    /**
     * 获取数据统计信息
     */
    public DataStatistics getStatistics() {
        if (data == null || data.length == 0) {
            return new DataStatistics(0, 0, 0, 0, 0);
        }
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        double sum = 0;
        double sumSquares = 0;
        
        for (float value : data) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
            sumSquares += value * value;
        }
        
        double mean = sum / data.length;
        double variance = (sumSquares / data.length) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, variance));
        
        return new DataStatistics(min, max, (float)mean, (float)stdDev, data.length);
    }
    
    @Override
    public String toString() {
        return String.format("SeismicRecord{channelId='%s', sampleRate=%.1f, dataLength=%d, duration=%.2fs}",
                channelId, sampleRate, dataLength, getDurationSeconds());
    }
    
    /**
     * 数据统计信息内部类
     */
    public static class DataStatistics {
        public final float min;
        public final float max;
        public final float mean;
        public final float stdDev;
        public final int count;
        
        public DataStatistics(float min, float max, float mean, float stdDev, int count) {
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.stdDev = stdDev;
            this.count = count;
        }
        
        @Override
        public String toString() {
            return String.format("Stats{min=%.3f, max=%.3f, mean=%.3f, stdDev=%.3f, count=%d}",
                    min, max, mean, stdDev, count);
        }
    }
}
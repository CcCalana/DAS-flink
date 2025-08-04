package com.zjujzl.das.model;

import java.util.*;

/**
 * DAS基准测试配置类
 * 定义基准测试的各种参数和设置
 */
public class BenchmarkConfig {
    // 基础配置
    // 通道数量
    private int channelCount = 1000;
    // 采样率 (Hz)
    private double samplingRate = 1000.0;
    // 测试持续时间 (毫秒)
    private long testDurationMs = 60000;
    // 处理窗口大小 (毫秒)
    private int windowSizeMs = 1000;
    // 批处理大小
    private int batchSize = 100;
    
    // 数据生成配置
    // 噪声水平
    private double noiseLevel = 0.1;
    // 信号幅度
    private double signalAmplitude = 1.0;
    // 事件频率 (每分钟)
    private int eventFrequency = 10;
    // 事件持续时间 (秒)
    private double eventDuration = 5.0;
    
    // 检测配置
    // STA窗口 (秒)
    private double staWindow = 1.0;
    // LTA窗口 (秒)
    private double ltaWindow = 30.0;
    // 检测阈值
    private double detectionThreshold = 3.0;
    // 质量阈值
    private double qualityThreshold = 0.8;
    
    // 性能配置
    // 最大并发线程数
    private int maxConcurrentThreads = 4;
    // 最大内存使用 (MB)
    private long maxMemoryMB = 1024;
    // 启用缓存
    private boolean enableCaching = true;
    // 启用压缩
    private boolean enableCompression = true;
    
    // 输出配置
    // 启用详细日志
    private boolean enableDetailedLogging = false;
    // 保存中间结果
    private boolean saveIntermediateResults = false;
    // 输出目录
    private String outputDirectory = "./benchmark_results";
    
    // 扩展配置
    private Map<String, Object> customParameters;
    
    public BenchmarkConfig() {
        this.customParameters = new HashMap<>();
    }
    
    // Getters
    public int getChannelCount() { return channelCount; }
    public double getSamplingRate() { return samplingRate; }
    public long getTestDurationMs() { return testDurationMs; }
    public int getWindowSizeMs() { return windowSizeMs; }
    public int getBatchSize() { return batchSize; }
    public double getNoiseLevel() { return noiseLevel; }
    public double getSignalAmplitude() { return signalAmplitude; }
    public int getEventFrequency() { return eventFrequency; }
    public double getEventDuration() { return eventDuration; }
    public double getStaWindow() { return staWindow; }
    public double getLtaWindow() { return ltaWindow; }
    public double getDetectionThreshold() { return detectionThreshold; }
    public double getQualityThreshold() { return qualityThreshold; }
    public int getMaxConcurrentThreads() { return maxConcurrentThreads; }
    public long getMaxMemoryMB() { return maxMemoryMB; }
    public boolean isEnableCaching() { return enableCaching; }
    public boolean isEnableCompression() { return enableCompression; }
    public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
    public boolean isSaveIntermediateResults() { return saveIntermediateResults; }
    public String getOutputDirectory() { return outputDirectory; }
    public Map<String, Object> getCustomParameters() { return customParameters; }
    
    // Setters
    public void setChannelCount(int channelCount) { this.channelCount = channelCount; }
    public void setSamplingRate(double samplingRate) { this.samplingRate = samplingRate; }
    public void setTestDurationMs(long testDurationMs) { this.testDurationMs = testDurationMs; }
    public void setWindowSizeMs(int windowSizeMs) { this.windowSizeMs = windowSizeMs; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public void setNoiseLevel(double noiseLevel) { this.noiseLevel = noiseLevel; }
    public void setSignalAmplitude(double signalAmplitude) { this.signalAmplitude = signalAmplitude; }
    public void setEventFrequency(int eventFrequency) { this.eventFrequency = eventFrequency; }
    public void setEventDuration(double eventDuration) { this.eventDuration = eventDuration; }
    public void setStaWindow(double staWindow) { this.staWindow = staWindow; }
    public void setLtaWindow(double ltaWindow) { this.ltaWindow = ltaWindow; }
    public void setDetectionThreshold(double detectionThreshold) { this.detectionThreshold = detectionThreshold; }
    public void setQualityThreshold(double qualityThreshold) { this.qualityThreshold = qualityThreshold; }
    public void setMaxConcurrentThreads(int maxConcurrentThreads) { this.maxConcurrentThreads = maxConcurrentThreads; }
    public void setMaxMemoryMB(long maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
    public void setEnableCaching(boolean enableCaching) { this.enableCaching = enableCaching; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
    public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
    public void setSaveIntermediateResults(boolean saveIntermediateResults) { this.saveIntermediateResults = saveIntermediateResults; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }
    
    // 辅助方法
    public void addCustomParameter(String key, Object value) {
        customParameters.put(key, value);
    }
    
    public Object getCustomParameter(String key) {
        return customParameters.get(key);
    }
    
    public long getTotalSamples() {
        return (long) (testDurationMs * samplingRate * channelCount / 1000.0);
    }
    
    public int getWindowSamples() {
        return (int) (windowSizeMs * samplingRate / 1000.0);
    }
    
    public int getStaSamples() {
        return (int) (staWindow * samplingRate);
    }
    
    public int getLtaSamples() {
        return (int) (ltaWindow * samplingRate);
    }
    
    public double getExpectedDataSizeGB() {
        // 假设每个样本8字节 (double)
        long totalBytes = getTotalSamples() * 8;
        return totalBytes / (1024.0 * 1024.0 * 1024.0);
    }
    
    public double getExpectedDataRateMbps() {
        double bytesPerSecond = channelCount * samplingRate * 8; // 8 bytes per double
        return bytesPerSecond * 8 / (1024.0 * 1024.0); // Convert to Mbps
    }
    
    public BenchmarkConfig copy() {
        BenchmarkConfig copy = new BenchmarkConfig();
        copy.channelCount = this.channelCount;
        copy.samplingRate = this.samplingRate;
        copy.testDurationMs = this.testDurationMs;
        copy.windowSizeMs = this.windowSizeMs;
        copy.batchSize = this.batchSize;
        copy.noiseLevel = this.noiseLevel;
        copy.signalAmplitude = this.signalAmplitude;
        copy.eventFrequency = this.eventFrequency;
        copy.eventDuration = this.eventDuration;
        copy.staWindow = this.staWindow;
        copy.ltaWindow = this.ltaWindow;
        copy.detectionThreshold = this.detectionThreshold;
        copy.qualityThreshold = this.qualityThreshold;
        copy.maxConcurrentThreads = this.maxConcurrentThreads;
        copy.maxMemoryMB = this.maxMemoryMB;
        copy.enableCaching = this.enableCaching;
        copy.enableCompression = this.enableCompression;
        copy.enableDetailedLogging = this.enableDetailedLogging;
        copy.saveIntermediateResults = this.saveIntermediateResults;
        copy.outputDirectory = this.outputDirectory;
        copy.customParameters = new HashMap<>(this.customParameters);
        return copy;
    }
    
    public void validate() throws IllegalArgumentException {
        if (channelCount <= 0) throw new IllegalArgumentException("Channel count must be positive");
        if (samplingRate <= 0) throw new IllegalArgumentException("Sampling rate must be positive");
        if (testDurationMs <= 0) throw new IllegalArgumentException("Test duration must be positive");
        if (windowSizeMs <= 0) throw new IllegalArgumentException("Window size must be positive");
        if (staWindow <= 0) throw new IllegalArgumentException("STA window must be positive");
        if (ltaWindow <= staWindow) throw new IllegalArgumentException("LTA window must be larger than STA window");
        if (detectionThreshold <= 1.0) throw new IllegalArgumentException("Detection threshold must be greater than 1.0");
        if (maxConcurrentThreads <= 0) throw new IllegalArgumentException("Max concurrent threads must be positive");
    }
    
    @Override
    public String toString() {
        return String.format(
            "BenchmarkConfig{channels=%d, rate=%.1fHz, duration=%dms, threshold=%.1f}",
            channelCount, samplingRate, testDurationMs, detectionThreshold
        );
    }
}
package com.zjujzl.das.optimization.compression;

/**
 * 压缩配置类
 * 
 * 定义压缩算法的全局配置参数
 */
public class CompressionConfig {
    
    // 稀疏性阈值配置
    private double waveletThreshold = 0.6;
    private double frequencyThreshold = 0.5;
    private double dictionaryThreshold = 0.7;
    
    // 信号长度阈值
    private int longSignalThreshold = 4096;
    private int shortSignalThreshold = 512;
    
    // 块大小配置
    private int largeBlockSize = 2048;
    private int smallBlockSize = 512;
    private int defaultBlockSize = 1024;
    
    // 字典学习配置
    private int dictionarySize = 256;
    private int maxDictionaryUpdates = 100;
    private double dictionaryLearningRate = 0.01;
    
    // 质量控制配置
    private double minQualityThreshold = 0.7;
    private double maxQualityThreshold = 0.95;
    private double defaultQualityThreshold = 0.8;
    
    // 压缩率配置
    private double minCompressionRatio = 0.1;
    private double maxCompressionRatio = 0.9;
    private double defaultCompressionRatio = 0.5;
    
    // 小波配置
    private int maxWaveletLevels = 6;
    private int minWaveletLevels = 2;
    private String defaultWaveletType = "db4";
    
    // 性能配置
    private int maxConcurrentCompressions = 4;
    private long compressionTimeoutMs = 5000;
    private boolean enableAdaptiveStrategy = true;
    
    // 默认构造函数
    public CompressionConfig() {
        // 使用默认值
    }
    
    // 构造函数，允许自定义主要参数
    public CompressionConfig(double waveletThreshold, double frequencyThreshold, 
                           double dictionaryThreshold, int dictionarySize) {
        this.waveletThreshold = waveletThreshold;
        this.frequencyThreshold = frequencyThreshold;
        this.dictionaryThreshold = dictionaryThreshold;
        this.dictionarySize = dictionarySize;
    }
    
    // Getter和Setter方法
    
    public double getWaveletThreshold() {
        return waveletThreshold;
    }
    
    public void setWaveletThreshold(double waveletThreshold) {
        this.waveletThreshold = Math.max(0.0, Math.min(1.0, waveletThreshold));
    }
    
    public double getFrequencyThreshold() {
        return frequencyThreshold;
    }
    
    public void setFrequencyThreshold(double frequencyThreshold) {
        this.frequencyThreshold = Math.max(0.0, Math.min(1.0, frequencyThreshold));
    }
    
    public double getDictionaryThreshold() {
        return dictionaryThreshold;
    }
    
    public void setDictionaryThreshold(double dictionaryThreshold) {
        this.dictionaryThreshold = Math.max(0.0, Math.min(1.0, dictionaryThreshold));
    }
    
    public int getLongSignalThreshold() {
        return longSignalThreshold;
    }
    
    public void setLongSignalThreshold(int longSignalThreshold) {
        this.longSignalThreshold = Math.max(1024, longSignalThreshold);
    }
    
    public int getShortSignalThreshold() {
        return shortSignalThreshold;
    }
    
    public void setShortSignalThreshold(int shortSignalThreshold) {
        this.shortSignalThreshold = Math.max(64, shortSignalThreshold);
    }
    
    public int getLargeBlockSize() {
        return largeBlockSize;
    }
    
    public void setLargeBlockSize(int largeBlockSize) {
        this.largeBlockSize = Math.max(512, largeBlockSize);
    }
    
    public int getSmallBlockSize() {
        return smallBlockSize;
    }
    
    public void setSmallBlockSize(int smallBlockSize) {
        this.smallBlockSize = Math.max(64, smallBlockSize);
    }
    
    public int getDefaultBlockSize() {
        return defaultBlockSize;
    }
    
    public void setDefaultBlockSize(int defaultBlockSize) {
        this.defaultBlockSize = Math.max(128, defaultBlockSize);
    }
    
    public int getDictionarySize() {
        return dictionarySize;
    }
    
    public void setDictionarySize(int dictionarySize) {
        this.dictionarySize = Math.max(64, Math.min(1024, dictionarySize));
    }
    
    public int getMaxDictionaryUpdates() {
        return maxDictionaryUpdates;
    }
    
    public void setMaxDictionaryUpdates(int maxDictionaryUpdates) {
        this.maxDictionaryUpdates = Math.max(10, maxDictionaryUpdates);
    }
    
    public double getDictionaryLearningRate() {
        return dictionaryLearningRate;
    }
    
    public void setDictionaryLearningRate(double dictionaryLearningRate) {
        this.dictionaryLearningRate = Math.max(0.001, Math.min(0.1, dictionaryLearningRate));
    }
    
    public double getMinQualityThreshold() {
        return minQualityThreshold;
    }
    
    public void setMinQualityThreshold(double minQualityThreshold) {
        this.minQualityThreshold = Math.max(0.0, Math.min(1.0, minQualityThreshold));
    }
    
    public double getMaxQualityThreshold() {
        return maxQualityThreshold;
    }
    
    public void setMaxQualityThreshold(double maxQualityThreshold) {
        this.maxQualityThreshold = Math.max(0.0, Math.min(1.0, maxQualityThreshold));
    }
    
    public double getDefaultQualityThreshold() {
        return defaultQualityThreshold;
    }
    
    public void setDefaultQualityThreshold(double defaultQualityThreshold) {
        this.defaultQualityThreshold = Math.max(0.0, Math.min(1.0, defaultQualityThreshold));
    }
    
    public double getMinCompressionRatio() {
        return minCompressionRatio;
    }
    
    public void setMinCompressionRatio(double minCompressionRatio) {
        this.minCompressionRatio = Math.max(0.01, Math.min(1.0, minCompressionRatio));
    }
    
    public double getMaxCompressionRatio() {
        return maxCompressionRatio;
    }
    
    public void setMaxCompressionRatio(double maxCompressionRatio) {
        this.maxCompressionRatio = Math.max(0.01, Math.min(1.0, maxCompressionRatio));
    }
    
    public double getDefaultCompressionRatio() {
        return defaultCompressionRatio;
    }
    
    public void setDefaultCompressionRatio(double defaultCompressionRatio) {
        this.defaultCompressionRatio = Math.max(0.01, Math.min(1.0, defaultCompressionRatio));
    }
    
    public int getMaxWaveletLevels() {
        return maxWaveletLevels;
    }
    
    public void setMaxWaveletLevels(int maxWaveletLevels) {
        this.maxWaveletLevels = Math.max(1, Math.min(8, maxWaveletLevels));
    }
    
    public int getMinWaveletLevels() {
        return minWaveletLevels;
    }
    
    public void setMinWaveletLevels(int minWaveletLevels) {
        this.minWaveletLevels = Math.max(1, Math.min(8, minWaveletLevels));
    }
    
    public String getDefaultWaveletType() {
        return defaultWaveletType;
    }
    
    public void setDefaultWaveletType(String defaultWaveletType) {
        this.defaultWaveletType = defaultWaveletType != null ? defaultWaveletType : "db4";
    }
    
    public int getMaxConcurrentCompressions() {
        return maxConcurrentCompressions;
    }
    
    public void setMaxConcurrentCompressions(int maxConcurrentCompressions) {
        this.maxConcurrentCompressions = Math.max(1, Math.min(16, maxConcurrentCompressions));
    }
    
    public long getCompressionTimeoutMs() {
        return compressionTimeoutMs;
    }
    
    public void setCompressionTimeoutMs(long compressionTimeoutMs) {
        this.compressionTimeoutMs = Math.max(1000, compressionTimeoutMs);
    }
    
    public boolean isEnableAdaptiveStrategy() {
        return enableAdaptiveStrategy;
    }
    
    public void setEnableAdaptiveStrategy(boolean enableAdaptiveStrategy) {
        this.enableAdaptiveStrategy = enableAdaptiveStrategy;
    }
    
    /**
     * 验证配置的有效性
     */
    public boolean isValid() {
        return waveletThreshold >= 0 && waveletThreshold <= 1 &&
               frequencyThreshold >= 0 && frequencyThreshold <= 1 &&
               dictionaryThreshold >= 0 && dictionaryThreshold <= 1 &&
               longSignalThreshold > shortSignalThreshold &&
               largeBlockSize > smallBlockSize &&
               dictionarySize > 0 &&
               minQualityThreshold <= maxQualityThreshold &&
               minCompressionRatio <= maxCompressionRatio &&
               minWaveletLevels <= maxWaveletLevels &&
               maxConcurrentCompressions > 0 &&
               compressionTimeoutMs > 0;
    }
    
    /**
     * 创建默认配置
     */
    public static CompressionConfig createDefault() {
        return new CompressionConfig();
    }
    
    /**
     * 创建高质量配置（低压缩率，高质量）
     */
    public static CompressionConfig createHighQuality() {
        CompressionConfig config = new CompressionConfig();
        config.setDefaultCompressionRatio(0.7);
        config.setDefaultQualityThreshold(0.9);
        config.setWaveletThreshold(0.7);
        config.setFrequencyThreshold(0.6);
        config.setDictionaryThreshold(0.8);
        return config;
    }
    
    /**
     * 创建高压缩配置（高压缩率，中等质量）
     */
    public static CompressionConfig createHighCompression() {
        CompressionConfig config = new CompressionConfig();
        config.setDefaultCompressionRatio(0.3);
        config.setDefaultQualityThreshold(0.7);
        config.setWaveletThreshold(0.5);
        config.setFrequencyThreshold(0.4);
        config.setDictionaryThreshold(0.6);
        return config;
    }
    
    /**
     * 创建快速配置（简单算法，快速处理）
     */
    public static CompressionConfig createFast() {
        CompressionConfig config = new CompressionConfig();
        config.setDefaultCompressionRatio(0.5);
        config.setDefaultQualityThreshold(0.75);
        config.setMaxWaveletLevels(3);
        config.setDictionarySize(128);
        config.setMaxDictionaryUpdates(50);
        config.setEnableAdaptiveStrategy(false);
        return config;
    }
    
    @Override
    public String toString() {
        return String.format("CompressionConfig{wavelet=%.2f, freq=%.2f, dict=%.2f, dictSize=%d, quality=%.2f, compression=%.2f}",
                waveletThreshold, frequencyThreshold, dictionaryThreshold, dictionarySize, 
                defaultQualityThreshold, defaultCompressionRatio);
    }
}
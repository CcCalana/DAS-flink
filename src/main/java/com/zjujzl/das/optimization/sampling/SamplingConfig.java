package com.zjujzl.das.optimization.sampling;

/**
 * 采样配置类
 * 
 * 定义自适应采样系统的全局配置参数
 * 包括重要性分析、采样策略、性能优化等相关配置
 */
public class SamplingConfig {
    
    // 重要性分析配置
    private double highImportanceThreshold = 0.8;
    private double mediumImportanceThreshold = 0.5;
    private double lowImportanceThreshold = 0.2;
    
    // 系统负载配置
    private double highLoadThreshold = 0.8;
    private double mediumLoadThreshold = 0.5;
    private double lowLoadThreshold = 0.2;
    
    // 历史数据配置
    private int maxHistorySize = 100;
    private int minSamplesForAdaptation = 10;
    
    // 特征权重配置
    private double energyWeight = 0.25;
    private double frequencyWeight = 0.20;
    private double temporalWeight = 0.20;
    private double statisticalWeight = 0.15;
    private double correlationWeight = 0.20;
    private double changeWeight = 0.15;
    private double abruptWeight = 0.25;
    
    // 参考值配置（用于归一化）
    private double referenceEnergy = 1000.0;
    private double referenceChangeRate = 10.0;
    private double referenceSnr = 10.0;
    
    // 频率分析配置
    private double minInterestFreq = 0.1;  // Hz
    private double maxInterestFreq = 50.0; // Hz
    
    // 采样质量配置
    private double minQualityThreshold = 0.3;
    private double defaultQualityThreshold = 0.8;
    private double maxQualityThreshold = 0.95;
    
    // 性能配置
    private int maxProcessingTimeMs = 1000;
    private double maxMemoryUsageRatio = 0.8;
    private int maxConcurrentTasks = 10;
    
    // 自适应配置
    private boolean enableAdaptiveAdjustment = true;
    private double adaptationSensitivity = 0.1;
    private long adaptationIntervalMs = 5000;
    
    // 事件检测配置
    private boolean enableEventDetection = true;
    private double eventDetectionThreshold = 2.0;
    private int eventWindowSize = 50;
    
    // 默认构造函数
    public SamplingConfig() {
        // 使用默认值
    }
    
    // 完整构造函数
    public SamplingConfig(double highImportanceThreshold, double mediumImportanceThreshold,
                         double highLoadThreshold, int maxHistorySize) {
        this.highImportanceThreshold = Math.max(0.0, Math.min(1.0, highImportanceThreshold));
        this.mediumImportanceThreshold = Math.max(0.0, Math.min(1.0, mediumImportanceThreshold));
        this.highLoadThreshold = Math.max(0.0, Math.min(1.0, highLoadThreshold));
        this.maxHistorySize = Math.max(1, maxHistorySize);
    }
    
    // Getters and Setters
    
    public double getHighImportanceThreshold() {
        return highImportanceThreshold;
    }
    
    public void setHighImportanceThreshold(double highImportanceThreshold) {
        this.highImportanceThreshold = Math.max(0.0, Math.min(1.0, highImportanceThreshold));
    }
    
    public double getMediumImportanceThreshold() {
        return mediumImportanceThreshold;
    }
    
    public void setMediumImportanceThreshold(double mediumImportanceThreshold) {
        this.mediumImportanceThreshold = Math.max(0.0, Math.min(1.0, mediumImportanceThreshold));
    }
    
    public double getLowImportanceThreshold() {
        return lowImportanceThreshold;
    }
    
    public void setLowImportanceThreshold(double lowImportanceThreshold) {
        this.lowImportanceThreshold = Math.max(0.0, Math.min(1.0, lowImportanceThreshold));
    }
    
    public double getHighLoadThreshold() {
        return highLoadThreshold;
    }
    
    public void setHighLoadThreshold(double highLoadThreshold) {
        this.highLoadThreshold = Math.max(0.0, Math.min(1.0, highLoadThreshold));
    }
    
    public double getMediumLoadThreshold() {
        return mediumLoadThreshold;
    }
    
    public void setMediumLoadThreshold(double mediumLoadThreshold) {
        this.mediumLoadThreshold = Math.max(0.0, Math.min(1.0, mediumLoadThreshold));
    }
    
    public double getLowLoadThreshold() {
        return lowLoadThreshold;
    }
    
    public void setLowLoadThreshold(double lowLoadThreshold) {
        this.lowLoadThreshold = Math.max(0.0, Math.min(1.0, lowLoadThreshold));
    }
    
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
    
    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = Math.max(1, maxHistorySize);
    }
    
    public int getMinSamplesForAdaptation() {
        return minSamplesForAdaptation;
    }
    
    public void setMinSamplesForAdaptation(int minSamplesForAdaptation) {
        this.minSamplesForAdaptation = Math.max(1, minSamplesForAdaptation);
    }
    
    // 特征权重 getters and setters
    
    public double getEnergyWeight() {
        return energyWeight;
    }
    
    public void setEnergyWeight(double energyWeight) {
        this.energyWeight = Math.max(0.0, energyWeight);
        normalizeWeights();
    }
    
    public double getFrequencyWeight() {
        return frequencyWeight;
    }
    
    public void setFrequencyWeight(double frequencyWeight) {
        this.frequencyWeight = Math.max(0.0, frequencyWeight);
        normalizeWeights();
    }
    
    public double getTemporalWeight() {
        return temporalWeight;
    }
    
    public void setTemporalWeight(double temporalWeight) {
        this.temporalWeight = Math.max(0.0, temporalWeight);
        normalizeWeights();
    }
    
    public double getStatisticalWeight() {
        return statisticalWeight;
    }
    
    public void setStatisticalWeight(double statisticalWeight) {
        this.statisticalWeight = Math.max(0.0, statisticalWeight);
        normalizeWeights();
    }
    
    public double getCorrelationWeight() {
        return correlationWeight;
    }
    
    public void setCorrelationWeight(double correlationWeight) {
        this.correlationWeight = Math.max(0.0, correlationWeight);
        normalizeWeights();
    }
    
    public double getChangeWeight() {
        return changeWeight;
    }
    
    public void setChangeWeight(double changeWeight) {
        this.changeWeight = Math.max(0.0, changeWeight);
    }
    
    public double getAbruptWeight() {
        return abruptWeight;
    }
    
    public void setAbruptWeight(double abruptWeight) {
        this.abruptWeight = Math.max(0.0, abruptWeight);
    }
    
    // 参考值 getters and setters
    
    public double getReferenceEnergy() {
        return referenceEnergy;
    }
    
    public void setReferenceEnergy(double referenceEnergy) {
        this.referenceEnergy = Math.max(1.0, referenceEnergy);
    }
    
    public double getReferenceChangeRate() {
        return referenceChangeRate;
    }
    
    public void setReferenceChangeRate(double referenceChangeRate) {
        this.referenceChangeRate = Math.max(0.1, referenceChangeRate);
    }
    
    public double getReferenceSnr() {
        return referenceSnr;
    }
    
    public void setReferenceSnr(double referenceSnr) {
        this.referenceSnr = Math.max(1.0, referenceSnr);
    }
    
    // 频率配置 getters and setters
    
    public double getMinInterestFreq() {
        return minInterestFreq;
    }
    
    public void setMinInterestFreq(double minInterestFreq) {
        this.minInterestFreq = Math.max(0.0, minInterestFreq);
    }
    
    public double getMaxInterestFreq() {
        return maxInterestFreq;
    }
    
    public void setMaxInterestFreq(double maxInterestFreq) {
        this.maxInterestFreq = Math.max(this.minInterestFreq, maxInterestFreq);
    }
    
    // 质量配置 getters and setters
    
    public double getMinQualityThreshold() {
        return minQualityThreshold;
    }
    
    public void setMinQualityThreshold(double minQualityThreshold) {
        this.minQualityThreshold = Math.max(0.0, Math.min(1.0, minQualityThreshold));
    }
    
    public double getDefaultQualityThreshold() {
        return defaultQualityThreshold;
    }
    
    public void setDefaultQualityThreshold(double defaultQualityThreshold) {
        this.defaultQualityThreshold = Math.max(0.0, Math.min(1.0, defaultQualityThreshold));
    }
    
    public double getMaxQualityThreshold() {
        return maxQualityThreshold;
    }
    
    public void setMaxQualityThreshold(double maxQualityThreshold) {
        this.maxQualityThreshold = Math.max(0.0, Math.min(1.0, maxQualityThreshold));
    }
    
    // 性能配置 getters and setters
    
    public int getMaxProcessingTimeMs() {
        return maxProcessingTimeMs;
    }
    
    public void setMaxProcessingTimeMs(int maxProcessingTimeMs) {
        this.maxProcessingTimeMs = Math.max(100, maxProcessingTimeMs);
    }
    
    public double getMaxMemoryUsageRatio() {
        return maxMemoryUsageRatio;
    }
    
    public void setMaxMemoryUsageRatio(double maxMemoryUsageRatio) {
        this.maxMemoryUsageRatio = Math.max(0.1, Math.min(1.0, maxMemoryUsageRatio));
    }
    
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
    
    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = Math.max(1, maxConcurrentTasks);
    }
    
    // 自适应配置 getters and setters
    
    public boolean isEnableAdaptiveAdjustment() {
        return enableAdaptiveAdjustment;
    }
    
    public void setEnableAdaptiveAdjustment(boolean enableAdaptiveAdjustment) {
        this.enableAdaptiveAdjustment = enableAdaptiveAdjustment;
    }
    
    public double getAdaptationSensitivity() {
        return adaptationSensitivity;
    }
    
    public void setAdaptationSensitivity(double adaptationSensitivity) {
        this.adaptationSensitivity = Math.max(0.01, Math.min(1.0, adaptationSensitivity));
    }
    
    public long getAdaptationIntervalMs() {
        return adaptationIntervalMs;
    }
    
    public void setAdaptationIntervalMs(long adaptationIntervalMs) {
        this.adaptationIntervalMs = Math.max(1000, adaptationIntervalMs);
    }
    
    // 事件检测配置 getters and setters
    
    public boolean isEnableEventDetection() {
        return enableEventDetection;
    }
    
    public void setEnableEventDetection(boolean enableEventDetection) {
        this.enableEventDetection = enableEventDetection;
    }
    
    public double getEventDetectionThreshold() {
        return eventDetectionThreshold;
    }
    
    public void setEventDetectionThreshold(double eventDetectionThreshold) {
        this.eventDetectionThreshold = Math.max(1.0, eventDetectionThreshold);
    }
    
    public int getEventWindowSize() {
        return eventWindowSize;
    }
    
    public void setEventWindowSize(int eventWindowSize) {
        this.eventWindowSize = Math.max(10, eventWindowSize);
    }
    
    // 工具方法
    
    /**
     * 归一化特征权重
     */
    private void normalizeWeights() {
        double totalWeight = energyWeight + frequencyWeight + temporalWeight + 
                           statisticalWeight + correlationWeight;
        
        if (totalWeight > 0) {
            energyWeight /= totalWeight;
            frequencyWeight /= totalWeight;
            temporalWeight /= totalWeight;
            statisticalWeight /= totalWeight;
            correlationWeight /= totalWeight;
        }
    }
    
    /**
     * 验证配置的有效性
     */
    public boolean isValid() {
        return highImportanceThreshold >= mediumImportanceThreshold &&
               mediumImportanceThreshold >= lowImportanceThreshold &&
               highLoadThreshold >= mediumLoadThreshold &&
               mediumLoadThreshold >= lowLoadThreshold &&
               maxInterestFreq >= minInterestFreq &&
               maxQualityThreshold >= defaultQualityThreshold &&
               defaultQualityThreshold >= minQualityThreshold &&
               maxHistorySize > 0 &&
               minSamplesForAdaptation > 0 &&
               maxProcessingTimeMs > 0 &&
               maxConcurrentTasks > 0;
    }
    
    /**
     * 创建预设配置
     */
    public static SamplingConfig createDefaultConfig() {
        return new SamplingConfig();
    }
    
    public static SamplingConfig createHighPerformanceConfig() {
        SamplingConfig config = new SamplingConfig();
        config.setHighImportanceThreshold(0.9);
        config.setMediumImportanceThreshold(0.7);
        config.setDefaultQualityThreshold(0.9);
        config.setMaxProcessingTimeMs(500);
        config.setAdaptationSensitivity(0.05);
        return config;
    }
    
    public static SamplingConfig createBalancedConfig() {
        SamplingConfig config = new SamplingConfig();
        config.setHighImportanceThreshold(0.8);
        config.setMediumImportanceThreshold(0.5);
        config.setDefaultQualityThreshold(0.8);
        config.setMaxProcessingTimeMs(1000);
        config.setAdaptationSensitivity(0.1);
        return config;
    }
    
    public static SamplingConfig createResourceConstrainedConfig() {
        SamplingConfig config = new SamplingConfig();
        config.setHighImportanceThreshold(0.7);
        config.setMediumImportanceThreshold(0.4);
        config.setDefaultQualityThreshold(0.6);
        config.setMaxProcessingTimeMs(2000);
        config.setMaxHistorySize(50);
        config.setAdaptationSensitivity(0.2);
        return config;
    }
    
    /**
     * 复制配置
     */
    public SamplingConfig copy() {
        SamplingConfig copy = new SamplingConfig();
        
        copy.highImportanceThreshold = this.highImportanceThreshold;
        copy.mediumImportanceThreshold = this.mediumImportanceThreshold;
        copy.lowImportanceThreshold = this.lowImportanceThreshold;
        
        copy.highLoadThreshold = this.highLoadThreshold;
        copy.mediumLoadThreshold = this.mediumLoadThreshold;
        copy.lowLoadThreshold = this.lowLoadThreshold;
        
        copy.maxHistorySize = this.maxHistorySize;
        copy.minSamplesForAdaptation = this.minSamplesForAdaptation;
        
        copy.energyWeight = this.energyWeight;
        copy.frequencyWeight = this.frequencyWeight;
        copy.temporalWeight = this.temporalWeight;
        copy.statisticalWeight = this.statisticalWeight;
        copy.correlationWeight = this.correlationWeight;
        copy.changeWeight = this.changeWeight;
        copy.abruptWeight = this.abruptWeight;
        
        copy.referenceEnergy = this.referenceEnergy;
        copy.referenceChangeRate = this.referenceChangeRate;
        copy.referenceSnr = this.referenceSnr;
        
        copy.minInterestFreq = this.minInterestFreq;
        copy.maxInterestFreq = this.maxInterestFreq;
        
        copy.minQualityThreshold = this.minQualityThreshold;
        copy.defaultQualityThreshold = this.defaultQualityThreshold;
        copy.maxQualityThreshold = this.maxQualityThreshold;
        
        copy.maxProcessingTimeMs = this.maxProcessingTimeMs;
        copy.maxMemoryUsageRatio = this.maxMemoryUsageRatio;
        copy.maxConcurrentTasks = this.maxConcurrentTasks;
        
        copy.enableAdaptiveAdjustment = this.enableAdaptiveAdjustment;
        copy.adaptationSensitivity = this.adaptationSensitivity;
        copy.adaptationIntervalMs = this.adaptationIntervalMs;
        
        copy.enableEventDetection = this.enableEventDetection;
        copy.eventDetectionThreshold = this.eventDetectionThreshold;
        copy.eventWindowSize = this.eventWindowSize;
        
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format(
            "SamplingConfig{importance=[%.2f,%.2f,%.2f], load=[%.2f,%.2f,%.2f], " +
            "quality=[%.2f,%.2f,%.2f], adaptive=%s, eventDetection=%s}",
            highImportanceThreshold, mediumImportanceThreshold, lowImportanceThreshold,
            highLoadThreshold, mediumLoadThreshold, lowLoadThreshold,
            minQualityThreshold, defaultQualityThreshold, maxQualityThreshold,
            enableAdaptiveAdjustment, enableEventDetection
        );
    }
}
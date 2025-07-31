package com.zjujzl.das.optimization.sampling;

/**
 * 采样策略配置
 * 
 * 定义具体的采样策略参数，包括采样方法、采样率、窗口大小等
 * 支持动态调整和策略优化
 */
public class SamplingStrategy {
    
    private SamplingMethod samplingMethod;
    private double samplingRatio;
    private int windowSize;
    private double qualityThreshold;
    private boolean adaptiveEnabled;
    private long lastUpdateTime;
    
    // 高级参数
    private double importanceThreshold;
    private int minSamples;
    private int maxSamples;
    private double smoothingFactor;
    private boolean eventDetectionEnabled;
    
    // 性能统计
    private double averageProcessingTime;
    private double averageCompressionRatio;
    private int usageCount;
    
    /**
     * 默认构造函数
     */
    public SamplingStrategy() {
        this.samplingMethod = SamplingMethod.ADAPTIVE_RATE;
        this.samplingRatio = 0.6;
        this.windowSize = 100;
        this.qualityThreshold = 0.8;
        this.adaptiveEnabled = true;
        this.lastUpdateTime = System.currentTimeMillis();
        
        // 高级参数默认值
        this.importanceThreshold = 0.5;
        this.minSamples = 10;
        this.maxSamples = 1000;
        this.smoothingFactor = 0.1;
        this.eventDetectionEnabled = true;
        
        // 性能统计初始化
        this.averageProcessingTime = 0.0;
        this.averageCompressionRatio = 0.0;
        this.usageCount = 0;
    }
    
    /**
     * 完整构造函数
     */
    public SamplingStrategy(SamplingMethod method, double ratio, int windowSize, 
                          double qualityThreshold, boolean adaptiveEnabled) {
        this();
        this.samplingMethod = method;
        this.samplingRatio = method.clampSamplingRatio(ratio);
        this.windowSize = Math.max(1, windowSize);
        this.qualityThreshold = Math.max(0.0, Math.min(1.0, qualityThreshold));
        this.adaptiveEnabled = adaptiveEnabled;
    }
    
    /**
     * 复制构造函数
     */
    public SamplingStrategy(SamplingStrategy other) {
        this.samplingMethod = other.samplingMethod;
        this.samplingRatio = other.samplingRatio;
        this.windowSize = other.windowSize;
        this.qualityThreshold = other.qualityThreshold;
        this.adaptiveEnabled = other.adaptiveEnabled;
        this.lastUpdateTime = System.currentTimeMillis();
        
        this.importanceThreshold = other.importanceThreshold;
        this.minSamples = other.minSamples;
        this.maxSamples = other.maxSamples;
        this.smoothingFactor = other.smoothingFactor;
        this.eventDetectionEnabled = other.eventDetectionEnabled;
        
        this.averageProcessingTime = other.averageProcessingTime;
        this.averageCompressionRatio = other.averageCompressionRatio;
        this.usageCount = other.usageCount;
    }
    
    // Getters and Setters
    
    public SamplingMethod getSamplingMethod() {
        return samplingMethod;
    }
    
    public void setSamplingMethod(SamplingMethod samplingMethod) {
        this.samplingMethod = samplingMethod;
        // 确保采样率在新方法的有效范围内
        this.samplingRatio = samplingMethod.clampSamplingRatio(this.samplingRatio);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public double getSamplingRatio() {
        return samplingRatio;
    }
    
    public void setSamplingRatio(double samplingRatio) {
        this.samplingRatio = samplingMethod.clampSamplingRatio(samplingRatio);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getWindowSize() {
        return windowSize;
    }
    
    public void setWindowSize(int windowSize) {
        this.windowSize = Math.max(1, windowSize);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public double getQualityThreshold() {
        return qualityThreshold;
    }
    
    public void setQualityThreshold(double qualityThreshold) {
        this.qualityThreshold = Math.max(0.0, Math.min(1.0, qualityThreshold));
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean isAdaptiveEnabled() {
        return adaptiveEnabled;
    }
    
    public void setAdaptiveEnabled(boolean adaptiveEnabled) {
        this.adaptiveEnabled = adaptiveEnabled;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public double getImportanceThreshold() {
        return importanceThreshold;
    }
    
    public void setImportanceThreshold(double importanceThreshold) {
        this.importanceThreshold = Math.max(0.0, Math.min(1.0, importanceThreshold));
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getMinSamples() {
        return minSamples;
    }
    
    public void setMinSamples(int minSamples) {
        this.minSamples = Math.max(1, minSamples);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getMaxSamples() {
        return maxSamples;
    }
    
    public void setMaxSamples(int maxSamples) {
        this.maxSamples = Math.max(this.minSamples, maxSamples);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public double getSmoothingFactor() {
        return smoothingFactor;
    }
    
    public void setSmoothingFactor(double smoothingFactor) {
        this.smoothingFactor = Math.max(0.0, Math.min(1.0, smoothingFactor));
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean isEventDetectionEnabled() {
        return eventDetectionEnabled;
    }
    
    public void setEventDetectionEnabled(boolean eventDetectionEnabled) {
        this.eventDetectionEnabled = eventDetectionEnabled;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public double getAverageProcessingTime() {
        return averageProcessingTime;
    }
    
    public double getAverageCompressionRatio() {
        return averageCompressionRatio;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    // 策略操作方法
    
    /**
     * 提高采样率
     */
    public SamplingStrategy withHigherSamplingRate() {
        SamplingStrategy newStrategy = new SamplingStrategy(this);
        double newRatio = Math.min(samplingMethod.getMaxSamplingRatio(), samplingRatio * 1.2);
        newStrategy.setSamplingRatio(newRatio);
        return newStrategy;
    }
    
    /**
     * 降低采样率
     */
    public SamplingStrategy withLowerSamplingRate() {
        SamplingStrategy newStrategy = new SamplingStrategy(this);
        double newRatio = Math.max(samplingMethod.getMinSamplingRatio(), samplingRatio * 0.8);
        newStrategy.setSamplingRatio(newRatio);
        return newStrategy;
    }
    
    /**
     * 切换到更高质量的采样方法
     */
    public SamplingStrategy withHigherQuality() {
        SamplingStrategy newStrategy = new SamplingStrategy(this);
        
        switch (samplingMethod) {
            case UNIFORM_DOWNSAMPLING:
                newStrategy.setSamplingMethod(SamplingMethod.INTELLIGENT_DECIMATION);
                break;
            case INTELLIGENT_DECIMATION:
                newStrategy.setSamplingMethod(SamplingMethod.MULTI_SCALE);
                break;
            case MULTI_SCALE:
                newStrategy.setSamplingMethod(SamplingMethod.ADAPTIVE_RATE);
                break;
            case ADAPTIVE_RATE:
                newStrategy.setSamplingMethod(SamplingMethod.EVENT_DRIVEN);
                break;
            case EVENT_DRIVEN:
                newStrategy.setSamplingMethod(SamplingMethod.FULL_RATE);
                break;
            default:
                // 已经是最高质量
                break;
        }
        
        return newStrategy;
    }
    
    /**
     * 切换到更低复杂度的采样方法
     */
    public SamplingStrategy withLowerComplexity() {
        SamplingStrategy newStrategy = new SamplingStrategy(this);
        
        switch (samplingMethod) {
            case MULTI_SCALE:
                newStrategy.setSamplingMethod(SamplingMethod.ADAPTIVE_RATE);
                break;
            case ADAPTIVE_RATE:
            case EVENT_DRIVEN:
                newStrategy.setSamplingMethod(SamplingMethod.INTELLIGENT_DECIMATION);
                break;
            case INTELLIGENT_DECIMATION:
                newStrategy.setSamplingMethod(SamplingMethod.UNIFORM_DOWNSAMPLING);
                break;
            default:
                // 已经是最低复杂度或保持不变
                break;
        }
        
        return newStrategy;
    }
    
    /**
     * 根据系统负载自适应调整
     */
    public void adaptToSystemLoad(double systemLoad) {
        if (!adaptiveEnabled) return;
        
        if (systemLoad > 0.8) {
            // 高负载：降低复杂度和采样率
            setSamplingMethod(SamplingMethod.recommendBySystemLoad(systemLoad));
            setSamplingRatio(samplingRatio * 0.9);
        } else if (systemLoad < 0.3) {
            // 低负载：可以提高质量
            if (samplingMethod.getComplexityLevel() < 4) {
                SamplingStrategy higherQuality = withHigherQuality();
                setSamplingMethod(higherQuality.getSamplingMethod());
            }
            setSamplingRatio(Math.min(samplingMethod.getMaxSamplingRatio(), samplingRatio * 1.1));
        }
    }
    
    /**
     * 根据信号重要性自适应调整
     */
    public void adaptToSignalImportance(double importance) {
        if (!adaptiveEnabled) return;
        
        if (importance > 0.8) {
            // 高重要性：提高采样率和质量
            setSamplingMethod(SamplingMethod.recommendByImportance(importance));
            setSamplingRatio(Math.min(samplingMethod.getMaxSamplingRatio(), samplingRatio * 1.2));
        } else if (importance < 0.3) {
            // 低重要性：可以降低采样率
            setSamplingRatio(Math.max(samplingMethod.getMinSamplingRatio(), samplingRatio * 0.8));
        }
    }
    
    /**
     * 根据质量反馈自适应调整
     */
    public void adaptToQualityFeedback(double actualQuality, double targetQuality) {
        if (!adaptiveEnabled) return;
        
        double qualityGap = targetQuality - actualQuality;
        
        if (qualityGap > 0.1) {
            // 质量不足：提高采样率或方法
            if (samplingRatio < samplingMethod.getMaxSamplingRatio() * 0.9) {
                setSamplingRatio(samplingRatio * 1.1);
            } else {
                SamplingStrategy higherQuality = withHigherQuality();
                setSamplingMethod(higherQuality.getSamplingMethod());
            }
        } else if (qualityGap < -0.1) {
            // 质量过剩：可以降低采样率
            setSamplingRatio(Math.max(samplingMethod.getMinSamplingRatio(), samplingRatio * 0.95));
        }
    }
    
    /**
     * 更新性能统计
     */
    public void updatePerformanceStats(double processingTime, double compressionRatio) {
        usageCount++;
        
        // 使用指数移动平均更新统计信息
        double alpha = 1.0 / Math.min(usageCount, 100); // 自适应学习率
        
        averageProcessingTime = (1 - alpha) * averageProcessingTime + alpha * processingTime;
        averageCompressionRatio = (1 - alpha) * averageCompressionRatio + alpha * compressionRatio;
    }
    
    /**
     * 计算策略效率评分
     */
    public double getEfficiencyScore() {
        if (usageCount == 0) return 0.5;
        
        // 综合考虑处理时间和压缩率
        double timeScore = Math.max(0.0, 1.0 - averageProcessingTime / 1000.0); // 假设1秒为基准
        double compressionScore = Math.min(1.0, averageCompressionRatio);
        
        return 0.6 * compressionScore + 0.4 * timeScore;
    }
    
    /**
     * 验证策略参数的有效性
     */
    public boolean isValid() {
        return samplingMethod != null &&
               samplingMethod.isValidSamplingRatio(samplingRatio) &&
               windowSize > 0 &&
               qualityThreshold >= 0.0 && qualityThreshold <= 1.0 &&
               importanceThreshold >= 0.0 && importanceThreshold <= 1.0 &&
               minSamples > 0 && maxSamples >= minSamples &&
               smoothingFactor >= 0.0 && smoothingFactor <= 1.0;
    }
    
    /**
     * 获取策略摘要
     */
    public String getSummary() {
        return String.format(
            "SamplingStrategy{method=%s, ratio=%.2f, window=%d, quality=%.2f, adaptive=%s, efficiency=%.2f}",
            samplingMethod.name(), samplingRatio, windowSize, qualityThreshold, 
            adaptiveEnabled, getEfficiencyScore()
        );
    }
    
    /**
     * 创建预设策略
     */
    public static SamplingStrategy createHighQualityStrategy() {
        return new SamplingStrategy(SamplingMethod.FULL_RATE, 1.0, 50, 0.95, true);
    }
    
    public static SamplingStrategy createBalancedStrategy() {
        return new SamplingStrategy(SamplingMethod.ADAPTIVE_RATE, 0.6, 100, 0.8, true);
    }
    
    public static SamplingStrategy createFastStrategy() {
        return new SamplingStrategy(SamplingMethod.UNIFORM_DOWNSAMPLING, 0.3, 200, 0.6, true);
    }
    
    public static SamplingStrategy createEventDrivenStrategy() {
        SamplingStrategy strategy = new SamplingStrategy(SamplingMethod.EVENT_DRIVEN, 0.5, 150, 0.85, true);
        strategy.setEventDetectionEnabled(true);
        strategy.setImportanceThreshold(0.7);
        return strategy;
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SamplingStrategy that = (SamplingStrategy) obj;
        return Double.compare(that.samplingRatio, samplingRatio) == 0 &&
               windowSize == that.windowSize &&
               Double.compare(that.qualityThreshold, qualityThreshold) == 0 &&
               adaptiveEnabled == that.adaptiveEnabled &&
               samplingMethod == that.samplingMethod;
    }
    
    @Override
    public int hashCode() {
        int result = samplingMethod.hashCode();
        result = 31 * result + Double.hashCode(samplingRatio);
        result = 31 * result + windowSize;
        result = 31 * result + Double.hashCode(qualityThreshold);
        result = 31 * result + Boolean.hashCode(adaptiveEnabled);
        return result;
    }
}
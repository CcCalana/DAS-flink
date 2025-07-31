package com.zjujzl.das.optimization.sampling;

/**
 * 采样方法枚举
 * 
 * 定义了多种自适应采样策略，每种方法适用于不同的信号特征和系统需求
 * 参考文献：
 * - IEEE Signal Processing 2023: Adaptive sampling strategies for real-time systems
 * - Digital Signal Processing 2022: Intelligent decimation techniques
 * - Applied Sciences 2021: Multi-scale sampling in seismic monitoring
 */
public enum SamplingMethod {
    
    /**
     * 全采样率 - 保持原始采样率
     * 适用于：高重要性信号、事件检测阶段、质量要求极高的场景
     * 计算复杂度：O(1)
     * 数据保留率：100%
     */
    FULL_RATE("全采样率", 1.0, 0.0, 1.0),
    
    /**
     * 自适应采样率 - 基于局部重要性动态调整采样率
     * 适用于：重要性变化较大的信号、需要保留关键特征的场景
     * 计算复杂度：O(n)
     * 数据保留率：30%-90%
     */
    ADAPTIVE_RATE("自适应采样率", 0.6, 0.3, 0.9),
    
    /**
     * 智能抽取 - 在固定窗口内选择最重要的样本
     * 适用于：中等重要性信号、需要均匀时间分布的场景
     * 计算复杂度：O(n/k) where k is decimation factor
     * 数据保留率：10%-60%
     */
    INTELLIGENT_DECIMATION("智能抽取", 0.3, 0.1, 0.6),
    
    /**
     * 多尺度采样 - 结合粗尺度和细尺度采样
     * 适用于：多频段信号、需要保留多层次特征的场景
     * 计算复杂度：O(n log n)
     * 数据保留率：20%-70%
     */
    MULTI_SCALE("多尺度采样", 0.4, 0.2, 0.7),
    
    /**
     * 事件驱动采样 - 在检测到事件时提高采样率
     * 适用于：稀疏事件检测、突发信号处理
     * 计算复杂度：O(n + m) where m is number of events
     * 数据保留率：15%-80%
     */
    EVENT_DRIVEN("事件驱动采样", 0.5, 0.15, 0.8),
    
    /**
     * 均匀下采样 - 固定间隔采样（备用方案）
     * 适用于：低重要性信号、系统资源极度受限的场景
     * 计算复杂度：O(n/k)
     * 数据保留率：5%-50%
     */
    UNIFORM_DOWNSAMPLING("均匀下采样", 0.2, 0.05, 0.5);
    
    private final String description;
    private final double defaultSamplingRatio;
    private final double minSamplingRatio;
    private final double maxSamplingRatio;
    
    SamplingMethod(String description, double defaultSamplingRatio, 
                   double minSamplingRatio, double maxSamplingRatio) {
        this.description = description;
        this.defaultSamplingRatio = defaultSamplingRatio;
        this.minSamplingRatio = minSamplingRatio;
        this.maxSamplingRatio = maxSamplingRatio;
    }
    
    /**
     * 获取方法描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取默认采样率
     */
    public double getDefaultSamplingRatio() {
        return defaultSamplingRatio;
    }
    
    /**
     * 获取最小采样率
     */
    public double getMinSamplingRatio() {
        return minSamplingRatio;
    }
    
    /**
     * 获取最大采样率
     */
    public double getMaxSamplingRatio() {
        return maxSamplingRatio;
    }
    
    /**
     * 验证采样率是否在有效范围内
     */
    public boolean isValidSamplingRatio(double ratio) {
        return ratio >= minSamplingRatio && ratio <= maxSamplingRatio;
    }
    
    /**
     * 将采样率限制在有效范围内
     */
    public double clampSamplingRatio(double ratio) {
        return Math.max(minSamplingRatio, Math.min(maxSamplingRatio, ratio));
    }
    
    /**
     * 判断是否为高质量采样方法
     */
    public boolean isHighQuality() {
        return this == FULL_RATE || this == ADAPTIVE_RATE || this == EVENT_DRIVEN;
    }
    
    /**
     * 判断是否为实时友好的方法（低计算复杂度）
     */
    public boolean isRealTimeFriendly() {
        return this == FULL_RATE || this == UNIFORM_DOWNSAMPLING || this == INTELLIGENT_DECIMATION;
    }
    
    /**
     * 判断是否为智能方法（基于信号特征）
     */
    public boolean isIntelligent() {
        return this == ADAPTIVE_RATE || this == INTELLIGENT_DECIMATION || 
               this == MULTI_SCALE || this == EVENT_DRIVEN;
    }
    
    /**
     * 获取计算复杂度等级
     * @return 1-5，1为最低复杂度，5为最高复杂度
     */
    public int getComplexityLevel() {
        switch (this) {
            case FULL_RATE:
                return 1;
            case UNIFORM_DOWNSAMPLING:
                return 2;
            case INTELLIGENT_DECIMATION:
                return 3;
            case ADAPTIVE_RATE:
            case EVENT_DRIVEN:
                return 4;
            case MULTI_SCALE:
                return 5;
            default:
                return 3;
        }
    }
    
    /**
     * 获取数据保真度等级
     * @return 1-5，1为最低保真度，5为最高保真度
     */
    public int getFidelityLevel() {
        switch (this) {
            case FULL_RATE:
                return 5;
            case ADAPTIVE_RATE:
            case EVENT_DRIVEN:
                return 4;
            case MULTI_SCALE:
                return 3;
            case INTELLIGENT_DECIMATION:
                return 2;
            case UNIFORM_DOWNSAMPLING:
                return 1;
            default:
                return 3;
        }
    }
    
    /**
     * 根据信号重要性推荐采样方法
     */
    public static SamplingMethod recommendByImportance(double importance) {
        if (importance > 0.8) {
            return FULL_RATE;
        } else if (importance > 0.6) {
            return EVENT_DRIVEN;
        } else if (importance > 0.4) {
            return ADAPTIVE_RATE;
        } else if (importance > 0.2) {
            return MULTI_SCALE;
        } else {
            return INTELLIGENT_DECIMATION;
        }
    }
    
    /**
     * 根据系统负载推荐采样方法
     */
    public static SamplingMethod recommendBySystemLoad(double systemLoad) {
        if (systemLoad > 0.8) {
            return UNIFORM_DOWNSAMPLING;
        } else if (systemLoad > 0.6) {
            return INTELLIGENT_DECIMATION;
        } else if (systemLoad > 0.4) {
            return MULTI_SCALE;
        } else if (systemLoad > 0.2) {
            return ADAPTIVE_RATE;
        } else {
            return FULL_RATE;
        }
    }
    
    /**
     * 根据质量要求推荐采样方法
     */
    public static SamplingMethod recommendByQualityRequirement(double qualityThreshold) {
        if (qualityThreshold > 0.9) {
            return FULL_RATE;
        } else if (qualityThreshold > 0.7) {
            return ADAPTIVE_RATE;
        } else if (qualityThreshold > 0.5) {
            return EVENT_DRIVEN;
        } else if (qualityThreshold > 0.3) {
            return MULTI_SCALE;
        } else {
            return INTELLIGENT_DECIMATION;
        }
    }
    
    /**
     * 综合推荐采样方法
     */
    public static SamplingMethod recommend(double importance, double systemLoad, double qualityThreshold) {
        // 加权评分
        double importanceWeight = 0.4;
        double loadWeight = 0.3;
        double qualityWeight = 0.3;
        
        SamplingMethod[] methods = values();
        double[] scores = new double[methods.length];
        
        for (int i = 0; i < methods.length; i++) {
            SamplingMethod method = methods[i];
            
            // 重要性评分
            double importanceScore = 0.0;
            if (importance > 0.8 && method == FULL_RATE) importanceScore = 1.0;
            else if (importance > 0.6 && method.isHighQuality()) importanceScore = 0.8;
            else if (importance > 0.4 && method.isIntelligent()) importanceScore = 0.6;
            else if (importance <= 0.4) importanceScore = 1.0 - method.getFidelityLevel() / 5.0;
            
            // 系统负载评分（负载高时偏好低复杂度方法）
            double loadScore = 1.0 - (systemLoad * method.getComplexityLevel() / 5.0);
            loadScore = Math.max(0.0, Math.min(1.0, loadScore));
            
            // 质量要求评分
            double qualityScore = method.getFidelityLevel() / 5.0;
            if (qualityThreshold > 0.8 && !method.isHighQuality()) {
                qualityScore *= 0.5; // 惩罚低质量方法
            }
            
            // 综合评分
            scores[i] = importanceWeight * importanceScore + 
                       loadWeight * loadScore + 
                       qualityWeight * qualityScore;
        }
        
        // 找到最高评分的方法
        int bestIndex = 0;
        double bestScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestIndex = i;
            }
        }
        
        return methods[bestIndex];
    }
    
    /**
     * 根据名称获取采样方法
     */
    public static SamplingMethod fromString(String name) {
        if (name == null) {
            return ADAPTIVE_RATE; // 默认方法
        }
        
        for (SamplingMethod method : values()) {
            if (method.name().equalsIgnoreCase(name) || 
                method.description.equals(name)) {
                return method;
            }
        }
        
        return ADAPTIVE_RATE; // 默认方法
    }
    
    @Override
    public String toString() {
        return String.format("%s (默认采样率: %.1f%%, 复杂度: %d, 保真度: %d)", 
                           description, defaultSamplingRatio * 100, 
                           getComplexityLevel(), getFidelityLevel());
    }
}
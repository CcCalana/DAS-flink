package com.zjujzl.das.optimization.sampling;

import java.util.Arrays;

/**
 * 信号重要性分析结果
 * 
 * 存储信号重要性分析的结果，包括局部重要性和全局重要性
 * 用于指导自适应采样策略的制定
 */
public class ImportanceProfile {
    
    private final double[] localImportance;
    private final double overallImportance;
    private final long timestamp;
    
    // 统计信息
    private final double maxLocalImportance;
    private final double minLocalImportance;
    private final double averageLocalImportance;
    private final double localImportanceVariance;
    
    public ImportanceProfile(double[] localImportance, double overallImportance) {
        this.localImportance = localImportance != null ? localImportance.clone() : new double[0];
        this.overallImportance = Math.max(0.0, Math.min(1.0, overallImportance));
        this.timestamp = System.currentTimeMillis();
        
        // 计算统计信息
        if (this.localImportance.length > 0) {
            this.maxLocalImportance = Arrays.stream(this.localImportance).max().orElse(0.0);
            this.minLocalImportance = Arrays.stream(this.localImportance).min().orElse(0.0);
            this.averageLocalImportance = Arrays.stream(this.localImportance).average().orElse(0.0);
            
            // 计算方差
            double variance = 0.0;
            for (double importance : this.localImportance) {
                double diff = importance - this.averageLocalImportance;
                variance += diff * diff;
            }
            this.localImportanceVariance = variance / this.localImportance.length;
        } else {
            this.maxLocalImportance = 0.0;
            this.minLocalImportance = 0.0;
            this.averageLocalImportance = 0.0;
            this.localImportanceVariance = 0.0;
        }
    }
    
    /**
     * 获取局部重要性数组
     */
    public double[] getLocalImportance() {
        return localImportance.clone();
    }
    
    /**
     * 获取指定位置的局部重要性
     */
    public double getLocalImportance(int index) {
        if (index >= 0 && index < localImportance.length) {
            return localImportance[index];
        }
        return 0.0;
    }
    
    /**
     * 获取全局重要性
     */
    public double getOverallImportance() {
        return overallImportance;
    }
    
    /**
     * 获取时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取最大局部重要性
     */
    public double getMaxLocalImportance() {
        return maxLocalImportance;
    }
    
    /**
     * 获取最小局部重要性
     */
    public double getMinLocalImportance() {
        return minLocalImportance;
    }
    
    /**
     * 获取平均局部重要性
     */
    public double getAverageLocalImportance() {
        return averageLocalImportance;
    }
    
    /**
     * 获取局部重要性方差
     */
    public double getLocalImportanceVariance() {
        return localImportanceVariance;
    }
    
    /**
     * 获取信号长度
     */
    public int getSignalLength() {
        return localImportance.length;
    }
    
    /**
     * 判断是否为高重要性信号
     */
    public boolean isHighImportance(double threshold) {
        return overallImportance > threshold;
    }
    
    /**
     * 获取高重要性区域的索引
     */
    public int[] getHighImportanceRegions(double threshold) {
        return Arrays.stream(localImportance)
                    .mapToInt(importance -> importance > threshold ? 1 : 0)
                    .toArray();
    }
    
    /**
     * 获取重要性分布的熵
     */
    public double getImportanceEntropy() {
        if (localImportance.length == 0) {
            return 0.0;
        }
        
        // 归一化重要性值作为概率分布
        double sum = Arrays.stream(localImportance).sum();
        if (sum <= 0) {
            return 0.0;
        }
        
        double entropy = 0.0;
        for (double importance : localImportance) {
            if (importance > 0) {
                double probability = importance / sum;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        return entropy;
    }
    
    /**
     * 获取重要性集中度（基尼系数）
     */
    public double getImportanceConcentration() {
        if (localImportance.length <= 1) {
            return 0.0;
        }
        
        double[] sorted = localImportance.clone();
        Arrays.sort(sorted);
        
        double sum = Arrays.stream(sorted).sum();
        if (sum <= 0) {
            return 0.0;
        }
        
        double gini = 0.0;
        for (int i = 0; i < sorted.length; i++) {
            gini += (2 * (i + 1) - sorted.length - 1) * sorted[i];
        }
        
        return gini / (sorted.length * sum);
    }
    
    /**
     * 获取推荐的采样策略类型
     */
    public SamplingMethod getRecommendedSamplingMethod() {
        if (overallImportance > 0.8) {
            return SamplingMethod.FULL_RATE;
        } else if (overallImportance > 0.6) {
            return SamplingMethod.EVENT_DRIVEN;
        } else if (localImportanceVariance > 0.1) {
            return SamplingMethod.ADAPTIVE_RATE;
        } else if (overallImportance > 0.3) {
            return SamplingMethod.MULTI_SCALE;
        } else {
            return SamplingMethod.INTELLIGENT_DECIMATION;
        }
    }
    
    /**
     * 获取推荐的采样率
     */
    public double getRecommendedSamplingRatio() {
        // 基于重要性和变异性推荐采样率
        double baseRatio = Math.max(0.1, Math.min(1.0, overallImportance));
        
        // 如果局部变异性高，增加采样率
        if (localImportanceVariance > 0.1) {
            baseRatio = Math.min(1.0, baseRatio * 1.2);
        }
        
        // 如果重要性集中度高，可以适当降低采样率
        double concentration = getImportanceConcentration();
        if (concentration > 0.5) {
            baseRatio = Math.max(0.1, baseRatio * 0.9);
        }
        
        return baseRatio;
    }
    
    /**
     * 创建重要性摘要
     */
    public ImportanceSummary createSummary() {
        return new ImportanceSummary(
            overallImportance,
            maxLocalImportance,
            minLocalImportance,
            averageLocalImportance,
            localImportanceVariance,
            getImportanceEntropy(),
            getImportanceConcentration(),
            getRecommendedSamplingMethod(),
            getRecommendedSamplingRatio()
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "ImportanceProfile{overall=%.3f, avg=%.3f, max=%.3f, min=%.3f, var=%.3f, length=%d}",
            overallImportance, averageLocalImportance, maxLocalImportance, 
            minLocalImportance, localImportanceVariance, localImportance.length
        );
    }
    
    /**
     * 重要性摘要类
     */
    public static class ImportanceSummary {
        private final double overallImportance;
        private final double maxLocalImportance;
        private final double minLocalImportance;
        private final double averageLocalImportance;
        private final double localImportanceVariance;
        private final double importanceEntropy;
        private final double importanceConcentration;
        private final SamplingMethod recommendedMethod;
        private final double recommendedSamplingRatio;
        
        public ImportanceSummary(double overallImportance, double maxLocalImportance, 
                               double minLocalImportance, double averageLocalImportance,
                               double localImportanceVariance, double importanceEntropy,
                               double importanceConcentration, SamplingMethod recommendedMethod,
                               double recommendedSamplingRatio) {
            this.overallImportance = overallImportance;
            this.maxLocalImportance = maxLocalImportance;
            this.minLocalImportance = minLocalImportance;
            this.averageLocalImportance = averageLocalImportance;
            this.localImportanceVariance = localImportanceVariance;
            this.importanceEntropy = importanceEntropy;
            this.importanceConcentration = importanceConcentration;
            this.recommendedMethod = recommendedMethod;
            this.recommendedSamplingRatio = recommendedSamplingRatio;
        }
        
        // Getters
        public double getOverallImportance() { return overallImportance; }
        public double getMaxLocalImportance() { return maxLocalImportance; }
        public double getMinLocalImportance() { return minLocalImportance; }
        public double getAverageLocalImportance() { return averageLocalImportance; }
        public double getLocalImportanceVariance() { return localImportanceVariance; }
        public double getImportanceEntropy() { return importanceEntropy; }
        public double getImportanceConcentration() { return importanceConcentration; }
        public SamplingMethod getRecommendedMethod() { return recommendedMethod; }
        public double getRecommendedSamplingRatio() { return recommendedSamplingRatio; }
        
        @Override
        public String toString() {
            return String.format(
                "ImportanceSummary{overall=%.3f, entropy=%.3f, concentration=%.3f, method=%s, ratio=%.3f}",
                overallImportance, importanceEntropy, importanceConcentration, 
                recommendedMethod, recommendedSamplingRatio
            );
        }
    }
}
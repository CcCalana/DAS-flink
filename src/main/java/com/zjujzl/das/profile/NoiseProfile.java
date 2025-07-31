package com.zjujzl.das.profile;

/**
 * 噪声特征配置文件
 * 包含信号的统计特征和频域特征，用于判断噪声类型
 */
public class NoiseProfile {
    public double kurtosis;           // 峰度值
    public double spectralCentroid;   // 频谱质心
    public double autocorrelationRatio; // 自相关比
    public int signalLength;          // 信号长度
    public double samplingRate;       // 采样频率（Hz）
    
    // 噪声判断阈值常量
    private static final double IMPULSIVE_NOISE_THRESHOLD = 2.0;  // 冲击性噪声阈值（更保守）
    private static final double PERIODIC_NOISE_THRESHOLD = 0.2;   // 周期性噪声阈值（更保守）
    private static final double LOW_FREQ_RATIO_THRESHOLD = 0.25;  // 低频噪声比例阈值

    /**
     * 判断是否存在冲击性噪声
     * 基于超额峰度：Kurt > 2.0 表示存在显著的冲击性成分
     * 正态分布的超额峰度为0，冲击性噪声会使峰度显著增大
     */
    public boolean hasImpulsiveNoise() {
        return kurtosis > IMPULSIVE_NOISE_THRESHOLD;
    }

    /**
     * 判断是否存在周期性噪声
     * 基于自相关比：ratio > 0.2 表示存在周期性成分
     * 较高的自相关比表明信号具有重复模式
     */
    public boolean hasPeriodicNoise() {
        return autocorrelationRatio > PERIODIC_NOISE_THRESHOLD;
    }

    /**
     * 判断是否存在低频噪声
     * 基于频谱质心：SC < 0.25 * Nyquist频率 表示能量集中在低频
     * 频谱质心偏低表明信号能量主要分布在低频段
     */
    public boolean hasLowFreqNoise() {
        double nyquistFreq = samplingRate / 2;
        return spectralCentroid < LOW_FREQ_RATIO_THRESHOLD * nyquistFreq;
    }
    
    /**
     * 获取噪声类型的综合评估
     * 返回主要噪声类型的字符串描述
     */
    public String getDominantNoiseType() {
        if (hasImpulsiveNoise() && hasPeriodicNoise()) {
            return "Mixed (Impulsive + Periodic)";
        } else if (hasImpulsiveNoise() && hasLowFreqNoise()) {
            return "Mixed (Impulsive + Low-freq)";
        } else if (hasPeriodicNoise() && hasLowFreqNoise()) {
            return "Mixed (Periodic + Low-freq)";
        } else if (hasImpulsiveNoise()) {
            return "Impulsive";
        } else if (hasPeriodicNoise()) {
            return "Periodic";
        } else if (hasLowFreqNoise()) {
            return "Low-frequency";
        } else {
            return "Broadband";
        }
    }
    
    /**
     * 计算噪声复杂度评分
     * 范围：0-1，值越高表示噪声越复杂
     */
    public double getNoiseComplexity() {
        double complexity = 0;
        
        // 冲击性噪声复杂度
        if (hasImpulsiveNoise()) {
            complexity += Math.min(1.0, kurtosis / 10.0) * 0.4;
        }
        
        // 周期性噪声复杂度
        if (hasPeriodicNoise()) {
            complexity += Math.min(1.0, autocorrelationRatio / 0.8) * 0.3;
        }
        
        // 低频噪声复杂度
        if (hasLowFreqNoise()) {
            complexity += 0.3;
        }
        
        return Math.min(1.0, complexity);
    }
    
    /**
     * 获取频谱质心在频率域的位置比例
     * 返回值范围：0-1，表示质心在0Hz到Nyquist频率之间的相对位置
     */
    public double getSpectralCentroidRatio() {
        if (samplingRate <= 0) return 0;
        double nyquistFreq = samplingRate / 2;
        return nyquistFreq > 0 ? Math.min(1.0, spectralCentroid / nyquistFreq) : 0;
    }
    
    /**
     * 判断是否为高频噪声
     * 基于频谱质心位置判断
     */
    public boolean hasHighFreqNoise() {
        return getSpectralCentroidRatio() > 0.75;
    }
    
    @Override
    public String toString() {
        return String.format("NoiseProfile{kurtosis=%.3f, spectralCentroid=%.3f Hz, " +
                           "autocorrelationRatio=%.3f, samplingRate=%.1f Hz, type=%s, complexity=%.3f}",
                           kurtosis, spectralCentroid, autocorrelationRatio, samplingRate,
                           getDominantNoiseType(), getNoiseComplexity());
    }
}
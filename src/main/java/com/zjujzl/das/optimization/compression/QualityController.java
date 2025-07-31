package com.zjujzl.das.optimization.compression;

import com.zjujzl.das.model.SeismicRecord;

import java.util.Arrays;

/**
 * 质量控制器
 * 
 * 负责评估压缩质量并进行质量控制
 */
public class QualityController {
    
    private final CompressionConfig config;
    
    public QualityController(CompressionConfig config) {
        this.config = config;
    }
    
    /**
     * 验证压缩质量
     */
    public boolean validateQuality(SeismicRecord original, CompressedData compressed) {
        if (original == null || compressed == null) {
            return false;
        }
        
        // 1. 检查压缩率是否合理
        if (compressed.getCompressionRatio() < config.getMinCompressionRatio() ||
            compressed.getCompressionRatio() > config.getMaxCompressionRatio()) {
            return false;
        }
        
        // 2. 检查质量评分
        double qualityScore = calculateQualityScore(original, compressed);
        
        return qualityScore >= config.getMinQualityThreshold();
    }
    
    /**
     * 计算质量评分
     */
    public double calculateQualityScore(SeismicRecord original, CompressedData compressed) {
        if (original == null || original.data == null || compressed == null) {
            return 0.0;
        }
        
        try {
            // 对于实际应用，这里应该解压缩数据进行比较
            // 现在使用简化的质量评估方法
            
            double[] originalSignal = Arrays.stream(original.data).asDoubleStream().toArray();
            
            // 1. 基于压缩率的质量估计
            double compressionQuality = estimateCompressionQuality(compressed.getCompressionRatio(), compressed.getMethod());
            
            // 2. 基于信号特征的质量估计
            double signalQuality = estimateSignalQuality(originalSignal, compressed.getMethod());
            
            // 3. 基于处理时间的效率评分
            double efficiencyScore = calculateEfficiencyScore(compressed.getCompressionTime(), originalSignal.length);
            
            // 综合质量评分
            return (compressionQuality * 0.5 + signalQuality * 0.3 + efficiencyScore * 0.2);
            
        } catch (Exception e) {
            System.err.println("WARNING: Quality calculation failed: " + e.getMessage());
            return 0.5; // 默认中等质量
        }
    }
    
    /**
     * 估计压缩质量
     */
    private double estimateCompressionQuality(double compressionRatio, CompressionMethod method) {
        // 基于压缩方法和压缩率估计质量
        double baseQuality;
        
        switch (method) {
            case WAVELET_CS:
                // 小波压缩感知通常有较好的质量保持
                baseQuality = 0.9;
                break;
            case FREQUENCY_DOMAIN:
                // 频域压缩质量中等
                baseQuality = 0.8;
                break;
            case DICTIONARY_LEARNING:
                // 字典学习质量较好但依赖于字典质量
                baseQuality = 0.85;
                break;
            case HYBRID:
                // 混合方法通常有最好的质量
                baseQuality = 0.95;
                break;
            case QUANTIZATION:
                // 量化压缩质量较低
                baseQuality = 0.6;
                break;
            default:
                baseQuality = 0.7;
                break;
        }
        
        // 根据压缩率调整质量估计
        if (compressionRatio > 0.8) {
            // 低压缩率，质量损失小
            return baseQuality * 0.95;
        } else if (compressionRatio > 0.5) {
            // 中等压缩率
            return baseQuality * 0.85;
        } else if (compressionRatio > 0.3) {
            // 高压缩率，质量损失较大
            return baseQuality * 0.7;
        } else {
            // 极高压缩率，质量损失很大
            return baseQuality * 0.5;
        }
    }
    
    /**
     * 估计信号质量
     */
    private double estimateSignalQuality(double[] signal, CompressionMethod method) {
        // 分析原始信号特征，估计压缩后的质量保持程度
        
        // 1. 计算信号的动态范围
        double max = Arrays.stream(signal).max().orElse(1.0);
        double min = Arrays.stream(signal).min().orElse(0.0);
        double dynamicRange = max - min;
        
        // 2. 计算信号的变化率
        double variationRate = calculateVariationRate(signal);
        
        // 3. 计算信号的频谱特征
        double spectralComplexity = estimateSpectralComplexity(signal);
        
        // 根据信号特征和压缩方法估计质量
        double qualityFactor = 1.0;
        
        if (method == CompressionMethod.FREQUENCY_DOMAIN && spectralComplexity > 0.8) {
            // 复杂频谱信号用频域压缩可能质量下降
            qualityFactor *= 0.8;
        }
        
        if (method == CompressionMethod.WAVELET_CS && variationRate > 0.7) {
            // 高变化率信号适合小波压缩
            qualityFactor *= 1.1;
        }
        
        if (dynamicRange < 0.1) {
            // 低动态范围信号容易压缩且质量保持好
            qualityFactor *= 1.05;
        }
        
        return Math.min(1.0, 0.8 * qualityFactor);
    }
    
    /**
     * 计算效率评分
     */
    private double calculateEfficiencyScore(long processingTime, int signalLength) {
        if (processingTime <= 0 || signalLength <= 0) {
            return 0.8; // 默认评分
        }
        
        // 计算每个样本的处理时间（毫秒）
        double timePerSample = (double) processingTime / signalLength;
        
        // 根据处理时间评分（越快越好）
        if (timePerSample < 0.001) {
            return 1.0; // 非常快
        } else if (timePerSample < 0.01) {
            return 0.9; // 快
        } else if (timePerSample < 0.1) {
            return 0.8; // 中等
        } else if (timePerSample < 1.0) {
            return 0.6; // 慢
        } else {
            return 0.4; // 很慢
        }
    }
    
    /**
     * 计算信号变化率
     */
    private double calculateVariationRate(double[] signal) {
        if (signal.length < 2) return 0.0;
        
        double totalVariation = 0.0;
        for (int i = 1; i < signal.length; i++) {
            totalVariation += Math.abs(signal[i] - signal[i-1]);
        }
        
        double signalRange = Arrays.stream(signal).max().orElse(1.0) - 
                           Arrays.stream(signal).min().orElse(0.0);
        
        return signalRange > 0 ? totalVariation / (signalRange * signal.length) : 0.0;
    }
    
    /**
     * 估计频谱复杂度
     */
    private double estimateSpectralComplexity(double[] signal) {
        // 简化的频谱复杂度估计
        // 计算信号的高频成分比例
        
        if (signal.length < 4) return 0.5;
        
        // 计算相邻样本差分的能量
        double diffEnergy = 0.0;
        double totalEnergy = 0.0;
        
        for (int i = 1; i < signal.length; i++) {
            double diff = signal[i] - signal[i-1];
            diffEnergy += diff * diff;
            totalEnergy += signal[i] * signal[i];
        }
        
        return totalEnergy > 0 ? Math.min(1.0, diffEnergy / totalEnergy) : 0.5;
    }
    
    /**
     * 获取质量评估报告
     */
    public QualityReport generateQualityReport(SeismicRecord original, CompressedData compressed) {
        double overallScore = calculateQualityScore(original, compressed);
        
        double compressionQuality = estimateCompressionQuality(compressed.getCompressionRatio(), compressed.getMethod());
        double[] originalSignal = Arrays.stream(original.data).asDoubleStream().toArray();
        double signalQuality = estimateSignalQuality(originalSignal, compressed.getMethod());
        double efficiencyScore = calculateEfficiencyScore(compressed.getCompressionTime(), originalSignal.length);
        
        boolean acceptable = overallScore >= config.getMinQualityThreshold();
        
        return new QualityReport(
            overallScore,
            compressionQuality,
            signalQuality,
            efficiencyScore,
            acceptable,
            compressed.getMethod(),
            compressed.getCompressionRatio()
        );
    }
    
    /**
     * 质量报告内部类
     */
    public static class QualityReport {
        private final double overallScore;
        private final double compressionQuality;
        private final double signalQuality;
        private final double efficiencyScore;
        private final boolean acceptable;
        private final CompressionMethod method;
        private final double compressionRatio;
        
        public QualityReport(double overallScore, double compressionQuality, double signalQuality,
                           double efficiencyScore, boolean acceptable, CompressionMethod method,
                           double compressionRatio) {
            this.overallScore = overallScore;
            this.compressionQuality = compressionQuality;
            this.signalQuality = signalQuality;
            this.efficiencyScore = efficiencyScore;
            this.acceptable = acceptable;
            this.method = method;
            this.compressionRatio = compressionRatio;
        }
        
        // Getter方法
        public double getOverallScore() { return overallScore; }
        public double getCompressionQuality() { return compressionQuality; }
        public double getSignalQuality() { return signalQuality; }
        public double getEfficiencyScore() { return efficiencyScore; }
        public boolean isAcceptable() { return acceptable; }
        public CompressionMethod getMethod() { return method; }
        public double getCompressionRatio() { return compressionRatio; }
        
        @Override
        public String toString() {
            return String.format("QualityReport{overall=%.3f, compression=%.3f, signal=%.3f, efficiency=%.3f, acceptable=%s, method=%s, ratio=%.2f}",
                    overallScore, compressionQuality, signalQuality, efficiencyScore, acceptable, method, compressionRatio);
        }
    }
}
package com.zjujzl.das.optimization.compression;

import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.algorithm.FFT;
import com.zjujzl.das.algorithm.WaveletDenoiser;

import java.util.Arrays;

/**
 * 稀疏性分析器
 * 
 * 分析地震信号在不同变换域的稀疏性特征，为压缩策略选择提供依据
 * 基于压缩感知理论和稀疏表示理论
 */
public class SparsityAnalyzer {
    
    private static final double SPARSITY_THRESHOLD = 0.1; // 稀疏性阈值
    private static final int MIN_SIGNAL_LENGTH = 64; // 最小信号长度
    
    /**
     * 分析信号的稀疏性特征
     */
    public SparsityProfile analyze(SeismicRecord record) {
        if (record == null || record.data == null || record.data.length < MIN_SIGNAL_LENGTH) {
            return createDefaultProfile();
        }
        
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        // 1. 时域稀疏性分析
        double timeSparsity = analyzeTimeSparsity(signal);
        
        // 2. 频域稀疏性分析
        double frequencySparsity = analyzeFrequencySparsity(signal);
        
        // 3. 小波域稀疏性分析
        double waveletSparsity = analyzeWaveletSparsity(signal);
        
        // 4. 字典稀疏性估计
        double dictionarySparsity = estimateDictionarySparsity(signal);
        
        // 5. 信号复杂度分析
        double complexity = analyzeSignalComplexity(signal);
        
        return new SparsityProfile(
            signal.length,
            timeSparsity,
            frequencySparsity,
            waveletSparsity,
            dictionarySparsity,
            complexity
        );
    }
    
    /**
     * 时域稀疏性分析
     */
    private double analyzeTimeSparsity(double[] signal) {
        // 计算信号中接近零值的比例
        double threshold = calculateAdaptiveThreshold(signal);
        int sparseCount = 0;
        
        for (double value : signal) {
            if (Math.abs(value) < threshold) {
                sparseCount++;
            }
        }
        
        return (double) sparseCount / signal.length;
    }
    
    /**
     * 频域稀疏性分析
     */
    private double analyzeFrequencySparsity(double[] signal) {
        try {
            // FFT变换
            double[] spectrum = FFT.fft(signal);
            
            // 计算频谱的稀疏性
            double[] magnitudes = new double[spectrum.length];
            for (int i = 0; i < spectrum.length; i++) {
                magnitudes[i] = Math.abs(spectrum[i]);
            }
            
            // 计算能量集中度
            double totalEnergy = Arrays.stream(magnitudes).map(x -> x * x).sum();
            double threshold = Math.sqrt(totalEnergy / magnitudes.length) * 0.1;
            
            int sparseCount = 0;
            for (double magnitude : magnitudes) {
                if (magnitude < threshold) {
                    sparseCount++;
                }
            }
            
            return (double) sparseCount / magnitudes.length;
            
        } catch (Exception e) {
            System.err.println("WARNING: Frequency sparsity analysis failed: " + e.getMessage());
            return 0.5; // 默认值
        }
    }
    
    /**
     * 小波域稀疏性分析
     */
    private double analyzeWaveletSparsity(double[] signal) {
        try {
            // 多级小波分解
            double[] coeffs = WaveletDenoiser.getWaveletCoefficients(signal, "db4");
            
            // 计算小波系数的稀疏性
            double threshold = calculateAdaptiveThreshold(coeffs);
            int sparseCount = 0;
            
            for (double coeff : coeffs) {
                if (Math.abs(coeff) < threshold) {
                    sparseCount++;
                }
            }
            
            return (double) sparseCount / coeffs.length;
            
        } catch (Exception e) {
            System.err.println("WARNING: Wavelet sparsity analysis failed: " + e.getMessage());
            return 0.5; // 默认值
        }
    }
    
    /**
     * 字典稀疏性估计
     */
    private double estimateDictionarySparsity(double[] signal) {
        // 基于信号的自相关特性估计字典稀疏性
        double[] autocorr = computeAutocorrelation(signal, Math.min(signal.length / 4, 100));
        
        // 分析自相关的衰减特性
        double decayRate = analyzeDecayRate(autocorr);
        
        // 快速衰减表明信号具有良好的字典稀疏性
        return Math.min(1.0, decayRate * 2.0);
    }
    
    /**
     * 信号复杂度分析
     */
    private double analyzeSignalComplexity(double[] signal) {
        // 1. 计算信号的变化率
        double variationRate = calculateVariationRate(signal);
        
        // 2. 计算信号的熵
        double entropy = calculateApproximateEntropy(signal);
        
        // 3. 计算信号的分形维数估计
        double fractalDimension = estimateFractalDimension(signal);
        
        // 综合复杂度指标
        return (variationRate + entropy + fractalDimension) / 3.0;
    }
    
    /**
     * 计算自适应阈值
     */
    private double calculateAdaptiveThreshold(double[] signal) {
        // 使用信号标准差的一定比例作为阈值
        double mean = Arrays.stream(signal).average().orElse(0.0);
        double variance = Arrays.stream(signal)
                .map(x -> (x - mean) * (x - mean))
                .average().orElse(0.0);
        double std = Math.sqrt(variance);
        
        return std * SPARSITY_THRESHOLD;
    }
    
    /**
     * 计算自相关函数
     */
    private double[] computeAutocorrelation(double[] signal, int maxLag) {
        double[] autocorr = new double[maxLag];
        double mean = Arrays.stream(signal).average().orElse(0.0);
        
        // 计算方差
        double variance = Arrays.stream(signal)
                .map(x -> (x - mean) * (x - mean))
                .average().orElse(1.0);
        
        for (int lag = 0; lag < maxLag; lag++) {
            double sum = 0.0;
            int count = 0;
            
            for (int i = 0; i < signal.length - lag; i++) {
                sum += (signal[i] - mean) * (signal[i + lag] - mean);
                count++;
            }
            
            autocorr[lag] = count > 0 ? (sum / count) / variance : 0.0;
        }
        
        return autocorr;
    }
    
    /**
     * 分析衰减率
     */
    private double analyzeDecayRate(double[] autocorr) {
        if (autocorr.length < 2) return 0.5;
        
        // 寻找自相关函数衰减到初值一半的位置
        double halfValue = Math.abs(autocorr[0]) * 0.5;
        
        for (int i = 1; i < autocorr.length; i++) {
            if (Math.abs(autocorr[i]) < halfValue) {
                return 1.0 / i; // 衰减率与衰减距离成反比
            }
        }
        
        return 1.0 / autocorr.length; // 慢衰减
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
        
        // 归一化
        double signalRange = Arrays.stream(signal).max().orElse(1.0) - 
                           Arrays.stream(signal).min().orElse(0.0);
        
        return signalRange > 0 ? totalVariation / (signalRange * signal.length) : 0.0;
    }
    
    /**
     * 计算近似熵
     */
    private double calculateApproximateEntropy(double[] signal) {
        // 简化的近似熵计算
        int m = 2; // 模式长度
        double r = 0.2 * calculateStandardDeviation(signal); // 容忍度
        
        return approximateEntropy(signal, m, r);
    }
    
    /**
     * 估计分形维数
     */
    private double estimateFractalDimension(double[] signal) {
        // 使用盒计数法的简化版本
        int[] boxSizes = {2, 4, 8, 16, 32};
        double[] logBoxSizes = new double[boxSizes.length];
        double[] logCounts = new double[boxSizes.length];
        
        for (int i = 0; i < boxSizes.length; i++) {
            int boxSize = boxSizes[i];
            if (boxSize >= signal.length) break;
            
            int count = countBoxes(signal, boxSize);
            logBoxSizes[i] = Math.log(1.0 / boxSize);
            logCounts[i] = Math.log(count);
        }
        
        // 线性回归估计斜率（分形维数）
        return estimateSlope(logBoxSizes, logCounts);
    }
    
    /**
     * 计算标准差
     */
    private double calculateStandardDeviation(double[] signal) {
        double mean = Arrays.stream(signal).average().orElse(0.0);
        double variance = Arrays.stream(signal)
                .map(x -> (x - mean) * (x - mean))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * 近似熵计算
     */
    private double approximateEntropy(double[] signal, int m, double r) {
        int N = signal.length;
        if (N < m + 1) return 0.0;
        
        double phi_m = calculatePhi(signal, m, r);
        double phi_m1 = calculatePhi(signal, m + 1, r);
        
        return phi_m - phi_m1;
    }
    
    /**
     * 计算Phi函数
     */
    private double calculatePhi(double[] signal, int m, double r) {
        int N = signal.length;
        double phi = 0.0;
        
        for (int i = 0; i <= N - m; i++) {
            int matches = 0;
            
            for (int j = 0; j <= N - m; j++) {
                boolean match = true;
                for (int k = 0; k < m; k++) {
                    if (Math.abs(signal[i + k] - signal[j + k]) > r) {
                        match = false;
                        break;
                    }
                }
                if (match) matches++;
            }
            
            if (matches > 0) {
                phi += Math.log((double) matches / (N - m + 1));
            }
        }
        
        return phi / (N - m + 1);
    }
    
    /**
     * 盒计数
     */
    private int countBoxes(double[] signal, int boxSize) {
        int count = 0;
        for (int i = 0; i < signal.length; i += boxSize) {
            int end = Math.min(i + boxSize, signal.length);
            if (end > i) {
                // 检查这个盒子是否包含信号
                boolean hasSignal = false;
                for (int j = i; j < end; j++) {
                    if (Math.abs(signal[j]) > 1e-10) {
                        hasSignal = true;
                        break;
                    }
                }
                if (hasSignal) count++;
            }
        }
        return Math.max(1, count);
    }
    
    /**
     * 估计斜率
     */
    private double estimateSlope(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) return 1.0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = 0;
        
        for (int i = 0; i < x.length; i++) {
            if (!Double.isNaN(x[i]) && !Double.isNaN(y[i]) && 
                !Double.isInfinite(x[i]) && !Double.isInfinite(y[i])) {
                sumX += x[i];
                sumY += y[i];
                sumXY += x[i] * y[i];
                sumX2 += x[i] * x[i];
                n++;
            }
        }
        
        if (n < 2) return 1.0;
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 1.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    /**
     * 创建默认稀疏性配置
     */
    private SparsityProfile createDefaultProfile() {
        return new SparsityProfile(0, 0.5, 0.5, 0.5, 0.5, 0.5);
    }
}
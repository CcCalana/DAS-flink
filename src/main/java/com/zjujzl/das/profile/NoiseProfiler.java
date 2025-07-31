package com.zjujzl.das.profile;

import com.zjujzl.das.algorithm.FFT;
import com.zjujzl.das.model.SeismicRecord;

import java.util.Arrays;

/**
 * 噪声特征分析器
 * 通过统计和频域分析提取信号的噪声特征
 */
public class NoiseProfiler {

    /**
     * 分析SeismicRecord对象的噪声特征
     * 使用JSON中的真实采样频率
     */
    public static NoiseProfile analyze(SeismicRecord record) {
        if (record == null || record.data == null || record.data.length == 0) {
            return new NoiseProfile();
        }
        
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        return analyze(signal, record.sampling_rate);
    }
    
    /**
     * 分析信号的噪声特征
     * 使用指定的采样频率
     */
    public static NoiseProfile analyze(double[] signal, double samplingRate) {
        if (signal == null || signal.length == 0) {
            return new NoiseProfile();
        }
        
        if (samplingRate <= 0) {
            System.err.println("WARN: Invalid sampling rate: " + samplingRate + ", using default value");
            samplingRate = signal.length; // 回退到默认值
        }
        
        NoiseProfile profile = new NoiseProfile();

        profile.kurtosis = calculateKurtosis(signal);
        profile.spectralCentroid = calculateSpectralCentroid(signal, samplingRate);
        profile.autocorrelationRatio = calculateAutocorrelationRatio(signal);
        profile.signalLength = signal.length;
        profile.samplingRate = samplingRate; // 保存采样频率

        return profile;
    }

    /**
     * 分析信号的噪声特征（向后兼容）
     * 使用默认的采样频率估算
     */
    public static NoiseProfile analyze(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new NoiseProfile();
        }
        
        // 使用默认的采样频率估算（向后兼容）
        double defaultSamplingRate = signal.length;
        System.out.println("WARN: Using default sampling rate estimation. Consider using analyze(SeismicRecord) or analyze(double[], double) for accurate results.");
        
        return analyze(signal, defaultSamplingRate);
    }

    /**
     * 计算峰度（用于判断冲击性噪声）
     * 数学公式：Kurt = E[(X-μ)^4]/σ^4 - 3
     * 其中：μ为均值，σ为标准差，E[]为期望值
     * 峰度 > 3 表示分布比正态分布更尖锐，可能存在冲击性噪声
     */
    private static double calculateKurtosis(double[] data) {
        if (data.length < 2) return 0;
        
        double mean = Arrays.stream(data).average().orElse(0);
        double variance = 0;
        double fourthMoment = 0;

        for (double value : data) {
            double diff = value - mean;
            variance += diff * diff;
            fourthMoment += diff * diff * diff * diff;
        }

        variance /= data.length;
        fourthMoment /= data.length;

        if (variance <= 0) return 0;
        return (fourthMoment / Math.pow(variance, 2)) - 3;
    }

    /**
     * 计算频谱质心（用于判断高频/低频分布）
     * 数学公式：SC = Σ(f_i * |X(f_i)|) / Σ|X(f_i)|
     * 其中：f_i 为频率，|X(f_i)| 为该频率的幅度
     * 频谱质心反映了信号能量的频率分布重心
     */
    private static double calculateSpectralCentroid(double[] data, double samplingRate) {
        if (data.length < 2) return 0;
        
        double[] magnitude = FFT.abs(data);
        double sum = 0;
        double weightedSum = 0;
        
        // 使用真实的采样频率计算频率分辨率
        double frequencyResolution = samplingRate / data.length;
        
        // 只计算到Nyquist频率（samplingRate/2）
        int nyquistBin = magnitude.length / 2;
        
        for (int i = 0; i < nyquistBin; i++) {
            double frequency = i * frequencyResolution;
            double mag = magnitude[i];
            
            weightedSum += frequency * mag;
            sum += mag;
        }

        return (sum == 0) ? 0 : weightedSum / sum;
    }

    /**
     * 计算自相关比（用于判断周期性噪声）
     * 数学公式：R(τ) = Σ[x(n) * x(n+τ)] / Σ[x(n)^2]
     * 自相关比 = max(R(τ)) / R(0)，其中 τ ≠ 0
     * 高自相关比表示信号具有周期性特征
     */
    private static double calculateAutocorrelationRatio(double[] data) {
        if (data.length < 2) return 0;
        
        int maxLag = Math.min(data.length / 2, 100);
        double zeroLag = 0;
        double maxOtherLag = 0;

        // 首先计算零滞后的自相关（信号能量）
        for (int i = 0; i < data.length; i++) {
            zeroLag += data[i] * data[i];
        }
        
        if (zeroLag == 0) return 0;

        // 计算其他滞后的自相关
        for (int lag = 1; lag < maxLag; lag++) {
            double corr = 0;
            int validLength = data.length - lag;
            
            for (int i = 0; i < validLength; i++) {
                corr += data[i] * data[i + lag];
            }
            
            // 归一化处理
            corr = corr / zeroLag;
            maxOtherLag = Math.max(maxOtherLag, Math.abs(corr));
        }

        return maxOtherLag;
    }
    
    /**
     * 计算信号能量分布
     * 辅助函数，用于更精确的噪声特征判断
     */
    private static double calculateEnergyRatio(double[] data, double samplingRate, double lowFreqThreshold) {
        if (data.length < 2) return 0;
        
        double[] magnitude = FFT.abs(data);
        double totalEnergy = 0;
        double lowFreqEnergy = 0;
        
        // 使用真实的采样频率计算频率分辨率
        double frequencyResolution = samplingRate / data.length;
        double nyquistFreq = samplingRate / 2;
        
        // 计算低频阈值对应的频率bin
        int lowFreqBin = (int) (lowFreqThreshold * nyquistFreq / frequencyResolution);
        int nyquistBin = magnitude.length / 2;
        
        for (int i = 0; i < nyquistBin; i++) {
            double energy = magnitude[i] * magnitude[i];
            totalEnergy += energy;
            
            if (i < lowFreqBin) {
                lowFreqEnergy += energy;
            }
        }
        
        return totalEnergy > 0 ? lowFreqEnergy / totalEnergy : 0;
    }
    
    /**
     * 获取信号的主要频率成分
     * 返回能量最大的频率（Hz）
     */
    public static double getDominantFrequency(double[] signal, double samplingRate) {
        if (signal == null || signal.length == 0 || samplingRate <= 0) {
            return 0;
        }
        
        double[] magnitude = FFT.abs(signal);
        double frequencyResolution = samplingRate / signal.length;
        int nyquistBin = magnitude.length / 2;
        
        int maxIndex = 0;
        double maxMagnitude = magnitude[0];
        
        for (int i = 1; i < nyquistBin; i++) {
            if (magnitude[i] > maxMagnitude) {
                maxMagnitude = magnitude[i];
                maxIndex = i;
            }
        }
        
        return maxIndex * frequencyResolution;
    }
    
    /**
     * 计算信号的有效带宽
     * 返回包含95%能量的频带范围
     */
    public static double[] getEffectiveBandwidth(double[] signal, double samplingRate) {
        if (signal == null || signal.length == 0 || samplingRate <= 0) {
            return new double[]{0, 0};
        }
        
        double[] magnitude = FFT.abs(signal);
        double[] psd = new double[magnitude.length];
        
        // 计算功率谱密度
        for (int i = 0; i < magnitude.length; i++) {
            psd[i] = magnitude[i] * magnitude[i];
        }
        
        // 计算总能量
        double totalEnergy = 0;
        int nyquistBin = magnitude.length / 2;
        for (int i = 0; i < nyquistBin; i++) {
            totalEnergy += psd[i];
        }
        
        // 找到95%能量的频带范围
        double targetEnergy = totalEnergy * 0.95;
        double cumulativeEnergy = 0;
        int startBin = 0, endBin = nyquistBin - 1;
        
        // 找到起始频率
        for (int i = 0; i < nyquistBin; i++) {
            cumulativeEnergy += psd[i];
            if (cumulativeEnergy >= totalEnergy * 0.025) { // 2.5%起始点
                startBin = i;
                break;
            }
        }
        
        // 找到结束频率
        cumulativeEnergy = 0;
        for (int i = nyquistBin - 1; i >= 0; i--) {
            cumulativeEnergy += psd[i];
            if (cumulativeEnergy >= totalEnergy * 0.025) { // 2.5%结束点
                endBin = i;
                break;
            }
        }
        
        double frequencyResolution = samplingRate / signal.length;
        return new double[]{
            startBin * frequencyResolution,
            endBin * frequencyResolution
        };
    }
}

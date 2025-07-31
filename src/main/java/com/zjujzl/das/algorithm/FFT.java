package com.zjujzl.das.algorithm;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

/**
 * 快速傅里叶变换（Fast Fourier Transform, FFT）算法
 * 
 * 数学原理：
 * 1. 离散傅里叶变换（DFT）定义：
 *    X(k) = Σ(n=0 to N-1) x(n) * e^(-j*2π*k*n/N)
 *    其中：k = 0, 1, ..., N-1
 * 
 * 2. 欧拉公式展开：
 *    X(k) = Σ(n=0 to N-1) x(n) * [cos(2πkn/N) - j*sin(2πkn/N)]
 *    实部：X_real(k) = Σ(n=0 to N-1) x(n) * cos(2πkn/N)
 *    虚部：X_imag(k) = -Σ(n=0 to N-1) x(n) * sin(2πkn/N)
 * 
 * 3. 幅度谱计算：
 *    |X(k)| = √[X_real(k)² + X_imag(k)²]
 *    这表示频率k处的能量大小
 * 
 * 4. 相位谱计算：
 *    φ(k) = arctan(X_imag(k) / X_real(k))
 *    这表示频率k处的相位信息
 * 
 * 5. 频率分辨率：
 *    Δf = fs/N，其中fs为采样频率，N为信号长度
 * 
 * 6. 时间复杂度：O(N*log(N))，相比DFT的O(N²)大幅提升
 * 
 * 应用场景：频谱分析、滤波器设计、信号处理、噪声分析
 */
public class FFT {

    /**
     * 计算信号的幅度谱
     * 
     * 输入：实数信号 x(n)
     * 输出：幅度谱 |X(k)|
     * 
     * 算法流程：
     * 1. 实数信号 -> 复数信号（虚部为0）
     * 2. 执行FFT变换
     * 3. 计算幅度：|X(k)| = √(real² + imag²)
     */
    public static double[] abs(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        int n = signal.length;

        // 为FFT准备数据：实部+虚部交替存储
        // 格式：[real0, imag0, real1, imag1, ...]
        double[] fftData = Arrays.copyOf(signal, 2 * n);

        // 使用 JTransforms 执行实数FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        fft.realForwardFull(fftData);

        // 提取幅度谱：|X(k)| = √(real² + imag²)
        double[] magnitude = new double[n];
        for (int i = 0; i < n; i++) {
            double re = fftData[2 * i];     // 实部
            double im = fftData[2 * i + 1]; // 虚部
            magnitude[i] = Math.sqrt(re * re + im * im);
        }

        return magnitude;
    }
    
    /**
     * 计算信号的功率谱密度
     * 
     * 数学公式：
     * PSD(k) = |X(k)|² / N
     * 其中N为信号长度
     */
    public static double[] powerSpectralDensity(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        double[] magnitude = abs(signal);
        double[] psd = new double[magnitude.length];
        
        for (int i = 0; i < magnitude.length; i++) {
            psd[i] = magnitude[i] * magnitude[i] / signal.length;
        }
        
        return psd;
    }
    
    /**
     * 计算信号的相位谱
     * 
     * 数学公式：
     * φ(k) = arctan(X_imag(k) / X_real(k))
     * 范围：[-π, π]
     */
    public static double[] phase(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        int n = signal.length;
        double[] fftData = Arrays.copyOf(signal, 2 * n);
        
        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        fft.realForwardFull(fftData);
        
        double[] phaseSpectrum = new double[n];
        for (int i = 0; i < n; i++) {
            double re = fftData[2 * i];
            double im = fftData[2 * i + 1];
            phaseSpectrum[i] = Math.atan2(im, re);
        }
        
        return phaseSpectrum;
    }
    
    /**
     * 计算频率轴
     * 
     * 数学公式：
     * f(k) = k * fs / N
     * 其中：fs为采样频率，N为信号长度，k为频率索引
     */
    public static double[] getFrequencyAxis(int signalLength, double samplingRate) {
        double[] frequencies = new double[signalLength];
        double frequencyResolution = samplingRate / signalLength;
        
        for (int i = 0; i < signalLength; i++) {
            frequencies[i] = i * frequencyResolution;
        }
        
        return frequencies;
    }
    
    /**
     * 计算信号的频谱能量
     * 
     * 数学公式：
     * E_total = Σ(k=0 to N-1) |X(k)|²
     * 根据Parseval定理，频域能量等于时域能量
     */
    public static double getTotalEnergy(double[] signal) {
        if (signal == null || signal.length == 0) {
            return 0;
        }
        
        double[] magnitude = abs(signal);
        double totalEnergy = 0;
        
        for (double mag : magnitude) {
            totalEnergy += mag * mag;
        }
        
        return totalEnergy;
    }
    
    /**
     * 计算主频率成分
     * 
     * 返回幅度谱中最大值对应的频率索引
     */
    public static int getDominantFrequencyIndex(double[] signal) {
        if (signal == null || signal.length == 0) {
            return 0;
        }
        
        double[] magnitude = abs(signal);
        int maxIndex = 0;
        double maxValue = magnitude[0];
        
        for (int i = 1; i < magnitude.length; i++) {
            if (magnitude[i] > maxValue) {
                maxValue = magnitude[i];
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    /**
     * 计算信号的带宽
     * 
     * 定义为包含95%能量的频率范围
     */
    public static double[] getBandwidth(double[] signal, double energyThreshold) {
        if (signal == null || signal.length == 0) {
            return new double[]{0, 0};
        }
        
        double[] psd = powerSpectralDensity(signal);
        double totalEnergy = getTotalEnergy(signal);
        double targetEnergy = totalEnergy * energyThreshold;
        
        double cumulativeEnergy = 0;
        int startIndex = 0, endIndex = psd.length - 1;
        
        // 找到包含目标能量的频率范围
        for (int i = 0; i < psd.length; i++) {
            cumulativeEnergy += psd[i];
            if (cumulativeEnergy >= targetEnergy * 0.025) { // 2.5%起始点
                startIndex = i;
                break;
            }
        }
        
        cumulativeEnergy = 0;
        for (int i = psd.length - 1; i >= 0; i--) {
            cumulativeEnergy += psd[i];
            if (cumulativeEnergy >= targetEnergy * 0.025) { // 2.5%结束点
                endIndex = i;
                break;
            }
        }
        
        return new double[]{startIndex, endIndex};
    }
}
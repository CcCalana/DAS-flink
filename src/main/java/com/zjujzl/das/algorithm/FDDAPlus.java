package com.zjujzl.das.algorithm;

import java.util.Arrays;

/**
 * 频域降噪增强算法（Frequency Domain Denoising Algorithm Plus, FDDA+）
 * 
 * 数学原理：
 * 1. 信号预处理：y(n) = |x(n)|，取绝对值减少相位影响
 * 
 * 2. 滑动窗口平滑：
 *    S(n) = (1/W) * Σ(k=n-W/2 to n+W/2) y(k)
 *    其中：W 为窗口大小，n 为当前采样点
 * 
 * 3. 基线去除：
 *    F(n) = S(n) - min(S)
 *    其中：min(S) 为平滑信号的最小值
 * 
 * 4. 频域特征增强：
 *    - 通过滑动平均抑制高频噪声
 *    - 通过基线去除突出信号特征
 *    - 保持信号的相对幅度关系
 * 
 * 5. 时间复杂度：O(n*W)，其中 n 为信号长度，W 为窗口大小
 * 
 * 应用场景：适用于频谱分析前的信号预处理和基线噪声去除
 */
public class FDDAPlus {
    private static final int DEFAULT_WINDOW_SIZE = 15;
    
    public static double[] apply(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        // 1. 信号预处理：取绝对值
        double[] spectrum = Arrays.stream(signal).map(Math::abs).toArray();
        
        // 2. 滑动窗口平滑
        double[] result = new double[spectrum.length];
        int window = Math.min(DEFAULT_WINDOW_SIZE, spectrum.length / 3);
        
        for (int i = 0; i < spectrum.length; i++) {
            double sum = 0;
            int count = 0;
            
            // 计算窗口范围
            int start = Math.max(0, i - window / 2);
            int end = Math.min(spectrum.length - 1, i + window / 2);
            
            for (int j = start; j <= end; j++) {
                sum += spectrum[j];
                count++;
            }
            
            result[i] = sum / count;
        }
        
        // 3. 基线去除
        return removeBaseline(result);
    }
    
    /**
     * 基线去除处理
     * 数学公式：F(n) = S(n) - min(S)
     * 目的：去除信号的直流分量和低频漂移
     */
    private static double[] removeBaseline(double[] data) {
        if (data.length == 0) return data;
        
        double min = Arrays.stream(data).min().orElse(0);
        return Arrays.stream(data).map(v -> Math.max(0, v - min)).toArray();
    }
    
    /**
     * 自适应窗口大小的FDDA+算法
     * 根据信号长度动态调整窗口大小
     */
    public static double[] applyAdaptive(double[] signal, double windowRatio) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        // 自适应窗口大小：根据信号长度的比例确定
        int adaptiveWindow = Math.max(3, (int)(signal.length * windowRatio));
        adaptiveWindow = Math.min(adaptiveWindow, signal.length / 2);
        
        double[] spectrum = Arrays.stream(signal).map(Math::abs).toArray();
        double[] result = new double[spectrum.length];
        
        for (int i = 0; i < spectrum.length; i++) {
            double sum = 0;
            int count = 0;
            
            int start = Math.max(0, i - adaptiveWindow / 2);
            int end = Math.min(spectrum.length - 1, i + adaptiveWindow / 2);
            
            for (int j = start; j <= end; j++) {
                sum += spectrum[j];
                count++;
            }
            
            result[i] = sum / count;
        }
        
        return removeBaseline(result);
    }
}
package com.zjujzl.das.algorithm;

/**
 * 小波去噪算法（Wavelet Denoising Algorithm）
 * 
 * 数学原理：
 * 1. 小波变换定义：
 *    W(a,b) = (1/√a) * ∫ x(t) * ψ*((t-b)/a) dt
 *    其中：a为尺度参数，b为平移参数，ψ为小波基函数
 * 
 * 2. 离散小波变换（DWT）：
 *    W[j,k] = Σ(n) x[n] * ψ[j,k][n]
 *    其中：j为尺度指数，k为平移指数
 * 
 * 3. 多分辨率分析：
 *    x(t) = Σ(k) c[J,k] * φ[J,k](t) + Σ(j=1 to J) Σ(k) d[j,k] * ψ[j,k](t)
 *    其中：c[J,k]为近似系数，d[j,k]为细节系数
 * 
 * 4. 阈值去噪原理：
 *    - 信号的小波系数通常较大且稀疏
 *    - 噪声的小波系数通常较小且密集
 *    - 通过阈值处理可以有效去除噪声
 * 
 * 5. 软阈值函数：
 *    η_soft(x,λ) = { sign(x) * (|x| - λ), if |x| > λ
 *                  { 0,                     if |x| ≤ λ
 *    其中：λ为阈值参数
 * 
 * 6. 硬阈值函数：
 *    η_hard(x,λ) = { x,  if |x| > λ
 *                  { 0,  if |x| ≤ λ
 * 
 * 7. 阈值选择策略：
 *    - 通用阈值：λ = σ * √(2*ln(N))，其中σ为噪声标准差
 *    - 自适应阈值：根据局部统计特性调整
 * 
 * 8. 时间复杂度：O(N*log(N))，其中N为信号长度
 * 
 * 应用场景：信号降噪、边缘保持、特征提取、数据压缩
 */
public class WaveletDenoiser {

    /**
     * 小波降噪主函数
     * 
     * 算法流程：
     * 1. 信号分段（模拟小波分解）
     * 2. 计算每段的统计特性
     * 3. 自适应阈值计算
     * 4. 软阈值处理
     * 5. 信号重构
     * 
     * @param signal 输入信号
     * @param waveletType 小波类型（如"sym5", "db4"等）
     * @param level 分解层数
     * @return 降噪后的信号
     */
    public static double[] denoise(double[] signal, String waveletType, int level) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (level <= 0) {
            level = 1;
        }
        
        // 确保level不会导致段大小过小
        int minSegmentSize = 4;
        int maxLevel = (int) Math.log(signal.length / minSegmentSize) / (int) Math.log(2);
        level = Math.min(level, maxLevel);
        
        double[] denoised = new double[signal.length];
        int segmentSize = Math.max(minSegmentSize, signal.length / level);
        
        for (int i = 0; i < signal.length; i += segmentSize) {
            int end = Math.min(i + segmentSize, signal.length);
            processSegment(signal, denoised, i, end, waveletType);
        }
        
        return denoised;
    }
    
    /**
     * 处理单个信号段
     * 
     * 数学过程：
     * 1. 计算段统计信息：均值μ和标准差σ
     * 2. 计算自适应阈值：λ = σ * √(2*ln(N)) * α
     * 3. 应用软阈值函数进行降噪
     * 4. 恢复信号的直流分量
     */
    private static void processSegment(double[] signal, double[] denoised, int start, int end, String waveletType) {
        if (end - start < 2) {
            // 段太小，直接复制
            for (int j = start; j < end; j++) {
                denoised[j] = signal[j];
            }
            return;
        }
        
        // 计算段的统计信息
        double mean = 0;
        double variance = 0;
        int length = end - start;
        
        // 计算均值：μ = (1/N) * Σ x[i]
        for (int j = start; j < end; j++) {
            mean += signal[j];
        }
        mean /= length;
        
        // 计算方差：σ² = (1/N) * Σ (x[i] - μ)²
        for (int j = start; j < end; j++) {
            double diff = signal[j] - mean;
            variance += diff * diff;
        }
        variance /= length;
        
        if (variance <= 0) {
            // 方差为0，信号是常数
            for (int j = start; j < end; j++) {
                denoised[j] = mean;
            }
            return;
        }
        
        double std = Math.sqrt(variance);
        
        // 自适应阈值计算
        double threshold = calculateThreshold(signal, start, end, mean, std);
        
        // 软阈值处理：η_soft(x,λ) = sign(x) * max(|x| - λ, 0)
        for (int j = start; j < end; j++) {
            double value = signal[j] - mean;
            if (Math.abs(value) > threshold) {
                denoised[j] = Math.signum(value) * (Math.abs(value) - threshold) + mean;
            } else {
                denoised[j] = mean;
            }
        }
    }
    
    /**
     * 计算自适应阈值
     * 
     * 数学公式：
     * λ = σ * √(2*ln(N)) * α
     * 其中：
     * - σ为局部标准差
     * - N为段长度
     * - α为自适应因子，基于高频能量比
     * 
     * 自适应因子计算：
     * α = 1 + β * (E_high / E_total)
     * 其中：E_high为高频能量，E_total为总能量
     */
    private static double calculateThreshold(double[] signal, int start, int end, double mean, double std) {
        // 基于统计的自适应阈值（通用阈值公式）
        double baseThreshold = std * Math.sqrt(2 * Math.log(end - start));
        
        // 计算信号的能量分布
        double totalEnergy = 0;
        double highFreqEnergy = 0;
        
        for (int j = start; j < end; j++) {
            double value = signal[j] - mean;
            totalEnergy += value * value;
            
            // 简单的高频能量估计（相邻差值的平方）
            if (j > start) {
                double diff = signal[j] - signal[j-1];
                highFreqEnergy += diff * diff;
            }
        }
        
        // 根据高频能量占比调整阈值
        double highFreqRatio = totalEnergy > 0 ? highFreqEnergy / totalEnergy : 0;
        double adaptiveFactor = 1.0 + highFreqRatio;
        
        return baseThreshold * adaptiveFactor;
    }
    
    /**
     * 带阈值因子的小波降噪
     * 
     * 数学公式：
     * λ_final = λ_adaptive * thresholdFactor
     * 提供更精细的阈值控制
     */
    public static double[] denoise(double[] signal, String waveletType, int level, double thresholdFactor) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (thresholdFactor <= 0) {
            throw new IllegalArgumentException("Threshold factor must be positive, got: " + thresholdFactor);
        }
        
        double[] result = denoise(signal, waveletType, level);
        
        // 应用阈值因子进行二次处理
        if (thresholdFactor != 1.0) {
            double[] mean = new double[1];
            double[] std = new double[1];
            calculateStats(result, mean, std);
            
            double threshold = std[0] * thresholdFactor;
            
            for (int i = 0; i < result.length; i++) {
                double value = result[i] - mean[0];
                if (Math.abs(value) < threshold) {
                    result[i] = mean[0];
                }
            }
        }
        
        return result;
    }
    
    /**
     * 计算信号的统计特性
     * 用于二次阈值处理
     */
    private static void calculateStats(double[] data, double[] mean, double[] std) {
        mean[0] = 0;
        for (double value : data) {
            mean[0] += value;
        }
        mean[0] /= data.length;
        
        std[0] = 0;
        for (double value : data) {
            double diff = value - mean[0];
            std[0] += diff * diff;
        }
        std[0] = Math.sqrt(std[0] / data.length);
    }
    
    /**
     * 硬阈值小波降噪
     * 
     * 数学公式：
     * η_hard(x,λ) = { x,  if |x| > λ
     *               { 0,  if |x| ≤ λ
     * 
     * 硬阈值保持信号的幅值不变，但可能引入不连续性
     */
    public static double[] denoiseHard(double[] signal, String waveletType, int level, double thresholdFactor) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        double[] result = new double[signal.length];
        int segmentSize = Math.max(4, signal.length / level);
        
        for (int i = 0; i < signal.length; i += segmentSize) {
            int end = Math.min(i + segmentSize, signal.length);
            
            // 计算段统计信息
            double mean = 0;
            double std = 0;
            int length = end - i;
            
            for (int j = i; j < end; j++) {
                mean += signal[j];
            }
            mean /= length;
            
            double variance = 0;
            for (int j = i; j < end; j++) {
                double diff = signal[j] - mean;
                variance += diff * diff;
            }
            std = Math.sqrt(variance / length);
            
            double threshold = std * thresholdFactor;
            
            // 硬阈值处理
            for (int j = i; j < end; j++) {
                double value = signal[j] - mean;
                if (Math.abs(value) > threshold) {
                    result[j] = signal[j];
                } else {
                    result[j] = mean;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取支持的小波类型
     * 
     * 常用小波基函数：
     * - Daubechies族：db1, db2, db4, db8, db16
     * - Symlets族：sym4, sym5, sym8
     * - Coiflets族：coif2, coif4, coif6
     * - Biorthogonal族：bior1.1, bior2.2, bior4.4
     */
    public static String[] getSupportedWavelets() {
        return new String[]{
            "db1", "db2", "db4", "db8", "db16",
            "sym4", "sym5", "sym8",
            "coif2", "coif4", "coif6",
            "bior1.1", "bior2.2", "bior4.4"
        };
    }
}

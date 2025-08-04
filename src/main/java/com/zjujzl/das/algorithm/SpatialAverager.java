package com.zjujzl.das.algorithm;

/**
 * 空间平均算法（Spatial Averager）
 * 
 * 数学原理：
 * 1. 滑动窗口平均滤波器：
 *    y(n) = (1/W) * Σ(k=n-W/2 to n+W/2) x(k)
 *    其中：W 为窗口大小（必须为奇数），n 为当前采样点
 * 
 * 2. 边界处理：
 *    - 对于 n < W/2 的点：使用可用的左侧点进行平均
 *    - 对于 n > N-W/2 的点：使用可用的右侧点进行平均
 *    - 其中 N 为信号长度
 * 
 * 3. 频域特性：
 *    H(ω) = sin(ωW/2) / (W * sin(ω/2))
 *    这是一个低通滤波器，截止频率约为 fs/(2W)
 * 
 * 4. 噪声抑制效果：
 *    - 白噪声功率减少因子：1/W
 *    - 信噪比提升：10*log10(W) dB
 * 
 * 5. 时间复杂度：O(n*W)，其中 n 为信号长度，W 为窗口大小
 * 
 * 应用场景：低通滤波、噪声平滑、信号去毛刺
 */
public class SpatialAverager {
    
    public static double[] apply(double[] signal, int windowSize) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (windowSize % 2 == 0) {
            windowSize++; // 窗口大小必须为奇数
        }
        
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive, got: " + windowSize);
        }
        
        if (windowSize > signal.length / 2) {
            windowSize = signal.length / 2; // 对于过大的窗口，限制其大小
            if (windowSize % 2 == 0) windowSize--;
        }
        
        int halfWindow = windowSize / 2;
        double[] result = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            // 计算窗口范围，处理边界情况
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(signal.length - 1, i + halfWindow);
            
            // 计算窗口内的平均值
            double sum = 0.0;
            int count = 0;
            for (int j = start; j <= end; j++) {
                sum += signal[j];
                count++;
            }
            result[i] = sum / count;
        }
        
        return result;
    }
    
    /**
     * 自适应窗口大小的空间平均算法
     * 根据局部信号变化调整窗口大小
     * 
     * 数学原理：
     * 1. 局部方差计算：σ²(n) = (1/W) * Σ(k=n-W/2 to n+W/2) [x(k) - μ(n)]²
     * 2. 自适应窗口：W_adaptive = W_base / (1 + α * σ²(n))
     *    其中：α 为调节参数，σ²(n) 为局部方差
     */
    public static double[] applyAdaptive(double[] signal, int baseWindowSize, double adaptationFactor) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (baseWindowSize % 2 == 0) {
            throw new IllegalArgumentException("Base window size must be odd");
        }
        
        double[] result = new double[signal.length];
        double threshold = 1.0; // 方差阈值
        int maxWindow = Math.min(baseWindowSize * 3, signal.length / 4);
        if (maxWindow % 2 == 0) maxWindow--;
        
        for (int i = 0; i < signal.length; i++) {
            // 计算局部方差
            double variance = calculateLocalVariance(signal, i, baseWindowSize);
            
            // 自适应窗口大小
            int currentWindow = (variance > threshold) ? 
                Math.max(3, (int)(baseWindowSize / (1 + adaptationFactor * variance))) : baseWindowSize;
            if (currentWindow % 2 == 0) currentWindow--;
            currentWindow = Math.min(currentWindow, maxWindow);
            
            // 应用自适应窗口进行平均
            result[i] = applyWindow(signal, i, currentWindow);
        }
        
        return result;
    }
    
    /**
     * 对指定位置应用窗口平均
     */
    private static double applyWindow(double[] signal, int center, int windowSize) {
        int halfWindow = windowSize / 2;
        int start = Math.max(0, center - halfWindow);
        int end = Math.min(signal.length - 1, center + halfWindow);
        
        double sum = 0.0;
        int count = 0;
        for (int j = start; j <= end; j++) {
            sum += signal[j];
            count++;
        }
        return sum / count;
    }
    
    /**
     * 计算局部方差
     * 用于自适应窗口大小调整
     */
    private static double calculateLocalVariance(double[] signal, int center, int windowSize) {
        int halfWindow = windowSize / 2;
        int start = Math.max(0, center - halfWindow);
        int end = Math.min(signal.length - 1, center + halfWindow);
        
        // 计算均值
        double mean = 0;
        int count = 0;
        for (int i = start; i <= end; i++) {
            mean += signal[i];
            count++;
        }
        mean /= count;
        
        // 计算方差
        double variance = 0;
        for (int i = start; i <= end; i++) {
            double diff = signal[i] - mean;
            variance += diff * diff;
        }
        
        return variance / count;
    }
}
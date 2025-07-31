package com.zjujzl.das.algorithm;

import java.util.Arrays;

/**
 * 移动微分算法（Moving Differentiator）
 * 
 * 数学原理：
 * 1. 一阶差分公式：
 *    y(n) = x(n) - x(n-1)
 *    这是数值微分的简单近似：dy/dx ≈ Δy/Δx
 * 
 * 2. 高阶差分公式：
 *    对于k阶差分：Δ^k[x(n)] = Δ^(k-1)[x(n)] - Δ^(k-1)[x(n-1)]
 *    递推关系：D^k(n) = D^(k-1)(n) - D^(k-1)(n-1)
 * 
 * 3. 频域特性：
 *    H(ω) = 1 - e^(-jω) = 1 - cos(ω) + j*sin(ω)
 *    |H(ω)| = 2*sin(ω/2)  （幅度响应）
 *    这是一个高通滤波器，强调信号的变化成分
 * 
 * 4. 噪声敏感性：
 *    - 微分操作会放大高频噪声
 *    - 信噪比恶化程度与频率成正比
 *    - 适用于检测信号的突变和边缘
 * 
 * 5. 边界条件：
 *    y(0) = 0  （第一个点设为0）
 * 
 * 6. 时间复杂度：O(n*k)，其中 n 为信号长度，k 为差分阶数
 * 
 * 应用场景：边缘检测、信号变化检测、冲击性噪声增强
 */
public class MovingDifferentiator {
    
    public static double[] apply(double[] signal, int order) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (order <= 0) {
            throw new IllegalArgumentException("Order must be positive, got: " + order);
        }
        
        if (order >= signal.length) {
            // 如果阶数过高，返回零信号
            return new double[signal.length];
        }
        
        double[] result = Arrays.copyOf(signal, signal.length);
        
        // 逐阶进行差分运算
        for (int currentOrder = 0; currentOrder < order; currentOrder++) {
            double[] temp = new double[result.length];
            
            // 第一个点设为0（边界条件）
            temp[0] = 0;
            
            // 计算差分：y(n) = x(n) - x(n-1)
            for (int i = 1; i < result.length; i++) {
                temp[i] = result[i] - result[i - 1];
            }
            
            result = temp;
        }
        
        return result;
    }
    
    /**
     * 中心差分算法
     * 使用中心差分公式提高精度
     * 
     * 数学公式：
     * y(n) = [x(n+1) - x(n-1)] / 2
     * 这是二阶精度的数值微分近似
     */
    public static double[] applyCentral(double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (signal.length < 3) {
            // 信号太短，使用前向差分
            return apply(signal, 1);
        }
        
        double[] result = new double[signal.length];
        
        // 边界点使用前向差分
        result[0] = signal[1] - signal[0];
        result[signal.length - 1] = signal[signal.length - 1] - signal[signal.length - 2];
        
        // 中间点使用中心差分
        for (int i = 1; i < signal.length - 1; i++) {
            result[i] = (signal[i + 1] - signal[i - 1]) / 2.0;
        }
        
        return result;
    }
    
    /**
     * 后向差分算法
     * 适用于因果系统和实时处理
     * 
     * 数学公式：
     * y(n) = x(n) - x(n-1)
     * 等价于标准的移动微分
     */
    public static double[] applyBackward(double[] signal, int order) {
        return apply(signal, order);
    }
    
    /**
     * 前向差分算法
     * 适用于离线处理和预测
     * 
     * 数学公式：
     * y(n) = x(n+1) - x(n)
     */
    public static double[] applyForward(double[] signal, int order) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (order <= 0) {
            throw new IllegalArgumentException("Order must be positive, got: " + order);
        }
        
        double[] result = Arrays.copyOf(signal, signal.length);
        
        for (int currentOrder = 0; currentOrder < order; currentOrder++) {
            double[] temp = new double[result.length];
            
            // 计算前向差分
            for (int i = 0; i < result.length - 1; i++) {
                temp[i] = result[i + 1] - result[i];
            }
            
            // 最后一个点设为0（边界条件）
            temp[result.length - 1] = 0;
            
            result = temp;
        }
        
        return result;
    }
    
    /**
     * 自适应阶数的移动微分
     * 根据信号特征自动选择合适的差分阶数
     * 
     * 选择策略：
     * - 平滑信号：低阶差分
     * - 快变信号：高阶差分
     * - 基于信号的统计特性进行判断
     */
    public static double[] applyAdaptive(double[] signal, int maxOrder) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        // 计算信号的变化率
        double[] firstDiff = apply(signal, 1);
        double changeRate = calculateChangeRate(firstDiff);
        
        // 根据变化率选择阶数
        int adaptiveOrder = Math.max(1, Math.min(maxOrder, (int)(changeRate * maxOrder)));
        
        return apply(signal, adaptiveOrder);
    }
    
    /**
     * 计算信号的变化率
     * 用于自适应阶数选择
     */
    private static double calculateChangeRate(double[] signal) {
        if (signal.length == 0) return 0;
        
        double totalChange = 0;
        double maxValue = Double.MIN_VALUE;
        
        for (double value : signal) {
            totalChange += Math.abs(value);
            maxValue = Math.max(maxValue, Math.abs(value));
        }
        
        return maxValue > 0 ? totalChange / (signal.length * maxValue) : 0;
    }
}
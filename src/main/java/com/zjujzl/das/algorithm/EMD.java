package com.zjujzl.das.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 经验模态分解（Empirical Mode Decomposition, EMD）算法
 * 
 * 数学原理：
 * 1. 信号分解公式：x(t) = Σ(i=1 to n) c_i(t) + r_n(t)
 *    其中：c_i(t) 为第i个固有模态函数（IMF），r_n(t) 为残差
 * 
 * 2. IMF提取过程（筛选过程）：
 *    a) 找到信号的局部极值点（极大值和极小值）
 *    b) 构造上包络线 e_max(t) 和下包络线 e_min(t)
 *    c) 计算平均包络线：m(t) = [e_max(t) + e_min(t)] / 2
 *    d) 更新信号：h(t) = x(t) - m(t)
 *    e) 重复步骤a-d直到满足IMF条件
 * 
 * 3. IMF判定条件：
 *    a) 极值点数量条件：|N_max - N_min| ≤ 1
 *    b) 停止条件：SD = Σ|h_k(t) - h_{k-1}(t)|² / Σ|h_{k-1}(t)|² < tolerance
 * 
 * 4. 包络插值（简化为线性插值）：
 *    y(x) = y_1 + (y_2 - y_1) * (x - x_1) / (x_2 - x_1)
 * 
 * 应用场景：非线性、非平稳信号的时频分析和降噪
 */
public class EMD {
    private static final double TOLERANCE = 0.01;
    private static final int MAX_ITERATIONS = 100;
    
    public static List<double[]> decompose(double[] signal, int maxImfCount) {
        List<double[]> imfs = new ArrayList<>();
        double[] residue = Arrays.copyOf(signal, signal.length);
        
        for (int i = 0; i < maxImfCount; i++) {
            double[] imf = extractIMF(residue);
            if (imf == null) break;
            
            imfs.add(imf);
            // 更新残差
            for (int j = 0; j < residue.length; j++) {
                residue[j] -= imf[j];
            }
            
            // 检查是否为单调函数
            if (isMonotonic(residue)) {
                imfs.add(Arrays.copyOf(residue, residue.length));
                break;
            }
        }
        
        return imfs;
    }
    
    private static double[] extractIMF(double[] signal) {
        double[] h = Arrays.copyOf(signal, signal.length);
        
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            List<Integer> maxima = findLocalMaxima(h);
            List<Integer> minima = findLocalMinima(h);
            
            if (maxima.size() < 2 || minima.size() < 2) {
                return null;
            }
            
            double[] upperEnvelope = cubicSplineInterpolation(maxima, h);
            double[] lowerEnvelope = cubicSplineInterpolation(minima, h);
            
            double[] mean = new double[h.length];
            for (int i = 0; i < h.length; i++) {
                mean[i] = (upperEnvelope[i] + lowerEnvelope[i]) / 2.0;
            }
            
            double[] newH = new double[h.length];
            for (int i = 0; i < h.length; i++) {
                newH[i] = h[i] - mean[i];
            }
            
            // 检查停止条件
            if (isImf(newH) || calculateSD(h, newH) < TOLERANCE) {
                return newH;
            }
            
            h = newH;
        }
        
        return h;
    }
    
    private static List<Integer> findLocalMaxima(double[] data) {
        List<Integer> maxima = new ArrayList<>();
        for (int i = 1; i < data.length - 1; i++) {
            if (data[i] > data[i-1] && data[i] > data[i+1]) {
                maxima.add(i);
            }
        }
        return maxima;
    }
    
    private static List<Integer> findLocalMinima(double[] data) {
        List<Integer> minima = new ArrayList<>();
        for (int i = 1; i < data.length - 1; i++) {
            if (data[i] < data[i-1] && data[i] < data[i+1]) {
                minima.add(i);
            }
        }
        return minima;
    }
    
    private static double[] cubicSplineInterpolation(List<Integer> points, double[] values) {
        // 简化的线性插值实现
        double[] result = new double[values.length];
        
        if (points.size() < 2) {
            Arrays.fill(result, 0);
            return result;
        }
        
        for (int i = 0; i < values.length; i++) {
            if (i <= points.get(0)) {
                result[i] = values[points.get(0)];
            } else if (i >= points.get(points.size() - 1)) {
                result[i] = values[points.get(points.size() - 1)];
            } else {
                // 线性插值
                for (int j = 0; j < points.size() - 1; j++) {
                    if (i >= points.get(j) && i <= points.get(j + 1)) {
                        int x1 = points.get(j);
                        int x2 = points.get(j + 1);
                        double y1 = values[x1];
                        double y2 = values[x2];
                        result[i] = y1 + (y2 - y1) * (i - x1) / (x2 - x1);
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    private static boolean isImf(double[] data) {
        List<Integer> maxima = findLocalMaxima(data);
        List<Integer> minima = findLocalMinima(data);
        return Math.abs(maxima.size() - minima.size()) <= 1;
    }
    
    private static boolean isMonotonic(double[] data) {
        boolean increasing = true;
        boolean decreasing = true;
        
        for (int i = 1; i < data.length; i++) {
            if (data[i] > data[i-1]) decreasing = false;
            if (data[i] < data[i-1]) increasing = false;
        }
        
        return increasing || decreasing;
    }
    
    private static double calculateSD(double[] h1, double[] h2) {
        double sum = 0;
        for (int i = 0; i < h1.length; i++) {
            sum += Math.pow(h1[i] - h2[i], 2) / Math.pow(h1[i], 2);
        }
        return sum;
    }
}

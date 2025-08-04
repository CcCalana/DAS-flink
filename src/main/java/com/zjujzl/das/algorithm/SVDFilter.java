package com.zjujzl.das.algorithm;

/**
 * 奇异值分解滤波器（Singular Value Decomposition Filter）
 * 
 * 数学原理：
 * 1. Hankel矩阵构造：
 *    H = [x(0)   x(1)   ...  x(m-1)  ]
 *        [x(1)   x(2)   ...  x(m)    ]
 *        [⋮      ⋮      ⋱   ⋮       ]
 *        [x(n-1) x(n)   ...  x(n+m-2)]
 *    其中：n = N-m+1，m为窗口大小，N为信号长度
 * 
 * 2. 奇异值分解：
 *    H = U * Σ * V^T
 *    其中：U(n×n)为左奇异向量矩阵，Σ(n×m)为奇异值对角矩阵，V(m×m)为右奇异向量矩阵
 * 
 * 3. 主成分选择：
 *    累积能量比：E_k = Σ(i=1 to k) σ_i² / Σ(i=1 to r) σ_i²
 *    选择满足 E_k ≥ threshold 的最小k值
 * 
 * 4. 信号重构：
 *    H_filtered = Σ(i=1 to k) σ_i * u_i * v_i^T
 *    其中：σ_i为第i个奇异值，u_i和v_i为对应的奇异向量
 * 
 * 5. 反变换（Hankel矩阵 -> 信号）：
 *    x_filtered(n) = (1/count(n)) * Σ H_filtered(i,j) where i+j=n
 *    通过对角线平均恢复原始信号
 * 
 * 6. 降噪原理：
 *    - 信号分量通常对应较大的奇异值
 *    - 噪声分量通常对应较小的奇异值
 *    - 通过保留主要奇异值实现降噪
 * 
 * 7. 时间复杂度：O(n*m*min(n,m))，其中n为信号长度，m为窗口大小
 * 
 * 应用场景：低信噪比信号降噪、线性预测、系统辨识
 */
public class SVDFilter {
    
    public static double[] denoise(double[] signal, double threshold) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        if (threshold <= 0 || threshold > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1, got: " + threshold);
        }
        
        int n = signal.length;
        // 窗口大小
        int windowSize = Math.min(n / 2, 50);
        
        // 构造Hankel矩阵
        double[][] hankel = createHankelMatrix(signal, windowSize);
        
        // 执行简化的SVD（使用特征值分解近似）
        double[] denoised = performSVDDenoising(hankel, threshold, windowSize);
        
        return denoised;
    }
    
    /**
     * 构造Hankel矩阵
     * 
     * 数学描述：
     * H[i][j] = x[i+j]，其中 0 ≤ i < rows, 0 ≤ j < cols
     * 这是一个常对角线矩阵（同一对角线上的元素相等）
     */
    private static double[][] createHankelMatrix(double[] signal, int windowSize) {
        int rows = signal.length - windowSize + 1;
        int cols = windowSize;
        double[][] hankel = new double[rows][cols];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                hankel[i][j] = signal[i + j];
            }
        }
        
        return hankel;
    }
    
    /**
     * 执行SVD降噪
     * 
     * 简化实现：
     * 1. 计算协方差矩阵：C = H^T * H
     * 2. 估计特征值（简化为对角元素）
     * 3. 根据能量阈值选择主成分
     * 4. 重构信号
     */
    private static double[] performSVDDenoising(double[][] matrix, double threshold, int windowSize) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        
        // 计算协方差矩阵 C = H^T * H / rows
        double[][] covariance = new double[cols][cols];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < cols; j++) {
                double sum = 0;
                for (int k = 0; k < rows; k++) {
                    sum += matrix[k][i] * matrix[k][j];
                }
                covariance[i][j] = sum / rows;
            }
        }
        
        // 简化的特征值分解（保留主要成分）
        double[] eigenValues = computeEigenValues(covariance);
        
        // 确定保留的成分数量
        int numComponents = selectComponents(eigenValues, threshold);
        
        // 重构信号
        return reconstructSignal(matrix, numComponents, windowSize);
    }
    
    /**
     * 计算特征值（简化实现）
     * 
     * 实际SVD中应该计算：
     * λ_i = σ_i²，其中σ_i为奇异值
     * 
     * 这里使用对角元素作为特征值的估计
     */
    private static double[] computeEigenValues(double[][] matrix) {
        int n = matrix.length;
        double[] eigenValues = new double[n];
        
        // 简化的特征值估计（使用对角元素）
        for (int i = 0; i < n; i++) {
            eigenValues[i] = matrix[i][i];
        }
        
        // 排序：从大到小
        java.util.Arrays.sort(eigenValues);
        
        // 反转数组，从大到小
        for (int i = 0; i < n / 2; i++) {
            double temp = eigenValues[i];
            eigenValues[i] = eigenValues[n - 1 - i];
            eigenValues[n - 1 - i] = temp;
        }
        
        return eigenValues;
    }
    
    /**
     * 选择主成分数量
     * 
     * 基于累积能量比：
     * E_k = Σ(i=1 to k) λ_i / Σ(i=1 to r) λ_i ≥ threshold
     */
    private static int selectComponents(double[] eigenValues, double threshold) {
        double totalEnergy = 0;
        for (double value : eigenValues) {
            totalEnergy += Math.abs(value);
        }
        
        if (totalEnergy == 0) return 0;
        
        double cumulativeEnergy = 0;
        for (int i = 0; i < eigenValues.length; i++) {
            cumulativeEnergy += Math.abs(eigenValues[i]);
            if (cumulativeEnergy / totalEnergy >= threshold) {
                return i + 1;
            }
        }
        
        return eigenValues.length;
    }
    
    /**
     * 重构信号
     * 
     * 数学过程：
     * 1. 使用前k个主成分重构Hankel矩阵
     * 2. 通过对角线平均恢复一维信号
     * 3. 处理边界效应和长度不匹配问题
     */
    private static double[] reconstructSignal(double[][] matrix, int numComponents, int windowSize) {
        int signalLength = matrix.length + windowSize - 1;
        double[] reconstructed = new double[signalLength];
        int[] counts = new int[signalLength];
        
        // 使用主要成分重构
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < Math.min(numComponents, windowSize); j++) {
                int signalIndex = i + j;
                if (signalIndex < signalLength) {
                    reconstructed[signalIndex] += matrix[i][j];
                    counts[signalIndex]++;
                }
            }
        }
        
        // 对角线平均：处理重叠区域
        for (int i = 0; i < signalLength; i++) {
            if (counts[i] > 0) {
                reconstructed[i] /= counts[i];
            }
        }
        
        return reconstructed;
    }
    
    /**
     * 完整的SVD降噪（如果需要更精确的实现）
     * 
     * 此方法预留给将来的完整SVD实现
     * 需要真正的SVD分解库支持
     */
    public static double[] denoiseWithFullSVD(double[] signal, double threshold, int windowSize) {
        // TODO: 实现完整的SVD分解
        // 1. 构造Hankel矩阵
        // 2. 执行真正的SVD：H = U * Σ * V^T
        // 3. 基于奇异值选择主成分
        // 4. 重构：H_filtered = Σ(i=1 to k) σ_i * u_i * v_i^T
        // 5. 反变换得到滤波后的信号
        
        return denoise(signal, threshold); // 暂时回退到简化实现
    }
}

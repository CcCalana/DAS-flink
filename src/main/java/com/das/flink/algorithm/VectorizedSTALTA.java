package com.das.flink.algorithm;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * 向量化 STA/LTA 算法实现
 * 利用 SIMD 指令和并行计算优化性能
 * 
 * @author DAS-Flink Team
 */
public class VectorizedSTALTA {
    
    private static final int VECTOR_SIZE = 8; // 向量化处理单元大小
    private static final float EPSILON = 1e-10f; // 避免除零的小值
    
    private final ForkJoinPool forkJoinPool;
    private final MemoryPool memoryPool;
    
    public VectorizedSTALTA() {
        this.forkJoinPool = new ForkJoinPool();
        this.memoryPool = new MemoryPool();
    }
    
    /**
     * 向量化计算 STA/LTA 比值
     * 
     * @param data 输入数据数组
     * @param staLength STA 窗口长度
     * @param ltaLength LTA 窗口长度
     * @return STA/LTA 比值数组
     */
    public float[] computeSTALTA(float[] data, int staLength, int ltaLength) {
        if (data == null || data.length < ltaLength + staLength) {
            throw new IllegalArgumentException("数据长度不足");
        }
        
        int resultLength = data.length - ltaLength;
        float[] result = memoryPool.borrowFloatArray(resultLength);
        
        try {
            // 预计算累积和数组，用于快速窗口计算
            float[] cumulativeSum = computeCumulativeSum(data);
            
            // 并行向量化计算
            if (resultLength > 1000) {
                computeParallel(cumulativeSum, result, staLength, ltaLength);
            } else {
                computeSequential(cumulativeSum, result, staLength, ltaLength);
            }
            
            return Arrays.copyOf(result, resultLength);
        } finally {
            memoryPool.returnFloatArray(result);
        }
    }
    
    /**
     * 计算累积和数组
     */
    private float[] computeCumulativeSum(float[] data) {
        float[] cumSum = memoryPool.borrowFloatArray(data.length + 1);
        cumSum[0] = 0.0f;
        
        // 向量化累积和计算
        for (int i = 0; i < data.length; i += VECTOR_SIZE) {
            int end = Math.min(i + VECTOR_SIZE, data.length);
            
            for (int j = i; j < end; j++) {
                cumSum[j + 1] = cumSum[j] + data[j];
            }
        }
        
        return cumSum;
    }
    
    /**
     * 串行向量化计算
     */
    private void computeSequential(float[] cumulativeSum, float[] result, 
                                 int staLength, int ltaLength) {
        
        for (int i = ltaLength; i < cumulativeSum.length - staLength - 1; i += VECTOR_SIZE) {
            int end = Math.min(i + VECTOR_SIZE, cumulativeSum.length - staLength - 1);
            
            // 向量化计算 STA 和 LTA
            for (int j = i; j < end; j++) {
                int resultIndex = j - ltaLength;
                if (resultIndex >= 0 && resultIndex < result.length) {
                    float sta = computeWindowAverage(cumulativeSum, j, staLength);
                    float lta = computeWindowAverage(cumulativeSum, j - ltaLength + staLength, ltaLength);
                    
                    result[resultIndex] = sta / (lta + EPSILON);
                }
            }
        }
    }
    
    /**
     * 并行向量化计算
     */
    private void computeParallel(float[] cumulativeSum, float[] result, 
                               int staLength, int ltaLength) {
        
        int startIndex = ltaLength;
        int endIndex = cumulativeSum.length - staLength - 1;
        
        ParallelSTALTATask task = new ParallelSTALTATask(
            cumulativeSum, result, startIndex, endIndex, staLength, ltaLength);
        
        forkJoinPool.invoke(task);
    }
    
    /**
     * 快速窗口平均值计算
     */
    private float computeWindowAverage(float[] cumulativeSum, int endIndex, int windowLength) {
        int startIdx = Math.max(0, endIndex - windowLength + 1);
        int endIdx = Math.min(cumulativeSum.length - 1, endIndex + 1);
        
        if (startIdx >= endIdx || startIdx < 0 || endIdx >= cumulativeSum.length) {
            return 0.0f;
        }
        
        float sum = cumulativeSum[endIdx] - cumulativeSum[startIdx];
        return sum / windowLength;
    }
    
    /**
     * 并行计算任务
     */
    private class ParallelSTALTATask extends RecursiveTask<Void> {
        private static final int THRESHOLD = 256; // 分治阈值
        
        private final float[] cumulativeSum;
        private final float[] result;
        private final int startIndex;
        private final int endIndex;
        private final int staLength;
        private final int ltaLength;
        
        public ParallelSTALTATask(float[] cumulativeSum, float[] result,
                                int startIndex, int endIndex,
                                int staLength, int ltaLength) {
            this.cumulativeSum = cumulativeSum;
            this.result = result;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.staLength = staLength;
            this.ltaLength = ltaLength;
        }
        
        @Override
        protected Void compute() {
            int length = endIndex - startIndex;
            
            if (length <= THRESHOLD) {
                // 直接计算
                computeRange(startIndex, endIndex);
            } else {
                // 分治递归
                int mid = startIndex + length / 2;
                
                ParallelSTALTATask leftTask = new ParallelSTALTATask(
                    cumulativeSum, result, startIndex, mid, staLength, ltaLength);
                ParallelSTALTATask rightTask = new ParallelSTALTATask(
                    cumulativeSum, result, mid, endIndex, staLength, ltaLength);
                
                leftTask.fork();
                rightTask.compute();
                leftTask.join();
            }
            
            return null;
        }
        
        private void computeRange(int start, int end) {
            for (int i = start; i < end; i += VECTOR_SIZE) {
                int vectorEnd = Math.min(i + VECTOR_SIZE, end);
                
                for (int j = i; j < vectorEnd; j++) {
                    int resultIndex = j - ltaLength;
                    if (resultIndex >= 0 && resultIndex < result.length) {
                        float sta = computeWindowAverage(cumulativeSum, j, staLength);
                        float lta = computeWindowAverage(cumulativeSum, j - ltaLength + staLength, ltaLength);
                        
                        result[resultIndex] = sta / (lta + EPSILON);
                    }
                }
            }
        }
    }
    
    /**
     * 自适应阈值计算
     */
    public float computeAdaptiveThreshold(float[] staLtaRatios, float noiseLevel) {
        if (staLtaRatios == null || staLtaRatios.length == 0) {
            return 2.0f; // 默认阈值
        }
        
        // 计算统计特征
        float mean = computeMean(staLtaRatios);
        float std = computeStandardDeviation(staLtaRatios, mean);
        
        // 基于噪声水平和统计特征的自适应阈值
        float baseThreshold = mean + 2.0f * std;
        float noiseAdjustment = 1.0f + noiseLevel * 0.5f;
        
        return Math.max(baseThreshold * noiseAdjustment, 1.5f);
    }
    
    /**
     * 计算均值
     */
    private float computeMean(float[] data) {
        double sum = 0.0;
        for (float value : data) {
            sum += value;
        }
        return (float) (sum / data.length);
    }
    
    /**
     * 计算标准差
     */
    private float computeStandardDeviation(float[] data, float mean) {
        double sumSquaredDiff = 0.0;
        for (float value : data) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return (float) Math.sqrt(sumSquaredDiff / data.length);
    }
    
    /**
     * 多尺度 STA/LTA 计算
     */
    public MultiScaleResult computeMultiScale(float[] data, int baseStaLength, int baseLtaLength) {
        int[] scaleFactors = {1, 2, 4, 8};
        float[] weights = {0.4f, 0.3f, 0.2f, 0.1f};
        
        float[] fusedResult = new float[data.length - baseLtaLength];
        Arrays.fill(fusedResult, 0.0f);
        
        for (int i = 0; i < scaleFactors.length; i++) {
            int scaledSta = baseStaLength * scaleFactors[i];
            int scaledLta = baseLtaLength * scaleFactors[i];
            
            if (scaledLta < data.length) {
                float[] scaleResult = computeSTALTA(data, scaledSta, scaledLta);
                
                // 加权融合
                for (int j = 0; j < Math.min(fusedResult.length, scaleResult.length); j++) {
                    fusedResult[j] += weights[i] * scaleResult[j];
                }
            }
        }
        
        return new MultiScaleResult(fusedResult, scaleFactors, weights);
    }
    
    /**
     * 释放资源
     */
    public void shutdown() {
        forkJoinPool.shutdown();
        memoryPool.clear();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            try {
                if (!forkJoinPool.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    forkJoinPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                forkJoinPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 多尺度结果类
     */
    public static class MultiScaleResult {
        private final float[] fusedRatios;
        private final int[] scaleFactors;
        private final float[] weights;
        
        public MultiScaleResult(float[] fusedRatios, int[] scaleFactors, float[] weights) {
            this.fusedRatios = fusedRatios;
            this.scaleFactors = scaleFactors;
            this.weights = weights;
        }
        
        public float[] getFusedRatios() { return fusedRatios; }
        public int[] getScaleFactors() { return scaleFactors; }
        public float[] getWeights() { return weights; }
        
        public float getMaxRatio() {
            float max = Float.NEGATIVE_INFINITY;
            for (float ratio : fusedRatios) {
                if (ratio > max) max = ratio;
            }
            return max;
        }
        
        public int getMaxRatioIndex() {
            float max = Float.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int i = 0; i < fusedRatios.length; i++) {
                if (fusedRatios[i] > max) {
                    max = fusedRatios[i];
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    }
}
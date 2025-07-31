package com.zjujzl.das.profile;

import com.zjujzl.das.algorithm.*;
import com.zjujzl.das.model.SeismicRecord;

import java.util.*;

public class SmartRouter {

    // 配置参数
    private static final Map<String, Double> defaultWeights = new HashMap<>();
    private static final Map<String, Double> performanceWeights = new HashMap<>();
    
    static {
        // 默认权重
        defaultWeights.put("SA", 0.3);
        defaultWeights.put("MD", 0.3);
        defaultWeights.put("FDDA", 0.4);
        defaultWeights.put("Wavelet", 0.5);
        
        // 性能权重（基于处理时间的倒数）
        performanceWeights.put("SA", 0.9);      // 空间平均处理快
        performanceWeights.put("MD", 0.8);      // 移动微分处理快
        performanceWeights.put("FDDA", 0.7);    // 频域处理中等
        performanceWeights.put("Wavelet", 0.6); // 小波处理较慢
    }

    public static double[] process(NoiseProfile profile, double[] signal) {
        if (signal == null || signal.length == 0) {
            return new double[0];
        }
        
        try {
            Map<String, Double> weights = adjustWeights(profile, signal.length);
            return applyWeightedAlgorithms(signal, weights);
        } catch (Exception e) {
            System.err.println("ERROR: SmartRouter failed, falling back to FDDA: " + e.getMessage());
            return FDDAPlus.apply(signal);
        }
    }

    private static Map<String, Double> adjustWeights(NoiseProfile profile, int signalLength) {
        Map<String, Double> weights = new HashMap<>(defaultWeights);
        
        // 根据信号长度调整权重
        if (signalLength > 10000) {
            // 长信号，倾向于使用较快的算法
            weights.put("SA", weights.get("SA") * 1.3);
            weights.put("MD", weights.get("MD") * 1.2);
            weights.put("Wavelet", weights.get("Wavelet") * 0.8);
        } else if (signalLength < 1000) {
            // 短信号，可以使用更复杂的算法
            weights.put("Wavelet", weights.get("Wavelet") * 1.3);
            weights.put("SA", weights.get("SA") * 0.9);
        }

        // 根据噪声类型调整权重
        if (profile.hasImpulsiveNoise()) {
            // 冲击性噪声：移动微分和小波更有效
            weights.put("MD", weights.get("MD") * 1.8);
            weights.put("Wavelet", weights.get("Wavelet") * 1.5);
            weights.put("SA", weights.get("SA") * 0.7);
        }
        
        if (profile.hasPeriodicNoise()) {
            // 周期性噪声：频域处理更有效
            weights.put("FDDA", weights.get("FDDA") * 1.6);
            weights.put("SA", weights.get("SA") * 1.2);
            weights.put("Wavelet", weights.get("Wavelet") * 0.9);
        }
        
        if (profile.hasLowFreqNoise()) {
            // 低频噪声：空间平均更有效
            weights.put("SA", weights.get("SA") * 1.8);
            weights.put("MD", weights.get("MD") * 0.5);
            weights.put("FDDA", weights.get("FDDA") * 1.1);
        }
        
        // 应用性能权重
        for (String algorithm : weights.keySet()) {
            Double perfWeight = performanceWeights.get(algorithm);
            if (perfWeight != null) {
                weights.put(algorithm, weights.get(algorithm) * perfWeight);
            }
        }

        // 归一化权重
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            for (String key : weights.keySet()) {
                weights.put(key, weights.get(key) / sum);
            }
        }
        
        return weights;
    }

    private static double[] applyWeightedAlgorithms(double[] signal, Map<String, Double> weights) {
        Map<String, double[]> results = new HashMap<>();
        Map<String, Long> processingTimes = new HashMap<>();

        // 并行处理算法
        weights.keySet().parallelStream().forEach(algorithm -> {
            try {
                long startTime = System.currentTimeMillis();
                double[] result = applyAlgorithm(algorithm, signal);
                long endTime = System.currentTimeMillis();
                
                synchronized (results) {
                    results.put(algorithm, result);
                    processingTimes.put(algorithm, endTime - startTime);
                }
            } catch (Exception e) {
                System.err.println("ERROR: Algorithm " + algorithm + " failed: " + e.getMessage());
                // 失败的算法不参与结果合并
            }
        });

        // 打印性能统计
        printPerformanceStats(processingTimes, weights);
        
        if (results.isEmpty()) {
            System.err.println("WARN: All algorithms failed, returning original signal");
            return signal.clone();
        }

        return combineResults(results, weights, signal.length);
    }

    private static double[] applyAlgorithm(String algorithm, double[] signal) {
        try {
            switch (algorithm) {
                case "SA":
                    return SpatialAverager.apply(signal, Math.min(21, signal.length / 10));
                case "MD":
                    return MovingDifferentiator.apply(signal, 1);
                case "FDDA":
                    return FDDAPlus.apply(signal);
                case "Wavelet":
                    return WaveletDenoiser.denoise(signal, "sym5", 
                            Math.min(4, (int) Math.log(signal.length) / (int) Math.log(2)));
                default:
                    System.err.println("WARN: Unknown algorithm: " + algorithm);
                    return signal.clone();
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to apply algorithm " + algorithm + ": " + e.getMessage());
            return signal.clone();
        }
    }

    private static double[] combineResults(Map<String, double[]> results,
                                           Map<String, Double> weights,
                                           int length) {
        double[] combined = new double[length];
        double totalWeight = 0;

        for (Map.Entry<String, double[]> entry : results.entrySet()) {
            String algorithm = entry.getKey();
            double[] result = entry.getValue();
            Double weight = weights.get(algorithm);
            
            if (weight != null && result != null) {
                totalWeight += weight;
                for (int i = 0; i < length; i++) {
                    if (i < result.length) {
                        combined[i] += result[i] * weight;
                    } else {
                        // 处理长度不匹配的情况
                        combined[i] += result[result.length - 1] * weight;
                    }
                }
            }
        }

        // 归一化
        if (totalWeight > 0) {
            for (int i = 0; i < length; i++) {
                combined[i] /= totalWeight;
            }
        }

        return combined;
    }
    
    private static void printPerformanceStats(Map<String, Long> processingTimes, Map<String, Double> weights) {
        System.out.println("=== SmartRouter Performance Stats ===");
        for (String algorithm : processingTimes.keySet()) {
            Long time = processingTimes.get(algorithm);
            Double weight = weights.get(algorithm);
            System.out.println(String.format("%s: %dms (weight: %.3f)", algorithm, time, weight));
        }
        System.out.println("=====================================");
    }

    public static double[] routeAndDenoise(SeismicRecord signal) {
        if (signal == null || signal.data == null || signal.data.length == 0) {
            return new double[0];
        }
        
        try {
            double[] raw = Arrays.stream(signal.data).asDoubleStream().toArray();
            // 使用SeismicRecord对象进行噪声分析，获取真实的采样频率
            NoiseProfile profile = NoiseProfiler.analyze(signal);
            return process(profile, raw);
        } catch (Exception e) {
            System.err.println("ERROR: routeAndDenoise failed: " + e.getMessage());
            return Arrays.stream(signal.data).asDoubleStream().toArray();
        }
    }
}

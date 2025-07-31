package com.zjujzl.das.process;

import com.zjujzl.das.algorithm.*;
import com.zjujzl.das.model.*;
import com.zjujzl.das.profile.NoiseProfiler;
import com.zjujzl.das.profile.SmartRouter;
import com.zjujzl.das.util.NoiseMetrics;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;

public class CascadeDenoiser extends ProcessFunction<SeismicRecord, DenoiseResult> {
    private final String algorithmType;
    private static final int MIN_SIGNAL_LENGTH = 10;
    private static final int MAX_SIGNAL_LENGTH = 100000;

    public CascadeDenoiser(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    @Override
    public void processElement(SeismicRecord record, Context ctx, Collector<DenoiseResult> out) {
        try {
            // 输入验证
            if (record == null || record.data == null || record.data.length == 0) {
                System.err.println("WARN: Invalid input record for algorithm " + algorithmType);
                return;
            }
            
            if (record.data.length < MIN_SIGNAL_LENGTH) {
                System.err.println("WARN: Signal too short (" + record.data.length + ") for algorithm " + algorithmType);
                return;
            }
            
            if (record.data.length > MAX_SIGNAL_LENGTH) {
                System.err.println("WARN: Signal too long (" + record.data.length + ") for algorithm " + algorithmType);
                return;
            }

            double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
            
            // 检查信号是否包含NaN或无穷大
            if (containsInvalidValues(signal)) {
                System.err.println("WARN: Signal contains invalid values for algorithm " + algorithmType);
                return;
            }
            
            long start = System.currentTimeMillis();
            double[] result = processSignal(record);
            long latency = System.currentTimeMillis() - start;
            
            if (result != null && result.length > 0) {
                out.collect(new DenoiseResult(record, result, algorithmType, latency));
            } else {
                System.err.println("WARN: Algorithm " + algorithmType + " produced empty result");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Algorithm " + algorithmType + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double[] processSignal(SeismicRecord record) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        try {
            switch (algorithmType) {
                case "A":
                    // 空间平均 -> 移动微分 -> 频域去噪
                    double[] stepA1 = SpatialAverager.apply(signal, 21);
                    double[] stepA2 = MovingDifferentiator.apply(stepA1, 1);
                    return FDDAPlus.apply(stepA2);
                    
                case "B":
                    // 小波去噪 -> 空间平均 -> 频域去噪
                    double[] stepB1 = WaveletDenoiser.denoise(signal, "sym5", 5);
                    double[] stepB2 = SpatialAverager.apply(stepB1, 15);
                    return FDDAPlus.apply(stepB2);
                    
                case "C":
                    // EMD分解 -> 主成分重构 -> SVD滤波 -> 频域去噪
                    List<double[]> imfs = EMD.decompose(signal, 5);
                    if (imfs.isEmpty()) {
                        return FDDAPlus.apply(signal);
                    }
                    
                    // 选择前几个IMF进行重构
                    double[] reconstructed = new double[signal.length];
                    int componentsToUse = Math.min(3, imfs.size());
                    for (int i = 0; i < componentsToUse; i++) {
                        double[] imf = imfs.get(i);
                        for (int j = 0; j < Math.min(reconstructed.length, imf.length); j++) {
                            reconstructed[j] += imf[j];
                        }
                    }
                    
                    double[] stepC2 = SVDFilter.denoise(reconstructed, 0.95);
                    return FDDAPlus.apply(stepC2);
                    
                case "D":
                    // 智能路由 - 使用SeismicRecord对象获取真实的采样频率
                    return SmartRouter.process(NoiseProfiler.analyze(record), signal);
                    
                case "Baseline":
                default:
                    return FDDAPlus.apply(signal);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to process signal with algorithm " + algorithmType + ": " + e.getMessage());
            return FDDAPlus.apply(signal); // 回退到基线算法
        }
    }
    
    private boolean containsInvalidValues(double[] signal) {
        for (double value : signal) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return true;
            }
        }
        return false;
    }
}

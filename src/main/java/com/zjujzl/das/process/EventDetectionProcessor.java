package com.zjujzl.das.process;

import com.zjujzl.das.algorithm.*;
import com.zjujzl.das.model.*;
import com.zjujzl.das.config.EventDetectionConfig;
import com.zjujzl.das.profile.NoiseProfiler;
import com.zjujzl.das.profile.SmartRouter;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Arrays;

/**
 * 集成事件检测处理器
 * 结合降噪算法和STA/LTA事件检测，提供完整的地震事件检测流程
 */
public class EventDetectionProcessor extends ProcessFunction<SeismicRecord, EventDetectionResult> {
    
    private final String algorithmType;
    private final boolean useAdaptiveDetection;
    
    // STA/LTA参数配置
    private final double staLengthSec;
    private final double ltaLengthSec;
    private final double thresholdOn;
    private final double thresholdOff;
    private final double minEventLengthSec;
    
    // 处理统计
    private transient ValueState<Long> processedCount;
    private transient ValueState<Long> eventsDetectedCount;
    private transient ValueState<Double> avgProcessingTime;
    
    // 使用配置文件中的参数
    private static final int MIN_SIGNAL_LENGTH = EventDetectionConfig.MIN_SIGNAL_LENGTH;
    private static final int MAX_SIGNAL_LENGTH = EventDetectionConfig.MAX_SIGNAL_LENGTH;
    private static final double MIN_SAMPLING_RATE = EventDetectionConfig.MIN_SAMPLING_RATE;
    private static final double MAX_SAMPLING_RATE = EventDetectionConfig.MAX_SAMPLING_RATE;
    
    /**
     * 使用默认参数的构造函数
     */
    public EventDetectionProcessor(String algorithmType) {
        this(algorithmType, EventDetectionConfig.ENABLE_ADAPTIVE_DETECTION);
    }
    
    /**
     * 使用算法特定配置的构造函数
     */
    public EventDetectionProcessor(String algorithmType, boolean useAdaptiveDetection) {
        EventDetectionConfig.STALTAParams params = EventDetectionConfig.getAlgorithmParams(algorithmType);
        this.algorithmType = algorithmType;
        this.useAdaptiveDetection = useAdaptiveDetection;
        this.staLengthSec = params.staLengthSec;
        this.ltaLengthSec = params.ltaLengthSec;
        this.thresholdOn = params.thresholdOn;
        this.thresholdOff = params.thresholdOff;
        this.minEventLengthSec = EventDetectionConfig.DEFAULT_MIN_EVENT_LENGTH_SEC;
    }
    
    /**
     * 完整参数的构造函数
     */
    public EventDetectionProcessor(String algorithmType, 
                                 boolean useAdaptiveDetection,
                                 double staLengthSec,
                                 double ltaLengthSec,
                                 double thresholdOn,
                                 double thresholdOff,
                                 double minEventLengthSec) {
        this.algorithmType = algorithmType;
        this.useAdaptiveDetection = useAdaptiveDetection;
        this.staLengthSec = staLengthSec;
        this.ltaLengthSec = ltaLengthSec;
        this.thresholdOn = thresholdOn;
        this.thresholdOff = thresholdOff;
        this.minEventLengthSec = minEventLengthSec;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 初始化状态
        processedCount = getRuntimeContext().getState(
            new ValueStateDescriptor<>("processedCount", TypeInformation.of(Long.class), 0L));
        eventsDetectedCount = getRuntimeContext().getState(
            new ValueStateDescriptor<>("eventsDetectedCount", TypeInformation.of(Long.class), 0L));
        avgProcessingTime = getRuntimeContext().getState(
            new ValueStateDescriptor<>("avgProcessingTime", TypeInformation.of(Double.class), 0.0));
    }
    
    @Override
    public void processElement(SeismicRecord record, Context ctx, Collector<EventDetectionResult> out) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 输入验证
            if (!isValidInput(record)) {
                System.err.println("WARN: Invalid input record for EventDetectionProcessor " + algorithmType);
                return;
            }
            
            // 第一步：降噪处理
            long denoisingStart = System.currentTimeMillis();
            double[] denoisedSignal = applyDenoising(record);
            long denoisingTime = System.currentTimeMillis() - denoisingStart;
            
            if (denoisedSignal == null || denoisedSignal.length == 0) {
                System.err.println("WARN: Denoising failed for algorithm " + algorithmType);
                return;
            }
            
            // 第二步：STA/LTA事件检测
            long detectionStart = System.currentTimeMillis();
            STALTADetector.DetectionResult detectionResult = performEventDetection(denoisedSignal, record.sampling_rate);
            long detectionTime = System.currentTimeMillis() - detectionStart;
            
            // 第三步：构建结果
            EventDetectionResult result = new EventDetectionResult(
                record,
                denoisedSignal,
                algorithmType,
                denoisingTime,
                detectionResult,
                // -1表示自适应参数
                useAdaptiveDetection ? -1 : staLengthSec,
                useAdaptiveDetection ? -1 : ltaLengthSec,
                useAdaptiveDetection ? -1 : thresholdOn,
                useAdaptiveDetection ? -1 : thresholdOff,
                detectionTime
            );
            
            // 更新统计信息
            updateStatistics(result);
            
            // 输出结果
            out.collect(result);
            
            // 记录处理信息
            long totalTime = System.currentTimeMillis() - startTime;
            if (result.totalEvents > 0) {
                System.out.println(String.format(
                    "INFO: [%s] Processed %s - Events: %d, MaxRatio: %.2f, Quality: %.2f, Time: %dms",
                    algorithmType, record.station, result.totalEvents, result.maxRatio, 
                    result.signalQuality, totalTime
                ));
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: EventDetectionProcessor failed for " + algorithmType + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 输入验证
     */
    private boolean isValidInput(SeismicRecord record) {
        if (record == null || record.data == null || record.data.length == 0) {
            return false;
        }
        
        if (record.data.length < MIN_SIGNAL_LENGTH || record.data.length > MAX_SIGNAL_LENGTH) {
            System.err.println(String.format(
                "WARN: Signal length %d out of range [%d, %d]", 
                record.data.length, MIN_SIGNAL_LENGTH, MAX_SIGNAL_LENGTH
            ));
            return false;
        }
        
        if (record.sampling_rate < MIN_SAMPLING_RATE || record.sampling_rate > MAX_SAMPLING_RATE) {
            System.err.println(String.format(
                "WARN: Sampling rate %.2f out of range [%.2f, %.2f]", 
                record.sampling_rate, MIN_SAMPLING_RATE, MAX_SAMPLING_RATE
            ));
            return false;
        }
        
        // 检查数据中是否包含异常值
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        for (double value : signal) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                System.err.println("WARN: Signal contains NaN or infinite values");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 应用降噪算法
     */
    private double[] applyDenoising(SeismicRecord record) {
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
                    return processEMDPipeline(signal);
                    
                case "D":
                    // 智能路由 - 根据信号特征自动选择最优降噪算法组合
                    return SmartRouter.routeAndDenoise(record);
                    
                case "STA_LTA_ONLY":
                    // 仅进行基础预处理，主要用于STA/LTA检测
                    return SpatialAverager.apply(signal, 5);
                    
                default:
                    System.err.println("WARN: Unknown algorithm type: " + algorithmType + ", using default");
                    return FDDAPlus.apply(signal);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Denoising failed for algorithm " + algorithmType + ": " + e.getMessage());
            // 降级处理：返回简单平滑的信号
            return SpatialAverager.apply(signal, 5);
        }
    }
    
    /**
     * EMD处理流水线
     */
    private double[] processEMDPipeline(double[] signal) {
        try {
            java.util.List<double[]> imfs = EMD.decompose(signal, 5);
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
            
            // SVD滤波
            double[] svdFiltered = SVDFilter.denoise(reconstructed, 0.95);
            
            // 最终频域去噪
            return FDDAPlus.apply(svdFiltered);
        } catch (Exception e) {
            System.err.println("WARN: EMD pipeline failed, using fallback: " + e.getMessage());
            return FDDAPlus.apply(signal);
        }
    }
    
    /**
     * 执行事件检测
     */
    private STALTADetector.DetectionResult performEventDetection(double[] signal, double samplingRate) {
        if (useAdaptiveDetection) {
            return STALTADetector.adaptiveDetect(signal, samplingRate);
        } else {
            return STALTADetector.detect(signal, samplingRate, 
                                       staLengthSec, ltaLengthSec,
                                       thresholdOn, thresholdOff, 
                                       minEventLengthSec);
        }
    }
    
    /**
     * 更新处理统计信息
     */
    private void updateStatistics(EventDetectionResult result) {
        try {
            // 更新处理计数
            Long currentCount = processedCount.value();
            processedCount.update(currentCount + 1);
            
            // 更新事件检测计数
            Long currentEventCount = eventsDetectedCount.value();
            eventsDetectedCount.update(currentEventCount + result.totalEvents);
            
            // 更新平均处理时间
            Double currentAvgTime = avgProcessingTime.value();
            double newAvgTime = (currentAvgTime * currentCount + result.getTotalProcessingTime()) / (currentCount + 1);
            avgProcessingTime.update(newAvgTime);
            
            // 定期输出统计信息
            if ((currentCount + 1) % 100 == 0) {
                System.out.println(String.format(
                    "STATS: [%s] Processed: %d, Events: %d, AvgTime: %.2fms",
                    algorithmType, currentCount + 1, currentEventCount + result.totalEvents, newAvgTime
                ));
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to update statistics: " + e.getMessage());
        }
    }
    
    /**
     * 获取处理器配置信息
     */
    public String getConfigInfo() {
        if (useAdaptiveDetection) {
            return String.format("EventDetectionProcessor[%s, Adaptive]", algorithmType);
        } else {
            return String.format(
                "EventDetectionProcessor[%s, STA=%.1fs, LTA=%.1fs, Th=%.1f/%.1f]",
                algorithmType, staLengthSec, ltaLengthSec, thresholdOn, thresholdOff
            );
        }
    }
}
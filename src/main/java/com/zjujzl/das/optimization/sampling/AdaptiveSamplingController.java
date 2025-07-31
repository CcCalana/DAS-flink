package com.zjujzl.das.optimization.sampling;

import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.optimization.StreamOptimizationFramework.AdaptiveParameters;
import com.zjujzl.das.profile.NoiseProfile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自适应采样控制器
 * 
 * 基于信号特征和系统负载动态调整采样策略
 * 参考文献：
 * - IEEE Transactions 2023: Adaptive sampling for real-time seismic monitoring
 * - Journal of Geophysical Research 2022: Intelligent sampling in DAS systems
 * - Applied Sciences 2021: Machine learning-driven adaptive sampling
 * 
 * 核心功能：
 * 1. 基于信号重要性的自适应采样
 * 2. 多尺度采样策略
 * 3. 事件驱动的采样率调整
 * 4. 负载感知的采样控制
 * 5. 质量保证的采样优化
 */
public class AdaptiveSamplingController {
    
    private final SamplingConfig config;
    private final SignalImportanceAnalyzer importanceAnalyzer;
    private final LoadBalancer loadBalancer;
    private final QualityAssurance qualityAssurance;
    
    // 采样统计
    private final Map<String, SamplingStats> stationStats;
    private final Map<String, SamplingStrategy> currentStrategies;
    
    // 系统状态
    private volatile double systemLoad = 0.0;
    private volatile long lastUpdateTime = System.currentTimeMillis();
    
    public AdaptiveSamplingController(SamplingConfig config) {
        this.config = config;
        this.importanceAnalyzer = new SignalImportanceAnalyzer(config);
        this.loadBalancer = new LoadBalancer(config);
        this.qualityAssurance = new QualityAssurance(config);
        this.stationStats = new ConcurrentHashMap<>();
        this.currentStrategies = new ConcurrentHashMap<>();
    }
    
    /**
     * 自适应采样主入口
     */
    public SeismicRecord adaptiveSample(SeismicRecord record, AdaptiveParameters params) {
        if (record == null || record.data == null || record.data.length == 0) {
            return record;
        }
        
        try {
            // 1. 分析信号重要性
            ImportanceProfile importance = importanceAnalyzer.analyze(record);
            
            // 2. 获取当前采样策略
            SamplingStrategy strategy = getCurrentStrategy(record.station, importance, params);
            
            // 3. 执行自适应采样
            SeismicRecord sampledRecord = executeSampling(record, strategy, importance);
            
            // 4. 质量验证
            if (!qualityAssurance.validateSamplingQuality(record, sampledRecord, strategy)) {
                // 质量不达标，使用保守策略重新采样
                strategy = strategy.withHigherSamplingRate();
                sampledRecord = executeSampling(record, strategy, importance);
            }
            
            // 5. 更新统计信息
            updateSamplingStats(record.station, strategy, importance);
            
            // 6. 动态调整策略
            adaptStrategy(record.station, importance, params);
            
            return sampledRecord;
            
        } catch (Exception e) {
            System.err.println("ERROR: Adaptive sampling failed for station " + record.station + ": " + e.getMessage());
            return record; // 返回原始记录
        }
    }
    
    /**
     * 获取当前采样策略
     */
    private SamplingStrategy getCurrentStrategy(String station, ImportanceProfile importance, AdaptiveParameters params) {
        return currentStrategies.computeIfAbsent(station, k -> {
            SamplingStrategy strategy = new SamplingStrategy();
            
            // 基于重要性设置初始策略
            if (importance.getOverallImportance() > config.getHighImportanceThreshold()) {
                strategy.setSamplingMethod(SamplingMethod.FULL_RATE);
                strategy.setSamplingRatio(1.0);
            } else if (importance.getOverallImportance() > config.getMediumImportanceThreshold()) {
                strategy.setSamplingMethod(SamplingMethod.ADAPTIVE_RATE);
                strategy.setSamplingRatio(params.getSamplingLevel() * 0.8);
            } else {
                strategy.setSamplingMethod(SamplingMethod.INTELLIGENT_DECIMATION);
                strategy.setSamplingRatio(params.getSamplingLevel() * 0.6);
            }
            
            return strategy;
        });
    }
    
    /**
     * 执行采样
     */
    private SeismicRecord executeSampling(SeismicRecord record, SamplingStrategy strategy, ImportanceProfile importance) {
        switch (strategy.getSamplingMethod()) {
            case FULL_RATE:
                return record; // 保持原始采样率
            case ADAPTIVE_RATE:
                return adaptiveRateSampling(record, strategy, importance);
            case INTELLIGENT_DECIMATION:
                return intelligentDecimation(record, strategy, importance);
            case MULTI_SCALE:
                return multiScaleSampling(record, strategy, importance);
            case EVENT_DRIVEN:
                return eventDrivenSampling(record, strategy, importance);
            default:
                return uniformDownsampling(record, strategy);
        }
    }
    
    /**
     * 自适应采样率采样
     */
    private SeismicRecord adaptiveRateSampling(SeismicRecord record, SamplingStrategy strategy, ImportanceProfile importance) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        // 基于局部重要性动态调整采样率
        List<Integer> selectedIndices = new ArrayList<>();
        double[] localImportance = importance.getLocalImportance();
        
        double targetSamplingRatio = strategy.getSamplingRatio();
        int targetSamples = (int) (signal.length * targetSamplingRatio);
        
        // 重要性加权采样
        double[] cumulativeImportance = computeCumulativeImportance(localImportance);
        double totalImportance = cumulativeImportance[cumulativeImportance.length - 1];
        
        for (int i = 0; i < targetSamples; i++) {
            double target = (double) i / targetSamples * totalImportance;
            int index = findIndexByImportance(cumulativeImportance, target);
            selectedIndices.add(index);
        }
        
        // 确保索引唯一且有序
        selectedIndices = selectedIndices.stream().distinct().sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return createSampledRecord(record, selectedIndices);
    }
    
    /**
     * 智能抽取采样
     */
    private SeismicRecord intelligentDecimation(SeismicRecord record, SamplingStrategy strategy, ImportanceProfile importance) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        // 基于信号特征的智能抽取
        List<Integer> selectedIndices = new ArrayList<>();
        double[] localImportance = importance.getLocalImportance();
        
        int decimationFactor = (int) Math.ceil(1.0 / strategy.getSamplingRatio());
        
        // 在每个抽取窗口内选择最重要的样本
        for (int start = 0; start < signal.length; start += decimationFactor) {
            int end = Math.min(start + decimationFactor, signal.length);
            
            // 在窗口内找到最重要的样本
            int bestIndex = start;
            double maxImportance = localImportance[start];
            
            for (int i = start + 1; i < end; i++) {
                if (localImportance[i] > maxImportance) {
                    maxImportance = localImportance[i];
                    bestIndex = i;
                }
            }
            
            selectedIndices.add(bestIndex);
        }
        
        return createSampledRecord(record, selectedIndices);
    }
    
    /**
     * 多尺度采样
     */
    private SeismicRecord multiScaleSampling(SeismicRecord record, SamplingStrategy strategy, ImportanceProfile importance) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        // 多尺度分析和采样
        List<Integer> selectedIndices = new ArrayList<>();
        
        // 粗尺度：保留主要结构
        int coarseStep = Math.max(1, (int) (1.0 / strategy.getSamplingRatio() * 2));
        for (int i = 0; i < signal.length; i += coarseStep) {
            selectedIndices.add(i);
        }
        
        // 细尺度：在重要区域增加采样点
        double[] localImportance = importance.getLocalImportance();
        double importanceThreshold = Arrays.stream(localImportance).average().orElse(0.5) * 1.5;
        
        for (int i = 0; i < signal.length; i++) {
            if (localImportance[i] > importanceThreshold && !selectedIndices.contains(i)) {
                selectedIndices.add(i);
            }
        }
        
        // 排序并去重
        selectedIndices = selectedIndices.stream().distinct().sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return createSampledRecord(record, selectedIndices);
    }
    
    /**
     * 事件驱动采样
     */
    private SeismicRecord eventDrivenSampling(SeismicRecord record, SamplingStrategy strategy, ImportanceProfile importance) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        // 检测潜在事件区域
        List<EventRegion> eventRegions = detectEventRegions(signal, importance);
        
        List<Integer> selectedIndices = new ArrayList<>();
        
        // 在事件区域使用高采样率
        for (EventRegion region : eventRegions) {
            for (int i = region.start; i <= region.end; i++) {
                selectedIndices.add(i);
            }
        }
        
        // 在非事件区域使用低采样率
        int backgroundStep = Math.max(1, (int) (1.0 / strategy.getSamplingRatio() * 3));
        boolean[] covered = new boolean[signal.length];
        
        // 标记已覆盖的区域
        for (EventRegion region : eventRegions) {
            for (int i = region.start; i <= region.end; i++) {
                covered[i] = true;
            }
        }
        
        // 在未覆盖区域进行背景采样
        for (int i = 0; i < signal.length; i += backgroundStep) {
            if (!covered[i]) {
                selectedIndices.add(i);
            }
        }
        
        // 排序并去重
        selectedIndices = selectedIndices.stream().distinct().sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return createSampledRecord(record, selectedIndices);
    }
    
    /**
     * 均匀下采样
     */
    private SeismicRecord uniformDownsampling(SeismicRecord record, SamplingStrategy strategy) {
        double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
        
        int step = Math.max(1, (int) Math.ceil(1.0 / strategy.getSamplingRatio()));
        List<Integer> selectedIndices = new ArrayList<>();
        
        for (int i = 0; i < signal.length; i += step) {
            selectedIndices.add(i);
        }
        
        return createSampledRecord(record, selectedIndices);
    }
    
    /**
     * 创建采样后的记录
     */
    private SeismicRecord createSampledRecord(SeismicRecord original, List<Integer> selectedIndices) {
        if (selectedIndices.isEmpty()) {
            return original;
        }
        
        // 创建新的采样数据
        float[] sampledData = new float[selectedIndices.size()];
        for (int i = 0; i < selectedIndices.size(); i++) {
            int originalIndex = selectedIndices.get(i);
            if (originalIndex < original.data.length) {
                sampledData[i] = original.data[originalIndex];
            }
        }
        
        // 创建新记录
        SeismicRecord sampledRecord = new SeismicRecord();
        sampledRecord.network = original.network;
        sampledRecord.station = original.station;
        sampledRecord.location = original.location;
        sampledRecord.channel = original.channel;
        sampledRecord.starttime = original.starttime;
        sampledRecord.endtime = original.endtime;
        sampledRecord.geo_lat = original.geo_lat;
        sampledRecord.idas_version = original.idas_version;
        sampledRecord.measure_length = original.measure_length;
        sampledRecord.data = sampledData;
        
        // 调整采样率
        double samplingRatio = (double) selectedIndices.size() / original.data.length;
        sampledRecord.sampling_rate = original.sampling_rate * samplingRatio;
        
        return sampledRecord;
    }
    
    // 辅助方法
    
    private double[] computeCumulativeImportance(double[] importance) {
        double[] cumulative = new double[importance.length];
        cumulative[0] = importance[0];
        
        for (int i = 1; i < importance.length; i++) {
            cumulative[i] = cumulative[i-1] + importance[i];
        }
        
        return cumulative;
    }
    
    private int findIndexByImportance(double[] cumulativeImportance, double target) {
        for (int i = 0; i < cumulativeImportance.length; i++) {
            if (cumulativeImportance[i] >= target) {
                return i;
            }
        }
        return cumulativeImportance.length - 1;
    }
    
    private List<EventRegion> detectEventRegions(double[] signal, ImportanceProfile importance) {
        List<EventRegion> regions = new ArrayList<>();
        double[] localImportance = importance.getLocalImportance();
        
        double threshold = Arrays.stream(localImportance).average().orElse(0.5) * 2.0;
        
        int start = -1;
        for (int i = 0; i < localImportance.length; i++) {
            if (localImportance[i] > threshold) {
                if (start == -1) {
                    start = i;
                }
            } else {
                if (start != -1) {
                    regions.add(new EventRegion(start, i - 1));
                    start = -1;
                }
            }
        }
        
        // 处理最后一个区域
        if (start != -1) {
            regions.add(new EventRegion(start, localImportance.length - 1));
        }
        
        return regions;
    }
    
    private void updateSamplingStats(String station, SamplingStrategy strategy, ImportanceProfile importance) {
        stationStats.compute(station, (k, v) -> {
            if (v == null) {
                v = new SamplingStats();
            }
            v.addSample(strategy.getSamplingRatio(), importance.getOverallImportance());
            return v;
        });
    }
    
    private void adaptStrategy(String station, ImportanceProfile importance, AdaptiveParameters params) {
        SamplingStrategy currentStrategy = currentStrategies.get(station);
        if (currentStrategy == null) return;
        
        // 基于历史统计调整策略
        SamplingStats stats = stationStats.get(station);
        if (stats != null && stats.getSampleCount() > config.getMinSamplesForAdaptation()) {
            double avgImportance = stats.getAverageImportance();
            double currentImportance = importance.getOverallImportance();
            
            // 动态调整采样方法
            if (currentImportance > avgImportance * 1.5) {
                // 重要性显著增加，提高采样率
                currentStrategy.setSamplingMethod(SamplingMethod.EVENT_DRIVEN);
                currentStrategy.setSamplingRatio(Math.min(1.0, currentStrategy.getSamplingRatio() * 1.2));
            } else if (currentImportance < avgImportance * 0.5) {
                // 重要性显著降低，可以降低采样率
                currentStrategy.setSamplingMethod(SamplingMethod.INTELLIGENT_DECIMATION);
                currentStrategy.setSamplingRatio(Math.max(0.1, currentStrategy.getSamplingRatio() * 0.8));
            }
        }
    }
    
    /**
     * 更新系统负载
     */
    public void updateSystemLoad(double load) {
        this.systemLoad = Math.max(0.0, Math.min(1.0, load));
        this.lastUpdateTime = System.currentTimeMillis();
        
        // 根据系统负载调整所有策略
        if (load > config.getHighLoadThreshold()) {
            // 高负载时降低采样率
            currentStrategies.values().forEach(strategy -> {
                strategy.setSamplingRatio(strategy.getSamplingRatio() * 0.9);
            });
        } else if (load < config.getLowLoadThreshold()) {
            // 低负载时可以提高采样率
            currentStrategies.values().forEach(strategy -> {
                strategy.setSamplingRatio(Math.min(1.0, strategy.getSamplingRatio() * 1.1));
            });
        }
    }
    
    /**
     * 获取采样统计信息
     */
    public Map<String, SamplingStats> getSamplingStats() {
        return new HashMap<>(stationStats);
    }
    
    /**
     * 获取当前策略
     */
    public Map<String, SamplingStrategy> getCurrentStrategies() {
        return new HashMap<>(currentStrategies);
    }
    
    // 内部类
    
    private static class EventRegion {
        final int start;
        final int end;
        
        EventRegion(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
    
    public static class SamplingStats {
        private double totalSamplingRatio = 0.0;
        private double totalImportance = 0.0;
        private long sampleCount = 0;
        
        public void addSample(double samplingRatio, double importance) {
            totalSamplingRatio += samplingRatio;
            totalImportance += importance;
            sampleCount++;
        }
        
        public double getAverageSamplingRatio() {
            return sampleCount > 0 ? totalSamplingRatio / sampleCount : 0.5;
        }
        
        public double getAverageImportance() {
            return sampleCount > 0 ? totalImportance / sampleCount : 0.5;
        }
        
        public long getSampleCount() {
            return sampleCount;
        }
    }
}
package com.das.flink.algorithm;

import com.das.flink.entity.SeismicRecord;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自适应 STA/LTA 算法实现
 * 根据信号特征动态调整参数，提升检测精度和性能
 * 
 * @author DAS-Flink Team
 */
public class AdaptiveSTALTA {
    
    private final SignalAnalyzer signalAnalyzer;
    private final VectorizedSTALTA vectorizedSTALTA;
    private final MemoryPool memoryPool;
    
    // 自适应参数缓存
    private final Map<String, AdaptiveParameters> parameterCache;
    private final long cacheExpirationMs = 300000; // 5分钟缓存过期
    
    // 默认参数
    private static final int DEFAULT_STA_LENGTH = 10;
    private static final int DEFAULT_LTA_LENGTH = 100;
    private static final float DEFAULT_THRESHOLD = 2.0f;
    
    public AdaptiveSTALTA() {
        this.signalAnalyzer = new SignalAnalyzer();
        this.vectorizedSTALTA = new VectorizedSTALTA();
        this.memoryPool = new MemoryPool();
        this.parameterCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 自适应计算 STA/LTA
     * 
     * @param record 地震记录
     * @return 检测结果
     */
    public AdaptiveSTALTAResult computeAdaptive(SeismicRecord record) {
        String channelId = record.getNetwork() + "." + record.getStation() + "." + record.getChannel();
        
        // 获取或计算自适应参数
        AdaptiveParameters params = getAdaptiveParameters(channelId, record);
        
        // 多尺度计算
        VectorizedSTALTA.MultiScaleResult multiScaleResult = 
            vectorizedSTALTA.computeMultiScale(record.getData(), params.staLength, params.ltaLength);
        
        // 自适应阈值检测
        List<DetectionEvent> events = detectEventsWithAdaptiveThreshold(
            multiScaleResult, params, record);
        
        // 质量评估
        float qualityScore = assessDetectionQuality(events, record);
        
        return new AdaptiveSTALTAResult(
            multiScaleResult.getFusedRatios(),
            events,
            params,
            qualityScore,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 获取自适应参数
     */
    private AdaptiveParameters getAdaptiveParameters(String channelId, SeismicRecord record) {
        AdaptiveParameters cachedParams = parameterCache.get(channelId);
        
        // 检查缓存是否有效
        if (cachedParams != null && 
            (System.currentTimeMillis() - cachedParams.timestamp) < cacheExpirationMs) {
            return cachedParams;
        }
        
        // 计算新的自适应参数
        AdaptiveParameters newParams = computeAdaptiveParameters(record);
        parameterCache.put(channelId, newParams);
        
        return newParams;
    }
    
    /**
     * 计算自适应参数
     */
    private AdaptiveParameters computeAdaptiveParameters(SeismicRecord record) {
        float[] data = record.getData();
        float samplingRate = record.getSampleRate();
        
        // 信号特征分析
        SignalCharacteristics characteristics = signalAnalyzer.analyze(data, samplingRate);
        
        // 动态调整 STA 长度
        int adaptiveSTA = computeAdaptiveSTALength(characteristics, samplingRate);
        
        // 动态调整 LTA 长度
        int adaptiveLTA = computeAdaptiveLTALength(characteristics, samplingRate, adaptiveSTA);
        
        // 动态调整阈值
        float adaptiveThreshold = computeAdaptiveThreshold(characteristics);
        
        // 计算置信度权重
        float[] scaleWeights = computeScaleWeights(characteristics);
        
        return new AdaptiveParameters(
            adaptiveSTA, adaptiveLTA, adaptiveThreshold, scaleWeights,
            characteristics, System.currentTimeMillis()
        );
    }
    
    /**
     * 计算自适应 STA 长度
     */
    private int computeAdaptiveSTALength(SignalCharacteristics characteristics, float samplingRate) {
        float dominantFreq = characteristics.getDominantFrequency();
        float signalVariability = characteristics.getVariability();
        
        // 基于主频计算基础 STA 长度（2-3个周期）
        int baseSTA = (int) (samplingRate / Math.max(dominantFreq, 1.0f) * 2.5f);
        
        // 根据信号变异性调整
        float variabilityFactor = 1.0f + signalVariability * 0.5f;
        int adaptiveSTA = (int) (baseSTA * variabilityFactor);
        
        // 限制在合理范围内
        return Math.max(5, Math.min(adaptiveSTA, (int) (samplingRate * 2))); // 5样本到2秒
    }
    
    /**
     * 计算自适应 LTA 长度
     */
    private int computeAdaptiveLTALength(SignalCharacteristics characteristics, 
                                       float samplingRate, int staLength) {
        float noiseLevel = characteristics.getNoiseLevel();
        float stationarity = characteristics.getStationarity();
        
        // 基础 LTA 长度（STA 的 8-15 倍）
        int baseLTA = staLength * 10;
        
        // 根据噪声水平调整
        float noiseFactor = 1.0f + noiseLevel * 2.0f; // 噪声越大，LTA 越长
        
        // 根据平稳性调整
        float stationarityFactor = 2.0f - stationarity; // 越不平稳，LTA 越短
        
        int adaptiveLTA = (int) (baseLTA * noiseFactor * stationarityFactor);
        
        // 限制在合理范围内
        return Math.max(staLength * 5, Math.min(adaptiveLTA, (int) (samplingRate * 300))); // 最长5分钟
    }
    
    /**
     * 计算自适应阈值
     */
    private float computeAdaptiveThreshold(SignalCharacteristics characteristics) {
        float noiseLevel = characteristics.getNoiseLevel();
        float signalToNoise = characteristics.getSignalToNoiseRatio();
        float variability = characteristics.getVariability();
        
        // 基础阈值
        float baseThreshold = 2.0f;
        
        // 噪声调整
        float noiseAdjustment = 1.0f + noiseLevel * 1.5f;
        
        // 信噪比调整
        float snrAdjustment = Math.max(0.5f, 2.0f - signalToNoise * 0.1f);
        
        // 变异性调整
        float variabilityAdjustment = 1.0f + variability * 0.3f;
        
        float adaptiveThreshold = baseThreshold * noiseAdjustment * snrAdjustment * variabilityAdjustment;
        
        // 限制在合理范围内
        return Math.max(1.2f, Math.min(adaptiveThreshold, 10.0f));
    }
    
    /**
     * 计算多尺度权重
     */
    private float[] computeScaleWeights(SignalCharacteristics characteristics) {
        float dominantFreq = characteristics.getDominantFrequency();
        float[] baseWeights = {0.4f, 0.3f, 0.2f, 0.1f}; // 默认权重
        
        // 根据主频调整权重分布
        if (dominantFreq > 50.0f) {
            // 高频信号，偏重短尺度
            return new float[]{0.5f, 0.3f, 0.15f, 0.05f};
        } else if (dominantFreq < 10.0f) {
            // 低频信号，偏重长尺度
            return new float[]{0.2f, 0.3f, 0.3f, 0.2f};
        }
        
        return baseWeights;
    }
    
    /**
     * 自适应阈值检测
     */
    private List<DetectionEvent> detectEventsWithAdaptiveThreshold(
            VectorizedSTALTA.MultiScaleResult multiScaleResult,
            AdaptiveParameters params,
            SeismicRecord record) {
        
        float[] ratios = multiScaleResult.getFusedRatios();
        List<DetectionEvent> events = new ArrayList<>();
        
        // 动态阈值计算
        float[] dynamicThresholds = computeDynamicThresholds(ratios, params);
        
        // 事件检测状态机
        EventDetectionState state = EventDetectionState.IDLE;
        DetectionEvent currentEvent = null;
        int eventStartIndex = -1;
        
        for (int i = 0; i < ratios.length; i++) {
            float ratio = ratios[i];
            float threshold = dynamicThresholds[i];
            
            switch (state) {
                case IDLE:
                    if (ratio > threshold) {
                        // 开始新事件
                        currentEvent = memoryPool.borrowDetectionEvent();
                        currentEvent.setStartTime(record.getStartTime() + (long)(i * 1000.0 / record.getSampleRate()));
                        currentEvent.setMaxRatio(ratio);
                        currentEvent.setMaxRatioIndex(i);
                        eventStartIndex = i;
                        state = EventDetectionState.DETECTING;
                    }
                    break;
                    
                case DETECTING:
                    if (ratio > threshold) {
                        // 更新事件最大值
                        if (ratio > currentEvent.getMaxRatio()) {
                            currentEvent.setMaxRatio(ratio);
                            currentEvent.setMaxRatioIndex(i);
                        }
                    } else {
                        // 事件结束
                        currentEvent.setEndTime(record.getStartTime() + (long)(i * 1000.0 / record.getSampleRate()));
                        currentEvent.setDuration(i - eventStartIndex);
                        
                        // 质量检查
                        if (isValidEvent(currentEvent, params)) {
                            events.add(currentEvent);
                        } else {
                            memoryPool.returnDetectionEvent(currentEvent);
                        }
                        
                        currentEvent = null;
                        state = EventDetectionState.IDLE;
                    }
                    break;
            }
        }
        
        // 处理未结束的事件
        if (currentEvent != null) {
            currentEvent.setEndTime(record.getEndTime());
            currentEvent.setDuration(ratios.length - eventStartIndex);
            
            if (isValidEvent(currentEvent, params)) {
                events.add(currentEvent);
            } else {
                memoryPool.returnDetectionEvent(currentEvent);
            }
        }
        
        return events;
    }
    
    /**
     * 计算动态阈值
     */
    private float[] computeDynamicThresholds(float[] ratios, AdaptiveParameters params) {
        float[] thresholds = new float[ratios.length];
        float baseThreshold = params.threshold;
        
        // 滑动窗口统计
        int windowSize = Math.min(100, ratios.length / 10);
        
        for (int i = 0; i < ratios.length; i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(ratios.length, i + windowSize / 2);
            
            // 计算局部统计
            float localMean = 0.0f;
            float localStd = 0.0f;
            
            // 计算均值
            for (int j = start; j < end; j++) {
                localMean += ratios[j];
            }
            localMean /= (end - start);
            
            // 计算标准差
            for (int j = start; j < end; j++) {
                float diff = ratios[j] - localMean;
                localStd += diff * diff;
            }
            localStd = (float) Math.sqrt(localStd / (end - start));
            
            // 动态阈值 = 基础阈值 + 局部统计调整
            thresholds[i] = Math.max(baseThreshold, localMean + 2.0f * localStd);
        }
        
        return thresholds;
    }
    
    /**
     * 验证事件有效性
     */
    private boolean isValidEvent(DetectionEvent event, AdaptiveParameters params) {
        // 最小持续时间检查
        if (event.getDuration() < 3) {
            return false;
        }
        
        // 最小比值检查
        if (event.getMaxRatio() < params.threshold * 0.8f) {
            return false;
        }
        
        // 最大持续时间检查（避免长时间噪声）
        if (event.getDuration() > params.ltaLength) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 评估检测质量
     */
    private float assessDetectionQuality(List<DetectionEvent> events, SeismicRecord record) {
        if (events.isEmpty()) {
            return 0.5f; // 中性质量分数
        }
        
        float totalQuality = 0.0f;
        
        for (DetectionEvent event : events) {
            float eventQuality = 0.0f;
            
            // 信噪比质量
            float snrQuality = Math.min(1.0f, event.getMaxRatio() / 10.0f);
            eventQuality += snrQuality * 0.4f;
            
            // 持续时间质量
            float durationQuality = event.getDuration() > 5 && event.getDuration() < 100 ? 1.0f : 0.5f;
            eventQuality += durationQuality * 0.3f;
            
            // 形状质量（简化评估）
            float shapeQuality = 0.8f; // 默认形状质量
            eventQuality += shapeQuality * 0.3f;
            
            totalQuality += eventQuality;
        }
        
        return totalQuality / events.size();
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        parameterCache.entrySet().removeIf(
            entry -> (currentTime - entry.getValue().timestamp) > cacheExpirationMs
        );
    }
    
    /**
     * 获取缓存统计
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            parameterCache.size(),
            cacheExpirationMs
        );
    }
    
    /**
     * 释放资源
     */
    public void shutdown() {
        vectorizedSTALTA.shutdown();
        memoryPool.clear();
        parameterCache.clear();
    }
    
    // 内部类和枚举
    
    private enum EventDetectionState {
        IDLE, DETECTING
    }
    
    /**
     * 自适应参数类
     */
    public static class AdaptiveParameters {
        public final int staLength;
        public final int ltaLength;
        public final float threshold;
        public final float[] scaleWeights;
        public final SignalCharacteristics characteristics;
        public final long timestamp;
        
        public AdaptiveParameters(int staLength, int ltaLength, float threshold,
                                float[] scaleWeights, SignalCharacteristics characteristics,
                                long timestamp) {
            this.staLength = staLength;
            this.ltaLength = ltaLength;
            this.threshold = threshold;
            this.scaleWeights = scaleWeights;
            this.characteristics = characteristics;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("AdaptiveParameters{STA=%d, LTA=%d, threshold=%.2f}",
                               staLength, ltaLength, threshold);
        }
    }
    
    /**
     * 自适应 STA/LTA 结果类
     */
    public static class AdaptiveSTALTAResult {
        private final float[] ratios;
        private final List<DetectionEvent> events;
        private final AdaptiveParameters parameters;
        private final float qualityScore;
        private final long timestamp;
        
        public AdaptiveSTALTAResult(float[] ratios, List<DetectionEvent> events,
                                  AdaptiveParameters parameters, float qualityScore,
                                  long timestamp) {
            this.ratios = ratios;
            this.events = events;
            this.parameters = parameters;
            this.qualityScore = qualityScore;
            this.timestamp = timestamp;
        }
        
        // Getters
        public float[] getRatios() { return ratios; }
        public List<DetectionEvent> getEvents() { return events; }
        public AdaptiveParameters getParameters() { return parameters; }
        public float getQualityScore() { return qualityScore; }
        public long getTimestamp() { return timestamp; }
        
        public boolean hasEvents() { return !events.isEmpty(); }
        public int getEventCount() { return events.size(); }
        
        public DetectionEvent getBestEvent() {
            return events.stream()
                .max(Comparator.comparing(DetectionEvent::getMaxRatio))
                .orElse(null);
        }
    }
    
    /**
     * 缓存统计类
     */
    public static class CacheStatistics {
        private final int cacheSize;
        private final long expirationMs;
        
        public CacheStatistics(int cacheSize, long expirationMs) {
            this.cacheSize = cacheSize;
            this.expirationMs = expirationMs;
        }
        
        public int getCacheSize() { return cacheSize; }
        public long getExpirationMs() { return expirationMs; }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{size=%d, expiration=%dms}",
                               cacheSize, expirationMs);
        }
    }
}
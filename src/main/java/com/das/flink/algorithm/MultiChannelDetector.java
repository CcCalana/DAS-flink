package com.das.flink.algorithm;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多通道融合检测器
 * 整合多个通道的STA/LTA结果，通过空间相关性分析提高检测准确性
 * 
 * @author DAS-Flink Team
 */
public class MultiChannelDetector {
    
    private final Map<String, ChannelState> channelStates;
    private final Map<String, Double> channelDistances; // 通道间距离
    private final MemoryPool memoryPool;
    private final AtomicLong eventIdCounter;
    
    // 配置参数
    private final int maxChannels;
    private final long timeWindowMs;        // 时间窗口（毫秒）
    private final double spatialThreshold;  // 空间阈值（米）
    private final int minChannelCount;      // 最小通道数
    private final float confidenceThreshold; // 置信度阈值
    
    // 检测参数
    private final float correlationThreshold;  // 相关性阈值
    private final int maxEventDuration;        // 最大事件持续时间（样本数）
    private final float amplitudeRatioThreshold; // 振幅比阈值
    
    // 统计信息
    private long totalDetections;
    private long validDetections;
    private long fusedDetections;
    private final Map<String, Long> channelDetectionCounts;
    
    public MultiChannelDetector() {
        this(100, 5000L, 1000.0, 3, 0.6f);
    }
    
    public MultiChannelDetector(int maxChannels, long timeWindowMs, double spatialThreshold, 
                               int minChannelCount, float confidenceThreshold) {
        this.maxChannels = maxChannels;
        this.timeWindowMs = timeWindowMs;
        this.spatialThreshold = spatialThreshold;
        this.minChannelCount = minChannelCount;
        this.confidenceThreshold = confidenceThreshold;
        
        this.correlationThreshold = 0.7f;
        this.maxEventDuration = 1000; // 4秒 @ 250Hz
        this.amplitudeRatioThreshold = 0.5f;
        
        this.channelStates = new ConcurrentHashMap<>();
        this.channelDistances = new ConcurrentHashMap<>();
        this.memoryPool = new MemoryPool();
        this.eventIdCounter = new AtomicLong(0);
        
        this.totalDetections = 0L;
        this.validDetections = 0L;
        this.fusedDetections = 0L;
        this.channelDetectionCounts = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册通道及其位置信息
     */
    public void registerChannel(String channelId, double latitude, double longitude, double fiberDistance) {
        ChannelState state = new ChannelState(channelId, latitude, longitude, fiberDistance);
        channelStates.put(channelId, state);
        channelDetectionCounts.put(channelId, 0L);
        
        // 计算与其他通道的距离
        updateChannelDistances(channelId, latitude, longitude);
    }
    
    /**
     * 处理单通道检测结果
     */
    public List<DetectionEvent> processChannelResult(STALTAResult result) {
        if (result == null || !result.hasTriggers()) {
            return Collections.emptyList();
        }
        
        totalDetections++;
        
        String channelId = result.getChannelId();
        ChannelState state = channelStates.get(channelId);
        if (state == null) {
            // 自动注册未知通道
            registerChannel(channelId, 0.0, 0.0, 0.0);
            state = channelStates.get(channelId);
        }
        
        // 更新通道状态
        state.updateResult(result);
        
        // 提取单通道事件
        List<DetectionEvent> channelEvents = extractChannelEvents(result);
        
        // 执行多通道融合
        List<DetectionEvent> fusedEvents = performMultiChannelFusion(channelId, channelEvents);
        
        // 更新统计信息
        validDetections += channelEvents.size();
        fusedDetections += fusedEvents.size();
        channelDetectionCounts.merge(channelId, (long) channelEvents.size(), Long::sum);
        
        return fusedEvents;
    }
    
    /**
     * 从STA/LTA结果中提取检测事件
     */
    private List<DetectionEvent> extractChannelEvents(STALTAResult result) {
        List<DetectionEvent> events = new ArrayList<>();
        
        boolean[] triggers = result.getTriggers();
        float[] ratios = result.getRatios();
        
        if (triggers == null || ratios == null) {
            return events;
        }
        
        boolean inEvent = false;
        int eventStart = -1;
        float maxRatio = 0.0f;
        int maxRatioIndex = -1;
        
        for (int i = 0; i < triggers.length; i++) {
            if (triggers[i] && !inEvent) {
                // 事件开始
                inEvent = true;
                eventStart = i;
                maxRatio = ratios[i];
                maxRatioIndex = i;
            } else if (triggers[i] && inEvent) {
                // 事件继续，更新最大比值
                if (ratios[i] > maxRatio) {
                    maxRatio = ratios[i];
                    maxRatioIndex = i;
                }
            } else if (!triggers[i] && inEvent) {
                // 事件结束
                inEvent = false;
                
                // 创建检测事件
                DetectionEvent event = createDetectionEvent(
                    result, eventStart, i - 1, maxRatio, maxRatioIndex
                );
                
                if (event != null && event.isValid()) {
                    events.add(event);
                }
            }
        }
        
        // 处理在数据末尾仍在进行的事件
        if (inEvent && eventStart >= 0) {
            DetectionEvent event = createDetectionEvent(
                result, eventStart, triggers.length - 1, maxRatio, maxRatioIndex
            );
            
            if (event != null && event.isValid()) {
                events.add(event);
            }
        }
        
        return events;
    }
    
    /**
     * 创建检测事件
     */
    private DetectionEvent createDetectionEvent(STALTAResult result, int startIndex, int endIndex, 
                                              float maxRatio, int maxRatioIndex) {
        
        DetectionEvent event = memoryPool.borrowDetectionEvent();
        
        // 基本信息
        event.setEventId(generateEventId());
        event.setChannelId(result.getChannelId());
        event.setMaxRatio(maxRatio);
        event.setMaxRatioIndex(maxRatioIndex);
        event.setDuration(endIndex - startIndex + 1);
        
        // 时间信息
        long baseTime = result.getTimestamp();
        float sampleRate = result.getSampleRate();
        long startTime = baseTime + (long) (startIndex * 1000.0f / sampleRate);
        long endTime = baseTime + (long) (endIndex * 1000.0f / sampleRate);
        
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setTimestamp(System.currentTimeMillis());
        
        // 设置比值历史
        float[] ratios = result.getRatios();
        if (ratios != null && startIndex >= 0 && endIndex < ratios.length) {
            event.setRatioHistory(ratios, startIndex, endIndex);
        }
        
        // 计算置信度
        float confidence = calculateEventConfidence(event, result);
        event.setConfidence(confidence);
        
        // 更新质量评估
        event.updateQuality();
        
        // 设置通道位置信息
        ChannelState state = channelStates.get(result.getChannelId());
        if (state != null) {
            event.setLatitude(state.latitude);
            event.setLongitude(state.longitude);
        }
        
        return event;
    }
    
    /**
     * 执行多通道融合
     */
    private List<DetectionEvent> performMultiChannelFusion(String triggerChannelId, 
                                                          List<DetectionEvent> channelEvents) {
        
        List<DetectionEvent> fusedEvents = new ArrayList<>();
        
        for (DetectionEvent event : channelEvents) {
            // 查找相关通道的同时事件
            List<DetectionEvent> correlatedEvents = findCorrelatedEvents(event, triggerChannelId);
            
            if (correlatedEvents.size() >= minChannelCount - 1) { // -1因为不包括触发通道本身
                // 执行事件融合
                DetectionEvent fusedEvent = fuseEvents(event, correlatedEvents);
                fusedEvents.add(fusedEvent);
            } else if (event.getConfidence() > confidenceThreshold * 1.2f) {
                // 高置信度单通道事件也保留
                fusedEvents.add(event);
            }
        }
        
        return fusedEvents;
    }
    
    /**
     * 查找相关事件
     */
    private List<DetectionEvent> findCorrelatedEvents(DetectionEvent referenceEvent, String triggerChannelId) {
        List<DetectionEvent> correlatedEvents = new ArrayList<>();
        
        long refStartTime = referenceEvent.getStartTime();
        long refEndTime = referenceEvent.getEndTime();
        
        for (Map.Entry<String, ChannelState> entry : channelStates.entrySet()) {
            String channelId = entry.getKey();
            ChannelState state = entry.getValue();
            
            // 跳过触发通道本身
            if (channelId.equals(triggerChannelId)) {
                continue;
            }
            
            // 检查空间距离
            String distanceKey = getDistanceKey(triggerChannelId, channelId);
            Double distance = channelDistances.get(distanceKey);
            if (distance != null && distance > spatialThreshold) {
                continue;
            }
            
            // 查找时间窗口内的事件
            List<DetectionEvent> recentEvents = state.getRecentEvents(refStartTime - timeWindowMs, 
                                                                     refEndTime + timeWindowMs);
            
            for (DetectionEvent candidateEvent : recentEvents) {
                if (isEventCorrelated(referenceEvent, candidateEvent)) {
                    correlatedEvents.add(candidateEvent);
                }
            }
        }
        
        return correlatedEvents;
    }
    
    /**
     * 判断事件是否相关
     */
    private boolean isEventCorrelated(DetectionEvent event1, DetectionEvent event2) {
        // 时间重叠检查
        long overlap = Math.min(event1.getEndTime(), event2.getEndTime()) - 
                      Math.max(event1.getStartTime(), event2.getStartTime());
        
        if (overlap <= 0) {
            return false;
        }
        
        // 振幅比检查
        float ratioRatio = Math.min(event1.getMaxRatio(), event2.getMaxRatio()) / 
                          Math.max(event1.getMaxRatio(), event2.getMaxRatio());
        
        if (ratioRatio < amplitudeRatioThreshold) {
            return false;
        }
        
        // 持续时间检查
        int durationDiff = Math.abs(event1.getDuration() - event2.getDuration());
        int maxDuration = Math.max(event1.getDuration(), event2.getDuration());
        
        if (maxDuration > 0 && (float) durationDiff / maxDuration > 0.5f) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 融合多个事件
     */
    private DetectionEvent fuseEvents(DetectionEvent primaryEvent, List<DetectionEvent> correlatedEvents) {
        DetectionEvent fusedEvent = memoryPool.borrowDetectionEvent();
        
        // 复制主事件信息
        fusedEvent.setEventId(generateEventId());
        fusedEvent.setChannelId("FUSED_" + primaryEvent.getChannelId());
        fusedEvent.setStartTime(primaryEvent.getStartTime());
        fusedEvent.setEndTime(primaryEvent.getEndTime());
        fusedEvent.setMaxRatio(primaryEvent.getMaxRatio());
        fusedEvent.setMaxRatioIndex(primaryEvent.getMaxRatioIndex());
        fusedEvent.setDuration(primaryEvent.getDuration());
        fusedEvent.setTimestamp(System.currentTimeMillis());
        
        // 融合相关事件
        for (DetectionEvent correlatedEvent : correlatedEvents) {
            fusedEvent.mergeWith(correlatedEvent);
        }
        
        // 设置融合特有属性
        fusedEvent.setTriggerCount(correlatedEvents.size() + 1); // +1包括主事件
        fusedEvent.setEventType(DetectionEvent.EventType.EARTHQUAKE); // 多通道检测通常是地震
        
        // 计算融合置信度
        float fusedConfidence = calculateFusedConfidence(primaryEvent, correlatedEvents);
        fusedEvent.setConfidence(fusedConfidence);
        
        // 更新质量评估
        fusedEvent.updateQuality();
        
        return fusedEvent;
    }
    
    /**
     * 计算事件置信度
     */
    private float calculateEventConfidence(DetectionEvent event, STALTAResult result) {
        float ratioConfidence = Math.min(1.0f, event.getMaxRatio() / (result.getThreshold() * 2.0f));
        float durationConfidence = Math.min(1.0f, event.getDuration() / 50.0f); // 50样本作为参考
        float qualityConfidence = result.getConfidence();
        
        return (ratioConfidence * 0.4f + durationConfidence * 0.3f + qualityConfidence * 0.3f);
    }
    
    /**
     * 计算融合置信度
     */
    private float calculateFusedConfidence(DetectionEvent primaryEvent, List<DetectionEvent> correlatedEvents) {
        float primaryConfidence = primaryEvent.getConfidence();
        
        // 相关事件数量加权
        float countBonus = Math.min(0.3f, correlatedEvents.size() * 0.1f);
        
        // 相关事件平均置信度
        float avgCorrelatedConfidence = 0.0f;
        if (!correlatedEvents.isEmpty()) {
            float sum = 0.0f;
            for (DetectionEvent event : correlatedEvents) {
                sum += event.getConfidence();
            }
            avgCorrelatedConfidence = sum / correlatedEvents.size();
        }
        
        return Math.min(1.0f, primaryConfidence + countBonus + avgCorrelatedConfidence * 0.2f);
    }
    
    /**
     * 更新通道间距离
     */
    private void updateChannelDistances(String newChannelId, double lat, double lon) {
        for (Map.Entry<String, ChannelState> entry : channelStates.entrySet()) {
            String existingChannelId = entry.getKey();
            if (existingChannelId.equals(newChannelId)) continue;
            
            ChannelState existingState = entry.getValue();
            double distance = calculateDistance(lat, lon, existingState.latitude, existingState.longitude);
            
            String key = getDistanceKey(newChannelId, existingChannelId);
            channelDistances.put(key, distance);
        }
    }
    
    /**
     * 计算两点间距离（简化版，使用欧几里得距离）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 简化计算，实际应用中可使用Haversine公式
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111000; // 转换为米（近似）
    }
    
    /**
     * 生成距离键
     */
    private String getDistanceKey(String channel1, String channel2) {
        return channel1.compareTo(channel2) < 0 ? 
               channel1 + "_" + channel2 : channel2 + "_" + channel1;
    }
    
    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return "EVT_" + System.currentTimeMillis() + "_" + eventIdCounter.incrementAndGet();
    }
    
    /**
     * 获取检测统计信息
     */
    public DetectionStatistics getStatistics() {
        return new DetectionStatistics(
            totalDetections, validDetections, fusedDetections,
            channelStates.size(), new HashMap<>(channelDetectionCounts)
        );
    }
    
    /**
     * 清理过期数据
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = currentTime - timeWindowMs * 2; // 保留2倍时间窗口的数据
        
        for (ChannelState state : channelStates.values()) {
            state.cleanup(cleanupThreshold);
        }
    }
    
    /**
     * 通道状态类
     */
    private static class ChannelState {
        private final String channelId;
        private final double latitude;
        private final double longitude;
        private final double fiberDistance;
        private final List<DetectionEvent> recentEvents;
        private STALTAResult lastResult;
        private long lastUpdateTime;
        
        public ChannelState(String channelId, double latitude, double longitude, double fiberDistance) {
            this.channelId = channelId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.fiberDistance = fiberDistance;
            this.recentEvents = new ArrayList<>();
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updateResult(STALTAResult result) {
            this.lastResult = result;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void addEvent(DetectionEvent event) {
            synchronized (recentEvents) {
                recentEvents.add(event);
            }
        }
        
        public List<DetectionEvent> getRecentEvents(long startTime, long endTime) {
            List<DetectionEvent> events = new ArrayList<>();
            synchronized (recentEvents) {
                for (DetectionEvent event : recentEvents) {
                    if (event.getStartTime() <= endTime && event.getEndTime() >= startTime) {
                        events.add(event);
                    }
                }
            }
            return events;
        }
        
        public void cleanup(long threshold) {
            synchronized (recentEvents) {
                recentEvents.removeIf(event -> event.getTimestamp() < threshold);
            }
        }
    }
    
    /**
     * 检测统计信息类
     */
    public static class DetectionStatistics {
        private final long totalDetections;
        private final long validDetections;
        private final long fusedDetections;
        private final int activeChannels;
        private final Map<String, Long> channelCounts;
        
        public DetectionStatistics(long totalDetections, long validDetections, long fusedDetections,
                                 int activeChannels, Map<String, Long> channelCounts) {
            this.totalDetections = totalDetections;
            this.validDetections = validDetections;
            this.fusedDetections = fusedDetections;
            this.activeChannels = activeChannels;
            this.channelCounts = channelCounts;
        }
        
        public double getValidDetectionRate() {
            return totalDetections > 0 ? (double) validDetections / totalDetections : 0.0;
        }
        
        public double getFusionRate() {
            return validDetections > 0 ? (double) fusedDetections / validDetections : 0.0;
        }
        
        // Getters
        public long getTotalDetections() { return totalDetections; }
        public long getValidDetections() { return validDetections; }
        public long getFusedDetections() { return fusedDetections; }
        public int getActiveChannels() { return activeChannels; }
        public Map<String, Long> getChannelCounts() { return channelCounts; }
        
        @Override
        public String toString() {
            return String.format(
                "DetectionStatistics{total=%d, valid=%d, fused=%d, channels=%d, validRate=%.2f%%, fusionRate=%.2f%%}",
                totalDetections, validDetections, fusedDetections, activeChannels,
                getValidDetectionRate() * 100, getFusionRate() * 100
            );
        }
    }
}
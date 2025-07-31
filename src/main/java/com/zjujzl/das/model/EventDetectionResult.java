package com.zjujzl.das.model;

import com.zjujzl.das.algorithm.STALTADetector;
import java.util.List;

/**
 * 事件检测结果模型
 * 包含原始信号、降噪结果和STA/LTA事件检测信息
 */
public class EventDetectionResult {
    public SeismicRecord originalSignal;           // 原始地震信号
    public double[] denoisedSignal;                // 降噪后的信号
    public String denoisingAlgorithm;              // 使用的降噪算法
    public long denoisingTime;                     // 降噪处理时间(ms)
    
    // STA/LTA检测结果
    public double[] staLtaRatio;                   // STA/LTA比值序列
    public List<STALTADetector.EventWindow> events; // 检测到的事件窗口
    public double maxRatio;                        // 最大STA/LTA比值
    public int totalEvents;                        // 检测到的事件总数
    public long detectionTime;                     // 事件检测处理时间(ms)
    
    // 检测参数
    public double staLengthSec;                    // STA窗口长度(秒)
    public double ltaLengthSec;                    // LTA窗口长度(秒)
    public double thresholdOn;                     // 触发阈值
    public double thresholdOff;                    // 结束阈值
    
    // 质量评估
    public double signalQuality;                   // 信号质量评分(0-1)
    public boolean hasSignificantEvents;           // 是否包含显著事件
    public String detectionSummary;                // 检测结果摘要
    
    public EventDetectionResult() {}
    
    public EventDetectionResult(SeismicRecord originalSignal, 
                               double[] denoisedSignal,
                               String denoisingAlgorithm,
                               long denoisingTime,
                               STALTADetector.DetectionResult detectionResult,
                               double staLengthSec,
                               double ltaLengthSec,
                               double thresholdOn,
                               double thresholdOff,
                               long detectionTime) {
        this.originalSignal = originalSignal;
        this.denoisedSignal = denoisedSignal;
        this.denoisingAlgorithm = denoisingAlgorithm;
        this.denoisingTime = denoisingTime;
        
        this.staLtaRatio = detectionResult.staLtaRatio;
        this.events = detectionResult.events;
        this.maxRatio = detectionResult.maxRatio;
        this.totalEvents = detectionResult.totalEvents;
        this.detectionTime = detectionTime;
        
        this.staLengthSec = staLengthSec;
        this.ltaLengthSec = ltaLengthSec;
        this.thresholdOn = thresholdOn;
        this.thresholdOff = thresholdOff;
        
        // 计算信号质量和事件显著性
        this.signalQuality = calculateSignalQuality();
        this.hasSignificantEvents = checkSignificantEvents();
        this.detectionSummary = STALTADetector.getDetectionSummary(detectionResult, originalSignal.sampling_rate);
    }
    
    /**
     * 计算信号质量评分
     * 基于信噪比、数据完整性等因素
     */
    private double calculateSignalQuality() {
        if (denoisedSignal == null || denoisedSignal.length == 0) {
            return 0.0;
        }
        
        // 计算信号的RMS值
        double rms = 0.0;
        for (double value : denoisedSignal) {
            rms += value * value;
        }
        rms = Math.sqrt(rms / denoisedSignal.length);
        
        // 计算信号的动态范围
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double value : denoisedSignal) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        double dynamicRange = max - min;
        
        // 基于RMS和动态范围计算质量评分
        double qualityScore = Math.min(1.0, (rms * dynamicRange) / 10000.0);
        
        // 如果有事件检测，根据最大比值调整评分
        if (maxRatio > 2.0) {
            qualityScore = Math.min(1.0, qualityScore * (1.0 + Math.log10(maxRatio)));
        }
        
        return qualityScore;
    }
    
    /**
     * 检查是否包含显著事件
     */
    private boolean checkSignificantEvents() {
        if (events == null || events.isEmpty()) {
            return false;
        }
        
        // 检查是否有持续时间超过1秒且比值超过3.0的事件
        for (STALTADetector.EventWindow event : events) {
            if (event.duration > 1.0 && event.maxRatio > 3.0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取最显著的事件
     */
    public STALTADetector.EventWindow getMostSignificantEvent() {
        if (events == null || events.isEmpty()) {
            return null;
        }
        
        STALTADetector.EventWindow mostSignificant = events.get(0);
        for (STALTADetector.EventWindow event : events) {
            if (event.maxRatio > mostSignificant.maxRatio) {
                mostSignificant = event;
            }
        }
        
        return mostSignificant;
    }
    
    /**
     * 获取事件密度（每分钟事件数）
     */
    public double getEventDensity() {
        if (originalSignal == null || originalSignal.data == null) {
            return 0.0;
        }
        
        double signalLengthMinutes = (originalSignal.data.length / originalSignal.sampling_rate) / 60.0;
        return totalEvents / Math.max(signalLengthMinutes, 1.0/60.0); // 避免除零
    }
    
    /**
     * 获取总处理时间
     */
    public long getTotalProcessingTime() {
        return denoisingTime + detectionTime;
    }
    
    /**
     * 判断是否为高质量检测结果
     */
    public boolean isHighQualityDetection() {
        return signalQuality > 0.7 && 
               (hasSignificantEvents || maxRatio > 2.5) &&
               getTotalProcessingTime() < 5000; // 处理时间小于5秒
    }
    
    /**
     * 获取检测结果的紧凑摘要
     */
    public String getCompactSummary() {
        return String.format(
            "Station: %s | Events: %d | MaxRatio: %.2f | Quality: %.2f | Time: %dms",
            originalSignal != null ? originalSignal.station : "Unknown",
            totalEvents,
            maxRatio,
            signalQuality,
            getTotalProcessingTime()
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "EventDetectionResult{station=%s, algorithm=%s, events=%d, maxRatio=%.2f, quality=%.2f, processingTime=%dms}",
            originalSignal != null ? originalSignal.station : "N/A",
            denoisingAlgorithm,
            totalEvents,
            maxRatio,
            signalQuality,
            getTotalProcessingTime()
        );
    }
}
package com.zjujzl.das.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * STA/LTA事件检测算法（Short-Term Average / Long-Term Average）
 * 
 * 数学原理：
 * 1. 短期平均（STA）：
 *    STA(n) = (1/N_s) * Σ(k=n-N_s+1 to n) |x(k)|²
 *    其中：N_s 为短期窗口长度，通常为1-5秒
 * 
 * 2. 长期平均（LTA）：
 *    LTA(n) = (1/N_l) * Σ(k=n-N_l+1 to n) |x(k)|²
 *    其中：N_l 为长期窗口长度，通常为10-60秒
 * 
 * 3. STA/LTA比值：
 *    R(n) = STA(n) / LTA(n)
 * 
 * 4. 事件检测逻辑：
 *    - 触发条件：R(n) > threshold_on（通常为2-10）
 *    - 结束条件：R(n) < threshold_off（通常为1.5-3）
 * 
 * 5. 递归实现（提高计算效率）：
 *    STA(n) = α_s * |x(n)|² + (1-α_s) * STA(n-1)
 *    LTA(n) = α_l * |x(n)|² + (1-α_l) * LTA(n-1)
 *    其中：α_s = 1/N_s, α_l = 1/N_l
 * 
 * 应用场景：地震事件检测、P波S波到时拾取、噪声环境下的信号检测
 */
public class STALTADetector {
    
    // 默认参数配置
    private static final double DEFAULT_STA_LENGTH_SEC = 2.0;    // 短期窗口2秒
    private static final double DEFAULT_LTA_LENGTH_SEC = 30.0;   // 长期窗口30秒
    private static final double DEFAULT_THRESHOLD_ON = 3.0;      // 触发阈值
    private static final double DEFAULT_THRESHOLD_OFF = 1.5;     // 结束阈值
    private static final double DEFAULT_MIN_EVENT_LENGTH_SEC = 1.0; // 最小事件长度
    
    /**
     * 事件检测结果
     */
    public static class DetectionResult {
        public double[] staLtaRatio;     // STA/LTA比值序列
        public List<EventWindow> events; // 检测到的事件窗口
        public double maxRatio;          // 最大比值
        public int totalEvents;          // 事件总数
        
        public DetectionResult(double[] staLtaRatio, List<EventWindow> events) {
            this.staLtaRatio = staLtaRatio;
            this.events = events;
            this.totalEvents = events.size();
            this.maxRatio = 0.0;
            for (double ratio : staLtaRatio) {
                if (ratio > maxRatio) {
                    maxRatio = ratio;
                }
            }
        }
    }
    
    /**
     * 事件窗口
     */
    public static class EventWindow {
        public int startIndex;    // 事件开始索引
        public int endIndex;      // 事件结束索引
        public double startTime;  // 事件开始时间（秒）
        public double endTime;    // 事件结束时间（秒）
        public double maxRatio;   // 事件期间最大STA/LTA比值
        public double duration;   // 事件持续时间（秒）
        
        public EventWindow(int startIndex, int endIndex, double samplingRate) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.startTime = startIndex / samplingRate;
            this.endTime = endIndex / samplingRate;
            this.duration = this.endTime - this.startTime;
        }
    }
    
    /**
     * 使用默认参数进行STA/LTA检测
     */
    public static DetectionResult detect(double[] signal, double samplingRate) {
        return detect(signal, samplingRate, 
                     DEFAULT_STA_LENGTH_SEC, DEFAULT_LTA_LENGTH_SEC,
                     DEFAULT_THRESHOLD_ON, DEFAULT_THRESHOLD_OFF,
                     DEFAULT_MIN_EVENT_LENGTH_SEC);
    }
    
    /**
     * 完整参数的STA/LTA检测
     */
    public static DetectionResult detect(double[] signal, double samplingRate,
                                       double staLengthSec, double ltaLengthSec,
                                       double thresholdOn, double thresholdOff,
                                       double minEventLengthSec) {
        
        if (signal == null || signal.length == 0) {
            return new DetectionResult(new double[0], new ArrayList<>());
        }
        
        // 转换时间窗口为采样点数
        int staLength = (int) Math.round(staLengthSec * samplingRate);
        int ltaLength = (int) Math.round(ltaLengthSec * samplingRate);
        int minEventLength = (int) Math.round(minEventLengthSec * samplingRate);
        
        // 确保窗口长度合理
        staLength = Math.max(1, Math.min(staLength, signal.length / 4));
        ltaLength = Math.max(staLength * 2, Math.min(ltaLength, signal.length / 2));
        
        // 计算STA/LTA比值
        double[] staLtaRatio = computeSTALTARatio(signal, staLength, ltaLength);
        
        // 检测事件
        List<EventWindow> events = detectEvents(staLtaRatio, samplingRate,
                                               thresholdOn, thresholdOff, minEventLength);
        
        // 计算事件期间的最大比值
        for (EventWindow event : events) {
            event.maxRatio = 0.0;
            for (int i = event.startIndex; i <= event.endIndex && i < staLtaRatio.length; i++) {
                if (staLtaRatio[i] > event.maxRatio) {
                    event.maxRatio = staLtaRatio[i];
                }
            }
        }
        
        return new DetectionResult(staLtaRatio, events);
    }
    
    /**
     * 计算STA/LTA比值序列（递归实现）
     */
    private static double[] computeSTALTARatio(double[] signal, int staLength, int ltaLength) {
        int n = signal.length;
        double[] staLtaRatio = new double[n];
        
        // 递归系数
        double alphaSta = 1.0 / staLength;
        double alphaLta = 1.0 / ltaLength;
        
        double sta = 0.0;
        double lta = 0.0;
        
        // 初始化LTA（使用前ltaLength个点）
        for (int i = 0; i < Math.min(ltaLength, n); i++) {
            double energy = signal[i] * signal[i];
            lta += energy;
        }
        lta /= Math.min(ltaLength, n);
        
        // 计算STA/LTA比值
        for (int i = 0; i < n; i++) {
            double energy = signal[i] * signal[i];
            
            // 更新STA（递归）
            if (i == 0) {
                sta = energy;
            } else {
                sta = alphaSta * energy + (1 - alphaSta) * sta;
            }
            
            // 更新LTA（递归）
            if (i >= ltaLength) {
                lta = alphaLta * energy + (1 - alphaLta) * lta;
            }
            
            // 计算比值（避免除零）
            if (lta > 1e-10) {
                staLtaRatio[i] = sta / lta;
            } else {
                staLtaRatio[i] = 0.0;
            }
        }
        
        return staLtaRatio;
    }
    
    /**
     * 基于阈值检测事件窗口
     */
    private static List<EventWindow> detectEvents(double[] staLtaRatio, double samplingRate,
                                                 double thresholdOn, double thresholdOff,
                                                 int minEventLength) {
        List<EventWindow> events = new ArrayList<>();
        boolean inEvent = false;
        int eventStart = 0;
        
        for (int i = 0; i < staLtaRatio.length; i++) {
            if (!inEvent && staLtaRatio[i] > thresholdOn) {
                // 事件开始
                inEvent = true;
                eventStart = i;
            } else if (inEvent && staLtaRatio[i] < thresholdOff) {
                // 事件结束
                inEvent = false;
                int eventEnd = i;
                
                // 检查事件长度是否满足最小要求
                if (eventEnd - eventStart >= minEventLength) {
                    events.add(new EventWindow(eventStart, eventEnd, samplingRate));
                }
            }
        }
        
        // 处理在信号末尾仍在进行的事件
        if (inEvent && staLtaRatio.length - eventStart >= minEventLength) {
            events.add(new EventWindow(eventStart, staLtaRatio.length - 1, samplingRate));
        }
        
        return events;
    }
    
    /**
     * 自适应参数检测（根据信号特征调整参数）
     */
    public static DetectionResult adaptiveDetect(double[] signal, double samplingRate) {
        if (signal == null || signal.length == 0) {
            return new DetectionResult(new double[0], new ArrayList<>());
        }
        
        // 计算信号统计特征
        double signalLength = signal.length / samplingRate;
        double rms = calculateRMS(signal);
        
        // 自适应参数调整
        double staLength = Math.max(1.0, Math.min(3.0, signalLength * 0.05));
        double ltaLength = Math.max(staLength * 5, Math.min(60.0, signalLength * 0.3));
        
        // 基于信号强度调整阈值
        double thresholdOn = rms > 1000 ? 2.5 : 3.5;  // 强信号用较低阈值
        double thresholdOff = thresholdOn * 0.6;
        
        return detect(signal, samplingRate, staLength, ltaLength,
                     thresholdOn, thresholdOff, DEFAULT_MIN_EVENT_LENGTH_SEC);
    }
    
    /**
     * 计算信号RMS值
     */
    private static double calculateRMS(double[] signal) {
        double sum = 0.0;
        for (double value : signal) {
            sum += value * value;
        }
        return Math.sqrt(sum / signal.length);
    }
    
    /**
     * 获取事件检测统计信息
     */
    public static String getDetectionSummary(DetectionResult result, double samplingRate) {
        if (result.events.isEmpty()) {
            return "No events detected";
        }
        
        double totalDuration = 0.0;
        double maxDuration = 0.0;
        double minDuration = Double.MAX_VALUE;
        
        for (EventWindow event : result.events) {
            totalDuration += event.duration;
            maxDuration = Math.max(maxDuration, event.duration);
            minDuration = Math.min(minDuration, event.duration);
        }
        
        double avgDuration = totalDuration / result.events.size();
        
        return String.format(
            "Events: %d, Max Ratio: %.2f, Avg Duration: %.2fs, Max Duration: %.2fs, Min Duration: %.2fs",
            result.totalEvents, result.maxRatio, avgDuration, maxDuration, minDuration
        );
    }
}
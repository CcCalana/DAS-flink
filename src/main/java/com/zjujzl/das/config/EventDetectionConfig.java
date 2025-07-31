package com.zjujzl.das.config;

/**
 * 事件检测配置类
 * 集中管理STA/LTA检测参数和系统配置
 */
public class EventDetectionConfig {
    
    // ==================== STA/LTA 基础参数 ====================
    
    /** 默认短期窗口长度（秒） */
    public static final double DEFAULT_STA_LENGTH_SEC = 2.0;
    
    /** 默认长期窗口长度（秒） */
    public static final double DEFAULT_LTA_LENGTH_SEC = 30.0;
    
    /** 默认触发阈值 */
    public static final double DEFAULT_THRESHOLD_ON = 3.0;
    
    /** 默认结束阈值 */
    public static final double DEFAULT_THRESHOLD_OFF = 1.5;
    
    /** 默认最小事件长度（秒） */
    public static final double DEFAULT_MIN_EVENT_LENGTH_SEC = 1.0;
    
    // ==================== 自适应参数配置 ====================
    
    /** 启用自适应检测 */
    public static final boolean ENABLE_ADAPTIVE_DETECTION = true;
    
    /** 自适应STA窗口范围：最小值（秒） */
    public static final double ADAPTIVE_STA_MIN_SEC = 1.0;
    
    /** 自适应STA窗口范围：最大值（秒） */
    public static final double ADAPTIVE_STA_MAX_SEC = 5.0;
    
    /** 自适应LTA窗口范围：最小值（秒） */
    public static final double ADAPTIVE_LTA_MIN_SEC = 10.0;
    
    /** 自适应LTA窗口范围：最大值（秒） */
    public static final double ADAPTIVE_LTA_MAX_SEC = 120.0;
    
    /** 自适应阈值范围：最小值 */
    public static final double ADAPTIVE_THRESHOLD_MIN = 2.0;
    
    /** 自适应阈值范围：最大值 */
    public static final double ADAPTIVE_THRESHOLD_MAX = 5.0;
    
    // ==================== 信号质量控制 ====================
    
    /** 最小信号长度（采样点数） */
    public static final int MIN_SIGNAL_LENGTH = 100;
    
    /** 最大信号长度（采样点数） */
    public static final int MAX_SIGNAL_LENGTH = 1000000;
    
    /** 最小采样率（Hz） */
    public static final double MIN_SAMPLING_RATE = 10.0;
    
    /** 最大采样率（Hz） */
    public static final double MAX_SAMPLING_RATE = 10000.0;
    
    /** 信号质量阈值 */
    public static final double SIGNAL_QUALITY_THRESHOLD = 0.6;
    
    /** 高质量检测的最小比值 */
    public static final double HIGH_QUALITY_MIN_RATIO = 2.5;
    
    // ==================== 性能配置 ====================
    
    /** 最大处理时间（毫秒） */
    public static final long MAX_PROCESSING_TIME_MS = 5000;
    
    /** 统计输出间隔（处理记录数） */
    public static final int STATS_OUTPUT_INTERVAL = 100;
    
    /** 状态清理间隔（毫秒） */
    public static final long STATE_CLEANUP_INTERVAL_MS = 300000; // 5分钟
    
    /** 事件缓存超时（毫秒） */
    public static final long EVENT_CACHE_TIMEOUT_MS = 60000; // 1分钟
    
    // ==================== 算法特定配置 ====================
    
    /**
     * 算法A配置：空间平均 → 移动微分 → 频域去噪
     */
    public static class AlgorithmA {
        public static final int SPATIAL_WINDOW_SIZE = 21;
        public static final int DIFFERENTIATOR_ORDER = 1;
        public static final double STA_LENGTH_SEC = 1.5;
        public static final double LTA_LENGTH_SEC = 25.0;
        public static final double THRESHOLD_ON = 3.2;
        public static final double THRESHOLD_OFF = 1.6;
    }
    
    /**
     * 算法B配置：小波去噪 → 空间平均 → 频域去噪
     */
    public static class AlgorithmB {
        public static final String WAVELET_TYPE = "sym5";
        public static final int WAVELET_LEVELS = 5;
        public static final int SPATIAL_WINDOW_SIZE = 15;
        public static final double STA_LENGTH_SEC = 2.0;
        public static final double LTA_LENGTH_SEC = 30.0;
        public static final double THRESHOLD_ON = 2.8;
        public static final double THRESHOLD_OFF = 1.4;
    }
    
    /**
     * 算法C配置：EMD分解 → 主成分重构 → SVD滤波 → 频域去噪
     */
    public static class AlgorithmC {
        public static final int EMD_MAX_IMFS = 5;
        public static final int EMD_COMPONENTS_TO_USE = 3;
        public static final int SVD_COMPONENTS = 8;
        public static final double STA_LENGTH_SEC = 2.5;
        public static final double LTA_LENGTH_SEC = 35.0;
        public static final double THRESHOLD_ON = 2.5;
        public static final double THRESHOLD_OFF = 1.3;
    }
    
    /**
     * 算法D配置：SVD滤波 → 小波去噪 → 空间平均
     */
    public static class AlgorithmD {
        public static final int SVD_COMPONENTS = 10;
        public static final String WAVELET_TYPE = "db4";
        public static final int WAVELET_LEVELS = 4;
        public static final int SPATIAL_WINDOW_SIZE = 11;
        public static final double STA_LENGTH_SEC = 1.8;
        public static final double LTA_LENGTH_SEC = 28.0;
        public static final double THRESHOLD_ON = 3.5;
        public static final double THRESHOLD_OFF = 1.7;
    }
    
    // ==================== 地震学专用配置 ====================
    
    /**
     * P波检测配置
     */
    public static class PWaveDetection {
        public static final double STA_LENGTH_SEC = 0.5;  // P波检测用较短STA
        public static final double LTA_LENGTH_SEC = 10.0;
        public static final double THRESHOLD_ON = 4.0;    // P波需要较高阈值
        public static final double THRESHOLD_OFF = 2.0;
        public static final double MIN_EVENT_LENGTH_SEC = 0.3;
    }
    
    /**
     * S波检测配置
     */
    public static class SWaveDetection {
        public static final double STA_LENGTH_SEC = 1.0;  // S波检测用中等STA
        public static final double LTA_LENGTH_SEC = 15.0;
        public static final double THRESHOLD_ON = 3.0;
        public static final double THRESHOLD_OFF = 1.5;
        public static final double MIN_EVENT_LENGTH_SEC = 0.5;
    }
    
    /**
     * 区域地震检测配置
     */
    public static class RegionalDetection {
        public static final double STA_LENGTH_SEC = 3.0;  // 区域地震用较长STA
        public static final double LTA_LENGTH_SEC = 60.0;
        public static final double THRESHOLD_ON = 2.5;
        public static final double THRESHOLD_OFF = 1.2;
        public static final double MIN_EVENT_LENGTH_SEC = 2.0;
    }
    
    /**
     * 远震检测配置
     */
    public static class TeleseismicDetection {
        public static final double STA_LENGTH_SEC = 5.0;  // 远震用最长STA
        public static final double LTA_LENGTH_SEC = 120.0;
        public static final double THRESHOLD_ON = 2.0;
        public static final double THRESHOLD_OFF = 1.1;
        public static final double MIN_EVENT_LENGTH_SEC = 5.0;
    }
    
    // ==================== DAS特定配置 ====================
    
    /**
     * DAS光纤声学传感特定配置
     */
    public static class DASSpecific {
        /** DAS系统典型采样率范围 */
        public static final double TYPICAL_SAMPLING_RATE_MIN = 100.0;
        public static final double TYPICAL_SAMPLING_RATE_MAX = 2000.0;
        
        /** DAS系统空间分辨率（米） */
        public static final double SPATIAL_RESOLUTION_M = 1.0;
        
        /** DAS系统典型光纤长度（公里） */
        public static final double TYPICAL_FIBER_LENGTH_KM = 50.0;
        
        /** DAS噪声特征：相干噪声抑制参数 */
        public static final int COHERENT_NOISE_WINDOW = 51;
        
        /** DAS信号增强：空间相关性利用窗口 */
        public static final int SPATIAL_CORRELATION_WINDOW = 21;
        
        /** DAS特定STA/LTA参数 */
        public static final double DAS_STA_LENGTH_SEC = 1.5;
        public static final double DAS_LTA_LENGTH_SEC = 20.0;
        public static final double DAS_THRESHOLD_ON = 3.5;
        public static final double DAS_THRESHOLD_OFF = 1.8;
    }
    
    // ==================== 环境自适应配置 ====================
    
    /**
     * 噪声环境分类
     */
    public enum NoiseEnvironment {
        QUIET(1.5, 2.0),      // 安静环境：低阈值
        MODERATE(2.5, 3.0),   // 中等噪声：标准阈值
        NOISY(3.5, 4.0),      // 高噪声：高阈值
        VERY_NOISY(4.5, 5.0); // 极高噪声：很高阈值
        
        public final double thresholdOn;
        public final double thresholdOff;
        
        NoiseEnvironment(double thresholdOff, double thresholdOn) {
            this.thresholdOn = thresholdOn;
            this.thresholdOff = thresholdOff;
        }
    }
    
    /**
     * 信号类型分类
     */
    public enum SignalType {
        LOCAL_EARTHQUAKE(0.5, 5.0, 4.0),     // 本地地震：短STA，高阈值
        REGIONAL_EARTHQUAKE(2.0, 30.0, 3.0), // 区域地震：中等参数
        TELESEISMIC(5.0, 60.0, 2.5),         // 远震：长STA，低阈值
        MICROSEISMIC(1.0, 15.0, 3.5),        // 微震：短中等参数
        NOISE(0.5, 10.0, 5.0);               // 噪声：高阈值
        
        public final double staLength;
        public final double ltaLength;
        public final double threshold;
        
        SignalType(double staLength, double ltaLength, double threshold) {
            this.staLength = staLength;
            this.ltaLength = ltaLength;
            this.threshold = threshold;
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 根据采样率调整窗口长度
     */
    public static int adjustWindowLength(double lengthSec, double samplingRate) {
        return Math.max(1, (int) Math.round(lengthSec * samplingRate));
    }
    
    /**
     * 根据信号长度调整LTA窗口
     */
    public static double adjustLTALength(double signalLengthSec, double defaultLTASec) {
        return Math.min(defaultLTASec, signalLengthSec * 0.3);
    }
    
    /**
     * 根据噪声水平调整阈值
     */
    public static double adjustThreshold(double baseThreshold, double noiseLevel) {
        // 噪声水平越高，阈值越高
        return baseThreshold * (1.0 + Math.log10(Math.max(1.0, noiseLevel)));
    }
    
    /**
     * 获取算法特定配置
     */
    public static STALTAParams getAlgorithmParams(String algorithmType) {
        switch (algorithmType.toUpperCase()) {
            case "A":
                return new STALTAParams(
                    AlgorithmA.STA_LENGTH_SEC,
                    AlgorithmA.LTA_LENGTH_SEC,
                    AlgorithmA.THRESHOLD_ON,
                    AlgorithmA.THRESHOLD_OFF
                );
            case "B":
                return new STALTAParams(
                    AlgorithmB.STA_LENGTH_SEC,
                    AlgorithmB.LTA_LENGTH_SEC,
                    AlgorithmB.THRESHOLD_ON,
                    AlgorithmB.THRESHOLD_OFF
                );
            case "C":
                return new STALTAParams(
                    AlgorithmC.STA_LENGTH_SEC,
                    AlgorithmC.LTA_LENGTH_SEC,
                    AlgorithmC.THRESHOLD_ON,
                    AlgorithmC.THRESHOLD_OFF
                );
            case "D":
                return new STALTAParams(
                    AlgorithmD.STA_LENGTH_SEC,
                    AlgorithmD.LTA_LENGTH_SEC,
                    AlgorithmD.THRESHOLD_ON,
                    AlgorithmD.THRESHOLD_OFF
                );
            default:
                return new STALTAParams(
                    DEFAULT_STA_LENGTH_SEC,
                    DEFAULT_LTA_LENGTH_SEC,
                    DEFAULT_THRESHOLD_ON,
                    DEFAULT_THRESHOLD_OFF
                );
        }
    }
    
    /**
     * STA/LTA参数封装类
     */
    public static class STALTAParams {
        public final double staLengthSec;
        public final double ltaLengthSec;
        public final double thresholdOn;
        public final double thresholdOff;
        
        public STALTAParams(double staLengthSec, double ltaLengthSec, 
                           double thresholdOn, double thresholdOff) {
            this.staLengthSec = staLengthSec;
            this.ltaLengthSec = ltaLengthSec;
            this.thresholdOn = thresholdOn;
            this.thresholdOff = thresholdOff;
        }
        
        @Override
        public String toString() {
            return String.format(
                "STALTAParams{STA=%.1fs, LTA=%.1fs, ThOn=%.1f, ThOff=%.1f}",
                staLengthSec, ltaLengthSec, thresholdOn, thresholdOff
            );
        }
    }
}
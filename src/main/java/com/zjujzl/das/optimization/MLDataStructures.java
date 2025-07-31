package com.zjujzl.das.optimization;

import java.util.*;

/**
 * 机器学习相关数据结构
 * 
 * 包含特征、模型、结果等核心数据结构
 */
public class MLDataStructures {
    
    /**
     * 机器学习配置
     */
    public static class MLConfig {
        private final EventDetectionConfig eventDetectionConfig;
        private final ArrivalPickingConfig arrivalPickingConfig;
        private final NoiseReductionConfig noiseReductionConfig;
        private final QualityConfig qualityConfig;
        private final FeatureConfig featureConfig;
        private final SpectralConfig spectralConfig;
        private final OnlineLearningConfig onlineLearningConfig;
        private final UpdateConfig updateConfig;
        
        private final boolean onlineLearningEnabled;
        private final double minAccuracyThreshold;
        
        public MLConfig(EventDetectionConfig eventDetectionConfig,
                       ArrivalPickingConfig arrivalPickingConfig,
                       NoiseReductionConfig noiseReductionConfig,
                       QualityConfig qualityConfig,
                       FeatureConfig featureConfig,
                       SpectralConfig spectralConfig,
                       OnlineLearningConfig onlineLearningConfig,
                       UpdateConfig updateConfig,
                       boolean onlineLearningEnabled,
                       double minAccuracyThreshold) {
            this.eventDetectionConfig = eventDetectionConfig;
            this.arrivalPickingConfig = arrivalPickingConfig;
            this.noiseReductionConfig = noiseReductionConfig;
            this.qualityConfig = qualityConfig;
            this.featureConfig = featureConfig;
            this.spectralConfig = spectralConfig;
            this.onlineLearningConfig = onlineLearningConfig;
            this.updateConfig = updateConfig;
            this.onlineLearningEnabled = onlineLearningEnabled;
            this.minAccuracyThreshold = minAccuracyThreshold;
        }
        
        // Getters
        public EventDetectionConfig getEventDetectionConfig() { return eventDetectionConfig; }
        public ArrivalPickingConfig getArrivalPickingConfig() { return arrivalPickingConfig; }
        public NoiseReductionConfig getNoiseReductionConfig() { return noiseReductionConfig; }
        public QualityConfig getQualityConfig() { return qualityConfig; }
        public FeatureConfig getFeatureConfig() { return featureConfig; }
        public SpectralConfig getSpectralConfig() { return spectralConfig; }
        public OnlineLearningConfig getOnlineLearningConfig() { return onlineLearningConfig; }
        public UpdateConfig getUpdateConfig() { return updateConfig; }
        public boolean isOnlineLearningEnabled() { return onlineLearningEnabled; }
        public double getMinAccuracyThreshold() { return minAccuracyThreshold; }
        
        public static MLConfig createDefaultConfig() {
            return new MLConfig(
                new EventDetectionConfig(0.7, 0.1, 100),
                new ArrivalPickingConfig(0.8, 0.05, 50),
                new NoiseReductionConfig(0.6, 0.2, "adaptive"),
                new QualityConfig(0.75, 0.8, 0.9),
                new FeatureConfig(true, true, true, true),
                new SpectralConfig(1024, 0.5, 100.0),
                new OnlineLearningConfig(0.01, 100, 1000),
                new UpdateConfig(3600, 0.05, 10),
                true,
                0.7
            );
        }
    }
    
    /**
     * 特征集合
     */
    public static class FeatureSet {
        private final TimeFeatures timeFeatures;
        private final FrequencyFeatures frequencyFeatures;
        private final StatisticalFeatures statisticalFeatures;
        private final SpatialFeatures spatialFeatures;
        private final double overallQuality;
        
        public FeatureSet(TimeFeatures timeFeatures, FrequencyFeatures frequencyFeatures,
                         StatisticalFeatures statisticalFeatures, SpatialFeatures spatialFeatures) {
            this.timeFeatures = timeFeatures;
            this.frequencyFeatures = frequencyFeatures;
            this.statisticalFeatures = statisticalFeatures;
            this.spatialFeatures = spatialFeatures;
            this.overallQuality = calculateOverallQuality();
        }
        
        private double calculateOverallQuality() {
            double timeQuality = timeFeatures != null ? timeFeatures.getQuality() : 0.0;
            double freqQuality = frequencyFeatures != null ? frequencyFeatures.getQuality() : 0.0;
            double statQuality = statisticalFeatures != null ? statisticalFeatures.getQuality() : 0.0;
            double spatialQuality = spatialFeatures != null ? spatialFeatures.getQuality() : 0.0;
            
            return (timeQuality + freqQuality + statQuality + spatialQuality) / 4.0;
        }
        
        public TimeFeatures getTimeFeatures() { return timeFeatures; }
        public FrequencyFeatures getFrequencyFeatures() { return frequencyFeatures; }
        public StatisticalFeatures getStatisticalFeatures() { return statisticalFeatures; }
        public SpatialFeatures getSpatialFeatures() { return spatialFeatures; }
        public double getOverallQuality() { return overallQuality; }
    }
    
    /**
     * 时域特征
     */
    public static class TimeFeatures {
        private final double[] amplitudes;
        private final double[] energies;
        private final double[] zeroCrossings;
        private final double quality;
        
        public TimeFeatures(double[] amplitudes, double[] energies, double[] zeroCrossings) {
            this.amplitudes = amplitudes.clone();
            this.energies = energies.clone();
            this.zeroCrossings = zeroCrossings.clone();
            this.quality = calculateQuality();
        }
        
        private double calculateQuality() {
            // 基于信噪比和稳定性计算质量
            double snr = calculateSNR();
            double stability = calculateStability();
            return (snr + stability) / 2.0;
        }
        
        private double calculateSNR() {
            if (amplitudes.length == 0) return 0.0;
            
            double signal = Arrays.stream(amplitudes).map(Math::abs).max().orElse(0.0);
            double noise = Arrays.stream(amplitudes).map(Math::abs).average().orElse(0.0);
            
            return noise > 0 ? Math.min(1.0, signal / noise / 10.0) : 0.0;
        }
        
        private double calculateStability() {
            if (energies.length < 2) return 0.0;
            
            double mean = Arrays.stream(energies).average().orElse(0.0);
            double variance = Arrays.stream(energies)
                .map(e -> Math.pow(e - mean, 2))
                .average().orElse(0.0);
            
            return mean > 0 ? Math.max(0.0, 1.0 - Math.sqrt(variance) / mean) : 0.0;
        }
        
        public double[] getAmplitudes() { return amplitudes.clone(); }
        public double[] getEnergies() { return energies.clone(); }
        public double[] getZeroCrossings() { return zeroCrossings.clone(); }
        public double getQuality() { return quality; }
    }
    
    /**
     * 频域特征
     */
    public static class FrequencyFeatures {
        private final double[] spectrum;
        private final double[] dominantFrequencies;
        private final double spectralCentroid;
        private final double spectralBandwidth;
        private final double complexity;
        private final double quality;
        
        public FrequencyFeatures(double[] spectrum, double[] dominantFrequencies,
                               double spectralCentroid, double spectralBandwidth) {
            this.spectrum = spectrum.clone();
            this.dominantFrequencies = dominantFrequencies.clone();
            this.spectralCentroid = spectralCentroid;
            this.spectralBandwidth = spectralBandwidth;
            this.complexity = calculateComplexity();
            this.quality = calculateQuality();
        }
        
        private double calculateComplexity() {
            // 基于频谱分布计算复杂度
            if (spectrum.length == 0) return 0.0;
            
            double totalEnergy = Arrays.stream(spectrum).sum();
            if (totalEnergy == 0) return 0.0;
            
            double entropy = 0.0;
            for (double value : spectrum) {
                if (value > 0) {
                    double p = value / totalEnergy;
                    entropy -= p * Math.log(p);
                }
            }
            
            return Math.min(1.0, entropy / Math.log(spectrum.length));
        }
        
        private double calculateQuality() {
            // 基于频谱清晰度和稳定性计算质量
            double clarity = calculateSpectralClarity();
            double stability = calculateSpectralStability();
            return (clarity + stability) / 2.0;
        }
        
        private double calculateSpectralClarity() {
            if (spectrum.length == 0) return 0.0;
            
            double maxPeak = Arrays.stream(spectrum).max().orElse(0.0);
            double avgLevel = Arrays.stream(spectrum).average().orElse(0.0);
            
            return avgLevel > 0 ? Math.min(1.0, maxPeak / avgLevel / 10.0) : 0.0;
        }
        
        private double calculateSpectralStability() {
            // 简化实现：基于主频稳定性
            return dominantFrequencies.length > 0 ? 0.8 : 0.3;
        }
        
        public double[] getSpectrum() { return spectrum.clone(); }
        public double[] getDominantFrequencies() { return dominantFrequencies.clone(); }
        public double getSpectralCentroid() { return spectralCentroid; }
        public double getSpectralBandwidth() { return spectralBandwidth; }
        public double getComplexity() { return complexity; }
        public double getQuality() { return quality; }
    }
    
    /**
     * 统计特征
     */
    public static class StatisticalFeatures {
        private final double mean;
        private final double variance;
        private final double skewness;
        private final double kurtosis;
        private final double noiseLevel;
        private final double quality;
        
        public StatisticalFeatures(double mean, double variance, double skewness, 
                                 double kurtosis, double noiseLevel) {
            this.mean = mean;
            this.variance = variance;
            this.skewness = skewness;
            this.kurtosis = kurtosis;
            this.noiseLevel = noiseLevel;
            this.quality = calculateQuality();
        }
        
        private double calculateQuality() {
            // 基于统计特征的合理性计算质量
            double normalityScore = calculateNormalityScore();
            double noiseScore = Math.max(0.0, 1.0 - noiseLevel);
            return (normalityScore + noiseScore) / 2.0;
        }
        
        private double calculateNormalityScore() {
            // 基于偏度和峰度评估数据的正态性
            double skewnessScore = Math.max(0.0, 1.0 - Math.abs(skewness) / 3.0);
            double kurtosisScore = Math.max(0.0, 1.0 - Math.abs(kurtosis - 3.0) / 5.0);
            return (skewnessScore + kurtosisScore) / 2.0;
        }
        
        public double getMean() { return mean; }
        public double getVariance() { return variance; }
        public double getSkewness() { return skewness; }
        public double getKurtosis() { return kurtosis; }
        public double getNoiseLevel() { return noiseLevel; }
        public double getQuality() { return quality; }
    }
    
    /**
     * 空间特征
     */
    public static class SpatialFeatures {
        private final double[] spatialCorrelation;
        private final double coherence;
        private final double variability;
        private final double quality;
        
        public SpatialFeatures(double[] spatialCorrelation, double coherence, double variability) {
            this.spatialCorrelation = spatialCorrelation.clone();
            this.coherence = coherence;
            this.variability = variability;
            this.quality = calculateQuality();
        }
        
        private double calculateQuality() {
            // 基于空间一致性和相关性计算质量
            double coherenceScore = coherence;
            double correlationScore = calculateCorrelationScore();
            return (coherenceScore + correlationScore) / 2.0;
        }
        
        private double calculateCorrelationScore() {
            if (spatialCorrelation.length == 0) return 0.0;
            
            double avgCorrelation = Arrays.stream(spatialCorrelation)
                .map(Math::abs)
                .average()
                .orElse(0.0);
            
            return Math.min(1.0, avgCorrelation);
        }
        
        public double[] getSpatialCorrelation() { return spatialCorrelation.clone(); }
        public double getCoherence() { return coherence; }
        public double getVariability() { return variability; }
        public double getQuality() { return quality; }
    }
    
    /**
     * 事件检测结果
     */
    public static class EventDetectionResult {
        private final List<ClassifiedEvent> events;
        private final double averageConfidence;
        
        public EventDetectionResult(List<ClassifiedEvent> events, double averageConfidence) {
            this.events = new ArrayList<>(events);
            this.averageConfidence = averageConfidence;
        }
        
        public List<ClassifiedEvent> getEvents() { return new ArrayList<>(events); }
        public double getAverageConfidence() { return averageConfidence; }
        public boolean hasEvents() { return !events.isEmpty(); }
        public int getEventCount() { return events.size(); }
    }
    
    /**
     * 分类事件
     */
    public static class ClassifiedEvent {
        private final long startTime;
        private final long endTime;
        private final EventType type;
        private final double confidence;
        private final Map<String, Double> attributes;
        
        public ClassifiedEvent(long startTime, long endTime, EventType type, 
                             double confidence, Map<String, Double> attributes) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.type = type;
            this.confidence = confidence;
            this.attributes = new HashMap<>(attributes);
        }
        
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public EventType getType() { return type; }
        public double getConfidence() { return confidence; }
        public Map<String, Double> getAttributes() { return new HashMap<>(attributes); }
        public long getDuration() { return endTime - startTime; }
    }
    
    /**
     * 到时拾取结果
     */
    public static class ArrivalPickingResult {
        private final List<ArrivalPick> picks;
        private final double quality;
        
        public ArrivalPickingResult(List<ArrivalPick> picks, double quality) {
            this.picks = new ArrayList<>(picks);
            this.quality = quality;
        }
        
        public List<ArrivalPick> getPicks() { return new ArrayList<>(picks); }
        public double getQuality() { return quality; }
        public int getPickCount() { return picks.size(); }
    }
    
    /**
     * 到时拾取
     */
    public static class ArrivalPick {
        private final long arrivalTime;
        private final PhaseType phase;
        private final double confidence;
        private final int channelIndex;
        
        public ArrivalPick(long arrivalTime, PhaseType phase, double confidence, int channelIndex) {
            this.arrivalTime = arrivalTime;
            this.phase = phase;
            this.confidence = confidence;
            this.channelIndex = channelIndex;
        }
        
        public long getArrivalTime() { return arrivalTime; }
        public PhaseType getPhase() { return phase; }
        public double getConfidence() { return confidence; }
        public int getChannelIndex() { return channelIndex; }
    }
    
    /**
     * 质量评估
     */
    public static class QualityAssessment {
        private final double signalQuality;
        private final double processingQuality;
        private final double dataIntegrity;
        private final double overallQuality;
        private final String qualityReport;
        
        public QualityAssessment(double signalQuality, double processingQuality, 
                               double dataIntegrity, double overallQuality, String qualityReport) {
            this.signalQuality = signalQuality;
            this.processingQuality = processingQuality;
            this.dataIntegrity = dataIntegrity;
            this.overallQuality = overallQuality;
            this.qualityReport = qualityReport;
        }
        
        public double getSignalQuality() { return signalQuality; }
        public double getProcessingQuality() { return processingQuality; }
        public double getDataIntegrity() { return dataIntegrity; }
        public double getOverallQuality() { return overallQuality; }
        public String getQualityReport() { return qualityReport; }
    }
    
    /**
     * ML处理结果
     */
    public static class MLProcessingResult {
        private final EdgeDataStructures.DASStreamData processedData;
        private final EventDetectionResult eventResult;
        private final ArrivalPickingResult arrivalResult;
        private final QualityAssessment qualityAssessment;
        private final FeatureSet features;
        private final long processingTime;
        
        public MLProcessingResult(EdgeDataStructures.DASStreamData processedData,
                                EventDetectionResult eventResult,
                                ArrivalPickingResult arrivalResult,
                                QualityAssessment qualityAssessment,
                                FeatureSet features,
                                long processingTime) {
            this.processedData = processedData;
            this.eventResult = eventResult;
            this.arrivalResult = arrivalResult;
            this.qualityAssessment = qualityAssessment;
            this.features = features;
            this.processingTime = processingTime;
        }
        
        public EdgeDataStructures.DASStreamData getProcessedData() { return processedData; }
        public EventDetectionResult getEventResult() { return eventResult; }
        public ArrivalPickingResult getArrivalResult() { return arrivalResult; }
        public QualityAssessment getQualityAssessment() { return qualityAssessment; }
        public FeatureSet getFeatures() { return features; }
        public long getProcessingTime() { return processingTime; }
    }
    
    // 配置类
    
    public static class EventDetectionConfig {
        private final double threshold;
        private final double minDuration;
        private final int windowSize;
        
        public EventDetectionConfig(double threshold, double minDuration, int windowSize) {
            this.threshold = threshold;
            this.minDuration = minDuration;
            this.windowSize = windowSize;
        }
        
        public double getThreshold() { return threshold; }
        public double getMinDuration() { return minDuration; }
        public int getWindowSize() { return windowSize; }
    }
    
    public static class ArrivalPickingConfig {
        private final double confidence;
        private final double precision;
        private final int maxPicks;
        
        public ArrivalPickingConfig(double confidence, double precision, int maxPicks) {
            this.confidence = confidence;
            this.precision = precision;
            this.maxPicks = maxPicks;
        }
        
        public double getConfidence() { return confidence; }
        public double getPrecision() { return precision; }
        public int getMaxPicks() { return maxPicks; }
    }
    
    public static class NoiseReductionConfig {
        private final double threshold;
        private final double strength;
        private final String method;
        
        public NoiseReductionConfig(double threshold, double strength, String method) {
            this.threshold = threshold;
            this.strength = strength;
            this.method = method;
        }
        
        public double getThreshold() { return threshold; }
        public double getStrength() { return strength; }
        public String getMethod() { return method; }
    }
    
    public static class QualityConfig {
        private final double minSignalQuality;
        private final double minProcessingQuality;
        private final double minDataIntegrity;
        
        public QualityConfig(double minSignalQuality, double minProcessingQuality, double minDataIntegrity) {
            this.minSignalQuality = minSignalQuality;
            this.minProcessingQuality = minProcessingQuality;
            this.minDataIntegrity = minDataIntegrity;
        }
        
        public double getMinSignalQuality() { return minSignalQuality; }
        public double getMinProcessingQuality() { return minProcessingQuality; }
        public double getMinDataIntegrity() { return minDataIntegrity; }
    }
    
    public static class FeatureConfig {
        private final boolean enableTimeFeatures;
        private final boolean enableFrequencyFeatures;
        private final boolean enableStatisticalFeatures;
        private final boolean enableSpatialFeatures;
        
        public FeatureConfig(boolean enableTimeFeatures, boolean enableFrequencyFeatures,
                           boolean enableStatisticalFeatures, boolean enableSpatialFeatures) {
            this.enableTimeFeatures = enableTimeFeatures;
            this.enableFrequencyFeatures = enableFrequencyFeatures;
            this.enableStatisticalFeatures = enableStatisticalFeatures;
            this.enableSpatialFeatures = enableSpatialFeatures;
        }
        
        public boolean isEnableTimeFeatures() { return enableTimeFeatures; }
        public boolean isEnableFrequencyFeatures() { return enableFrequencyFeatures; }
        public boolean isEnableStatisticalFeatures() { return enableStatisticalFeatures; }
        public boolean isEnableSpatialFeatures() { return enableSpatialFeatures; }
    }
    
    public static class SpectralConfig {
        private final int fftSize;
        private final double overlap;
        private final double maxFrequency;
        
        public SpectralConfig(int fftSize, double overlap, double maxFrequency) {
            this.fftSize = fftSize;
            this.overlap = overlap;
            this.maxFrequency = maxFrequency;
        }
        
        public int getFftSize() { return fftSize; }
        public double getOverlap() { return overlap; }
        public double getMaxFrequency() { return maxFrequency; }
    }
    
    public static class OnlineLearningConfig {
        private final double learningRate;
        private final int batchSize;
        private final int maxSamples;
        
        public OnlineLearningConfig(double learningRate, int batchSize, int maxSamples) {
            this.learningRate = learningRate;
            this.batchSize = batchSize;
            this.maxSamples = maxSamples;
        }
        
        public double getLearningRate() { return learningRate; }
        public int getBatchSize() { return batchSize; }
        public int getMaxSamples() { return maxSamples; }
    }
    
    public static class UpdateConfig {
        private final long updateInterval;
        private final double performanceThreshold;
        private final int maxRetries;
        
        public UpdateConfig(long updateInterval, double performanceThreshold, int maxRetries) {
            this.updateInterval = updateInterval;
            this.performanceThreshold = performanceThreshold;
            this.maxRetries = maxRetries;
        }
        
        public long getUpdateInterval() { return updateInterval; }
        public double getPerformanceThreshold() { return performanceThreshold; }
        public int getMaxRetries() { return maxRetries; }
    }
    
    // 枚举类型
    
    public enum EventType {
        SEISMIC_EVENT("地震事件"),
        NOISE_EVENT("噪声事件"),
        CALIBRATION_EVENT("校准事件"),
        UNKNOWN_EVENT("未知事件");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum PhaseType {
        P_WAVE("P波"),
        S_WAVE("S波"),
        SURFACE_WAVE("面波"),
        UNKNOWN("未知");
        
        private final String description;
        
        PhaseType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
}
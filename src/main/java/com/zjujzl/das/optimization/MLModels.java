package com.zjujzl.das.optimization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 机器学习模型实现
 * 
 * 包含事件检测、到时拾取、噪声降低等核心ML模型
 */
public class MLModels {
    
    /**
     * 事件检测模型
     */
    public static class EventDetectionModel {
        private final MLDataStructures.EventDetectionConfig config;
        private final Map<String, Double> modelWeights;
        private volatile double accuracy = 0.85;
        
        public EventDetectionModel(MLDataStructures.EventDetectionConfig config) {
            this.config = config;
            this.modelWeights = new ConcurrentHashMap<>();
            initializeWeights();
        }
        
        public List<DetectedEvent> detectEvents(EdgeDataStructures.DASStreamData streamData, 
                                              MLDataStructures.FeatureSet features) {
            List<DetectedEvent> events = new ArrayList<>();
            
            // 基于能量阈值的初步检测
            List<TimeWindow> candidates = findEventCandidates(streamData, features);
            
            // ML模型验证
            for (TimeWindow candidate : candidates) {
                double confidence = calculateEventConfidence(candidate, features);
                if (confidence > config.getThreshold()) {
                    events.add(new DetectedEvent(
                        candidate.getStartTime(),
                        candidate.getEndTime(),
                        confidence
                    ));
                }
            }
            
            return events;
        }
        
        public MLDataStructures.ClassifiedEvent classifyEvent(DetectedEvent event, 
                                                             MLDataStructures.FeatureSet features) {
            // 事件分类逻辑
            MLDataStructures.EventType type = classifyEventType(event, features);
            double confidence = refineEventConfidence(event, features, type);
            
            Map<String, Double> attributes = extractEventAttributes(event, features);
            
            return new MLDataStructures.ClassifiedEvent(
                event.getStartTime(),
                event.getEndTime(),
                type,
                confidence,
                attributes
            );
        }
        
        private void initializeWeights() {
            // 初始化模型权重
            modelWeights.put("energy_weight", 0.3);
            modelWeights.put("frequency_weight", 0.25);
            modelWeights.put("spatial_weight", 0.25);
            modelWeights.put("temporal_weight", 0.2);
        }
        
        private List<TimeWindow> findEventCandidates(EdgeDataStructures.DASStreamData streamData, 
                                                    MLDataStructures.FeatureSet features) {
            List<TimeWindow> candidates = new ArrayList<>();
            
            // 基于能量特征检测候选事件
            double[] energies = features.getTimeFeatures().getEnergies();
            double threshold = calculateAdaptiveThreshold(energies);
            
            boolean inEvent = false;
            long eventStart = 0;
            
            for (int i = 0; i < energies.length; i++) {
                long currentTime = streamData.getTimestamp() + i * 1000; // 假设1秒间隔
                
                if (!inEvent && energies[i] > threshold) {
                    inEvent = true;
                    eventStart = currentTime;
                } else if (inEvent && energies[i] <= threshold) {
                    inEvent = false;
                    long eventEnd = currentTime;
                    
                    if (eventEnd - eventStart >= config.getMinDuration() * 1000) {
                        candidates.add(new TimeWindow(eventStart, eventEnd));
                    }
                }
            }
            
            return candidates;
        }
        
        private double calculateAdaptiveThreshold(double[] energies) {
            if (energies.length == 0) return 0.0;
            
            double mean = Arrays.stream(energies).average().orElse(0.0);
            double std = calculateStandardDeviation(energies, mean);
            
            return mean + 2.0 * std; // 2-sigma阈值
        }
        
        private double calculateStandardDeviation(double[] values, double mean) {
            double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
            return Math.sqrt(variance);
        }
        
        private double calculateEventConfidence(TimeWindow candidate, MLDataStructures.FeatureSet features) {
            // 综合多个特征计算事件置信度
            double energyScore = calculateEnergyScore(features.getTimeFeatures());
            double frequencyScore = calculateFrequencyScore(features.getFrequencyFeatures());
            double spatialScore = calculateSpatialScore(features.getSpatialFeatures());
            double temporalScore = calculateTemporalScore(candidate);
            
            return energyScore * modelWeights.get("energy_weight") +
                   frequencyScore * modelWeights.get("frequency_weight") +
                   spatialScore * modelWeights.get("spatial_weight") +
                   temporalScore * modelWeights.get("temporal_weight");
        }
        
        private MLDataStructures.EventType classifyEventType(DetectedEvent event, 
                                                            MLDataStructures.FeatureSet features) {
            // 简化的事件分类逻辑
            double complexity = features.getFrequencyFeatures().getComplexity();
            double noiseLevel = features.getStatisticalFeatures().getNoiseLevel();
            
            if (noiseLevel > 0.8) {
                return MLDataStructures.EventType.NOISE_EVENT;
            } else if (complexity > 0.7) {
                return MLDataStructures.EventType.SEISMIC_EVENT;
            } else {
                return MLDataStructures.EventType.UNKNOWN_EVENT;
            }
        }
        
        private double refineEventConfidence(DetectedEvent event, MLDataStructures.FeatureSet features, 
                                           MLDataStructures.EventType type) {
            double baseConfidence = event.getConfidence();
            
            // 根据事件类型调整置信度
            switch (type) {
                case SEISMIC_EVENT:
                    return Math.min(1.0, baseConfidence * 1.1);
                case NOISE_EVENT:
                    return Math.max(0.0, baseConfidence * 0.8);
                default:
                    return baseConfidence;
            }
        }
        
        private Map<String, Double> extractEventAttributes(DetectedEvent event, 
                                                          MLDataStructures.FeatureSet features) {
            Map<String, Double> attributes = new HashMap<>();
            
            attributes.put("duration", (double) (event.getEndTime() - event.getStartTime()));
            attributes.put("energy", calculateTotalEnergy(features.getTimeFeatures()));
            attributes.put("dominant_frequency", getDominantFrequency(features.getFrequencyFeatures()));
            attributes.put("spatial_extent", calculateSpatialExtent(features.getSpatialFeatures()));
            
            return attributes;
        }
        
        // 辅助计算方法
        private double calculateEnergyScore(MLDataStructures.TimeFeatures timeFeatures) {
            double[] energies = timeFeatures.getEnergies();
            double maxEnergy = Arrays.stream(energies).max().orElse(0.0);
            return Math.min(1.0, maxEnergy / 100.0); // 归一化
        }
        
        private double calculateFrequencyScore(MLDataStructures.FrequencyFeatures freqFeatures) {
            return freqFeatures.getComplexity();
        }
        
        private double calculateSpatialScore(MLDataStructures.SpatialFeatures spatialFeatures) {
            return spatialFeatures.getCoherence();
        }
        
        private double calculateTemporalScore(TimeWindow window) {
            long duration = window.getEndTime() - window.getStartTime();
            return Math.min(1.0, duration / 10000.0); // 10秒为满分
        }
        
        private double calculateTotalEnergy(MLDataStructures.TimeFeatures timeFeatures) {
            return Arrays.stream(timeFeatures.getEnergies()).sum();
        }
        
        private double getDominantFrequency(MLDataStructures.FrequencyFeatures freqFeatures) {
            double[] frequencies = freqFeatures.getDominantFrequencies();
            return frequencies.length > 0 ? frequencies[0] : 0.0;
        }
        
        private double calculateSpatialExtent(MLDataStructures.SpatialFeatures spatialFeatures) {
            return spatialFeatures.getVariability();
        }
    }
    
    /**
     * 到时拾取模型
     */
    public static class ArrivalPickingModel {
        private final MLDataStructures.ArrivalPickingConfig config;
        private volatile double accuracy = 0.82;
        
        public ArrivalPickingModel(MLDataStructures.ArrivalPickingConfig config) {
            this.config = config;
        }
        
        public List<MLDataStructures.ArrivalPick> pickArrivals(EdgeDataStructures.DASStreamData streamData,
                                                              MLDataStructures.ClassifiedEvent event,
                                                              MLDataStructures.FeatureSet features) {
            List<MLDataStructures.ArrivalPick> picks = new ArrayList<>();
            
            // P波到时拾取
            List<MLDataStructures.ArrivalPick> pPicks = pickPWaveArrivals(streamData, event, features);
            picks.addAll(pPicks);
            
            // S波到时拾取
            List<MLDataStructures.ArrivalPick> sPicks = pickSWaveArrivals(streamData, event, features);
            picks.addAll(sPicks);
            
            // 限制拾取数量
            return picks.stream()
                .sorted((p1, p2) -> Double.compare(p2.getConfidence(), p1.getConfidence()))
                .limit(config.getMaxPicks())
                .collect(Collectors.toList());
        }
        
        public List<MLDataStructures.ArrivalPick> refineArrivals(List<MLDataStructures.ArrivalPick> picks,
                                                               MLDataStructures.FeatureSet features) {
            return picks.stream()
                .map(pick -> refineArrivalPick(pick, features))
                .filter(pick -> pick.getConfidence() >= config.getConfidence())
                .collect(Collectors.toList());
        }
        
        private List<MLDataStructures.ArrivalPick> pickPWaveArrivals(EdgeDataStructures.DASStreamData streamData,
                                                                   MLDataStructures.ClassifiedEvent event,
                                                                   MLDataStructures.FeatureSet features) {
            List<MLDataStructures.ArrivalPick> picks = new ArrayList<>();
            
            // 基于高频特征检测P波
            double[] spectrum = features.getFrequencyFeatures().getSpectrum();
            long eventStart = event.getStartTime();
            
            // 在事件开始附近搜索P波到时
            for (int channel = 0; channel < streamData.getChannelCount(); channel++) {
                long arrivalTime = detectPWaveArrival(eventStart, spectrum, channel);
                if (arrivalTime > 0) {
                    double confidence = calculateArrivalConfidence(arrivalTime, MLDataStructures.PhaseType.P_WAVE, features);
                    if (confidence >= config.getConfidence()) {
                        picks.add(new MLDataStructures.ArrivalPick(
                            arrivalTime,
                            MLDataStructures.PhaseType.P_WAVE,
                            confidence,
                            channel
                        ));
                    }
                }
            }
            
            return picks;
        }
        
        private List<MLDataStructures.ArrivalPick> pickSWaveArrivals(EdgeDataStructures.DASStreamData streamData,
                                                                   MLDataStructures.ClassifiedEvent event,
                                                                   MLDataStructures.FeatureSet features) {
            List<MLDataStructures.ArrivalPick> picks = new ArrayList<>();
            
            // 基于低频特征检测S波
            double[] spectrum = features.getFrequencyFeatures().getSpectrum();
            long eventStart = event.getStartTime();
            
            // 在P波之后搜索S波到时
            for (int channel = 0; channel < streamData.getChannelCount(); channel++) {
                long arrivalTime = detectSWaveArrival(eventStart, spectrum, channel);
                if (arrivalTime > 0) {
                    double confidence = calculateArrivalConfidence(arrivalTime, MLDataStructures.PhaseType.S_WAVE, features);
                    if (confidence >= config.getConfidence()) {
                        picks.add(new MLDataStructures.ArrivalPick(
                            arrivalTime,
                            MLDataStructures.PhaseType.S_WAVE,
                            confidence,
                            channel
                        ));
                    }
                }
            }
            
            return picks;
        }
        
        private long detectPWaveArrival(long eventStart, double[] spectrum, int channel) {
            // 简化的P波检测算法
            // 在实际实现中，这里会使用更复杂的信号处理算法
            return eventStart + (long) (Math.random() * 2000); // 模拟检测结果
        }
        
        private long detectSWaveArrival(long eventStart, double[] spectrum, int channel) {
            // 简化的S波检测算法
            // S波通常在P波之后到达
            return eventStart + (long) (3000 + Math.random() * 2000); // 模拟检测结果
        }
        
        private double calculateArrivalConfidence(long arrivalTime, MLDataStructures.PhaseType phase, 
                                                 MLDataStructures.FeatureSet features) {
            // 基于信号特征计算到时置信度
            double signalQuality = features.getOverallQuality();
            double phaseClarity = calculatePhaseClarity(phase, features);
            
            return (signalQuality + phaseClarity) / 2.0;
        }
        
        private double calculatePhaseClarity(MLDataStructures.PhaseType phase, MLDataStructures.FeatureSet features) {
            // 根据震相类型计算清晰度
            switch (phase) {
                case P_WAVE:
                    return features.getFrequencyFeatures().getQuality() * 0.8;
                case S_WAVE:
                    return features.getFrequencyFeatures().getQuality() * 0.7;
                default:
                    return 0.5;
            }
        }
        
        private MLDataStructures.ArrivalPick refineArrivalPick(MLDataStructures.ArrivalPick pick, 
                                                              MLDataStructures.FeatureSet features) {
            // 精化到时拾取
            double refinedConfidence = pick.getConfidence() * features.getOverallQuality();
            
            return new MLDataStructures.ArrivalPick(
                pick.getArrivalTime(),
                pick.getPhase(),
                refinedConfidence,
                pick.getChannelIndex()
            );
        }
    }
    
    /**
     * 噪声降低模型
     */
    public static class NoiseReductionModel {
        private final MLDataStructures.NoiseReductionConfig config;
        
        public NoiseReductionModel(MLDataStructures.NoiseReductionConfig config) {
            this.config = config;
        }
        
        public NoiseProfile analyzeNoise(MLDataStructures.FeatureSet features) {
            double noiseLevel = features.getStatisticalFeatures().getNoiseLevel();
            NoiseType type = classifyNoiseType(features);
            double[] characteristics = extractNoiseCharacteristics(features);
            
            return new NoiseProfile(noiseLevel, type, characteristics);
        }
        
        public NoiseReductionStrategy selectStrategy(NoiseProfile noiseProfile) {
            switch (noiseProfile.getType()) {
                case WHITE_NOISE:
                    return NoiseReductionStrategy.SPECTRAL_SUBTRACTION;
                case COLORED_NOISE:
                    return NoiseReductionStrategy.ADAPTIVE_FILTERING;
                case IMPULSE_NOISE:
                    return NoiseReductionStrategy.MEDIAN_FILTERING;
                default:
                    return NoiseReductionStrategy.WIENER_FILTERING;
            }
        }
        
        public EdgeDataStructures.DASStreamData applyNoiseReduction(EdgeDataStructures.DASStreamData streamData,
                                                                   NoiseReductionStrategy strategy) {
            // 应用噪声降低策略
            // 这里返回处理后的数据（简化实现）
            return streamData; // 实际实现中会对数据进行处理
        }
        
        private NoiseType classifyNoiseType(MLDataStructures.FeatureSet features) {
            double spectralFlatness = calculateSpectralFlatness(features.getFrequencyFeatures());
            
            if (spectralFlatness > 0.8) {
                return NoiseType.WHITE_NOISE;
            } else if (spectralFlatness > 0.4) {
                return NoiseType.COLORED_NOISE;
            } else {
                return NoiseType.IMPULSE_NOISE;
            }
        }
        
        private double calculateSpectralFlatness(MLDataStructures.FrequencyFeatures freqFeatures) {
            double[] spectrum = freqFeatures.getSpectrum();
            if (spectrum.length == 0) return 0.0;
            
            double geometricMean = calculateGeometricMean(spectrum);
            double arithmeticMean = Arrays.stream(spectrum).average().orElse(0.0);
            
            return arithmeticMean > 0 ? geometricMean / arithmeticMean : 0.0;
        }
        
        private double calculateGeometricMean(double[] values) {
            double product = 1.0;
            int count = 0;
            
            for (double value : values) {
                if (value > 0) {
                    product *= value;
                    count++;
                }
            }
            
            return count > 0 ? Math.pow(product, 1.0 / count) : 0.0;
        }
        
        private double[] extractNoiseCharacteristics(MLDataStructures.FeatureSet features) {
            return new double[]{
                features.getStatisticalFeatures().getVariance(),
                features.getFrequencyFeatures().getSpectralBandwidth(),
                features.getSpatialFeatures().getVariability()
            };
        }
    }
    
    /**
     * 质量评估模型
     */
    public static class QualityAssessmentModel {
        private final MLDataStructures.QualityConfig config;
        
        public QualityAssessmentModel(MLDataStructures.QualityConfig config) {
            this.config = config;
        }
        
        public double assessSignalQuality(EdgeDataStructures.DASStreamData streamData, 
                                         MLDataStructures.FeatureSet features) {
            double snr = calculateSNR(features.getTimeFeatures());
            double spectralQuality = features.getFrequencyFeatures().getQuality();
            double spatialConsistency = features.getSpatialFeatures().getCoherence();
            
            return (snr + spectralQuality + spatialConsistency) / 3.0;
        }
        
        public double assessProcessingQuality(MLDataStructures.EventDetectionResult eventResult,
                                             MLDataStructures.FeatureSet features) {
            if (!eventResult.hasEvents()) {
                return 0.5; // 中等质量，没有检测到事件
            }
            
            double avgConfidence = eventResult.getAverageConfidence();
            double featureQuality = features.getOverallQuality();
            
            return (avgConfidence + featureQuality) / 2.0;
        }
        
        public double assessDataIntegrity(EdgeDataStructures.DASStreamData streamData) {
            // 评估数据完整性
            double completeness = calculateDataCompleteness(streamData);
            double consistency = calculateDataConsistency(streamData);
            
            return (completeness + consistency) / 2.0;
        }
        
        public double predictQuality(MLDataStructures.FeatureSet features, 
                                   MLAssistedProcessor.ProcessingStrategy strategy) {
            double baseQuality = features.getOverallQuality();
            double strategyFactor = getStrategyQualityFactor(strategy);
            
            return Math.min(1.0, baseQuality * strategyFactor);
        }
        
        private double calculateSNR(MLDataStructures.TimeFeatures timeFeatures) {
            double[] amplitudes = timeFeatures.getAmplitudes();
            if (amplitudes.length == 0) return 0.0;
            
            double signal = Arrays.stream(amplitudes).map(Math::abs).max().orElse(0.0);
            double noise = Arrays.stream(amplitudes).map(Math::abs).average().orElse(0.0);
            
            return noise > 0 ? Math.min(1.0, signal / noise / 10.0) : 0.0;
        }
        
        private double calculateDataCompleteness(EdgeDataStructures.DASStreamData streamData) {
            // 简化实现：假设数据是完整的
            return streamData.getChannelCount() > 0 ? 1.0 : 0.0;
        }
        
        private double calculateDataConsistency(EdgeDataStructures.DASStreamData streamData) {
            // 简化实现：基于数据大小和采样率的一致性
            double expectedSize = streamData.getChannelCount() * streamData.getSamplingRate() * 8; // 8字节/样本
            double actualSize = streamData.getDataSize();
            
            return actualSize > 0 ? Math.min(1.0, expectedSize / actualSize) : 0.0;
        }
        
        private double getStrategyQualityFactor(MLAssistedProcessor.ProcessingStrategy strategy) {
            switch (strategy) {
                case HIGH_RESOLUTION:
                    return 1.2;
                case AGGRESSIVE_DENOISING:
                    return 1.1;
                case SPATIAL_ADAPTIVE:
                    return 1.05;
                case BALANCED:
                    return 1.0;
                case FAST_PROCESSING:
                    return 0.9;
                default:
                    return 1.0;
            }
        }
    }
    
    // 辅助类
    
    public static class DetectedEvent {
        private final long startTime;
        private final long endTime;
        private final double confidence;
        
        public DetectedEvent(long startTime, long endTime, double confidence) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.confidence = confidence;
        }
        
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public double getConfidence() { return confidence; }
    }
    
    public static class TimeWindow {
        private final long startTime;
        private final long endTime;
        
        public TimeWindow(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
    }
    
    public static class NoiseProfile {
        private final double level;
        private final NoiseType type;
        private final double[] characteristics;
        
        public NoiseProfile(double level, NoiseType type, double[] characteristics) {
            this.level = level;
            this.type = type;
            this.characteristics = characteristics.clone();
        }
        
        public double getLevel() { return level; }
        public NoiseType getType() { return type; }
        public double[] getCharacteristics() { return characteristics.clone(); }
    }
    
    // 枚举类型
    
    public enum NoiseType {
        WHITE_NOISE,
        COLORED_NOISE,
        IMPULSE_NOISE,
        UNKNOWN_NOISE
    }
    
    public enum NoiseReductionStrategy {
        SPECTRAL_SUBTRACTION,
        ADAPTIVE_FILTERING,
        MEDIAN_FILTERING,
        WIENER_FILTERING
    }
}
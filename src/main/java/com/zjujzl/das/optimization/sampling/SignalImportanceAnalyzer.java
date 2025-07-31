package com.zjujzl.das.optimization.sampling;

import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.algorithm.FFTProcessor;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 信号重要性分析器
 * 
 * 基于多维度特征分析地震信号的重要性
 * 参考文献：
 * - Geophysical Journal International 2023: Signal importance metrics for seismic monitoring
 * - IEEE Transactions on Geoscience 2022: Multi-feature importance analysis
 * - Seismological Research Letters 2021: Adaptive signal processing in DAS
 * 
 * 分析维度：
 * 1. 能量特征（振幅、功率谱密度）
 * 2. 频率特征（主频、频带能量分布）
 * 3. 时域特征（变化率、突变检测）
 * 4. 统计特征（方差、偏度、峰度）
 * 5. 相关性特征（与历史事件的相似性）
 */
public class SignalImportanceAnalyzer {
    
    private final SamplingConfig config;
    private final FFTProcessor fftProcessor;
    
    // 历史数据缓存
    private final Map<String, List<HistoricalFeature>> historicalFeatures;
    private final int maxHistorySize;
    
    // 特征权重
    private final FeatureWeights weights;
    
    public SignalImportanceAnalyzer(SamplingConfig config) {
        this.config = config;
        this.fftProcessor = new FFTProcessor();
        this.historicalFeatures = new HashMap<>();
        this.maxHistorySize = config.getMaxHistorySize();
        this.weights = new FeatureWeights(config);
    }
    
    /**
     * 分析信号重要性
     */
    public ImportanceProfile analyze(SeismicRecord record) {
        if (record == null || record.data == null || record.data.length == 0) {
            return new ImportanceProfile(new double[0], 0.0);
        }
        
        try {
            double[] signal = Arrays.stream(record.data).asDoubleStream().toArray();
            
            // 1. 计算多维度特征
            EnergyFeatures energyFeatures = computeEnergyFeatures(signal);
            FrequencyFeatures frequencyFeatures = computeFrequencyFeatures(signal, record.sampling_rate);
            TemporalFeatures temporalFeatures = computeTemporalFeatures(signal);
            StatisticalFeatures statisticalFeatures = computeStatisticalFeatures(signal);
            CorrelationFeatures correlationFeatures = computeCorrelationFeatures(signal, record.station);
            
            // 2. 计算局部重要性
            double[] localImportance = computeLocalImportance(signal, energyFeatures, frequencyFeatures, temporalFeatures);
            
            // 3. 计算全局重要性
            double overallImportance = computeOverallImportance(
                energyFeatures, frequencyFeatures, temporalFeatures, 
                statisticalFeatures, correlationFeatures
            );
            
            // 4. 更新历史特征
            updateHistoricalFeatures(record.station, energyFeatures, frequencyFeatures, temporalFeatures);
            
            return new ImportanceProfile(localImportance, overallImportance);
            
        } catch (Exception e) {
            System.err.println("ERROR: Signal importance analysis failed: " + e.getMessage());
            return new ImportanceProfile(new double[record.data.length], 0.5);
        }
    }
    
    /**
     * 计算能量特征
     */
    private EnergyFeatures computeEnergyFeatures(double[] signal) {
        EnergyFeatures features = new EnergyFeatures();
        
        // 总能量
        features.totalEnergy = Arrays.stream(signal).map(x -> x * x).sum();
        
        // 平均功率
        features.averagePower = features.totalEnergy / signal.length;
        
        // 峰值功率
        features.peakPower = Arrays.stream(signal).map(Math::abs).max().orElse(0.0);
        features.peakPower *= features.peakPower;
        
        // 能量分布（滑动窗口）
        int windowSize = Math.min(signal.length / 10, 100);
        features.energyDistribution = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(signal.length, i + windowSize / 2);
            
            double windowEnergy = 0.0;
            for (int j = start; j < end; j++) {
                windowEnergy += signal[j] * signal[j];
            }
            features.energyDistribution[i] = windowEnergy / (end - start);
        }
        
        // 能量变化率
        features.energyVariation = computeVariation(features.energyDistribution);
        
        return features;
    }
    
    /**
     * 计算频率特征
     */
    private FrequencyFeatures computeFrequencyFeatures(double[] signal, double samplingRate) {
        FrequencyFeatures features = new FrequencyFeatures();
        
        try {
            // FFT分析
            double[] spectrum = fftProcessor.computePowerSpectrum(signal);
            double[] frequencies = fftProcessor.getFrequencies(signal.length, samplingRate);
            
            // 主频
            int peakIndex = 0;
            double maxPower = spectrum[0];
            for (int i = 1; i < spectrum.length; i++) {
                if (spectrum[i] > maxPower) {
                    maxPower = spectrum[i];
                    peakIndex = i;
                }
            }
            features.dominantFrequency = frequencies[peakIndex];
            
            // 频带能量分布
            features.bandEnergies = computeBandEnergies(spectrum, frequencies);
            
            // 频谱重心
            double numerator = 0.0, denominator = 0.0;
            for (int i = 0; i < spectrum.length; i++) {
                numerator += frequencies[i] * spectrum[i];
                denominator += spectrum[i];
            }
            features.spectralCentroid = denominator > 0 ? numerator / denominator : 0.0;
            
            // 频谱带宽
            double variance = 0.0;
            for (int i = 0; i < spectrum.length; i++) {
                double diff = frequencies[i] - features.spectralCentroid;
                variance += diff * diff * spectrum[i];
            }
            features.spectralBandwidth = denominator > 0 ? Math.sqrt(variance / denominator) : 0.0;
            
            // 频谱熵
            features.spectralEntropy = computeSpectralEntropy(spectrum);
            
        } catch (Exception e) {
            System.err.println("WARNING: Frequency analysis failed: " + e.getMessage());
            // 设置默认值
            features.dominantFrequency = 0.0;
            features.bandEnergies = new double[5]; // 5个频带
            features.spectralCentroid = 0.0;
            features.spectralBandwidth = 0.0;
            features.spectralEntropy = 0.0;
        }
        
        return features;
    }
    
    /**
     * 计算时域特征
     */
    private TemporalFeatures computeTemporalFeatures(double[] signal) {
        TemporalFeatures features = new TemporalFeatures();
        
        // 变化率
        features.changeRate = new double[signal.length - 1];
        for (int i = 0; i < signal.length - 1; i++) {
            features.changeRate[i] = Math.abs(signal[i + 1] - signal[i]);
        }
        
        // 平均变化率
        features.averageChangeRate = Arrays.stream(features.changeRate).average().orElse(0.0);
        
        // 突变检测（基于变化率的标准差）
        double changeRateStd = computeStandardDeviation(features.changeRate);
        double changeRateMean = features.averageChangeRate;
        double threshold = changeRateMean + 2 * changeRateStd;
        
        features.abruptChanges = new ArrayList<>();
        for (int i = 0; i < features.changeRate.length; i++) {
            if (features.changeRate[i] > threshold) {
                features.abruptChanges.add(i);
            }
        }
        
        // 信号平滑度（基于二阶差分）
        double smoothness = 0.0;
        for (int i = 1; i < signal.length - 1; i++) {
            double secondDiff = signal[i + 1] - 2 * signal[i] + signal[i - 1];
            smoothness += secondDiff * secondDiff;
        }
        features.smoothness = smoothness / (signal.length - 2);
        
        // 零交叉率
        int zeroCrossings = 0;
        for (int i = 0; i < signal.length - 1; i++) {
            if ((signal[i] >= 0 && signal[i + 1] < 0) || (signal[i] < 0 && signal[i + 1] >= 0)) {
                zeroCrossings++;
            }
        }
        features.zeroCrossingRate = (double) zeroCrossings / (signal.length - 1);
        
        return features;
    }
    
    /**
     * 计算统计特征
     */
    private StatisticalFeatures computeStatisticalFeatures(double[] signal) {
        StatisticalFeatures features = new StatisticalFeatures();
        
        // 基本统计量
        features.mean = Arrays.stream(signal).average().orElse(0.0);
        features.variance = computeVariance(signal, features.mean);
        features.standardDeviation = Math.sqrt(features.variance);
        
        // 偏度和峰度
        features.skewness = computeSkewness(signal, features.mean, features.standardDeviation);
        features.kurtosis = computeKurtosis(signal, features.mean, features.standardDeviation);
        
        // 动态范围
        double min = Arrays.stream(signal).min().orElse(0.0);
        double max = Arrays.stream(signal).max().orElse(0.0);
        features.dynamicRange = max - min;
        
        // 信噪比估计（基于信号方差和噪声方差的比值）
        features.snrEstimate = estimateSignalToNoiseRatio(signal);
        
        return features;
    }
    
    /**
     * 计算相关性特征
     */
    private CorrelationFeatures computeCorrelationFeatures(double[] signal, String station) {
        CorrelationFeatures features = new CorrelationFeatures();
        
        List<HistoricalFeature> history = historicalFeatures.get(station);
        if (history == null || history.isEmpty()) {
            features.historicalSimilarity = 0.5; // 默认中等相似性
            features.anomalyScore = 0.5;
            return features;
        }
        
        // 计算与历史数据的相似性
        double maxSimilarity = 0.0;
        double totalSimilarity = 0.0;
        
        for (HistoricalFeature historical : history) {
            double similarity = computeSimilarity(signal, historical);
            maxSimilarity = Math.max(maxSimilarity, similarity);
            totalSimilarity += similarity;
        }
        
        features.historicalSimilarity = totalSimilarity / history.size();
        features.maxHistoricalSimilarity = maxSimilarity;
        
        // 异常评分（基于与历史平均的偏差）
        features.anomalyScore = 1.0 - features.historicalSimilarity;
        
        return features;
    }
    
    /**
     * 计算局部重要性
     */
    private double[] computeLocalImportance(double[] signal, EnergyFeatures energyFeatures, 
                                          FrequencyFeatures frequencyFeatures, TemporalFeatures temporalFeatures) {
        double[] importance = new double[signal.length];
        
        // 基于能量分布的重要性
        double[] energyImportance = normalizeArray(energyFeatures.energyDistribution);
        
        // 基于变化率的重要性
        double[] changeImportance = new double[signal.length];
        for (int i = 0; i < temporalFeatures.changeRate.length; i++) {
            changeImportance[i] = temporalFeatures.changeRate[i];
        }
        if (changeImportance.length > 0) {
            changeImportance[changeImportance.length - 1] = changeImportance[changeImportance.length - 2];
        }
        changeImportance = normalizeArray(changeImportance);
        
        // 基于突变点的重要性
        double[] abruptImportance = new double[signal.length];
        for (int index : temporalFeatures.abruptChanges) {
            if (index < abruptImportance.length) {
                abruptImportance[index] = 1.0;
                // 在突变点周围增加重要性
                int radius = 5;
                for (int i = Math.max(0, index - radius); i <= Math.min(abruptImportance.length - 1, index + radius); i++) {
                    abruptImportance[i] = Math.max(abruptImportance[i], 1.0 - Math.abs(i - index) / (double) radius);
                }
            }
        }
        
        // 综合重要性
        for (int i = 0; i < importance.length; i++) {
            importance[i] = weights.energyWeight * energyImportance[i] +
                           weights.changeWeight * changeImportance[i] +
                           weights.abruptWeight * abruptImportance[i];
        }
        
        return normalizeArray(importance);
    }
    
    /**
     * 计算全局重要性
     */
    private double computeOverallImportance(EnergyFeatures energyFeatures, FrequencyFeatures frequencyFeatures,
                                          TemporalFeatures temporalFeatures, StatisticalFeatures statisticalFeatures,
                                          CorrelationFeatures correlationFeatures) {
        
        // 能量重要性
        double energyImportance = Math.min(1.0, energyFeatures.averagePower / config.getReferenceEnergy());
        
        // 频率重要性（基于主频是否在关注频带内）
        double frequencyImportance = 0.5;
        if (frequencyFeatures.dominantFrequency >= config.getMinInterestFreq() && 
            frequencyFeatures.dominantFrequency <= config.getMaxInterestFreq()) {
            frequencyImportance = 1.0;
        }
        
        // 时域重要性
        double temporalImportance = Math.min(1.0, temporalFeatures.averageChangeRate / config.getReferenceChangeRate());
        
        // 统计重要性
        double statisticalImportance = Math.min(1.0, statisticalFeatures.snrEstimate / config.getReferenceSnr());
        
        // 相关性重要性
        double correlationImportance = correlationFeatures.anomalyScore;
        
        // 加权综合
        double overallImportance = 
            weights.energyWeight * energyImportance +
            weights.frequencyWeight * frequencyImportance +
            weights.temporalWeight * temporalImportance +
            weights.statisticalWeight * statisticalImportance +
            weights.correlationWeight * correlationImportance;
        
        return Math.max(0.0, Math.min(1.0, overallImportance));
    }
    
    // 辅助方法
    
    private double[] normalizeArray(double[] array) {
        if (array.length == 0) return array;
        
        double min = Arrays.stream(array).min().orElse(0.0);
        double max = Arrays.stream(array).max().orElse(1.0);
        
        if (max - min < 1e-10) {
            Arrays.fill(array, 0.5);
            return array;
        }
        
        double[] normalized = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            normalized[i] = (array[i] - min) / (max - min);
        }
        
        return normalized;
    }
    
    private double computeVariation(double[] array) {
        if (array.length < 2) return 0.0;
        
        double variation = 0.0;
        for (int i = 1; i < array.length; i++) {
            variation += Math.abs(array[i] - array[i-1]);
        }
        
        return variation / (array.length - 1);
    }
    
    private double computeStandardDeviation(double[] array) {
        double mean = Arrays.stream(array).average().orElse(0.0);
        return Math.sqrt(computeVariance(array, mean));
    }
    
    private double computeVariance(double[] array, double mean) {
        return Arrays.stream(array).map(x -> (x - mean) * (x - mean)).average().orElse(0.0);
    }
    
    private double computeSkewness(double[] array, double mean, double std) {
        if (std < 1e-10) return 0.0;
        
        double skewness = 0.0;
        for (double value : array) {
            double normalized = (value - mean) / std;
            skewness += normalized * normalized * normalized;
        }
        
        return skewness / array.length;
    }
    
    private double computeKurtosis(double[] array, double mean, double std) {
        if (std < 1e-10) return 0.0;
        
        double kurtosis = 0.0;
        for (double value : array) {
            double normalized = (value - mean) / std;
            kurtosis += normalized * normalized * normalized * normalized;
        }
        
        return kurtosis / array.length - 3.0; // 减去3得到超额峰度
    }
    
    private double[] computeBandEnergies(double[] spectrum, double[] frequencies) {
        // 定义5个频带：0-1Hz, 1-5Hz, 5-15Hz, 15-50Hz, 50Hz+
        double[] bandLimits = {0, 1, 5, 15, 50, Double.MAX_VALUE};
        double[] bandEnergies = new double[5];
        
        for (int i = 0; i < spectrum.length; i++) {
            double freq = frequencies[i];
            for (int band = 0; band < 5; band++) {
                if (freq >= bandLimits[band] && freq < bandLimits[band + 1]) {
                    bandEnergies[band] += spectrum[i];
                    break;
                }
            }
        }
        
        return bandEnergies;
    }
    
    private double computeSpectralEntropy(double[] spectrum) {
        double totalEnergy = Arrays.stream(spectrum).sum();
        if (totalEnergy < 1e-10) return 0.0;
        
        double entropy = 0.0;
        for (double power : spectrum) {
            if (power > 0) {
                double probability = power / totalEnergy;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        return entropy;
    }
    
    private double estimateSignalToNoiseRatio(double[] signal) {
        // 简单的SNR估计：假设信号的高能量部分是信号，低能量部分是噪声
        double[] sortedPowers = Arrays.stream(signal).map(x -> x * x).sorted().toArray();
        
        int signalSamples = (int) (sortedPowers.length * 0.1); // 前10%作为信号
        int noiseSamples = (int) (sortedPowers.length * 0.1);  // 后10%作为噪声
        
        double signalPower = Arrays.stream(sortedPowers, sortedPowers.length - signalSamples, sortedPowers.length)
                                  .average().orElse(0.0);
        double noisePower = Arrays.stream(sortedPowers, 0, noiseSamples)
                                 .average().orElse(1e-10);
        
        return signalPower / Math.max(noisePower, 1e-10);
    }
    
    private double computeSimilarity(double[] signal, HistoricalFeature historical) {
        // 基于特征向量的相似性计算
        double[] currentFeatures = extractFeatureVector(signal);
        double[] historicalFeatures = historical.getFeatureVector();
        
        if (currentFeatures.length != historicalFeatures.length) {
            return 0.5; // 默认中等相似性
        }
        
        // 计算余弦相似性
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < currentFeatures.length; i++) {
            dotProduct += currentFeatures[i] * historicalFeatures[i];
            normA += currentFeatures[i] * currentFeatures[i];
            normB += historicalFeatures[i] * historicalFeatures[i];
        }
        
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator > 0 ? dotProduct / denominator : 0.0;
    }
    
    private double[] extractFeatureVector(double[] signal) {
        // 提取简化的特征向量
        double mean = Arrays.stream(signal).average().orElse(0.0);
        double variance = computeVariance(signal, mean);
        double energy = Arrays.stream(signal).map(x -> x * x).sum();
        double maxAbs = Arrays.stream(signal).map(Math::abs).max().orElse(0.0);
        
        return new double[]{mean, variance, energy, maxAbs};
    }
    
    private void updateHistoricalFeatures(String station, EnergyFeatures energyFeatures, 
                                         FrequencyFeatures frequencyFeatures, TemporalFeatures temporalFeatures) {
        List<HistoricalFeature> history = historicalFeatures.computeIfAbsent(station, k -> new ArrayList<>());
        
        HistoricalFeature newFeature = new HistoricalFeature(
            energyFeatures.averagePower,
            frequencyFeatures.dominantFrequency,
            temporalFeatures.averageChangeRate,
            System.currentTimeMillis()
        );
        
        history.add(newFeature);
        
        // 保持历史记录大小
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }
    
    // 内部特征类
    
    private static class EnergyFeatures {
        double totalEnergy;
        double averagePower;
        double peakPower;
        double[] energyDistribution;
        double energyVariation;
    }
    
    private static class FrequencyFeatures {
        double dominantFrequency;
        double[] bandEnergies;
        double spectralCentroid;
        double spectralBandwidth;
        double spectralEntropy;
    }
    
    private static class TemporalFeatures {
        double[] changeRate;
        double averageChangeRate;
        List<Integer> abruptChanges;
        double smoothness;
        double zeroCrossingRate;
    }
    
    private static class StatisticalFeatures {
        double mean;
        double variance;
        double standardDeviation;
        double skewness;
        double kurtosis;
        double dynamicRange;
        double snrEstimate;
    }
    
    private static class CorrelationFeatures {
        double historicalSimilarity;
        double maxHistoricalSimilarity;
        double anomalyScore;
    }
    
    private static class FeatureWeights {
        final double energyWeight;
        final double frequencyWeight;
        final double temporalWeight;
        final double statisticalWeight;
        final double correlationWeight;
        final double changeWeight;
        final double abruptWeight;
        
        FeatureWeights(SamplingConfig config) {
            this.energyWeight = config.getEnergyWeight();
            this.frequencyWeight = config.getFrequencyWeight();
            this.temporalWeight = config.getTemporalWeight();
            this.statisticalWeight = config.getStatisticalWeight();
            this.correlationWeight = config.getCorrelationWeight();
            this.changeWeight = config.getChangeWeight();
            this.abruptWeight = config.getAbruptWeight();
        }
    }
    
    private static class HistoricalFeature {
        private final double averagePower;
        private final double dominantFrequency;
        private final double averageChangeRate;
        private final long timestamp;
        
        HistoricalFeature(double averagePower, double dominantFrequency, double averageChangeRate, long timestamp) {
            this.averagePower = averagePower;
            this.dominantFrequency = dominantFrequency;
            this.averageChangeRate = averageChangeRate;
            this.timestamp = timestamp;
        }
        
        double[] getFeatureVector() {
            return new double[]{averagePower, dominantFrequency, averageChangeRate};
        }
    }
}
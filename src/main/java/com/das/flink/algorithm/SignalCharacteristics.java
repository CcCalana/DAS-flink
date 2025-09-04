package com.das.flink.algorithm;

/**
 * 信号特征综合类
 * 包含时域、频域、统计和噪声特征的完整描述
 * 
 * @author DAS-Flink Team
 */
public class SignalCharacteristics {
    
    private final TimeDomainFeatures timeDomainFeatures;
    private final FrequencyDomainFeatures frequencyDomainFeatures;
    private final StatisticalFeatures statisticalFeatures;
    private final NoiseCharacteristics noiseCharacteristics;
    private final float samplingRate;
    private final long timestamp;
    
    public SignalCharacteristics(TimeDomainFeatures timeDomainFeatures,
                               FrequencyDomainFeatures frequencyDomainFeatures,
                               StatisticalFeatures statisticalFeatures,
                               NoiseCharacteristics noiseCharacteristics,
                               float samplingRate,
                               long timestamp) {
        this.timeDomainFeatures = timeDomainFeatures;
        this.frequencyDomainFeatures = frequencyDomainFeatures;
        this.statisticalFeatures = statisticalFeatures;
        this.noiseCharacteristics = noiseCharacteristics;
        this.samplingRate = samplingRate;
        this.timestamp = timestamp;
    }
    
    // 便捷访问方法
    public float getDominantFrequency() {
        return frequencyDomainFeatures.getDominantFrequency();
    }
    
    public float getNoiseLevel() {
        return noiseCharacteristics.getNoiseLevel();
    }
    
    public float getSignalToNoiseRatio() {
        return noiseCharacteristics.getSnr();
    }
    
    public float getVariability() {
        return timeDomainFeatures.getVariability();
    }
    
    public float getStationarity() {
        return noiseCharacteristics.getStationarity();
    }
    
    public float getSpectralCentroid() {
        return frequencyDomainFeatures.getSpectralCentroid();
    }
    
    public float getBandwidth() {
        return frequencyDomainFeatures.getBandwidth();
    }
    
    // Getters
    public TimeDomainFeatures getTimeDomainFeatures() { return timeDomainFeatures; }
    public FrequencyDomainFeatures getFrequencyDomainFeatures() { return frequencyDomainFeatures; }
    public StatisticalFeatures getStatisticalFeatures() { return statisticalFeatures; }
    public NoiseCharacteristics getNoiseCharacteristics() { return noiseCharacteristics; }
    public float getSamplingRate() { return samplingRate; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format(
            "SignalCharacteristics{dominantFreq=%.2fHz, SNR=%.2fdB, noise=%.4f, variability=%.3f}",
            getDominantFrequency(), getSignalToNoiseRatio(), getNoiseLevel(), getVariability()
        );
    }
}

/**
 * 时域特征类
 */
class TimeDomainFeatures {
    private final float mean;
    private final float variance;
    private final float rms;
    private final float maxValue;
    private final float minValue;
    private final int maxIndex;
    private final int minIndex;
    private final float peakToPeak;
    private final float crestFactor;
    private final float zeroCrossingRate;
    private final float[] energyDistribution;
    private final float variability;
    
    public TimeDomainFeatures(float mean, float variance, float rms,
                            float maxValue, float minValue, int maxIndex, int minIndex,
                            float peakToPeak, float crestFactor, float zeroCrossingRate,
                            float[] energyDistribution, float variability) {
        this.mean = mean;
        this.variance = variance;
        this.rms = rms;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.maxIndex = maxIndex;
        this.minIndex = minIndex;
        this.peakToPeak = peakToPeak;
        this.crestFactor = crestFactor;
        this.zeroCrossingRate = zeroCrossingRate;
        this.energyDistribution = energyDistribution;
        this.variability = variability;
    }
    
    // Getters
    public float getMean() { return mean; }
    public float getVariance() { return variance; }
    public float getRms() { return rms; }
    public float getMaxValue() { return maxValue; }
    public float getMinValue() { return minValue; }
    public int getMaxIndex() { return maxIndex; }
    public int getMinIndex() { return minIndex; }
    public float getPeakToPeak() { return peakToPeak; }
    public float getCrestFactor() { return crestFactor; }
    public float getZeroCrossingRate() { return zeroCrossingRate; }
    public float[] getEnergyDistribution() { return energyDistribution; }
    public float getVariability() { return variability; }
}

/**
 * 频域特征类
 */
class FrequencyDomainFeatures {
    private final float dominantFrequency;
    private final float spectralCentroid;
    private final float spectralRolloff;
    private final float spectralFlatness;
    private final float bandwidth;
    private final float spectralEntropy;
    private final float[] powerSpectrum;
    private final float[] frequencies;
    
    public FrequencyDomainFeatures(float dominantFrequency, float spectralCentroid,
                                 float spectralRolloff, float spectralFlatness,
                                 float bandwidth, float spectralEntropy,
                                 float[] powerSpectrum, float[] frequencies) {
        this.dominantFrequency = dominantFrequency;
        this.spectralCentroid = spectralCentroid;
        this.spectralRolloff = spectralRolloff;
        this.spectralFlatness = spectralFlatness;
        this.bandwidth = bandwidth;
        this.spectralEntropy = spectralEntropy;
        this.powerSpectrum = powerSpectrum;
        this.frequencies = frequencies;
    }
    
    // Getters
    public float getDominantFrequency() { return dominantFrequency; }
    public float getSpectralCentroid() { return spectralCentroid; }
    public float getSpectralRolloff() { return spectralRolloff; }
    public float getSpectralFlatness() { return spectralFlatness; }
    public float getBandwidth() { return bandwidth; }
    public float getSpectralEntropy() { return spectralEntropy; }
    public float[] getPowerSpectrum() { return powerSpectrum; }
    public float[] getFrequencies() { return frequencies; }
}

/**
 * 统计特征类
 */
class StatisticalFeatures {
    private final float mean;
    private final float variance;
    private final float standardDeviation;
    private final float skewness;
    private final float kurtosis;
    private final float q25;
    private final float median;
    private final float q75;
    private final float iqr;
    private final int outlierCount;
    private final float outlierRatio;
    
    public StatisticalFeatures(float mean, float variance, float standardDeviation,
                             float skewness, float kurtosis, float q25, float median,
                             float q75, float iqr, int outlierCount, float outlierRatio) {
        this.mean = mean;
        this.variance = variance;
        this.standardDeviation = standardDeviation;
        this.skewness = skewness;
        this.kurtosis = kurtosis;
        this.q25 = q25;
        this.median = median;
        this.q75 = q75;
        this.iqr = iqr;
        this.outlierCount = outlierCount;
        this.outlierRatio = outlierRatio;
    }
    
    // Getters
    public float getMean() { return mean; }
    public float getVariance() { return variance; }
    public float getStandardDeviation() { return standardDeviation; }
    public float getSkewness() { return skewness; }
    public float getKurtosis() { return kurtosis; }
    public float getQ25() { return q25; }
    public float getMedian() { return median; }
    public float getQ75() { return q75; }
    public float getIqr() { return iqr; }
    public int getOutlierCount() { return outlierCount; }
    public float getOutlierRatio() { return outlierRatio; }
}

/**
 * 噪声特征类
 */
class NoiseCharacteristics {
    private final float noiseLevel;
    private final float snr;
    private final float stationarity;
    private final boolean isWhiteNoise;
    private final SignalAnalyzer.NoiseType noiseType;
    
    public NoiseCharacteristics(float noiseLevel, float snr, float stationarity,
                              boolean isWhiteNoise, SignalAnalyzer.NoiseType noiseType) {
        this.noiseLevel = noiseLevel;
        this.snr = snr;
        this.stationarity = stationarity;
        this.isWhiteNoise = isWhiteNoise;
        this.noiseType = noiseType;
    }
    
    // Getters
    public float getNoiseLevel() { return noiseLevel; }
    public float getSnr() { return snr; }
    public float getStationarity() { return stationarity; }
    public boolean isWhiteNoise() { return isWhiteNoise; }
    public SignalAnalyzer.NoiseType getNoiseType() { return noiseType; }
}
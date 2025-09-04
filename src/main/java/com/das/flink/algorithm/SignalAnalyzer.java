package com.das.flink.algorithm;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 信号分析器
 * 分析地震信号的时频特征，为自适应算法提供参数依据
 * 
 * @author DAS-Flink Team
 */
public class SignalAnalyzer {
    
    private static final int MIN_FFT_SIZE = 64;
    private static final int MAX_FFT_SIZE = 4096;
    private static final float NOISE_FLOOR_PERCENTILE = 0.1f;
    
    private final MemoryPool memoryPool;
    private final FFTProcessor fftProcessor;
    
    public SignalAnalyzer() {
        this.memoryPool = new MemoryPool();
        this.fftProcessor = new FFTProcessor();
    }
    
    /**
     * 分析信号特征
     * 
     * @param data 信号数据
     * @param samplingRate 采样率
     * @return 信号特征
     */
    public SignalCharacteristics analyze(float[] data, float samplingRate) {
        if (data == null || data.length < MIN_FFT_SIZE) {
            return createDefaultCharacteristics();
        }
        
        // 时域特征分析
        TimeDomainFeatures timeFeatures = analyzeTimeDomain(data);
        
        // 频域特征分析
        FrequencyDomainFeatures freqFeatures = analyzeFrequencyDomain(data, samplingRate);
        
        // 统计特征分析
        StatisticalFeatures statFeatures = analyzeStatisticalFeatures(data);
        
        // 噪声特征分析
        NoiseCharacteristics noiseFeatures = analyzeNoiseCharacteristics(data, samplingRate);
        
        return new SignalCharacteristics(
            timeFeatures,
            freqFeatures,
            statFeatures,
            noiseFeatures,
            samplingRate,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 时域特征分析
     */
    private TimeDomainFeatures analyzeTimeDomain(float[] data) {
        int length = data.length;
        
        // 基本统计量
        float mean = computeMean(data);
        float variance = computeVariance(data, mean);
        float rms = computeRMS(data);
        
        // 峰值特征
        float maxValue = Float.NEGATIVE_INFINITY;
        float minValue = Float.POSITIVE_INFINITY;
        int maxIndex = -1;
        int minIndex = -1;
        
        for (int i = 0; i < length; i++) {
            if (data[i] > maxValue) {
                maxValue = data[i];
                maxIndex = i;
            }
            if (data[i] < minValue) {
                minValue = data[i];
                minIndex = i;
            }
        }
        
        float peakToPeak = maxValue - minValue;
        float crestFactor = maxValue / rms;
        
        // 过零率
        int zeroCrossings = computeZeroCrossings(data);
        float zeroCrossingRate = (float) zeroCrossings / length;
        
        // 能量分布
        float[] energyDistribution = computeEnergyDistribution(data, 10);
        
        // 变异性分析
        float variability = computeVariability(data);
        
        return new TimeDomainFeatures(
            mean, variance, rms, maxValue, minValue, maxIndex, minIndex,
            peakToPeak, crestFactor, zeroCrossingRate, energyDistribution, variability
        );
    }
    
    /**
     * 频域特征分析
     */
    private FrequencyDomainFeatures analyzeFrequencyDomain(float[] data, float samplingRate) {
        // 选择合适的FFT大小
        int fftSize = selectOptimalFFTSize(data.length);
        
        // 应用窗函数
        float[] windowedData = applyHammingWindow(data, fftSize);
        
        // 计算FFT
        double[][] fftData = fftProcessor.computeFFT(windowedData);
        Complex[] fftResult = convertToComplex(fftData[0], fftData[1]);
        
        // 计算功率谱密度
        float[] powerSpectrum = computePowerSpectrum(fftResult);
        
        // 频率轴
        float[] frequencies = computeFrequencyAxis(powerSpectrum.length, samplingRate);
        
        // 主频分析
        int dominantFreqIndex = findDominantFrequency(powerSpectrum);
        float dominantFrequency = frequencies[dominantFreqIndex];
        
        // 频谱质心
        float spectralCentroid = computeSpectralCentroid(powerSpectrum, frequencies);
        
        // 频谱滚降
        float spectralRolloff = computeSpectralRolloff(powerSpectrum, frequencies, 0.85f);
        
        // 频谱平坦度
        float spectralFlatness = computeSpectralFlatness(powerSpectrum);
        
        // 带宽分析
        float bandwidth = computeBandwidth(powerSpectrum, frequencies, spectralCentroid);
        
        // 频谱熵
        float spectralEntropy = computeSpectralEntropy(powerSpectrum);
        
        return new FrequencyDomainFeatures(
            dominantFrequency, spectralCentroid, spectralRolloff,
            spectralFlatness, bandwidth, spectralEntropy, powerSpectrum, frequencies
        );
    }
    
    /**
     * 统计特征分析
     */
    private StatisticalFeatures analyzeStatisticalFeatures(float[] data) {
        float mean = computeMean(data);
        float variance = computeVariance(data, mean);
        float standardDeviation = (float) Math.sqrt(variance);
        
        // 偏度和峰度
        float skewness = computeSkewness(data, mean, standardDeviation);
        float kurtosis = computeKurtosis(data, mean, standardDeviation);
        
        // 分位数
        float[] sortedData = Arrays.copyOf(data, data.length);
        Arrays.sort(sortedData);
        
        float q25 = computePercentile(sortedData, 0.25f);
        float median = computePercentile(sortedData, 0.5f);
        float q75 = computePercentile(sortedData, 0.75f);
        float iqr = q75 - q25;
        
        // 异常值检测
        int outlierCount = countOutliers(data, q25, q75, iqr);
        float outlierRatio = (float) outlierCount / data.length;
        
        return new StatisticalFeatures(
            mean, variance, standardDeviation, skewness, kurtosis,
            q25, median, q75, iqr, outlierCount, outlierRatio
        );
    }
    
    /**
     * 噪声特征分析
     */
    private NoiseCharacteristics analyzeNoiseCharacteristics(float[] data, float samplingRate) {
        // 估计噪声水平
        float noiseLevel = estimateNoiseLevel(data);
        
        // 信噪比估计
        float signalPower = computeSignalPower(data);
        float noisePower = noiseLevel * noiseLevel;
        float snr = 10.0f * (float) Math.log10(signalPower / noisePower);
        
        // 平稳性分析
        float stationarity = analyzeStationarity(data);
        
        // 白噪声测试
        boolean isWhiteNoise = testWhiteNoise(data, samplingRate);
        
        // 噪声类型分类
        NoiseType noiseType = classifyNoiseType(data, samplingRate);
        
        return new NoiseCharacteristics(
            noiseLevel, snr, stationarity, isWhiteNoise, noiseType
        );
    }
    
    // 辅助计算方法
    
    private float computeMean(float[] data) {
        double sum = 0.0;
        for (float value : data) {
            sum += value;
        }
        return (float) (sum / data.length);
    }
    
    private float computeVariance(float[] data, float mean) {
        double sumSquaredDiff = 0.0;
        for (float value : data) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return (float) (sumSquaredDiff / data.length);
    }
    
    private float computeRMS(float[] data) {
        double sumSquares = 0.0;
        for (float value : data) {
            sumSquares += value * value;
        }
        return (float) Math.sqrt(sumSquares / data.length);
    }
    
    private int computeZeroCrossings(float[] data) {
        int crossings = 0;
        for (int i = 1; i < data.length; i++) {
            if ((data[i] >= 0 && data[i-1] < 0) || (data[i] < 0 && data[i-1] >= 0)) {
                crossings++;
            }
        }
        return crossings;
    }
    
    private float[] computeEnergyDistribution(float[] data, int numBins) {
        float[] energyBins = new float[numBins];
        int binSize = data.length / numBins;
        
        for (int bin = 0; bin < numBins; bin++) {
            int start = bin * binSize;
            int end = Math.min(start + binSize, data.length);
            
            double energy = 0.0;
            for (int i = start; i < end; i++) {
                energy += data[i] * data[i];
            }
            energyBins[bin] = (float) energy;
        }
        
        return energyBins;
    }
    
    private float computeVariability(float[] data) {
        if (data.length < 10) return 0.0f;
        
        // 计算局部方差的变异性
        int windowSize = Math.max(10, data.length / 20);
        float[] localVariances = new float[data.length - windowSize + 1];
        
        for (int i = 0; i <= data.length - windowSize; i++) {
            float localMean = 0.0f;
            for (int j = i; j < i + windowSize; j++) {
                localMean += data[j];
            }
            localMean /= windowSize;
            
            float localVar = 0.0f;
            for (int j = i; j < i + windowSize; j++) {
                float diff = data[j] - localMean;
                localVar += diff * diff;
            }
            localVariances[i] = localVar / windowSize;
        }
        
        // 计算局部方差的变异系数
        float meanVar = computeMean(localVariances);
        float stdVar = (float) Math.sqrt(computeVariance(localVariances, meanVar));
        
        return meanVar > 0 ? stdVar / meanVar : 0.0f;
    }
    
    private int selectOptimalFFTSize(int dataLength) {
        int fftSize = MIN_FFT_SIZE;
        while (fftSize < dataLength && fftSize < MAX_FFT_SIZE) {
            fftSize *= 2;
        }
        return Math.min(fftSize, MAX_FFT_SIZE);
    }
    
    private float[] applyHammingWindow(float[] data, int fftSize) {
        int length = Math.min(data.length, fftSize);
        float[] windowed = new float[fftSize];
        
        for (int i = 0; i < length; i++) {
            float window = 0.54f - 0.46f * (float) Math.cos(2.0 * Math.PI * i / (length - 1));
            windowed[i] = data[i] * window;
        }
        
        // 零填充
        Arrays.fill(windowed, length, fftSize, 0.0f);
        
        return windowed;
    }
    
    private float[] computePowerSpectrum(Complex[] fftResult) {
        int length = fftResult.length / 2; // 只取正频率部分
        float[] powerSpectrum = new float[length];
        
        for (int i = 0; i < length; i++) {
            Complex c = fftResult[i];
            powerSpectrum[i] = c.real * c.real + c.imag * c.imag;
        }
        
        return powerSpectrum;
    }
    
    private float[] computeFrequencyAxis(int spectrumLength, float samplingRate) {
        float[] frequencies = new float[spectrumLength];
        float freqStep = samplingRate / (2.0f * spectrumLength);
        
        for (int i = 0; i < spectrumLength; i++) {
            frequencies[i] = i * freqStep;
        }
        
        return frequencies;
    }
    
    private int findDominantFrequency(float[] powerSpectrum) {
        int maxIndex = 0;
        float maxPower = powerSpectrum[0];
        
        for (int i = 1; i < powerSpectrum.length; i++) {
            if (powerSpectrum[i] > maxPower) {
                maxPower = powerSpectrum[i];
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    private float computeSpectralCentroid(float[] powerSpectrum, float[] frequencies) {
        double weightedSum = 0.0;
        double totalPower = 0.0;
        
        for (int i = 0; i < powerSpectrum.length; i++) {
            weightedSum += frequencies[i] * powerSpectrum[i];
            totalPower += powerSpectrum[i];
        }
        
        return totalPower > 0 ? (float) (weightedSum / totalPower) : 0.0f;
    }
    
    private float computeSpectralRolloff(float[] powerSpectrum, float[] frequencies, float threshold) {
        double totalPower = 0.0;
        for (float power : powerSpectrum) {
            totalPower += power;
        }
        
        double cumulativePower = 0.0;
        double targetPower = totalPower * threshold;
        
        for (int i = 0; i < powerSpectrum.length; i++) {
            cumulativePower += powerSpectrum[i];
            if (cumulativePower >= targetPower) {
                return frequencies[i];
            }
        }
        
        return frequencies[frequencies.length - 1];
    }
    
    private float computeSpectralFlatness(float[] powerSpectrum) {
        double geometricMean = 1.0;
        double arithmeticMean = 0.0;
        int count = 0;
        
        for (float power : powerSpectrum) {
            if (power > 0) {
                geometricMean *= Math.pow(power, 1.0 / powerSpectrum.length);
                arithmeticMean += power;
                count++;
            }
        }
        
        if (count == 0) return 0.0f;
        
        arithmeticMean /= count;
        return arithmeticMean > 0 ? (float) (geometricMean / arithmeticMean) : 0.0f;
    }
    
    private float computeBandwidth(float[] powerSpectrum, float[] frequencies, float centroid) {
        double weightedVariance = 0.0;
        double totalPower = 0.0;
        
        for (int i = 0; i < powerSpectrum.length; i++) {
            double freqDiff = frequencies[i] - centroid;
            weightedVariance += freqDiff * freqDiff * powerSpectrum[i];
            totalPower += powerSpectrum[i];
        }
        
        return totalPower > 0 ? (float) Math.sqrt(weightedVariance / totalPower) : 0.0f;
    }
    
    private float computeSpectralEntropy(float[] powerSpectrum) {
        // 归一化功率谱
        double totalPower = 0.0;
        for (float power : powerSpectrum) {
            totalPower += power;
        }
        
        if (totalPower == 0) return 0.0f;
        
        double entropy = 0.0;
        for (float power : powerSpectrum) {
            if (power > 0) {
                double probability = power / totalPower;
                entropy -= probability * Math.log(probability) / Math.log(2.0);
            }
        }
        
        return (float) entropy;
    }
    
    private float computeSkewness(float[] data, float mean, float std) {
        if (std == 0) return 0.0f;
        
        double sum = 0.0;
        for (float value : data) {
            double normalized = (value - mean) / std;
            sum += normalized * normalized * normalized;
        }
        
        return (float) (sum / data.length);
    }
    
    private float computeKurtosis(float[] data, float mean, float std) {
        if (std == 0) return 0.0f;
        
        double sum = 0.0;
        for (float value : data) {
            double normalized = (value - mean) / std;
            sum += normalized * normalized * normalized * normalized;
        }
        
        return (float) (sum / data.length - 3.0); // 减去3得到超峰度
    }
    
    private float computePercentile(float[] sortedData, float percentile) {
        int index = (int) (percentile * (sortedData.length - 1));
        return sortedData[index];
    }
    
    private int countOutliers(float[] data, float q25, float q75, float iqr) {
        float lowerBound = q25 - 1.5f * iqr;
        float upperBound = q75 + 1.5f * iqr;
        
        int count = 0;
        for (float value : data) {
            if (value < lowerBound || value > upperBound) {
                count++;
            }
        }
        
        return count;
    }
    
    private float estimateNoiseLevel(float[] data) {
        // 使用中位数绝对偏差(MAD)估计噪声水平
        float[] sortedData = Arrays.copyOf(data, data.length);
        Arrays.sort(sortedData);
        
        float median = computePercentile(sortedData, 0.5f);
        
        float[] deviations = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            deviations[i] = Math.abs(data[i] - median);
        }
        
        Arrays.sort(deviations);
        float mad = computePercentile(deviations, 0.5f);
        
        return mad * 1.4826f; // 转换为标准差估计
    }
    
    private float computeSignalPower(float[] data) {
        double power = 0.0;
        for (float value : data) {
            power += value * value;
        }
        return (float) (power / data.length);
    }
    
    private float analyzeStationarity(float[] data) {
        // 简化的平稳性分析：比较前后两半的统计特性
        int halfLength = data.length / 2;
        
        float[] firstHalf = Arrays.copyOfRange(data, 0, halfLength);
        float[] secondHalf = Arrays.copyOfRange(data, halfLength, data.length);
        
        float mean1 = computeMean(firstHalf);
        float mean2 = computeMean(secondHalf);
        float var1 = computeVariance(firstHalf, mean1);
        float var2 = computeVariance(secondHalf, mean2);
        
        // 计算均值和方差的相对差异
        float meanDiff = Math.abs(mean1 - mean2) / (Math.abs(mean1) + Math.abs(mean2) + 1e-10f);
        float varDiff = Math.abs(var1 - var2) / (var1 + var2 + 1e-10f);
        
        // 平稳性分数（0-1，1表示完全平稳）
        return 1.0f - Math.min(1.0f, (meanDiff + varDiff) / 2.0f);
    }
    
    private boolean testWhiteNoise(float[] data, float samplingRate) {
        // 简化的白噪声测试：检查自相关函数
        int maxLag = Math.min(50, data.length / 4);
        
        float[] autocorr = computeAutocorrelation(data, maxLag);
        
        // 检查除了零滞后外的自相关是否接近零
        int significantLags = 0;
        float threshold = 0.1f;
        
        for (int lag = 1; lag < autocorr.length; lag++) {
            if (Math.abs(autocorr[lag]) > threshold) {
                significantLags++;
            }
        }
        
        return significantLags < maxLag * 0.1; // 少于10%的滞后显著
    }
    
    private float[] computeAutocorrelation(float[] data, int maxLag) {
        float[] autocorr = new float[maxLag];
        float mean = computeMean(data);
        
        for (int lag = 0; lag < maxLag; lag++) {
            double sum = 0.0;
            int count = 0;
            
            for (int i = 0; i < data.length - lag; i++) {
                sum += (data[i] - mean) * (data[i + lag] - mean);
                count++;
            }
            
            autocorr[lag] = count > 0 ? (float) (sum / count) : 0.0f;
        }
        
        // 归一化
        if (autocorr[0] > 0) {
            for (int i = 0; i < autocorr.length; i++) {
                autocorr[i] /= autocorr[0];
            }
        }
        
        return autocorr;
    }
    
    private NoiseType classifyNoiseType(float[] data, float samplingRate) {
        // 简化的噪声类型分类
        double[][] fftData = fftProcessor.computeFFT(applyHammingWindow(data, selectOptimalFFTSize(data.length)));
        Complex[] fftResult = convertToComplex(fftData[0], fftData[1]);
        float[] powerSpectrum = computePowerSpectrum(fftResult);
        
        float spectralFlatness = computeSpectralFlatness(powerSpectrum);
        
        if (spectralFlatness > 0.8f) {
            return NoiseType.WHITE;
        } else if (spectralFlatness > 0.5f) {
            return NoiseType.COLORED;
        } else {
            return NoiseType.STRUCTURED;
        }
    }
    
    private SignalCharacteristics createDefaultCharacteristics() {
        return new SignalCharacteristics(
            new TimeDomainFeatures(0, 0, 0, 0, 0, -1, -1, 0, 0, 0, new float[10], 0),
            new FrequencyDomainFeatures(0, 0, 0, 0, 0, 0, new float[0], new float[0]),
            new StatisticalFeatures(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            new NoiseCharacteristics(0, 0, 0, false, NoiseType.UNKNOWN),
            0,
            System.currentTimeMillis()
        );
    }
    
    private Complex[] convertToComplex(double[] real, double[] imag) {
        Complex[] result = new Complex[real.length];
        for (int i = 0; i < real.length; i++) {
            result[i] = new Complex((float)real[i], (float)imag[i]);
        }
        return result;
    }
    
    // 内部类定义
    
    public enum NoiseType {
        WHITE, COLORED, STRUCTURED, UNKNOWN
    }
    
    public static class Complex {
        public final float real;
        public final float imag;
        
        public Complex(float real, float imag) {
            this.real = real;
            this.imag = imag;
        }
    }
}
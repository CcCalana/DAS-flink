package com.zjujzl.das.optimization.sampling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 质量保证控制器
 * 
 * 实时监控采样质量并确保满足地震学研究的精度要求
 * 参考文献：
 * - Geophysical Journal International 2023: Quality control in seismic data processing
 * - IEEE Transactions on Geoscience and Remote Sensing 2022: Adaptive quality assurance
 * - Bulletin of the Seismological Society of America 2021: Data quality metrics
 */
public class QualityAssurance {
    
    private final SamplingConfig config;
    
    // 质量监控指标
    private volatile double currentSignalQuality = 1.0;
    private volatile double currentSamplingQuality = 1.0;
    private volatile double overallQuality = 1.0;
    
    // 质量历史记录
    private final Queue<QualitySnapshot> qualityHistory;
    private final int maxHistorySize = 200;
    
    // 质量统计
    private final Map<String, QualityMetrics> channelQuality;
    private final AtomicLong totalSamples;
    private final AtomicLong qualityViolations;
    
    // 自适应质量控制
    private volatile double qualityThreshold;
    private volatile long lastQualityCheck = 0;
    private final QualityPredictor qualityPredictor;
    
    // 质量评估器
    private final SignalQualityEvaluator signalEvaluator;
    private final SamplingQualityEvaluator samplingEvaluator;
    
    public QualityAssurance(SamplingConfig config) {
        this.config = config;
        this.qualityHistory = new LinkedList<>();
        this.channelQuality = new ConcurrentHashMap<>();
        this.totalSamples = new AtomicLong(0);
        this.qualityViolations = new AtomicLong(0);
        this.qualityThreshold = config.getQualityThreshold();
        this.qualityPredictor = new QualityPredictor();
        this.signalEvaluator = new SignalQualityEvaluator();
        this.samplingEvaluator = new SamplingQualityEvaluator();
    }
    
    /**
     * 评估信号质量
     */
    public double evaluateSignalQuality(double[] signal, String channelId) {
        if (signal == null || signal.length == 0) {
            return 0.0;
        }
        
        totalSamples.addAndGet(signal.length);
        
        // 计算信号质量指标
        double snr = signalEvaluator.calculateSNR(signal);
        double dynamicRange = signalEvaluator.calculateDynamicRange(signal);
        double spectralQuality = signalEvaluator.calculateSpectralQuality(signal);
        double continuity = signalEvaluator.calculateContinuity(signal);
        
        // 综合质量评分
        double signalQuality = calculateSignalQualityScore(snr, dynamicRange, spectralQuality, continuity);
        
        // 更新通道质量统计
        updateChannelQuality(channelId, signalQuality, signal.length);
        
        // 记录质量快照
        recordQualitySnapshot(signalQuality, "signal");
        
        this.currentSignalQuality = signalQuality;
        updateOverallQuality();
        
        return signalQuality;
    }
    
    /**
     * 评估采样质量
     */
    public double evaluateSamplingQuality(SamplingStrategy strategy, double[] originalSignal, 
                                        double[] sampledSignal) {
        if (originalSignal == null || sampledSignal == null) {
            return 0.0;
        }
        
        // 计算采样质量指标
        double fidelity = samplingEvaluator.calculateFidelity(originalSignal, sampledSignal);
        double aliasing = samplingEvaluator.calculateAliasingLevel(originalSignal, strategy.getSamplingRate());
        double reconstruction = samplingEvaluator.calculateReconstructionQuality(originalSignal, sampledSignal);
        double efficiency = samplingEvaluator.calculateSamplingEfficiency(strategy);
        
        // 综合采样质量评分
        double samplingQuality = calculateSamplingQualityScore(fidelity, aliasing, reconstruction, efficiency);
        
        // 记录质量快照
        recordQualitySnapshot(samplingQuality, "sampling");
        
        this.currentSamplingQuality = samplingQuality;
        updateOverallQuality();
        
        return samplingQuality;
    }
    
    /**
     * 检查质量是否满足要求
     */
    public boolean isQualityAcceptable() {
        return overallQuality >= qualityThreshold;
    }
    
    /**
     * 检查是否需要质量干预
     */
    public boolean requiresQualityIntervention() {
        // 当前质量低于阈值
        if (overallQuality < qualityThreshold * 0.8) {
            return true;
        }
        
        // 质量下降趋势明显
        double qualityTrend = calculateQualityTrend();
        if (qualityTrend < -0.1 && overallQuality < qualityThreshold * 0.9) {
            return true;
        }
        
        // 预测质量将下降
        double predictedQuality = qualityPredictor.predictQuality(5000);
        if (predictedQuality < qualityThreshold * 0.85) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取质量改进建议
     */
    public QualityImprovementSuggestion getImprovementSuggestion() {
        List<String> suggestions = new ArrayList<>();
        double priorityScore = 0.0;
        
        // 分析信号质量问题
        if (currentSignalQuality < qualityThreshold) {
            if (getAverageSNR() < config.getMinSNR()) {
                suggestions.add("增强信号预处理以提高信噪比");
                priorityScore += 0.3;
            }
            
            if (getSpectralQualityScore() < 0.7) {
                suggestions.add("优化频域滤波参数");
                priorityScore += 0.2;
            }
        }
        
        // 分析采样质量问题
        if (currentSamplingQuality < qualityThreshold) {
            if (getAliasingLevel() > 0.1) {
                suggestions.add("提高采样率以减少混叠");
                priorityScore += 0.4;
            }
            
            if (getReconstructionError() > 0.05) {
                suggestions.add("调整采样策略以改善重建质量");
                priorityScore += 0.3;
            }
        }
        
        // 分析系统性问题
        double qualityVariance = calculateQualityVariance();
        if (qualityVariance > 0.1) {
            suggestions.add("稳定采样参数以减少质量波动");
            priorityScore += 0.2;
        }
        
        return new QualityImprovementSuggestion(suggestions, priorityScore, overallQuality);
    }
    
    /**
     * 自适应调整质量阈值
     */
    public void adaptQualityThreshold() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastQualityCheck < config.getAdaptationIntervalMs()) {
            return;
        }
        
        // 基于历史质量表现调整阈值
        double avgQuality = calculateAverageQuality();
        double qualityStability = 1.0 - calculateQualityVariance();
        
        if (avgQuality > qualityThreshold * 1.1 && qualityStability > 0.8) {
            // 质量稳定且良好，可以适当提高阈值
            qualityThreshold = Math.min(0.95, qualityThreshold * 1.02);
        } else if (avgQuality < qualityThreshold * 0.9) {
            // 质量不达标，降低阈值但保持最低要求
            qualityThreshold = Math.max(config.getMinQualityThreshold(), qualityThreshold * 0.98);
        }
        
        lastQualityCheck = currentTime;
    }
    
    /**
     * 获取质量统计报告
     */
    public QualityReport getQualityReport() {
        return new QualityReport(
            currentSignalQuality,
            currentSamplingQuality,
            overallQuality,
            qualityThreshold,
            calculateAverageQuality(),
            calculateQualityVariance(),
            calculateQualityTrend(),
            totalSamples.get(),
            qualityViolations.get(),
            getChannelQualitySummary()
        );
    }
    
    // 私有方法
    
    private double calculateSignalQualityScore(double snr, double dynamicRange, 
                                             double spectralQuality, double continuity) {
        // 加权计算信号质量
        double snrWeight = 0.3;
        double dynamicWeight = 0.2;
        double spectralWeight = 0.3;
        double continuityWeight = 0.2;
        
        // 归一化各指标到[0,1]范围
        double normalizedSNR = Math.min(1.0, Math.max(0.0, (snr - 10) / 40)); // 10-50dB范围
        double normalizedDynamic = Math.min(1.0, Math.max(0.0, dynamicRange / 60)); // 0-60dB范围
        double normalizedSpectral = Math.min(1.0, Math.max(0.0, spectralQuality));
        double normalizedContinuity = Math.min(1.0, Math.max(0.0, continuity));
        
        return snrWeight * normalizedSNR + 
               dynamicWeight * normalizedDynamic + 
               spectralWeight * normalizedSpectral + 
               continuityWeight * normalizedContinuity;
    }
    
    private double calculateSamplingQualityScore(double fidelity, double aliasing, 
                                               double reconstruction, double efficiency) {
        // 加权计算采样质量
        double fidelityWeight = 0.4;
        double aliasingWeight = 0.3;
        double reconstructionWeight = 0.2;
        double efficiencyWeight = 0.1;
        
        // 混叠是负面指标，需要反转
        double normalizedAliasing = 1.0 - Math.min(1.0, Math.max(0.0, aliasing));
        
        return fidelityWeight * fidelity + 
               aliasingWeight * normalizedAliasing + 
               reconstructionWeight * reconstruction + 
               efficiencyWeight * efficiency;
    }
    
    private void updateOverallQuality() {
        // 综合信号质量和采样质量
        this.overallQuality = 0.6 * currentSignalQuality + 0.4 * currentSamplingQuality;
        
        // 检查质量违规
        if (overallQuality < qualityThreshold) {
            qualityViolations.incrementAndGet();
        }
        
        // 更新质量预测
        qualityPredictor.updatePrediction(overallQuality);
    }
    
    private void updateChannelQuality(String channelId, double quality, int sampleCount) {
        channelQuality.compute(channelId, (key, metrics) -> {
            if (metrics == null) {
                metrics = new QualityMetrics(channelId);
            }
            metrics.addQualityMeasurement(quality, sampleCount);
            return metrics;
        });
    }
    
    private void recordQualitySnapshot(double quality, String type) {
        QualitySnapshot snapshot = new QualitySnapshot(
            System.currentTimeMillis(),
            quality,
            type
        );
        
        synchronized (qualityHistory) {
            qualityHistory.offer(snapshot);
            while (qualityHistory.size() > maxHistorySize) {
                qualityHistory.poll();
            }
        }
    }
    
    private double calculateQualityTrend() {
        synchronized (qualityHistory) {
            if (qualityHistory.size() < 10) {
                return 0.0;
            }
            
            List<QualitySnapshot> snapshots = new ArrayList<>(qualityHistory);
            int size = snapshots.size();
            int windowSize = Math.min(20, size);
            
            // 计算线性趋势
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            
            for (int i = 0; i < windowSize; i++) {
                int index = size - windowSize + i;
                double x = i;
                double y = snapshots.get(index).quality;
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }
            
            double denominator = windowSize * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) {
                return 0.0;
            }
            
            return (windowSize * sumXY - sumX * sumY) / denominator;
        }
    }
    
    private double calculateAverageQuality() {
        synchronized (qualityHistory) {
            if (qualityHistory.isEmpty()) {
                return overallQuality;
            }
            
            return qualityHistory.stream()
                .mapToDouble(snapshot -> snapshot.quality)
                .average()
                .orElse(overallQuality);
        }
    }
    
    private double calculateQualityVariance() {
        synchronized (qualityHistory) {
            if (qualityHistory.size() < 2) {
                return 0.0;
            }
            
            double mean = calculateAverageQuality();
            double variance = qualityHistory.stream()
                .mapToDouble(snapshot -> Math.pow(snapshot.quality - mean, 2))
                .average()
                .orElse(0.0);
            
            return Math.sqrt(variance); // 返回标准差
        }
    }
    
    private double getAverageSNR() {
        return channelQuality.values().stream()
            .mapToDouble(QualityMetrics::getAverageQuality)
            .average()
            .orElse(0.0) * 50; // 假设质量分数对应SNR范围
    }
    
    private double getSpectralQualityScore() {
        return currentSignalQuality; // 简化实现
    }
    
    private double getAliasingLevel() {
        return Math.max(0.0, 1.0 - currentSamplingQuality); // 简化实现
    }
    
    private double getReconstructionError() {
        return Math.max(0.0, 1.0 - currentSamplingQuality) * 0.1; // 简化实现
    }
    
    private Map<String, Double> getChannelQualitySummary() {
        Map<String, Double> summary = new HashMap<>();
        channelQuality.forEach((channelId, metrics) -> 
            summary.put(channelId, metrics.getAverageQuality()));
        return summary;
    }
    
    // 内部类
    
    private static class QualitySnapshot {
        final long timestamp;
        final double quality;
        final String type;
        
        QualitySnapshot(long timestamp, double quality, String type) {
            this.timestamp = timestamp;
            this.quality = quality;
            this.type = type;
        }
    }
    
    public static class QualityMetrics {
        private final String channelId;
        private long totalMeasurements = 0;
        private double totalQuality = 0.0;
        private double averageQuality = 0.0;
        private double minQuality = Double.MAX_VALUE;
        private double maxQuality = Double.MIN_VALUE;
        private final Queue<Double> recentQualities;
        
        public QualityMetrics(String channelId) {
            this.channelId = channelId;
            this.recentQualities = new LinkedList<>();
        }
        
        public void addQualityMeasurement(double quality, int sampleCount) {
            totalMeasurements++;
            totalQuality += quality;
            averageQuality = totalQuality / totalMeasurements;
            
            minQuality = Math.min(minQuality, quality);
            maxQuality = Math.max(maxQuality, quality);
            
            recentQualities.offer(quality);
            while (recentQualities.size() > 50) {
                recentQualities.poll();
            }
        }
        
        public String getChannelId() { return channelId; }
        public double getAverageQuality() { return averageQuality; }
        public double getMinQuality() { return minQuality; }
        public double getMaxQuality() { return maxQuality; }
        public long getTotalMeasurements() { return totalMeasurements; }
        
        public double getRecentQualityTrend() {
            if (recentQualities.size() < 5) return 0.0;
            
            List<Double> qualities = new ArrayList<>(recentQualities);
            int size = qualities.size();
            int halfSize = size / 2;
            
            double firstHalf = qualities.subList(0, halfSize).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
            double secondHalf = qualities.subList(halfSize, size).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            return secondHalf - firstHalf;
        }
    }
    
    public static class QualityImprovementSuggestion {
        private final List<String> suggestions;
        private final double priorityScore;
        private final double currentQuality;
        
        public QualityImprovementSuggestion(List<String> suggestions, double priorityScore, double currentQuality) {
            this.suggestions = new ArrayList<>(suggestions);
            this.priorityScore = priorityScore;
            this.currentQuality = currentQuality;
        }
        
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
        public double getPriorityScore() { return priorityScore; }
        public double getCurrentQuality() { return currentQuality; }
        public boolean isUrgent() { return priorityScore > 0.5; }
        
        @Override
        public String toString() {
            return String.format("QualityImprovementSuggestion{quality=%.3f, priority=%.3f, suggestions=%s}",
                currentQuality, priorityScore, suggestions);
        }
    }
    
    public static class QualityReport {
        private final double signalQuality;
        private final double samplingQuality;
        private final double overallQuality;
        private final double qualityThreshold;
        private final double averageQuality;
        private final double qualityVariance;
        private final double qualityTrend;
        private final long totalSamples;
        private final long qualityViolations;
        private final Map<String, Double> channelQualities;
        
        public QualityReport(double signalQuality, double samplingQuality, double overallQuality,
                           double qualityThreshold, double averageQuality, double qualityVariance,
                           double qualityTrend, long totalSamples, long qualityViolations,
                           Map<String, Double> channelQualities) {
            this.signalQuality = signalQuality;
            this.samplingQuality = samplingQuality;
            this.overallQuality = overallQuality;
            this.qualityThreshold = qualityThreshold;
            this.averageQuality = averageQuality;
            this.qualityVariance = qualityVariance;
            this.qualityTrend = qualityTrend;
            this.totalSamples = totalSamples;
            this.qualityViolations = qualityViolations;
            this.channelQualities = new HashMap<>(channelQualities);
        }
        
        // Getters
        public double getSignalQuality() { return signalQuality; }
        public double getSamplingQuality() { return samplingQuality; }
        public double getOverallQuality() { return overallQuality; }
        public double getQualityThreshold() { return qualityThreshold; }
        public double getAverageQuality() { return averageQuality; }
        public double getQualityVariance() { return qualityVariance; }
        public double getQualityTrend() { return qualityTrend; }
        public long getTotalSamples() { return totalSamples; }
        public long getQualityViolations() { return qualityViolations; }
        public Map<String, Double> getChannelQualities() { return new HashMap<>(channelQualities); }
        
        public double getViolationRate() {
            return totalSamples > 0 ? (double) qualityViolations / totalSamples : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "QualityReport{signal=%.3f, sampling=%.3f, overall=%.3f, threshold=%.3f, " +
                "avg=%.3f, variance=%.3f, trend=%.3f, samples=%d, violations=%d}",
                signalQuality, samplingQuality, overallQuality, qualityThreshold,
                averageQuality, qualityVariance, qualityTrend, totalSamples, qualityViolations
            );
        }
    }
    
    private static class QualityPredictor {
        private final Queue<Double> qualitySamples;
        private final int maxSamples = 30;
        private double trend = 0.0;
        private double lastQuality = 1.0;
        
        public QualityPredictor() {
            this.qualitySamples = new LinkedList<>();
        }
        
        public void updatePrediction(double currentQuality) {
            qualitySamples.offer(currentQuality);
            while (qualitySamples.size() > maxSamples) {
                qualitySamples.poll();
            }
            
            if (qualitySamples.size() >= 2) {
                trend = 0.8 * trend + 0.2 * (currentQuality - lastQuality);
            }
            
            lastQuality = currentQuality;
        }
        
        public double predictQuality(long futureTimeMs) {
            if (qualitySamples.isEmpty()) {
                return 0.8; // 默认预测值
            }
            
            double timeFactorSeconds = futureTimeMs / 1000.0;
            double predictedQuality = lastQuality + trend * timeFactorSeconds;
            
            return Math.max(0.0, Math.min(1.0, predictedQuality));
        }
    }
    
    private static class SignalQualityEvaluator {
        
        public double calculateSNR(double[] signal) {
            if (signal.length < 10) return 0.0;
            
            // 计算信号功率和噪声功率的简化估计
            double signalPower = 0.0;
            double noisePower = 0.0;
            
            // 假设前10%和后10%为噪声段
            int noiseLength = signal.length / 10;
            
            for (int i = 0; i < noiseLength; i++) {
                noisePower += signal[i] * signal[i];
                noisePower += signal[signal.length - 1 - i] * signal[signal.length - 1 - i];
            }
            noisePower /= (2 * noiseLength);
            
            for (double value : signal) {
                signalPower += value * value;
            }
            signalPower /= signal.length;
            
            if (noisePower <= 0) return 50.0; // 最大SNR
            
            return 10 * Math.log10(signalPower / noisePower);
        }
        
        public double calculateDynamicRange(double[] signal) {
            if (signal.length == 0) return 0.0;
            
            double min = Arrays.stream(signal).min().orElse(0.0);
            double max = Arrays.stream(signal).max().orElse(0.0);
            
            if (min <= 0) return Math.abs(max - min);
            
            return 20 * Math.log10(max / min);
        }
        
        public double calculateSpectralQuality(double[] signal) {
            // 简化的频谱质量评估
            if (signal.length < 32) return 0.5;
            
            // 计算频谱平坦度作为质量指标
            double[] spectrum = computeSpectrum(signal);
            double geometricMean = 1.0;
            double arithmeticMean = 0.0;
            
            for (double value : spectrum) {
                if (value > 0) {
                    geometricMean *= Math.pow(value, 1.0 / spectrum.length);
                    arithmeticMean += value;
                }
            }
            arithmeticMean /= spectrum.length;
            
            return arithmeticMean > 0 ? geometricMean / arithmeticMean : 0.0;
        }
        
        public double calculateContinuity(double[] signal) {
            if (signal.length < 2) return 1.0;
            
            // 计算信号连续性（基于一阶差分）
            double totalVariation = 0.0;
            for (int i = 1; i < signal.length; i++) {
                totalVariation += Math.abs(signal[i] - signal[i-1]);
            }
            
            double signalRange = Arrays.stream(signal).max().orElse(1.0) - 
                               Arrays.stream(signal).min().orElse(0.0);
            
            if (signalRange <= 0) return 1.0;
            
            double normalizedVariation = totalVariation / (signal.length * signalRange);
            return Math.max(0.0, 1.0 - normalizedVariation);
        }
        
        private double[] computeSpectrum(double[] signal) {
            // 简化的频谱计算（实际应用中应使用FFT）
            int spectrumSize = Math.min(signal.length / 2, 64);
            double[] spectrum = new double[spectrumSize];
            
            for (int k = 0; k < spectrumSize; k++) {
                double real = 0.0, imag = 0.0;
                for (int n = 0; n < signal.length; n++) {
                    double angle = -2.0 * Math.PI * k * n / signal.length;
                    real += signal[n] * Math.cos(angle);
                    imag += signal[n] * Math.sin(angle);
                }
                spectrum[k] = Math.sqrt(real * real + imag * imag);
            }
            
            return spectrum;
        }
    }
    
    private static class SamplingQualityEvaluator {
        
        public double calculateFidelity(double[] original, double[] sampled) {
            if (original.length == 0 || sampled.length == 0) return 0.0;
            
            // 计算相关系数作为保真度指标
            double[] interpolated = interpolate(sampled, original.length);
            return calculateCorrelation(original, interpolated);
        }
        
        public double calculateAliasingLevel(double[] signal, double samplingRate) {
            // 简化的混叠检测
            double nyquistFreq = samplingRate / 2.0;
            double[] spectrum = computeSpectrum(signal);
            
            double totalEnergy = 0.0;
            double aliasingEnergy = 0.0;
            
            for (int i = 0; i < spectrum.length; i++) {
                double freq = (double) i * samplingRate / (2 * spectrum.length);
                totalEnergy += spectrum[i];
                
                if (freq > nyquistFreq) {
                    aliasingEnergy += spectrum[i];
                }
            }
            
            return totalEnergy > 0 ? aliasingEnergy / totalEnergy : 0.0;
        }
        
        public double calculateReconstructionQuality(double[] original, double[] sampled) {
            if (original.length == 0 || sampled.length == 0) return 0.0;
            
            double[] reconstructed = interpolate(sampled, original.length);
            double mse = 0.0;
            
            for (int i = 0; i < original.length; i++) {
                double error = original[i] - reconstructed[i];
                mse += error * error;
            }
            mse /= original.length;
            
            double signalPower = Arrays.stream(original)
                .map(x -> x * x)
                .average()
                .orElse(1.0);
            
            return signalPower > 0 ? Math.max(0.0, 1.0 - mse / signalPower) : 0.0;
        }
        
        public double calculateSamplingEfficiency(SamplingStrategy strategy) {
            // 基于采样率和方法复杂度计算效率
            double rateEfficiency = Math.min(1.0, strategy.getSamplingRate() / 1000.0); // 假设1kHz为参考
            double methodEfficiency = strategy.getSamplingMethod().getComplexityLevel() <= 2 ? 1.0 : 0.8;
            
            return 0.7 * rateEfficiency + 0.3 * methodEfficiency;
        }
        
        private double[] interpolate(double[] sampled, int targetLength) {
            if (sampled.length >= targetLength) {
                return Arrays.copyOf(sampled, targetLength);
            }
            
            double[] interpolated = new double[targetLength];
            double ratio = (double) (sampled.length - 1) / (targetLength - 1);
            
            for (int i = 0; i < targetLength; i++) {
                double index = i * ratio;
                int lowerIndex = (int) Math.floor(index);
                int upperIndex = Math.min(lowerIndex + 1, sampled.length - 1);
                double fraction = index - lowerIndex;
                
                interpolated[i] = sampled[lowerIndex] * (1 - fraction) + 
                                sampled[upperIndex] * fraction;
            }
            
            return interpolated;
        }
        
        private double calculateCorrelation(double[] x, double[] y) {
            if (x.length != y.length || x.length == 0) return 0.0;
            
            double meanX = Arrays.stream(x).average().orElse(0.0);
            double meanY = Arrays.stream(y).average().orElse(0.0);
            
            double numerator = 0.0;
            double denomX = 0.0;
            double denomY = 0.0;
            
            for (int i = 0; i < x.length; i++) {
                double dx = x[i] - meanX;
                double dy = y[i] - meanY;
                
                numerator += dx * dy;
                denomX += dx * dx;
                denomY += dy * dy;
            }
            
            double denominator = Math.sqrt(denomX * denomY);
            return denominator > 0 ? numerator / denominator : 0.0;
        }
        
        private double[] computeSpectrum(double[] signal) {
            // 重用SignalQualityEvaluator中的方法
            int spectrumSize = Math.min(signal.length / 2, 64);
            double[] spectrum = new double[spectrumSize];
            
            for (int k = 0; k < spectrumSize; k++) {
                double real = 0.0, imag = 0.0;
                for (int n = 0; n < signal.length; n++) {
                    double angle = -2.0 * Math.PI * k * n / signal.length;
                    real += signal[n] * Math.cos(angle);
                    imag += signal[n] * Math.sin(angle);
                }
                spectrum[k] = Math.sqrt(real * real + imag * imag);
            }
            
            return spectrum;
        }
    }
}
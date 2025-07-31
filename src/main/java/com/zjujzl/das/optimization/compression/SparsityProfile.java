package com.zjujzl.das.optimization.compression;

/**
 * 稀疏性配置文件
 * 
 * 存储信号在不同变换域的稀疏性特征分析结果
 */
public class SparsityProfile {
    
    private final int signalLength;
    private final double timeSparsity;
    private final double frequencySparsity;
    private final double waveletSparsity;
    private final double dictionarySparsity;
    private final double complexity;
    
    public SparsityProfile(int signalLength, double timeSparsity, double frequencySparsity, 
                          double waveletSparsity, double dictionarySparsity, double complexity) {
        this.signalLength = signalLength;
        this.timeSparsity = timeSparsity;
        this.frequencySparsity = frequencySparsity;
        this.waveletSparsity = waveletSparsity;
        this.dictionarySparsity = dictionarySparsity;
        this.complexity = complexity;
    }
    
    public int getSignalLength() {
        return signalLength;
    }
    
    public double getTimeSparsity() {
        return timeSparsity;
    }
    
    public double getFrequencySparsity() {
        return frequencySparsity;
    }
    
    public double getWaveletSparsity() {
        return waveletSparsity;
    }
    
    public double getDictionarySparsity() {
        return dictionarySparsity;
    }
    
    public double getComplexity() {
        return complexity;
    }
    
    /**
     * 获取最佳稀疏性（最高的稀疏性值）
     */
    public double getBestSparsity() {
        return Math.max(Math.max(timeSparsity, frequencySparsity), 
                       Math.max(waveletSparsity, dictionarySparsity));
    }
    
    /**
     * 获取推荐的压缩方法
     */
    public CompressionMethod getRecommendedMethod() {
        double maxSparsity = Math.max(Math.max(timeSparsity, frequencySparsity), 
                                    Math.max(waveletSparsity, dictionarySparsity));
        
        if (maxSparsity == waveletSparsity) {
            return CompressionMethod.WAVELET_CS;
        } else if (maxSparsity == frequencySparsity) {
            return CompressionMethod.FREQUENCY_DOMAIN;
        } else if (maxSparsity == dictionarySparsity) {
            return CompressionMethod.DICTIONARY_LEARNING;
        } else {
            return CompressionMethod.QUANTIZATION;
        }
    }
    
    /**
     * 判断是否适合压缩
     */
    public boolean isCompressible(double threshold) {
        return getBestSparsity() > threshold;
    }
    
    @Override
    public String toString() {
        return String.format("SparsityProfile{length=%d, time=%.3f, freq=%.3f, wavelet=%.3f, dict=%.3f, complexity=%.3f}",
                signalLength, timeSparsity, frequencySparsity, waveletSparsity, dictionarySparsity, complexity);
    }
}
package com.zjujzl.das.optimization.compression;

/**
 * 压缩策略配置
 * 
 * 定义具体的压缩参数和策略选择
 */
public class CompressionStrategy {
    
    private CompressionMethod primaryMethod;
    private double compressionRatio;
    private int blockSize;
    private int waveletLevels;
    private boolean multiScaleEnabled;
    private double qualityThreshold;
    
    // 默认构造函数
    public CompressionStrategy() {
        this.primaryMethod = CompressionMethod.QUANTIZATION;
        this.compressionRatio = 0.5;
        this.blockSize = 1024;
        this.waveletLevels = 4;
        this.multiScaleEnabled = false;
        this.qualityThreshold = 0.8;
    }
    
    // 完整构造函数
    public CompressionStrategy(CompressionMethod primaryMethod, double compressionRatio, 
                             int blockSize, int waveletLevels, boolean multiScaleEnabled, 
                             double qualityThreshold) {
        this.primaryMethod = primaryMethod;
        this.compressionRatio = compressionRatio;
        this.blockSize = blockSize;
        this.waveletLevels = waveletLevels;
        this.multiScaleEnabled = multiScaleEnabled;
        this.qualityThreshold = qualityThreshold;
    }
    
    // Getter和Setter方法
    public CompressionMethod getPrimaryMethod() {
        return primaryMethod;
    }
    
    public void setPrimaryMethod(CompressionMethod primaryMethod) {
        this.primaryMethod = primaryMethod;
    }
    
    public double getCompressionRatio() {
        return compressionRatio;
    }
    
    public void setCompressionRatio(double compressionRatio) {
        this.compressionRatio = Math.max(0.1, Math.min(1.0, compressionRatio));
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public void setBlockSize(int blockSize) {
        this.blockSize = Math.max(64, blockSize);
    }
    
    public int getWaveletLevels() {
        return waveletLevels;
    }
    
    public void setWaveletLevels(int waveletLevels) {
        this.waveletLevels = Math.max(1, Math.min(8, waveletLevels));
    }
    
    public boolean isMultiScaleEnabled() {
        return multiScaleEnabled;
    }
    
    public void enableMultiScale(boolean multiScaleEnabled) {
        this.multiScaleEnabled = multiScaleEnabled;
    }
    
    public double getQualityThreshold() {
        return qualityThreshold;
    }
    
    public void setQualityThreshold(double qualityThreshold) {
        this.qualityThreshold = Math.max(0.0, Math.min(1.0, qualityThreshold));
    }
    
    /**
     * 创建降低压缩率的策略副本
     */
    public CompressionStrategy withLowerCompressionRatio() {
        CompressionStrategy newStrategy = this.copy();
        newStrategy.setCompressionRatio(this.compressionRatio * 1.2); // 降低20%的压缩率
        return newStrategy;
    }
    
    /**
     * 创建指定方法的策略副本
     */
    public CompressionStrategy withMethod(CompressionMethod method) {
        CompressionStrategy newStrategy = this.copy();
        newStrategy.setPrimaryMethod(method);
        return newStrategy;
    }
    
    /**
     * 创建策略副本
     */
    public CompressionStrategy copy() {
        return new CompressionStrategy(
            this.primaryMethod,
            this.compressionRatio,
            this.blockSize,
            this.waveletLevels,
            this.multiScaleEnabled,
            this.qualityThreshold
        );
    }
    
    /**
     * 根据信号特征自动调整策略
     */
    public void autoAdjust(SparsityProfile profile) {
        // 根据信号长度调整块大小
        if (profile.getSignalLength() > 4096) {
            this.blockSize = 2048;
            this.multiScaleEnabled = true;
        } else if (profile.getSignalLength() > 1024) {
            this.blockSize = 1024;
        } else {
            this.blockSize = 512;
        }
        
        // 根据复杂度调整压缩率
        double complexity = profile.getComplexity();
        if (complexity > 0.8) {
            // 高复杂度信号，降低压缩率
            this.compressionRatio = Math.min(this.compressionRatio * 1.3, 0.9);
        } else if (complexity < 0.3) {
            // 低复杂度信号，可以提高压缩率
            this.compressionRatio = Math.max(this.compressionRatio * 0.8, 0.2);
        }
        
        // 根据稀疏性调整小波级数
        if (profile.getWaveletSparsity() > 0.7) {
            this.waveletLevels = Math.min(this.waveletLevels + 1, 6);
        } else if (profile.getWaveletSparsity() < 0.3) {
            this.waveletLevels = Math.max(this.waveletLevels - 1, 2);
        }
    }
    
    /**
     * 验证策略参数的有效性
     */
    public boolean isValid() {
        return primaryMethod != null &&
               compressionRatio > 0 && compressionRatio <= 1.0 &&
               blockSize > 0 &&
               waveletLevels > 0 && waveletLevels <= 8 &&
               qualityThreshold >= 0 && qualityThreshold <= 1.0;
    }
    
    /**
     * 获取策略的预估计算复杂度
     */
    public double getEstimatedComplexity() {
        double baseComplexity = primaryMethod.getComplexityLevel();
        
        // 多尺度处理增加复杂度
        if (multiScaleEnabled) {
            baseComplexity *= 1.5;
        }
        
        // 小波级数影响复杂度
        if (primaryMethod == CompressionMethod.WAVELET_CS) {
            baseComplexity *= (1.0 + waveletLevels * 0.2);
        }
        
        // 块大小影响复杂度
        baseComplexity *= Math.log(blockSize) / Math.log(1024);
        
        return baseComplexity;
    }
    
    @Override
    public String toString() {
        return String.format("CompressionStrategy{method=%s, ratio=%.3f, blockSize=%d, waveletLevels=%d, multiScale=%s, quality=%.3f}",
                primaryMethod, compressionRatio, blockSize, waveletLevels, multiScaleEnabled, qualityThreshold);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CompressionStrategy that = (CompressionStrategy) obj;
        return Double.compare(that.compressionRatio, compressionRatio) == 0 &&
               blockSize == that.blockSize &&
               waveletLevels == that.waveletLevels &&
               multiScaleEnabled == that.multiScaleEnabled &&
               Double.compare(that.qualityThreshold, qualityThreshold) == 0 &&
               primaryMethod == that.primaryMethod;
    }
    
    @Override
    public int hashCode() {
        int result = primaryMethod != null ? primaryMethod.hashCode() : 0;
        long temp = Double.doubleToLongBits(compressionRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + blockSize;
        result = 31 * result + waveletLevels;
        result = 31 * result + (multiScaleEnabled ? 1 : 0);
        temp = Double.doubleToLongBits(qualityThreshold);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
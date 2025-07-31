package com.zjujzl.das.optimization.compression;

/**
 * 压缩数据类
 * 
 * 存储压缩后的数据和相关元信息
 */
public class CompressedData {
    
    private final byte[] compressedBytes;
    private final CompressionMethod method;
    private final double compressionRatio;
    private final long originalSize;
    private final long compressedSize;
    private final long compressionTime;
    private final double qualityScore;
    
    // 基本构造函数
    public CompressedData(byte[] compressedBytes, CompressionMethod method, double compressionRatio) {
        this(compressedBytes, method, compressionRatio, 0, 0, 0.0);
    }
    
    // 完整构造函数
    public CompressedData(byte[] compressedBytes, CompressionMethod method, double compressionRatio,
                         long originalSize, long compressionTime, double qualityScore) {
        this.compressedBytes = compressedBytes != null ? compressedBytes.clone() : new byte[0];
        this.method = method != null ? method : CompressionMethod.NONE;
        this.compressionRatio = Math.max(0.0, compressionRatio);
        this.originalSize = Math.max(0, originalSize);
        this.compressedSize = this.compressedBytes.length;
        this.compressionTime = Math.max(0, compressionTime);
        this.qualityScore = Math.max(0.0, Math.min(1.0, qualityScore));
    }
    
    // Getter方法
    
    public byte[] getCompressedBytes() {
        return compressedBytes.clone();
    }
    
    public CompressionMethod getMethod() {
        return method;
    }
    
    public double getCompressionRatio() {
        return compressionRatio;
    }
    
    public long getOriginalSize() {
        return originalSize;
    }
    
    public long getCompressedSize() {
        return compressedSize;
    }
    
    public long getCompressionTime() {
        return compressionTime;
    }
    
    public double getQualityScore() {
        return qualityScore;
    }
    
    /**
     * 计算实际压缩率（基于字节大小）
     */
    public double getActualCompressionRatio() {
        if (originalSize <= 0) {
            return compressionRatio;
        }
        return (double) originalSize / compressedSize;
    }
    
    /**
     * 计算空间节省百分比
     */
    public double getSpaceSavingPercentage() {
        if (originalSize <= 0) {
            return 0.0;
        }
        return (1.0 - (double) compressedSize / originalSize) * 100.0;
    }
    
    /**
     * 计算压缩效率（质量/压缩率）
     */
    public double getCompressionEfficiency() {
        if (compressionRatio <= 0) {
            return 0.0;
        }
        return qualityScore / compressionRatio;
    }
    
    /**
     * 判断压缩是否有效
     */
    public boolean isEffective() {
        return compressionRatio > 1.0 && qualityScore > 0.5;
    }
    
    /**
     * 判断压缩质量是否达标
     */
    public boolean isQualityAcceptable(double threshold) {
        return qualityScore >= threshold;
    }
    
    /**
     * 获取压缩性能摘要
     */
    public CompressionSummary getSummary() {
        return new CompressionSummary(
            method,
            compressionRatio,
            getActualCompressionRatio(),
            getSpaceSavingPercentage(),
            qualityScore,
            getCompressionEfficiency(),
            compressionTime
        );
    }
    
    /**
     * 创建带有质量评分的新实例
     */
    public CompressedData withQualityScore(double newQualityScore) {
        return new CompressedData(
            this.compressedBytes,
            this.method,
            this.compressionRatio,
            this.originalSize,
            this.compressionTime,
            newQualityScore
        );
    }
    
    /**
     * 创建带有压缩时间的新实例
     */
    public CompressedData withCompressionTime(long newCompressionTime) {
        return new CompressedData(
            this.compressedBytes,
            this.method,
            this.compressionRatio,
            this.originalSize,
            newCompressionTime,
            this.qualityScore
        );
    }
    
    @Override
    public String toString() {
        return String.format("CompressedData{method=%s, ratio=%.2f, actualRatio=%.2f, quality=%.3f, size=%d->%d, time=%dms}",
                method, compressionRatio, getActualCompressionRatio(), qualityScore, 
                originalSize, compressedSize, compressionTime);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CompressedData that = (CompressedData) obj;
        return Double.compare(that.compressionRatio, compressionRatio) == 0 &&
               originalSize == that.originalSize &&
               compressedSize == that.compressedSize &&
               compressionTime == that.compressionTime &&
               Double.compare(that.qualityScore, qualityScore) == 0 &&
               java.util.Arrays.equals(compressedBytes, that.compressedBytes) &&
               method == that.method;
    }
    
    @Override
    public int hashCode() {
        int result = java.util.Arrays.hashCode(compressedBytes);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        long temp = Double.doubleToLongBits(compressionRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (originalSize ^ (originalSize >>> 32));
        result = 31 * result + (int) (compressedSize ^ (compressedSize >>> 32));
        result = 31 * result + (int) (compressionTime ^ (compressionTime >>> 32));
        temp = Double.doubleToLongBits(qualityScore);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
    
    /**
     * 压缩摘要内部类
     */
    public static class CompressionSummary {
        private final CompressionMethod method;
        private final double targetRatio;
        private final double actualRatio;
        private final double spaceSaving;
        private final double quality;
        private final double efficiency;
        private final long processingTime;
        
        public CompressionSummary(CompressionMethod method, double targetRatio, double actualRatio,
                                double spaceSaving, double quality, double efficiency, long processingTime) {
            this.method = method;
            this.targetRatio = targetRatio;
            this.actualRatio = actualRatio;
            this.spaceSaving = spaceSaving;
            this.quality = quality;
            this.efficiency = efficiency;
            this.processingTime = processingTime;
        }
        
        // Getter方法
        public CompressionMethod getMethod() { return method; }
        public double getTargetRatio() { return targetRatio; }
        public double getActualRatio() { return actualRatio; }
        public double getSpaceSaving() { return spaceSaving; }
        public double getQuality() { return quality; }
        public double getEfficiency() { return efficiency; }
        public long getProcessingTime() { return processingTime; }
        
        @Override
        public String toString() {
            return String.format("Summary{method=%s, target=%.2f, actual=%.2f, saving=%.1f%%, quality=%.3f, efficiency=%.3f, time=%dms}",
                    method, targetRatio, actualRatio, spaceSaving, quality, efficiency, processingTime);
        }
    }
}
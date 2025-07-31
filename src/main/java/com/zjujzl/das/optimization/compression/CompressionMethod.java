package com.zjujzl.das.optimization.compression;

/**
 * 压缩方法枚举
 * 
 * 定义支持的数据压缩算法类型
 */
public enum CompressionMethod {
    
    /**
     * 小波压缩感知方法
     * 基于小波变换的稀疏表示和压缩感知理论
     */
    WAVELET_CS("Wavelet Compressed Sensing", "基于小波变换的压缩感知"),
    
    /**
     * 频域压缩方法
     * 基于FFT变换的频域稀疏化压缩
     */
    FREQUENCY_DOMAIN("Frequency Domain Compression", "频域稀疏化压缩"),
    
    /**
     * 字典学习压缩方法
     * 基于自适应字典学习的稀疏编码
     */
    DICTIONARY_LEARNING("Dictionary Learning Compression", "字典学习稀疏编码"),
    
    /**
     * 混合压缩方法
     * 组合多种压缩算法，选择最优结果
     */
    HYBRID("Hybrid Compression", "混合压缩算法"),
    
    /**
     * 量化压缩方法
     * 简单的信号量化和下采样
     */
    QUANTIZATION("Quantization Compression", "量化压缩"),
    
    /**
     * 无压缩
     * 保持原始数据不变
     */
    NONE("No Compression", "无压缩");
    
    private final String name;
    private final String description;
    
    CompressionMethod(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为高级压缩方法
     */
    public boolean isAdvanced() {
        return this == WAVELET_CS || this == FREQUENCY_DOMAIN || 
               this == DICTIONARY_LEARNING || this == HYBRID;
    }
    
    /**
     * 获取压缩复杂度等级
     */
    public int getComplexityLevel() {
        switch (this) {
            case NONE:
                return 0;
            case QUANTIZATION:
                return 1;
            case FREQUENCY_DOMAIN:
                return 2;
            case WAVELET_CS:
                return 3;
            case DICTIONARY_LEARNING:
                return 4;
            case HYBRID:
                return 5;
            default:
                return 1;
        }
    }
    
    /**
     * 根据名称获取压缩方法
     */
    public static CompressionMethod fromName(String name) {
        for (CompressionMethod method : values()) {
            if (method.name.equalsIgnoreCase(name) || method.name().equalsIgnoreCase(name)) {
                return method;
            }
        }
        return QUANTIZATION; // 默认方法
    }
    
    @Override
    public String toString() {
        return name;
    }
}
package com.zjujzl.das.optimization.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 稀疏编码类
 * 
 * 存储稀疏表示的结果，包括原子索引、系数和重建误差
 */
public class SparseCode {
    
    private final int[] atomIndices;     // 选中的字典原子索引
    private final double[] coefficients; // 对应的系数
    private final double reconstructionError; // 重建误差
    private final int originalLength;     // 原始信号长度
    
    public SparseCode(int[] atomIndices, double[] coefficients, double reconstructionError) {
        this(atomIndices, coefficients, reconstructionError, 0);
    }
    
    public SparseCode(int[] atomIndices, double[] coefficients, double reconstructionError, int originalLength) {
        this.atomIndices = atomIndices != null ? atomIndices.clone() : new int[0];
        this.coefficients = coefficients != null ? coefficients.clone() : new double[0];
        this.reconstructionError = Math.max(0.0, reconstructionError);
        this.originalLength = Math.max(0, originalLength);
        
        // 验证数据一致性
        if (this.atomIndices.length != this.coefficients.length) {
            throw new IllegalArgumentException("Atom indices and coefficients must have the same length");
        }
    }
    
    // Getter方法
    
    public int[] getAtomIndices() {
        return atomIndices.clone();
    }
    
    public double[] getCoefficients() {
        return coefficients.clone();
    }
    
    public double getReconstructionError() {
        return reconstructionError;
    }
    
    public int getOriginalLength() {
        return originalLength;
    }
    
    /**
     * 获取稀疏度（非零系数的比例）
     */
    public double getSparsity() {
        if (originalLength <= 0) {
            return atomIndices.length > 0 ? 1.0 : 0.0;
        }
        return (double) atomIndices.length / originalLength;
    }
    
    /**
     * 获取有效系数数量
     */
    public int getNonZeroCount() {
        return atomIndices.length;
    }
    
    /**
     * 获取压缩率估计
     */
    public double getCompressionRatio() {
        if (originalLength <= 0) {
            return 1.0;
        }
        
        // 估计压缩后的大小：索引 + 系数
        int compressedSize = atomIndices.length * (Integer.BYTES + Double.BYTES);
        int originalSize = originalLength * Double.BYTES;
        
        return originalSize > 0 ? (double) originalSize / compressedSize : 1.0;
    }
    
    /**
     * 计算系数的能量
     */
    public double getCoefficientsEnergy() {
        double energy = 0.0;
        for (double coeff : coefficients) {
            energy += coeff * coeff;
        }
        return energy;
    }
    
    /**
     * 获取最大系数的绝对值
     */
    public double getMaxCoefficientMagnitude() {
        double maxMagnitude = 0.0;
        for (double coeff : coefficients) {
            maxMagnitude = Math.max(maxMagnitude, Math.abs(coeff));
        }
        return maxMagnitude;
    }
    
    /**
     * 获取系数的平均绝对值
     */
    public double getMeanCoefficientMagnitude() {
        if (coefficients.length == 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double coeff : coefficients) {
            sum += Math.abs(coeff);
        }
        return sum / coefficients.length;
    }
    
    /**
     * 检查是否为空编码
     */
    public boolean isEmpty() {
        return atomIndices.length == 0;
    }
    
    /**
     * 检查编码是否有效
     */
    public boolean isValid() {
        // 检查索引是否非负
        for (int index : atomIndices) {
            if (index < 0) {
                return false;
            }
        }
        
        // 检查系数是否为有限数
        for (double coeff : coefficients) {
            if (!Double.isFinite(coeff)) {
                return false;
            }
        }
        
        // 检查重建误差是否为有限数
        return Double.isFinite(reconstructionError) && reconstructionError >= 0;
    }
    
    /**
     * 创建阈值化的稀疏编码
     */
    public SparseCode threshold(double threshold) {
        if (threshold <= 0) {
            return this;
        }
        
        // 找出大于阈值的系数
        int count = 0;
        for (double coeff : coefficients) {
            if (Math.abs(coeff) >= threshold) {
                count++;
            }
        }
        
        if (count == coefficients.length) {
            return this; // 没有变化
        }
        
        // 创建新的数组
        int[] newIndices = new int[count];
        double[] newCoefficients = new double[count];
        
        int index = 0;
        for (int i = 0; i < coefficients.length; i++) {
            if (Math.abs(coefficients[i]) >= threshold) {
                newIndices[index] = atomIndices[i];
                newCoefficients[index] = coefficients[i];
                index++;
            }
        }
        
        return new SparseCode(newIndices, newCoefficients, reconstructionError, originalLength);
    }
    
    /**
     * 创建保留前K个最大系数的稀疏编码
     */
    public SparseCode keepTopK(int k) {
        if (k <= 0) {
            return new SparseCode(new int[0], new double[0], reconstructionError, originalLength);
        }
        
        if (k >= coefficients.length) {
            return this;
        }
        
        // 创建索引-系数对并按系数绝对值排序
        IndexCoefficientPair[] pairs = new IndexCoefficientPair[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            pairs[i] = new IndexCoefficientPair(atomIndices[i], coefficients[i]);
        }
        
        Arrays.sort(pairs, (a, b) -> Double.compare(Math.abs(b.coefficient), Math.abs(a.coefficient)));
        
        // 保留前K个
        int[] newIndices = new int[k];
        double[] newCoefficients = new double[k];
        
        for (int i = 0; i < k; i++) {
            newIndices[i] = pairs[i].index;
            newCoefficients[i] = pairs[i].coefficient;
        }
        
        return new SparseCode(newIndices, newCoefficients, reconstructionError, originalLength);
    }
    
    /**
     * 合并两个稀疏编码
     */
    public SparseCode merge(SparseCode other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        
        if (this.isEmpty()) {
            return other;
        }
        
        // 合并索引和系数
        int[] mergedIndices = new int[this.atomIndices.length + other.atomIndices.length];
        double[] mergedCoefficients = new double[this.coefficients.length + other.coefficients.length];
        
        System.arraycopy(this.atomIndices, 0, mergedIndices, 0, this.atomIndices.length);
        System.arraycopy(other.atomIndices, 0, mergedIndices, this.atomIndices.length, other.atomIndices.length);
        
        System.arraycopy(this.coefficients, 0, mergedCoefficients, 0, this.coefficients.length);
        System.arraycopy(other.coefficients, 0, mergedCoefficients, this.coefficients.length, other.coefficients.length);
        
        double mergedError = this.reconstructionError + other.reconstructionError;
        int mergedLength = Math.max(this.originalLength, other.originalLength);
        
        return new SparseCode(mergedIndices, mergedCoefficients, mergedError, mergedLength);
    }
    
    /**
     * 转换为字节数组（用于序列化）
     */
    public byte[] toByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 写入长度信息
            baos.write(ByteBuffer.allocate(4).putInt(atomIndices.length).array());
            baos.write(ByteBuffer.allocate(4).putInt(originalLength).array());
            baos.write(ByteBuffer.allocate(8).putDouble(reconstructionError).array());
            
            // 写入原子索引
            for (int index : atomIndices) {
                baos.write(ByteBuffer.allocate(4).putInt(index).array());
            }
            
            // 写入系数
            for (double coeff : coefficients) {
                baos.write(ByteBuffer.allocate(8).putDouble(coeff).array());
            }
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize SparseCode", e);
        }
    }
    
    /**
     * 从字节数组创建稀疏编码
     */
    public static SparseCode fromByteArray(byte[] bytes) {
        if (bytes == null || bytes.length < 16) {
            return new SparseCode(new int[0], new double[0], 0.0);
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            
            // 读取长度信息
            int length = buffer.getInt();
            int originalLength = buffer.getInt();
            double reconstructionError = buffer.getDouble();
            
            if (length < 0 || length > 10000) { // 合理性检查
                throw new IllegalArgumentException("Invalid sparse code length: " + length);
            }
            
            // 读取原子索引
            int[] atomIndices = new int[length];
            for (int i = 0; i < length; i++) {
                atomIndices[i] = buffer.getInt();
            }
            
            // 读取系数
            double[] coefficients = new double[length];
            for (int i = 0; i < length; i++) {
                coefficients[i] = buffer.getDouble();
            }
            
            return new SparseCode(atomIndices, coefficients, reconstructionError, originalLength);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize SparseCode", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format("SparseCode{atoms=%d, sparsity=%.3f, error=%.6f, energy=%.3f}",
                atomIndices.length, getSparsity(), reconstructionError, getCoefficientsEnergy());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SparseCode that = (SparseCode) obj;
        return Double.compare(that.reconstructionError, reconstructionError) == 0 &&
               originalLength == that.originalLength &&
               Arrays.equals(atomIndices, that.atomIndices) &&
               Arrays.equals(coefficients, that.coefficients);
    }
    
    @Override
    public int hashCode() {
        int result = Arrays.hashCode(atomIndices);
        result = 31 * result + Arrays.hashCode(coefficients);
        long temp = Double.doubleToLongBits(reconstructionError);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + originalLength;
        return result;
    }
    
    /**
     * 内部辅助类：索引-系数对
     */
    private static class IndexCoefficientPair {
        final int index;
        final double coefficient;
        
        IndexCoefficientPair(int index, double coefficient) {
            this.index = index;
            this.coefficient = coefficient;
        }
    }
}
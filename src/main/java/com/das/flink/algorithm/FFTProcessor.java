package com.das.flink.algorithm;

/**
 * FFT处理器 - 快速傅里叶变换实现
 * 用于频域分析和信号处理
 * 
 * @author DAS-Flink Team
 */
public class FFTProcessor {
    
    private static final double TWO_PI = 2.0 * Math.PI;
    
    /**
     * 计算复数FFT
     * 
     * @param real 实部数组
     * @param imag 虚部数组
     * @param n 数据长度（必须是2的幂）
     */
    public static void fft(double[] real, double[] imag, int n) {
        if (n <= 1) return;
        
        // 位反转
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while (j >= bit) {
                j -= bit;
                bit >>= 1;
            }
            j += bit;
            if (i < j) {
                // 交换实部
                double temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                // 交换虚部
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
        }
        
        // FFT计算
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -TWO_PI / len;
            double wlenReal = Math.cos(angle);
            double wlenImag = Math.sin(angle);
            
            for (int i = 0; i < n; i += len) {
                double wReal = 1.0;
                double wImag = 0.0;
                
                for (int k = 0; k < len / 2; k++) {
                    int u = i + k;
                    int v = i + k + len / 2;
                    
                    double uReal = real[u];
                    double uImag = imag[u];
                    double vReal = real[v] * wReal - imag[v] * wImag;
                    double vImag = real[v] * wImag + imag[v] * wReal;
                    
                    real[u] = uReal + vReal;
                    imag[u] = uImag + vImag;
                    real[v] = uReal - vReal;
                    imag[v] = uImag - vImag;
                    
                    double tempReal = wReal * wlenReal - wImag * wlenImag;
                    wImag = wReal * wlenImag + wImag * wlenReal;
                    wReal = tempReal;
                }
            }
        }
    }
    
    /**
     * 计算FFT
     * 
     * @param data 输入数据
     * @return FFT结果的复数数组 [实部, 虚部]
     */
    public static double[][] computeFFT(float[] data) {
        int n = nextPowerOf2(data.length);
        double[] real = new double[n];
        double[] imag = new double[n];
        
        // 复制数据并填充零
        for (int i = 0; i < data.length; i++) {
            real[i] = data[i];
        }
        
        fft(real, imag, n);
        
        return new double[][]{real, imag};
    }
    
    /**
     * 计算功率谱密度
     * 
     * @param data 输入数据
     * @return 功率谱密度数组
     */
    public static double[] computePowerSpectrum(float[] data) {
        int n = nextPowerOf2(data.length);
        double[] real = new double[n];
        double[] imag = new double[n];
        
        // 复制数据并填充零
        for (int i = 0; i < data.length; i++) {
            real[i] = data[i];
        }
        
        fft(real, imag, n);
        
        // 计算功率谱
        double[] power = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            power[i] = real[i] * real[i] + imag[i] * imag[i];
        }
        
        return power;
    }
    
    /**
     * 找到主频率
     * 
     * @param data 输入数据
     * @param sampleRate 采样率
     * @return 主频率（Hz）
     */
    public static double findDominantFrequency(float[] data, float sampleRate) {
        double[] power = computePowerSpectrum(data);
        
        int maxIndex = 0;
        double maxPower = power[0];
        
        for (int i = 1; i < power.length; i++) {
            if (power[i] > maxPower) {
                maxPower = power[i];
                maxIndex = i;
            }
        }
        
        return (double) maxIndex * sampleRate / (2 * power.length);
    }
    
    /**
     * 计算频谱质心
     * 
     * @param data 输入数据
     * @param sampleRate 采样率
     * @return 频谱质心（Hz）
     */
    public static double computeSpectralCentroid(float[] data, float sampleRate) {
        double[] power = computePowerSpectrum(data);
        
        double weightedSum = 0.0;
        double totalPower = 0.0;
        
        for (int i = 0; i < power.length; i++) {
            double frequency = (double) i * sampleRate / (2 * power.length);
            weightedSum += frequency * power[i];
            totalPower += power[i];
        }
        
        return totalPower > 0 ? weightedSum / totalPower : 0.0;
    }
    
    /**
     * 找到下一个2的幂
     */
    private static int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }
}
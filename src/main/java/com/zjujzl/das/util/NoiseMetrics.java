package com.zjujzl.das.util;

import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.model.DenoiseResult;
import com.zjujzl.das.model.EvaluationMetric;

public class NoiseMetrics {

    //信噪比提升计算
    public static double snrImprovement(double[] original, double[] denoised) {
        double signalPower = 0;
        double noisePower = 0;

        for (int i = 0; i < original.length; i++) {
            signalPower += original[i] * original[i];
            double noise = original[i] - denoised[i];
            noisePower += noise * noise;
        }

        if (noisePower == 0) noisePower = 1e-12;
        return 10 * Math.log10(signalPower / noisePower);
    }

    //波形失真度计算
    public static double waveformDistortion(double[] original, double[] denoised) {
        double diffSum = 0;
        double maxOriginal = Double.MIN_VALUE;

        for (int i = 0; i < original.length; i++) {
            diffSum += Math.abs(original[i] - denoised[i]);
            if (Math.abs(original[i]) > maxOriginal) {
                maxOriginal = Math.abs(original[i]);
            }
        }

        return diffSum / (original.length * maxOriginal);
    }

    //特征相关性计算
    public static double featureCorrelation(double[] original, double[] denoised) {
        double meanOrig = mean(original);
        double meanDenoised = mean(denoised);
        double numerator = 0;
        double denomOrig = 0;
        double denomDenoised = 0;

        for (int i = 0; i < original.length; i++) {
            double o = original[i] - meanOrig;
            double d = denoised[i] - meanDenoised;
            numerator += o * d;
            denomOrig += o * o;
            denomDenoised += d * d;
        }

        double denominator = Math.sqrt(denomOrig * denomDenoised);
        if (denominator == 0) return 0;
        return numerator / denominator;
    }

    private static double mean(double[] data) {
        double sum = 0;
        for (double v : data) sum += v;
        return sum / data.length;
    }
}

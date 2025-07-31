package com.zjujzl.das.model;

public class EvaluationMetric {
    public String signalId;
    public String algorithm;
    public double snrImprovement;
    public double waveformDistortion;
    public double featureCorrelation;
    public long latency;

    public EvaluationMetric() {}

    public EvaluationMetric(String signalId, String algorithm, double snrImprovement,
                            double waveformDistortion, double featureCorrelation, long latency) {
        this.signalId = signalId;
        this.algorithm = algorithm;
        this.snrImprovement = snrImprovement;
        this.waveformDistortion = waveformDistortion;
        this.featureCorrelation = featureCorrelation;
        this.latency = latency;
    }

    @Override
    public String toString() {
        return String.format("Metric{alg=%s, SNR=%.2f, Dist=%.2f, Corr=%.2f, latency=%dms}",
                algorithm, snrImprovement, waveformDistortion, featureCorrelation, latency);
    }
}


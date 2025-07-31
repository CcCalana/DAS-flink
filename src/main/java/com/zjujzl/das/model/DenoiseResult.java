package com.zjujzl.das.model;

public class DenoiseResult {
    public SeismicRecord signal;
    public double[] denoised;
    public String algorithmId;
    public long processingTime;

    public DenoiseResult() {}

    public DenoiseResult(SeismicRecord signal, double[] denoised, String algorithmId, long processingTime) {
        this.signal = signal;
        this.denoised = denoised;
        this.algorithmId = algorithmId;
        this.processingTime = processingTime;
    }

    @Override
    public String toString() {
        return String.format("DenoiseResult{station=%s, alg=%s, latency=%dms}",
                signal != null ? signal.station : "N/A", algorithmId, processingTime);
    }
}
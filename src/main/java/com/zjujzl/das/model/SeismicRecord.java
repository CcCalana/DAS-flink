package com.zjujzl.das.model;

import java.util.Arrays;

/**
 * 输入原始信号类
 */
public class SeismicRecord {
    public String network;
    public String station;
    public String location;
    public String channel;
    public double starttime;
    public double endtime;
    public double sampling_rate;
    public int[] data;
    public String idas_version;
    public int measure_length;
    public double geo_lat;

    @Override
    public String toString() {
        return String.format("SeismicRecord{station=%s, starttime=%.3f, length=%d}",
                station, starttime, data != null ? data.length : 0);
    }
}
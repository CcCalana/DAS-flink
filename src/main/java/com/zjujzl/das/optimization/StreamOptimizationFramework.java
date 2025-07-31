package com.zjujzl.das.optimization;

import com.zjujzl.das.model.SeismicRecord;
import com.zjujzl.das.model.EventDetectionResult;
import com.zjujzl.das.optimization.compression.AdaptiveCompressionManager;
import com.zjujzl.das.optimization.sampling.IntelligentSamplingController;
import com.zjujzl.das.optimization.edge.EdgeComputingOptimizer;
import com.zjujzl.das.optimization.ml.MLBasedOptimizer;
import com.zjujzl.das.optimization.resource.ResourceAllocationManager;
import com.zjujzl.das.optimization.latency.LatencyOptimizer;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAS流处理综合优化框架
 * 
 * 基于最新研究成果，在降噪和事件检测之外提供多维度优化：
 * 1. 自适应数据压缩 - 基于信号特征的智能压缩
 * 2. 智能采样控制 - 动态调整采样率以平衡精度和性能
 * 3. 边缘计算优化 - 分布式处理和负载均衡
 * 4. 机器学习驱动优化 - 预测性资源分配和参数调优
 * 5. 资源自适应管理 - 动态资源分配和弹性扩缩容
 * 6. 延迟优化 - 多级缓存和预处理策略
 * 
 * 参考文献：
 * - Photonix 2025: AI-driven DAS optimization
 * - MDPI Algorithms 2023: Sequential noise reduction algorithms
 * - Journal of Grid Computing 2022: Latency and energy-aware stream processing
 * - Applied Sciences 2021: ML-based seismic data compression
 */
public class StreamOptimizationFramework extends ProcessFunction<SeismicRecord, SeismicRecord> {
    
    // 优化组件
    private transient AdaptiveCompressionManager compressionManager;
    private transient IntelligentSamplingController samplingController;
    private transient EdgeComputingOptimizer edgeOptimizer;
    private transient MLBasedOptimizer mlOptimizer;
    private transient ResourceAllocationManager resourceManager;
    private transient LatencyOptimizer latencyOptimizer;
    
    // 状态管理
    private transient ValueState<OptimizationMetrics> metricsState;
    private transient ValueState<AdaptiveParameters> parametersState;
    
    // 配置参数
    private final OptimizationConfig config;
    
    // 性能监控
    private static final Map<String, PerformanceMetrics> globalMetrics = new ConcurrentHashMap<>();
    
    public StreamOptimizationFramework(OptimizationConfig config) {
        this.config = config;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 初始化优化组件
        this.compressionManager = new AdaptiveCompressionManager(config.getCompressionConfig());
        this.samplingController = new IntelligentSamplingController(config.getSamplingConfig());
        this.edgeOptimizer = new EdgeComputingOptimizer(config.getEdgeConfig());
        this.mlOptimizer = new MLBasedOptimizer(config.getMlConfig());
        this.resourceManager = new ResourceAllocationManager(config.getResourceConfig());
        this.latencyOptimizer = new LatencyOptimizer(config.getLatencyConfig());
        
        // 初始化状态
        ValueStateDescriptor<OptimizationMetrics> metricsDescriptor = 
            new ValueStateDescriptor<>("optimization-metrics", OptimizationMetrics.class);
        this.metricsState = getRuntimeContext().getState(metricsDescriptor);
        
        ValueStateDescriptor<AdaptiveParameters> parametersDescriptor = 
            new ValueStateDescriptor<>("adaptive-parameters", AdaptiveParameters.class);
        this.parametersState = getRuntimeContext().getState(parametersDescriptor);
    }
    
    @Override
    public void processElement(SeismicRecord record, Context ctx, Collector<SeismicRecord> out) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取当前优化状态
            OptimizationMetrics currentMetrics = metricsState.value();
            if (currentMetrics == null) {
                currentMetrics = new OptimizationMetrics();
            }
            
            AdaptiveParameters currentParams = parametersState.value();
            if (currentParams == null) {
                currentParams = new AdaptiveParameters();
            }
            
            // 2. 延迟优化 - 预处理和缓存策略
            SeismicRecord preprocessedRecord = latencyOptimizer.preprocess(record, currentParams);
            
            // 3. 智能采样控制 - 动态调整采样策略
            if (!samplingController.shouldProcess(preprocessedRecord, currentMetrics)) {
                // 跳过处理，但更新统计信息
                currentMetrics.incrementSkippedSamples();
                metricsState.update(currentMetrics);
                return;
            }
            
            // 4. 自适应数据压缩
            SeismicRecord compressedRecord = compressionManager.compress(preprocessedRecord, currentParams);
            
            // 5. 边缘计算优化 - 负载均衡和分布式处理
            SeismicRecord optimizedRecord = edgeOptimizer.optimize(compressedRecord, currentMetrics);
            
            // 6. 机器学习驱动的参数优化
            AdaptiveParameters updatedParams = mlOptimizer.optimizeParameters(
                optimizedRecord, currentParams, currentMetrics);
            
            // 7. 资源自适应管理
            resourceManager.adjustResources(currentMetrics, ctx);
            
            // 8. 更新性能指标
            long processingTime = System.currentTimeMillis() - startTime;
            currentMetrics.updateProcessingTime(processingTime);
            currentMetrics.updateThroughput();
            
            // 9. 输出优化后的记录
            out.collect(optimizedRecord);
            
            // 10. 更新状态
            metricsState.update(currentMetrics);
            parametersState.update(updatedParams);
            
            // 11. 全局性能监控
            updateGlobalMetrics(record.station, processingTime, currentMetrics);
            
        } catch (Exception e) {
            System.err.println("ERROR: StreamOptimizationFramework failed: " + e.getMessage());
            // 降级处理：直接输出原始记录
            out.collect(record);
        }
    }
    
    /**
     * 更新全局性能指标
     */
    private void updateGlobalMetrics(String station, long processingTime, OptimizationMetrics metrics) {
        globalMetrics.compute(station, (k, v) -> {
            if (v == null) {
                v = new PerformanceMetrics();
            }
            v.addProcessingTime(processingTime);
            v.updateThroughput(metrics.getThroughput());
            v.updateCompressionRatio(metrics.getCompressionRatio());
            return v;
        });
    }
    
    /**
     * 获取全局性能统计
     */
    public static Map<String, PerformanceMetrics> getGlobalMetrics() {
        return new HashMap<>(globalMetrics);
    }
    
    /**
     * 重置全局性能统计
     */
    public static void resetGlobalMetrics() {
        globalMetrics.clear();
    }
    
    /**
     * 优化配置类
     */
    public static class OptimizationConfig {
        private CompressionConfig compressionConfig;
        private SamplingConfig samplingConfig;
        private EdgeConfig edgeConfig;
        private MLConfig mlConfig;
        private ResourceConfig resourceConfig;
        private LatencyConfig latencyConfig;
        
        // 构造函数和getter/setter方法
        public OptimizationConfig() {
            this.compressionConfig = new CompressionConfig();
            this.samplingConfig = new SamplingConfig();
            this.edgeConfig = new EdgeConfig();
            this.mlConfig = new MLConfig();
            this.resourceConfig = new ResourceConfig();
            this.latencyConfig = new LatencyConfig();
        }
        
        public CompressionConfig getCompressionConfig() { return compressionConfig; }
        public SamplingConfig getSamplingConfig() { return samplingConfig; }
        public EdgeConfig getEdgeConfig() { return edgeConfig; }
        public MLConfig getMlConfig() { return mlConfig; }
        public ResourceConfig getResourceConfig() { return resourceConfig; }
        public LatencyConfig getLatencyConfig() { return latencyConfig; }
    }
    
    /**
     * 优化指标类
     */
    public static class OptimizationMetrics {
        private long processedSamples = 0;
        private long skippedSamples = 0;
        private double averageProcessingTime = 0.0;
        private double throughput = 0.0;
        private double compressionRatio = 1.0;
        private long lastUpdateTime = System.currentTimeMillis();
        
        public void incrementSkippedSamples() {
            skippedSamples++;
        }
        
        public void updateProcessingTime(long time) {
            processedSamples++;
            averageProcessingTime = (averageProcessingTime * (processedSamples - 1) + time) / processedSamples;
        }
        
        public void updateThroughput() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastUpdateTime;
            if (timeDiff > 0) {
                throughput = (processedSamples * 1000.0) / timeDiff;
            }
            lastUpdateTime = currentTime;
        }
        
        // Getter方法
        public long getProcessedSamples() { return processedSamples; }
        public long getSkippedSamples() { return skippedSamples; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public double getThroughput() { return throughput; }
        public double getCompressionRatio() { return compressionRatio; }
        
        public void setCompressionRatio(double ratio) { this.compressionRatio = ratio; }
    }
    
    /**
     * 自适应参数类
     */
    public static class AdaptiveParameters {
        private double samplingRate = 1.0;
        private double compressionLevel = 0.5;
        private int bufferSize = 1024;
        private double qualityThreshold = 0.7;
        private Map<String, Double> algorithmWeights = new HashMap<>();
        
        public AdaptiveParameters() {
            // 初始化默认算法权重
            algorithmWeights.put("compression", 1.0);
            algorithmWeights.put("sampling", 1.0);
            algorithmWeights.put("edge", 1.0);
            algorithmWeights.put("ml", 1.0);
        }
        
        // Getter和Setter方法
        public double getSamplingRate() { return samplingRate; }
        public void setSamplingRate(double samplingRate) { this.samplingRate = samplingRate; }
        
        public double getCompressionLevel() { return compressionLevel; }
        public void setCompressionLevel(double compressionLevel) { this.compressionLevel = compressionLevel; }
        
        public int getBufferSize() { return bufferSize; }
        public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
        
        public double getQualityThreshold() { return qualityThreshold; }
        public void setQualityThreshold(double qualityThreshold) { this.qualityThreshold = qualityThreshold; }
        
        public Map<String, Double> getAlgorithmWeights() { return algorithmWeights; }
        public void setAlgorithmWeights(Map<String, Double> algorithmWeights) { this.algorithmWeights = algorithmWeights; }
    }
    
    /**
     * 性能指标类
     */
    public static class PerformanceMetrics {
        private double averageProcessingTime = 0.0;
        private double throughput = 0.0;
        private double compressionRatio = 1.0;
        private long sampleCount = 0;
        
        public void addProcessingTime(long time) {
            sampleCount++;
            averageProcessingTime = (averageProcessingTime * (sampleCount - 1) + time) / sampleCount;
        }
        
        public void updateThroughput(double newThroughput) {
            this.throughput = newThroughput;
        }
        
        public void updateCompressionRatio(double newRatio) {
            this.compressionRatio = newRatio;
        }
        
        // Getter方法
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public double getThroughput() { return throughput; }
        public double getCompressionRatio() { return compressionRatio; }
        public long getSampleCount() { return sampleCount; }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{avgTime=%.2fms, throughput=%.2f/s, compression=%.2fx, samples=%d}",
                averageProcessingTime, throughput, compressionRatio, sampleCount);
        }
    }
}
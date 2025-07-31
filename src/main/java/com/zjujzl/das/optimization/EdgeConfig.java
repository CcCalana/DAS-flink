package com.zjujzl.das.optimization;

/**
 * 边缘计算配置类
 * 
 * 定义边缘计算环境的配置参数
 */
public class EdgeConfig {
    
    // 节点配置
    private int maxEdgeNodes = 8;
    private double nodeCpuCores = 4.0;
    private double nodeMemoryGB = 8.0;
    private double nodeBandwidthMbps = 1000.0;
    
    // 资源分配配置
    private double maxResourceUtilization = 0.8;
    private double minResourceReserve = 0.2;
    private double allocationTimeout = 30.0; // 秒
    
    // 通信配置
    private double maxLatencyMs = 50.0;
    private int maxRetries = 3;
    private double communicationTimeout = 10.0; // 秒
    
    // 负载均衡配置
    private double loadBalanceThreshold = 0.7;
    private double rebalanceInterval = 60.0; // 秒
    
    // 故障恢复配置
    private double healthCheckInterval = 30.0; // 秒
    private int maxFailureCount = 3;
    private double recoveryTimeout = 120.0; // 秒
    
    public EdgeConfig() {
        // 默认构造函数
    }
    
    public EdgeConfig(int maxEdgeNodes, double nodeCpuCores, double nodeMemoryGB, 
                     double nodeBandwidthMbps, double maxLatencyMs) {
        this.maxEdgeNodes = maxEdgeNodes;
        this.nodeCpuCores = nodeCpuCores;
        this.nodeMemoryGB = nodeMemoryGB;
        this.nodeBandwidthMbps = nodeBandwidthMbps;
        this.maxLatencyMs = maxLatencyMs;
    }
    
    // 预设配置
    
    public static EdgeConfig createHighPerformanceConfig() {
        EdgeConfig config = new EdgeConfig();
        config.maxEdgeNodes = 16;
        config.nodeCpuCores = 8.0;
        config.nodeMemoryGB = 16.0;
        config.nodeBandwidthMbps = 10000.0;
        config.maxLatencyMs = 20.0;
        config.maxResourceUtilization = 0.9;
        return config;
    }
    
    public static EdgeConfig createBalancedConfig() {
        EdgeConfig config = new EdgeConfig();
        config.maxEdgeNodes = 8;
        config.nodeCpuCores = 4.0;
        config.nodeMemoryGB = 8.0;
        config.nodeBandwidthMbps = 1000.0;
        config.maxLatencyMs = 50.0;
        config.maxResourceUtilization = 0.8;
        return config;
    }
    
    public static EdgeConfig createResourceConstrainedConfig() {
        EdgeConfig config = new EdgeConfig();
        config.maxEdgeNodes = 4;
        config.nodeCpuCores = 2.0;
        config.nodeMemoryGB = 4.0;
        config.nodeBandwidthMbps = 500.0;
        config.maxLatencyMs = 100.0;
        config.maxResourceUtilization = 0.7;
        return config;
    }
    
    // 验证配置
    
    public boolean isValid() {
        return maxEdgeNodes > 0 &&
               nodeCpuCores > 0 &&
               nodeMemoryGB > 0 &&
               nodeBandwidthMbps > 0 &&
               maxLatencyMs > 0 &&
               maxResourceUtilization > 0 && maxResourceUtilization <= 1.0 &&
               minResourceReserve >= 0 && minResourceReserve < maxResourceUtilization;
    }
    
    // Getters and Setters
    
    public int getMaxEdgeNodes() { return maxEdgeNodes; }
    public void setMaxEdgeNodes(int maxEdgeNodes) { this.maxEdgeNodes = maxEdgeNodes; }
    
    public double getNodeCpuCores() { return nodeCpuCores; }
    public void setNodeCpuCores(double nodeCpuCores) { this.nodeCpuCores = nodeCpuCores; }
    
    public double getNodeMemoryGB() { return nodeMemoryGB; }
    public void setNodeMemoryGB(double nodeMemoryGB) { this.nodeMemoryGB = nodeMemoryGB; }
    
    public double getNodeBandwidthMbps() { return nodeBandwidthMbps; }
    public void setNodeBandwidthMbps(double nodeBandwidthMbps) { this.nodeBandwidthMbps = nodeBandwidthMbps; }
    
    public double getMaxResourceUtilization() { return maxResourceUtilization; }
    public void setMaxResourceUtilization(double maxResourceUtilization) { this.maxResourceUtilization = maxResourceUtilization; }
    
    public double getMinResourceReserve() { return minResourceReserve; }
    public void setMinResourceReserve(double minResourceReserve) { this.minResourceReserve = minResourceReserve; }
    
    public double getAllocationTimeout() { return allocationTimeout; }
    public void setAllocationTimeout(double allocationTimeout) { this.allocationTimeout = allocationTimeout; }
    
    public double getMaxLatencyMs() { return maxLatencyMs; }
    public void setMaxLatencyMs(double maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public double getCommunicationTimeout() { return communicationTimeout; }
    public void setCommunicationTimeout(double communicationTimeout) { this.communicationTimeout = communicationTimeout; }
    
    public double getLoadBalanceThreshold() { return loadBalanceThreshold; }
    public void setLoadBalanceThreshold(double loadBalanceThreshold) { this.loadBalanceThreshold = loadBalanceThreshold; }
    
    public double getRebalanceInterval() { return rebalanceInterval; }
    public void setRebalanceInterval(double rebalanceInterval) { this.rebalanceInterval = rebalanceInterval; }
    
    public double getHealthCheckInterval() { return healthCheckInterval; }
    public void setHealthCheckInterval(double healthCheckInterval) { this.healthCheckInterval = healthCheckInterval; }
    
    public int getMaxFailureCount() { return maxFailureCount; }
    public void setMaxFailureCount(int maxFailureCount) { this.maxFailureCount = maxFailureCount; }
    
    public double getRecoveryTimeout() { return recoveryTimeout; }
    public void setRecoveryTimeout(double recoveryTimeout) { this.recoveryTimeout = recoveryTimeout; }
    
    @Override
    public String toString() {
        return String.format(
            "EdgeConfig{maxNodes=%d, cpu=%.1f, memory=%.1fGB, bandwidth=%.0fMbps, latency=%.1fms}",
            maxEdgeNodes, nodeCpuCores, nodeMemoryGB, nodeBandwidthMbps, maxLatencyMs
        );
    }
}
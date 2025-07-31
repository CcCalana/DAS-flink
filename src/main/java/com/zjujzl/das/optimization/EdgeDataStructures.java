package com.zjujzl.das.optimization;

import java.util.*;

/**
 * 边缘计算相关数据结构
 * 
 * 包含资源分配、数据分片、通信拓扑等核心数据结构
 */
public class EdgeDataStructures {
    
    /**
     * 资源需求描述
     */
    public static class ResourceRequirement {
        private final double cpuCores;
        private final double memoryGB;
        private final double bandwidthMbps;
        private final double latencyRequirement;
        private final int channelCount;
        
        public ResourceRequirement(double cpuCores, double memoryGB, double bandwidthMbps, 
                                 double latencyRequirement, int channelCount) {
            this.cpuCores = cpuCores;
            this.memoryGB = memoryGB;
            this.bandwidthMbps = bandwidthMbps;
            this.latencyRequirement = latencyRequirement;
            this.channelCount = channelCount;
        }
        
        public double getCpuCores() { return cpuCores; }
        public double getMemoryGB() { return memoryGB; }
        public double getBandwidthMbps() { return bandwidthMbps; }
        public double getLatencyRequirement() { return latencyRequirement; }
        public int getChannelCount() { return channelCount; }
        
        @Override
        public String toString() {
            return String.format("ResourceRequirement{cpu=%.2f, memory=%.2fGB, bandwidth=%.2fMbps, latency=%.2fms, channels=%d}",
                cpuCores, memoryGB, bandwidthMbps, latencyRequirement, channelCount);
        }
    }
    
    /**
     * 已分配资源
     */
    public static class AllocatedResource {
        private final String nodeId;
        private final double cpuCores;
        private final double memoryGB;
        private final double bandwidthMbps;
        private final long allocationTime;
        
        public AllocatedResource(String nodeId, double cpuCores, double memoryGB, double bandwidthMbps) {
            this.nodeId = nodeId;
            this.cpuCores = cpuCores;
            this.memoryGB = memoryGB;
            this.bandwidthMbps = bandwidthMbps;
            this.allocationTime = System.currentTimeMillis();
        }
        
        public String getNodeId() { return nodeId; }
        public double getCpuCores() { return cpuCores; }
        public double getMemoryGB() { return memoryGB; }
        public double getBandwidthMbps() { return bandwidthMbps; }
        public long getAllocationTime() { return allocationTime; }
        
        public double getTotalResourceScore() {
            return cpuCores + memoryGB + bandwidthMbps / 100.0;
        }
        
        @Override
        public String toString() {
            return String.format("AllocatedResource{node=%s, cpu=%.2f, memory=%.2fGB, bandwidth=%.2fMbps}",
                nodeId, cpuCores, memoryGB, bandwidthMbps);
        }
    }
    
    /**
     * 数据分片策略
     */
    public static class DataShardingStrategy {
        private final ShardingMethod method;
        private final Map<String, List<Integer>> channelAssignment;
        private final double shardingOverhead;
        
        public DataShardingStrategy(ShardingMethod method, Map<String, List<Integer>> channelAssignment, 
                                  double shardingOverhead) {
            this.method = method;
            this.channelAssignment = new HashMap<>(channelAssignment);
            this.shardingOverhead = shardingOverhead;
        }
        
        public ShardingMethod getMethod() { return method; }
        public Map<String, List<Integer>> getChannelAssignment() { return new HashMap<>(channelAssignment); }
        public double getShardingOverhead() { return shardingOverhead; }
        
        public int getTotalChannels() {
            return channelAssignment.values().stream()
                .mapToInt(List::size)
                .sum();
        }
        
        public List<String> getAssignedNodes() {
            return new ArrayList<>(channelAssignment.keySet());
        }
        
        @Override
        public String toString() {
            return String.format("DataShardingStrategy{method=%s, nodes=%d, totalChannels=%d, overhead=%.3f}",
                method, channelAssignment.size(), getTotalChannels(), shardingOverhead);
        }
    }
    
    /**
     * 通信拓扑
     */
    public static class CommunicationTopology {
        private final TopologyType type;
        private final Map<String, List<String>> connections;
        private final double communicationLatency;
        
        public CommunicationTopology(TopologyType type, Map<String, List<String>> connections, 
                                   double communicationLatency) {
            this.type = type;
            this.connections = new HashMap<>(connections);
            this.communicationLatency = communicationLatency;
        }
        
        public TopologyType getType() { return type; }
        public Map<String, List<String>> getConnections() { return new HashMap<>(connections); }
        public double getCommunicationLatency() { return communicationLatency; }
        
        public int getNodeCount() {
            Set<String> allNodes = new HashSet<>(connections.keySet());
            connections.values().forEach(allNodes::addAll);
            return allNodes.size();
        }
        
        public List<String> getNeighbors(String nodeId) {
            return connections.getOrDefault(nodeId, new ArrayList<>());
        }
        
        @Override
        public String toString() {
            return String.format("CommunicationTopology{type=%s, nodes=%d, latency=%.2fms}",
                type, getNodeCount(), communicationLatency);
        }
    }
    
    /**
     * 边缘资源分配结果
     */
    public static class EdgeResourceAllocation {
        private final Map<String, AllocatedResource> allocatedResources;
        private final DataShardingStrategy shardingStrategy;
        private final CommunicationTopology topology;
        private final double resourceUtilizationFactor;
        private final long allocationTime;
        
        public EdgeResourceAllocation(Map<String, AllocatedResource> allocatedResources,
                                    DataShardingStrategy shardingStrategy,
                                    CommunicationTopology topology,
                                    double resourceUtilizationFactor) {
            this.allocatedResources = new HashMap<>(allocatedResources);
            this.shardingStrategy = shardingStrategy;
            this.topology = topology;
            this.resourceUtilizationFactor = resourceUtilizationFactor;
            this.allocationTime = System.currentTimeMillis();
        }
        
        public Map<String, AllocatedResource> getAllocatedResources() { 
            return new HashMap<>(allocatedResources); 
        }
        
        public DataShardingStrategy getShardingStrategy() { return shardingStrategy; }
        public CommunicationTopology getTopology() { return topology; }
        public double getResourceUtilizationFactor() { return resourceUtilizationFactor; }
        public long getAllocationTime() { return allocationTime; }
        
        public int getAllocatedNodeCount() {
            return allocatedResources.size();
        }
        
        public double getTotalAllocatedCpu() {
            return allocatedResources.values().stream()
                .mapToDouble(AllocatedResource::getCpuCores)
                .sum();
        }
        
        public double getTotalAllocatedMemory() {
            return allocatedResources.values().stream()
                .mapToDouble(AllocatedResource::getMemoryGB)
                .sum();
        }
        
        public double getTotalAllocatedBandwidth() {
            return allocatedResources.values().stream()
                .mapToDouble(AllocatedResource::getBandwidthMbps)
                .sum();
        }
        
        public boolean isValid() {
            return !allocatedResources.isEmpty() && 
                   shardingStrategy != null && 
                   topology != null &&
                   resourceUtilizationFactor >= 0;
        }
        
        @Override
        public String toString() {
            return String.format("EdgeResourceAllocation{nodes=%d, cpu=%.2f, memory=%.2fGB, bandwidth=%.2fMbps, utilization=%.3f}",
                getAllocatedNodeCount(), getTotalAllocatedCpu(), getTotalAllocatedMemory(), 
                getTotalAllocatedBandwidth(), resourceUtilizationFactor);
        }
    }
    
    /**
     * 边缘系统指标
     */
    public static class EdgeSystemMetrics {
        private final int totalNodes;
        private final double averageLatency;
        private final double totalBandwidth;
        private final double resourceUtilization;
        private final double nodeAvailability;
        private final int activeNodes;
        private final long timestamp;
        
        public EdgeSystemMetrics(int totalNodes, double averageLatency, double totalBandwidth,
                               double resourceUtilization, double nodeAvailability, int activeNodes) {
            this.totalNodes = totalNodes;
            this.averageLatency = averageLatency;
            this.totalBandwidth = totalBandwidth;
            this.resourceUtilization = resourceUtilization;
            this.nodeAvailability = nodeAvailability;
            this.activeNodes = activeNodes;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getTotalNodes() { return totalNodes; }
        public double getAverageLatency() { return averageLatency; }
        public double getTotalBandwidth() { return totalBandwidth; }
        public double getResourceUtilization() { return resourceUtilization; }
        public double getNodeAvailability() { return nodeAvailability; }
        public int getActiveNodes() { return activeNodes; }
        public long getTimestamp() { return timestamp; }
        
        public double getSystemHealthScore() {
            double latencyScore = Math.max(0, 1.0 - averageLatency / 100.0); // 100ms为基准
            double utilizationScore = resourceUtilization; // 利用率越高越好
            double availabilityScore = nodeAvailability; // 可用性越高越好
            
            return (latencyScore + utilizationScore + availabilityScore) / 3.0;
        }
        
        @Override
        public String toString() {
            return String.format("EdgeSystemMetrics{nodes=%d/%d, latency=%.2fms, bandwidth=%.2fMbps, utilization=%.3f, availability=%.3f}",
                activeNodes, totalNodes, averageLatency, totalBandwidth, resourceUtilization, nodeAvailability);
        }
    }
    
    /**
     * DAS流数据
     */
    public static class DASStreamData {
        private final int channelCount;
        private final long dataSize; // bytes
        private final double dataRate; // bytes/second
        private final double samplingRate; // Hz
        private final long timestamp;
        
        public DASStreamData(int channelCount, long dataSize, double dataRate, double samplingRate) {
            this.channelCount = channelCount;
            this.dataSize = dataSize;
            this.dataRate = dataRate;
            this.samplingRate = samplingRate;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getChannelCount() { return channelCount; }
        public long getDataSize() { return dataSize; }
        public double getDataRate() { return dataRate; }
        public double getSamplingRate() { return samplingRate; }
        public long getTimestamp() { return timestamp; }
        
        public double getDataSizeGB() {
            return dataSize / (1024.0 * 1024.0 * 1024.0);
        }
        
        public double getDataRateMbps() {
            return dataRate * 8.0 / (1024.0 * 1024.0);
        }
        
        @Override
        public String toString() {
            return String.format("DASStreamData{channels=%d, size=%.2fGB, rate=%.2fMbps, sampling=%.1fHz}",
                channelCount, getDataSizeGB(), getDataRateMbps(), samplingRate);
        }
    }
    
    /**
     * 优化策略
     */
    public static class OptimizationStrategy {
        private final String name;
        private final double complexityFactor;
        private final double latencyRequirement;
        private final double qualityRequirement;
        private final boolean enableAdaptive;
        
        public OptimizationStrategy(String name, double complexityFactor, double latencyRequirement,
                                  double qualityRequirement, boolean enableAdaptive) {
            this.name = name;
            this.complexityFactor = complexityFactor;
            this.latencyRequirement = latencyRequirement;
            this.qualityRequirement = qualityRequirement;
            this.enableAdaptive = enableAdaptive;
        }
        
        public String getName() { return name; }
        public double getComplexityFactor() { return complexityFactor; }
        public double getLatencyRequirement() { return latencyRequirement; }
        public double getQualityRequirement() { return qualityRequirement; }
        public boolean isEnableAdaptive() { return enableAdaptive; }
        
        // 预设策略
        public static OptimizationStrategy createHighQualityStrategy() {
            return new OptimizationStrategy("HighQuality", 2.0, 20.0, 0.95, true);
        }
        
        public static OptimizationStrategy createBalancedStrategy() {
            return new OptimizationStrategy("Balanced", 1.0, 50.0, 0.8, true);
        }
        
        public static OptimizationStrategy createLowLatencyStrategy() {
            return new OptimizationStrategy("LowLatency", 0.5, 10.0, 0.7, false);
        }
        
        @Override
        public String toString() {
            return String.format("OptimizationStrategy{name=%s, complexity=%.2f, latency=%.1fms, quality=%.3f, adaptive=%s}",
                name, complexityFactor, latencyRequirement, qualityRequirement, enableAdaptive);
        }
    }
    
    // 枚举类型
    
    /**
     * 数据分片方法
     */
    public enum ShardingMethod {
        NONE("无分片"),
        CHANNEL_BASED("基于通道分片"),
        TIME_BASED("基于时间分片"),
        SPATIAL_BASED("基于空间分片"),
        HYBRID("混合分片");
        
        private final String description;
        
        ShardingMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * 通信拓扑类型
     */
    public enum TopologyType {
        SINGLE("单节点"),
        STAR("星型"),
        MESH("网状"),
        RING("环型"),
        TREE("树型");
        
        private final String description;
        
        TopologyType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
}
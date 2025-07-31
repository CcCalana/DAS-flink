package com.zjujzl.das.optimization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 边缘计算管理器
 * 
 * 管理边缘计算资源分配和优化，支持分布式DAS数据处理
 * 参考文献：
 * - IDC Edge Computing Market Report 2024: $378 billion market by 2028
 * - BigDataWire 2025: Stream processing at the edge challenges
 * - TechTarget 2025: Edge computing trends and IoT device growth
 */
public class EdgeComputingManager {
    
    private final EdgeConfig config;
    
    // 边缘节点管理
    private final Map<String, EdgeNode> edgeNodes;
    private final Map<String, EdgeResourceMetrics> nodeMetrics;
    
    // 资源分配策略
    private final ResourceAllocationStrategy allocationStrategy;
    private final LoadDistributor loadDistributor;
    
    // 性能监控
    private final AtomicLong totalAllocations;
    private final AtomicLong failedAllocations;
    private volatile double overallResourceUtilization = 0.0;
    
    public EdgeComputingManager(EdgeConfig config) {
        this.config = config;
        this.edgeNodes = new ConcurrentHashMap<>();
        this.nodeMetrics = new ConcurrentHashMap<>();
        this.allocationStrategy = new ResourceAllocationStrategy(config);
        this.loadDistributor = new LoadDistributor();
        this.totalAllocations = new AtomicLong(0);
        this.failedAllocations = new AtomicLong(0);
        
        initializeEdgeNodes();
    }
    
    /**
     * 分配边缘计算资源
     */
    public EdgeResourceAllocation allocateResources(DASStreamData streamData, 
                                                   OptimizationStrategy strategy) {
        totalAllocations.incrementAndGet();
        
        try {
            // 分析资源需求
            ResourceRequirement requirement = analyzeResourceRequirement(streamData, strategy);
            
            // 选择最优边缘节点
            List<EdgeNode> selectedNodes = selectOptimalNodes(requirement);
            
            // 分配计算资源
            Map<String, AllocatedResource> allocatedResources = allocateComputeResources(selectedNodes, requirement);
            
            // 配置数据分片策略
            DataShardingStrategy shardingStrategy = configureDataSharding(streamData, selectedNodes);
            
            // 设置通信拓扑
            CommunicationTopology topology = setupCommunicationTopology(selectedNodes);
            
            return new EdgeResourceAllocation(
                allocatedResources,
                shardingStrategy,
                topology,
                calculateResourceUtilizationFactor(allocatedResources)
            );
            
        } catch (Exception e) {
            failedAllocations.incrementAndGet();
            return createFallbackAllocation(streamData);
        }
    }
    
    /**
     * 更新资源分配
     */
    public void updateResourceAllocation(PerformanceMetrics metrics) {
        // 基于性能指标调整资源分配策略
        if (metrics.getEfficiency() < 0.6) {
            // 效率低，增加资源分配
            allocationStrategy.increaseResourceAllocation();
        } else if (metrics.getEfficiency() > 0.9 && overallResourceUtilization < 0.5) {
            // 效率高且资源利用率低，可以减少分配
            allocationStrategy.optimizeResourceAllocation();
        }
        
        // 更新节点性能指标
        updateNodeMetrics(metrics);
    }
    
    /**
     * 获取系统指标
     */
    public EdgeSystemMetrics getSystemMetrics() {
        return new EdgeSystemMetrics(
            edgeNodes.size(),
            calculateAverageLatency(),
            calculateTotalBandwidth(),
            overallResourceUtilization,
            calculateNodeAvailability(),
            getActiveNodeCount()
        );
    }
    
    /**
     * 获取资源利用率
     */
    public double getResourceUtilization() {
        return overallResourceUtilization;
    }
    
    // 私有方法
    
    private void initializeEdgeNodes() {
        // 初始化边缘节点配置
        for (int i = 0; i < config.getMaxEdgeNodes(); i++) {
            String nodeId = "edge-node-" + i;
            EdgeNode node = new EdgeNode(
                nodeId,
                config.getNodeCpuCores(),
                config.getNodeMemoryGB(),
                config.getNodeBandwidthMbps(),
                calculateNodeLatency(i)
            );
            
            edgeNodes.put(nodeId, node);
            nodeMetrics.put(nodeId, new EdgeResourceMetrics(nodeId));
        }
    }
    
    private ResourceRequirement analyzeResourceRequirement(DASStreamData streamData, 
                                                          OptimizationStrategy strategy) {
        // 计算CPU需求
        double cpuRequirement = estimateCpuRequirement(streamData, strategy);
        
        // 计算内存需求
        double memoryRequirement = estimateMemoryRequirement(streamData, strategy);
        
        // 计算带宽需求
        double bandwidthRequirement = estimateBandwidthRequirement(streamData, strategy);
        
        // 计算延迟要求
        double latencyRequirement = strategy.getLatencyRequirement();
        
        return new ResourceRequirement(
            cpuRequirement,
            memoryRequirement,
            bandwidthRequirement,
            latencyRequirement,
            streamData.getChannelCount()
        );
    }
    
    private List<EdgeNode> selectOptimalNodes(ResourceRequirement requirement) {
        List<EdgeNode> availableNodes = edgeNodes.values().stream()
            .filter(node -> node.canSatisfyRequirement(requirement))
            .sorted((n1, n2) -> Double.compare(n1.getScore(requirement), n2.getScore(requirement)))
            .collect(Collectors.toList());
        
        // 选择最优的节点组合
        int requiredNodes = Math.min(
            requirement.getChannelCount(),
            Math.max(1, availableNodes.size() / 2)
        );
        
        return availableNodes.subList(0, Math.min(requiredNodes, availableNodes.size()));
    }
    
    private Map<String, AllocatedResource> allocateComputeResources(List<EdgeNode> nodes, 
                                                                   ResourceRequirement requirement) {
        Map<String, AllocatedResource> allocations = new HashMap<>();
        
        double cpuPerNode = requirement.getCpuCores() / nodes.size();
        double memoryPerNode = requirement.getMemoryGB() / nodes.size();
        double bandwidthPerNode = requirement.getBandwidthMbps() / nodes.size();
        
        for (EdgeNode node : nodes) {
            AllocatedResource resource = new AllocatedResource(
                node.getNodeId(),
                Math.min(cpuPerNode, node.getAvailableCpu()),
                Math.min(memoryPerNode, node.getAvailableMemory()),
                Math.min(bandwidthPerNode, node.getAvailableBandwidth())
            );
            
            // 更新节点资源状态
            node.allocateResources(resource);
            allocations.put(node.getNodeId(), resource);
        }
        
        return allocations;
    }
    
    private DataShardingStrategy configureDataSharding(DASStreamData streamData, List<EdgeNode> nodes) {
        int totalChannels = streamData.getChannelCount();
        int nodeCount = nodes.size();
        
        // 计算每个节点处理的通道数
        Map<String, List<Integer>> channelAssignment = new HashMap<>();
        
        for (int i = 0; i < nodeCount; i++) {
            String nodeId = nodes.get(i).getNodeId();
            List<Integer> channels = new ArrayList<>();
            
            int channelsPerNode = totalChannels / nodeCount;
            int startChannel = i * channelsPerNode;
            int endChannel = (i == nodeCount - 1) ? totalChannels : (i + 1) * channelsPerNode;
            
            for (int ch = startChannel; ch < endChannel; ch++) {
                channels.add(ch);
            }
            
            channelAssignment.put(nodeId, channels);
        }
        
        return new DataShardingStrategy(
            ShardingMethod.CHANNEL_BASED,
            channelAssignment,
            calculateShardingOverhead(channelAssignment)
        );
    }
    
    private CommunicationTopology setupCommunicationTopology(List<EdgeNode> nodes) {
        // 创建星型拓扑（简化实现）
        Map<String, List<String>> connections = new HashMap<>();
        
        if (nodes.size() > 1) {
            String masterNode = nodes.get(0).getNodeId();
            List<String> masterConnections = new ArrayList<>();
            
            for (int i = 1; i < nodes.size(); i++) {
                String slaveNode = nodes.get(i).getNodeId();
                masterConnections.add(slaveNode);
                connections.put(slaveNode, Arrays.asList(masterNode));
            }
            
            connections.put(masterNode, masterConnections);
        }
        
        return new CommunicationTopology(
            TopologyType.STAR,
            connections,
            calculateCommunicationLatency(nodes)
        );
    }
    
    private double calculateResourceUtilizationFactor(Map<String, AllocatedResource> allocations) {
        if (allocations.isEmpty()) return 0.0;
        
        double totalUtilization = allocations.values().stream()
            .mapToDouble(this::calculateResourceUtilization)
            .average()
            .orElse(0.0);
        
        this.overallResourceUtilization = totalUtilization;
        return totalUtilization;
    }
    
    private double calculateResourceUtilization(AllocatedResource resource) {
        EdgeNode node = edgeNodes.get(resource.getNodeId());
        if (node == null) return 0.0;
        
        double cpuUtil = resource.getCpuCores() / node.getTotalCpu();
        double memUtil = resource.getMemoryGB() / node.getTotalMemory();
        double bandUtil = resource.getBandwidthMbps() / node.getTotalBandwidth();
        
        return (cpuUtil + memUtil + bandUtil) / 3.0;
    }
    
    private EdgeResourceAllocation createFallbackAllocation(DASStreamData streamData) {
        // 创建降级资源分配
        Map<String, AllocatedResource> fallbackResources = new HashMap<>();
        
        if (!edgeNodes.isEmpty()) {
            EdgeNode firstNode = edgeNodes.values().iterator().next();
            fallbackResources.put(firstNode.getNodeId(), new AllocatedResource(
                firstNode.getNodeId(), 1.0, 2.0, 100.0
            ));
        }
        
        return new EdgeResourceAllocation(
            fallbackResources,
            new DataShardingStrategy(ShardingMethod.NONE, new HashMap<>(), 0.0),
            new CommunicationTopology(TopologyType.SINGLE, new HashMap<>(), 0.0),
            0.1
        );
    }
    
    private void updateNodeMetrics(PerformanceMetrics metrics) {
        // 更新所有节点的性能指标
        nodeMetrics.values().forEach(nodeMetric -> 
            nodeMetric.updateMetrics(metrics.getThroughput(), metrics.getQualityScore())
        );
    }
    
    // 估算方法
    
    private double estimateCpuRequirement(DASStreamData streamData, OptimizationStrategy strategy) {
        // 基于数据量和处理复杂度估算CPU需求
        double baseRequirement = streamData.getChannelCount() * 0.1; // 每通道0.1核
        double complexityFactor = strategy.getComplexityFactor();
        
        return baseRequirement * complexityFactor;
    }
    
    private double estimateMemoryRequirement(DASStreamData streamData, OptimizationStrategy strategy) {
        // 基于数据大小估算内存需求
        double dataSizeGB = streamData.getDataSize() / (1024.0 * 1024.0 * 1024.0);
        double bufferFactor = 2.0; // 缓冲因子
        
        return dataSizeGB * bufferFactor;
    }
    
    private double estimateBandwidthRequirement(DASStreamData streamData, OptimizationStrategy strategy) {
        // 基于数据传输需求估算带宽
        double dataRateMbps = streamData.getDataRate() * 8.0 / (1024.0 * 1024.0); // 转换为Mbps
        double replicationFactor = 1.5; // 考虑数据复制
        
        return dataRateMbps * replicationFactor;
    }
    
    private double calculateNodeLatency(int nodeIndex) {
        // 模拟不同节点的网络延迟
        return 5.0 + nodeIndex * 2.0; // 5-15ms范围
    }
    
    private double calculateAverageLatency() {
        return edgeNodes.values().stream()
            .mapToDouble(EdgeNode::getLatency)
            .average()
            .orElse(0.0);
    }
    
    private double calculateTotalBandwidth() {
        return edgeNodes.values().stream()
            .mapToDouble(EdgeNode::getTotalBandwidth)
            .sum();
    }
    
    private double calculateNodeAvailability() {
        long availableNodes = edgeNodes.values().stream()
            .mapToLong(node -> node.isAvailable() ? 1 : 0)
            .sum();
        
        return edgeNodes.isEmpty() ? 0.0 : (double) availableNodes / edgeNodes.size();
    }
    
    private int getActiveNodeCount() {
        return (int) edgeNodes.values().stream()
            .mapToLong(node -> node.isActive() ? 1 : 0)
            .sum();
    }
    
    private double calculateShardingOverhead(Map<String, List<Integer>> channelAssignment) {
        // 计算数据分片的开销
        int totalShards = channelAssignment.size();
        return totalShards > 1 ? 0.1 * Math.log(totalShards) : 0.0;
    }
    
    private double calculateCommunicationLatency(List<EdgeNode> nodes) {
        if (nodes.size() <= 1) return 0.0;
        
        // 计算节点间通信延迟
        return nodes.stream()
            .mapToDouble(EdgeNode::getLatency)
            .max()
            .orElse(0.0);
    }
    
    // 内部类
    
    public static class EdgeNode {
        private final String nodeId;
        private final double totalCpu;
        private final double totalMemory;
        private final double totalBandwidth;
        private final double latency;
        
        private volatile double availableCpu;
        private volatile double availableMemory;
        private volatile double availableBandwidth;
        private volatile boolean available = true;
        private volatile boolean active = true;
        
        public EdgeNode(String nodeId, double totalCpu, double totalMemory, 
                       double totalBandwidth, double latency) {
            this.nodeId = nodeId;
            this.totalCpu = totalCpu;
            this.totalMemory = totalMemory;
            this.totalBandwidth = totalBandwidth;
            this.latency = latency;
            
            this.availableCpu = totalCpu;
            this.availableMemory = totalMemory;
            this.availableBandwidth = totalBandwidth;
        }
        
        public boolean canSatisfyRequirement(ResourceRequirement requirement) {
            return availableCpu >= requirement.getCpuCores() / 4 && // 允许部分分配
                   availableMemory >= requirement.getMemoryGB() / 4 &&
                   availableBandwidth >= requirement.getBandwidthMbps() / 4 &&
                   latency <= requirement.getLatencyRequirement() * 2;
        }
        
        public double getScore(ResourceRequirement requirement) {
            // 计算节点适合度评分（越低越好）
            double cpuScore = Math.abs(availableCpu - requirement.getCpuCores());
            double memoryScore = Math.abs(availableMemory - requirement.getMemoryGB());
            double latencyScore = latency / requirement.getLatencyRequirement();
            
            return cpuScore + memoryScore + latencyScore;
        }
        
        public void allocateResources(AllocatedResource resource) {
            availableCpu -= resource.getCpuCores();
            availableMemory -= resource.getMemoryGB();
            availableBandwidth -= resource.getBandwidthMbps();
        }
        
        public void releaseResources(AllocatedResource resource) {
            availableCpu = Math.min(totalCpu, availableCpu + resource.getCpuCores());
            availableMemory = Math.min(totalMemory, availableMemory + resource.getMemoryGB());
            availableBandwidth = Math.min(totalBandwidth, availableBandwidth + resource.getBandwidthMbps());
        }
        
        // Getters
        public String getNodeId() { return nodeId; }
        public double getTotalCpu() { return totalCpu; }
        public double getTotalMemory() { return totalMemory; }
        public double getTotalBandwidth() { return totalBandwidth; }
        public double getLatency() { return latency; }
        public double getAvailableCpu() { return availableCpu; }
        public double getAvailableMemory() { return availableMemory; }
        public double getAvailableBandwidth() { return availableBandwidth; }
        public boolean isAvailable() { return available; }
        public boolean isActive() { return active; }
    }
    
    public static class EdgeResourceMetrics {
        private final String nodeId;
        private volatile double averageThroughput = 0.0;
        private volatile double averageQuality = 0.0;
        private volatile long measurementCount = 0;
        
        public EdgeResourceMetrics(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public void updateMetrics(double throughput, double quality) {
            measurementCount++;
            averageThroughput = (averageThroughput * (measurementCount - 1) + throughput) / measurementCount;
            averageQuality = (averageQuality * (measurementCount - 1) + quality) / measurementCount;
        }
        
        public String getNodeId() { return nodeId; }
        public double getAverageThroughput() { return averageThroughput; }
        public double getAverageQuality() { return averageQuality; }
        public long getMeasurementCount() { return measurementCount; }
    }
    
    private static class ResourceAllocationStrategy {
        private final EdgeConfig config;
        private volatile double allocationFactor = 1.0;
        
        public ResourceAllocationStrategy(EdgeConfig config) {
            this.config = config;
        }
        
        public void increaseResourceAllocation() {
            allocationFactor = Math.min(2.0, allocationFactor * 1.1);
        }
        
        public void optimizeResourceAllocation() {
            allocationFactor = Math.max(0.5, allocationFactor * 0.95);
        }
        
        public double getAllocationFactor() {
            return allocationFactor;
        }
    }
    
    private static class LoadDistributor {
        
        public void distributeLoad(List<EdgeNode> nodes, ResourceRequirement requirement) {
            // 负载分发逻辑
            // 简化实现
        }
    }
}
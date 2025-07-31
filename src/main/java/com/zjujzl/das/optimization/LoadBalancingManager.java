package com.zjujzl.das.optimization;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 负载均衡管理器
 * 
 * 提供DAS数据流的智能负载均衡、资源调度和性能优化功能
 */
public class LoadBalancingManager {
    
    private final LoadBalancingConfig config;
    private final ResourceMonitor resourceMonitor;
    private final LoadDistributor loadDistributor;
    private final PerformanceOptimizer performanceOptimizer;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ProcessingNode> processingNodes;
    private final Map<String, LoadMetrics> nodeMetrics;
    
    public LoadBalancingManager(LoadBalancingConfig config) {
        this.config = config;
        this.resourceMonitor = new ResourceMonitor();
        this.loadDistributor = new LoadDistributor(config.getDistributionStrategy());
        this.performanceOptimizer = new PerformanceOptimizer();
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.processingNodes = new ConcurrentHashMap<>();
        this.nodeMetrics = new ConcurrentHashMap<>();
        
        initializeLoadBalancing();
    }
    
    /**
     * 分配DAS数据流到处理节点
     */
    public LoadBalancingResult distributeLoad(EdgeDataStructures.DASStreamData streamData,
                                            MLAssistedProcessor.ProcessingStrategy strategy) {
        long startTime = System.currentTimeMillis();
        
        // 评估当前负载状态
        LoadAssessment assessment = assessCurrentLoad();
        
        // 选择最优处理节点
        List<ProcessingNode> selectedNodes = selectOptimalNodes(streamData, strategy, assessment);
        
        // 分配数据流
        Map<String, DataAllocation> allocations = allocateDataStreams(streamData, selectedNodes);
        
        // 更新负载指标
        updateLoadMetrics(allocations);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        return new LoadBalancingResult(
            allocations,
            selectedNodes,
            assessment,
            processingTime
        );
    }
    
    /**
     * 动态负载重平衡
     */
    public RebalancingResult performLoadRebalancing() {
        LoadAssessment currentLoad = assessCurrentLoad();
        
        if (!needsRebalancing(currentLoad)) {
            return RebalancingResult.noRebalancingNeeded();
        }
        
        // 识别过载和空闲节点
        List<ProcessingNode> overloadedNodes = identifyOverloadedNodes(currentLoad);
        List<ProcessingNode> underutilizedNodes = identifyUnderutilizedNodes(currentLoad);
        
        // 生成重平衡策略
        RebalancingStrategy strategy = generateRebalancingStrategy(overloadedNodes, underutilizedNodes);
        
        // 执行负载迁移
        List<LoadMigration> migrations = executeLoadMigration(strategy);
        
        return new RebalancingResult(strategy, migrations, true);
    }
    
    /**
     * 自适应性能优化
     */
    public PerformanceOptimizationResult optimizePerformance(LoadBalancingResult loadResult) {
        return performanceOptimizer.optimize(loadResult, nodeMetrics);
    }
    
    /**
     * 获取负载均衡状态
     */
    public LoadBalancingStatus getLoadBalancingStatus() {
        LoadAssessment assessment = assessCurrentLoad();
        Map<String, NodeStatus> nodeStatuses = getNodeStatuses();
        
        return new LoadBalancingStatus(
            assessment.getOverallLoadLevel(),
            nodeStatuses,
            calculateSystemThroughput(),
            calculateAverageLatency()
        );
    }
    
    // 私有方法实现
    
    private void initializeLoadBalancing() {
        // 启动资源监控
        scheduler.scheduleAtFixedRate(
            resourceMonitor::updateResourceMetrics,
            0,
            config.getMonitoringInterval(),
            TimeUnit.MILLISECONDS
        );
        
        // 启动负载重平衡检查
        scheduler.scheduleAtFixedRate(
            this::checkAndPerformRebalancing,
            config.getRebalancingInterval(),
            config.getRebalancingInterval(),
            TimeUnit.MILLISECONDS
        );
        
        // 初始化处理节点
        initializeProcessingNodes();
    }
    
    private void initializeProcessingNodes() {
        for (int i = 0; i < config.getMaxNodes(); i++) {
            String nodeId = "node-" + i;
            ProcessingNode node = new ProcessingNode(
                nodeId,
                config.getNodeCapacity(),
                NodeType.STANDARD
            );
            processingNodes.put(nodeId, node);
            nodeMetrics.put(nodeId, new LoadMetrics());
        }
    }
    
    private LoadAssessment assessCurrentLoad() {
        double totalLoad = 0.0;
        double maxLoad = 0.0;
        double minLoad = 1.0;
        int activeNodes = 0;
        
        for (ProcessingNode node : processingNodes.values()) {
            if (node.isActive()) {
                double nodeLoad = node.getCurrentLoad();
                totalLoad += nodeLoad;
                maxLoad = Math.max(maxLoad, nodeLoad);
                minLoad = Math.min(minLoad, nodeLoad);
                activeNodes++;
            }
        }
        
        double averageLoad = activeNodes > 0 ? totalLoad / activeNodes : 0.0;
        double loadVariance = calculateLoadVariance(averageLoad);
        
        return new LoadAssessment(
            averageLoad,
            maxLoad,
            minLoad,
            loadVariance,
            activeNodes,
            calculateLoadBalance()
        );
    }
    
    private List<ProcessingNode> selectOptimalNodes(EdgeDataStructures.DASStreamData streamData,
                                                  MLAssistedProcessor.ProcessingStrategy strategy,
                                                  LoadAssessment assessment) {
        // 计算所需资源
        ResourceRequirement requirement = calculateResourceRequirement(streamData, strategy);
        
        // 根据负载均衡策略选择节点
        return processingNodes.values().stream()
            .filter(node -> node.isActive() && node.canHandle(requirement))
            .sorted((n1, n2) -> compareNodeSuitability(n1, n2, requirement))
            .limit(config.getMaxNodesPerTask())
            .collect(Collectors.toList());
    }
    
    private Map<String, DataAllocation> allocateDataStreams(EdgeDataStructures.DASStreamData streamData,
                                                          List<ProcessingNode> selectedNodes) {
        Map<String, DataAllocation> allocations = new HashMap<>();
        
        if (selectedNodes.isEmpty()) {
            return allocations;
        }
        
        // 根据分配策略分配数据
        switch (config.getAllocationStrategy()) {
            case ROUND_ROBIN:
                allocations = allocateRoundRobin(streamData, selectedNodes);
                break;
            case LOAD_BASED:
                allocations = allocateLoadBased(streamData, selectedNodes);
                break;
            case CAPACITY_BASED:
                allocations = allocateCapacityBased(streamData, selectedNodes);
                break;
            case ADAPTIVE:
                allocations = allocateAdaptive(streamData, selectedNodes);
                break;
        }
        
        return allocations;
    }
    
    private Map<String, DataAllocation> allocateRoundRobin(EdgeDataStructures.DASStreamData streamData,
                                                          List<ProcessingNode> nodes) {
        Map<String, DataAllocation> allocations = new HashMap<>();
        int nodeIndex = 0;
        
        for (int channel = 0; channel < streamData.getChannelCount(); channel++) {
            ProcessingNode node = nodes.get(nodeIndex % nodes.size());
            String allocationId = "allocation-" + channel;
            
            allocations.put(allocationId, new DataAllocation(
                allocationId,
                node.getNodeId(),
                Arrays.asList(channel),
                streamData.getDataSize() / streamData.getChannelCount()
            ));
            
            nodeIndex++;
        }
        
        return allocations;
    }
    
    private Map<String, DataAllocation> allocateLoadBased(EdgeDataStructures.DASStreamData streamData,
                                                         List<ProcessingNode> nodes) {
        Map<String, DataAllocation> allocations = new HashMap<>();
        
        // 按负载排序节点
        List<ProcessingNode> sortedNodes = nodes.stream()
            .sorted(Comparator.comparingDouble(ProcessingNode::getCurrentLoad))
            .collect(Collectors.toList());
        
        int channelsPerNode = streamData.getChannelCount() / sortedNodes.size();
        int remainingChannels = streamData.getChannelCount() % sortedNodes.size();
        
        int channelIndex = 0;
        for (int i = 0; i < sortedNodes.size(); i++) {
            ProcessingNode node = sortedNodes.get(i);
            int channelCount = channelsPerNode + (i < remainingChannels ? 1 : 0);
            
            List<Integer> assignedChannels = new ArrayList<>();
            for (int j = 0; j < channelCount; j++) {
                assignedChannels.add(channelIndex++);
            }
            
            if (!assignedChannels.isEmpty()) {
                String allocationId = "allocation-" + node.getNodeId();
                allocations.put(allocationId, new DataAllocation(
                    allocationId,
                    node.getNodeId(),
                    assignedChannels,
                    (streamData.getDataSize() * channelCount) / streamData.getChannelCount()
                ));
            }
        }
        
        return allocations;
    }
    
    private Map<String, DataAllocation> allocateCapacityBased(EdgeDataStructures.DASStreamData streamData,
                                                             List<ProcessingNode> nodes) {
        Map<String, DataAllocation> allocations = new HashMap<>();
        
        double totalCapacity = nodes.stream()
            .mapToDouble(ProcessingNode::getAvailableCapacity)
            .sum();
        
        if (totalCapacity <= 0) {
            return allocateRoundRobin(streamData, nodes);
        }
        
        int channelIndex = 0;
        for (ProcessingNode node : nodes) {
            double capacityRatio = node.getAvailableCapacity() / totalCapacity;
            int channelCount = (int) Math.round(streamData.getChannelCount() * capacityRatio);
            
            if (channelCount > 0 && channelIndex < streamData.getChannelCount()) {
                List<Integer> assignedChannels = new ArrayList<>();
                for (int i = 0; i < channelCount && channelIndex < streamData.getChannelCount(); i++) {
                    assignedChannels.add(channelIndex++);
                }
                
                String allocationId = "allocation-" + node.getNodeId();
                allocations.put(allocationId, new DataAllocation(
                    allocationId,
                    node.getNodeId(),
                    assignedChannels,
                    (streamData.getDataSize() * assignedChannels.size()) / streamData.getChannelCount()
                ));
            }
        }
        
        return allocations;
    }
    
    private Map<String, DataAllocation> allocateAdaptive(EdgeDataStructures.DASStreamData streamData,
                                                        List<ProcessingNode> nodes) {
        // 自适应分配：结合负载、容量和历史性能
        Map<String, DataAllocation> allocations = new HashMap<>();
        
        // 计算每个节点的适应性评分
        Map<ProcessingNode, Double> nodeScores = new HashMap<>();
        for (ProcessingNode node : nodes) {
            double loadScore = 1.0 - node.getCurrentLoad();
            double capacityScore = node.getAvailableCapacity() / node.getTotalCapacity();
            double performanceScore = getNodePerformanceScore(node);
            
            double adaptiveScore = (loadScore * 0.4 + capacityScore * 0.3 + performanceScore * 0.3);
            nodeScores.put(node, adaptiveScore);
        }
        
        // 按评分分配通道
        double totalScore = nodeScores.values().stream().mapToDouble(Double::doubleValue).sum();
        
        int channelIndex = 0;
        for (Map.Entry<ProcessingNode, Double> entry : nodeScores.entrySet()) {
            ProcessingNode node = entry.getKey();
            double scoreRatio = entry.getValue() / totalScore;
            int channelCount = (int) Math.round(streamData.getChannelCount() * scoreRatio);
            
            if (channelCount > 0 && channelIndex < streamData.getChannelCount()) {
                List<Integer> assignedChannels = new ArrayList<>();
                for (int i = 0; i < channelCount && channelIndex < streamData.getChannelCount(); i++) {
                    assignedChannels.add(channelIndex++);
                }
                
                String allocationId = "allocation-" + node.getNodeId();
                allocations.put(allocationId, new DataAllocation(
                    allocationId,
                    node.getNodeId(),
                    assignedChannels,
                    (streamData.getDataSize() * assignedChannels.size()) / streamData.getChannelCount()
                ));
            }
        }
        
        return allocations;
    }
    
    private void updateLoadMetrics(Map<String, DataAllocation> allocations) {
        for (DataAllocation allocation : allocations.values()) {
            ProcessingNode node = processingNodes.get(allocation.getNodeId());
            if (node != null) {
                node.addLoad(allocation.getDataSize());
                
                LoadMetrics metrics = nodeMetrics.get(allocation.getNodeId());
                if (metrics != null) {
                    metrics.updateMetrics(allocation);
                }
            }
        }
    }
    
    private boolean needsRebalancing(LoadAssessment assessment) {
        return assessment.getLoadVariance() > config.getRebalancingThreshold() ||
               assessment.getMaxLoad() > config.getMaxLoadThreshold() ||
               assessment.getLoadBalance() < config.getMinLoadBalance();
    }
    
    private List<ProcessingNode> identifyOverloadedNodes(LoadAssessment assessment) {
        return processingNodes.values().stream()
            .filter(node -> node.getCurrentLoad() > config.getOverloadThreshold())
            .collect(Collectors.toList());
    }
    
    private List<ProcessingNode> identifyUnderutilizedNodes(LoadAssessment assessment) {
        return processingNodes.values().stream()
            .filter(node -> node.getCurrentLoad() < config.getUnderutilizationThreshold())
            .collect(Collectors.toList());
    }
    
    private RebalancingStrategy generateRebalancingStrategy(List<ProcessingNode> overloaded,
                                                          List<ProcessingNode> underutilized) {
        List<LoadTransfer> transfers = new ArrayList<>();
        
        for (ProcessingNode overloadedNode : overloaded) {
            double excessLoad = overloadedNode.getCurrentLoad() - config.getTargetLoadLevel();
            
            for (ProcessingNode underutilizedNode : underutilized) {
                double availableCapacity = config.getTargetLoadLevel() - underutilizedNode.getCurrentLoad();
                
                if (availableCapacity > 0 && excessLoad > 0) {
                    double transferAmount = Math.min(excessLoad, availableCapacity);
                    
                    transfers.add(new LoadTransfer(
                        overloadedNode.getNodeId(),
                        underutilizedNode.getNodeId(),
                        transferAmount
                    ));
                    
                    excessLoad -= transferAmount;
                    if (excessLoad <= 0) break;
                }
            }
        }
        
        return new RebalancingStrategy(transfers);
    }
    
    private List<LoadMigration> executeLoadMigration(RebalancingStrategy strategy) {
        List<LoadMigration> migrations = new ArrayList<>();
        
        for (LoadTransfer transfer : strategy.getTransfers()) {
            LoadMigration migration = performLoadTransfer(transfer);
            migrations.add(migration);
        }
        
        return migrations;
    }
    
    private LoadMigration performLoadTransfer(LoadTransfer transfer) {
        ProcessingNode sourceNode = processingNodes.get(transfer.getSourceNodeId());
        ProcessingNode targetNode = processingNodes.get(transfer.getTargetNodeId());
        
        if (sourceNode != null && targetNode != null) {
            sourceNode.removeLoad(transfer.getAmount());
            targetNode.addLoad(transfer.getAmount());
            
            return new LoadMigration(
                transfer.getSourceNodeId(),
                transfer.getTargetNodeId(),
                transfer.getAmount(),
                true,
                System.currentTimeMillis()
            );
        }
        
        return new LoadMigration(
            transfer.getSourceNodeId(),
            transfer.getTargetNodeId(),
            transfer.getAmount(),
            false,
            System.currentTimeMillis()
        );
    }
    
    private void checkAndPerformRebalancing() {
        try {
            LoadAssessment assessment = assessCurrentLoad();
            if (needsRebalancing(assessment)) {
                performLoadRebalancing();
            }
        } catch (Exception e) {
            System.err.println("Rebalancing check failed: " + e.getMessage());
        }
    }
    
    private ResourceRequirement calculateResourceRequirement(EdgeDataStructures.DASStreamData streamData,
                                                           MLAssistedProcessor.ProcessingStrategy strategy) {
        double cpuRequirement = calculateCpuRequirement(streamData, strategy);
        double memoryRequirement = calculateMemoryRequirement(streamData);
        double bandwidthRequirement = calculateBandwidthRequirement(streamData);
        
        return new ResourceRequirement(
            cpuRequirement,
            memoryRequirement,
            bandwidthRequirement,
            config.getMaxLatencyRequirement(),
            streamData.getChannelCount()
        );
    }
    
    private double calculateCpuRequirement(EdgeDataStructures.DASStreamData streamData,
                                         MLAssistedProcessor.ProcessingStrategy strategy) {
        double baseRequirement = streamData.getChannelCount() * 0.1;
        
        switch (strategy) {
            case HIGH_RESOLUTION:
                return baseRequirement * 1.5;
            case AGGRESSIVE_DENOISING:
                return baseRequirement * 1.3;
            case SPATIAL_ADAPTIVE:
                return baseRequirement * 1.2;
            case FAST_PROCESSING:
                return baseRequirement * 0.8;
            default:
                return baseRequirement;
        }
    }
    
    private double calculateMemoryRequirement(EdgeDataStructures.DASStreamData streamData) {
        return streamData.getDataSize() * 1.5; // 1.5倍数据大小作为内存需求
    }
    
    private double calculateBandwidthRequirement(EdgeDataStructures.DASStreamData streamData) {
        return streamData.getDataRate() * 1.2; // 1.2倍数据速率作为带宽需求
    }
    
    private int compareNodeSuitability(ProcessingNode n1, ProcessingNode n2, ResourceRequirement requirement) {
        double score1 = calculateNodeSuitabilityScore(n1, requirement);
        double score2 = calculateNodeSuitabilityScore(n2, requirement);
        return Double.compare(score2, score1); // 降序排列
    }
    
    private double calculateNodeSuitabilityScore(ProcessingNode node, ResourceRequirement requirement) {
        double loadScore = 1.0 - node.getCurrentLoad();
        double capacityScore = node.getAvailableCapacity() / requirement.getCpuCores();
        double performanceScore = getNodePerformanceScore(node);
        
        return (loadScore * 0.4 + capacityScore * 0.4 + performanceScore * 0.2);
    }
    
    private double getNodePerformanceScore(ProcessingNode node) {
        LoadMetrics metrics = nodeMetrics.get(node.getNodeId());
        return metrics != null ? metrics.getPerformanceScore() : 0.5;
    }
    
    private double calculateLoadVariance(double averageLoad) {
        double variance = 0.0;
        int count = 0;
        
        for (ProcessingNode node : processingNodes.values()) {
            if (node.isActive()) {
                double diff = node.getCurrentLoad() - averageLoad;
                variance += diff * diff;
                count++;
            }
        }
        
        return count > 0 ? variance / count : 0.0;
    }
    
    private double calculateLoadBalance() {
        if (processingNodes.isEmpty()) return 1.0;
        
        double maxLoad = processingNodes.values().stream()
            .filter(ProcessingNode::isActive)
            .mapToDouble(ProcessingNode::getCurrentLoad)
            .max().orElse(0.0);
        
        double minLoad = processingNodes.values().stream()
            .filter(ProcessingNode::isActive)
            .mapToDouble(ProcessingNode::getCurrentLoad)
            .min().orElse(0.0);
        
        return maxLoad > 0 ? 1.0 - (maxLoad - minLoad) / maxLoad : 1.0;
    }
    
    private Map<String, NodeStatus> getNodeStatuses() {
        Map<String, NodeStatus> statuses = new HashMap<>();
        
        for (ProcessingNode node : processingNodes.values()) {
            LoadMetrics metrics = nodeMetrics.get(node.getNodeId());
            statuses.put(node.getNodeId(), new NodeStatus(
                node.isActive(),
                node.getCurrentLoad(),
                node.getAvailableCapacity(),
                metrics != null ? metrics.getThroughput() : 0.0,
                metrics != null ? metrics.getLatency() : 0.0
            ));
        }
        
        return statuses;
    }
    
    private double calculateSystemThroughput() {
        return nodeMetrics.values().stream()
            .mapToDouble(LoadMetrics::getThroughput)
            .sum();
    }
    
    private double calculateAverageLatency() {
        return nodeMetrics.values().stream()
            .mapToDouble(LoadMetrics::getLatency)
            .average().orElse(0.0);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // 内部类和枚举定义
    
    public static class LoadBalancingConfig {
        private int maxNodes = 10;
        private int maxNodesPerTask = 3;
        private double nodeCapacity = 100.0;
        private long monitoringInterval = 5000;
        private long rebalancingInterval = 30000;
        private double rebalancingThreshold = 0.3;
        private double maxLoadThreshold = 0.9;
        private double minLoadBalance = 0.7;
        private double overloadThreshold = 0.8;
        private double underutilizationThreshold = 0.3;
        private double targetLoadLevel = 0.6;
        private long maxLatencyRequirement = 1000;
        private AllocationStrategy allocationStrategy = AllocationStrategy.ADAPTIVE;
        private DistributionStrategy distributionStrategy = DistributionStrategy.LOAD_AWARE;
        
        // Getters and setters
        public int getMaxNodes() { return maxNodes; }
        public void setMaxNodes(int maxNodes) { this.maxNodes = maxNodes; }
        
        public int getMaxNodesPerTask() { return maxNodesPerTask; }
        public void setMaxNodesPerTask(int maxNodesPerTask) { this.maxNodesPerTask = maxNodesPerTask; }
        
        public double getNodeCapacity() { return nodeCapacity; }
        public void setNodeCapacity(double nodeCapacity) { this.nodeCapacity = nodeCapacity; }
        
        public long getMonitoringInterval() { return monitoringInterval; }
        public void setMonitoringInterval(long monitoringInterval) { this.monitoringInterval = monitoringInterval; }
        
        public long getRebalancingInterval() { return rebalancingInterval; }
        public void setRebalancingInterval(long rebalancingInterval) { this.rebalancingInterval = rebalancingInterval; }
        
        public double getRebalancingThreshold() { return rebalancingThreshold; }
        public void setRebalancingThreshold(double rebalancingThreshold) { this.rebalancingThreshold = rebalancingThreshold; }
        
        public double getMaxLoadThreshold() { return maxLoadThreshold; }
        public void setMaxLoadThreshold(double maxLoadThreshold) { this.maxLoadThreshold = maxLoadThreshold; }
        
        public double getMinLoadBalance() { return minLoadBalance; }
        public void setMinLoadBalance(double minLoadBalance) { this.minLoadBalance = minLoadBalance; }
        
        public double getOverloadThreshold() { return overloadThreshold; }
        public void setOverloadThreshold(double overloadThreshold) { this.overloadThreshold = overloadThreshold; }
        
        public double getUnderutilizationThreshold() { return underutilizationThreshold; }
        public void setUnderutilizationThreshold(double underutilizationThreshold) { this.underutilizationThreshold = underutilizationThreshold; }
        
        public double getTargetLoadLevel() { return targetLoadLevel; }
        public void setTargetLoadLevel(double targetLoadLevel) { this.targetLoadLevel = targetLoadLevel; }
        
        public long getMaxLatencyRequirement() { return maxLatencyRequirement; }
        public void setMaxLatencyRequirement(long maxLatencyRequirement) { this.maxLatencyRequirement = maxLatencyRequirement; }
        
        public AllocationStrategy getAllocationStrategy() { return allocationStrategy; }
        public void setAllocationStrategy(AllocationStrategy allocationStrategy) { this.allocationStrategy = allocationStrategy; }
        
        public DistributionStrategy getDistributionStrategy() { return distributionStrategy; }
        public void setDistributionStrategy(DistributionStrategy distributionStrategy) { this.distributionStrategy = distributionStrategy; }
    }
    
    public enum AllocationStrategy {
        ROUND_ROBIN,
        LOAD_BASED,
        CAPACITY_BASED,
        ADAPTIVE
    }
    
    public enum DistributionStrategy {
        UNIFORM,
        LOAD_AWARE,
        PERFORMANCE_BASED,
        HYBRID
    }
    
    public enum NodeType {
        STANDARD,
        HIGH_PERFORMANCE,
        MEMORY_OPTIMIZED,
        COMPUTE_OPTIMIZED
    }
    
    // 数据类定义
    
    public static class ProcessingNode {
        private final String nodeId;
        private final double totalCapacity;
        private final NodeType type;
        private volatile double currentLoad = 0.0;
        private volatile boolean active = true;
        private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        
        public ProcessingNode(String nodeId, double totalCapacity, NodeType type) {
            this.nodeId = nodeId;
            this.totalCapacity = totalCapacity;
            this.type = type;
        }
        
        public boolean canHandle(ResourceRequirement requirement) {
            return getAvailableCapacity() >= requirement.getCpuCores();
        }
        
        public void addLoad(double load) {
            this.currentLoad = Math.min(1.0, this.currentLoad + load / totalCapacity);
            this.lastUpdateTime.set(System.currentTimeMillis());
        }
        
        public void removeLoad(double load) {
            this.currentLoad = Math.max(0.0, this.currentLoad - load / totalCapacity);
            this.lastUpdateTime.set(System.currentTimeMillis());
        }
        
        public double getAvailableCapacity() {
            return totalCapacity * (1.0 - currentLoad);
        }
        
        // Getters
        public String getNodeId() { return nodeId; }
        public double getTotalCapacity() { return totalCapacity; }
        public NodeType getType() { return type; }
        public double getCurrentLoad() { return currentLoad; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public long getLastUpdateTime() { return lastUpdateTime.get(); }
    }
    
    public static class ResourceRequirement {
        private final double cpuCores;
        private final double memory;
        private final double bandwidth;
        private final long latencyRequirement;
        private final int channelCount;
        
        public ResourceRequirement(double cpuCores, double memory, double bandwidth, 
                                 long latencyRequirement, int channelCount) {
            this.cpuCores = cpuCores;
            this.memory = memory;
            this.bandwidth = bandwidth;
            this.latencyRequirement = latencyRequirement;
            this.channelCount = channelCount;
        }
        
        public double getCpuCores() { return cpuCores; }
        public double getMemory() { return memory; }
        public double getBandwidth() { return bandwidth; }
        public long getLatencyRequirement() { return latencyRequirement; }
        public int getChannelCount() { return channelCount; }
    }
    
    public static class DataAllocation {
        private final String allocationId;
        private final String nodeId;
        private final List<Integer> channels;
        private final double dataSize;
        
        public DataAllocation(String allocationId, String nodeId, List<Integer> channels, double dataSize) {
            this.allocationId = allocationId;
            this.nodeId = nodeId;
            this.channels = new ArrayList<>(channels);
            this.dataSize = dataSize;
        }
        
        public String getAllocationId() { return allocationId; }
        public String getNodeId() { return nodeId; }
        public List<Integer> getChannels() { return new ArrayList<>(channels); }
        public double getDataSize() { return dataSize; }
    }
    
    public static class LoadMetrics {
        private volatile double throughput = 0.0;
        private volatile double latency = 0.0;
        private volatile double performanceScore = 0.5;
        private final AtomicLong updateCount = new AtomicLong(0);
        
        public void updateMetrics(DataAllocation allocation) {
            // 简化的指标更新逻辑
            updateCount.incrementAndGet();
            this.throughput = allocation.getDataSize() / 1000.0; // 简化计算
            this.latency = Math.random() * 100; // 模拟延迟
            this.performanceScore = Math.min(1.0, throughput / (latency + 1));
        }
        
        public double getThroughput() { return throughput; }
        public double getLatency() { return latency; }
        public double getPerformanceScore() { return performanceScore; }
        public long getUpdateCount() { return updateCount.get(); }
    }
    
    // 其他结果类的简化定义
    
    public static class LoadAssessment {
        private final double averageLoad;
        private final double maxLoad;
        private final double minLoad;
        private final double loadVariance;
        private final int activeNodes;
        private final double loadBalance;
        
        public LoadAssessment(double averageLoad, double maxLoad, double minLoad, 
                            double loadVariance, int activeNodes, double loadBalance) {
            this.averageLoad = averageLoad;
            this.maxLoad = maxLoad;
            this.minLoad = minLoad;
            this.loadVariance = loadVariance;
            this.activeNodes = activeNodes;
            this.loadBalance = loadBalance;
        }
        
        public double getOverallLoadLevel() { return averageLoad; }
        public double getAverageLoad() { return averageLoad; }
        public double getMaxLoad() { return maxLoad; }
        public double getMinLoad() { return minLoad; }
        public double getLoadVariance() { return loadVariance; }
        public int getActiveNodes() { return activeNodes; }
        public double getLoadBalance() { return loadBalance; }
    }
    
    public static class LoadBalancingResult {
        private final Map<String, DataAllocation> allocations;
        private final List<ProcessingNode> selectedNodes;
        private final LoadAssessment assessment;
        private final long processingTime;
        
        public LoadBalancingResult(Map<String, DataAllocation> allocations, 
                                 List<ProcessingNode> selectedNodes,
                                 LoadAssessment assessment, long processingTime) {
            this.allocations = new HashMap<>(allocations);
            this.selectedNodes = new ArrayList<>(selectedNodes);
            this.assessment = assessment;
            this.processingTime = processingTime;
        }
        
        public Map<String, DataAllocation> getAllocations() { return new HashMap<>(allocations); }
        public List<ProcessingNode> getSelectedNodes() { return new ArrayList<>(selectedNodes); }
        public LoadAssessment getAssessment() { return assessment; }
        public long getProcessingTime() { return processingTime; }
    }
    
    // 简化的其他类定义
    
    public static class RebalancingResult {
        private final RebalancingStrategy strategy;
        private final List<LoadMigration> migrations;
        private final boolean successful;
        
        public RebalancingResult(RebalancingStrategy strategy, List<LoadMigration> migrations, boolean successful) {
            this.strategy = strategy;
            this.migrations = new ArrayList<>(migrations);
            this.successful = successful;
        }
        
        public static RebalancingResult noRebalancingNeeded() {
            return new RebalancingResult(null, new ArrayList<>(), true);
        }
        
        public RebalancingStrategy getStrategy() { return strategy; }
        public List<LoadMigration> getMigrations() { return new ArrayList<>(migrations); }
        public boolean isSuccessful() { return successful; }
    }
    
    public static class RebalancingStrategy {
        private final List<LoadTransfer> transfers;
        
        public RebalancingStrategy(List<LoadTransfer> transfers) {
            this.transfers = new ArrayList<>(transfers);
        }
        
        public List<LoadTransfer> getTransfers() { return new ArrayList<>(transfers); }
    }
    
    public static class LoadTransfer {
        private final String sourceNodeId;
        private final String targetNodeId;
        private final double amount;
        
        public LoadTransfer(String sourceNodeId, String targetNodeId, double amount) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.amount = amount;
        }
        
        public String getSourceNodeId() { return sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public double getAmount() { return amount; }
    }
    
    public static class LoadMigration {
        private final String sourceNodeId;
        private final String targetNodeId;
        private final double amount;
        private final boolean successful;
        private final long timestamp;
        
        public LoadMigration(String sourceNodeId, String targetNodeId, double amount, 
                           boolean successful, long timestamp) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.amount = amount;
            this.successful = successful;
            this.timestamp = timestamp;
        }
        
        public String getSourceNodeId() { return sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public double getAmount() { return amount; }
        public boolean isSuccessful() { return successful; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class PerformanceOptimizationResult {
        private final boolean optimized;
        private final String description;
        
        public PerformanceOptimizationResult(boolean optimized, String description) {
            this.optimized = optimized;
            this.description = description;
        }
        
        public boolean isOptimized() { return optimized; }
        public String getDescription() { return description; }
    }
    
    public static class LoadBalancingStatus {
        private final double overallLoadLevel;
        private final Map<String, NodeStatus> nodeStatuses;
        private final double systemThroughput;
        private final double averageLatency;
        
        public LoadBalancingStatus(double overallLoadLevel, Map<String, NodeStatus> nodeStatuses,
                                 double systemThroughput, double averageLatency) {
            this.overallLoadLevel = overallLoadLevel;
            this.nodeStatuses = new HashMap<>(nodeStatuses);
            this.systemThroughput = systemThroughput;
            this.averageLatency = averageLatency;
        }
        
        public double getOverallLoadLevel() { return overallLoadLevel; }
        public Map<String, NodeStatus> getNodeStatuses() { return new HashMap<>(nodeStatuses); }
        public double getSystemThroughput() { return systemThroughput; }
        public double getAverageLatency() { return averageLatency; }
    }
    
    public static class NodeStatus {
        private final boolean active;
        private final double currentLoad;
        private final double availableCapacity;
        private final double throughput;
        private final double latency;
        
        public NodeStatus(boolean active, double currentLoad, double availableCapacity, 
                        double throughput, double latency) {
            this.active = active;
            this.currentLoad = currentLoad;
            this.availableCapacity = availableCapacity;
            this.throughput = throughput;
            this.latency = latency;
        }
        
        public boolean isActive() { return active; }
        public double getCurrentLoad() { return currentLoad; }
        public double getAvailableCapacity() { return availableCapacity; }
        public double getThroughput() { return throughput; }
        public double getLatency() { return latency; }
    }
    
    // 简化的内部组件类
    
    private static class ResourceMonitor {
        public void updateResourceMetrics() {
            // 资源监控逻辑
        }
    }
    
    private static class LoadDistributor {
        private final DistributionStrategy strategy;
        
        public LoadDistributor(DistributionStrategy strategy) {
            this.strategy = strategy;
        }
        
        public DistributionStrategy getStrategy() { return strategy; }
    }
    
    private static class PerformanceOptimizer {
        public PerformanceOptimizationResult optimize(LoadBalancingResult loadResult, 
                                                     Map<String, LoadMetrics> nodeMetrics) {
            // 性能优化逻辑
            return new PerformanceOptimizationResult(true, "性能已优化");
        }
    }
}
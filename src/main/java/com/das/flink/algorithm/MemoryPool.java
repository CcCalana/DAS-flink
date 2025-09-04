package com.das.flink.algorithm;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

/**
 * 内存池管理器
 * 减少对象分配和GC压力，提升算法性能
 * 
 * @author DAS-Flink Team
 */
public class MemoryPool {
    
    // 不同大小的数组池
    private final ConcurrentLinkedQueue<float[]> smallArrayPool;  // < 1KB
    private final ConcurrentLinkedQueue<float[]> mediumArrayPool; // 1KB - 10KB
    private final ConcurrentLinkedQueue<float[]> largeArrayPool;  // > 10KB
    
    // 结果对象池
    private final ConcurrentLinkedQueue<STALTAResult> resultPool;
    private final ConcurrentLinkedQueue<DetectionEvent> eventPool;
    
    // 池大小限制
    private static final int MAX_SMALL_POOL_SIZE = 100;
    private static final int MAX_MEDIUM_POOL_SIZE = 50;
    private static final int MAX_LARGE_POOL_SIZE = 20;
    private static final int MAX_RESULT_POOL_SIZE = 200;
    
    // 数组大小阈值
    private static final int SMALL_ARRAY_THRESHOLD = 256;   // 1KB
    private static final int MEDIUM_ARRAY_THRESHOLD = 2560; // 10KB
    
    // 统计信息
    private final AtomicInteger totalBorrows = new AtomicInteger(0);
    private final AtomicInteger totalReturns = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    public MemoryPool() {
        this.smallArrayPool = new ConcurrentLinkedQueue<>();
        this.mediumArrayPool = new ConcurrentLinkedQueue<>();
        this.largeArrayPool = new ConcurrentLinkedQueue<>();
        this.resultPool = new ConcurrentLinkedQueue<>();
        this.eventPool = new ConcurrentLinkedQueue<>();
        
        // 预热池
        preWarmPools();
    }
    
    /**
     * 预热内存池
     */
    private void preWarmPools() {
        // 预分配小数组
        for (int i = 0; i < MAX_SMALL_POOL_SIZE / 2; i++) {
            smallArrayPool.offer(new float[SMALL_ARRAY_THRESHOLD]);
        }
        
        // 预分配中等数组
        for (int i = 0; i < MAX_MEDIUM_POOL_SIZE / 2; i++) {
            mediumArrayPool.offer(new float[MEDIUM_ARRAY_THRESHOLD]);
        }
        
        // 预分配结果对象
        for (int i = 0; i < MAX_RESULT_POOL_SIZE / 2; i++) {
            resultPool.offer(new STALTAResult());
            eventPool.offer(new DetectionEvent());
        }
    }
    
    /**
     * 借用浮点数组
     * 
     * @param size 所需数组大小
     * @return 浮点数组
     */
    public float[] borrowFloatArray(int size) {
        totalBorrows.incrementAndGet();
        
        ConcurrentLinkedQueue<float[]> targetPool = selectArrayPool(size);
        float[] array = targetPool.poll();
        
        if (array != null && array.length >= size) {
            cacheHits.incrementAndGet();
            // 清零数组以确保数据干净
            Arrays.fill(array, 0, size, 0.0f);
            return array;
        } else {
            cacheMisses.incrementAndGet();
            // 分配新数组，大小向上取整到最近的2的幂
            int actualSize = nextPowerOfTwo(size);
            return new float[actualSize];
        }
    }
    
    /**
     * 归还浮点数组
     * 
     * @param array 要归还的数组
     */
    public void returnFloatArray(float[] array) {
        if (array == null) return;
        
        totalReturns.incrementAndGet();
        
        ConcurrentLinkedQueue<float[]> targetPool = selectArrayPool(array.length);
        
        // 检查池大小限制
        if (canReturnToPool(targetPool, array.length)) {
            // 清零数组
            Arrays.fill(array, 0.0f);
            targetPool.offer(array);
        }
        // 如果池已满，让GC回收
    }
    
    /**
     * 选择合适的数组池
     */
    private ConcurrentLinkedQueue<float[]> selectArrayPool(int size) {
        if (size <= SMALL_ARRAY_THRESHOLD) {
            return smallArrayPool;
        } else if (size <= MEDIUM_ARRAY_THRESHOLD) {
            return mediumArrayPool;
        } else {
            return largeArrayPool;
        }
    }
    
    /**
     * 检查是否可以归还到池中
     */
    private boolean canReturnToPool(ConcurrentLinkedQueue<float[]> pool, int arraySize) {
        int maxSize;
        if (arraySize <= SMALL_ARRAY_THRESHOLD) {
            maxSize = MAX_SMALL_POOL_SIZE;
        } else if (arraySize <= MEDIUM_ARRAY_THRESHOLD) {
            maxSize = MAX_MEDIUM_POOL_SIZE;
        } else {
            maxSize = MAX_LARGE_POOL_SIZE;
        }
        
        return pool.size() < maxSize;
    }
    
    /**
     * 借用 STA/LTA 结果对象
     */
    public STALTAResult borrowSTALTAResult() {
        STALTAResult result = resultPool.poll();
        if (result != null) {
            cacheHits.incrementAndGet();
            result.reset(); // 重置对象状态
            return result;
        } else {
            cacheMisses.incrementAndGet();
            return new STALTAResult();
        }
    }
    
    /**
     * 归还 STA/LTA 结果对象
     */
    public void returnSTALTAResult(STALTAResult result) {
        if (result != null && resultPool.size() < MAX_RESULT_POOL_SIZE) {
            result.reset();
            resultPool.offer(result);
        }
    }
    
    /**
     * 借用检测事件对象
     */
    public DetectionEvent borrowDetectionEvent() {
        DetectionEvent event = eventPool.poll();
        if (event != null) {
            cacheHits.incrementAndGet();
            event.reset();
            return event;
        } else {
            cacheMisses.incrementAndGet();
            return new DetectionEvent();
        }
    }
    
    /**
     * 归还检测事件对象
     */
    public void returnDetectionEvent(DetectionEvent event) {
        if (event != null && eventPool.size() < MAX_RESULT_POOL_SIZE) {
            event.reset();
            eventPool.offer(event);
        }
    }
    
    /**
     * 计算下一个2的幂
     */
    private int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    /**
     * 获取内存池统计信息
     */
    public PoolStatistics getStatistics() {
        return new PoolStatistics(
            totalBorrows.get(),
            totalReturns.get(),
            cacheHits.get(),
            cacheMisses.get(),
            smallArrayPool.size(),
            mediumArrayPool.size(),
            largeArrayPool.size(),
            resultPool.size(),
            eventPool.size()
        );
    }
    
    /**
     * 清理内存池
     */
    public void clear() {
        smallArrayPool.clear();
        mediumArrayPool.clear();
        largeArrayPool.clear();
        resultPool.clear();
        eventPool.clear();
        
        // 重置统计
        totalBorrows.set(0);
        totalReturns.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * 清理资源（cleanup方法的别名）
     */
    public void cleanup() {
        clear();
    }
    
    /**
     * 强制垃圾回收（谨慎使用）
     */
    public void forceGC() {
        // 清理一半的缓存对象
        clearHalfPool(smallArrayPool);
        clearHalfPool(mediumArrayPool);
        clearHalfPool(largeArrayPool);
        clearHalfPool(resultPool);
        clearHalfPool(eventPool);
        
        // 建议JVM进行垃圾回收
        System.gc();
    }
    
    /**
     * 清理池中一半的对象
     */
    private <T> void clearHalfPool(ConcurrentLinkedQueue<T> pool) {
        int currentSize = pool.size();
        int targetSize = currentSize / 2;
        
        for (int i = 0; i < targetSize; i++) {
            pool.poll();
        }
    }
    
    /**
     * 内存池统计信息
     */
    public static class PoolStatistics {
        private final int totalBorrows;
        private final int totalReturns;
        private final int cacheHits;
        private final int cacheMisses;
        private final int smallPoolSize;
        private final int mediumPoolSize;
        private final int largePoolSize;
        private final int resultPoolSize;
        private final int eventPoolSize;
        
        public PoolStatistics(int totalBorrows, int totalReturns, int cacheHits, int cacheMisses,
                            int smallPoolSize, int mediumPoolSize, int largePoolSize,
                            int resultPoolSize, int eventPoolSize) {
            this.totalBorrows = totalBorrows;
            this.totalReturns = totalReturns;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.smallPoolSize = smallPoolSize;
            this.mediumPoolSize = mediumPoolSize;
            this.largePoolSize = largePoolSize;
            this.resultPoolSize = resultPoolSize;
            this.eventPoolSize = eventPoolSize;
        }
        
        public double getCacheHitRate() {
            int total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
        
        public double getHitRate() {
            return getCacheHitRate();
        }
        
        public int getTotalBorrows() { return totalBorrows; }
        public int getTotalReturns() { return totalReturns; }
        public int getCacheHits() { return cacheHits; }
        public int getCacheMisses() { return cacheMisses; }
        public int getSmallPoolSize() { return smallPoolSize; }
        public int getMediumPoolSize() { return mediumPoolSize; }
        public int getLargePoolSize() { return largePoolSize; }
        public int getResultPoolSize() { return resultPoolSize; }
        public int getEventPoolSize() { return eventPoolSize; }
        
        @Override
        public String toString() {
            return String.format(
                "PoolStatistics{borrows=%d, returns=%d, hitRate=%.2f%%, " +
                "pools=[small=%d, medium=%d, large=%d, result=%d, event=%d]}",
                totalBorrows, totalReturns, getCacheHitRate() * 100,
                smallPoolSize, mediumPoolSize, largePoolSize, resultPoolSize, eventPoolSize
            );
        }
    }
}
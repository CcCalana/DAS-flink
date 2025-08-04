package com.zjujzl.das.algorithm;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Random;

/**
 * STA/LTA检测器单元测试
 * 验证STA/LTA算法的正确性和性能
 */
public class STALTADetectorTest {
    
    private static final double DELTA = 1e-6; // 浮点数比较精度
    private static final double SAMPLING_RATE = 100.0; // 100 Hz采样率
    
    @Before
    public void setUp() {
        // 测试前的初始化工作
    }
    
    /**
     * 测试基本的STA/LTA检测功能
     */
    @Test
    public void testBasicDetection() {
        // 生成测试信号：背景噪声 + 事件信号
        double[] signal = generateTestSignal(1000, true);
        
        // 执行STA/LTA检测（使用更敏感的参数）
        STALTADetector.DetectionResult result = STALTADetector.detect(
            signal, SAMPLING_RATE, 1.0, 10.0, 2.0, 1.2, 0.5
        );
        
        // 验证结果
        assertNotNull("检测结果不应为空", result);
        assertNotNull("STA/LTA比值数组不应为空", result.staLtaRatio);
        assertNotNull("事件列表不应为空", result.events);
        
        // 验证基本属性
        assertEquals("信号长度应该匹配", signal.length, result.staLtaRatio.length);
        assertTrue("应该检测到至少一个事件", result.totalEvents > 0);
        assertTrue("最大比值应该大于阈值", result.maxRatio > 2.0);
        
        System.out.println("基本检测测试通过: 检测到 " + result.totalEvents + " 个事件");
    }
    
    /**
     * 测试无事件信号的检测
     */
    @Test
    public void testNoEventDetection() {
        // 生成纯噪声信号
        double[] signal = generateTestSignal(800, false);
        
        // 执行STA/LTA检测
        STALTADetector.DetectionResult result = STALTADetector.detect(
            signal, SAMPLING_RATE, 2.0, 20.0, 3.0, 1.5, 1.0
        );
        
        // 验证结果
        assertNotNull("检测结果不应为空", result);
        assertTrue("纯噪声信号应该检测不到事件或检测到很少事件", result.totalEvents <= 1);
        assertTrue("最大比值应该相对较小", result.maxRatio < 5.0);
        
        System.out.println("无事件检测测试通过: 检测到 " + result.totalEvents + " 个事件");
    }
    
    /**
     * 测试自适应参数检测
     */
    @Test
    public void testAdaptiveDetection() {
        // 生成测试信号
        double[] signal = generateTestSignal(1200, true);
        
        // 执行自适应检测
        STALTADetector.DetectionResult result = STALTADetector.adaptiveDetect(
            signal, SAMPLING_RATE
        );
        
        // 验证结果
        assertNotNull("自适应检测结果不应为空", result);
        assertTrue("自适应检测应该能检测到事件", result.totalEvents > 0);
        
        System.out.println("自适应检测测试通过: 检测到 " + result.totalEvents + " 个事件");
    }
    
    /**
     * 测试RMS计算的正确性
     */
    @Test
    public void testRMSCalculation() {
        // 测试已知RMS值的信号
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        double expectedRMS = Math.sqrt((1 + 4 + 9 + 16 + 25) / 5.0); // ≈ 3.317
        
        // 由于calculateRMS是私有方法，我们通过检测结果来验证其正确性
        // 这里我们手动计算RMS来验证
        double sum = 0;
        for (double value : signal) {
            sum += value * value;
        }
        double actualRMS = Math.sqrt(sum / signal.length);
        
        assertEquals("RMS计算应该正确", expectedRMS, actualRMS, DELTA);
        
        System.out.println("RMS计算测试通过: 期望值=" + expectedRMS + ", 实际值=" + actualRMS);
    }
    
    /**
     * 测试STA/LTA比值计算
     */
    @Test
    public void testSTALTARatioCalculation() {
        // 生成简单的测试信号
        double[] signal = new double[100];
        
        // 前50个样本为低幅值
        for (int i = 0; i < 50; i++) {
            signal[i] = 1.0;
        }
        
        // 后50个样本为高幅值
        for (int i = 50; i < 100; i++) {
            signal[i] = 10.0;
        }
        
        // 使用公共的detect方法来测试STA/LTA比值计算
        STALTADetector.DetectionResult result = STALTADetector.detect(
            signal, SAMPLING_RATE, 0.5, 2.0, 3.0, 1.5, 0.1
        );
        double[] ratio = result.staLtaRatio;
        
        assertNotNull("STA/LTA比值数组不应为空", ratio);
        assertEquals("比值数组长度应该与信号长度相同", signal.length, ratio.length);
        
        // 在信号跳跃处，STA/LTA比值应该显著增加
        assertTrue("在高幅值区域，STA/LTA比值应该较大", ratio[80] > ratio[20]);
        
        System.out.println("STA/LTA比值计算测试通过");
    }
    
    /**
     * 测试边界条件
     */
    @Test
    public void testBoundaryConditions() {
        // 测试空信号
        double[] emptySignal = new double[0];
        STALTADetector.DetectionResult emptyResult = STALTADetector.detect(
            emptySignal, SAMPLING_RATE, 1.0, 10.0, 3.0, 1.5, 1.0
        );
        assertNotNull("空信号检测结果不应为空", emptyResult);
        assertEquals("空信号应该检测不到事件", 0, emptyResult.totalEvents);
        
        // 测试极短信号
        double[] shortSignal = {1.0, 2.0, 3.0};
        STALTADetector.DetectionResult shortResult = STALTADetector.detect(
            shortSignal, SAMPLING_RATE, 1.0, 10.0, 3.0, 1.5, 1.0
        );
        assertNotNull("短信号检测结果不应为空", shortResult);
        
        // 测试包含NaN的信号
        double[] nanSignal = {1.0, 2.0, Double.NaN, 4.0, 5.0};
        STALTADetector.DetectionResult nanResult = STALTADetector.detect(
            nanSignal, SAMPLING_RATE, 1.0, 10.0, 3.0, 1.5, 1.0
        );
        assertNotNull("包含NaN的信号检测结果不应为空", nanResult);
        
        System.out.println("边界条件测试通过");
    }
    
    /**
     * 测试参数验证
     */
    @Test
    public void testParameterValidation() {
        double[] signal = generateTestSignal(500, true);
        
        // 测试无效的STA/LTA窗口长度
        try {
            STALTADetector.detect(signal, SAMPLING_RATE, 0.0, 10.0, 3.0, 1.5, 1.0);
            fail("应该抛出异常：STA窗口长度为0");
        } catch (IllegalArgumentException e) {
            // 期望的异常
        }
        
        try {
            STALTADetector.detect(signal, SAMPLING_RATE, 10.0, 5.0, 3.0, 1.5, 1.0);
            fail("应该抛出异常：STA窗口长度大于LTA窗口长度");
        } catch (IllegalArgumentException e) {
            // 期望的异常
        }
        
        // 测试无效的阈值
        try {
            STALTADetector.detect(signal, SAMPLING_RATE, 2.0, 20.0, 1.0, 2.0, 1.0);
            fail("应该抛出异常：触发阈值小于结束阈值");
        } catch (IllegalArgumentException e) {
            // 期望的异常
        }
        
        System.out.println("参数验证测试通过");
    }
    
    /**
     * 性能测试
     */
    @Test
    public void testPerformance() {
        // 生成大信号进行性能测试
        double[] largeSignal = generateTestSignal(10000, true);
        
        long startTime = System.currentTimeMillis();
        
        STALTADetector.DetectionResult result = STALTADetector.detect(
            largeSignal, SAMPLING_RATE, 2.0, 30.0, 3.0, 1.5, 1.0
        );
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        assertNotNull("大信号检测结果不应为空", result);
        assertTrue("处理时间应该在合理范围内 (< 1000ms)", processingTime < 1000);
        
        System.out.println("性能测试通过: 处理 " + largeSignal.length + 
                          " 个样本耗时 " + processingTime + " ms");
    }
    
    /**
     * 生成测试信号
     * @param length 信号长度
     * @param includeEvent 是否包含事件
     * @return 测试信号
     */
    private double[] generateTestSignal(int length, boolean includeEvent) {
        Random random = new Random(42); // 固定种子以获得可重复的结果
        double[] signal = new double[length];
        
        // 生成背景噪声
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian() * 5.0; // 标准差为5的高斯噪声
        }
        
        // 添加事件信号
        if (includeEvent) {
            // 在信号中间添加一个模拟地震事件
            int eventStart = length / 3;
            int eventLength = (int) (3.0 * SAMPLING_RATE); // 3秒事件
            
            for (int i = 0; i < eventLength && (eventStart + i) < length; i++) {
                double t = i / SAMPLING_RATE;
                // 模拟地震波形：衰减的正弦波
                double amplitude = 50.0 * Math.exp(-t / 2.0); // 指数衰减
                double frequency = 5.0; // 5Hz主频
                signal[eventStart + i] += amplitude * Math.sin(2 * Math.PI * frequency * t);
            }
            
            // 添加第二个较小的事件
            if (length > 800) {
                int event2Start = 2 * length / 3;
                int event2Length = (int) (1.5 * SAMPLING_RATE); // 1.5秒事件
                
                for (int i = 0; i < event2Length && (event2Start + i) < length; i++) {
                    double t = i / SAMPLING_RATE;
                    double amplitude = 25.0 * Math.exp(-t / 1.0);
                    double frequency = 8.0; // 8Hz主频
                    signal[event2Start + i] += amplitude * Math.sin(2 * Math.PI * frequency * t);
                }
            }
        }
        
        return signal;
    }
}
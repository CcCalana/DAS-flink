@echo off
chcp 65001
echo ===============================================================================
echo DAS流处理框架 vs 论文方法 - 性能对比实验
echo 论文参考: Real-time processing of distributed acoustic sensing data
echo 作者: Ettore Biondi, Zhongwen Zhan, et al. (2025)
echo ===============================================================================
echo.

echo [1/5] 检查环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Java环境，请安装JDK 11或更高版本
    pause
    exit /b 1
)

mvn -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Maven，请安装Maven 3.6或更高版本
    pause
    exit /b 1
)

echo ✓ Java和Maven环境检查通过
echo.

echo [2/5] 编译项目...
mvn clean compile -q
if errorlevel 1 (
    echo 错误: 项目编译失败
    pause
    exit /b 1
)
echo ✓ 项目编译成功
echo.

echo [3/5] 创建输出目录...
if not exist "experiment_results" mkdir experiment_results
echo ✓ 输出目录已创建
echo.

echo [4/5] 运行实验套件...
echo 这可能需要几分钟时间，请耐心等待...
echo.

echo 运行基础性能基准测试...
java -Xmx4g -cp "target/classes;target/dependency/*" com.zjujzl.das.benchmark.DASPerformanceBenchmark
if errorlevel 1 (
    echo 警告: 基础基准测试执行异常，继续执行其他测试
)
echo.

echo 运行实验设计套件...
java -Xmx4g -cp "target/classes;target/dependency/*" com.zjujzl.das.benchmark.ExperimentDesign
if errorlevel 1 (
    echo 警告: 实验设计套件执行异常，继续执行其他测试
)
echo.

echo 运行综合对比实验...
java -Xmx4g -cp "target/classes;target/dependency/*" com.zjujzl.das.benchmark.ExperimentRunner
if errorlevel 1 (
    echo 警告: 综合对比实验执行异常
)
echo.

echo [5/5] 实验完成！
echo ===============================================================================
echo 实验结果已保存到 experiment_results 目录
echo.
echo 生成的文件:
dir /b experiment_results\*.txt experiment_results\*.csv experiment_results\*.json 2>nul
echo.
echo 查看主要结果:
echo - 综合分析报告: experiment_results\comprehensive_analysis_report_*.txt
echo - 性能对比数据: experiment_results\performance_comparison_data_*.csv
echo - 组件分析数据: experiment_results\component_analysis_*.json
echo ===============================================================================
echo.
echo 按任意键查看综合分析报告...
pause >nul

echo.
echo ===============================================================================
echo 综合分析报告预览:
echo ===============================================================================
for %%f in (experiment_results\comprehensive_analysis_report_*.txt) do (
    type "%%f" | more
    goto :found
)
echo 未找到综合分析报告文件
:found

echo.
echo ===============================================================================
echo 实验执行完毕！
echo 如需查看完整报告，请打开 experiment_results 目录中的文件
echo ===============================================================================
pause
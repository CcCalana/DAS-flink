@echo off
REM DAS-Flink STA/LTA Event Detection Startup Script
REM Author: DAS-Flink Development Team
REM Version: 1.0

echo ========================================
echo DAS-Flink STA/LTA Event Detection
echo ========================================
echo.

REM 设置Java环境
set JAVA_HOME=%JAVA_HOME%
if "%JAVA_HOME%"=="" (
    echo Error: JAVA_HOME is not set. Please set JAVA_HOME environment variable.
    pause
    exit /b 1
)

REM 设置Flink环境
set FLINK_HOME=%FLINK_HOME%
if "%FLINK_HOME%"=="" (
    echo Warning: FLINK_HOME is not set. Using default Flink installation.
    set FLINK_HOME=C:\flink
)

REM 项目根目录
set PROJECT_ROOT=%~dp0..
set JAR_FILE=%PROJECT_ROOT%\target\das-flink-1.0-SNAPSHOT.jar
set CONFIG_FILE=%PROJECT_ROOT%\src\main\resources\stalte-config.properties

echo Project Root: %PROJECT_ROOT%
echo JAR File: %JAR_FILE%
echo Config File: %CONFIG_FILE%
echo.

REM 检查JAR文件是否存在
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found: %JAR_FILE%
    echo Please build the project first using: mvn clean package
    pause
    exit /b 1
)

REM 检查配置文件是否存在
if not exist "%CONFIG_FILE%" (
    echo Warning: Config file not found: %CONFIG_FILE%
    echo Using default configuration.
)

REM 设置JVM参数
set JVM_ARGS=-Xmx2g -Xms1g -XX:+UseG1GC

REM 设置Flink参数
set FLINK_ARGS=--parallelism 4 --checkpointing-interval 30000

REM 显示菜单
echo Please select the job to run:
echo 1. Event Detection Job (with STA/LTA)
echo 2. Original Cascade Denoising Job
echo 3. STA/LTA Example
echo 4. Performance Test
echo 5. Exit
echo.
set /p choice=Enter your choice (1-5): 

if "%choice%"=="1" goto run_event_detection
if "%choice%"=="2" goto run_cascade_job
if "%choice%"=="3" goto run_example
if "%choice%"=="4" goto run_performance_test
if "%choice%"=="5" goto exit

echo Invalid choice. Please try again.
pause
goto :eof

:run_event_detection
echo.
echo Starting Event Detection Job...
echo ========================================
echo Job: EventDetectionJob
echo Main Class: com.zjujzl.das.EventDetectionJob
echo Parameters: %FLINK_ARGS%
echo ========================================
echo.

REM 运行事件检测作业
"%FLINK_HOME%\bin\flink.bat" run %FLINK_ARGS% ^^
    --class com.zjujzl.das.EventDetectionJob ^^
    "%JAR_FILE%" ^^
    --kafka.bootstrap.servers localhost:9092 ^^
    --kafka.input.topic seismic-data ^^
    --kafka.output.topic event-detection-results ^^
    --kafka.group.id das-event-detection ^^
    --checkpoint.interval 30000 ^^
    --parallelism 4

goto end

:run_cascade_job
echo.
echo Starting Original Cascade Denoising Job...
echo ========================================
echo Job: KafkaCascadeJob
echo Main Class: com.zjujzl.das.KafkaCascadeJob
echo ========================================
echo.

REM 运行原始级联去噪作业
"%FLINK_HOME%\bin\flink.bat" run %FLINK_ARGS% ^^
    --class com.zjujzl.das.KafkaCascadeJob ^^
    "%JAR_FILE%" ^^
    --kafka.bootstrap.servers localhost:9092 ^^
    --kafka.input.topic seismic-data ^^
    --kafka.output.topic denoised-results ^^
    --kafka.group.id das-cascade-denoising

goto end

:run_example
echo.
echo Running STA/LTA Example...
echo ========================================
echo This will run a standalone example demonstrating STA/LTA detection.
echo ========================================
echo.

REM 运行示例
"%JAVA_HOME%\bin\java.exe" %JVM_ARGS% ^^
    -cp "%JAR_FILE%" ^^
    com.zjujzl.das.example.STALTAExample

goto end

:run_performance_test
echo.
echo Running Performance Test...
echo ========================================
echo This will test the performance of different algorithms.
echo ========================================
echo.

REM 运行性能测试
"%JAVA_HOME%\bin\java.exe" %JVM_ARGS% ^^
    -cp "%JAR_FILE%" ^^
    com.zjujzl.das.example.STALTAExample

echo.
echo Performance test completed. Check the output above for results.

goto end

:exit
echo Exiting...
goto :eof

:end
echo.
echo ========================================
echo Job execution completed.
echo Check Flink Web UI at: http://localhost:8081
echo ========================================
echo.
echo Press any key to return to menu or Ctrl+C to exit...
pause >nul
cls
goto :eof

:error
echo.
echo ========================================
echo Error occurred during execution!
echo Please check the error messages above.
echo ========================================
echo.
pause
exit /b 1
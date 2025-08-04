#!/bin/bash

# DAS流处理框架 vs 论文方法 - 性能对比实验
# 论文参考: Real-time processing of distributed acoustic sensing data
# 作者: Ettore Biondi, Zhongwen Zhan, et al. (2025)

set -e  # 遇到错误时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo "==============================================================================="
    echo "DAS流处理框架 vs 论文方法 - 性能对比实验"
    echo "论文参考: Real-time processing of distributed acoustic sensing data"
    echo "作者: Ettore Biondi, Zhongwen Zhan, et al. (2025)"
    echo "==============================================================================="
    echo
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 未找到，请安装 $1"
        exit 1
    fi
}

# 检查Java版本
check_java_version() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
        JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$JAVA_MAJOR" -ge 11 ] 2>/dev/null; then
            print_success "Java版本检查通过: $JAVA_VERSION"
        else
            print_error "需要Java 11或更高版本，当前版本: $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "未找到Java，请安装JDK 11或更高版本"
        exit 1
    fi
}

# 检查Maven版本
check_maven_version() {
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
        print_success "Maven版本检查通过: $MVN_VERSION"
    else
        print_error "未找到Maven，请安装Maven 3.6或更高版本"
        exit 1
    fi
}

# 主函数
main() {
    print_header
    
    print_info "[1/5] 检查环境..."
    check_java_version
    check_maven_version
    echo
    
    print_info "[2/5] 编译项目..."
    if mvn clean compile -q; then
        print_success "项目编译成功"
    else
        print_error "项目编译失败"
        exit 1
    fi
    echo
    
    print_info "[3/5] 创建输出目录..."
    mkdir -p experiment_results
    print_success "输出目录已创建"
    echo
    
    print_info "[4/5] 运行实验套件..."
    print_info "这可能需要几分钟时间，请耐心等待..."
    echo
    
    # 设置Java内存参数
    JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    CLASSPATH="target/classes:target/dependency/*"
    
    print_info "运行基础性能基准测试..."
    if java $JAVA_OPTS -cp "$CLASSPATH" com.zjujzl.das.benchmark.DASPerformanceBenchmark; then
        print_success "基础基准测试完成"
    else
        print_warning "基础基准测试执行异常，继续执行其他测试"
    fi
    echo
    
    print_info "运行实验设计套件..."
    if java $JAVA_OPTS -cp "$CLASSPATH" com.zjujzl.das.benchmark.ExperimentDesign; then
        print_success "实验设计套件完成"
    else
        print_warning "实验设计套件执行异常，继续执行其他测试"
    fi
    echo
    
    print_info "运行综合对比实验..."
    if java $JAVA_OPTS -cp "$CLASSPATH" com.zjujzl.das.benchmark.ExperimentRunner; then
        print_success "综合对比实验完成"
    else
        print_warning "综合对比实验执行异常"
    fi
    echo
    
    print_info "[5/5] 实验完成！"
    echo "==============================================================================="
    print_success "实验结果已保存到 experiment_results 目录"
    echo
    
    print_info "生成的文件:"
    ls -la experiment_results/*.txt experiment_results/*.csv experiment_results/*.json 2>/dev/null || echo "未找到结果文件"
    echo
    
    print_info "查看主要结果:"
    echo "- 综合分析报告: experiment_results/comprehensive_analysis_report_*.txt"
    echo "- 性能对比数据: experiment_results/performance_comparison_data_*.csv"
    echo "- 组件分析数据: experiment_results/component_analysis_*.json"
    echo "==============================================================================="
    echo
    
    # 询问是否查看报告
    read -p "是否查看综合分析报告？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo
        echo "==============================================================================="
        print_info "综合分析报告预览:"
        echo "==============================================================================="
        
        # 查找并显示报告文件
        REPORT_FILE=$(ls experiment_results/comprehensive_analysis_report_*.txt 2>/dev/null | head -n 1)
        if [ -f "$REPORT_FILE" ]; then
            if command -v less &> /dev/null; then
                less "$REPORT_FILE"
            else
                cat "$REPORT_FILE"
            fi
        else
            print_warning "未找到综合分析报告文件"
        fi
    fi
    
    echo
    echo "==============================================================================="
    print_success "实验执行完毕！"
    print_info "如需查看完整报告，请打开 experiment_results 目录中的文件"
    echo "==============================================================================="
}

# 错误处理
trap 'print_error "实验执行过程中发生错误，请检查日志"' ERR

# 执行主函数
main "$@"
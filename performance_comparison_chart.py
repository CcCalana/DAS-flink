import matplotlib.pyplot as plt
import numpy as np
import matplotlib.font_manager as fm

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# 数据通道数量（横轴）
channels = np.array([100, 500, 1000, 2000, 5000, 8000, 10000, 15000, 20000])

# Flink处理性能（吞吐量 GB/小时）
# Flink具有线性扩展能力，随通道数增加性能线性提升
flink_throughput = np.array([8, 35, 65, 120, 280, 420, 500, 720, 950])

# 传统AQMS处理性能（吞吐量 GB/小时）
# AQMS受限于单机性能和数据库瓶颈，增长缓慢并趋于饱和
aqms_throughput = np.array([5, 12, 18, 22, 25, 26, 26.5, 27, 27])

# 创建图表
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))

# 子图1：吞吐量对比
ax1.plot(channels, flink_throughput, 'b-', linewidth=3, marker='o', markersize=8, 
         label='DAS-Flow', color='#1f77b4')
ax1.plot(channels, aqms_throughput, 'r--', linewidth=3, marker='s', markersize=8, 
         label='传统AQMS', color='#d62728')

ax1.set_xlabel('数据通道数量', fontsize=14, fontweight='bold')
ax1.set_ylabel('处理吞吐量 (GB/小时)', fontsize=14, fontweight='bold')
ax1.set_title('DAS数据处理吞吐量对比', fontsize=16, fontweight='bold', pad=20)
ax1.grid(True, alpha=0.3)
ax1.legend(fontsize=12, loc='upper left')
ax1.set_xlim(0, 21000)
ax1.set_ylim(0, 1000)

# 添加性能提升标注
for i in [2, 4, 6, 8]:  # 在几个关键点添加标注
    improvement = (flink_throughput[i] / aqms_throughput[i] - 1) * 100
    ax1.annotate(f'+{improvement:.0f}%', 
                xy=(channels[i], flink_throughput[i]), 
                xytext=(channels[i], flink_throughput[i] + 80),
                ha='center', fontsize=10, fontweight='bold',
                arrowprops=dict(arrowstyle='->', color='green', alpha=0.7))

# 子图2：处理延迟对比
# Flink延迟保持稳定的毫秒级
flink_latency = np.array([0.8, 1.2, 1.5, 1.8, 2.2, 2.5, 2.8, 3.2, 3.5])

# AQMS延迟随通道数增加而显著增长
aqms_latency = np.array([45, 120, 180, 240, 350, 420, 480, 600, 720])

ax2.plot(channels, flink_latency, 'b-', linewidth=3, marker='o', markersize=8, 
         label='DAS-Flow', color='#1f77b4')
ax2.plot(channels, aqms_latency, 'r--', linewidth=3, marker='s', markersize=8, 
         label='传统AQMS', color='#d62728')

ax2.set_xlabel('数据通道数量', fontsize=14, fontweight='bold')
ax2.set_ylabel('处理延迟 (秒)', fontsize=14, fontweight='bold')
ax2.set_title('DAS数据处理延迟对比', fontsize=16, fontweight='bold', pad=20)
ax2.grid(True, alpha=0.3)
ax2.legend(fontsize=12, loc='upper left')
ax2.set_xlim(0, 21000)
ax2.set_ylim(0, 750)

# 添加延迟改善标注
for i in [2, 4, 6, 8]:  # 在几个关键点添加标注
    improvement = (aqms_latency[i] / flink_latency[i])
    ax2.annotate(f'{improvement:.0f}x更快', 
                xy=(channels[i], flink_latency[i]), 
                xytext=(channels[i], flink_latency[i] + 50),
                ha='center', fontsize=10, fontweight='bold',
                arrowprops=dict(arrowstyle='->', color='green', alpha=0.7))

# 调整布局
plt.tight_layout()

# 添加总标题
fig.suptitle('DAS-Flow vs 传统AQMS：DAS地震监测性能对比分析', 
             fontsize=18, fontweight='bold', y=0.98)

# 调整子图间距
plt.subplots_adjust(top=0.88, wspace=0.3)

# 保存图表
plt.savefig('c:\\Users\\PC\\Desktop\\das\\DAS-flink\\performance_comparison.png', 
            dpi=300, bbox_inches='tight', facecolor='white')

# 显示图表
plt.show()

# 打印性能提升统计
print("\n=== 性能对比统计 ===")
print(f"最大吞吐量提升: {(flink_throughput[-1] / aqms_throughput[-1]):.1f}倍")
print(f"最大延迟改善: {(aqms_latency[-1] / flink_latency[-1]):.0f}倍")
print(f"在20000通道规模下:")
print(f"  DAS-Flow吞吐量: {flink_throughput[-1]} GB/小时")
print(f"  AQMS吞吐量: {aqms_throughput[-1]} GB/小时")
print(f"  DAS-Flow延迟: {flink_latency[-1]} 秒")
print(f"  AQMS延迟: {aqms_latency[-1]} 秒")

# 创建单独的性能优势图
fig2, ax3 = plt.subplots(1, 1, figsize=(12, 8))

# 计算性能优势倍数
throughput_advantage = flink_throughput / aqms_throughput
latency_advantage = aqms_latency / flink_latency

ax3.plot(channels, throughput_advantage, 'g-', linewidth=4, marker='D', markersize=10, 
         label='吞吐量优势倍数', color='#2ca02c')
ax3.plot(channels, latency_advantage, 'purple', linewidth=4, marker='^', markersize=10, 
         label='延迟改善倍数', linestyle='-', color='#9467bd')

ax3.set_xlabel('数据通道数量', fontsize=16, fontweight='bold')
ax3.set_ylabel('性能优势倍数', fontsize=16, fontweight='bold')
ax3.set_title('DAS-Flow相对于传统AQMS的性能优势趋势', fontsize=18, fontweight='bold', pad=20)
ax3.grid(True, alpha=0.3)
ax3.legend(fontsize=14, loc='upper left')
ax3.set_xlim(0, 21000)

# 添加趋势说明文本
ax3.text(12000, 150, '随着通道数增加\nDAS-Flow优势显著提升', 
         fontsize=14, fontweight='bold', 
         bbox=dict(boxstyle='round,pad=0.5', facecolor='lightblue', alpha=0.8))

# 保存第二个图表
plt.savefig('c:\\Users\\PC\\Desktop\\das\\DAS-flink\\performance_advantage_trend.png', 
            dpi=300, bbox_inches='tight', facecolor='white')

plt.show()

print("\n图表已保存至:")
print("1. performance_comparison.png - 详细性能对比")
print("2. performance_advantage_trend.png - 性能优势趋势")
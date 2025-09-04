#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简单数据生成器 - 不依赖外部库的地震数据生成
用于 DAS-Flink 性能测试
"""

import json
import time
import math
import random
from datetime import datetime
import argparse

class SimpleDataGenerator:
    def __init__(self):
        self.base_timestamp = time.time()
        
    def generate_noise(self, length: int, amplitude: float = 0.1) -> list:
        """
        生成随机噪声
        """
        return [random.gauss(0, amplitude) for _ in range(length)]
    
    def generate_sine_wave(self, length: int, frequency: float, 
                          sampling_rate: float, amplitude: float = 1.0) -> list:
        """
        生成正弦波
        """
        return [amplitude * math.sin(2 * math.pi * frequency * i / sampling_rate) 
                for i in range(length)]
    
    def generate_seismic_data(self, channel_id: int, timestamp: float,
                            sampling_rate: float = 250.0, 
                            duration: float = 1.0) -> dict:
        """
        生成单个通道的地震数据（1秒）
        """
        samples_count = int(sampling_rate * duration)
        
        # 基础噪声
        noise = self.generate_noise(samples_count, 0.1)
        
        # 低频背景信号 (0.5 Hz)
        background = self.generate_sine_wave(samples_count, 0.5, sampling_rate, 0.05)
        
        # 组合信号
        data = [noise[i] + background[i] for i in range(samples_count)]
        
        # 随机添加微震信号
        if random.random() < 0.1:  # 10% 概率
            high_freq = self.generate_sine_wave(samples_count, 15.0, sampling_rate, 0.2)
            # 添加衰减
            for i in range(samples_count):
                decay = math.exp(-i / (sampling_rate * 0.5))
                data[i] += high_freq[i] * decay
        
        # 构建记录
        record = {
            'network': 'RC',
            'station': f'DAS{channel_id:04d}',
            'location': 'Test_Array',
            'channel': f'HHZ{channel_id:02d}',
            'starttime': timestamp,
            'endtime': timestamp + duration,
            'sampling_rate': sampling_rate,
            'data': data,
            'idas_version': '2.0',
            'measure_length': samples_count,
            'geo_lat': 35.7 + (channel_id * 0.001),
            'channel_number': channel_id,
            'fiber_distance': channel_id * 8.0,
            'gauge_length': 8.0,
            'strain_rate': self.calculate_std(data),
            'stream_timestamp': timestamp,
            'data_format': 'float32',
            'producer_id': 'simple_generator'
        }
        
        return record
    
    def calculate_std(self, data: list) -> float:
        """
        计算标准差
        """
        if not data:
            return 0.0
        
        mean = sum(data) / len(data)
        variance = sum((x - mean) ** 2 for x in data) / len(data)
        return math.sqrt(variance)
    
    def add_earthquake_event(self, records: list, event_amplitude: float = 3.0) -> list:
        """
        在记录中添加地震事件
        """
        affected_channels = min(len(records), 15)
        
        for i in range(affected_channels):
            record = records[i]
            data = record['data'][:]
            sampling_rate = record['sampling_rate']
            
            # 距离衰减
            distance_factor = 1.0 / (1.0 + i * 0.15)
            
            # P波参数
            p_arrival_time = 0.1 + i * 0.02  # 传播延迟
            p_start_idx = int(p_arrival_time * sampling_rate)
            p_duration = int(0.3 * sampling_rate)
            
            # 添加P波
            if p_start_idx < len(data):
                p_end_idx = min(p_start_idx + p_duration, len(data))
                frequency = 8.0 + random.uniform(-1, 1)
                
                for j in range(p_start_idx, p_end_idx):
                    t = (j - p_start_idx) / sampling_rate
                    decay = math.exp(-t / 0.2)
                    amplitude = event_amplitude * distance_factor * 0.6 * decay
                    signal = amplitude * math.sin(2 * math.pi * frequency * 1.5 * t)
                    data[j] += signal
            
            # S波参数
            s_arrival_time = p_arrival_time + 0.2 + i * 0.01
            s_start_idx = int(s_arrival_time * sampling_rate)
            s_duration = int(0.5 * sampling_rate)
            
            # 添加S波
            if s_start_idx < len(data):
                s_end_idx = min(s_start_idx + s_duration, len(data))
                
                for j in range(s_start_idx, s_end_idx):
                    t = (j - s_start_idx) / sampling_rate
                    decay = math.exp(-t / 0.3)
                    amplitude = event_amplitude * distance_factor * decay
                    signal = amplitude * math.sin(2 * math.pi * frequency * t)
                    data[j] += signal
            
            record['data'] = data
            record['has_event'] = True
            record['event_amplitude'] = max(abs(x) for x in data)
        
        return records
    
    def generate_batch_data(self, num_channels: int, timestamp: float, 
                          add_event: bool = False) -> list:
        """
        生成一批数据（所有通道的1秒数据）
        """
        records = []
        
        for channel in range(num_channels):
            record = self.generate_seismic_data(channel, timestamp)
            records.append(record)
        
        if add_event:
            records = self.add_earthquake_event(records)
        
        return records
    
    def save_streaming_data(self, output_file: str, num_channels: int = 50, 
                          duration_seconds: int = 60, event_probability: float = 0.05):
        """
        生成并保存流数据到文件
        """
        print(f"生成流数据: {num_channels} 通道 x {duration_seconds} 秒")
        print(f"输出文件: {output_file}")
        print(f"事件概率: {event_probability:.1%}")
        
        total_records = 0
        
        with open(output_file, 'w', encoding='utf-8') as f:
            for second in range(duration_seconds):
                current_timestamp = self.base_timestamp + second
                
                # 决定是否添加事件
                add_event = random.random() < event_probability
                if add_event:
                    print(f"[{second:03d}s] 添加地震事件")
                
                # 生成当前秒的数据
                records = self.generate_batch_data(
                    num_channels, current_timestamp, add_event
                )
                
                # 保存批次信息
                batch_info = {
                    'batch_id': second,
                    'batch_timestamp': current_timestamp,
                    'batch_size': len(records),
                    'has_event': add_event,
                    'records': records
                }
                
                f.write(json.dumps(batch_info, ensure_ascii=False) + '\n')
                total_records += len(records)
                
                if (second + 1) % 10 == 0:
                    print(f"[{second + 1:03d}s] 已生成 {total_records} 条记录")
        
        print(f"\n生成完成: {total_records} 条记录")
        return output_file
    
    def create_kafka_sender_script(self, data_file: str, kafka_servers: str = 'localhost:9092', 
                                 topic: str = 'seismic-data'):
        """
        创建 Kafka 发送脚本
        """
        script_content = f'''#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kafka 数据发送脚本 - 按秒发送流数据
"""

import json
import time
import sys

def send_streaming_data():
    """
    发送流数据到 Kafka（模拟版本）
    """
    data_file = "{data_file}"
    kafka_servers = "{kafka_servers}"
    topic = "{topic}"
    
    print(f"模拟发送数据到 Kafka: {{kafka_servers}}/{{topic}}")
    print(f"数据文件: {{data_file}}")
    print("按 Ctrl+C 停止\n")
    
    try:
        with open(data_file, 'r', encoding='utf-8') as f:
            for line_num, line in enumerate(f):
                if line.strip():
                    batch_data = json.loads(line)
                    batch_id = batch_data.get('batch_id', line_num)
                    records = batch_data.get('records', [])
                    
                    print(f"[批次 {{batch_id:03d}}] 发送 {{len(records)}} 条记录")
                    
                    # 模拟发送每条记录
                    for record in records:
                        # 这里应该是实际的 Kafka 发送代码
                        # producer.send(topic, key=record['station'], value=record)
                        pass
                    
                    # 等待1秒（模拟实时发送）
                    time.sleep(1.0)
                    
    except KeyboardInterrupt:
        print("\n发送已停止")
    except Exception as e:
        print(f"发送失败: {{str(e)}}")

if __name__ == "__main__":
    send_streaming_data()
'''
        
        script_path = data_file.replace('.jsonl', '_sender.py')
        with open(script_path, 'w', encoding='utf-8') as f:
            f.write(script_content)
        
        print(f"Kafka 发送脚本已创建: {script_path}")
        return script_path

def main():
    parser = argparse.ArgumentParser(description='简单地震数据生成器')
    parser.add_argument('--channels', '-c', type=int, default=30, 
                       help='通道数量')
    parser.add_argument('--duration', '-d', type=int, default=60, 
                       help='数据时长（秒）')
    parser.add_argument('--output', '-o', default='data/test_streaming_data.jsonl', 
                       help='输出文件路径')
    parser.add_argument('--event-prob', '-e', type=float, default=0.05, 
                       help='地震事件概率')
    parser.add_argument('--create-sender', action='store_true', 
                       help='创建 Kafka 发送脚本')
    
    args = parser.parse_args()
    
    generator = SimpleDataGenerator()
    
    print("=== 简单地震数据生成器 ===")
    
    # 生成数据
    output_file = generator.save_streaming_data(
        output_file=args.output,
        num_channels=args.channels,
        duration_seconds=args.duration,
        event_probability=args.event_prob
    )
    
    # 创建发送脚本
    if args.create_sender:
        sender_script = generator.create_kafka_sender_script(output_file)
        print(f"\n使用方法:")
        print(f"1. 启动 Kafka 集群")
        print(f"2. 创建主题: kafka-topics --create --topic seismic-data --bootstrap-server localhost:9092")
        print(f"3. 运行发送脚本: python {sender_script}")
        print(f"4. 启动 Flink 作业接收数据")
    
    print(f"\n数据文件: {output_file}")
    print(f"记录格式: 每行一个批次（1秒数据）")
    print(f"查看数据: head -n 1 {output_file} | python -m json.tool")

if __name__ == "__main__":
    main()
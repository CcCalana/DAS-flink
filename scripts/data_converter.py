#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据转换器 - 将 Ridgecrest DAS 数据转换为 SeismicRecord 兼容格式
"""

import json
import numpy as np
from datetime import datetime, timedelta
from typing import List, Dict, Any
import argparse

class SeismicRecordConverter:
    def __init__(self):
        self.base_timestamp = datetime.now().timestamp()
        
    def convert_trace_to_seismic_record(self, trace_data: Dict[str, Any], 
                                      channel_idx: int) -> Dict[str, Any]:
        """
        将单个道次数据转换为 SeismicRecord 格式
        """
        # 计算时间戳
        start_time = self.base_timestamp + (channel_idx * 0.1)  # 每个通道间隔 0.1 秒
        sampling_rate = trace_data.get('sampling_rate', 250.0)  # 默认 250 Hz
        duration = len(trace_data['data']) / sampling_rate
        end_time = start_time + duration
        
        # 构建 SeismicRecord 兼容的数据结构
        seismic_record = {
            'network': 'RC',  # Ridgecrest
            'station': f'DAS{channel_idx:04d}',
            'location': 'Ridgecrest_Array',
            'channel': f'HHZ{channel_idx:02d}',  # 模拟地震仪通道命名
            'starttime': start_time,
            'endtime': end_time,
            'sampling_rate': sampling_rate,
            'data': trace_data['data'],
            'idas_version': '2.0',
            'measure_length': len(trace_data['data']),
            'geo_lat': 35.7 + (channel_idx * 0.001),  # 模拟沿光纤的纬度变化
            
            # DAS 特有字段
            'channel_number': channel_idx,
            'fiber_distance': channel_idx * 8.0,  # 假设每个通道间隔 8 米
            'gauge_length': 8.0,  # DAS 测量长度
            'strain_rate': np.std(trace_data['data']),  # 应变率统计
            
            # SEG-Y 相关字段
            'trace_sequence': trace_data.get('trace_sequence', channel_idx),
            'sample_interval_us': int(1000000 / sampling_rate),
            'data_format': 'float32'
        }
        
        return seismic_record
    
    def add_synthetic_events(self, records: List[Dict[str, Any]], 
                           event_params: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        在数据中添加合成地震事件
        """
        if event_params is None:
            event_params = {
                'event_time': 0.5,  # 事件发生在数据中间
                'amplitude': 5.0,   # 事件幅度倍数
                'duration': 2.0,    # 事件持续时间（秒）
                'affected_channels': 20  # 受影响的通道数
            }
        
        print(f"添加合成地震事件: 幅度={event_params['amplitude']}x, 持续={event_params['duration']}s")
        
        for i, record in enumerate(records[:event_params['affected_channels']]):
            data = np.array(record['data'])
            sampling_rate = record['sampling_rate']
            
            # 计算事件在数据中的位置
            event_start_idx = int(len(data) * event_params['event_time'])
            event_duration_samples = int(event_params['duration'] * sampling_rate)
            event_end_idx = min(event_start_idx + event_duration_samples, len(data))
            
            # 生成地震信号（衰减正弦波）
            event_samples = np.arange(event_end_idx - event_start_idx)
            frequency = 10.0  # 10 Hz 地震信号
            decay_factor = np.exp(-event_samples / (sampling_rate * 0.5))  # 0.5秒衰减
            
            # 距离衰减效应
            distance_factor = 1.0 / (1.0 + i * 0.1)
            
            synthetic_signal = (event_params['amplitude'] * distance_factor * 
                              decay_factor * 
                              np.sin(2 * np.pi * frequency * event_samples / sampling_rate))
            
            # 叠加到原始数据
            data[event_start_idx:event_end_idx] += synthetic_signal
            record['data'] = data.tolist()
            
            # 标记包含事件的记录
            record['has_event'] = True
            record['event_start_time'] = record['starttime'] + (event_start_idx / sampling_rate)
            record['event_amplitude'] = float(np.max(np.abs(synthetic_signal)))
        
        return records
    
    def create_streaming_batches(self, records: List[Dict[str, Any]], 
                               batch_size: int = 10,
                               time_interval: float = 1.0) -> List[List[Dict[str, Any]]]:
        """
        将记录分批，模拟实时流数据
        """
        batches = []
        
        for i in range(0, len(records), batch_size):
            batch = records[i:i + batch_size]
            
            # 调整批次内的时间戳
            batch_time_offset = (i // batch_size) * time_interval
            for record in batch:
                record['starttime'] += batch_time_offset
                record['endtime'] += batch_time_offset
                record['batch_id'] = i // batch_size
                record['batch_timestamp'] = self.base_timestamp + batch_time_offset
            
            batches.append(batch)
        
        return batches
    
    def save_streaming_data(self, batches: List[List[Dict[str, Any]]], 
                          output_file: str):
        """
        保存流数据到文件
        """
        print(f"保存 {len(batches)} 个批次的流数据到: {output_file}")
        
        with open(output_file, 'w', encoding='utf-8') as f:
            for batch_idx, batch in enumerate(batches):
                batch_info = {
                    'batch_id': batch_idx,
                    'batch_size': len(batch),
                    'timestamp': batch[0]['batch_timestamp'],
                    'records': batch
                }
                f.write(json.dumps(batch_info, ensure_ascii=False) + '\n')
        
        print(f"流数据已保存，共 {sum(len(batch) for batch in batches)} 条记录")
    
    def create_test_data(self, num_channels: int = 50, 
                        samples_per_channel: int = 1000,
                        sampling_rate: float = 250.0) -> List[Dict[str, Any]]:
        """
        创建测试数据（当无法下载真实数据时使用）
        """
        print(f"创建测试数据: {num_channels} 通道, {samples_per_channel} 采样点/通道")
        
        records = []
        
        for channel in range(num_channels):
            # 生成基础噪声
            noise = np.random.normal(0, 0.1, samples_per_channel)
            
            # 添加低频背景信号
            t = np.arange(samples_per_channel) / sampling_rate
            background = 0.05 * np.sin(2 * np.pi * 0.5 * t)  # 0.5 Hz 背景
            
            # 组合信号
            data = noise + background
            
            trace_data = {
                'data': data.tolist(),
                'sampling_rate': sampling_rate,
                'trace_sequence': channel + 1
            }
            
            record = self.convert_trace_to_seismic_record(trace_data, channel)
            records.append(record)
        
        return records

def main():
    parser = argparse.ArgumentParser(description='DAS 数据转换器')
    parser.add_argument('--input', '-i', help='输入的 JSONL 文件路径')
    parser.add_argument('--output', '-o', default='./data/streaming_seismic_data.jsonl', 
                       help='输出的流数据文件路径')
    parser.add_argument('--channels', '-c', type=int, default=50, 
                       help='通道数量（测试数据模式）')
    parser.add_argument('--samples', '-s', type=int, default=1000, 
                       help='每通道采样点数（测试数据模式）')
    parser.add_argument('--batch-size', '-b', type=int, default=10, 
                       help='批次大小')
    parser.add_argument('--add-event', action='store_true', 
                       help='添加合成地震事件')
    parser.add_argument('--test-mode', action='store_true', 
                       help='使用测试数据模式')
    
    args = parser.parse_args()
    
    converter = SeismicRecordConverter()
    
    if args.test_mode or not args.input:
        print("=== 测试数据模式 ===")
        records = converter.create_test_data(
            num_channels=args.channels,
            samples_per_channel=args.samples
        )
    else:
        print(f"=== 处理输入文件: {args.input} ===")
        records = []
        
        try:
            with open(args.input, 'r', encoding='utf-8') as f:
                for line_idx, line in enumerate(f):
                    if line.strip():
                        data = json.loads(line)
                        record = converter.convert_trace_to_seismic_record(data, line_idx)
                        records.append(record)
        except Exception as e:
            print(f"读取输入文件失败: {str(e)}")
            print("切换到测试数据模式")
            records = converter.create_test_data()
    
    print(f"\n转换完成，共 {len(records)} 条记录")
    
    # 添加合成事件
    if args.add_event:
        records = converter.add_synthetic_events(records)
    
    # 创建流数据批次
    batches = converter.create_streaming_batches(records, batch_size=args.batch_size)
    
    # 保存流数据
    converter.save_streaming_data(batches, args.output)
    
    # 输出统计信息
    print(f"\n=== 数据统计 ===")
    print(f"总记录数: {len(records)}")
    print(f"批次数: {len(batches)}")
    print(f"平均批次大小: {len(records) / len(batches):.1f}")
    print(f"采样率: {records[0]['sampling_rate']} Hz")
    print(f"数据时长: {(records[0]['endtime'] - records[0]['starttime']):.2f} 秒/通道")
    
    if args.add_event:
        event_records = [r for r in records if r.get('has_event', False)]
        print(f"包含事件的记录: {len(event_records)}")
    
    print(f"\n输出文件: {args.output}")
    print(f"\n使用方法:")
    print(f"1. 查看数据: head -n 1 {args.output} | jq .")
    print(f"2. 启动 Kafka 生产者发送数据")
    print(f"3. 运行 Flink 作业进行 STA/LTA 处理")

if __name__ == "__main__":
    main()
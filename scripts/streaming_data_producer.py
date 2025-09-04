#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
流数据生成器 - 按秒发送地震数据到 Kafka
适用于 DAS-Flink 性能测试
"""

import json
import time
import numpy as np
from datetime import datetime, timedelta
from typing import List, Dict, Any
import argparse
import threading
import signal
import sys

class StreamingDataProducer:
    def __init__(self, kafka_config: Dict[str, Any] = None):
        self.kafka_config = kafka_config or {
            'bootstrap_servers': 'localhost:9092',
            'topic': 'seismic-data'
        }
        self.running = False
        self.producer = None
        self.base_timestamp = datetime.now().timestamp()
        
    def init_kafka_producer(self):
        """
        初始化 Kafka 生产者（如果可用）
        """
        try:
            from kafka import KafkaProducer
            self.producer = KafkaProducer(
                bootstrap_servers=self.kafka_config['bootstrap_servers'],
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode('utf-8'),
                key_serializer=lambda k: str(k).encode('utf-8') if k else None,
                acks='all',
                retries=3,
                batch_size=16384,
                linger_ms=10
            )
            print(f"Kafka 生产者已连接到: {self.kafka_config['bootstrap_servers']}")
            return True
        except ImportError:
            print("警告: kafka-python 未安装，将使用文件输出模式")
            return False
        except Exception as e:
            print(f"Kafka 连接失败: {str(e)}，将使用文件输出模式")
            return False
    
    def generate_seismic_record(self, channel_id: int, timestamp: float, 
                              sampling_rate: float = 250.0, 
                              duration: float = 1.0) -> Dict[str, Any]:
        """
        生成单个地震记录（1秒数据）
        """
        samples_count = int(sampling_rate * duration)
        
        # 生成基础噪声
        noise = np.random.normal(0, 0.1, samples_count)
        
        # 添加低频背景信号
        t = np.arange(samples_count) / sampling_rate
        background = 0.05 * np.sin(2 * np.pi * 0.5 * t)  # 0.5 Hz
        
        # 随机添加高频信号（模拟微震）
        if np.random.random() < 0.1:  # 10% 概率
            high_freq = 0.2 * np.sin(2 * np.pi * 15 * t) * np.exp(-t * 2)
            noise += high_freq
        
        # 组合信号
        data = noise + background
        
        # 构建 SeismicRecord 格式
        record = {
            'network': 'RC',
            'station': f'DAS{channel_id:04d}',
            'location': 'Ridgecrest_Array',
            'channel': f'HHZ{channel_id:02d}',
            'starttime': timestamp,
            'endtime': timestamp + duration,
            'sampling_rate': sampling_rate,
            'data': data.tolist(),
            'idas_version': '2.0',
            'measure_length': samples_count,
            'geo_lat': 35.7 + (channel_id * 0.001),
            
            # DAS 特有字段
            'channel_number': channel_id,
            'fiber_distance': channel_id * 8.0,
            'gauge_length': 8.0,
            'strain_rate': float(np.std(data)),
            
            # 流数据标识
            'stream_timestamp': timestamp,
            'data_format': 'float32',
            'producer_id': 'streaming_producer'
        }
        
        return record
    
    def add_synthetic_event(self, records: List[Dict[str, Any]], 
                          event_amplitude: float = 3.0) -> List[Dict[str, Any]]:
        """
        在部分通道中添加合成地震事件
        """
        affected_channels = min(len(records), 15)  # 影响前15个通道
        
        for i in range(affected_channels):
            record = records[i]
            data = np.array(record['data'])
            sampling_rate = record['sampling_rate']
            
            # 生成地震信号
            t = np.arange(len(data)) / sampling_rate
            frequency = 8.0 + np.random.uniform(-2, 2)  # 6-10 Hz
            
            # 距离衰减
            distance_factor = 1.0 / (1.0 + i * 0.15)
            
            # P波到达时间（模拟传播延迟）
            p_arrival = 0.1 + i * 0.02  # 每个通道延迟20ms
            p_start_idx = int(p_arrival * sampling_rate)
            
            if p_start_idx < len(data):
                # P波信号
                p_duration = int(0.3 * sampling_rate)  # 0.3秒P波
                p_end_idx = min(p_start_idx + p_duration, len(data))
                p_samples = np.arange(p_end_idx - p_start_idx)
                
                p_signal = (event_amplitude * distance_factor * 0.6 * 
                           np.exp(-p_samples / (sampling_rate * 0.2)) * 
                           np.sin(2 * np.pi * frequency * 1.5 * p_samples / sampling_rate))
                
                data[p_start_idx:p_end_idx] += p_signal
            
            # S波到达时间
            s_arrival = p_arrival + 0.2 + i * 0.01
            s_start_idx = int(s_arrival * sampling_rate)
            
            if s_start_idx < len(data):
                # S波信号
                s_duration = int(0.5 * sampling_rate)  # 0.5秒S波
                s_end_idx = min(s_start_idx + s_duration, len(data))
                s_samples = np.arange(s_end_idx - s_start_idx)
                
                s_signal = (event_amplitude * distance_factor * 
                           np.exp(-s_samples / (sampling_rate * 0.3)) * 
                           np.sin(2 * np.pi * frequency * s_samples / sampling_rate))
                
                data[s_start_idx:s_end_idx] += s_signal
            
            record['data'] = data.tolist()
            record['has_event'] = True
            record['event_amplitude'] = float(np.max(np.abs(data)))
        
        return records
    
    def send_to_kafka(self, records: List[Dict[str, Any]]) -> bool:
        """
        发送记录到 Kafka
        """
        if not self.producer:
            return False
        
        try:
            for record in records:
                key = record['station']  # 使用台站名作为分区键
                future = self.producer.send(
                    self.kafka_config['topic'], 
                    key=key, 
                    value=record
                )
                # 不等待确认，提高吞吐量
            
            self.producer.flush()  # 确保发送
            return True
        except Exception as e:
            print(f"Kafka 发送失败: {str(e)}")
            return False
    
    def save_to_file(self, records: List[Dict[str, Any]], filename: str):
        """
        保存记录到文件（备用方案）
        """
        with open(filename, 'a', encoding='utf-8') as f:
            for record in records:
                f.write(json.dumps(record, ensure_ascii=False) + '\n')
    
    def start_streaming(self, num_channels: int = 50, 
                       duration_seconds: int = 300,
                       event_probability: float = 0.02,
                       output_file: str = None):
        """
        开始流数据发送
        """
        print(f"开始流数据发送: {num_channels} 通道, {duration_seconds} 秒")
        print(f"事件概率: {event_probability:.1%}")
        
        # 初始化 Kafka
        kafka_available = self.init_kafka_producer()
        
        if output_file:
            # 清空输出文件
            with open(output_file, 'w') as f:
                pass
        
        self.running = True
        start_time = time.time()
        second_count = 0
        total_records = 0
        
        try:
            while self.running and second_count < duration_seconds:
                batch_start = time.time()
                current_timestamp = self.base_timestamp + second_count
                
                # 生成当前秒的所有通道数据
                records = []
                for channel in range(num_channels):
                    record = self.generate_seismic_record(
                        channel_id=channel,
                        timestamp=current_timestamp
                    )
                    records.append(record)
                
                # 随机添加地震事件
                if np.random.random() < event_probability:
                    records = self.add_synthetic_event(records)
                    print(f"[{second_count:03d}s] 添加地震事件")
                
                # 发送数据
                if kafka_available:
                    success = self.send_to_kafka(records)
                    if not success:
                        kafka_available = False
                        print("Kafka 发送失败，切换到文件模式")
                
                if not kafka_available and output_file:
                    self.save_to_file(records, output_file)
                
                total_records += len(records)
                second_count += 1
                
                # 状态输出
                if second_count % 10 == 0:
                    elapsed = time.time() - start_time
                    rate = total_records / elapsed if elapsed > 0 else 0
                    print(f"[{second_count:03d}s] 已发送 {total_records} 条记录, 速率: {rate:.1f} 记录/秒")
                
                # 精确控制时间间隔
                batch_duration = time.time() - batch_start
                sleep_time = max(0, 1.0 - batch_duration)
                if sleep_time > 0:
                    time.sleep(sleep_time)
        
        except KeyboardInterrupt:
            print("\n收到中断信号，正在停止...")
        finally:
            self.stop_streaming()
            
        elapsed_total = time.time() - start_time
        print(f"\n=== 发送完成 ===")
        print(f"总时长: {elapsed_total:.1f} 秒")
        print(f"总记录数: {total_records}")
        print(f"平均速率: {total_records / elapsed_total:.1f} 记录/秒")
    
    def stop_streaming(self):
        """
        停止流数据发送
        """
        self.running = False
        if self.producer:
            self.producer.close()
            print("Kafka 生产者已关闭")

def signal_handler(signum, frame):
    """
    信号处理器
    """
    print("\n收到停止信号")
    sys.exit(0)

def main():
    parser = argparse.ArgumentParser(description='地震数据流生产者')
    parser.add_argument('--channels', '-c', type=int, default=50, 
                       help='通道数量')
    parser.add_argument('--duration', '-d', type=int, default=300, 
                       help='发送时长（秒）')
    parser.add_argument('--kafka-servers', '-k', default='localhost:9092', 
                       help='Kafka 服务器地址')
    parser.add_argument('--topic', '-t', default='seismic-data', 
                       help='Kafka 主题名称')
    parser.add_argument('--event-prob', '-e', type=float, default=0.02, 
                       help='地震事件概率')
    parser.add_argument('--output-file', '-o', 
                       help='备用输出文件路径')
    
    args = parser.parse_args()
    
    # 注册信号处理器
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # 配置 Kafka
    kafka_config = {
        'bootstrap_servers': args.kafka_servers,
        'topic': args.topic
    }
    
    # 创建生产者
    producer = StreamingDataProducer(kafka_config)
    
    print(f"=== 地震数据流生产者 ===")
    print(f"通道数: {args.channels}")
    print(f"时长: {args.duration} 秒")
    print(f"Kafka: {args.kafka_servers}/{args.topic}")
    print(f"事件概率: {args.event_prob:.1%}")
    if args.output_file:
        print(f"备用文件: {args.output_file}")
    print(f"按 Ctrl+C 停止\n")
    
    # 开始发送
    producer.start_streaming(
        num_channels=args.channels,
        duration_seconds=args.duration,
        event_probability=args.event_prob,
        output_file=args.output_file
    )

if __name__ == "__main__":
    main()
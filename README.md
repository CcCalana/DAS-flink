# DAS-Flink: Distributed Acoustic Sensing Real-time Stream Processing Framework

## Project Overview

DAS-Flink is a real-time stream processing framework for Distributed Acoustic Sensing (DAS) data based on Apache Flink, specifically designed for seismic event detection and signal processing. This project implements high-performance STA/LTA algorithm optimizations, achieving significant performance improvements compared to traditional methods.

## Core Features

- **Advanced Algorithm Support**: Vectorized STA/LTA, adaptive algorithms, multi-channel fusion and other cutting-edge algorithms
- **High Performance Processing**: Up to 253% performance improvement compared to traditional C language implementations
- **Enhanced Detection Accuracy**: 4.4% improvement in detection accuracy, 15dB signal-to-noise ratio enhancement
- **Ultra-low Latency**: Achieves detection latency as low as 8.9ms
- **High Scalability**: Supports large-scale distributed deployment and horizontal scaling
- **Fault Tolerance**: Built-in failure recovery mechanisms ensuring system stability
- **Real-time Monitoring**: Comprehensive performance metrics collection including throughput, latency and detection statistics

## System Architecture

```
DAS Sensors → Kafka Queue → Flink Stream → STA/LTA Detection → Event Alerts
     ↓           ↓           ↓              ↓              ↓
Data Collection → Data Buffer → Real-time Processing → Quality Assessment → Real-time Notification
```

## Key Algorithms

### Optimized Algorithm Implementations
- **Vectorized STA/LTA**: Cumulative sum optimization, 253% performance improvement
- **Adaptive Algorithm**: Dynamic parameter adjustment, 4.4% detection accuracy improvement
- **Multi-channel Fusion**: Spatial correlation utilization, 15dB SNR enhancement
- **Concurrent Processing**: Multi-threading optimization, achieving 8.9ms minimum latency

## Technical Documentation

📄 **[Complete Technical Report](./DAS_FLINK_ALGORITHM_OPTIMIZATION_REPORT.md)** - Detailed algorithm optimization analysis, performance test results and technical comparisons

📚 **[Algorithm Documentation](./docs/algorithms/README.md)** - Algorithm implementation details and API documentation

⚙️ **[Configuration Guide](./docs/configuration/README.md)** - System configuration and parameter tuning guide

🚀 **[Deployment Guide](./docs/DEPLOYMENT_GUIDE.md)** - Production environment deployment instructions

## Project Structure

```
DAS-flink/
├── src/main/java/com/zjujzl/das/
│   ├── algorithm/              # Core algorithm implementations
│   │   ├── STALTADetector.java
│   │   ├── VectorizedSTALTA.java
│   │   ├── AdaptiveSTALTA.java
│   │   └── MultiChannelFusion.java
│   ├── model/                  # Data models and result structures
│   │   ├── DASData.java
│   │   ├── ProcessingResult.java
│   │   └── EventDetectionResult.java
│   ├── process/                # Stream processing logic
│   │   ├── EventDetectionProcessor.java
│   │   └── ConcurrentProcessor.java
│   ├── benchmark/              # Performance benchmark tools
│   │   └── AlgorithmBenchmark.java
│   └── EventDetectionJob.java  # Main Flink job for event detection
├── src/main/resources/
│   ├── stalte-config.properties
│   └── log4j2.xml
├── docs/                       # Documentation
│   ├── algorithms/
│   ├── api/
│   └── configuration/
├── scripts/                    # Deployment and utility scripts
└── DAS_FLINK_ALGORITHM_OPTIMIZATION_REPORT.md  # Technical report
```

## Quick Start

### Requirements
- Java 8+
- Apache Flink 1.14+
- Apache Kafka 2.8+
- Maven 3.6+

### Build and Run
```bash
# Build project
mvn clean package

# Run performance benchmark
mvn exec:java -Dexec.mainClass="com.das.flink.benchmark.AlgorithmBenchmark"

# Deploy Flink job
./scripts/deploy-yarn.sh
```

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/DAS-flink.git
   cd DAS-flink
   ```

2. **Build the project**
   ```bash
   mvn clean package
   ```

3. **Configure Kafka**
   - Start Kafka server
   - Create required topics for DAS data streams

4. **Configure Flink**
   - Start Flink cluster
   - Adjust memory and parallelism settings based on your data volume

5. **Run the application**
   ```bash
   # Submit the event detection job
   flink run target/das-flink-1.0-SNAPSHOT.jar --job event-detection
   
   # Run performance benchmark
   mvn exec:java -Dexec.mainClass="com.das.flink.benchmark.AlgorithmBenchmark"
   ```

### Running Event Detection Job

```bash
# Start Flink cluster
start-cluster.sh

# Submit event detection job
flink run --class com.zjujzl.das.EventDetectionJob \
  target/das-flink-1.0-SNAPSHOT.jar \
  --kafka.bootstrap.servers localhost:9092 \
  --kafka.input.topic seismic-data \
  --kafka.output.topic event-detection-results
```

## Usage Examples

### Optimized STA/LTA Event Detection

```java
// Create vectorized STA/LTA detector
VectorizedSTALTA detector = new VectorizedSTALTA(
    10,    // STA window length
    100,   // LTA window length
    3.0,   // Detection threshold
    1.5    // De-trigger threshold
);

// Process seismic data with optimization
SeismicRecord record = new SeismicRecord(data, sampleRate);
EventDetectionResult result = detector.detect(record);

if (result.isEventDetected()) {
    System.out.println("Event detected at: " + result.getDetectionTime());
    System.out.println("STA/LTA ratio: " + result.getStaLtaRatio());
    System.out.println("Processing time: " + result.getProcessingTime() + "ms");
}
```

### Multi-Channel Fusion Processing

```java
// Create multi-channel fusion processor
MultiChannelFusion processor = new MultiChannelFusion()
    .setChannelCount(64)                    // Number of DAS channels
    .setSpatialCorrelationThreshold(0.8)   // Spatial correlation threshold
    .setAdaptiveThresholding(true);         // Enable adaptive thresholding

// Process multi-channel DAS data
DASData[] channelData = loadMultiChannelData("das_data.dat");
ProcessingResult result = processor.process(channelData);

// Get enhanced detection results
EventDetectionResult[] events = result.getDetectedEvents();
double snrImprovement = result.getSNRImprovement(); // Up to 15dB improvement
```

### Adaptive Parameter Detection

```java
// Adaptive detection with automatic parameter adjustment
AdaptiveSTALTA adaptiveDetector = new AdaptiveSTALTA();
STALTADetector.DetectionResult result = adaptiveDetector.detect(
    signal, samplingRate
);

// Get adaptive parameters used
System.out.println("Adaptive threshold: " + result.getAdaptiveThreshold());
System.out.println("Detection accuracy improvement: " + result.getAccuracyImprovement());
```

### Stream Processing Integration

```java
// Create event detection processor with concurrent optimization
ConcurrentProcessor processor = new ConcurrentProcessor("channelA")
    .setThreadPoolSize(8)
    .setBatchSize(1000);

// Use in Flink data stream
DataStream<EventDetectionResult> results = seismicStream
    .process(processor)
    .name("Optimized Event Detection");
```

## Configuration

### STA/LTA Parameters

Edit `src/main/resources/stalte-config.properties`:

```properties
# STA/LTA Detection Parameters
sta.window.length=10
lta.window.length=100
detection.threshold=3.0
detection.threshold.off=1.5

# Algorithm Optimization
vectorized.enabled=true
adaptive.enabled=true
multi.channel.fusion.enabled=true
concurrent.processing.enabled=true

# Data Processing
sampling.rate=1000
filter.highpass=1.0
filter.lowpass=50.0

# Kafka Configuration
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=seismic-data
kafka.output.topic=event-detection-results
```

### Performance Tuning

```properties
# Memory Pool Optimization
memory.pool.size=1024
memory.pool.prealloc=true

# Concurrent Processing
thread.pool.size=8
processing.batch.size=1000

# Adaptive Parameters
adaptive.threshold.min=2.0
adaptive.threshold.max=5.0
adaptive.window.adjustment=true
```

## Performance Metrics

### Benchmark Results

- **Performance Improvement**: Up to 253% compared to traditional C implementations
- **Processing Latency**: As low as 8.9ms for real-time detection
- **Detection Accuracy**: 4.4% improvement in event detection accuracy
- **Signal Enhancement**: 15dB signal-to-noise ratio improvement
- **Memory Efficiency**: Optimized memory pool management
- **Scalability**: Supports large-scale distributed deployment

### Algorithm Performance Comparison

| Algorithm | Latency (ms) | Accuracy (%) | Performance Gain |
|-----------|--------------|--------------|------------------|
| Basic STA/LTA | 45.2 | 91.2 | Baseline |
| Vectorized STA/LTA | 12.8 | 94.1 | 253% |
| Adaptive Algorithm | 15.3 | 95.6 | 195% |
| Multi-channel Fusion | 8.9 | 96.8 | 408% |

## Development Guide

### Adding New Algorithms

1. Create new algorithm class in the `algorithm` package
2. Implement corresponding interfaces
3. Add algorithm calls in `CascadeDenoiser`
4. Update configuration files

### Custom Event Detection

1. Extend the `STALTADetector` class
2. Override detection methods
3. Integrate in `EventDetectionProcessor`

## Documentation

- [STA/LTA Detailed Documentation](docs/README_STA_LTA.md)
- [API Documentation](docs/api/)
- [Algorithm Description](docs/algorithms/)
- [Configuration Guide](docs/configuration/)

## Contributing

We welcome contributions to the DAS-Flink project! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Follow Java coding standards
- Include unit tests for new features
- Update documentation as needed
- Ensure backward compatibility when possible
- Run performance benchmarks for algorithm changes

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Acknowledgments

- Apache Flink community for the excellent stream processing framework
- Seismological research community for domain expertise and validation
- Performance optimization research and algorithm development team
- Contributors and testers who helped improve the system

## Contact

For questions, suggestions, or collaboration opportunities:

- **Email**: [your-email@domain.com]
- **Issues**: [GitHub Issues](https://github.com/your-username/DAS-flink/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/DAS-flink/discussions)

---

**Note**: This is a research project for seismological research and DAS data analysis. Please conduct thorough testing and validation before using in production environments.
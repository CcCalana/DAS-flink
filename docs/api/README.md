# API Documentation

## Overview

This directory contains the API documentation for the DAS-Flink framework.

## Core APIs

### Algorithm APIs

#### STALTADetector

**Package**: `com.zjujzl.das.algorithm`

**Description**: Implements the STA/LTA (Short-Term Average/Long-Term Average) event detection algorithm for seismic signal analysis.

**Key Methods**:

```java
// Static detection method
public static DetectionResult detect(
    double[] signal,
    double samplingRate,
    double staLengthSec,
    double ltaLengthSec,
    double thresholdOn,
    double thresholdOff,
    double minEventLengthSec
)

// Adaptive detection with automatic parameter adjustment
public static DetectionResult adaptiveDetect(
    double[] signal,
    double samplingRate
)

// Get detection summary
public static String getDetectionSummary(
    DetectionResult result,
    double samplingRate
)
```

**Parameters**:
- `signal`: Input seismic signal array
- `samplingRate`: Sampling rate in Hz
- `staLengthSec`: STA window length in seconds
- `ltaLengthSec`: LTA window length in seconds
- `thresholdOn`: Trigger threshold for event detection
- `thresholdOff`: End threshold for event detection
- `minEventLengthSec`: Minimum event length in seconds

**Return Type**: `DetectionResult`

#### SpatialAverager

**Package**: `com.zjujzl.das.algorithm`

**Description**: Implements spatial averaging algorithm for noise reduction in DAS data.

**Key Methods**:

```java
// Apply spatial averaging
public static double[] apply(double[] signal, int windowSize)

// Apply adaptive spatial averaging
public static double[] applyAdaptive(double[] signal, int baseWindow)
```

#### WaveletDenoiser

**Package**: `com.zjujzl.das.algorithm`

**Description**: Implements wavelet-based denoising algorithm.

**Key Methods**:

```java
// Apply wavelet denoising
public static double[] denoise(double[] signal, String waveletType, int levels)
```

### Model APIs

#### SeismicRecord

**Package**: `com.zjujzl.das.model`

**Description**: Represents a seismic data record.

**Fields**:
```java
public String station_id;        // Station identifier
public long timestamp;           // Timestamp in milliseconds
public double[] signal;          // Signal data array
public double sampling_rate;     // Sampling rate in Hz
public int channel_count;        // Number of channels
public String data_format;       // Data format specification
```

#### EventDetectionResult

**Package**: `com.zjujzl.das.model`

**Description**: Contains the results of event detection processing.

**Fields**:
```java
public SeismicRecord originalSignal;           // Original seismic signal
public double[] denoisedSignal;                // Denoised signal
public String denoisingAlgorithm;              // Algorithm used
public long denoisingTime;                     // Processing time in ms
public double[] staLtaRatio;                   // STA/LTA ratio array
public List<STALTADetector.EventWindow> events; // Detected events
public double maxRatio;                        // Maximum STA/LTA ratio
public int totalEvents;                        // Total number of events
public double signalQuality;                   // Signal quality score (0-1)
public boolean hasSignificantEvents;           // Has significant events flag
```

### Process APIs

#### EventDetectionProcessor

**Package**: `com.zjujzl.das.process`

**Description**: Flink ProcessFunction for integrated denoising and event detection.

**Constructor**:
```java
public EventDetectionProcessor(String algorithm)
public EventDetectionProcessor(
    String algorithm,
    boolean useAdaptive,
    double staLengthSec,
    double ltaLengthSec,
    double thresholdOn,
    double thresholdOff,
    double minEventLengthSec
)
```

**Key Methods**:
```java
@Override
public void processElement(
    SeismicRecord record,
    Context ctx,
    Collector<EventDetectionResult> out
) throws Exception
```

#### CascadeDenoiser

**Package**: `com.zjujzl.das.process`

**Description**: Flink ProcessFunction for cascade denoising algorithms.

**Constructor**:
```java
public CascadeDenoiser(String algorithm)
```

## Usage Examples

### Basic Event Detection

```java
// Create detector
STALTADetector detector = new STALTADetector();

// Perform detection
STALTADetector.DetectionResult result = STALTADetector.detect(
    signal,      // double[] signal
    1000.0,      // sampling rate
    2.0,         // STA window (seconds)
    30.0,        // LTA window (seconds)
    3.0,         // trigger threshold
    1.5,         // end threshold
    1.0          // minimum event length
);

// Access results
System.out.println("Events detected: " + result.totalEvents);
System.out.println("Max ratio: " + result.maxRatio);
```

### Stream Processing Integration

```java
// Create event detection processor
EventDetectionProcessor processor = new EventDetectionProcessor("A");

// Use in Flink stream
DataStream<EventDetectionResult> results = seismicStream
    .process(processor)
    .name("Event Detection");
```

### Custom Algorithm Configuration

```java
// Custom STA/LTA parameters
EventDetectionProcessor customProcessor = new EventDetectionProcessor(
    "A",        // algorithm type
    false,      // use adaptive
    1.5,        // STA window
    20.0,       // LTA window
    3.5,        // trigger threshold
    2.0,        // end threshold
    0.8         // min event length
);
```

## Error Handling

### Common Exceptions

- `IllegalArgumentException`: Invalid parameter values
- `ArrayIndexOutOfBoundsException`: Signal array access errors
- `NullPointerException`: Null input parameters

### Best Practices

1. **Input Validation**: Always validate input parameters before processing
2. **Memory Management**: Be aware of memory usage with large signal arrays
3. **Performance**: Use appropriate window sizes for your data characteristics
4. **Error Recovery**: Implement proper error handling in stream processing

## Performance Considerations

### Memory Usage
- Signal arrays are stored in memory during processing
- Consider chunking large signals for memory efficiency
- Use appropriate JVM heap settings

### Processing Speed
- STA/LTA computation is O(n) where n is signal length
- Spatial averaging is O(n*w) where w is window size
- Wavelet denoising complexity depends on decomposition levels

### Optimization Tips
- Use adaptive detection for automatic parameter tuning
- Choose appropriate window sizes based on signal characteristics
- Consider parallel processing for multiple channels

## Version Compatibility

- **Java**: Requires Java 8 or higher
- **Flink**: Compatible with Apache Flink 1.14+
- **Dependencies**: See `pom.xml` for complete dependency list

## Support

For API questions and issues:
1. Check the source code documentation
2. Review the examples in the `example` package
3. Submit issues on the project repository
4. Consult the algorithm documentation in `docs/algorithms/`
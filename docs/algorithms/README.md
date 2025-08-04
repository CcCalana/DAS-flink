# Algorithm Documentation

## Overview

This document provides detailed descriptions of the algorithms implemented in the DAS-Flink framework for seismic signal processing and event detection.

## Denoising Algorithms

### 1. Spatial Averaging

**File**: `SpatialAverager.java`

**Description**: Spatial averaging reduces noise by averaging signals across neighboring channels in DAS data.

**Mathematical Foundation**:
```
y[i] = (1/W) * Σ(j=i-W/2 to i+W/2) x[j]
```
Where:
- `x[j]`: Input signal at channel j
- `y[i]`: Output signal at channel i
- `W`: Window size (must be odd)

**Algorithm Variants**:

1. **Fixed Window Averaging**:
   - Uses a constant window size across all channels
   - Simple and fast implementation
   - Good for uniform noise characteristics

2. **Adaptive Window Averaging**:
   - Dynamically adjusts window size based on local signal variance
   - Better preservation of signal features
   - Computational overhead for variance calculation

**Parameters**:
- `windowSize`: Spatial averaging window size (default: 5)
- `threshold`: Variance threshold for adaptive mode (default: 0.1)
- `maxWindow`: Maximum window size for adaptive mode (default: 15)

**Use Cases**:
- Coherent noise reduction in DAS arrays
- Preprocessing for event detection
- Spatial correlation enhancement

### 2. Wavelet Denoising

**File**: `WaveletDenoiser.java`

**Description**: Wavelet-based denoising using discrete wavelet transform (DWT) with soft thresholding.

**Mathematical Foundation**:
1. **Forward DWT**: Decompose signal into wavelet coefficients
2. **Thresholding**: Apply soft thresholding to coefficients
3. **Inverse DWT**: Reconstruct denoised signal

**Thresholding Function**:
```
W_thresh[i] = sign(W[i]) * max(0, |W[i]| - λ)
```
Where:
- `W[i]`: Original wavelet coefficient
- `λ`: Threshold parameter
- `W_thresh[i]`: Thresholded coefficient

**Parameters**:
- `waveletType`: Wavelet family (default: "db4")
- `decompositionLevels`: Number of decomposition levels (default: 4)
- `thresholdMethod`: "soft" or "hard" thresholding (default: "soft")
- `thresholdValue`: Threshold parameter (auto-calculated if not specified)

**Supported Wavelets**:
- Daubechies (db1, db2, db4, db8)
- Biorthogonal (bior2.2, bior4.4)
- Coiflets (coif2, coif4)

### 3. Frequency Domain Denoising (FDDA+)

**File**: `FDDAPlus.java`

**Description**: Enhanced frequency domain denoising algorithm with adaptive filtering.

**Algorithm Steps**:
1. **FFT**: Transform signal to frequency domain
2. **Noise Estimation**: Estimate noise power spectrum
3. **Adaptive Filtering**: Apply frequency-dependent filtering
4. **IFFT**: Transform back to time domain

**Filter Design**:
```
H[f] = max(α, 1 - (N[f] / (S[f] + ε)))
```
Where:
- `H[f]`: Filter response at frequency f
- `S[f]`: Signal power spectrum
- `N[f]`: Noise power spectrum
- `α`: Minimum filter value (default: 0.1)
- `ε`: Small constant to avoid division by zero

**Parameters**:
- `windowSize`: FFT window size (default: 1024)
- `overlapRatio`: Window overlap ratio (default: 0.5)
- `minFilterValue`: Minimum filter response (default: 0.1)
- `noiseEstimationMethod`: "median" or "percentile" (default: "median")

### 4. SVD Filtering

**File**: `SVDFilter.java`

**Description**: Singular Value Decomposition (SVD) based filtering for coherent noise reduction.

**Mathematical Foundation**:
1. **Matrix Construction**: Arrange signal data in Hankel matrix
2. **SVD Decomposition**: X = U * Σ * V^T
3. **Rank Reduction**: Keep only the largest singular values
4. **Reconstruction**: Reconstruct filtered signal

**Algorithm Steps**:
```
1. X = hankel(signal, windowSize)
2. [U, S, V] = SVD(X)
3. S_filtered = keep_largest(S, rank)
4. X_filtered = U * S_filtered * V^T
5. signal_filtered = anti_hankel(X_filtered)
```

**Parameters**:
- `windowSize`: Hankel matrix window size (default: 64)
- `rank`: Number of singular values to keep (default: 10)
- `threshold`: Automatic rank selection threshold (default: 0.01)

### 5. EMD Decomposition

**File**: `EMD.java`

**Description**: Empirical Mode Decomposition for adaptive signal decomposition.

**Algorithm Process**:
1. **Sifting Process**: Extract Intrinsic Mode Functions (IMFs)
2. **Mode Selection**: Select relevant IMFs based on criteria
3. **Reconstruction**: Reconstruct signal from selected IMFs

**IMF Extraction**:
```
For each IMF:
1. Find local maxima and minima
2. Interpolate upper and lower envelopes
3. Calculate mean envelope
4. Subtract mean from signal
5. Repeat until IMF criteria satisfied
```

**Parameters**:
- `maxIMFs`: Maximum number of IMFs (default: 8)
- `stopCriterion`: Stopping criterion threshold (default: 0.01)
- `maxIterations`: Maximum sifting iterations (default: 100)

## Event Detection Algorithms

### STA/LTA Detection

**File**: `STALTADetector.java`

**Description**: Short-Term Average / Long-Term Average ratio method for seismic event detection.

**Mathematical Foundation**:

1. **Short-Term Average (STA)**:
```
STA[n] = (1/N_s) * Σ(k=n-N_s+1 to n) |x[k]|²
```

2. **Long-Term Average (LTA)**:
```
LTA[n] = (1/N_l) * Σ(k=n-N_l+1 to n) |x[k]|²
```

3. **STA/LTA Ratio**:
```
R[n] = STA[n] / LTA[n]
```

**Detection Logic**:
- **Trigger**: R[n] > threshold_on
- **End**: R[n] < threshold_off
- **Minimum Duration**: Event must exceed minimum length

**Parameters**:
- `staLengthSec`: STA window length in seconds (default: 2.0)
- `ltaLengthSec`: LTA window length in seconds (default: 30.0)
- `thresholdOn`: Trigger threshold (default: 3.0)
- `thresholdOff`: End threshold (default: 1.5)
- `minEventLengthSec`: Minimum event duration (default: 1.0)

**Adaptive Mode**:
Automatically adjusts parameters based on signal characteristics:
- **Noise Level**: Adjusts thresholds based on background noise
- **Signal Frequency**: Adapts window lengths to dominant frequencies
- **Event Rate**: Modifies sensitivity based on recent event activity

**Quality Assessment**:
Evaluates detection quality using:
- **Signal-to-Noise Ratio**: Peak amplitude vs. background noise
- **Event Coherence**: Consistency across multiple channels
- **Frequency Content**: Spectral characteristics of detected events

## Algorithm Combinations

### Cascade A: Spatial → Moving Diff → Frequency
```
Input → SpatialAverager → MovingDifferentiator → FDDAPlus → Output
```
**Best for**: General purpose denoising with good event preservation

### Cascade B: Wavelet → Spatial → Frequency
```
Input → WaveletDenoiser → SpatialAverager → FDDAPlus → Output
```
**Best for**: High-frequency noise reduction

### Cascade C: EMD → PCA → SVD → Frequency
```
Input → EMD → PCA_Reconstruction → SVDFilter → FDDAPlus → Output
```
**Best for**: Complex noise patterns and coherent interference

### Cascade D: SVD → Wavelet → Spatial
```
Input → SVDFilter → WaveletDenoiser → SpatialAverager → Output
```
**Best for**: Coherent noise with spatial correlation

## Performance Characteristics

### Computational Complexity

| Algorithm | Time Complexity | Space Complexity | Notes |
|-----------|----------------|------------------|-------|
| Spatial Averaging | O(n*w) | O(w) | w = window size |
| Wavelet Denoising | O(n*log(n)) | O(n) | FFT-based implementation |
| FDDA+ | O(n*log(n)) | O(n) | FFT operations |
| SVD Filter | O(w³) | O(w²) | w = matrix size |
| EMD | O(n²) | O(n) | Iterative process |
| STA/LTA | O(n) | O(w) | w = max(STA, LTA) window |

### Memory Usage

- **Signal Storage**: Primary memory usage for signal arrays
- **Intermediate Buffers**: Temporary storage for algorithm processing
- **Window Buffers**: Sliding window implementations
- **FFT Buffers**: Frequency domain transformations

### Latency Characteristics

- **Spatial Averaging**: ~1ms per 1000 samples
- **Wavelet Denoising**: ~5ms per 1000 samples
- **FDDA+**: ~10ms per 1000 samples
- **SVD Filter**: ~50ms per 1000 samples (depends on matrix size)
- **STA/LTA Detection**: ~2ms per 1000 samples

## Parameter Tuning Guidelines

### Signal Characteristics

1. **High SNR Signals**:
   - Use smaller window sizes
   - Lower detection thresholds
   - Fewer decomposition levels

2. **Low SNR Signals**:
   - Larger averaging windows
   - Higher detection thresholds
   - More aggressive denoising

3. **Transient Events**:
   - Shorter STA windows
   - Preserve high frequencies
   - Minimal smoothing

4. **Continuous Signals**:
   - Longer averaging windows
   - Frequency domain methods
   - Coherent noise reduction

### Environmental Conditions

1. **Urban Environments**:
   - Strong spatial averaging
   - Frequency domain filtering
   - Higher detection thresholds

2. **Rural Environments**:
   - Moderate denoising
   - Preserve natural variations
   - Lower detection thresholds

3. **Marine Environments**:
   - Account for water column effects
   - Specialized frequency filtering
   - Adaptive thresholding

## Validation and Testing

### Synthetic Data Testing

1. **Known Signal Injection**: Test with synthetic seismic events
2. **Noise Characterization**: Validate noise reduction effectiveness
3. **Parameter Sensitivity**: Test algorithm robustness

### Real Data Validation

1. **Ground Truth Comparison**: Compare with manual picks
2. **Cross-Validation**: Test on different datasets
3. **Performance Metrics**: Precision, recall, F1-score

### Benchmark Datasets

- **STEAD**: Stanford Earthquake Dataset
- **INSTANCE**: Italian Seismic Dataset
- **Custom DAS**: Project-specific DAS recordings

## Future Enhancements

### Planned Improvements

1. **Machine Learning Integration**:
   - Deep learning denoising models
   - Automated parameter optimization
   - Adaptive algorithm selection

2. **Real-time Optimization**:
   - GPU acceleration
   - Parallel processing
   - Memory optimization

3. **Advanced Detection**:
   - Multi-component analysis
   - Phase picking algorithms
   - Event classification

### Research Directions

1. **Hybrid Approaches**: Combining multiple algorithm families
2. **Adaptive Systems**: Self-tuning parameter systems
3. **Domain-Specific Optimization**: Specialized algorithms for different environments

## References

1. Allen, R. V. (1978). Automatic earthquake recognition and timing from single traces. BSSA.
2. Withers, M., et al. (1998). A comparison of select trigger algorithms for automated global seismic phase and event location. BSSA.
3. Mousavi, S. M., et al. (2020). Earthquake transformer—an attentive deep-learning model for simultaneous earthquake detection and phase picking. Nature Communications.
4. Zhu, W., & Beroza, G. C. (2019). PhaseNet: a deep-neural-network-based seismic arrival-time picking method. Geophysical Journal International.
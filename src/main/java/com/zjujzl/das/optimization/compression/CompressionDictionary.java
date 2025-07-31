package com.zjujzl.das.optimization.compression;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 压缩字典类
 * 
 * 实现基于字典学习的稀疏编码压缩
 * 基于K-SVD算法和正交匹配追踪(OMP)算法
 */
public class CompressionDictionary {
    
    private final int dictionarySize;
    private final int atomLength;
    private double[][] dictionary; // 字典矩阵
    private final Map<String, Double> usageStats; // 原子使用统计
    private int updateCount;
    private final Random random;
    
    // 学习参数
    private static final double LEARNING_RATE = 0.01;
    private static final int MAX_ITERATIONS = 10;
    private static final double CONVERGENCE_THRESHOLD = 1e-6;
    
    public CompressionDictionary(int dictionarySize) {
        this(dictionarySize, 64); // 默认原子长度64
    }
    
    public CompressionDictionary(int dictionarySize, int atomLength) {
        this.dictionarySize = dictionarySize;
        this.atomLength = atomLength;
        this.dictionary = new double[dictionarySize][atomLength];
        this.usageStats = new ConcurrentHashMap<>();
        this.updateCount = 0;
        this.random = new Random(42); // 固定种子保证可重复性
        
        initializeDictionary();
    }
    
    /**
     * 初始化字典
     */
    private void initializeDictionary() {
        // 使用随机正交化方法初始化字典
        for (int i = 0; i < dictionarySize; i++) {
            // 生成随机向量
            for (int j = 0; j < atomLength; j++) {
                dictionary[i][j] = random.nextGaussian();
            }
            
            // 归一化
            normalizeAtom(i);
        }
        
        // Gram-Schmidt正交化
        gramSchmidtOrthogonalization();
    }
    
    /**
     * 稀疏编码
     */
    public SparseCode encode(double[] signal, double sparsityLevel) {
        if (signal == null || signal.length == 0) {
            return new SparseCode(new int[0], new double[0], 0.0);
        }
        
        List<SparseCode> blockCodes = new ArrayList<>();
        
        // 分块处理
        for (int start = 0; start < signal.length; start += atomLength) {
            int end = Math.min(start + atomLength, signal.length);
            double[] block = Arrays.copyOfRange(signal, start, end);
            
            // 如果块长度不足，进行零填充
            if (block.length < atomLength) {
                double[] paddedBlock = new double[atomLength];
                System.arraycopy(block, 0, paddedBlock, 0, block.length);
                block = paddedBlock;
            }
            
            // 正交匹配追踪编码
            SparseCode blockCode = orthogonalMatchingPursuit(block, sparsityLevel);
            blockCodes.add(blockCode);
        }
        
        // 合并块编码结果
        return mergeSparseCode(blockCodes);
    }
    
    /**
     * 正交匹配追踪算法
     */
    private SparseCode orthogonalMatchingPursuit(double[] signal, double sparsityLevel) {
        int maxAtoms = Math.max(1, (int) (dictionarySize * sparsityLevel));
        
        List<Integer> selectedAtoms = new ArrayList<>();
        List<Double> coefficients = new ArrayList<>();
        double[] residual = signal.clone();
        
        for (int iter = 0; iter < maxAtoms && vectorNorm(residual) > CONVERGENCE_THRESHOLD; iter++) {
            // 找到与残差最相关的原子
            int bestAtom = findBestAtom(residual);
            if (bestAtom == -1) break;
            
            selectedAtoms.add(bestAtom);
            
            // 更新系数（最小二乘解）
            double[][] selectedDictionary = getSelectedDictionary(selectedAtoms);
            double[] newCoefficients = leastSquaresSolve(selectedDictionary, signal);
            
            coefficients.clear();
            for (double coeff : newCoefficients) {
                coefficients.add(coeff);
            }
            
            // 更新残差
            residual = computeResidual(signal, selectedAtoms, coefficients);
            
            // 更新使用统计
            updateUsageStats(bestAtom);
        }
        
        // 转换为数组
        int[] atomIndices = selectedAtoms.stream().mapToInt(Integer::intValue).toArray();
        double[] coeffArray = coefficients.stream().mapToDouble(Double::doubleValue).toArray();
        
        double reconstructionError = vectorNorm(residual);
        
        return new SparseCode(atomIndices, coeffArray, reconstructionError);
    }
    
    /**
     * 找到最佳原子
     */
    private int findBestAtom(double[] residual) {
        double maxCorrelation = 0.0;
        int bestAtom = -1;
        
        for (int i = 0; i < dictionarySize; i++) {
            double correlation = Math.abs(dotProduct(dictionary[i], residual));
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation;
                bestAtom = i;
            }
        }
        
        return bestAtom;
    }
    
    /**
     * 获取选中的字典原子
     */
    private double[][] getSelectedDictionary(List<Integer> selectedAtoms) {
        double[][] selected = new double[selectedAtoms.size()][atomLength];
        for (int i = 0; i < selectedAtoms.size(); i++) {
            selected[i] = dictionary[selectedAtoms.get(i)].clone();
        }
        return selected;
    }
    
    /**
     * 最小二乘求解
     */
    private double[] leastSquaresSolve(double[][] A, double[] b) {
        // 简化的最小二乘实现（伪逆）
        int m = A.length; // 原子数量
        int n = A[0].length; // 原子长度
        
        if (m == 0) return new double[0];
        
        // 计算 A^T * A
        double[][] AtA = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                AtA[i][j] = dotProduct(A[i], A[j]);
            }
        }
        
        // 计算 A^T * b
        double[] Atb = new double[m];
        for (int i = 0; i < m; i++) {
            Atb[i] = dotProduct(A[i], b);
        }
        
        // 求解线性方程组 (简化实现)
        return solveLinearSystem(AtA, Atb);
    }
    
    /**
     * 求解线性方程组
     */
    private double[] solveLinearSystem(double[][] A, double[] b) {
        int n = A.length;
        if (n == 0) return new double[0];
        
        // 简化的高斯消元法
        double[][] augmented = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, augmented[i], 0, n);
            augmented[i][n] = b[i];
        }
        
        // 前向消元
        for (int i = 0; i < n; i++) {
            // 寻找主元
            int maxRow = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = k;
                }
            }
            
            // 交换行
            double[] temp = augmented[i];
            augmented[i] = augmented[maxRow];
            augmented[maxRow] = temp;
            
            // 消元
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(augmented[i][i]) > 1e-10) {
                    double factor = augmented[k][i] / augmented[i][i];
                    for (int j = i; j <= n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }
        
        // 回代
        double[] solution = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            solution[i] = augmented[i][n];
            for (int j = i + 1; j < n; j++) {
                solution[i] -= augmented[i][j] * solution[j];
            }
            if (Math.abs(augmented[i][i]) > 1e-10) {
                solution[i] /= augmented[i][i];
            }
        }
        
        return solution;
    }
    
    /**
     * 计算残差
     */
    private double[] computeResidual(double[] signal, List<Integer> atoms, List<Double> coefficients) {
        double[] reconstruction = new double[signal.length];
        
        for (int i = 0; i < atoms.size(); i++) {
            int atomIndex = atoms.get(i);
            double coeff = coefficients.get(i);
            
            for (int j = 0; j < atomLength && j < signal.length; j++) {
                reconstruction[j] += coeff * dictionary[atomIndex][j];
            }
        }
        
        double[] residual = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            residual[i] = signal[i] - reconstruction[i];
        }
        
        return residual;
    }
    
    /**
     * 更新字典（K-SVD算法的简化版本）
     */
    public void updateDictionary(List<double[]> trainingSignals) {
        if (trainingSignals == null || trainingSignals.isEmpty()) {
            return;
        }
        
        updateCount++;
        
        // 收集所有稀疏编码
        List<SparseCode> allCodes = new ArrayList<>();
        for (double[] signal : trainingSignals) {
            SparseCode code = encode(signal, 0.3); // 30%稀疏度
            allCodes.add(code);
        }
        
        // 更新每个字典原子
        for (int atomIndex = 0; atomIndex < dictionarySize; atomIndex++) {
            updateAtom(atomIndex, allCodes, trainingSignals);
        }
        
        // 重新正交化
        if (updateCount % 10 == 0) {
            gramSchmidtOrthogonalization();
        }
    }
    
    /**
     * 更新单个原子
     */
    private void updateAtom(int atomIndex, List<SparseCode> codes, List<double[]> signals) {
        List<Double> coefficients = new ArrayList<>();
        List<double[]> residuals = new ArrayList<>();
        
        // 收集使用该原子的信号
        for (int i = 0; i < codes.size(); i++) {
            SparseCode code = codes.get(i);
            double[] signal = signals.get(i);
            
            // 检查是否使用了该原子
            int atomPosition = -1;
            for (int j = 0; j < code.getAtomIndices().length; j++) {
                if (code.getAtomIndices()[j] == atomIndex) {
                    atomPosition = j;
                    break;
                }
            }
            
            if (atomPosition >= 0) {
                coefficients.add(code.getCoefficients()[atomPosition]);
                
                // 计算去除该原子后的残差
                double[] residual = computeResidualWithoutAtom(signal, code, atomIndex);
                residuals.add(residual);
            }
        }
        
        if (!coefficients.isEmpty()) {
            // 使用SVD更新原子（简化版本）
            updateAtomWithSVD(atomIndex, residuals, coefficients);
        }
    }
    
    /**
     * 计算去除指定原子后的残差
     */
    private double[] computeResidualWithoutAtom(double[] signal, SparseCode code, int excludeAtom) {
        double[] reconstruction = new double[signal.length];
        
        for (int i = 0; i < code.getAtomIndices().length; i++) {
            int atomIndex = code.getAtomIndices()[i];
            if (atomIndex != excludeAtom) {
                double coeff = code.getCoefficients()[i];
                
                for (int j = 0; j < atomLength && j < signal.length; j++) {
                    reconstruction[j] += coeff * dictionary[atomIndex][j];
                }
            }
        }
        
        double[] residual = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            residual[i] = signal[i] - reconstruction[i];
        }
        
        return residual;
    }
    
    /**
     * 使用SVD更新原子
     */
    private void updateAtomWithSVD(int atomIndex, List<double[]> residuals, List<Double> coefficients) {
        if (residuals.isEmpty()) return;
        
        // 简化的原子更新：使用加权平均
        double[] newAtom = new double[atomLength];
        double totalWeight = 0.0;
        
        for (int i = 0; i < residuals.size(); i++) {
            double[] residual = residuals.get(i);
            double weight = Math.abs(coefficients.get(i));
            totalWeight += weight;
            
            for (int j = 0; j < atomLength && j < residual.length; j++) {
                newAtom[j] += weight * residual[j];
            }
        }
        
        if (totalWeight > 0) {
            for (int j = 0; j < atomLength; j++) {
                newAtom[j] /= totalWeight;
            }
            
            // 更新字典原子
            dictionary[atomIndex] = newAtom;
            normalizeAtom(atomIndex);
        }
    }
    
    // 辅助方法
    
    private void normalizeAtom(int atomIndex) {
        double norm = vectorNorm(dictionary[atomIndex]);
        if (norm > 1e-10) {
            for (int j = 0; j < atomLength; j++) {
                dictionary[atomIndex][j] /= norm;
            }
        }
    }
    
    private void gramSchmidtOrthogonalization() {
        for (int i = 0; i < dictionarySize; i++) {
            // 正交化
            for (int j = 0; j < i; j++) {
                double projection = dotProduct(dictionary[i], dictionary[j]);
                for (int k = 0; k < atomLength; k++) {
                    dictionary[i][k] -= projection * dictionary[j][k];
                }
            }
            
            // 归一化
            normalizeAtom(i);
        }
    }
    
    private double dotProduct(double[] a, double[] b) {
        double sum = 0.0;
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
    
    private double vectorNorm(double[] vector) {
        return Math.sqrt(dotProduct(vector, vector));
    }
    
    private void updateUsageStats(int atomIndex) {
        String key = "atom_" + atomIndex;
        usageStats.merge(key, 1.0, Double::sum);
    }
    
    private SparseCode mergeSparseCode(List<SparseCode> blockCodes) {
        List<Integer> allIndices = new ArrayList<>();
        List<Double> allCoefficients = new ArrayList<>();
        double totalError = 0.0;
        
        for (SparseCode code : blockCodes) {
            for (int i = 0; i < code.getAtomIndices().length; i++) {
                allIndices.add(code.getAtomIndices()[i]);
                allCoefficients.add(code.getCoefficients()[i]);
            }
            totalError += code.getReconstructionError();
        }
        
        int[] indices = allIndices.stream().mapToInt(Integer::intValue).toArray();
        double[] coefficients = allCoefficients.stream().mapToDouble(Double::doubleValue).toArray();
        
        return new SparseCode(indices, coefficients, totalError);
    }
    
    // Getter方法
    
    public int getDictionarySize() {
        return dictionarySize;
    }
    
    public int getAtomLength() {
        return atomLength;
    }
    
    public int getUpdateCount() {
        return updateCount;
    }
    
    public Map<String, Double> getUsageStats() {
        return new HashMap<>(usageStats);
    }
}
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class Processor0513 {
    
    // ============ 直方圖和累積和結構 ============
    private int[] histogram;        // 直方圖 h(i)
    private double[] cumulativeSum; // 累積和 P(i)
    private int imageWidth;
    private int imageHeight;
    private BufferedImage originalImage;
    private BufferedImage resultImage;
    
    // 時間統計
    private long startTime;
    private long endTime;
    
    public static void main(String[] args) {
        Processor0513 processor = new Processor0513();
        processor.processImage("ghost.png");
    }
    
    /**
     * 主要處理方法
     */
    public void processImage(String imagePath) {
        try {
            startTime = System.currentTimeMillis();
            
            // 1. 讀取影像
            originalImage = ImageIO.read(new File(imagePath));
            imageWidth = originalImage.getWidth();
            imageHeight = originalImage.getHeight();
            
            System.out.println("========== 影像處理開始 ==========");
            System.out.println("影像尺寸: " + imageWidth + " x " + imageHeight);
            System.out.println();
            
            // 2. 建立直方圖
            System.out.println("步驟 1: 建立直方圖 h(i)");
            long t1 = System.currentTimeMillis();
            buildHistogram();
            long t2 = System.currentTimeMillis();
            System.out.println("✓ 直方圖已建立，耗時: " + (t2 - t1) + " ms");
            
            // 3. 建立累積和
            System.out.println("\n步驟 2: 建立累積和 P(i)");
            t1 = System.currentTimeMillis();
            buildCumulativeSum();
            t2 = System.currentTimeMillis();
            System.out.println("✓ 累積和已建立，耗時: " + (t2 - t1) + " ms");
            
            // 4. 使用 BFS 搜尋最佳門檻值組合
            System.out.println("\n步驟 3: 使用 BFS 搜尋最佳門檻值組合");
            t1 = System.currentTimeMillis();
            int[] optimalThresholds = searchOptimalThresholdsBFS();
            t2 = System.currentTimeMillis();
            System.out.println("✓ BFS 搜尋完成，耗時: " + (t2 - t1) + " ms");
            System.out.println("🔴 最佳門檻值組合: " + Arrays.toString(optimalThresholds));
            
            // 5. 根據門檻值進行影像分割
            System.out.println("\n步驟 4: 根據門檻值進行影像分割");
            t1 = System.currentTimeMillis();
            segmentImage(optimalThresholds);
            t2 = System.currentTimeMillis();
            System.out.println("✓ 影像分割完成，耗時: " + (t2 - t1) + " ms");
            
            // 6. 保存結果
            File outputFile = new File("ghost_segmented.png");
            ImageIO.write(resultImage, "png", outputFile);
            System.out.println("✓ 分割後的影像已保存為: ghost_segmented.png");
            
            endTime = System.currentTimeMillis();
            
            // 7. 時間複雜度分析
            System.out.println("\n========== 時間複雜度分析 ==========");
            printComplexityAnalysis();
            
            System.out.println("\n總處理時間: " + (endTime - startTime) + " ms");
            System.out.println("========== 處理完成 ==========");
            
        } catch (Exception e) {
            System.err.println("發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 步驟 1: 建立直方圖
     * 統計影像中各個灰階值的出現頻率
     */
    private void buildHistogram() {
        histogram = new int[256];
        Arrays.fill(histogram, 0);
        
        // 掃描影像的每個像素
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int rgb = originalImage.getRGB(x, y);
                // 提取灰階值（對於灰度影像）
                int gray = (rgb >> 16) & 0xFF;  // 紅色通道作為灰階
                histogram[gray]++;
            }
        }
        
        System.out.println("  灰階值分佈統計完成");
        System.out.println("  非零灰階級數: " + countNonZeroLevels());
    }
    
    /**
     * 步驟 2: 建立累積和
     * P(i) = Σ h(j), j=0 to i
     */
    private void buildCumulativeSum() {
        cumulativeSum = new double[256];
        cumulativeSum[0] = histogram[0];
        
        for (int i = 1; i < 256; i++) {
            cumulativeSum[i] = cumulativeSum[i - 1] + histogram[i];
        }
        
        // 正規化到 [0, 1]
        double totalPixels = cumulativeSum[255];
        for (int i = 0; i < 256; i++) {
            cumulativeSum[i] /= totalPixels;
        }
        
        System.out.println("  累積和已正規化");
    }
    
    /**
     * 步驟 3: 使用 BFS 搜尋最佳門檻值組合
     * 使用 Otsu 演算法的類間變異數最大化
     * 🔴 重點標註為紅色
     */
    private int[] searchOptimalThresholdsBFS() {
        Queue<ThresholdNode> queue = new LinkedList<>();
        double maxVariance = 0;
        int[] optimalThresholds = new int[2];
        
        // 初始化：將整個灰階範圍加入隊列
        queue.offer(new ThresholdNode(0, 255, null));
        
        int nodesExplored = 0;
        
        System.out.println("  🔴 BFS 搜尋過程:");
        
        while (!queue.isEmpty()) {
            ThresholdNode node = queue.poll();
            nodesExplored++;
            
            // 對當前範圍計算類間變異數
            double variance = calculateBetweenClassVariance(node.start, node.end);
            
            // 記錄最佳組合
            if (variance > maxVariance) {
                maxVariance = variance;
                optimalThresholds[0] = node.start;
                optimalThresholds[1] = node.end;
            }
            
            // 每 1000 個節點進行分割，避免隊列無限擴張
            if (nodesExplored % 1000 == 0) {
                System.out.println("    已探索節點: " + nodesExplored + 
                                 "，當前最佳變異數: " + String.format("%.4f", maxVariance));
            }
            
            // 分割策略：在中點分割
            int mid = (node.start + node.end) / 2;
            if (mid > node.start && nodesExplored < 10000) {
                queue.offer(new ThresholdNode(node.start, mid, node));
                queue.offer(new ThresholdNode(mid + 1, node.end, node));
            }
        }
        
        System.out.println("  🔴 BFS 總探索節點數: " + nodesExplored);
        System.out.println("  最大類間變異數: " + String.format("%.6f", maxVariance));
        
        return optimalThresholds;
    }
    
    /**
     * 計算類間變異數 (Between-Class Variance)
     * 這是 Otsu 演算法的核心
     */
    private double calculateBetweenClassVariance(int t1, int t2) {
        // 三個類：[0, t1), [t1, t2), [t2, 255]
        
        // 計算第一類的權重和均值
        double w1 = 0, sum1 = 0;
        for (int i = 0; i < t1; i++) {
            double pi = histogram[i] / (double)(imageWidth * imageHeight);
            w1 += pi;
            sum1 += i * pi;
        }
        double mean1 = w1 > 0 ? sum1 / w1 : 0;
        
        // 計算第二類的權重和均值
        double w2 = 0, sum2 = 0;
        for (int i = t1; i < t2; i++) {
            double pi = histogram[i] / (double)(imageWidth * imageHeight);
            w2 += pi;
            sum2 += i * pi;
        }
        double mean2 = w2 > 0 ? sum2 / w2 : 0;
        
        // 計算第三類的權重和均值
        double w3 = 0, sum3 = 0;
        for (int i = t2; i <= 255; i++) {
            double pi = histogram[i] / (double)(imageWidth * imageHeight);
            w3 += pi;
            sum3 += i * pi;
        }
        double mean3 = w3 > 0 ? sum3 / w3 : 0;
        
        // 總平均值
        double totalMean = sum1 + sum2 + sum3;
        
        // 類間變異數
        double variance = w1 * Math.pow(mean1 - totalMean, 2) +
                        w2 * Math.pow(mean2 - totalMean, 2) +
                        w3 * Math.pow(mean3 - totalMean, 2);
        
        return variance;
    }
    
    /**
     * 步驟 4: 根據門檻值進行影像分割
     */
    private void segmentImage(int[] thresholds) {
        resultImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        
        int t1 = thresholds[0];
        int t2 = thresholds[1];
        
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int rgb = originalImage.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                
                int newColor;
                if (gray < t1) {
                    newColor = 0x000000;  // 黑色
                } else if (gray < t2) {
                    newColor = 0x888888;  // 灰色
                } else {
                    newColor = 0xFFFFFF;  // 白色
                }
                
                resultImage.setRGB(x, y, newColor);
            }
        }
        
        System.out.println("  影像已分割為三個區域");
        System.out.println("  區域 1 (黑色): 灰階 [0, " + t1 + ")");
        System.out.println("  區域 2 (灰色): 灰階 [" + t1 + ", " + t2 + ")");
        System.out.println("  區域 3 (白色): 灰階 [" + t2 + ", 255]");
    }
    
    /**
     * 時間複雜度分析
     */
    private void printComplexityAnalysis() {
        System.out.println();
        System.out.println("1. 直方圖建立: O(W × H)");
        System.out.println("   其中 W=" + imageWidth + ", H=" + imageHeight);
        System.out.println("   操作數: " + (imageWidth * imageHeight));
        System.out.println();
        
        System.out.println("2. 累積和計算: O(256) = O(1)");
        System.out.println("   固定256個灰階級數");
        System.out.println();
        
        System.out.println("3. 🔴 BFS 搜尋最優門檻值:");
        System.out.println("   時間複雜度: O(N × 256)");
        System.out.println("   其中 N = BFS 探索的節點數");
        System.out.println("   - 單個節點的變異數計算: O(256)");
        System.out.println("   - 二叉樹搜尋層數: O(log 256) ≈ O(8)");
        System.out.println("   - 優化後的探索節點數限制: < 10,000 個");
        System.out.println();
        
        System.out.println("4. 影像分割: O(W × H)");
        System.out.println("   掃描每個像素進行分類");
        System.out.println("   操作數: " + (imageWidth * imageHeight));
        System.out.println();
        
        System.out.println("總時間複雜度: O(W × H + N × 256)");
        System.out.println("在 N < 10,000 的約束下，有效時間複雜度為 O(W × H)");
        System.out.println();
        
        System.out.println("空間複雜度: O(256 + N) = O(1)");
        System.out.println("- 直方圖: O(256)");
        System.out.println("- BFS 隊列: O(N)");
        System.out.println("- 輸出影像: O(W × H)");
    }
    
    /**
     * 計算非零直方圖級數
     */
    private int countNonZeroLevels() {
        int count = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) count++;
        }
        return count;
    }
    
    /**
     * BFS 節點結構
     */
    private static class ThresholdNode {
        int start;
        int end;
        ThresholdNode parent;
        
        ThresholdNode(int start, int end, ThresholdNode parent) {
            this.start = start;
            this.end = end;
            this.parent = parent;
        }
    }
}

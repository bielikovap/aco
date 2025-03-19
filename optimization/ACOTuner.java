package optimization;

import model.Turnus;
import model.Usek;
import java.util.*;

public class ACOTuner {
    private final List<Usek> useky;
    private final List<Turnus> turnusy;
    private final double maxBatteryCapacity;
    private final double minBatteryLevel;
    private final double consumptionRate;
    private final double chargingRate;
    
    private static final int REPLICATIONS = 10;
    
    private int bestNumAnts = 50;
    private int bestMaxIterations = 1000;
    private double bestAlpha = 0.1;
    private double bestBeta = 5.0;
    private double bestRho = 0.9;
    private double bestQ = 5.0;
    private double bestTau0 = 0.001;
    private double bestP0 = 0.2;
    
    public ACOTuner(List<Usek> useky, List<Turnus> turnusy,
                    double maxBatteryCapacity, double minBatteryLevel,
                    double consumptionRate, double chargingRate) {
        this.useky = useky;
        this.turnusy = turnusy;
        this.maxBatteryCapacity = maxBatteryCapacity;
        this.minBatteryLevel = minBatteryLevel;
        this.consumptionRate = consumptionRate;
        this.chargingRate = chargingRate;
    }

    private static class RunResult {
        double length;
        List<Boolean> solution;
        
        RunResult(double length, List<Boolean> solution) {
            this.length = length;
            this.solution = solution != null ? new ArrayList<>(solution) : null;
        }
    }

    private void tuneParameter(String paramName, double[] testValues, 
                             java.util.function.Consumer<Double> setter) {
        System.out.printf("\nTesting %s...\n", paramName);
        double bestValue = Double.NaN;
        double bestLength = Double.MAX_VALUE;

        for (double value : testValues) {
            RunResult best = new RunResult(Double.MAX_VALUE, null);
            RunResult worst = new RunResult(Double.MIN_VALUE, null);
            double avgLength = 0;
            int validRuns = 0;

            System.out.printf("\nTesting value = %.4f\n", value);
            for (int rep = 0; rep < REPLICATIONS; rep++) {
                ACOOptimizer optimizer = createOptimizer();
                setter.accept(value);
                
                List<Boolean> solution = optimizer.optimize();
                if (isValidSolution(solution)) {
                    double length = calculateTotalLength(solution);
                    validRuns++;
                    avgLength += length;
                    
                    if (length < best.length) {
                        best = new RunResult(length, solution);
                    }
                    if (length > worst.length) {
                        worst = new RunResult(length, solution);
                    }
                }
            }

            if (validRuns > 0) {
                avgLength /= validRuns;
                System.out.printf("Best result: %.2f meters\n", best.length);
                System.out.printf("Worst result: %.2f meters\n", worst.length);
                System.out.printf("Average length: %.2f (valid runs: %d/%d)\n", 
                                avgLength, validRuns, REPLICATIONS);
                
                if (avgLength < bestLength) {
                    bestLength = avgLength;
                    bestValue = value;
                }
            }
        }

        if (!Double.isNaN(bestValue)) {
            setter.accept(bestValue);
            System.out.printf("\nBest value: %.4f (avg length: %.2f)\n", 
                            bestValue, bestLength);
        }
    }

    private void tuneParameter(String paramName, int[] testValues, 
                             java.util.function.Consumer<Integer> setter) {
        System.out.printf("\nTesting %s...\n", paramName);
        int bestValue = -1;
        double bestLength = Double.MAX_VALUE;

        for (int value : testValues) {
            RunResult best = new RunResult(Double.MAX_VALUE, null);
            RunResult worst = new RunResult(Double.MIN_VALUE, null);
            double avgLength = 0;
            int validRuns = 0;

            System.out.printf("\nTesting value = %d\n", value);
            for (int rep = 0; rep < REPLICATIONS; rep++) {
                ACOOptimizer optimizer = createOptimizer();
                setter.accept(value);
                
                List<Boolean> solution = optimizer.optimize();
                if (isValidSolution(solution)) {
                    double length = calculateTotalLength(solution);
                    validRuns++;
                    avgLength += length;
                    
                    if (length < best.length) {
                        best = new RunResult(length, solution);
                    }
                    if (length > worst.length) {
                        worst = new RunResult(length, solution);
                    }
                }
            }

            if (validRuns > 0) {
                avgLength /= validRuns;
                System.out.printf("Best result: %.2f meters\n", best.length);
                System.out.printf("Worst result: %.2f meters\n", worst.length);
                System.out.printf("Average length: %.2f (valid runs: %d/%d)\n", 
                                avgLength, validRuns, REPLICATIONS);
                
                if (avgLength < bestLength) {
                    bestLength = avgLength;
                    bestValue = value;
                }
            }
        }

        if (bestValue != -1) {
            setter.accept(bestValue);
            System.out.printf("\nBest value: %d (avg length: %.2f)\n", 
                            bestValue, bestLength);
        }
    }

    public void tuneAll() {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting ACO parameter tuning...");
        
        
        tuneParameter("ALPHA", new double[]{0.1, 0.5, 1.0, 1.5, 2.0, 3.0}, 
                     value -> setAlpha(value));
        tuneParameter("BETA", new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0}, 
                     value -> setBeta(value));
        tuneParameter("P_0", new double[]{0.05, 0.1, 0.2, 0.3, 0.4, 0.5}, 
                     value -> setP0(value));
        tuneParameter("NUM_ANTS", new int[]{20, 30, 40, 50, 75, 100}, 
                     value -> setNumAnts(value));
        tuneParameter("Q", new double[]{1.0, 10.0, 50.0, 100.0, 500.0, 1000.0}, 
                     value -> setQ(value));
        tuneParameter("RHO", new double[]{0.1, 0.3, 0.5, 0.7, 0.8, 0.9}, 
                     value -> setRho(value));
        tuneParameter("TAU_0", new double[]{0.0001, 0.001, 0.01, 0.1, 0.5, 1.0}, 
                     value -> setTau0(value));
        tuneParameter("MAX_ITERATIONS", new int[]{500, 750, 1000, 1500, 2000, 3000}, 
                     value -> setMaxIterations(value));

        System.out.println("\n=================================");
        System.out.println("Parameter tuning complete!");
        System.out.println("=================================");
        printBestConfiguration();
        
        System.out.println("\nRunning 100 final experiments with best parameters...");
        List<RunResult> results = new ArrayList<>();
        RunResult bestRun = new RunResult(Double.MAX_VALUE, null);
        RunResult worstRun = new RunResult(Double.MIN_VALUE, null);
        double totalLength = 0;
        int validRuns = 0;

        for (int i = 0; i < 50; i++) {
            ACOOptimizer optimizer = createOptimizer();
            List<Boolean> solution = optimizer.optimize();
            
            if (isValidSolution(solution)) {
                double length = calculateTotalLength(solution);
                validRuns++;
                totalLength += length;
                
                RunResult result = new RunResult(length, solution);
                results.add(result);
                
                if (length < bestRun.length) {
                    bestRun = result;
                }
                if (length > worstRun.length) {
                    worstRun = result;
                }

                System.out.printf("Run %d: Length = %.2f meters\n", i + 1, length);
            }
        }

        if (validRuns > 0) {
            List<Double> allLengths = results.stream()
                .map(r -> r.length)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
                
            double median = (allLengths.size() % 2 == 0) ? 
                (allLengths.get(allLengths.size()/2) + allLengths.get(allLengths.size()/2 - 1))/2 :
                allLengths.get(allLengths.size()/2);
            double average = totalLength / validRuns;
            
            System.out.println("\nFinal Results (50 runs):");
            System.out.println("=========================");
            System.out.printf("Best result: %.2f meters\n", bestRun.length);
            System.out.printf("Worst result: %.2f meters\n", worstRun.length);
            System.out.printf("Average length: %.2f meters\n", average);
            System.out.printf("Median length: %.2f meters\n", median);
            System.out.printf("Standard deviation: %.2f meters\n", 
                calculateStandardDeviation(allLengths, average));
            System.out.printf("Valid runs: %d/100\n", validRuns);
            
            if (bestRun.solution != null) {
                System.out.println("\nBest Solution Details:");
                System.out.println("=====================");
                System.out.printf("Length: %.2f meters\n", bestRun.length);
                long wiredCount = bestRun.solution.stream().filter(b -> b).count();
                System.out.printf("Number of wired segments: %d\n", wiredCount);
                
                System.out.println("\nWired segments in best solution:");
                for (int i = 0; i < bestRun.solution.size(); i++) {
                    if (bestRun.solution.get(i)) {
                        Usek usek = useky.get(i);
                        System.out.printf("Segment %d: Node %d -> %d (%.2f meters)\n",
                            usek.getId(), usek.getNode1Id(), usek.getNode2Id(), usek.getDistance());
                    }
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("\nTotal computation time: %.2f seconds\n", 
                         (endTime - startTime) / 1000.0);
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        return Math.sqrt(values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0));
    }

    private ACOOptimizer createOptimizer() {
        ACOOptimizer optimizer = new ACOOptimizer(useky, turnusy, maxBatteryCapacity,
                                                minBatteryLevel, consumptionRate, chargingRate);
        
        optimizer.setNumAnts(bestNumAnts);
        optimizer.setMaxIterations(bestMaxIterations);
        optimizer.setAlpha(bestAlpha);
        optimizer.setBeta(bestBeta);
        optimizer.setRho(bestRho);
        optimizer.setQ(bestQ);
        optimizer.setTau0(bestTau0);
        optimizer.setP0(bestP0);
        return optimizer;
    }

    private boolean isValidSolution(List<Boolean> solution) {
        if (solution == null) return false;
        return solution.stream().filter(b -> !b).count() > 0;
    }

    private double calculateTotalLength(List<Boolean> solution) {
        double totalLength = 0;
        for (int i = 0; i < solution.size(); i++) {
            if (solution.get(i)) {
                totalLength += useky.get(i).getDistance();
            }
        }
        return totalLength;
    }

    public void printBestConfiguration() {
        System.out.println("\nBest ACO Configuration:");
        System.out.println("=======================");
        System.out.printf("TAU_0: %.4f\n", bestTau0);
        System.out.printf("P_0: %.4f\n", bestP0);
        System.out.printf("ALPHA: %.4f\n", bestAlpha);
        System.out.printf("BETA: %.4f\n", bestBeta);
        System.out.printf("RHO: %.4f\n", bestRho);
        System.out.printf("Q: %.4f\n", bestQ);
        System.out.printf("NUM_ANTS: %d\n", bestNumAnts);
        System.out.printf("MAX_ITERATIONS: %d\n", bestMaxIterations);
    }

    private void setTau0(double value) { bestTau0 = value; }
    private void setP0(double value) { bestP0 = value; }
    private void setAlpha(double value) { bestAlpha = value; }
    private void setBeta(double value) { bestBeta = value; }
    private void setRho(double value) { bestRho = value; }
    private void setQ(double value) { bestQ = value; }
    private void setNumAnts(int value) { bestNumAnts = value; }
    private void setMaxIterations(int value) { bestMaxIterations = value; }
}

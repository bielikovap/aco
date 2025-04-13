package optimization;

import model.Turnus;
import model.Usek;
import util.CSVParser;
import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class ACOSolverMain {
    private static String RESULTS_FILE;
    private static String RUNS_LOG_FILE;
    private static final long MAX_RUNTIME_HOURS = 12;

    private enum Configuration {
        J(40.0, 10.0, 0.0013, 0.0026, "J", 9835.0),
        L(40.0, 10.0, 0.0023, 0.0026, "L", 18811.0),
        Z(30.0, 10.0, 0.0023, 0.0026, "Z", 20441.0);

        final double batteryCapacity;
        final double minBatteryCapacity;
        final double chargingRate;
        final double consumptionRate;
        final String prefix;
        final double targetValue;

        Configuration(double batteryCapacity, double minBatteryCapacity, 
                     double chargingRate, double consumptionRate, 
                     String prefix, double targetValue) {
            this.batteryCapacity = batteryCapacity;
            this.minBatteryCapacity = minBatteryCapacity;
            this.chargingRate = chargingRate;
            this.consumptionRate = consumptionRate;
            this.prefix = prefix;
            this.targetValue = targetValue;
        }
    }

    public static void main(String[] args) {
        try {
            for (Configuration config : Configuration.values()) {
                RESULTS_FILE = String.format("D:\\uniza\\bakalarka_1\\final_files\\datasets_B\\B2_%s_results.csv", config.prefix);
                RUNS_LOG_FILE = String.format("D:\\uniza\\bakalarka_1\\final_files\\datasets_B\\B2_%s_all_runs.csv", config.prefix);

                System.out.printf("\n\nStarting optimization for configuration %s\n", config.prefix);
                System.out.printf("Target value: %.2f meters\n", config.targetValue);
                System.out.printf("Battery Capacity: %.2f, Min Capacity: %.2f\n", 
                                config.batteryCapacity, config.minBatteryCapacity);
                System.out.printf("Charging Rate: %.4f, Consumption Rate: %.4f\n", 
                                config.chargingRate, config.consumptionRate);

                runOptimization(config);
            }
        } catch (Exception e) {
            System.err.println("Error running optimizer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runOptimization(Configuration config) {
        try {
            String usekyPath = "C:\\Users\\petro\\Downloads\\dataUseky\\data\\B2_useky.csv";
            String turnusyPath = "C:\\Users\\petro\\Downloads\\dataUseky\\data\\B2_turnusy.csv";

            List<Usek> useky = CSVParser.parseUseky(usekyPath);
            List<Turnus> turnusy = CSVParser.parseTurnusy(turnusyPath);

            ACOOptimizer optimizer = new ACOOptimizer(useky, turnusy, 
                config.batteryCapacity, config.minBatteryCapacity, 
                config.chargingRate, config.consumptionRate);

            optimizer.setNumAnts(100);
            optimizer.setMaxIterations(3000);
            optimizer.setAlpha(0.1);
            optimizer.setBeta(2.0);
            optimizer.setRho(0.75);
            optimizer.setQ(100.0);
            optimizer.setTau0(0.75);
            optimizer.setP0(0.1);

            List<Boolean> bestSolution = null;
            double bestLength = Double.MAX_VALUE;
            long startTime = System.currentTimeMillis();
            LocalDateTime endDateTime = LocalDateTime.now().plusHours(MAX_RUNTIME_HOURS);
            int iterationCount = 0;

            initializeResultsFile(optimizer);

            System.out.println("\nStarting optimization...");
            System.out.println("Will run until target achieved or " + MAX_RUNTIME_HOURS + " hours elapsed");

            while (LocalDateTime.now().isBefore(endDateTime)) {
                iterationCount++;
                long iterationStartTime = System.currentTimeMillis();
                List<Boolean> solution = optimizer.optimize();

                if (solution != null && isValidSolution(solution, useky, turnusy)) {
                    double length = calculateTotalLength(solution, useky);
                    double iterationTime = (System.currentTimeMillis() - iterationStartTime) / 1000.0;

                    try {
                        FileWriter runsfw = new FileWriter(RUNS_LOG_FILE, true);
                        String timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        runsfw.write(String.format("%s;%d;%.2f;%.2f\n",
                            timestamp, iterationCount, length, iterationTime));
                        runsfw.close();
                    } catch (IOException e) {
                        System.err.println("Error logging run result: " + e.getMessage());
                    }

                    if (length < bestLength) {
                        bestLength = length;
                        bestSolution = new ArrayList<>(solution);

                        logResult(iterationCount, bestLength, solution, useky, iterationTime, optimizer);

                        System.out.printf("\nNew best solution found (iteration %d):\n", iterationCount);
                        System.out.printf("Length: %.2f meters\n", bestLength);
                        System.out.printf("Time elapsed: %.2f seconds\n", 
                                        (System.currentTimeMillis() - startTime) / 1000.0);

                        if (length <= config.targetValue) {
                            System.out.println("\nTarget value achieved!");
                            break;
                        }
                    }
                }

                if (iterationCount % 10 == 0) {
                    System.out.printf("Completed %d iterations, current best: %.2f meters\r", 
                                    iterationCount, bestLength);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("\n\nOptimization complete!");
            System.out.printf("Total runtime: %.2f seconds\n", totalTime / 1000.0);
            System.out.printf("Best solution length: %.2f meters\n", bestLength);
            System.out.printf("Total iterations: %d\n", iterationCount);

        } catch (Exception e) {
            System.err.printf("Error in configuration %s: %s\n", config.prefix, e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeResultsFile(ACOOptimizer optimizer) throws IOException {
        File detailsFile = new File(RESULTS_FILE);
        boolean isNewDetails = !detailsFile.exists();
        FileWriter fw = new FileWriter(detailsFile, true);

        if (isNewDetails) {
            fw.write("=== ACO Solver Results ===\n\n");
            fw.write("Format for each solution:\n");
            fw.write("1. Timestamp and iteration info\n");
            fw.write("2. Algorithm parameters\n");
            fw.write("3. Solution metrics\n");
            fw.write("4. List of wired segments\n");
            fw.write("5. Separator line\n\n");
        }

        File runsFile = new File(RUNS_LOG_FILE);
        boolean isNewRuns = !runsFile.exists();
        FileWriter runsfw = new FileWriter(runsFile, true);

        if (isNewRuns) {
            runsfw.write("Initial Parameters:\n");
            runsfw.write(String.format("NUM_ANTS=%d; MAX_ITERATIONS=%d\n", optimizer.getNumAnts(), optimizer.getMaxIterations()));
            runsfw.write(String.format("ALPHA=%.2f; BETA=%.2f; RHO=%.2f\n", optimizer.getAlpha(), optimizer.getBeta(), optimizer.getRho()));
            runsfw.write(String.format("Q=%.2f; TAU_0=%.4f; P_0=%.4f\n\n", optimizer.getQ(), optimizer.getTau0(), optimizer.getP0()));
            runsfw.write("Results:\n");
            runsfw.write("Timestamp;Iterations;BestLength;ComputationTime\n");
        }

        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        fw.write("\n==================================\n");
        fw.write("New optimization run started at: " + timestamp + "\n");
        fw.write("==================================\n\n");

        fw.close();
        runsfw.close();
    }

    private static void logResult(int iteration, double length, List<Boolean> solution, 
                                List<Usek> useky, double totalTime, ACOOptimizer optimizer) {
        try {
            FileWriter fw = new FileWriter(RESULTS_FILE, true);
            StringBuilder sb = new StringBuilder();

            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            double iterationTime = totalTime / iteration;

            sb.append(String.format("Found at: %s (Iteration %d)\n", timestamp, iteration));

            sb.append("\nAlgorithm Parameters:\n");
            sb.append(String.format("NUM_ANTS=%d, MAX_ITERATIONS=%d\n", optimizer.getNumAnts(), optimizer.getMaxIterations()));
            sb.append(String.format("ALPHA=%.2f, BETA=%.2f, RHO=%.2f\n", optimizer.getAlpha(), optimizer.getBeta(), optimizer.getRho()));
            sb.append(String.format("Q=%.2f, TAU_0=%.4f, P_0=%.4f\n", optimizer.getQ(), optimizer.getTau0(), optimizer.getP0()));

            sb.append("\nSolution Metrics:\n");
            sb.append(String.format("Total Length: %.2f meters\n", length));
            sb.append(String.format("Computation Time: %.2f seconds\n", totalTime));
            sb.append(String.format("Number of Wired Segments: %d\n", 
                solution.stream().filter(b -> b).count()));

            sb.append("\nWired Segments:\n");
            sb.append("ID;StartNode;EndNode;Distance(m)\n");
            for (int i = 0; i < solution.size(); i++) {
                if (solution.get(i)) {
                    Usek usek = useky.get(i);
                    sb.append(String.format("%d;%d;%d;%.2f\n",
                        usek.getId(), usek.getNode1Id(), usek.getNode2Id(), usek.getDistance()));
                }
            }

            sb.append("\n----------------------------------\n\n");

            fw.write(sb.toString());
            fw.close();

            FileWriter runsfw = new FileWriter(RUNS_LOG_FILE, true);
            runsfw.write(String.format("%s;%d;%.2f;%.2f\n",
                timestamp, iteration, length, iterationTime));
            runsfw.close();
        } catch (IOException e) {
            System.err.println("Error logging result: " + e.getMessage());
        }
    }

    private static boolean isValidSolution(List<Boolean> solution, List<Usek> useky, List<Turnus> turnusy) {
        if (solution == null) return false;
        return solution.stream().filter(b -> !b).count() > 0;
    }

    private static double calculateTotalLength(List<Boolean> solution, List<Usek> useky) {
        double totalLength = 0;
        for (int i = 0; i < solution.size(); i++) {
            if (solution.get(i)) {
                totalLength += useky.get(i).getDistance();
            }
        }
        return totalLength;
    }
}
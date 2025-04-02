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
    private static final String RESULTS_FILE = "d:\\uniza\\bakalarka_1\\B2_solver_results.csv";
    private static final long MAX_RUNTIME_HOURS = 12;
    
    public static void main(String[] args) {
        try {
            double targetValue = args.length > 0 ? Double.parseDouble(args[0]) : 9835.0;
            System.out.printf("Target value: %.2f meters\n", targetValue);
            
            String usekyPath = "C:/Users/petro/Downloads/dataUseky/data/B2_useky.csv";
            String turnusyPath = "C:/Users/petro/Downloads/dataUseky/data/B2_turnusy.csv";
            
            List<Usek> useky = CSVParser.parseUseky(usekyPath);
            List<Turnus> turnusy = CSVParser.parseTurnusy(turnusyPath);
            
            System.out.println("Loaded " + useky.size() + " useky");
            System.out.println("Loaded " + turnusy.size() + " turnusy");
            
            ACOOptimizer optimizer = new ACOOptimizer(useky, turnusy, 40.0, 10.0, 0.0013, 0.0026);
            optimizer.setNumAnts(40);
            optimizer.setMaxIterations(2000);
            optimizer.setAlpha(0.5);
            optimizer.setBeta(10.0);
            optimizer.setRho(0.5);
            optimizer.setQ(100.0);
            optimizer.setTau0(0.01);
            optimizer.setP0(0.1);
            
            List<Boolean> bestSolution = null;
            double bestLength = Double.MAX_VALUE;
            long startTime = System.currentTimeMillis();
            LocalDateTime endDateTime = LocalDateTime.now().plusHours(MAX_RUNTIME_HOURS);
            int iterationCount = 0;
            
            initializeResultsFile();
            
            System.out.println("\nStarting optimization...");
            System.out.println("Will run until target achieved or " + MAX_RUNTIME_HOURS + " hours elapsed");
            
            while (LocalDateTime.now().isBefore(endDateTime)) {
                iterationCount++;
                List<Boolean> solution = optimizer.optimize();
                
                if (solution != null && isValidSolution(solution, useky, turnusy)) {
                    double length = calculateTotalLength(solution, useky);
                    
                    if (length < bestLength) {
                        bestLength = length;
                        bestSolution = new ArrayList<>(solution);
                        
                        logResult(iterationCount, bestLength, solution, useky, 
                                (System.currentTimeMillis() - startTime) / 1000.0, optimizer);
                        
                        System.out.printf("\nNew best solution found (iteration %d):\n", iterationCount);
                        System.out.printf("Length: %.2f meters\n", bestLength);
                        System.out.printf("Time elapsed: %.2f seconds\n", 
                                        (System.currentTimeMillis() - startTime) / 1000.0);
                        
                        if (length <= targetValue) {
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
            System.err.println("Error running optimizer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void initializeResultsFile() throws IOException {
        File file = new File(RESULTS_FILE);
        boolean isNew = !file.exists();
        FileWriter fw = new FileWriter(file, true);
        
        if (isNew) {
            fw.write("=== ACO Solver Results ===\n\n");
            fw.write("Format for each solution:\n");
            fw.write("1. Timestamp and iteration info\n");
            fw.write("2. Algorithm parameters\n");
            fw.write("3. Solution metrics\n");
            fw.write("4. List of wired segments\n");
            fw.write("5. Separator line\n\n");
        }
        
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        fw.write("\n==================================\n");
        fw.write("New optimization run started at: " + timestamp + "\n");
        fw.write("==================================\n\n");
        
        fw.close();
    }
    
    private static void logResult(int iteration, double length, List<Boolean> solution, 
                                List<Usek> useky, double computationTime, ACOOptimizer optimizer) {
        try {
            FileWriter fw = new FileWriter(RESULTS_FILE, true);
            StringBuilder sb = new StringBuilder();
            
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            sb.append(String.format("Found at: %s (Iteration %d)\n", timestamp, iteration));
            
            sb.append("\nAlgorithm Parameters:\n");
            sb.append(String.format("NUM_ANTS=%d, MAX_ITERATIONS=%d\n", optimizer.getNumAnts(), optimizer.getMaxIterations()));
            sb.append(String.format("ALPHA=%.2f, BETA=%.2f, RHO=%.2f\n", optimizer.getAlpha(), optimizer.getBeta(), optimizer.getRho()));
            sb.append(String.format("Q=%.2f, TAU_0=%.4f, P_0=%.4f\n", optimizer.getQ(), optimizer.getTau0(), optimizer.getP0()));
            
            sb.append("\nSolution Metrics:\n");
            sb.append(String.format("Total Length: %.2f meters\n", length));
            sb.append(String.format("Computation Time: %.2f seconds\n", computationTime));
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

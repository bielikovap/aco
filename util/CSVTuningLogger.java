package util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.Usek;

public class CSVTuningLogger {
    private static final String SUMMARY_FILE = "d:\\uniza\\bakalarka_1\\B2_summary.csv";
    private static final String RUNS_FILE = "d:\\uniza\\bakalarka_1\\B2_runs.csv";
    private static final String BEST_SOLUTION_FILE = "d:\\uniza\\bakalarka_1\\B2_best_solution.csv";
    
    public static void initializeFiles(double alpha, double beta, double rho, 
                                     double q, double tau0, double p0, 
                                     int numAnts, int maxIterations) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String metadata = String.format(
            "\n=== New Experiment ===\n" +
            "Started: %s\n" +
            "Initial parameters:\n" +
            "ALPHA=%.4f;BETA=%.4f;RHO=%.4f;Q=%.2f;TAU_0=%.4f;P_0=%.4f;NUM_ANTS=%d;MAX_ITERATIONS=%d\n" +
            "\n",
            timestamp, alpha, beta, rho, q, tau0, p0, numAnts, maxIterations);
        
        try {
            writeMetadata(SUMMARY_FILE, metadata);
            writeMetadata(RUNS_FILE, metadata);
        } catch (IOException e) {
            System.err.println("Error initializing log files: " + e.getMessage());
        }
    }

    private static void writeMetadata(String filepath, String metadata) throws IOException {
        File file = new File(filepath);
        boolean isNew = !file.exists();
        
        FileWriter fw = new FileWriter(file, true);  
        BufferedWriter bw = new BufferedWriter(fw);
        
        if (isNew) {
            if (filepath.equals(SUMMARY_FILE)) {
                bw.write("Parameter;Value;BestLength;WorstLength;AverageLength;ValidRuns;ComputationTime\n");
            } else {
                bw.write("Parameter;Value;RunNumber;Length;ComputationTime(s)\n");
            }
        }
        
        bw.write(metadata);
        bw.close();
        fw.close();
    }

    public static void logTuningRun(String paramName, String paramValue, 
                                  double bestLength, double worstLength, 
                                  double avgLength, int validRuns, 
                                  double computationTime) {
        try {
            FileWriter fw = new FileWriter(SUMMARY_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write(String.format("%s;%s;%.2f;%.2f;%.2f;%d;%.2f\n",
                paramName, paramValue, bestLength, worstLength,
                avgLength, validRuns, computationTime));
                
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    public static void logIndividualRun(String paramName, String paramValue, 
                                      int runNumber, double length, 
                                      double computationTimeMs) {
        try {
            FileWriter fw = new FileWriter(RUNS_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
                
            bw.write(String.format("%s;%s;%d;%.2f;%.3f\n",
                paramName, paramValue, runNumber, length,
                computationTimeMs / 1000.0));
                
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    public static void logBestConfiguration(double alpha, double beta, double rho, 
                                          double q, double tau0, double p0, 
                                          int numAnts, int maxIterations) {
        try {
            FileWriter fw = new FileWriter(SUMMARY_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write("\n=== Best Configuration Found ===\n");
            bw.write(String.format("ALPHA=%.4f;BETA=%.4f;RHO=%.4f;Q=%.2f;\n", alpha, beta, rho, q));
            bw.write(String.format("TAU_0=%.4f;P_0=%.4f;NUM_ANTS=%d;MAX_ITERATIONS=%d\n\n", 
                     tau0, p0, numAnts, maxIterations));
            
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("Error writing best configuration to CSV file: " + e.getMessage());
        }
    }

    public static void logBestSolution(List<Usek> useky, List<Boolean> solution, 
                                     double totalLength, double computationTime,
                                     double alpha, double beta, double rho, 
                                     double q, double tau0, double p0, 
                                     int numAnts, int maxIterations) {
        try {
            FileWriter fw = new FileWriter(BEST_SOLUTION_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            bw.write("\n=== Best Solution Found ===\n");
            bw.write(String.format("Timestamp: %s\n", timestamp));
            bw.write(String.format("Total Length: %.2f meters\n", totalLength));
            bw.write(String.format("Computation Time: %.2f seconds\n", computationTime));
            bw.write("\nParameters used:\n");
            bw.write(String.format("ALPHA=%.4f;BETA=%.4f;RHO=%.4f;Q=%.2f;\n", alpha, beta, rho, q));
            bw.write(String.format("TAU_0=%.4f;P_0=%.4f;NUM_ANTS=%d;MAX_ITERATIONS=%d\n", 
                     tau0, p0, numAnts, maxIterations));
            
            bw.write("\nWired segments:\n");
            bw.write("SegmentID;Node1;Node2;Distance\n");
            for (int i = 0; i < solution.size(); i++) {
                if (solution.get(i)) {
                    Usek usek = useky.get(i);
                    bw.write(String.format("%d;%d;%d;%.2f\n",
                        usek.getId(), usek.getNode1Id(), usek.getNode2Id(), usek.getDistance()));
                }
            }
            bw.write("\n");
            
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("Error writing best solution to file: " + e.getMessage());
        }
    }

    public static void logExperimentNumber(int experimentNumber) {
        try {
            String message = String.format("\n=== Tuning Experiment %d ===\n", experimentNumber);
            FileWriter fwSummary = new FileWriter(SUMMARY_FILE, true);
            FileWriter fwRuns = new FileWriter(RUNS_FILE, true);
            
            fwSummary.write(message);
            fwRuns.write(message);
            
            fwSummary.close();
            fwRuns.close();
        } catch (IOException e) {
            System.err.println("Error writing experiment number: " + e.getMessage());
        }
    }
}

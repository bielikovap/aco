package optimization;

import model.Turnus;
import model.Usek;
import util.CSVParser;
import java.util.List;
import java.io.File;

public class TuningMain {
    public static void main(String[] args) {
        try {
            long startTime = System.currentTimeMillis();
            
            String usekyPath = "C:/Users/petro/Downloads/dataUseky/data/T2_useky.csv";  
            String turnusyPath = "C:/Users/petro/Downloads/dataUseky/data/T2_turnusy.csv"; 
            
            File uskyFile = new File(usekyPath);
            File turnsyFile = new File(turnusyPath);
            
            if (!uskyFile.exists() || !turnsyFile.exists()) {
                System.err.println("Input files not found!");
                System.err.println("Looking for files at:");
                System.err.println("Useky: " + uskyFile.getAbsolutePath());
                System.err.println("Turnusy: " + turnsyFile.getAbsolutePath());
                return;
            }

            List<Usek> useky = CSVParser.parseUseky(usekyPath);
            System.out.println("Loaded " + useky.size() + " useky");
            
            List<Turnus> turnusy = CSVParser.parseTurnusy(turnusyPath);
            System.out.println("Loaded " + turnusy.size() + " turnusy");

            ACOTuner tuner = new ACOTuner(useky, turnusy, 
                40.0,  
                10.0,  
                0.0013,
                0.0026 
            );

            tuner.tuneAll();
            System.out.println("\n\nRunning final validation experiments...\n");
            
            long endTime = System.currentTimeMillis();
            System.out.printf("\nTotal execution time: %.2f seconds\n", 
                            (endTime - startTime)/1000.0);
        } catch (Exception e) {
            System.err.println("Error running tuner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

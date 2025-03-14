package util;

import model.Turnus;
import model.Usek;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class CSVParser {
    public static List<Usek> parseUseky(String filePath) throws IOException {
        List<Usek> useky = new ArrayList<>();
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("subor nenajdeny: " + filePath);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            
            String header = br.readLine();
            
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(";"); 
                
                if (parts.length >= 5) {
                    try {
                        parts = cleanParts(parts);
                        
                        int index = Integer.parseInt(parts[0]);
                        int idUseku = Integer.parseInt(parts[1]);
                        int idUzol1 = Integer.parseInt(parts[2]);
                        int idUzol2 = Integer.parseInt(parts[3]);
                        double dlzka = Double.parseDouble(parts[4].replace(",", "."));
                        
                        useky.add(new Usek(idUseku, dlzka, idUzol1, idUzol2));
                    } catch (NumberFormatException e) {
                        System.out.println("chyba pri parsovani " + lineNumber + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("zly format na riadku: " + line);
                }
            }
        }
        if (useky.isEmpty()) {
            throw new IllegalStateException("nenajdene ziadne useky v subore");
        }
        return useky;
    }

    private static String[] cleanParts(String[] parts) {
        String[] cleaned = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            cleaned[i] = parts[i].trim().replaceAll("[\\uFEFF\\s]", "");
        }
        return cleaned;
    }

    public static List<Turnus> parseTurnusy(String filePath) throws IOException {
        List<Turnus> turnusy = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            
            String header = br.readLine();
            
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(";", 5); 
                if (parts.length < 5) {
                    System.out.println("Warning: Invalid line format: " + line);
                    continue;
                }
                
                try {
                    int index = Integer.parseInt(parts[0].trim());
                    int id = Integer.parseInt(parts[1].trim());
                    String nazov = parts[2].trim();
                    int pocetUsekov = Integer.parseInt(parts[3].trim());
                    
                     List<Integer> usekyIndex = new ArrayList<>();
                    String[] uskyParts = parts[4].trim().split(";");
                    
                    for (String part : uskyParts) {
                        if (!part.trim().isEmpty()) {
                            try {
                                usekyIndex.add(Integer.parseInt(part.trim()));
                            } catch (NumberFormatException e) {
                                System.out.println("zly index pre usek: " + part);
                            }
                        }
                    }
                    
                    if (!usekyIndex.isEmpty()) {
                        turnusy.add(new Turnus(index, id, nazov, pocetUsekov, usekyIndex));
                    }
                    
                } catch (NumberFormatException e) {
                    System.out.println("chyba pri parsovani na riadku: " + e.getMessage());
                    System.out.println("obsah riadku " + line);
                }
            }
        }
         if (turnusy.isEmpty()) {
            throw new IllegalStateException("nenajdene ziadne turnusy");
        }
        return turnusy;
    }
}

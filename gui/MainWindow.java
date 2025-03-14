package gui;

import model.*;
import util.CSVParser;
import optimization.ACOOptimizer;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainWindow extends JFrame {
    private static final String DEFAULT_DIRECTORY = "C:/Users/petro/Downloads/dataUseky/data";
    private File currentDirectory;
    
    private JTextField uskyFileField;
    private JTextField turnsFileField;
    private JTextField batteryCapacityField;
    private JTextField minBatteryCapacityField;
    private JTextField batteryConsumptionField;
    private JTextField chargingRateField;
    private JTextField criticalLevelField;
    private JButton runButton;
    private JTextArea resultArea;
    
    public MainWindow() {
        setTitle("Pokrytie usekov trolejovym vedenim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        currentDirectory = new File(DEFAULT_DIRECTORY);
        if (!currentDirectory.exists()) {
            currentDirectory = new File(System.getProperty("user.home"));
        }
        setupUI();
        pack();
        setMinimumSize(new Dimension(600, 400));
    }
    
    private void setupUI() {
        setLayout(new BorderLayout(5, 5));
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Useky:"), gbc);
        
        uskyFileField = new JTextField(20);
        gbc.gridx = 1;
        inputPanel.add(uskyFileField, gbc);
        
        JButton uskyChooseButton = new JButton("Browse");
        gbc.gridx = 2;
        uskyChooseButton.addActionListener(e -> chooseFile(uskyFileField));
        inputPanel.add(uskyChooseButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Turnusy:"), gbc);
        
        turnsFileField = new JTextField(20);
        gbc.gridx = 1;
        inputPanel.add(turnsFileField, gbc);
        
        JButton turnsChooseButton = new JButton("Browse");
        gbc.gridx = 2;
        turnsChooseButton.addActionListener(e -> chooseFile(turnsFileField));
        inputPanel.add(turnsChooseButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Battery capacity (kWh):"), gbc);
        
        batteryCapacityField = new JTextField("40", 10);
        gbc.gridx = 1;
        inputPanel.add(batteryCapacityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Minimum battery level (kWh):"), gbc);
        
        minBatteryCapacityField = new JTextField("10", 10);
        gbc.gridx = 1;
        inputPanel.add(minBatteryCapacityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        inputPanel.add(new JLabel("Consumption (kWh/m):"), gbc);
        
        batteryConsumptionField = new JTextField("0.0013", 10); 
        gbc.gridx = 1;
        inputPanel.add(batteryConsumptionField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        inputPanel.add(new JLabel("Charging rate (kWh/m):"), gbc);
        
        chargingRateField = new JTextField("0.0026", 10); 
        gbc.gridx = 1;
        inputPanel.add(chargingRateField, gbc);
        
        
        criticalLevelField = new JTextField("10", 10);
        gbc.gridx = 1;
        inputPanel.add(criticalLevelField, gbc);

        runButton = new JButton("Run Optimization");
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 3;
        runButton.addActionListener(e -> runOptimization());
        inputPanel.add(runButton, gbc);
        
        add(inputPanel, BorderLayout.NORTH);
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
    }
    
    private void chooseFile(JTextField field) {
        JFileChooser fileChooser = new JFileChooser(currentDirectory);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            field.setText(selectedFile.getAbsolutePath());
            currentDirectory = selectedFile.getParentFile();
        }
    }
    
    private void runOptimization() {
        try {
            if (uskyFileField.getText().isEmpty() || turnsFileField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select both input files");
                return;
            }

            File uskyFile = new File(uskyFileField.getText());
            File turnsFile = new File(turnsFileField.getText());

            if (!uskyFile.exists() || !turnsFile.exists()) {
                JOptionPane.showMessageDialog(this, "One or both input files do not exist");
                return;
            }
            
            double batteryCapacity = Double.parseDouble(batteryCapacityField.getText());
            double minBatteryLevel = Double.parseDouble(minBatteryCapacityField.getText());
            double consumption = Double.parseDouble(batteryConsumptionField.getText());
            double chargingRate = Double.parseDouble(chargingRateField.getText());
            double criticalLevel = Double.parseDouble(criticalLevelField.getText()) / 100.0;
            
            if (batteryCapacity <= 0 || consumption <= 0 || minBatteryLevel < 0 || 
                minBatteryLevel >= batteryCapacity || chargingRate <= 0 || criticalLevel < 0 || criticalLevel >= 1) {
                JOptionPane.showMessageDialog(this, "Invalid battery parameters");
                return;
            }
            
            runButton.setEnabled(false);
            resultArea.setText("Running optimization...\n");
            
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    long startTime = System.currentTimeMillis();
                    String startTimeStr = new java.text.SimpleDateFormat("HH:mm:ss.SSS")
                        .format(new java.util.Date(startTime));
                    
                    List<Usek> useky = CSVParser.parseUseky(uskyFileField.getText());
                    resultArea.append("Parsed " + useky.size() + " useky\n");
                    
                    List<Turnus> turnusy = CSVParser.parseTurnusy(turnsFileField.getText());
                    resultArea.append("Parsed " + turnusy.size() + " turnusy\n");
                    
                    if (useky.isEmpty() || turnusy.isEmpty()) {
                        throw new IllegalStateException("No data parsed from input files");
                    }
                    
                    ACOOptimizer optimizer = new ACOOptimizer(useky, turnusy, 
                        batteryCapacity, minBatteryLevel, consumption, chargingRate);
                    List<Boolean> solution = optimizer.optimize();
                    
                    long endTime = System.currentTimeMillis();
                    String endTimeStr = new java.text.SimpleDateFormat("HH:mm:ss.SSS")
                        .format(new java.util.Date(endTime));
                    double computationTime = (endTime - startTime) / 1000.0;
                    
                    StringBuilder result = new StringBuilder();
                    result.append("Optimization Timing:\n");
                    result.append("==================\n");
                    result.append(String.format("Start time: %s\n", startTimeStr));
                    result.append(String.format("End time: %s\n", endTimeStr));
                    result.append(String.format("Total computation time: %.3f seconds\n\n", computationTime));
                    
                    Map<Integer, List<Usek>> nodeConnections = new TreeMap<>();
                    double totalWiringLength = 0;
                    
                    for (int i = 0; i < solution.size(); i++) {
                        if (solution.get(i)) {
                            Usek usek = useky.get(i);
                            totalWiringLength += usek.getDistance();
                            
                             nodeConnections.computeIfAbsent(usek.getNode1Id(), k -> new ArrayList<>()).add(usek);
                        }
                    }
                    
                    result.append("Required wiring segments by node:\n");
                    result.append("================================\n");
                    
                    for (Map.Entry<Integer, List<Usek>> entry : nodeConnections.entrySet()) {
                        result.append(String.format("\nFrom node %d:\n", entry.getKey()));
                        for (Usek usek : entry.getValue()) {
                            result.append(String.format("  → To node %d (Segment ID: %d, Length: %.2fm)\n", 
                                usek.getNode2Id(), 
                                usek.getId(), 
                                usek.getDistance()));
                        }
                    }
                    
                    result.append("\nWiring Paths:\n");
                    result.append("=============\n");
                    
                    Map<Integer, Set<Integer>> connections = new HashMap<>();
                    for (int i = 0; i < solution.size(); i++) {
                        if (solution.get(i)) {
                            Usek usek = useky.get(i);
                            connections.computeIfAbsent(usek.getNode1Id(), k -> new HashSet<>())
                                .add(usek.getNode2Id());
                            connections.computeIfAbsent(usek.getNode2Id(), k -> new HashSet<>())
                                .add(usek.getNode1Id());
                        }
                    }
                    
                    Set<Integer> visited = new HashSet<>();
                    for (Map.Entry<Integer, Set<Integer>> entry : connections.entrySet()) {
                        if (!visited.contains(entry.getKey())) {
                            List<Integer> path = new ArrayList<>();
                            findPath(entry.getKey(), connections, visited, path);
                            if (path.size() > 1) {
                                result.append(String.join("->", path.stream()
                                    .map(String::valueOf)
                                    .collect(java.util.stream.Collectors.toList())));
                                result.append("\n");
                            }
                        }
                    }
                    
                    result.append("\nSummary:\n");
                    result.append("========\n");
                    result.append(String.format("Total segments to wire: %d\n", 
                        solution.stream().filter(b -> b).count()));
                    result.append(String.format("Total wiring length: %.2f meters\n", totalWiringLength));
                    result.append(String.format("Average segment length: %.2f meters", 
                        totalWiringLength / solution.stream().filter(b -> b).count()));
                    
                    return result.toString();
                }
                
                private void findPath(Integer current, Map<Integer, Set<Integer>> connections, 
                                   Set<Integer> visited, List<Integer> path) {
                    visited.add(current);
                    path.add(current);
                    
                    Set<Integer> neighbors = connections.get(current);
                    if (neighbors != null) {
                        for (Integer next : neighbors) {
                            if (!visited.contains(next)) {
                                findPath(next, connections, visited, path);
                            }
                        }
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        resultArea.setText(get());
                    } catch (Exception ex) {
                        resultArea.setText("Error: " + ex.getMessage());
                        ex.printStackTrace(); 
                    } finally {
                        runButton.setEnabled(true);
                    }
                }
            };
            
            worker.execute();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format in inputs");
        }
    }
}

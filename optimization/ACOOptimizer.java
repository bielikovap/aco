package optimization;

import model.Turnus;
import model.Usek;
import java.util.*;

public class ACOOptimizer {
    private int NUM_ANTS = 50;              
    private int MAX_ITERATIONS = 1000;       
    private double ALPHA = 0.1;             
    private double BETA = 5.0;              
    private double RHO = 0.8;              
    private double Q = 10.0;             
    private double TAU_0 = 0.001;            
    private double P_0 = 0.2;               
    private int ELITE_SOLUTIONS = 5;

    private final List<Usek> useky;
    private final List<Turnus> turnusy;
    private final double maxBatteryCapacity;
    private final double minBatteryLevel;
    private final double consumptionRate;
    private final double chargingRate;
    
    private double[][] pheromones;
    private List<Boolean> bestSolution;
    private double bestSolutionLength;

    public ACOOptimizer(List<Usek> useky, List<Turnus> turnusy,
                       double maxBatteryCapacity, double minBatteryLevel,
                       double consumptionRate, double chargingRate) {
        this.useky = useky;
        this.turnusy = turnusy;
        this.maxBatteryCapacity = maxBatteryCapacity;
        this.minBatteryLevel = minBatteryLevel;
        this.consumptionRate = consumptionRate;
        this.chargingRate = chargingRate;
        this.pheromones = new double[useky.size()][2];
        this.bestSolutionLength = Double.MAX_VALUE;
        initializePheromones();
    }

    private void initializePheromones() {
        pheromones = new double[useky.size()][2];
        for (int i = 0; i < useky.size(); i++) {
            pheromones[i][0] = TAU_0;
            pheromones[i][1] = TAU_0;
        }
    }

    public List<Boolean> optimize() {
        bestSolution = null;
        bestSolutionLength = Double.MAX_VALUE;
        List<List<Boolean>> eliteSolutions = new ArrayList<>();
        List<Double> eliteLengths = new ArrayList<>();
        
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            List<List<Boolean>> antSolutions = new ArrayList<>();
            List<Double> solutionLengths = new ArrayList<>();

            for (int ant = 0; ant < NUM_ANTS; ant++) {
                List<Boolean> solution = constructSolution();
                if (isValidSolution(solution)) {
                    double length = calculateTotalLength(solution);
                    antSolutions.add(solution);
                    solutionLengths.add(length);
                    
                    updateEliteSolutions(solution, length, eliteSolutions, eliteLengths);
                }
            }

            if (!antSolutions.isEmpty()) {
                int bestIndex = solutionLengths.indexOf(Collections.min(solutionLengths));
                double length = solutionLengths.get(bestIndex);
                if (length < bestSolutionLength) {
                    bestSolutionLength = length;
                    bestSolution = new ArrayList<>(antSolutions.get(bestIndex));
                }
            }
            
            evaporatePheromones();
            for (int i = 0; i < antSolutions.size(); i++) {
                updatePheromones(antSolutions.get(i), solutionLengths.get(i));
            }

            for (int i = 0; i < eliteSolutions.size(); i++) {
                updatePheromones(eliteSolutions.get(i), eliteLengths.get(i));
            }
        }

        return bestSolution != null ? bestSolution : generateDefaultSolution();
    }

    private List<Boolean> constructSolution() {
        List<Boolean> solution = new ArrayList<>();
        Random random = new Random();
        
        Map<Integer, Integer> segmentUsage = new HashMap<>();
        for (Turnus turnus : turnusy) {
            for (Integer usekIndex : turnus.getUskyIndices()) {
                segmentUsage.merge(usekIndex, 1, Integer::sum);
            }
        }
        
        int maxUsage = segmentUsage.values().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(1);

        for (int i = 0; i < useky.size(); i++) {
            Usek currentUsek = useky.get(i);
            
            if (random.nextDouble() < P_0) {
                solution.add(random.nextBoolean());
            } else {
                double usageFrequency = segmentUsage.getOrDefault(i, 0) / (double) maxUsage;
                
                double maxDistance = useky.stream().mapToDouble(Usek::getDistance).max().orElse(1.0);
                double distanceFactor = 1.0 - (currentUsek.getDistance() / maxDistance);
                
                double heuristicValue = (usageFrequency + distanceFactor) / 1.4;
                
                double p1 = Math.pow(pheromones[i][1], ALPHA) * Math.pow(heuristicValue, BETA);
                double p0 = Math.pow(pheromones[i][0], ALPHA) * Math.pow(1.0/heuristicValue, BETA);
                double sum = p0 + p1;
                
                if (sum == 0) {
                    solution.add(random.nextBoolean());
                } else {
                    double probability = p1 / sum;
                    solution.add(random.nextDouble() < probability);
                }
            }
        }

        ensureMinimumConnectivity(solution);
        return solution;
    }

    private void ensureMinimumConnectivity(List<Boolean> solution) {
        for (Turnus turnus : turnusy) {
            double currentBattery = maxBatteryCapacity;
            double distanceWithoutCharging = 0;
            List<Integer> uskyIndices = turnus.getUskyIndices();
            
            for (int i = 0; i < uskyIndices.size(); i++) {
                int usekIndex = uskyIndices.get(i);
                Usek usek = useky.get(usekIndex);
                double distance = usek.getDistance();
                
                if (currentBattery - (distance * consumptionRate) < minBatteryLevel ||
                    distanceWithoutCharging + distance > (maxBatteryCapacity - minBatteryLevel) / consumptionRate) {
                    solution.set(usekIndex, true);
                    currentBattery = maxBatteryCapacity;
                    distanceWithoutCharging = 0;
                } else if (!solution.get(usekIndex)) {
                    currentBattery -= distance * consumptionRate;
                    distanceWithoutCharging += distance;
                }
            }
        }    
    }

    private boolean isValidSolution(List<Boolean> solution) {
         for (Turnus turnus : turnusy) {
            if (!turnus.isValidBatteryState(useky, solution, maxBatteryCapacity, 
                                          consumptionRate, chargingRate)) {
                return false;
            }
        }
        return true;
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

    private void evaporatePheromones() {
        for (int i = 0; i < useky.size(); i++) {
            pheromones[i][0] *= (1.0 - RHO);
            pheromones[i][1] *= (1.0 - RHO);
        }
    }

    private void updatePheromones(List<Boolean> solution, double solutionLength) {
        double deposit = Q / solutionLength;
        for (int i = 0; i < solution.size(); i++) {
            int index = solution.get(i) ? 1 : 0;
            pheromones[i][index] += deposit;
        }
    }

    private void updateEliteSolutions(List<Boolean> solution, double length,
                                    List<List<Boolean>> eliteSolutions,
                                    List<Double> eliteLengths) {
        if (eliteSolutions.size() < ELITE_SOLUTIONS) {
            eliteSolutions.add(new ArrayList<>(solution));
            eliteLengths.add(length);
        } else if (length < Collections.max(eliteLengths)) {
            int worstIndex = eliteLengths.indexOf(Collections.max(eliteLengths));
            eliteSolutions.set(worstIndex, new ArrayList<>(solution));
            eliteLengths.set(worstIndex, length);
        }
    }

    private List<Boolean> generateDefaultSolution() {
        List<Boolean> solution = new ArrayList<>();
        for (int i = 0; i < useky.size(); i++) {
            solution.add(true);
        }
        return solution;
    }

    public void setNumAnts(int value) { this.NUM_ANTS = value; }
    public void setMaxIterations(int value) { this.MAX_ITERATIONS = value; }
    public void setAlpha(double value) { this.ALPHA = value; }
    public void setBeta(double value) { this.BETA = value; }
    public void setRho(double value) { this.RHO = value; }
    public void setQ(double value) { this.Q = value; }
    public void setTau0(double value) { this.TAU_0 = value; }
    public void setP0(double value) { this.P_0 = value; }
}

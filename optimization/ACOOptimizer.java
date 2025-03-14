package optimization;

import model.*;
import java.util.*;

public class ACOOptimizer {
    private static final double ALPHA = 1.0;          // α - Pheromone importance
    private static final double BETA = 20.0;          // β - Heuristic importance
    private static final double RHO = 0.1;           // ρ - Pheromone evaporation rate
    private static final double Q = 100.0;           // Q - Pheromone deposit factor
    private static final double TAU_0 = 0.1;         // τ₀ - Initial pheromone level
    private static final int M = 1;                // m - Number of ants
    private static final int T_MAX = 1000;           // t_max - Maximum iterations
    private static final double TAU_MIN = 0.001;     // τ_min - Minimum pheromone limit
    private static final double TAU_MAX = 5.0;       // τ_max - Maximum pheromone limit 
    private static final int ELITE_COUNT = 5;        // Number of elite solutions to keep

    private final List<Usek> useky;
    private final List<Turnus> turnusy;
    private final double batteryCapacity;
    private final double minBatteryLevel;
    private final double consumptionPerMeter;
    private final double[][] pheromoneMatrix;
    private final Random random = new Random();

    public ACOOptimizer(List<Usek> useky, List<Turnus> turnusy, double batteryCapacity, double minBatteryLevel, double consumptionPerMeter, double chargingRatePerMeter) {
        if (useky == null || useky.isEmpty() || turnusy == null || turnusy.isEmpty()) {
            throw new IllegalArgumentException("Useky a turnusy nesmu byt prazdne");
        }
        this.useky = useky;
        this.turnusy = turnusy;
        this.batteryCapacity = batteryCapacity;
        this.minBatteryLevel = minBatteryLevel;
        this.consumptionPerMeter = consumptionPerMeter;
        this.pheromoneMatrix = new double[useky.size()][useky.size()];
        initializePheromones();
    }

    private void initializePheromones() {
        for (int i = 0; i < useky.size(); i++) {
            for (int j = 0; j < useky.size(); j++) {
                pheromoneMatrix[i][j] = TAU_0;
            }
        }
    }

    public List<Boolean> optimize() {
        List<Boolean> globalBestSolution = new ArrayList<>(Collections.nCopies(useky.size(), true));
        double globalBestCost = calculateCost(globalBestSolution);
        int stagnationCounter = 0;
        List<AntSolution> population = new ArrayList<>();
        initializePopulation(population);

        for (int iteration = 0; iteration < T_MAX; iteration++) {
            List<AntSolution> newSolutions = new ArrayList<>();
            for (int ant = 0; ant < M; ant++) {
                List<Boolean> solution = constructSolution();
                if (solution != null && isValidSolution(solution)) {
                    double cost = calculateCost(solution);
                    newSolutions.add(new AntSolution(solution, cost));
                    if (cost < globalBestCost) {
                        globalBestCost = cost;
                        globalBestSolution = new ArrayList<>(solution);
                        stagnationCounter = 0;
                    }
                }
            }
            if (!newSolutions.isEmpty()) {
                population.addAll(newSolutions);
                population.sort(Comparator.comparingDouble(a -> a.cost));
            } else if (population.isEmpty()) {
                List<Boolean> seedSolution = generateSeedSolution();
                population.add(new AntSolution(seedSolution, calculateCost(seedSolution)));
            }
            if (stagnationCounter > 100) {
                diversifyPopulation(population);
                evaporatePheromones();
                stagnationCounter = 0;
            } else {
                stagnationCounter++;
            }
            if (!population.isEmpty()) {
                updatePheromonesWithElite(population.subList(0, Math.min(ELITE_COUNT, population.size())));
            }
            evaporatePheromones();
        }
        return globalBestSolution;
    }

    private void initializePopulation(List<AntSolution> population) {
        population.add(new AntSolution(new ArrayList<>(Collections.nCopies(useky.size(), true)), calculateCost(new ArrayList<>(Collections.nCopies(useky.size(), true)))));
        population.add(new AntSolution(generateSeedSolution(), calculateCost(generateSeedSolution())));
        addRandomizedSolutions(population, 5);
    }

    private void addRandomizedSolutions(List<AntSolution> solutions, int count) {
        for (int i = 0; i < count; i++) {
            List<Boolean> base = random.nextBoolean() ? new ArrayList<>(Collections.nCopies(useky.size(), true)) : generateSeedSolution();
            for (int j = 0; j < base.size(); j++) {
                if (random.nextDouble() < 0.2) base.set(j, !base.get(j));
            }
            if (!isValidSolution(base)) base = repairSolution(base);
            if (base != null) {
                solutions.add(new AntSolution(base, calculateCost(base)));
            }
        }
    }

    protected List<Boolean> constructSolution() {
        List<Boolean> solution = new ArrayList<>(Collections.nCopies(useky.size(), false));
        List<Integer> availableSegments = new ArrayList<>();
        for (int i = 0; i < useky.size(); i++) {
            if (isSegmentCritical(i)) {
                solution.set(i, true);
            } else {
                availableSegments.add(i);
            }
        }
        if (isValidSolution(solution)) {
            return localSearch(solution);
        }
        List<Turnus> invalidTurnusy = getInvalidTurnusy(solution);
        int attempts = 0;
        int maxAttempts = useky.size() * 2;
        while (!invalidTurnusy.isEmpty() && attempts < maxAttempts && !availableSegments.isEmpty()) {
            int selectedSegment = (random.nextDouble() < 0.2) ? selectNextSegment(availableSegments, solution) : selectNextSegmentForTurnusy(availableSegments, solution, invalidTurnusy);
            if (selectedSegment >= 0) {
                solution.set(selectedSegment, true);
                availableSegments.remove(Integer.valueOf(selectedSegment));
                invalidTurnusy = getInvalidTurnusy(solution);
            }
            attempts++;
        }
        return isValidSolution(solution) ? localSearch(solution) : null;
    }

    private List<Boolean> generateSeedSolution() {
        List<Boolean> solution = new ArrayList<>(Collections.nCopies(useky.size(), true));
        for (int i = 0; i < solution.size(); i++) {
            if (!isSegmentCritical(i)) {
                solution.set(i, false);
                if (!isValidSolution(solution)) solution.set(i, true);
            }
        }
        return solution;
    }

    private void diversifyPopulation(List<AntSolution> population) {
        for (int i = ELITE_COUNT; i < population.size(); i++) {
            List<Boolean> solution = population.get(i).solution;
            for (int j = 0; j < solution.size(); j++) {
                if (random.nextDouble() < 0.1) solution.set(j, !solution.get(j));
            }
            if (!isValidSolution(solution)) {
                List<Boolean> repairedSolution = repairSolution(solution);
                if (repairedSolution != null) {
                    population.get(i).solution = repairedSolution;
                    population.get(i).cost = calculateCost(repairedSolution);
                }
            }
        }
    }

    private List<Boolean> repairSolution(List<Boolean> solution) {
        List<Boolean> repaired = new ArrayList<>(solution);
        List<Turnus> invalidTurnusy = getInvalidTurnusy(repaired);
        int attempts = 0;
        while (!invalidTurnusy.isEmpty() && attempts < useky.size()) {
            for (Turnus turnus : invalidTurnusy) {
                List<Integer> turnusSegments = turnus.getUskyIndices();
                int randomIndex = turnusSegments.get(random.nextInt(turnusSegments.size()));
                repaired.set(randomIndex, true);
            }
            invalidTurnusy = getInvalidTurnusy(repaired);
            attempts++;
        }
        return isValidSolution(repaired) ? repaired : new ArrayList<>(Collections.nCopies(useky.size(), true));
    }

    private void updatePheromonesWithElite(List<AntSolution> eliteSolutions) {
        for (int i = 0; i < useky.size(); i++) {
            for (int j = 0; j < useky.size(); j++) {
                pheromoneMatrix[i][j] *= (1.0 - RHO);
                if (pheromoneMatrix[i][j] < TAU_MIN) {
                    pheromoneMatrix[i][j] = TAU_MIN;
                }
            }
        }

        for (AntSolution solution : eliteSolutions) {
            double pheromoneDeposit = Q / solution.cost;
            for (int i = 0; i < useky.size(); i++) {
                if (solution.solution.get(i)) {
                    double newPheromone = pheromoneMatrix[i][i] + pheromoneDeposit;
                    pheromoneMatrix[i][i] = Math.min(TAU_MAX, Math.max(TAU_MIN, newPheromone));
                }
            }
        }
    }

    private List<Boolean> localSearch(List<Boolean> solution) {
        List<Boolean> bestSolution = new ArrayList<>(solution);
        double bestCost = calculateCost(solution);
        boolean improved = true;
        while (improved) {
            improved = false;
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < solution.size(); i++) {
                if (solution.get(i) && !isSegmentCritical(i)) indices.add(i);
            }
            indices.sort((i1, i2) -> Double.compare(useky.get(i2).getDistance(), useky.get(i1).getDistance()));
            for (int idx : indices) {
                solution.set(idx, false);
                if (isValidSolution(solution)) {
                    double newCost = calculateCost(solution);
                    if (newCost < bestCost) {
                        bestCost = newCost;
                        bestSolution = new ArrayList<>(solution);
                        improved = true;
                    } else {
                        solution.set(idx, true);
                    }
                } else {
                    solution.set(idx, true);
                }
            }
            if (improved) solution = new ArrayList<>(bestSolution);
        }
        return bestSolution;
    }

    private List<Turnus> getInvalidTurnusy(List<Boolean> solution) {
        return turnusy.stream().filter(t -> !t.isValidBatteryState(useky, solution, batteryCapacity, consumptionPerMeter, minBatteryLevel)).collect(java.util.stream.Collectors.toList());
    }

    private int selectNextSegmentForTurnusy(List<Integer> available, List<Boolean> currentSolution, List<Turnus> invalidTurnusy) {
        if (available.isEmpty()) return -1;
        Map<Integer, Integer> segmentCounts = new HashMap<>();
        for (Turnus turnus : invalidTurnusy) {
            for (Integer segmentIdx : turnus.getUskyIndices()) {
                if (available.contains(segmentIdx)) {
                    segmentCounts.put(segmentIdx, segmentCounts.getOrDefault(segmentIdx, 0) + 1);
                }
            }
        }
        if (segmentCounts.isEmpty()) return available.get(random.nextInt(available.size()));
        return segmentCounts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(available.get(random.nextInt(available.size())));
    }

    private int selectNextSegment(List<Integer> available, List<Boolean> currentSolution) {
        if (available.isEmpty()) return -1;

        double[] probabilities = new double[available.size()];
        double total = 0.0;

        for (int i = 0; i < available.size(); i++) {
            int segmentIndex = available.get(i);
            double pheromone = Math.max(TAU_MIN, pheromoneMatrix[segmentIndex][segmentIndex]);
            double heuristic = getHeuristicValue(segmentIndex);
            probabilities[i] = Math.pow(pheromone, ALPHA) * Math.pow(heuristic, BETA);
            total += probabilities[i];
        }

        if (total > 0) {
            double rand = random.nextDouble() * total;
            double sum = 0;
            for (int i = 0; i < probabilities.length; i++) {
                sum += probabilities[i];
                if (sum >= rand) {
                    return available.get(i);
                }
            }
        }
        
        return available.get(random.nextInt(available.size()));
    }

    private double getPheromoneValue(int segmentIndex, List<Boolean> currentSolution) {
        double pheromone = TAU_0;
        for (int j = 0; j < useky.size(); j++) {
            if (currentSolution.get(j)) pheromone += pheromoneMatrix[segmentIndex][j];
        }
        for (Turnus turnus : turnusy) {
            if (!turnus.isValidBatteryState(useky, currentSolution, batteryCapacity, consumptionPerMeter, minBatteryLevel) && turnus.getUskyIndices().contains(segmentIndex)) {
                pheromone *= 1.5;
            }
        }
        return pheromone;
    }

    private double calculateMaxDistanceWithoutCharging() {
        return (batteryCapacity - minBatteryLevel) / consumptionPerMeter * 0.7;
    }

    private double getHeuristicValue(int segmentIndex) {
        Usek segment = useky.get(segmentIndex);
        double maxRange = calculateMaxDistanceWithoutCharging();
        Map<Integer, BatteryState> routeStates = new HashMap<>();
        for (Turnus turnus : turnusy) {
            List<Integer> segments = turnus.getUskyIndices();
            if (segments.contains(segmentIndex)) {
                BatteryState state = simulateRoute(segments, segmentIndex);
                routeStates.put(turnus.getId(), state);
            }
        }
        if (routeStates.isEmpty()) return 0.0;
        double avgBatteryLevel = routeStates.values().stream().mapToDouble(s -> s.batteryLevel).average().orElse(batteryCapacity);
        double avgDistance = routeStates.values().stream().mapToDouble(s -> s.distanceFromCharge).average().orElse(0.0);
        double lengthPenalty = Math.pow(segment.getDistance() / 100.0, 6);
        double batteryNeed = (batteryCapacity - avgBatteryLevel) / batteryCapacity;
        double distanceNeed = avgDistance / maxRange;
        double usageScore = routeStates.size() / (double)turnusy.size();
        return (batteryNeed * 3.0 + distanceNeed * 2.0 + usageScore) / (1.0 + lengthPenalty);
    }

    private BatteryState simulateRoute(List<Integer> segments, int targetSegment) {
        double batteryLevel = batteryCapacity;
        double distance = 0;
        for (int segmentId : segments) {
            if (segmentId == targetSegment) break;
            Usek usek = useky.get(segmentId);
            distance += usek.getDistance();
            batteryLevel -= usek.getDistance() * consumptionPerMeter;
        }
        return new BatteryState(batteryLevel, distance);
    }

    private boolean isSegmentCritical(int usekIndex) {
        Usek segment = useky.get(usekIndex);
        double maxRange = calculateMaxDistanceWithoutCharging();
        int criticalCount = 0;
        double totalBatteryDeficit = 0;
        Set<Integer> uniqueRoutes = new HashSet<>();
        for (Turnus turnus : turnusy) {
            if (turnus.getUskyIndices().contains(usekIndex)) {
                uniqueRoutes.add(turnus.getId());
                BatteryState state = simulateRoute(turnus.getUskyIndices(), usekIndex);
                if (state.batteryLevel < minBatteryLevel * 1.5 || state.distanceFromCharge > maxRange * 0.6) {
                    criticalCount++;
                    totalBatteryDeficit += (batteryCapacity - state.batteryLevel);
                }
            }
        }
        return (criticalCount >= 4 && segment.getDistance() < 300) || (segment.getDistance() < 200 && uniqueRoutes.size() > turnusy.size() * 0.6) || (totalBatteryDeficit > batteryCapacity * uniqueRoutes.size() * 0.8);
    }

    private boolean isValidSolution(List<Boolean> solution) {
        for (Turnus turnus : turnusy) {
            if (!turnus.isValidBatteryState(useky, solution, batteryCapacity, consumptionPerMeter, minBatteryLevel)) return false;
        }
        return true;
    }

    private double calculateCost(List<Boolean> solution) {
        return solution.stream().mapToDouble(i -> i ? useky.get(solution.indexOf(i)).getDistance() : 0).sum();
    }

    private void evaporatePheromones() {
        for (int i = 0; i < useky.size(); i++) {
            for (int j = 0; j < useky.size(); j++) {
                pheromoneMatrix[i][j] *= (1.0 - RHO);
            }
        }
    }

    private static class AntSolution {
        List<Boolean> solution;
        double cost;

        AntSolution(List<Boolean> solution, double cost) {
            this.solution = solution;
            this.cost = cost;
        }
    }

    private static class BatteryState {
        final double batteryLevel;
        final double distanceFromCharge;

        BatteryState(double batteryLevel, double distanceFromCharge) {
            this.batteryLevel = batteryLevel;
            this.distanceFromCharge = distanceFromCharge;
        }
    }
}
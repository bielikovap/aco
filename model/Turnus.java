package model;

import java.util.List;

public class Turnus {
    private int id;
    private String nazov;
    private int pocetUsekov;
    private List<Integer> usekyIndex;

    public Turnus(int index, int id, String nazov, int pocetUsekov, List<Integer> usekyIndex) {
        this.id = id;
        this.nazov = nazov;
        this.pocetUsekov = pocetUsekov;
        this.usekyIndex = usekyIndex;
    }

    public boolean isValidBatteryState(List<Usek> useky, List<Boolean> wiringConfiguration, 
                                     double maxBatteryCapacity, double consumptionPerMeter, 
                                     double chargingRatePerMeter) {
        double currentBattery = maxBatteryCapacity;
        double distanceFromLastCharge = 0;
        
        for (int i = 0; i < usekyIndex.size(); i++) {
            int currentUsekIndex = usekyIndex.get(i);
            Usek currentUsek = useky.get(currentUsekIndex);
            double segmentLength = currentUsek.getDistance();
            
            double consumption = segmentLength * consumptionPerMeter;
            
            if (wiringConfiguration.get(currentUsekIndex)) {
                double charging = segmentLength * chargingRatePerMeter;
                currentBattery = Math.min(maxBatteryCapacity, currentBattery + charging);
                distanceFromLastCharge = 0;
            } else {
                currentBattery -= consumption;
                distanceFromLastCharge += segmentLength;
            }
            
            if (currentBattery <= 0) {
                return false;
            }
            double maxDistance = maxBatteryCapacity / consumptionPerMeter * 0.8; 
            if (distanceFromLastCharge > maxDistance) {
                return false;
            }
        }
        
        return true;
    }
    
    public List<Integer> getUskyIndices() {
        return usekyIndex;
    }

    @Override
    public String toString() {
        return String.format("Turnus{id=%d, nazov='%s', pocetUsekov=%d, usekyCount=%d}",
            id, nazov, pocetUsekov, usekyIndex.size());
    }

    public Integer getId() {
        return id;
    }
}
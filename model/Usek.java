package model;

public class Usek {
    private int id;
    private double distance;
    private int node1Id;
    private int node2Id;
    private boolean hasWiring;

    public Usek(int id, double distance, int node1Id, int node2Id) {
        this.id = id;
        this.distance = distance;
        this.node1Id = node1Id;
        this.node2Id = node2Id;
        this.hasWiring = false;
    }

    public int getId() { return id; }
    public double getDistance() { return distance; }
    public int getNode1Id() { return node1Id; }
    public int getNode2Id() { return node2Id; }
    public boolean hasWiring() { return hasWiring; }
    public void setWiring(boolean hasWiring) { this.hasWiring = hasWiring; }

    @Override
    public String toString() {
        return String.format("Usek{id=%d, nodes=%d->%d, length=%.2f, wiring=%s}",
            id, node1Id, node2Id, distance, hasWiring);
    }
}
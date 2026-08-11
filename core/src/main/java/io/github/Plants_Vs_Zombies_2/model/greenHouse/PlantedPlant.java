package io.github.Plants_Vs_Zombies_2.model.greenHouse;

public class PlantedPlant {
    private String plantName;
    private boolean isMarigold;
    private long plantedTimeMillis;
    private long durationMillis;

    public PlantedPlant(String plantName, boolean isMarigold, long plantedTimeMillis, long durationMillis) {
        this.plantName = plantName;
        this.isMarigold = isMarigold;
        this.plantedTimeMillis = plantedTimeMillis;
        this.durationMillis = durationMillis;
    }

    public PlantedPlant(String plantName, boolean isMarigold, long durationHours) {
        this(plantName, isMarigold, System.currentTimeMillis(), durationHours * 60 * 60 * 1000L);
    }

    public boolean isGrown() {
        return System.currentTimeMillis() - plantedTimeMillis >= durationMillis;
    }

    public void grow() {
        this.plantedTimeMillis = System.currentTimeMillis() - durationMillis;
    }

    public long getRemainingMillis() {
        long elapsed = System.currentTimeMillis() - plantedTimeMillis;
        return Math.max(0, durationMillis - elapsed);
    }

    public int getRemainingHoursCeil() {
        return (int) Math.ceil(getRemainingMillis() / (double) (60 * 60 * 1000L));
    }

    public String getPlantName() {
        return plantName;
    }

    public boolean isMarigold() {
        return isMarigold;
    }

    public long getPlantedTimeMillis() {
        return plantedTimeMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
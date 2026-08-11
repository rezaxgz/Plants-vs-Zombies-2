package model.greenHouse;

public class Pot {
    private boolean isLocked;
    private PlantedPlant plant;

    public Pot(boolean startLocked) {
        this.isLocked = startLocked;
        this.plant = null;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public void unlock() {
        this.isLocked = false;
    }

    public boolean isEmpty() {
        return plant == null;
    }

    public PlantedPlant getPlant() {
        return plant;
    }

    public void setPlant(PlantedPlant plant) {
        this.plant = plant;
    }

    public void harvest() {
        this.plant = null;
    }
}
package model.greenHouse;

import java.time.Instant;

import model.collections.plants.PlantCollectionItem;

public class PlantedPlant {
    private PlantCollectionItem plant;

    private boolean isGrown;

    private Instant plantedTime;

    private boolean checkGrowth() {
        return false;
    }

    private void grow() {

    }
}

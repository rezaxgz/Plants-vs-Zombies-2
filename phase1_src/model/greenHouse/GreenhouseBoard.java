package model.greenHouse;

public class GreenhouseBoard {
    public static final int COLUMNS = 5;
    public static final int ROWS = 4;

    private Pot[][] pots;

    public GreenhouseBoard() {
        pots = new Pot[ROWS][COLUMNS];
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                pots[y][x] = new Pot(y > 0);
            }
        }
    }

    public Pot getPotAt(int x, int y) {
        if (x < 1 || x > COLUMNS || y < 1 || y > ROWS)
            return null;
        return pots[y - 1][x - 1];
    }

    public Pot[][] getPots() {
        return pots;
    }

    public int getUnlockedPotCount() {
        int count = 0;
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                if (pot != null && !pot.isLocked()) {
                    count++;
                }
            }
        }
        return count;
    }
}
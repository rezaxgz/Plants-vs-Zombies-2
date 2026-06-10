package model.greenHouse;

import java.util.List;

public class GreenhouseBoard {
    private final int ROWS;
    private final int COLUMN;

    private List<Pot> greanhousePots;

    public GreenhouseBoard(int ROWS, int COLUMN) {
        this.ROWS = ROWS;
        this.COLUMN = COLUMN;
    }

}

package io.github.some_example_name.model.greenHouse;

public class GreenHouse {
    private GreenhouseBoard board;

    public GreenHouse() {
        this.board = new GreenhouseBoard();
    }

    public GreenhouseBoard getBoard() {
        return board;
    }
}
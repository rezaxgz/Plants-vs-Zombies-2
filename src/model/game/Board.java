package model.game;

import java.util.List;

import javax.swing.text.html.parser.Entity;

import model.game.structure.BaseStructure;
import model.game.tile.Tile;

public class Board {
    private int numberOfRows;
    private int numberOfColumns;
    private List<Tile> tiles;
    private List<Entity> allEntities; // zombies, plants, projectiles, lawnmowers, drops
    private List<BaseStructure> sructures;

    public void tickAllEntities() {

    }
}

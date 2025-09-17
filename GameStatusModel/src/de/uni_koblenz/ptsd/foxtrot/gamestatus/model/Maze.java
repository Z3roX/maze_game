package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;

public class Maze {
    private int width;
    private int height;
    private CellType[][] cellType;

    public Maze(int width, int height, CellType[][] cellType) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        this.width = width;
        this.height = height;
        this.cellType = cellType;
    }

    public CellType getTypeAt(int x, int y) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
            throw new IllegalArgumentException();
        }
        return this.cellType[y][x];
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public CellType[][] getCellType() {
        return this.cellType;
    }

}

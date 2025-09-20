package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;

/**
 * Represents the maze structure of the MazeGame.
 * <p>
 * A {@code Maze} is defined by its {@link #width}, {@link #height}, and
 * a two-dimensional array of {@link CellType} values that specify the type
 * of each field in the maze. The coordinates follow the game’s convention:
 * <ul>
 *   <li>{@code x} increases from west to east</li>
 *   <li>{@code y} increases from north to south</li>
 * </ul>
 * </p>
 *
 * <p>
 * The maze can contain various types of cells, such as walls, water, or accessible paths,
 * as defined in {@link CellType}.
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public class Maze {
	
	// The width of the maze (number of columns).
    private int width;
    
    // The height of the maze (number of rows).
    private int height;
    
    // Two-dimensional grid of cell types, indexed by [y][x].
    private CellType[][] cellType;

    /**
     * Creates a new {@code Maze} with the specified dimensions and cell structure.
     *
     * @param width     the number of columns (must be greater than 0)
     * @param height    the number of rows (must be greater than 0)
     * @param cellType  a two-dimensional array of {@link CellType} values representing the maze
     * @throws IllegalArgumentException if {@code width <= 0} or {@code height <= 0}
     */
    public Maze(int width, int height, CellType[][] cellType) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        this.width = width;
        this.height = height;
        this.cellType = cellType;
    }

    /**
     * Returns the type of cell at the given coordinates.
     *
     * @param x the horizontal coordinate (0 ≤ x < width)
     * @param y the vertical coordinate (0 ≤ y < height)
     * @return the {@link CellType} at the specified position
     * @throws IllegalArgumentException if the coordinates are outside the maze bounds
     */
    public CellType getTypeAt(int x, int y) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
            throw new IllegalArgumentException();
        }
        return this.cellType[y][x];
    }

    /**
     * Returns the width of the maze (number of columns).
     *
     * @return the maze width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the height of the maze (number of rows).
     *
     * @return the maze height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Returns the underlying 2D array of {@link CellType} values.
     * <p>
     * The first index corresponds to the row (y-coordinate), the second
     * index to the column (x-coordinate).
     * </p>
     *
     * @return the two-dimensional array of cell types
     */
    public CellType[][] getCellType() {
        return this.cellType;
    }

}

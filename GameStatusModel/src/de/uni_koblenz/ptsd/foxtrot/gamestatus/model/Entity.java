package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Abstract base class for all entities in the MazeGame that occupy a position in the maze.
 * <p>
 * An {@code Entity} defines the two-dimensional coordinates {@code (x, y)} in the maze.
 * The coordinates are implemented as JavaFX {@link IntegerProperty} instances so that
 * they can be observed and automatically update the user interface when values change.
 * </p>
 *
 * <p>
 * Typical subclasses of this class are:
 * <ul>
 *   <li>{@link Bait} – represents treasures or traps in the maze</li>
 *   <li>{@code Player} – represents a player in the maze (not shown here)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Coordinates are defined as:
 * <ul>
 *   <li>{@code xPosition} – horizontal coordinate (0 = west, increasing to the east)</li>
 *   <li>{@code yPosition} – vertical coordinate (0 = north, increasing to the south)</li>
 * </ul>
 * </p>
 *
 *<p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 *
 */
public abstract class Entity {
    
	// The horizontal coordinate of the entity within the maze.
	private IntegerProperty xPosition = new SimpleIntegerProperty();
	
	// The vertical coordinate of the entity within the maze.
    private IntegerProperty yPosition = new SimpleIntegerProperty();

    /**
     * Creates a new {@code Entity} at the specified maze coordinates.
     *
     * @param xPosition the initial horizontal coordinate of the entity
     * @param yPosition the initial vertical coordinate of the entity
     */
    public Entity(int xPosition, int yPosition) {
        this.xPosition = new SimpleIntegerProperty(xPosition);
        this.yPosition = new SimpleIntegerProperty(yPosition);
    }


    /**
     * Returns the current horizontal coordinate of this entity.
     *
     * @return the x-position of this entity
     */
    public int getxPosition() {
        return this.xPosition.get();
    }

    /**
     * Updates the horizontal coordinate of this entity.
     *
     * @param x the new x-position
     */
    public void setxPosition(int x) {
        this.xPosition.set(x);
    }

    /**
     * Returns the property object for the horizontal coordinate.
     * <p>
     * This can be used in data binding with JavaFX UI components.
     *
     * @return the {@link IntegerProperty} representing the x-position
     */
    public IntegerProperty xPositionProperty() {
        return this.xPosition;
    }

    /**
     * Returns the current vertical coordinate of this entity.
     *
     * @return the y-position of this entity
     */
    public int getyPosition() {
        return this.yPosition.get();
    }

    /**
     * Updates the vertical coordinate of this entity.
     *
     * @param y the new y-position
     */
    public void setyPosition(int y) {
        this.yPosition.set(y);
    }

    /**
     * Returns the property object for the vertical coordinate.
     * <p>
     * This can be used in data binding with JavaFX UI components.
     *
     * @return the {@link IntegerProperty} representing the y-position
     */
    public IntegerProperty yPositionProperty() {
        return this.yPosition;
    }

}

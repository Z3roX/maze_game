package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;

/**
 * Represents a bait (treasure or trap) within the MazeGame.
 * <p>
 * Baits are special objects that appear on fields of the maze. They can be
 * collected by players to either gain points or trigger negative effects,
 * depending on their {@link BaitType}.
 * <ul>
 *   <li>{@code gem} – valuable diamond (+314 points)</li>
 *   <li>{@code coffee} – a cup of coffee (+42 points)</li>
 *   <li>{@code food} – voucher for a free dish (+13 points)</li>
 *   <li>{@code trap} – a trap that teleports the player and decreases score (-128 points)</li>
 * </ul>
 * <p>
 * Each bait is located at a specific coordinate in the maze, inherited from
 * {@link Entity}, and can be either visible or invisible to players.
 *
 *<p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 * @see Entity
 * @see BaitType
 */


public class Bait extends Entity {
   
	// The type of this bait (e.g., gem, coffee, food, trap)
	private BaitType baitType;
    
	// Indicates whether this bait is currently visible to players.
	private boolean visible;

	 /**
     * Creates a new bait at the given maze coordinates.
     *
     * @param xPosition the horizontal coordinate of the bait in the maze
     * @param yPosition the vertical coordinate of the bait in the maze
     * @param baitType  the {@link BaitType} of this bait
     * @param visible   {@code true} if the bait is visible to players,
     *                  {@code false} if it is hidden
     */
    public Bait(int xPosition, int yPosition, BaitType baitType, boolean visible) {
        super(xPosition, yPosition);
        this.baitType = baitType;
        this.visible = visible;
    }

    /**
     * Returns the type of this bait.
     *
     * @return the {@link BaitType} of this bait
     */
    public BaitType getBaitType() {
        return this.baitType;
    }

    /**
     * Sets the type of this bait.
     *
     * @param baitType the new {@link BaitType}
     */
    public void setBaitType(BaitType baitType) {
        this.baitType = baitType;
    }

    /**
     * Checks whether this bait is currently visible to players.
     *
     * @return {@code true} if the bait is visible, otherwise {@code false}
     */
    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Updates the visibility of this bait.
     *
     * @param visible {@code true} if the bait should be visible,
     *                {@code false} if it should be hidden
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}

package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the different types of baits (treasures or traps) that can appear
 * in the maze during the MazeGame.
 * <p>
 * Each {@code BaitType} has a specific meaning and point effect when collected
 * by a player. These correspond directly to the protocol definition
 * (<b>BPOS</b> messages).
 * </p>
 *
 * <p><b>Types:</b></p>
 * <ul>
 *   <li>{@link #GEM} – A valuable diamond, increases the player score by +314 points.</li>
 *   <li>{@link #COFFEE} – A cup of coffee, increases the player score by +42 points.</li>
 *   <li>{@link #FOOD} – A free meal voucher, increases the player score by +13 points.</li>
 *   <li>{@link #TRAP} – A dangerous trap; the player is teleported and loses 128 points.</li>
 * </ul>
 *
 * <p>
 * The bait types are used in the protocol messages to inform clients which
 * kind of object has appeared or vanished at a specific maze position.
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public enum BaitType {
GEM, COFFEE, FOOD, TRAP;
}

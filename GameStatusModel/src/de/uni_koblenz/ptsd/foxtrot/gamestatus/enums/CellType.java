package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the possible types of cells in the maze of the MazeGame.
 * <p>
 * Each {@code CellType} corresponds to a specific field as defined in the
 * server's <b>MAZE</b> protocol message. These values are used in the
 * {@link de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze Maze} class
 * to describe the structure of the playing field.
 * </p>
 *
 * <p><b>Types:</b></p>
 * <ul>
 *   <li>{@link #UNKNOWN} – Unknown or inaccessible field, marked with <code>?</code>.</li>
 *   <li>{@link #WALL} – Impenetrable wall, marked with <code>#</code>.</li>
 *   <li>{@link #WATER} – Water field that teleports players, marked with <code>~</code>.</li>
 *   <li>{@link #PATH} – Accessible path field where players can move, marked with <code>.</code>.</li>
 * </ul>
 *
 * <p>
 * These cell types are part of the maze definition and determine where players
 * can move and which areas are blocked.
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public enum CellType {
	UNKNOWN, WALL, WATER, PATH;
}

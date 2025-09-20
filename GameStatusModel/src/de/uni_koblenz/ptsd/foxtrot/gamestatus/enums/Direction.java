package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the four possible viewing directions of a player in the MazeGame.
 * <p>
 * A player's {@code Direction} indicates where the player is currently facing
 * within the maze. This is important for interpreting movement commands
 * ({@code STEP}, {@code TURN}) and for rendering the player's orientation
 * in the user interface.
 * </p>
 *
 * <p><b>Values:</b></p>
 * <ul>
 *   <li>{@link #N} – Facing north (up).</li>
 *   <li>{@link #E} – Facing east (right).</li>
 *   <li>{@link #S} – Facing south (down).</li>
 *   <li>{@link #W} – Facing west (left).</li>
 * </ul>
 *
 * <p>
 * These values correspond directly to the protocol specification, where
 * player positions (<b>PPOS</b> messages) include one of the characters
 * <code>n</code>, <code>e</code>, <code>s</code>, or <code>w</code>
 * to indicate the viewing direction.
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public enum Direction {
	N, E, S, W;
}

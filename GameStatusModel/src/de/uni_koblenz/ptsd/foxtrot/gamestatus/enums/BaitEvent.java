package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the possible events that can occur for a {@code Bait} in the MazeGame.
 * <p>
 * A bait is an object placed in the maze (e.g. gem, coffee, food, or trap).
 * The server informs the client about changes in bait visibility using this event type.
 * </p>
 *
 * <p><b>Values:</b></p>
 * <ul>
 *   <li>{@link #APP} – A bait appears (spawns) at a given position in the maze.</li>
 *   <li>{@link #VAN} – A bait vanishes (is collected by a player or removed) from the maze.</li>
 * </ul>
 *
 * <p>
 * These values directly correspond to the <code>app</code> and <code>van</code>
 * keywords in the protocol message <b>BPOS</b> (Bait Position).
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 *
 */
public enum BaitEvent {
	APP, VAN;
}

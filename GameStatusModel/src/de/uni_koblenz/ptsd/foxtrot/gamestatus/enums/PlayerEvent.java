package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the possible events that can occur for a player in the MazeGame.
 * <p>
 * These events are transmitted by the server via <b>PPOS</b> messages and
 * describe why and how a player's position or viewing direction changed.
 * </p>
 *
 * <p><b>Values:</b></p>
 * <ul>
 *   <li>{@link #TEL} – The player was teleported to a new position (e.g., due to collision, water, or trap).</li>
 *   <li>{@link #APP} – The player appeared (spawned) in the maze after joining.</li>
 *   <li>{@link #VAN} – The player vanished from the maze (before leaving).</li>
 *   <li>{@link #MOV} – The player moved one step forward into the viewing direction.</li>
 *   <li>{@link #TRN} – The player turned left or right, changing the viewing direction.</li>
 * </ul>
 *
 * <p>
 * These values correspond directly to the protocol specification and should
 * be used to update the {@code GameStatusModel} and the user interface.
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public enum PlayerEvent {
	TEL, APP, VAN, MOV, TRN;
}

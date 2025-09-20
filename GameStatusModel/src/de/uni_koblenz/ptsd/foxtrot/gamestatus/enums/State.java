package de.uni_koblenz.ptsd.foxtrot.gamestatus.enums;

/**
 * Represents the different states of the MazeGame client during its lifecycle.
 * <p>
 * The {@code State} enum is used to describe the current connection and activity
 * status of the client with respect to the MazeGame server. It can be used in
 * the {@code GameStatusModel} to manage state transitions and update the UI
 * accordingly.
 * </p>
 *
 * <p><b>States:</b></p>
 * <ul>
 *   <li>{@link #DISCONNECTED} – The client is not connected to any server.</li>
 *   <li>{@link #CONNECTED} – The client is connected to a server but has not yet logged in.</li>
 *   <li>{@link #LOGGEDIN} – The client has successfully logged in and received a player ID.</li>
 *   <li>{@link #NOTLOGGEDIN} – The client attempted to connect but failed to log in (e.g., wrong nickname).</li>
 *   <li>{@link #ACTIVE} – The client is in an active game session and receives status updates.</li>
 *   <li>{@link #QUITTING} – The client is in the process of leaving the game (after sending {@code BYE!}).</li>
 *   <li>{@link #READY} – The client has received a {@code RDY.} message from the server and can send the next command.</li>
 *   <li>{@link #INFO} – The client is in an informational/error state after receiving an {@code INFO} message from the server.</li>
 * </ul>
 *
 * <p>
 * These values directly reflect phases and events described in the protocol
 * (e.g., {@code HELO}, {@code WELC}, {@code RDY.}, {@code INFO}, {@code QUIT}).
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public enum State {
	DISCONNECTED, CONNECTED, LOGGEDIN, NOTLOGGEDIN, ACTIVE;
}

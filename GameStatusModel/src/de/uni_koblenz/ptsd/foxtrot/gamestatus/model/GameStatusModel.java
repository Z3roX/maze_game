package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Central data model of the MazeGame Client (MGC).
 * <p>
 * The {@code GameStatusModel} holds all relevant information about the current
 * state of the game from the perspective of the client, such as:
 * <ul>
 *   <li>the {@link Maze} structure,</li>
 *   <li>all connected {@link Player players},</li>
 *   <li>all active {@link Bait baits},</li>
 *   <li>the current {@link State} of the client,</li>
 *   <li>the assigned {@code clientID} and {@code serverID},</li>
 *   <li>and whether the client is currently marked as {@code ready}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class is implemented as a <b>singleton</b>. Use {@link #getInstance()} to access
 * the single shared model instance. This ensures that all subsystems (UI,
 * CommandHandler, Protocol) operate on the same data representation.
 * </p>
 *
 * <p>
 * All fields are implemented as JavaFX {@code Property} objects to enable
 * binding with the JavaFX user interface. This way, updates to the model are
 * automatically propagated to the UI.
 * </p>
 *
 * <p>
 * <b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.
 * </p>
 *
 * 
 */
public class GameStatusModel {
	
	// The single shared instance of this model (singleton).
    private static GameStatusModel model;
    
    // The maze structure of the current game.
    private ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    
    // Map of all known players in the game, keyed by their player ID.
    private MapProperty<Integer, Player> players = new SimpleMapProperty<>(FXCollections.observableHashMap());
    
    // Map of all known baits in the game, keyed by their unique bait ID.
    private MapProperty<Integer, Bait> baits = new SimpleMapProperty<>(FXCollections.observableHashMap());
    
    // The current connection/game state of the client.
    private ObjectProperty<State> state = new SimpleObjectProperty<>();
    
    // The ID assigned to this client by the server.
    private ObjectProperty<Integer> clientID = new SimpleObjectProperty<>();
    
    // The ID of the connected server.
    private ObjectProperty<Integer> serverID = new SimpleObjectProperty<>();

    // Flag indicating whether the client is ready to send/receive game commands.
    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    // Private constructor to enforce singleton pattern.
    private GameStatusModel() {
    }

    /**
     * Returns the singleton instance of the {@code GameStatusModel}.
     * If none exists yet, a new instance is created.
     *
     * @return the single {@code GameStatusModel} instance
     */
    public static synchronized GameStatusModel getInstance() {
        if (model == null) {
            model = new GameStatusModel();
        }
        return model;
    }
    
    /**
     * Resets the model to its initial state.
     * <p>
     * Clears all players and baits, removes the maze,
     * sets the state to {@link State#DISCONNECTED}, and resets IDs and ready flag.
     * </p>
     */
    public void reset() {
        this.players.clear();
        this.baits.clear();
        this.maze.set(null);
        this.state.set(State.DISCONNECTED);
        this.ready.set(false);
        this.clientID.set(null);
        this.serverID.set(null);
    }

    /**
     * Property accessor for the maze.
     *
     * @return the {@link ObjectProperty} containing the current maze
     */
    public ObjectProperty<Maze> mazeProperty() {
        return this.maze;
    }

    /**
     * Returns the current maze.
     *
     * @return the current {@link Maze}, or {@code null} if none is set
     */
    public Maze getMaze() {
        return this.maze.get();
    }

    /**
     * Sets the current maze.
     *
     * @param maze the new {@link Maze} to be set
     */

    public void setMaze(Maze maze) {
        this.maze.set(maze);
    }

    /**
     * Property accessor for the players map.
     *
     * @return the {@link MapProperty} of players
     */
    public MapProperty<Integer, Player> playersProperty() {
        return this.players;
    }

    /**
     * Returns the current map of players.
     *
     * @return an {@link ObservableMap} of players
     */
    public ObservableMap<Integer, Player> getPlayers() {
        return this.players.get();
    }

    /**
     * Sets the map of players.
     *
     * @param players the {@link ObservableMap} to set
     */
    public void setPlayers(ObservableMap<Integer, Player> players) {
        this.players.set(players);
    }

    /**
     * Property accessor for the baits map.
     *
     * @return the {@link MapProperty} of baits
     */
    public MapProperty<Integer, Bait> baitsProperty() {
        return this.baits;
    }

    /**
     * Returns the current map of baits.
     *
     * @return an {@link ObservableMap} of baits
     */
    public ObservableMap<Integer, Bait> getBaits() {
        return this.baits.get();
    }

    /**
     * Sets the map of baits.
     *
     * @param baits the {@link ObservableMap} to set
     */
    public void setBaits(ObservableMap<Integer, Bait> baits) {
        this.baits.set(baits);
    }

    /**
     * Property accessor for the current client state.
     *
     * @return the {@link ObjectProperty} of the state
     */
    public ObjectProperty<State> stateProperty() {
        return this.state;
    }

    /**
     * Returns the current client state.
     *
     * @return the {@link State} of the client
     */
    public State getState() {
        return this.state.get();
    }

    /**
     * Sets the current client state.
     *
     * @param state the {@link State} to set
     */
    public void setState(State state) {
        this.state.set(state);
    }

    /**
     * Property accessor for the client ID.
     *
     * @return the {@link ObjectProperty} of the client ID
     */
    public ObjectProperty<Integer> clientIDProperty() {
        return this.clientID;
    }

    /**
     * Returns the client ID.
     *
     * @return the ID of the client
     */
    public int getclientID() {
        return this.clientID.get();
    }

    /**
     * Sets the client ID.
     *
     * @param clientID the new client ID
     */
    public void setclientID(int clientID) {
        this.clientID.set(clientID);
    }

    /**
     * Property accessor for the server ID.
     *
     * @return the {@link ObjectProperty} of the server ID
     */
    public ObjectProperty<Integer> serverIDProperty() {
        return this.serverID;
    }

    /**
     * Returns the server ID.
     *
     * @return the ID of the server
     */
    public int getserverID() {
        return this.serverID.get();
    }

    /**
     * Sets the server ID.
     *
     * @param serverID the new server ID
     */
    public void setserverID(int serverID) {
        this.serverID.set(serverID);
    }

    /**
     * Property accessor for the ready flag.
     *
     * @return the {@link BooleanProperty} of the ready state
     */
    public BooleanProperty readyProperty() {
        return this.ready;
    }

    /**
     * Returns whether the client is ready.
     *
     * @return {@code true} if ready, otherwise {@code false}
     */
    public boolean isReady() {
        return this.ready.get();
    }

    /**
     * Sets the ready state of the client.
     *
     * @param value {@code true} if ready, otherwise {@code false}
     */
    public void setReady(boolean value) {
        this.ready.set(value);
    }

}

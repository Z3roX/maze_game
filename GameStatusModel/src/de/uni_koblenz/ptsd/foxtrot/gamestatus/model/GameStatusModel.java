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

public class GameStatusModel {
    private static GameStatusModel model;
    private ObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private MapProperty<Integer, Player> players = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private MapProperty<Integer, Bait> baits = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private ObjectProperty<State> state = new SimpleObjectProperty<>();
    private ObjectProperty<Integer> clientID = new SimpleObjectProperty<>();
    private ObjectProperty<Integer> serverID = new SimpleObjectProperty<>();

    // Boolean für Ready
    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    private GameStatusModel() {
    }

    public static synchronized GameStatusModel getInstance() {
        if (model == null) {
            model = new GameStatusModel();
        }
        return model;
    }
    
    public void reset() {
        this.players.clear();
        this.baits.clear();
        this.maze.set(null);
        this.state.set(State.DISCONNECTED);
        this.ready.set(false);
        this.clientID.set(null);
        this.serverID.set(null);
    }

    public ObjectProperty<Maze> mazeProperty() {
        return this.maze;
    }

    public Maze getMaze() {
        return this.maze.get();
    }

    public void setMaze(Maze maze) {
        this.maze.set(maze);
    }

    public MapProperty<Integer, Player> playersProperty() {
        return this.players;
    }

    public ObservableMap<Integer, Player> getPlayers() {
        return this.players.get();
    }

    public void setPlayers(ObservableMap<Integer, Player> players) {
        this.players.set(players);
    }

    public MapProperty<Integer, Bait> baitsProperty() {
        return this.baits;
    }

    public ObservableMap<Integer, Bait> getBaits() {
        return this.baits.get();
    }

    public void setBaits(ObservableMap<Integer, Bait> baits) {
        this.baits.set(baits);
    }

    public ObjectProperty<State> stateProperty() {
        return this.state;
    }

    public State getState() {
        return this.state.get();
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public ObjectProperty<Integer> clientIDProperty() {
        return this.clientID;
    }

    public int getclientID() {
        return this.clientID.get();
    }

    public void setclientID(int clientID) {
        this.clientID.set(clientID);
    }

    public ObjectProperty<Integer> serverIDProperty() {
        return this.serverID;
    }

    public int getserverID() {
        return this.serverID.get();
    }

    public void setserverID(int serverID) {
        this.serverID.set(serverID);
    }

    public BooleanProperty readyProperty() {
        return this.ready;
    }

    public boolean isReady() {
        return this.ready.get();
    }

    public void setReady(boolean value) {
        this.ready.set(value);
    }

}

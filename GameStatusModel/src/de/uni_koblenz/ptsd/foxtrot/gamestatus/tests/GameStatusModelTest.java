package de.uni_koblenz.ptsd.foxtrot.gamestatus.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.*;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Unit tests for the {@link GameStatusModel} class.
 * <p>
 * These tests ensure that all public methods of the GameStatusModel
 * behave as expected and that the internal state of the singleton
 * instance can be reset and updated correctly.
 * </p>
 * 
 * @source We used ChatGPT to generate the structure of the test cases. 
 * 
 */
public class GameStatusModelTest {

    private GameStatusModel model;

    /**
     * Prepares a fresh instance of the {@link GameStatusModel}
     * before each test by calling {@link GameStatusModel#reset()}.
     */
    @BeforeEach
    void setUp() {
        model = GameStatusModel.getInstance();
        model.reset();
    }

    /**
     * Verifies that {@link GameStatusModel#getInstance()} always
     * returns the same singleton instance.
     */
    @Test
    void testSingleton() {
        GameStatusModel another = GameStatusModel.getInstance();
        assertSame(model, another, "getInstance() must return the same object");
    }

    /**
     * Verifies that {@link GameStatusModel#reset()} clears all
     * stored values and resets the state to its initial condition.
     */
    @Test
    void testReset() {
        model.setMaze(new Maze(2, 2, new CellType[][] {
            { CellType.PATH, CellType.WALL },
            { CellType.WATER, CellType.UNKNOWN }
        }));
        model.setReady(true);
        model.setclientID(1);
        model.setserverID(2);
        model.getPlayers().put(1, new Player(0, 0, 1, "Alice"));
        model.getBaits().put(1, new Bait(1, 1, BaitType.GEM, true));
        model.setState(State.ACTIVE);

        model.reset();

        assertNull(model.getMaze(), "Maze should be cleared after reset");
        assertFalse(model.isReady(), "Ready flag should be reset to false");
        assertNull(model.clientIDProperty().get(), "ClientID should be reset");
        assertNull(model.serverIDProperty().get(), "ServerID should be reset");
        assertEquals(State.DISCONNECTED, model.getState(), "State should be DISCONNECTED");
        assertTrue(model.getPlayers().isEmpty(), "Players map should be cleared");
        assertTrue(model.getBaits().isEmpty(), "Baits map should be cleared");
    }

    /**
     * Tests that the {@link GameStatusModel#setMaze(Maze)} and
     * {@link GameStatusModel#getMaze()} methods work correctly.
     */
    @Test
    void testMazeProperty() {
        Maze maze = new Maze(2, 2, new CellType[][] {
            { CellType.PATH, CellType.WALL },
            { CellType.WATER, CellType.UNKNOWN }
        });
        model.setMaze(maze);
        assertEquals(maze, model.getMaze());
    }

    /**
     * Tests adding and replacing players in the {@link GameStatusModel}.
     */
    @Test
    void testPlayersProperty() {
        Player p = new Player(0, 0, 1, "Alice");
        model.getPlayers().put(1, p);
        assertEquals(1, model.getPlayers().size());
        assertEquals("Alice", model.getPlayers().get(1).getNickName());

        ObservableMap<Integer, Player> newPlayers = FXCollections.observableHashMap();
        model.setPlayers(newPlayers);
        assertSame(newPlayers, model.getPlayers());
    }

    /**
     * Tests adding and replacing baits in the {@link GameStatusModel}.
     */
    @Test
    void testBaitsProperty() {
        Bait bait = new Bait(1, 1, BaitType.COFFEE, true);
        model.getBaits().put(1, bait);
        assertEquals(1, model.getBaits().size());
        assertEquals(BaitType.COFFEE, model.getBaits().get(1).getBaitType());

        ObservableMap<Integer, Bait> newBaits = FXCollections.observableHashMap();
        model.setBaits(newBaits);
        assertSame(newBaits, model.getBaits());
    }

    /**
     * Verifies that the {@link State} property can be set and retrieved.
     */
    @Test
    void testStateProperty() {
        model.setState(State.CONNECTED);
        assertEquals(State.CONNECTED, model.getState());
    }

    /**
     * Verifies that the client ID can be set and retrieved correctly.
     */
    @Test
    void testClientIdProperty() {
        model.setclientID(42);
        assertEquals(42, model.getclientID());
    }

    /**
     * Verifies that the server ID can be set and retrieved correctly.
     */
    @Test
    void testServerIdProperty() {
        model.setserverID(99);
        assertEquals(99, model.getserverID());
    }

    /**
     * Verifies that the ready flag can be set and retrieved correctly.
     */
    @Test
    void testReadyProperty() {
        model.setReady(true);
        assertTrue(model.isReady());
        model.setReady(false);
        assertFalse(model.isReady());
    }
}
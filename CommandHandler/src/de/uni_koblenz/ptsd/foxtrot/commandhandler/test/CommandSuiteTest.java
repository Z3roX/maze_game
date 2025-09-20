package de.uni_koblenz.ptsd.foxtrot.commandhandler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.BaitPosCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.InfoCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.JoinCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.LeaveCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.MazeCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.PlayerPosCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.PlayerScoreCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.QuitCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.ReadyCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.ServerVersionCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.TerminateCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.WelcomeCommand;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.PlayerEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;

class CommandSuiteTest {

    private GameStatusModel model;

    @BeforeAll
    static void initializeFxToolkit() throws Exception {
        FxTestSupport.ensureToolkitInitialized();
    }

    @BeforeEach
    void setUp() throws Exception {
        model = GameStatusModel.getInstance();
        ensureModelCleanState();
    }

    @AfterEach
    void tearDown() throws Exception {
        ensureModelCleanState();
    }

    private void ensureModelCleanState() throws Exception {
        if (model.getPlayers() == null) {
            model.setPlayers(FXCollections.observableHashMap());
        } else {
            model.getPlayers().clear();
        }
        if (model.getBaits() == null) {
            model.setBaits(FXCollections.observableHashMap());
        } else {
            model.getBaits().clear();
        }
        model.setMaze(null);
        model.setState(State.DISCONNECTED);
        model.setReady(false);
        model.setclientID(0);
        model.setserverID(0);
        closeOpenDialogs();
        FxTestSupport.waitForFxEvents();
    }

    private void closeOpenDialogs() throws Exception {
        captureAndCloseDialogs();
    }

    private List<DialogPane> captureAndCloseDialogs() throws Exception {
        List<DialogPane> dialogs = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();

        Runnable poller = new Runnable() {
            @Override
            public void run() {
                boolean foundAny = false;
                List<Window> openWindows = new ArrayList<>(Window.getWindows());
                for (Window window : openWindows) {
                    if (window instanceof Stage stage && stage.getScene() != null
                            && stage.getScene().getRoot() instanceof DialogPane pane) {
                        dialogs.add(pane);
                        stage.close();
                        foundAny = true;
                    }
                }
                if (foundAny || attempts.incrementAndGet() >= 10) {
                    latch.countDown();
                } else {
                    Platform.runLater(this);
                }
            }
        };

        Platform.runLater(poller);
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out while waiting for dialogs to close");
        }
        FxTestSupport.waitForFxEvents();
        return dialogs;
    }

    @Nested
    class BaitPosCommandTests {
        @Test
        void appInitializesMapAndAddsBait() {
            model.setBaits(null);
            BaitPosCommand command = new BaitPosCommand(3, 4, BaitType.GEM, BaitEvent.APP);
            command.execute();

            assertNotNull(model.getBaits(), "Executing APP should create the bait map when missing");
            int key = 3 * 100_000 + 4;
            Bait bait = model.getBaits().get(key);
            assertNotNull(bait, "Bait must be stored under the computed key");
            assertEquals(BaitType.GEM, bait.getBaitType());
            assertTrue(bait.isVisible());
            assertEquals(3, bait.getxPosition());
            assertEquals(4, bait.getyPosition());
        }

        @Test
        void vanRemovesExistingBait() {
            int key = 1 * 100_000 + 2;
            model.getBaits().put(key, new Bait(1, 2, BaitType.COFFEE, true));

            BaitPosCommand command = new BaitPosCommand(1, 2, BaitType.COFFEE, BaitEvent.VAN);
            command.execute();

            assertFalse(model.getBaits().containsKey(key), "VAN event must remove the bait entry");
        }

        @Test
        void appReplacesExistingBaitData() {
            int key = 5 * 100_000 + 6;
            model.getBaits().put(key, new Bait(5, 6, BaitType.FOOD, false));

            BaitPosCommand command = new BaitPosCommand(5, 6, BaitType.TRAP, BaitEvent.APP);
            command.execute();

            Bait bait = model.getBaits().get(key);
            assertNotNull(bait);
            assertEquals(BaitType.TRAP, bait.getBaitType(), "Latest APP event must overwrite bait data");
            assertTrue(bait.isVisible(), "Bait created by APP must be visible");
        }
    }

    @Nested
    class JoinCommandTests {
        @Test
        void executeInitializesMapAndAddsPlayer() {
            model.setPlayers(null);
            JoinCommand command = new JoinCommand(42, "Alice");
            command.execute();

            assertNotNull(model.getPlayers(), "Join must initialize players map when missing");
            Player player = model.getPlayers().get(42);
            assertNotNull(player, "Joined player must be registered");
            assertEquals("Alice", player.getNickName());
            assertEquals(0, player.getxPosition());
            assertEquals(0, player.getyPosition());
        }

        @Test
        void executeReplacesExistingPlayer() {
            model.getPlayers().put(7, new Player(1, 1, 7, "OldNick"));
            JoinCommand command = new JoinCommand(7, "NewNick");
            command.execute();

            Player player = model.getPlayers().get(7);
            assertNotNull(player);
            assertEquals("NewNick", player.getNickName(), "Join should update nickname for rejoining player");
            assertEquals(0, player.getxPosition(), "Join resets the server provided spawn position");
            assertEquals(0, player.getyPosition());
        }
    }

    @Nested
    class LeaveCommandTests {
        @Test
        void executeRemovesPlayer() {
            model.getPlayers().put(1, new Player(0, 0, 1, "P1"));
            model.getPlayers().put(2, new Player(0, 0, 2, "P2"));

            LeaveCommand command = new LeaveCommand(1);
            command.execute();

            assertNull(model.getPlayers().get(1));
            assertNotNull(model.getPlayers().get(2));
        }

        @Test
        void executeWithMissingPlayersMapDoesNothing() {
            model.setPlayers(null);
            LeaveCommand command = new LeaveCommand(99);
            command.execute();

            assertNull(model.getPlayers(), "Leave without players map must keep map null");
        }
    }

    @Nested
    class MazeCommandTests {
        @Test
        void executeStoresMazeAndUpdatesState() {
            CellType[][] cells = new CellType[][] { { CellType.PATH, CellType.WALL },
                    { CellType.WATER, CellType.UNKNOWN } };
            MazeCommand command = new MazeCommand(2, 2, cells);
            command.execute();

            Maze maze = model.getMaze();
            assertNotNull(maze);
            assertEquals(2, maze.getWidth());
            assertEquals(2, maze.getHeight());
            assertEquals(CellType.WATER, maze.getTypeAt(0, 1));
            assertEquals(State.NOTLOGGEDIN, model.getState(), "Maze reception should reset state to NOTLOGGEDIN");
        }

        @Test
        void invalidDimensionsPropagateException() {
            CellType[][] cells = new CellType[][] { { CellType.PATH } };
            MazeCommand command = new MazeCommand(0, 1, cells);

            assertThrows(IllegalArgumentException.class, command::execute,
                    "Invalid maze dimensions must throw an exception");
        }
    }

    @Nested
    class PlayerPosCommandTests {
        @Test
        void executeInitializesMapAndCreatesPlayer() {
            model.setPlayers(null);
            PlayerPosCommand command = new PlayerPosCommand(5, 3, 4, Direction.N, PlayerEvent.APP);
            command.execute();

            Player player = model.getPlayers().get(5);
            assertNotNull(player);
            assertEquals(3, player.getxPosition());
            assertEquals(4, player.getyPosition());
            assertEquals(Direction.N, player.getDirection());
            assertEquals("Player5", player.getNickName(), "New players get a generated nickname");
        }

        @Test
        void executeUpdatesExistingPlayerWithoutChangingNickname() {
            Player player = new Player(1, 1, 7, "Hero");
            player.setDirection(Direction.S);
            model.getPlayers().put(7, player);

            PlayerPosCommand command = new PlayerPosCommand(7, 8, 9, Direction.W, PlayerEvent.MOV);
            command.execute();

            Player updated = model.getPlayers().get(7);
            assertNotNull(updated);
            assertEquals("Hero", updated.getNickName());
            assertEquals(8, updated.getxPosition());
            assertEquals(9, updated.getyPosition());
            assertEquals(Direction.W, updated.getDirection());
        }

        @Test
        void supportsAllPlayerEvents() {
            for (PlayerEvent event : PlayerEvent.values()) {
                PlayerPosCommand command = new PlayerPosCommand(11, 1, 2, Direction.E, event);
                command.execute();
                assertNotNull(model.getPlayers().get(11));
                model.getPlayers().remove(11);
            }
        }
    }

    @Nested
    class PlayerScoreCommandTests {
        @Test
        void executeInitializesMapAndCreatesPlayer() {
            model.setPlayers(null);
            PlayerScoreCommand command = new PlayerScoreCommand(3, 77);
            command.execute();

            Player player = model.getPlayers().get(3);
            assertNotNull(player);
            assertEquals(77, player.getScore());
            assertEquals("Player3", player.getNickName());
        }

        @Test
        void executeUpdatesExistingPlayerScore() {
            Player player = new Player(0, 0, 5, "ScoreGuy");
            player.setScore(10);
            model.getPlayers().put(5, player);

            PlayerScoreCommand command = new PlayerScoreCommand(5, 1234);
            command.execute();

            assertEquals(1234, model.getPlayers().get(5).getScore());
        }
    }

    @Nested
    class InfoCommandTests {
        @Test
        void infoCodeAccessibleViaGetter() {
            InfoCommand command = new InfoCommand(451);
            assertEquals(451, command.getInfoCode());
        }

        @Test
        void code453MarksClientReady() throws Exception {
            InfoCommand command = new InfoCommand(453);
            command.execute();

            FxTestSupport.waitForFxEvents();
            assertTrue(model.isReady(), "INFO 453 must mark the client as ready");
        }

        @Test
        void code452ShowsNicknameInUseAlert() throws Exception {
            InfoCommand command = new InfoCommand(452);
            command.execute();

            List<DialogPane> dialogs = captureAndCloseDialogs();
            assertFalse(dialogs.isEmpty(), "INFO 452 must display an alert");
            assertEquals("Nickname already in use. Please choose another.", dialogs.get(0).getContentText());
            assertFalse(model.isReady());
        }

        @Test
        void code451ShowsTooManyClientsAlert() throws Exception {
            InfoCommand command = new InfoCommand(451);
            command.execute();

            List<DialogPane> dialogs = captureAndCloseDialogs();
            assertFalse(dialogs.isEmpty());
            assertEquals("Too many clients connected. Please wait and try again later.",
                    dialogs.get(0).getContentText());
        }

        @Test
        void code457ShowsLoginTimeoutAlert() throws Exception {
            InfoCommand command = new InfoCommand(457);
            command.execute();

            List<DialogPane> dialogs = captureAndCloseDialogs();
            assertFalse(dialogs.isEmpty());
            assertEquals("Login timeout. Please reconnect.", dialogs.get(0).getContentText());
        }

        @Test
        void unknownCodeLogsToErrorStream() throws Exception {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            try (PrintStream capture = new PrintStream(sink, true, StandardCharsets.UTF_8)) {
                System.setErr(capture);
                InfoCommand command = new InfoCommand(999);
                command.execute();
                FxTestSupport.waitForFxEvents();
            } finally {
                System.setErr(originalErr);
            }

            String output = sink.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Unhandled INFO code: 999"),
                    "Unknown codes should be reported on the error stream");
        }
    }

    @Nested
    class ReadyCommandTests {
        @Test
        void executeMarksModelReadyAndActive() throws Exception {
            model.setState(State.CONNECTED);
            ReadyCommand command = new ReadyCommand();
            command.execute();

            FxTestSupport.waitForFxEvents();
            assertEquals(State.ACTIVE, model.getState());
            assertTrue(model.isReady());
        }
    }

    @Nested
    class QuitCommandTests {
        @Test
        void executeMarksModelDisconnected() {
            model.setState(State.CONNECTED);
            QuitCommand command = new QuitCommand();
            command.execute();

            assertEquals(State.DISCONNECTED, model.getState());
        }
    }

    @Nested
    class ServerVersionCommandTests {
        @Test
        void executeStoresServerIdAndState() {
            ServerVersionCommand command = new ServerVersionCommand(314);
            command.execute();

            assertEquals(314, model.getserverID());
            assertEquals(State.CONNECTED, model.getState());
        }
    }

    @Nested
    class TerminateCommandTests {
        @Test
        void executeMarksDisconnectedAndShowsAlert() throws Exception {
            TerminateCommand command = new TerminateCommand();
            command.execute();

            List<DialogPane> dialogs = captureAndCloseDialogs();
            assertFalse(dialogs.isEmpty());
            assertEquals("Server has terminated.", dialogs.get(0).getContentText());
            assertEquals(State.DISCONNECTED, model.getState());
        }
    }

    @Nested
    class WelcomeCommandTests {
        @Test
        void executeStoresClientIdAndState() {
            WelcomeCommand command = new WelcomeCommand(27);
            command.execute();

            assertEquals(27, model.getclientID());
            assertEquals(State.LOGGEDIN, model.getState());
        }
    }
}
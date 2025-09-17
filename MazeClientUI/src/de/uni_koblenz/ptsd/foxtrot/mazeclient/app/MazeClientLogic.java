package de.uni_koblenz.ptsd.foxtrot.mazeclient.app;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.CommandHandler;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.protocol.MazeGameProtocol;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.RobotRunner;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl.StrategyFactory;
import javafx.collections.MapChangeListener;

public class MazeClientLogic {
    private static final Logger LOG = Logger.getLogger(MazeClientLogic.class.getName());

    private final GameStatusModel model;
    private MazeGameProtocol protocol;
    private CommandHandler commandHandler;
    private StrategyMode strategyMode = StrategyMode.OFF;
    private StrategyMode activeRobotMode = StrategyMode.OFF;
    private RobotRunner robotRunner;

    private static class Holder {
        private static final MazeClientLogic INSTANCE = new MazeClientLogic();
    }

    public static MazeClientLogic getInstance() {
        return Holder.INSTANCE;
    }

    private MazeClientLogic() {
        this.model = GameStatusModel.getInstance();
        this.model.getPlayers().addListener((MapChangeListener<Integer, Player>) change -> this.ensureRobotRunner());
        this.model.clientIDProperty().addListener((obs, oldVal, newVal) -> this.ensureRobotRunner());
    }

    public GameStatusModel getModel() {
        return this.model;
    }

    public void connect(String host, int port) throws IOException {
        LOG.info("Connecting to " + host + ":" + port);
        if (this.protocol != null) {
            this.disconnect();
        }

        CommandHandler handler = new CommandHandler();
        MazeGameProtocol newProtocol = new MazeGameProtocol(handler);
        try {
            newProtocol.connect(host, port);
        } catch (IOException e) {
            handler.stop();
            throw e;
        }

        this.commandHandler = handler;
        this.protocol = newProtocol;
        this.model.setState(State.CONNECTED);
        this.ensureRobotRunner();
    }

    public void login(String nickname) throws IOException {
        if (this.protocol == null) {
            throw new IllegalStateException("Not connected to server.");
        }

        this.protocol.sendHello(nickname);
        this.model.setState(State.LOGGEDIN);
    }

    public void requestMaze() throws IOException {
        if (this.protocol == null) {
            throw new IllegalStateException("Not connected to server.");
        }

        this.protocol.sendMazeQuery();
    }

    public void disconnect() {
        try {
            if (this.protocol != null) {
                this.protocol.sendBye();
                this.protocol.close();
            }
        } catch (IOException e) {
            LOG.warning("Error during disconnect: " + e.getMessage());
        } finally {
            this.stopRobotRunner();
            if (this.commandHandler != null) {
                this.commandHandler.stop();
                this.commandHandler = null;
            }
            this.protocol = null;
            this.model.clientIDProperty().set(null);
            this.model.setState(State.DISCONNECTED);
        }
    }

    public boolean isConnected() {
        return this.protocol != null;
    }

    public void step() throws IOException {
        if (this.protocol != null) {
            this.protocol.sendStep();
        }
    }

    public void turnLeft() throws IOException {
        if (this.protocol != null) {
            this.protocol.sendTurn('l');
        }
    }

    public void turnRight() throws IOException {
        if (this.protocol != null) {
            this.protocol.sendTurn('r');
        }
    }

    public synchronized void setStrategyMode(StrategyMode mode) {
        StrategyMode normalized = mode == null ? StrategyMode.OFF : mode;
        if (this.strategyMode == normalized) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(() -> "Strategy mode already " + normalized + ", re-checking runner state");
            }
            this.ensureRobotRunner();
            return;
        }
        LOG.info(() -> "Switching robot strategy mode to " + normalized);
        this.strategyMode = normalized;
        this.ensureRobotRunner();
    }

    public StrategyMode getStrategyMode() {
        return this.strategyMode;
    }

    private synchronized void ensureRobotRunner() {
        if (this.strategyMode == StrategyMode.OFF) {
            LOG.info("Robot strategy disabled");
            this.stopRobotRunner();
            return;
        }

        if (this.protocol == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Cannot start robot strategy: no active protocol connection");
            }
            this.stopRobotRunner();
            return;
        }

        Player me = this.resolveSelf();
        if (me == null) {
            if (LOG.isLoggable(Level.FINE)) {
                Integer id = this.model.clientIDProperty().getValue();
                LOG.fine(() -> "Robot strategy waiting for player entity (clientId=" + id + ")");
            }
            this.stopRobotRunner();
            return;
        }

        if (this.robotRunner != null && this.activeRobotMode == this.strategyMode && this.robotRunner.isRunning()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(() -> "Robot runner already active with mode " + this.activeRobotMode);
            }
            return;
        }

        Strategy strategy = StrategyFactory.create(this.strategyMode);
        if (strategy == null) {
            LOG.warning(() -> "No strategy implementation available for mode " + this.strategyMode);
            this.stopRobotRunner();
            return;
        }

        this.stopRobotRunner();
        this.robotRunner = new RobotRunner(this.model, me, this.protocol, strategy);
        this.robotRunner.start();
        this.activeRobotMode = this.strategyMode;
        LOG.info(() -> "Robot runner started with mode " + this.strategyMode + " for player " + me.getID());
    }

    private synchronized void stopRobotRunner() {
        if (this.robotRunner != null) {
            LOG.info(() -> "Stopping robot runner (mode was " + this.activeRobotMode + ")");
            this.robotRunner.stop();
            this.robotRunner = null;
        }
        this.activeRobotMode = StrategyMode.OFF;
    }

    private Player resolveSelf() {
        if (this.model.getPlayers() == null) {
            return null;
        }
        Integer id = this.model.clientIDProperty().getValue();
        if (id == null) {
            return null;
        }
        Player player = this.model.getPlayers().get(id);
        if (player == null && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(() -> "Client id " + id + " not yet present in player map");
        }
        return player;
    }

}

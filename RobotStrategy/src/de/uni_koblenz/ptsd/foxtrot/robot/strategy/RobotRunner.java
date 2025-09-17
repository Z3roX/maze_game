package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.protocol.MazeGameProtocol;
import javafx.application.Platform;

/**
 * Executes strategy decisions on a background thread and forwards actions to the protocol.
 */
public class RobotRunner {
    private static final Logger LOG = Logger.getLogger(RobotRunner.class.getName());
    private static final long COMMAND_INTERVAL_MS = 200L;
    private static final long STRATEGY_TIMEOUT_MS = 1000L;

    private final GameStatusModel model;
    private final Player me;
    private final MazeGameProtocol protocol;
    private final Strategy strategy;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public RobotRunner(GameStatusModel model, Player me, MazeGameProtocol protocol, Strategy strategy) {
        this.model = Objects.requireNonNull(model, "model");
        this.me = Objects.requireNonNull(me, "player");
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public synchronized void start() {
        if (this.running.get()) {
            return;
        }
        this.running.set(true);
        this.strategy.reset();
        this.worker = new Thread(this::runLoop, "RobotRunner");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public synchronized void stop() {
        this.running.set(false);
        if (this.worker != null) {
            this.worker.interrupt();
            try {
                this.worker.join(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.worker = null;
        }
        // Ensure strategy state is cleared on the FX thread
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                this.strategy.reset();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(STRATEGY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return this.running.get();
    }

    private void runLoop() {
        while (this.running.get()) {
            Action action = requestNextAction();
            if (action == null || action == Action.IDLE) {
                sleepQuietly(COMMAND_INTERVAL_MS);
                continue;
            }
            try {
                execute(action);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to send robot command", e);
                this.running.set(false);
                break;
            }
            sleepQuietly(COMMAND_INTERVAL_MS);
        }
    }

    private Action requestNextAction() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Action> result = new AtomicReference<>(Action.IDLE);
        Platform.runLater(() -> {
            try {
                result.set(this.strategy.decideNext(this.model, this.me));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error while computing next robot action", e);
                result.set(Action.IDLE);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(STRATEGY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                LOG.warning("Timed out waiting for strategy decision");
                return Action.IDLE;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Action.IDLE;
        }
        return result.get();
    }

    private void execute(Action action) throws IOException {
        switch (action) {
        case STEP -> this.protocol.sendStep();
        case TURN_LEFT -> this.protocol.sendTurn('l');
        case TURN_RIGHT -> this.protocol.sendTurn('r');
        case IDLE -> {
            // nothing to do
        }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.protocol.MazeGameProtocol;

/**
 * Orchestrates periodic decision making for the robot using a {@code Strategy}
 * and dispatches resulting {@code Action}s to the game protocol.
 * <p>
 * This runner is designed to be executed on a background thread. UI-facing work
 * is marshalled onto the JavaFX Application Thread via {@link javafx.application.Platform}.
 * </p>
 * <h2>Threading</h2>
 * The runner itself is thread-safe. It avoids blocking the FX thread and uses
 * atomics/listeners to react to model changes.
 */

public final class RobotRunner implements Runnable {
    private static final Logger LOG = Logger.getLogger(RobotRunner.class.getName());

    private final GameStatusModel model;
    private final Player me;
    private final MazeGameProtocol protocol;

    private volatile Strategy strategy;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean readyPermitted = new AtomicBoolean(false);
    private final AtomicBoolean readyListenerRegistered = new AtomicBoolean(false);
    private final ChangeListener<Boolean> readyListener;
    private Thread thread;

    private long actionDelayMs = 80;
    private Action lastLoggedAction;
    private long lastActionLogNanos;

    public RobotRunner(GameStatusModel model, Player me, MazeGameProtocol protocol, Strategy strategy) {
        this.model = model;
        this.me = me;
        this.protocol = protocol;
        this.strategy = strategy;
        this.readyListener = (obs, oldVal, newVal) -> this.readyPermitted.set(Boolean.TRUE.equals(newVal));
        registerReadyListener();
    }

    public synchronized void start() {
        if (running.get() || strategy == null) {
            return;
        }
        registerReadyListener();
        try {
            strategy.reset();
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Strategy reset failed before start", ex);
        }
        running.set(true);
        thread = new Thread(this, "RobotRunner");
        thread.setDaemon(true);
        LOG.info(() -> "Robot runner thread starting with strategy " + strategy.getClass().getSimpleName()
                + " for player " + me.getID());
        thread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        unregisterReadyListener();
        readyPermitted.set(false);
        LOG.info(() -> "Robot runner stopped for player " + me.getID());
        System.out.println("[RobotRunner] stop player=" + me.getID());
    }

    public synchronized void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        if (strategy != null) {
            LOG.info(() -> "Robot runner switching to strategy " + strategy.getClass().getSimpleName());
            System.out.println("[RobotRunner] switch to " + strategy.getClass().getSimpleName());
            try {
                strategy.reset();
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override

    public void run() {
        try {
            while (running.get()) {
                Strategy current = this.strategy;
                if (current == null) {
                    break;
                }

                if (!readyPermitted.get()) {
                    Thread.sleep(actionDelayMs);
                    continue;
                }

                Action action = current.decideNext(model, me);
                if (action == null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(() -> current.getClass().getSimpleName() + " returned null action; waiting");
                    }
                    Thread.sleep(actionDelayMs);
                    continue;
                }

                long now = System.nanoTime();
                boolean logAction = action != lastLoggedAction || action != Action.IDLE
                        || now - lastActionLogNanos >= 1_000_000_000L;
                if (logAction) {
                    LOG.info(() -> "Strategy " + current.getClass().getSimpleName() + " -> " + action
                            + " (pos=" + me.getxPosition() + "," + me.getyPosition() + ", dir=" + me.getDirection()
                            + ")");
                    lastLoggedAction = action;
                    lastActionLogNanos = now;
                }

                if (action == Action.IDLE) {
                    Thread.sleep(actionDelayMs);
                    continue;
                }

                switch (action) {
                    case STEP -> {
                        protocol.sendStep();
                        consumeReady();
                    }
                    case TURN_LEFT -> {
                        protocol.sendTurn('l');
                        consumeReady();
                    }
                    case TURN_RIGHT -> {
                        protocol.sendTurn('r');
                        consumeReady();
                    }
                    default -> {
                    }
                }

                Thread.sleep(actionDelayMs);
            }
        } catch (InterruptedException ignored) {
        } catch (Exception ex) {
            running.set(false);
            LOG.log(Level.WARNING, "Robot runner stopped due to exception", ex);
        }
    }

    private void consumeReady() {
        readyPermitted.set(false);
        Runnable task = () -> model.setReady(false);
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    private void registerReadyListener() {
        Runnable task = () -> {
            readyPermitted.set(model.isReady());
            if (!readyListenerRegistered.getAndSet(true)) {
                model.readyProperty().addListener(readyListener);
            }
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    private void unregisterReadyListener() {
        Runnable task = () -> {
            if (readyListenerRegistered.getAndSet(false)) {
                model.readyProperty().removeListener(readyListener);
            }
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}






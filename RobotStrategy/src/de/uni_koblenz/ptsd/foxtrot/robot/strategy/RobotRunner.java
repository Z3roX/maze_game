package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.protocol.MazeGameProtocol;

public final class RobotRunner implements Runnable {
    private static final Logger LOG = Logger.getLogger(RobotRunner.class.getName());

    private final GameStatusModel model;
    private final Player me;
    private final MazeGameProtocol protocol;

    private volatile Strategy strategy;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    private long actionDelayMs = 80;
    private Action lastLoggedAction;
    private long lastActionLogNanos;

    public RobotRunner(GameStatusModel model, Player me, MazeGameProtocol protocol, Strategy strategy) {
        this.model = model;
        this.me = me;
        this.protocol = protocol;
        this.strategy = strategy;
    }

    public synchronized void start() {
        if (running.get() || strategy == null) return;
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
        }
        LOG.info(() -> "Robot runner stopped for player " + me.getID());
    }

    public synchronized void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        if (strategy != null) {
            LOG.info(() -> "Robot runner switching to strategy " + strategy.getClass().getSimpleName());
            try { strategy.reset(); } catch (Exception ignore) {}
        }
    }

    public boolean isRunning() { return running.get(); }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Strategy current = this.strategy;
                if (current == null) break;

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

                switch (action) {
                    case STEP -> protocol.sendStep();
                    case TURN_LEFT -> protocol.sendTurn('l');
                    case TURN_RIGHT -> protocol.sendTurn('r');
                    case IDLE -> { }
                }

                Thread.sleep(actionDelayMs);
            }
        } catch (InterruptedException ignored) {
        } catch (Exception ex) {
            running.set(false);
            LOG.log(Level.WARNING, "Robot runner stopped due to exception", ex);
        }
    }
}

package de.uni_koblenz.ptsd.foxtrot.commandhandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.Command;
import javafx.application.Platform;

/**
 * Manages a FIFO queue of {@link Command} instances and processes them on a
 * dedicated background thread. The actual command logic is always executed on
 * the JavaFX Application Thread via {@link Platform#runLater(Runnable)} to keep
 * UI operations safe.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CommandHandler handler = new CommandHandler();
 * handler.submit(new SomeCommand());
 * // ...
 * handler.stop(); // shuts down the worker gracefully
 * }</pre>
 *
 * <h2>Threading</h2>
 * <ul>
 *   <li>Commands are enqueued thread-safely in a {@link BlockingQueue}.</li>
 *   <li>A background worker takes commands and dispatches them to the FX thread using
 *       {@code Platform.runLater}.</li>
 *   <li>Command processing is <em>asynchronous</em> with respect to the caller of
 *       {@link #submit(Command)}; it does not wait for the UI update to complete.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>Creating an instance automatically starts a daemon worker thread.</li>
 *   <li>{@link #stop()} stops the worker and wakes it up if it is blocking.</li>
 * </ul>
 *
 * <p>This class is thread-safe.</p>
 *
 */
public class CommandHandler {

    /** FIFO queue for commands. Never {@code null}. */
    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

    /** Background thread that pulls commands from the queue. */
    private final Thread worker;

    /** Running flag for the worker. */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Creates a new {@code CommandHandler} and starts the background worker.
     * The worker is marked as a daemon so it won't block JVM shutdown.
     */

    public CommandHandler() {
        worker = new Thread(this::process, "CommandHandler-Worker");
        worker.setDaemon(true);
        worker.start();
    }
    /**
     * Submits a {@link Command} for execution. Returns as soon as the command has been queued.
     * <p>Execution happens later on the JavaFX Application Thread.</p>
     *
     * @param cmd the command to submit (must not be {@code null})
     * @throws NullPointerException if {@code cmd} is {@code null}
     */

    public void enqueueCommand(Command cmd) {
        try {
            commandQueue.put(cmd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void process() {
        while (running.get()) {
            try {
                Command cmd = commandQueue.take();
                if (Platform.isFxApplicationThread()) {
                    cmd.execute();
                } else {
                    Platform.runLater(cmd::execute);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running.set(false);
        worker.interrupt();
    }
}

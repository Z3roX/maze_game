package de.uni_koblenz.ptsd.foxtrot.commandhandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.Command;
import javafx.application.Platform;

/**
 * Processes queued commands on a background thread in FIFO order.
 * Ensures that command execution happens on the JavaFX Application Thread.
 */
public class CommandHandler {

    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;

    public CommandHandler() {
        worker = new Thread(this::process, "CommandHandler-Worker");
        worker.setDaemon(true);
        worker.start();
    }

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
                    // Defensive: unlikely here, but execute directly if already on FX thread
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

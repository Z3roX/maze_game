package de.uni_koblenz.ptsd.foxtrot.commandhandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.Command;
import javafx.application.Platform;

class CommandHandlerTest {

    private CommandHandler handler;

    @BeforeAll
    static void initializeFxToolkit() throws Exception {
        FxTestSupport.ensureToolkitInitialized();
    }

    @BeforeEach
    void setUp() {
        handler = new CommandHandler();
    }

    @AfterEach
    void tearDown() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Test
    @DisplayName("Commands are executed on the JavaFX application thread")
    void executesCommandsOnJavaFxThread() throws Exception {
        CountDownLatch executed = new CountDownLatch(1);
        AtomicBoolean ranOnFxThread = new AtomicBoolean(false);
        AtomicReference<String> executingThreadName = new AtomicReference<>(null);

        Command command = () -> {
            ranOnFxThread.set(Platform.isFxApplicationThread());
            executingThreadName.set(Thread.currentThread().getName());
            executed.countDown();
        };

        handler.enqueueCommand(command);

        assertTrue(executed.await(2, TimeUnit.SECONDS), "Command should have executed within timeout");
        assertTrue(ranOnFxThread.get(), "Commands must run on the JavaFX application thread");
        assertFalse("CommandHandler-Worker".equals(executingThreadName.get()),
                "Commands must not run on the background worker thread");
    }

    @Test
    @DisplayName("Commands maintain their FIFO execution order")
    void processesCommandsInFifoOrder() throws Exception {
        int commandCount = 5;
        CountDownLatch allExecuted = new CountDownLatch(commandCount);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= commandCount; i++) {
            String id = "cmd" + i;
            handler.enqueueCommand(() -> {
                executionOrder.add(id);
                allExecuted.countDown();
            });
        }

        assertTrue(allExecuted.await(2, TimeUnit.SECONDS), "All commands should have completed in FIFO order");
        List<String> expectedOrder = List.of("cmd1", "cmd2", "cmd3", "cmd4", "cmd5");
        assertEquals(expectedOrder, executionOrder, "Commands must be executed in the order they are enqueued");
    }

    @Test
    @DisplayName("Handler processes large bursts of commands without dropping any")
    void processesLargeBatchesWithoutLoss() throws Exception {
        int commandCount = 200;
        CountDownLatch executed = new CountDownLatch(commandCount);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < commandCount; i++) {
            handler.enqueueCommand(() -> {
                counter.incrementAndGet();
                executed.countDown();
            });
        }

        assertTrue(executed.await(5, TimeUnit.SECONDS), "All commands from the burst must complete");
        assertEquals(commandCount, counter.get(), "Every enqueued command should have executed exactly once");
    }

    @Test
    @DisplayName("Stopping the handler prevents further command execution")
    void stopPreventsFurtherCommandExecution() throws Exception {
        CountDownLatch firstExecuted = new CountDownLatch(1);
        handler.enqueueCommand(firstExecuted::countDown);
        assertTrue(firstExecuted.await(2, TimeUnit.SECONDS), "Initial command should have been executed");

        handler.stop();
        // Give the worker thread a moment to acknowledge the stop request
        Thread.sleep(Duration.ofMillis(50).toMillis());

        CountDownLatch secondExecuted = new CountDownLatch(1);
        handler.enqueueCommand(secondExecuted::countDown);

        assertFalse(secondExecuted.await(200, TimeUnit.MILLISECONDS),
                "No commands should be executed once the handler has been stopped");
    }

    @Test
    @DisplayName("Stop can be called multiple times without side effects")
    void stopIsIdempotent() {
        handler.stop();
        assertDoesNotThrow(() -> handler.stop());
    }

    @Test
    @DisplayName("Stop terminates the internal worker thread")
    void stopTerminatesWorkerThread() throws Exception {
        Field workerField = CommandHandler.class.getDeclaredField("worker");
        workerField.setAccessible(true);
        Thread worker = (Thread) workerField.get(handler);

        handler.stop();
        worker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(worker.isAlive(), "Worker thread should terminate after stop()");
    }

    @Test
    @DisplayName("Commands in flight complete even if stop() is invoked")
    void stopDoesNotInterruptCommandInFlight() throws Exception {
        CountDownLatch commandStarted = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        handler.enqueueCommand(() -> {
            commandStarted.countDown();
            try {
                allowCompletion.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completed.set(true);
        });

        assertTrue(commandStarted.await(2, TimeUnit.SECONDS), "Command should have started on FX thread");
        handler.stop();
        allowCompletion.countDown();
        FxTestSupport.waitForFxEvents();

        assertTrue(completed.get(), "Command that was already running must finish cleanly");
    }
}

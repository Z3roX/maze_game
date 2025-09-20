package de.uni_koblenz.ptsd.foxtrot.commandhandler.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

/**
 * Utility helpers for unit tests that need a running JavaFX platform.
 */
public final class FxTestSupport {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private FxTestSupport() {
        // utility class
    }

    /**
     * Ensures the JavaFX platform is started exactly once for the entire test
     * suite.
     *
     * @throws Exception if the platform fails to start within the timeout
     */
    public static void ensureToolkitInitialized() throws Exception {
        if (INITIALIZED.compareAndSet(false, true)) {
            CountDownLatch startup = new CountDownLatch(1);
            try {
                Platform.startup(startup::countDown);
            } catch (IllegalStateException alreadyRunning) {
                // JavaFX platform was already started by another test
                startup.countDown();
            }

            if (!startup.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while starting JavaFX toolkit");
            }

            Platform.setImplicitExit(false);
        }
    }

    /**
     * Blocks until all pending JavaFX {@code runLater} tasks have executed.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public static void waitForFxEvents() throws InterruptedException {
        CountDownLatch drainLatch = new CountDownLatch(1);
        Platform.runLater(drainLatch::countDown);
        if (!drainLatch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for JavaFX tasks to finish");
        }
    }
}
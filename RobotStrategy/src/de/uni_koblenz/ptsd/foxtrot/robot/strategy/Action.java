package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

/**
 * Discrete actions a robot can perform within the grid world.
 * <p>
 * The set is intentionally small and composable so strategies can be kept simple
 * and predictable. Actions are consumed by the runner/executor which maps them
 * to protocol-level commands.
 */

public enum Action {
    /** Take a single forward step. */
    STEP,
    /** Rotate left by one discrete turn. */
    TURN_LEFT,
    /** Rotate right by one discrete turn. */
    TURN_RIGHT,
    /** Do nothing for one tick. */
    IDLE
}


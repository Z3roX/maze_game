package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

/**
 * Configurable modes that influence the robot's high-level behavior.
 * <p>Modes are hints to a {@code Strategy} implementation and do not impose
 * specific algorithms by themselves.</p>
 */

public enum StrategyMode {
    /** Strategy disabled; no automated actions. */
    OFF,
    /** Balanced behavior: mixes gem collection, path safety, and opportunistic plays. */
    SMART_BALANCED,
    /** Aggressively prioritizes gem (bait) collection. */
    SMART_GEM_RUSH,
    /** Tries to intercept opponents proactively, taking more risks. */
    SMART_INTERCEPT_AGGRO,
    /** Plays safe; avoids risky paths and prefers maintaining advantage. */
    SMART_DEFENSIVE
}


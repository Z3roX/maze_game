package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;

/**
 * Creates strategy implementations for the requested mode.
 */
public final class StrategyFactory {
    private StrategyFactory() {
    }

    public static Strategy create(StrategyMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
        case OFF -> null;
        case ASTAR -> new ShortestPathStrategy();
        case SMART -> new RStarStrategy();
        };
    }
}

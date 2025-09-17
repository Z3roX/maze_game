package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.logging.Logger;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;

/**
 * Creates strategy implementations for the requested mode.
 */
public final class StrategyFactory {
    private static final Logger LOG = Logger.getLogger(StrategyFactory.class.getName());

    private StrategyFactory() {
    }

    public static Strategy create(StrategyMode mode) {
        if (mode == null) {
            LOG.warning("Requested strategy for null mode");
            return null;
        }
        Strategy strategy = switch (mode) {
        case OFF -> null;
        case ASTAR, SMART -> new ShortestPathStrategy();
        };
        if (strategy == null) {
            LOG.info(() -> String.format("Strategy mode %s resolved to OFF", mode));
        } else {
            LOG.info(() -> String.format("Created strategy %s for mode %s", strategy.getClass().getSimpleName(), mode));
        }
        return strategy;
    }
}

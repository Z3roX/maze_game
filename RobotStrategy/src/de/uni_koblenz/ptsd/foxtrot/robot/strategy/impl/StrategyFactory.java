package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;

public final class StrategyFactory {
    private StrategyFactory(){}

    public static Strategy create(StrategyMode mode) {
        return switch (mode) {
            case ASTAR -> new AStarStrategy();
            case SMART -> new SmartStrategy(SmartTuning.gemRush());
            case OFF   -> null;
        };
    }
}

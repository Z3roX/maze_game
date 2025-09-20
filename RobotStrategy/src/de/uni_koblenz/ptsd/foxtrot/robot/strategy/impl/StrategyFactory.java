package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;

public final class StrategyFactory {
    private StrategyFactory() {}

    public static Strategy create(StrategyMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case SMART_BALANCED -> new SmartStrategy(SmartTuning.balanced());
            case SMART_GEM_RUSH -> new SmartStrategy(SmartTuning.gemRush());
            case SMART_INTERCEPT_AGGRO -> new SmartStrategy(SmartTuning.interceptAggro());
            case SMART_DEFENSIVE -> new SmartStrategy(SmartTuning.defensiveControl());
            case OFF -> null;
        };
    }
}


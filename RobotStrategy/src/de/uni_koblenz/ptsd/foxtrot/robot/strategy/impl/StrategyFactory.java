package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;

/**
* Factory methods for constructing {@link SmartStrategy} instances with
* consistent collaborators and tuning.
*
* <p>This utility centralizes strategy wiring so callers do not need to know
* about the internal helper classes ({@link AStarPathfinder}, {@link ExplorationPlanner},
* {@link InterceptPlanner}, {@link HysteresisController}) or the specific
* {@link SmartTuning} values used. It also serves as a single place to adjust
* defaults for tournaments, testing, or debugging builds.</p>
*
* <h2>Usage</h2>
* <pre>{@code
* SmartStrategy strategy = StrategyFactory.createDefault();
* // or with custom tuning
* SmartTuning tuning = SmartTuning.defaults();
* SmartStrategy custom = StrategyFactory.create(tuning);
* }</pre>
*
* <h2>Thread-safety</h2>
* <p>The factory itself is stateless and thread-safe. The returned
* {@code SmartStrategy} instances are <em>not</em> thread-safe; create one per
* controlled player or guard externally.</p>
*/

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


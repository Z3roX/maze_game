package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.Deque;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;

/** Shared cost model: TURN=0.5, STEP=1.0 */
final class CostUtil {
    static final double COST_TURN = 0.5;
    static final double COST_STEP = 1.0;

    private CostUtil() {}

    static double actionCost(Action a) {
        return switch (a) {
            case TURN_LEFT, TURN_RIGHT -> COST_TURN;
            case STEP -> COST_STEP;
            default -> 0.0;
        };
    }

    static double planCost(Deque<Action> plan) {
        if (plan == null) return Double.POSITIVE_INFINITY;
        double c = 0.0;
        for (Action a : plan) c += actionCost(a);
        return c;
    }
}

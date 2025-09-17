package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.Deque;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

/** Simple exploration fallback: walk to nearest anchor (corners + center). */
final class ExplorationPlanner {
    private final PlanProvider planner;

    ExplorationPlanner(PlanProvider planner) {
        this.planner = planner;
    }

    PlanCandidate explorationFallback(GameStatusModel model, Player me) {
        Maze maze = model.getMaze();
        if (maze == null) return null;
        int w = maze.getWidth();
        int h = maze.getHeight();
        GridPos[] anchors = new GridPos[] {
            new GridPos(1, 1),
            new GridPos(w-2, 1),
            new GridPos(1, h-2),
            new GridPos(w-2, h-2),
            new GridPos(w/2, h/2)
        };
        PlanCandidate best = null;
        for (GridPos p : anchors) {
            Target t = Target.of(p, 0);
            Deque<Action> plan = planner.planFor(me, t, model);
            if (plan == null || plan.isEmpty()) continue;
            double cost = CostUtil.planCost(plan);
            PlanCandidate cand = new PlanCandidate(t, new ArrayDeque<>(plan), /*score*/ -cost);
            if (best == null || cand.score() > best.score()) {
                best = cand;
            }
        }
        return best;
    }
}

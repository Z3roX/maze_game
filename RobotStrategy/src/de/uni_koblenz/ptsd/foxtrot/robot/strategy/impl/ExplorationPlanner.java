package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

final class ExplorationPlanner {
    private final SmartStrategy strategy;

    ExplorationPlanner(SmartStrategy strategy) {
        this.strategy = strategy;
    }

    SmartStrategy.Candidate explorationFallback(GameStatusModel model, Player me) {
        Maze maze = model.getMaze();
        if (maze == null) {
            return null;
        }
        int w = maze.getWidth();
        int h = maze.getHeight();
        GridPos[] anchors = new GridPos[] {
            new GridPos(1, 1),
            new GridPos(w - 2, 1),
            new GridPos(1, h - 2),
            new GridPos(w - 2, h - 2),
            new GridPos(w / 2, h / 2)
        };
        SmartStrategy.Candidate best = null;
        for (GridPos p : anchors) {
            Target target = Target.of(p, 0);
            AStarPathfinder.Result route = strategy.planFor(me, p, model);
            if (!route.success() || route.actions().isEmpty()) {
                continue;
            }
            double cost = route.cost();
            SmartStrategy.Candidate cand = new SmartStrategy.Candidate(target, route.actions(), cost, -cost);
            if (best == null || cand.score() > best.score()) {
                best = cand;
            }
        }
        return best;
    }
}


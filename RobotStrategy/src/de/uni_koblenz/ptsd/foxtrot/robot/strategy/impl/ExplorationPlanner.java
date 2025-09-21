package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

/**
* Greedy exploration planner that proposes a "good enough" target when the main
* strategy cannot find a high-scoring plan.
*
* <p>The planner inspects the current {@link Maze} and the robot's position to
* assemble a list of eligible {@link Target}s (e.g., unexplored cells, pickups,
* or safe frontier cells — depending on {@link SmartStrategy}'s configuration).
* For each candidate target it requests a route from {@link SmartStrategy#planFor}
* which uses the grid planner (see {@link AStarPathfinder}) to obtain a sequence
* of {@link de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action Actions}. It then
* evaluates the candidate using a simple score that balances route cost and
* potential value and returns the best-scoring option.
*
* <p>This class is intentionally lightweight: it does not keep internal state and
* can be re-used across turns. If no viable target can be reached, it returns
* {@code null} to signal that the caller should try another fallback.
*/

final class ExplorationPlanner {
    private final SmartStrategy strategy;

    /**
    * Creates a new exploration helper tied to a {@link SmartStrategy} instance.
    *
    * @param strategy strategy facade used for environment queries and path planning
    */

    ExplorationPlanner(SmartStrategy strategy) {
        this.strategy = strategy;
    }

    /**
    * Compute a reasonable fallback target and route when the primary strategy fails.
    *
    * <p>Enumerates potential {@link Target}s for the player, asks the strategy to
    * plan a route to each, and returns the best {@link SmartStrategy.Candidate}
    * according to the strategy's scoring (typically lower cost = better).
    *
    * @param model current game state (maze, items, opponents, etc.)
    * @param me the controlled {@link Player}
    * @return a non-null {@code Candidate} on success, or {@code null} if no
    * reachable/acceptable target exists.
    */

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


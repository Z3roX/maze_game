package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.Objects;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

/**
 * Opportunistic intercept planner that tries to beat opponents to a high–value pickup.
 *
 * <p>Given the current strategic suggestion ({@code normal}), this helper checks whether
 * the suggested target is a {@link BaitType#GEM GEM}. If so, it probes a small neighborhood
 * around the gem and evaluates interception points. For each probe position it compares our
 * estimated route cost against every opponent's route cost produced by
 * {@link SmartStrategy#planFor(Player, GridPos, GameStatusModel)}. If we can arrive at least
 * {@link SmartTuning#interceptMargin} steps earlier than an opponent, it proposes an
 * intercept {@link SmartStrategy.Candidate}. The best‑scoring candidate (highest score) is
 * returned.
 *
 * <p>The search radius and number of probe positions are controlled by
 * {@link SmartTuning#interceptKSteps}. Probe positions are clamped to the inner rectangle
 * of the maze to avoid walls/borders.
 *
 * <p>Returns {@code null} when interception is disabled, the normal target is not a gem, or
 * no suitable interception point is found.
 */
final class InterceptPlanner {
    private final SmartStrategy strategy;
    private final SmartTuning tuning;

    /**
     * Creates a new intercept planner bound to a strategy facade and tuning parameters.
     *
     * @param strategy facade used for path planning and candidate construction
     * @param tuning   configuration flags and thresholds (e.g., enable/disable, margins)
     */
    InterceptPlanner(SmartStrategy strategy, SmartTuning tuning) {
        this.strategy = Objects.requireNonNull(strategy);
        this.tuning = Objects.requireNonNull(tuning);
    }

    /**
     * Suggest an intercept route toward a gem if we can plausibly arrive before an opponent.
     *
     * <p>Evaluates up to {@link SmartTuning#interceptKSteps} probe positions around the current
     * gem target from {@code normal}. For each probe position it computes our and every
     * opponent's route cost. If our cost plus {@link SmartTuning#interceptMargin} is less than
     * an opponent's cost, a candidate is generated with a slight bonus in its score.
     *
     * @param model   current game state (players, maze, baits)
     * @param me      the controlled player
     * @param normal  the currently preferred/normal candidate (its target should point to a gem)
     * @return the best intercept candidate or {@code null} if none is competitive
     *
     * @implNote Probe positions are clamped to {@code [1, width-2] × [1, height-2]} and
     *           jittered in a simple alternating pattern to look around the gem without
     *           scanning the full neighborhood.
     */
    SmartStrategy.Candidate selectInterceptCandidate(GameStatusModel model, Player me, SmartStrategy.Candidate normal) {
        if (!tuning.enableIntercept) {
            return null;
        }
        ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) {
            return null;
        }

        Bait targetBait = (normal != null && normal.target() != null) ? normal.target().bait() : null;
        if (targetBait == null || targetBait.getBaitType() != BaitType.GEM) {
            return null;
        }

        int raceMargin = tuning.interceptMargin;
        GridPos probe = Target.of(targetBait, 0).pos();
        SmartStrategy.Candidate best = null;
        int k = tuning.interceptKSteps;
        for (int i = 0; i < k; i++) {
            GridPos pos = new GridPos(
                    Math.max(1, Math.min(model.getMaze().getWidth() - 2, probe.x())),
                    Math.max(1, Math.min(model.getMaze().getHeight() - 2, probe.y())));
            Target interceptPoint = Target.of(pos, 0);

            for (Player opp : players.values()) {
                if (opp == null || opp.getID() == me.getID()) {
                    continue;
                }
                AStarPathfinder.Result ourRoute = strategy.planFor(me, pos, model);
                AStarPathfinder.Result oppRoute = strategy.planFor(opp, pos, model);
                if (!ourRoute.success() || ourRoute.actions().isEmpty()) {
                    continue;
                }
                if (!oppRoute.success() || oppRoute.actions().isEmpty()) {
                    continue;
                }
                double ourCost = ourRoute.cost();
                double oppCost = oppRoute.cost();
                if (ourCost + raceMargin <= oppCost) {
                    double score = -ourCost + 1.0; // slight bonus for successful intercept
                    SmartStrategy.Candidate cand = new SmartStrategy.Candidate(interceptPoint, ourRoute.actions(), ourCost, score);
                    if (best == null || cand.score() > best.score()) {
                        best = cand;
                    }
                }
            }
            probe = new GridPos(probe.x() + ((i % 2 == 0) ? 1 : -1), probe.y() + ((i % 3 == 0) ? 1 : -1));
        }
        return best;
    }
}

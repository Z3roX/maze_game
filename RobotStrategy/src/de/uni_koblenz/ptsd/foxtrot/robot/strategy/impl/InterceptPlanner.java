package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.Objects;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

final class InterceptPlanner {
    private final SmartStrategy strategy;
    private final SmartTuning tuning;

    InterceptPlanner(SmartStrategy strategy, SmartTuning tuning) {
        this.strategy = Objects.requireNonNull(strategy);
        this.tuning = Objects.requireNonNull(tuning);
    }

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


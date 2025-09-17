package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

final class InterceptPlanner {
    private final SmartTuning tuning;
    private final PlanProvider planner;
    InterceptPlanner(SmartTuning tuning, PlanProvider planner) {
        this.tuning = Objects.requireNonNull(tuning);
        this.planner = Objects.requireNonNull(planner);
    }

    PlanCandidate selectInterceptCandidate(GameStatusModel model, Player me, PlanCandidate normal) {
        if (!tuning.enableIntercept) return null;
        ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) return null;

        Bait targetBait = (normal != null && normal.target() != null) ? normal.target().bait() : null;
        if (targetBait == null) return null;

        if (targetBait.getBaitType() == BaitType.COFFEE && !tuning.enableInterceptCoffee) {
            Deque<Action> ourToTarget = planner.planFor(me, Target.of(targetBait, 0), model);
            if (!(ourToTarget != null && ourToTarget.size() <= 4)) return null;
        }

        int raceMargin = (targetBait.getBaitType() == BaitType.GEM) ? tuning.interceptMargin : 0;
        GridPos probe = Target.of(targetBait, 0).pos();
        PlanCandidate best = null;
        int k = tuning.interceptKSteps;
        for (int i = 0; i < k; i++) {
            GridPos pos = new GridPos(
                Math.max(1, Math.min(model.getMaze().getWidth()-2, probe.x())),
                Math.max(1, Math.min(model.getMaze().getHeight()-2, probe.y()))
            );
            Target interceptPoint = Target.of(pos, 0);

            for (Player opp : players.values()) {
                if (opp == null || opp.getID() == me.getID()) continue;
                Deque<Action> ourPlan = planner.planFor(me, interceptPoint, model);
                Deque<Action> oppPlan = planner.planFor(opp, interceptPoint, model);
                if (ourPlan == null || oppPlan == null || ourPlan.isEmpty() || oppPlan.isEmpty()) continue;
                double ourCost = CostUtil.planCost(ourPlan);
                double oppCost = CostUtil.planCost(oppPlan);
                if (ourCost + raceMargin <= oppCost) {
                    double score = -ourCost + ((targetBait.getBaitType() == BaitType.GEM) ? 1.0 : 0.0);
                    PlanCandidate cand = new PlanCandidate(interceptPoint, new ArrayDeque<>(ourPlan), score);
                    if (best == null || cand.score() > best.score()) best = cand;
                }
            }
            probe = new GridPos(probe.x() + ((i % 2 == 0) ? 1 : -1), probe.y() + ((i % 3 == 0) ? 1 : -1));
        }
        return best;
    }
}

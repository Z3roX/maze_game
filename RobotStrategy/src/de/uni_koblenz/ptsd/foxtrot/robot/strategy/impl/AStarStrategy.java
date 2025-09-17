package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.Deque;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

public final class AStarStrategy implements Strategy {

    private Deque<Action> currentPlan = new ArrayDeque<>();
    private final Pathfinder pathfinder = new AStarPathfinder();

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        if (model == null || me == null) {
            return Action.IDLE;
        }
        if (!currentPlan.isEmpty()) {
            return currentPlan.pollFirst();
        }

        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            return Action.IDLE;
        }

        Bait bestBait = null;
        Deque<Action> bestPlan = null;
        double bestCost = Double.POSITIVE_INFINITY;

        for (Bait bait : baitMap.values()) {
            if (bait == null) continue;
            if (!bait.isVisible()) continue;
            if (bait.getBaitType() == BaitType.TRAP) continue;

            Target target = Target.of(bait, 0);
            Deque<Action> plan = pathfinder.plan(me, target, model);
            if (plan.isEmpty()) continue;

            double cost = CostUtil.planCost(plan);
            if (cost < bestCost || (cost == bestCost && betterTieBreak(bait, bestBait, me))) {
                bestCost = cost;
                bestBait = bait;
                bestPlan = plan;
            }
        }

        if (bestPlan == null) {
            return Action.IDLE;
        }

        currentPlan = bestPlan;
        return currentPlan.pollFirst();
    }

    private static boolean betterTieBreak(Bait candidate, Bait incumbent, Player me) {
        if (incumbent == null) {
            return true;
        }
        int candDist = manhattan(candidate, me);
        int incDist = manhattan(incumbent, me);
        if (candDist != incDist) {
            return candDist < incDist;
        }
        if (candidate.getxPosition() != incumbent.getxPosition()) {
            return candidate.getxPosition() < incumbent.getxPosition();
        }
        return candidate.getyPosition() < incumbent.getyPosition();
    }

    private static int manhattan(Bait bait, Player me) {
        return Math.abs(bait.getxPosition() - me.getxPosition())
                + Math.abs(bait.getyPosition() - me.getyPosition());
    }
}

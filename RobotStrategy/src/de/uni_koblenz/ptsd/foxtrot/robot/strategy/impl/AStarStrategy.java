package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Objects;

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
        if (!currentPlan.isEmpty()) {
            return currentPlan.pollFirst();
        }

        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            return Action.IDLE;
        }
        Collection<Bait> baits = baitMap.values();

        Bait target = baits.stream()
                .filter(Objects::nonNull)
                .filter(Bait::isVisible)
                .filter(b -> b.getBaitType() != BaitType.TRAP)
                .min(Comparator.comparingInt(b ->
                        Math.abs(b.getxPosition() - me.getxPosition()) + Math.abs(b.getyPosition() - me.getyPosition())))
                .orElse(null);

        if (target == null) {
            return Action.IDLE;
        }

        Target tgt = Target.of(target, 0);
        currentPlan = pathfinder.plan(me, tgt, model);
        if (currentPlan.isEmpty()) {
            return Action.IDLE;
        }
        return currentPlan.pollFirst();
    }
}

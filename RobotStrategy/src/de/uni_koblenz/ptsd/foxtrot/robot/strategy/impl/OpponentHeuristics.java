package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

/** Opponent ETA and pressure heuristics. */
final class OpponentHeuristics {
    private OpponentHeuristics() {}

    static double estimateOpponentCost(GridPos goal, Player me, GameStatusModel model, int topLOpponents, PlanProvider planner) {
        ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        List<Player> opps = new ArrayList<>();
        for (Player p : players.values()) {
            if (p == null) continue;
            if (me.getID() == p.getID()) continue;
            opps.add(p);
        }
        opps.sort((a,b) -> {
            int da = Math.abs(a.getxPosition()-goal.x()) + Math.abs(a.getyPosition()-goal.y());
            int db = Math.abs(b.getxPosition()-goal.x()) + Math.abs(b.getyPosition()-goal.y());
            return Integer.compare(da, db);
        });
        if (opps.size() > topLOpponents) opps = opps.subList(0, topLOpponents);

        double best = Double.POSITIVE_INFINITY;
        for (Player player : opps) {
            Deque<de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action> oppPlan =
                    planner.planFor(player, Target.of(goal, 0), model);
            if (oppPlan != null && !oppPlan.isEmpty()) {
                best = Math.min(best, CostUtil.planCost(oppPlan));
            }
        }
        return best;
    }

    static double multiOpponentPenalty(GridPos goal, Player me, GameStatusModel model, double ourCost, int nearExtra, double weight) {
        javafx.collections.ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) {
            return 0.0;
        }
        int close = 0;
        for (Player p : players.values()) {
            if (p == null) continue;
            if (me.getID() == p.getID()) continue;
            int d = Math.abs(p.getxPosition() - goal.x()) + Math.abs(p.getyPosition() - goal.y());
            if (d <= ourCost + nearExtra) {
                close++;
            }
        }
        return weight * close;
    }
}

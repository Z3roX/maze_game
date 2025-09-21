package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import javafx.collections.ObservableMap;

/**
* Lightweight opponent-competition heuristics used by the smart strategy.
*
* <p>Given a goal cell, this utility estimates how many opponents can plausibly
* contest the goal before or around the same time as us. It is designed to be
* fast: it first filters by Manhattan distance and, for a configurable number
* of nearest opponents, it may consult a supplied route planner for a more
* faithful cost. The result is returned as a weighted "pressure" value that
* indicates how much opponent competition to expect.
*
* <p>This class is stateless and not thread-safe by itself, but it holds no
* mutable global state and can be freely re-used.
*/

final class OpponentHeuristics {
    private OpponentHeuristics() {}


    /**
    * Estimate opponent pressure near a goal.
    *
    * <p>Counts (with weights) opponents that could reach the {@code goal}
    * not much slower than we can. A cheap Manhattan pre-check keeps it fast.
    * Optionally, the provided {@code planner} is used for more accurate costs
    * for a subset of opponents.
    *
    * @param goal target cell to evaluate
    * @param me our player (excluded from the opponent set)
    * @param model current game state providing the set of players
    * @param topLOpponents consider at most this many nearest opponents for
    * expensive re-evaluation (by cost)
    * @param planner callback that returns a planned route to {@code goal}
    * for a given player; may be ignored for some players
    * @return a non-negative pressure value; larger means stronger opponent
    * competition/likelihood of interception
    *
    * @implNote Internally this method uses Manhattan distance as a quick bound
    * and compares it against our own estimated route cost plus a small
    * slack. Opponents within that bound increment a counter that is
    * finally scaled by a weight.
    */
    static double estimateOpponentCost(GridPos goal, Player me, GameStatusModel model, int topLOpponents,
            Function<Player, AStarPathfinder.Result> planner) {
        ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        List<Player> opps = new ArrayList<>();
        for (Player p : players.values()) {
            if (p == null) {
                continue;
            }
            if (me.getID() == p.getID()) {
                continue;
            }
            opps.add(p);
        }
        opps.sort((a, b) -> {
            int da = Math.abs(a.getxPosition() - goal.x()) + Math.abs(a.getyPosition() - goal.y());
            int db = Math.abs(b.getxPosition() - goal.x()) + Math.abs(b.getyPosition() - goal.y());
            return Integer.compare(da, db);
        });
        if (opps.size() > topLOpponents) {
            opps = opps.subList(0, topLOpponents);
        }

        double best = Double.POSITIVE_INFINITY;
        for (Player player : opps) {
            AStarPathfinder.Result route = planner.apply(player);
            if (route != null && route.success() && !route.actions().isEmpty()) {
                best = Math.min(best, route.cost());
            }
        }
        return best;
    }

    static double multiOpponentPenalty(GridPos goal, Player me, GameStatusModel model, double ourCost, int nearExtra,
            double weight) {
        ObservableMap<Integer, Player> players = model.getPlayers();
        if (players == null || players.isEmpty()) {
            return 0.0;
        }
        int close = 0;
        for (Player p : players.values()) {
            if (p == null) {
                continue;
            }
            if (me.getID() == p.getID()) {
                continue;
            }
            int d = Math.abs(p.getxPosition() - goal.x()) + Math.abs(p.getyPosition() - goal.y());
            if (d <= ourCost + nearExtra) {
                close++;
            }
        }
        return weight * close;
    }
}


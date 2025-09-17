package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;

/**
 * Path planner that approximates an R* search by using a weighted A* evaluation function and
 * additional turn costs. This favours shorter paths that require fewer re-orientations.
 */
public class RStarStrategy extends ShortestPathStrategy {
    private static final double TURN_COST = 0.4;
    private static final double HEURISTIC_WEIGHT = 1.1;
    private static final double EPSILON = 1e-9;

    @Override
    protected List<GridPos> findPath(Maze maze, GridPos start, GridPos goal, Set<GridPos> traps, Direction startDir) {
        int width = maze.getWidth();
        int height = maze.getHeight();

        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> {
            int cmp = Double.compare(a.fScore, b.fScore);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Double.compare(a.gScore, b.gScore);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
        Map<GridPos, Double> gScore = new HashMap<>();
        Map<GridPos, GridPos> parent = new HashMap<>();

        Node startNode = new Node(start, 0.0, heuristic(start, goal), startDir);
        open.add(startNode);
        gScore.put(start, 0.0);

        while (!open.isEmpty()) {
            Node current = open.poll();
            double bestKnown = gScore.getOrDefault(current.position, Double.POSITIVE_INFINITY);
            if (current.gScore - bestKnown > EPSILON) {
                continue;
            }
            if (current.position.equals(goal)) {
                return reconstructPath(parent, goal, start);
            }
            for (GridPos next : neighbours(current.position)) {
                if (!inBounds(next, width, height)) {
                    continue;
                }
                if (!isWalkable(maze, next, traps, goal, start)) {
                    continue;
                }
                Direction moveDir = directionTowards(current.position, next);
                if (moveDir == null) {
                    continue;
                }
                double stepCost = 1.0;
                if (current.direction != null && current.direction != moveDir) {
                    stepCost += TURN_COST;
                }
                double tentative = current.gScore + stepCost;
                double best = gScore.getOrDefault(next, Double.POSITIVE_INFINITY);
                if (tentative + EPSILON < best) {
                    gScore.put(next, tentative);
                    parent.put(next, current.position);
                    double estimate = tentative + HEURISTIC_WEIGHT * heuristic(next, goal);
                    open.add(new Node(next, tentative, estimate, moveDir));
                }
            }
        }

        return null;
    }

    private double heuristic(GridPos from, GridPos to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y());
    }

    private static final class Node {
        final GridPos position;
        final double gScore;
        final double fScore;
        final Direction direction;

        Node(GridPos position, double gScore, double fScore, Direction direction) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.direction = direction;
        }
    }
}

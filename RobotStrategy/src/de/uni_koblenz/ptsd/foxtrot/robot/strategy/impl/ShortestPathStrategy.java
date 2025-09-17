package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import javafx.collections.ObservableMap;

/**
 * Extremely small A*-like strategy that simply walks the shortest path to the nearest safe bait.
 */
public class ShortestPathStrategy implements Strategy {
    private static final Logger LOG = Logger.getLogger(ShortestPathStrategy.class.getName());

    private final Deque<Action> currentPlan = new ArrayDeque<>();
    private GridPos currentTarget;

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(me, "player");

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(() -> String.format(
                    "decideNext(): position=(%d,%d) direction=%s planSize=%d target=%s",
                    me.getxPosition(), me.getyPosition(), me.getDirection(), this.currentPlan.size(),
                    this.currentTarget));
        }

        if (needsReplan(model)) {
            LOG.fine("Plan invalid or missing – triggering replanning");
            rebuildPlan(model, me);
        }

        if (this.currentPlan.isEmpty()) {
            LOG.fine("No plan available – returning IDLE");
            return Action.IDLE;
        }

        Action next = this.currentPlan.pollFirst();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(() -> String.format("Next action: %s (remainingPlan=%s)", next,
                    formatActions(this.currentPlan)));
        }
        return next;
    }

    @Override
    public void reset() {
        LOG.fine("Resetting strategy state");
        this.currentPlan.clear();
        this.currentTarget = null;
    }

    private boolean needsReplan(GameStatusModel model) {
        if (this.currentPlan.isEmpty() || this.currentTarget == null) {
            LOG.fine("Replan required: plan empty or target missing");
            return true;
        }
        ObservableMap<Integer, Bait> baits = model.getBaits();
        if (baits == null || baits.isEmpty()) {
            LOG.fine("Replan required: no baits available in model");
            return true;
        }
        boolean targetStillValid = baits.values().stream().anyMatch(
                b -> b != null && b.isVisible() && b.getBaitType() != BaitType.TRAP && isAtTarget(b));
        if (!targetStillValid) {
            LOG.fine(() -> String.format("Replan required: current target %s no longer valid", this.currentTarget));
        }
        return !targetStillValid;
    }

    private void rebuildPlan(GameStatusModel model, Player me) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(() -> String.format("Rebuilding plan from position (%d,%d) direction=%s", me.getxPosition(),
                    me.getyPosition(), me.getDirection()));
        }
        this.currentPlan.clear();
        this.currentTarget = null;

        Maze maze = model.getMaze();
        if (maze == null) {
            LOG.warning("Cannot rebuild plan: maze is null");
            return;
        }
        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            LOG.fine("Cannot rebuild plan: no baits available");
            return;
        }

        List<Bait> candidates = new ArrayList<>();
        for (Bait bait : baitMap.values()) {
            if (bait == null || !bait.isVisible() || bait.getBaitType() == BaitType.TRAP) {
                continue;
            }
            candidates.add(bait);
        }
        if (candidates.isEmpty()) {
            LOG.fine("No visible non-trap bait candidates available");
            return;
        }

        GridPos start = new GridPos(me.getxPosition(), me.getyPosition());
        Direction startDir = me.getDirection() != null ? me.getDirection() : Direction.N;
        Map<GridPos, List<GridPos>> paths = new HashMap<>();
        Set<GridPos> traps = collectTrapPositions(baitMap.values());

        int bestLength = Integer.MAX_VALUE;
        List<GridPos> bestPath = null;
        GridPos bestTarget = null;
        for (Bait bait : candidates) {
            GridPos goal = new GridPos(bait.getxPosition(), bait.getyPosition());
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(() -> String.format("Evaluating bait at %s (type=%s)", goal, bait.getBaitType()));
            }
            List<GridPos> path = paths.computeIfAbsent(goal, key -> findShortestPath(maze, start, key, traps));
            if (path == null) {
                LOG.fine(() -> String.format("No path from %s to candidate %s", start, goal));
                continue;
            }
            int pathLength = path.size() - 1; // tiles to move
            if (pathLength < bestLength) {
                bestLength = pathLength;
                bestPath = path;
                bestTarget = goal;
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(() -> String.format("New best target %s with path length %d (path=%s)", bestTarget,
                            bestLength, formatPath(bestPath)));
                }
            }
        }

        if (bestPath == null || bestPath.size() < 2) {
            LOG.warning("Rebuild failed: no viable path found to any bait");
            return;
        }

        this.currentPlan.addAll(convertPathToActions(bestPath, startDir));
        this.currentTarget = bestTarget;
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(() -> String.format("Planned path to %s with %d actions: %s", this.currentTarget,
                    this.currentPlan.size(), formatActions(this.currentPlan)));
        }
    }

    private boolean isAtTarget(Bait bait) {
        return this.currentTarget != null && bait.getxPosition() == this.currentTarget.x()
                && bait.getyPosition() == this.currentTarget.y();
    }

    private Set<GridPos> collectTrapPositions(Iterable<Bait> baits) {
        Set<GridPos> traps = new HashSet<>();
        for (Bait bait : baits) {
            if (bait != null && bait.isVisible() && bait.getBaitType() == BaitType.TRAP) {
                traps.add(new GridPos(bait.getxPosition(), bait.getyPosition()));
            }
        }
        return traps;
    }

    private List<GridPos> findShortestPath(Maze maze, GridPos start, GridPos goal, Set<GridPos> traps) {
        int width = maze.getWidth();
        int height = maze.getHeight();
        boolean[][] visited = new boolean[height][width];
        Queue<GridPos> queue = new ArrayDeque<>();
        Map<GridPos, GridPos> parent = new HashMap<>();

        queue.add(start);
        visited[start.y()][start.x()] = true;

        while (!queue.isEmpty()) {
            GridPos current = queue.poll();
            if (current.equals(goal)) {
                break;
            }
            for (GridPos next : neighbours(current)) {
                if (!inBounds(next, width, height)) {
                    continue;
                }
                if (visited[next.y()][next.x()]) {
                    continue;
                }
                if (!isWalkable(maze, next, traps, goal, start)) {
                    continue;
                }
                visited[next.y()][next.x()] = true;
                parent.put(next, current);
                queue.add(next);
            }
        }

        if (!visited[goal.y()][goal.x()]) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(() -> String.format("BFS terminated without reaching goal %s from %s", goal, start));
            }
            return null;
        }

        LinkedList<GridPos> path = new LinkedList<>();
        GridPos step = goal;
        path.addFirst(step);
        while (!step.equals(start)) {
            step = parent.get(step);
            if (step == null) {
                LOG.warning("Path reconstruction failed due to missing parent");
                return null;
            }
            path.addFirst(step);
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(() -> String.format("Found path: %s", formatPath(path)));
        }
        return path;
    }

    private boolean isWalkable(Maze maze, GridPos pos, Set<GridPos> traps, GridPos goal, GridPos start) {
        if (!pos.equals(goal) && traps.contains(pos)) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(() -> String.format("Tile %s blocked because it is a visible trap", pos));
            }
            return false;
        }
        CellType type = maze.getTypeAt(pos.x(), pos.y());
        if (pos.equals(start)) {
            return type != CellType.WALL && type != CellType.WATER;
        }
        return type == CellType.PATH;
    }

    private boolean inBounds(GridPos pos, int width, int height) {
        return pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height;
    }

    private List<GridPos> neighbours(GridPos pos) {
        List<GridPos> neighbours = new ArrayList<>(4);
        neighbours.add(new GridPos(pos.x(), pos.y() - 1));
        neighbours.add(new GridPos(pos.x() + 1, pos.y()));
        neighbours.add(new GridPos(pos.x(), pos.y() + 1));
        neighbours.add(new GridPos(pos.x() - 1, pos.y()));
        return neighbours;
    }

    private Deque<Action> convertPathToActions(List<GridPos> path, Direction startDir) {
        Deque<Action> actions = new ArrayDeque<>();
        Direction dir = startDir;
        GridPos current = path.get(0);
        for (int i = 1; i < path.size(); i++) {
            GridPos next = path.get(i);
            Direction needed = directionTowards(current, next);
            if (needed == null) {
                LOG.warning(() -> String.format("Non-adjacent steps in path between %s and %s", current, next));
                return new ArrayDeque<>();
            }
            addTurns(actions, dir, needed);
            dir = needed;
            actions.add(Action.STEP);
            current = next;
        }
        return actions;
    }

    private Direction directionTowards(GridPos from, GridPos to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (dx == 1 && dy == 0) {
            return Direction.E;
        }
        if (dx == -1 && dy == 0) {
            return Direction.W;
        }
        if (dx == 0 && dy == 1) {
            return Direction.S;
        }
        if (dx == 0 && dy == -1) {
            return Direction.N;
        }
        return null;
    }

    private void addTurns(Deque<Action> actions, Direction current, Direction needed) {
        if (current == needed) {
            return;
        }
        int currentIdx = directionIndex(current);
        int targetIdx = directionIndex(needed);
        int diff = (targetIdx - currentIdx + 4) % 4;
        switch (diff) {
        case 0 -> {
        }
        case 1 -> {
            actions.add(Action.TURN_RIGHT);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(() -> String.format("Added TURN_RIGHT to rotate from %s to %s", current, needed));
            }
        }
        case 2 -> {
            actions.add(Action.TURN_RIGHT);
            actions.add(Action.TURN_RIGHT);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(() -> String.format("Added 180 degree turn from %s to %s", current, needed));
            }
        }
        case 3 -> {
            actions.add(Action.TURN_LEFT);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(() -> String.format("Added TURN_LEFT to rotate from %s to %s", current, needed));
            }
        }
        default -> {
        }
        }
    }

    private int directionIndex(Direction dir) {
        return switch (dir) {
        case N -> 0;
        case E -> 1;
        case S -> 2;
        case W -> 3;
        };
    }

    private static String formatPath(List<GridPos> path) {
        return path == null ? "[]" : path.toString();
    }

    private static String formatActions(Deque<Action> actions) {
        return actions == null ? "[]" : actions.toString();
    }
}

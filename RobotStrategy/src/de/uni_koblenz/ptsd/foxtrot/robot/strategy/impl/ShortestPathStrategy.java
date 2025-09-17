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
    private final Deque<Action> currentPlan = new ArrayDeque<>();
    private GridPos currentTarget;

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(me, "player");

        if (needsReplan(model)) {
            rebuildPlan(model, me);
        }

        if (this.currentPlan.isEmpty()) {
            return Action.IDLE;
        }

        return this.currentPlan.pollFirst();
    }

    @Override
    public void reset() {
        this.currentPlan.clear();
        this.currentTarget = null;
    }

    private boolean needsReplan(GameStatusModel model) {
        if (this.currentPlan.isEmpty() || this.currentTarget == null) {
            return true;
        }
        ObservableMap<Integer, Bait> baits = model.getBaits();
        if (baits == null || baits.isEmpty()) {
            return true;
        }
        return baits.values().stream().noneMatch(b -> b != null && b.isVisible()
                && b.getBaitType() != BaitType.TRAP && isAtTarget(b));
    }

    private void rebuildPlan(GameStatusModel model, Player me) {
        this.currentPlan.clear();
        this.currentTarget = null;

        Maze maze = model.getMaze();
        if (maze == null) {
            return;
        }
        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
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
            List<GridPos> path = paths.computeIfAbsent(goal, key -> findShortestPath(maze, start, key, traps));
            if (path == null) {
                continue;
            }
            int pathLength = path.size() - 1; // tiles to move
            if (pathLength < bestLength) {
                bestLength = pathLength;
                bestPath = path;
                bestTarget = goal;
            }
        }

        if (bestPath == null || bestPath.size() < 2) {
            return;
        }

        this.currentPlan.addAll(convertPathToActions(bestPath, startDir));
        this.currentTarget = bestTarget;
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
            return null;
        }

        LinkedList<GridPos> path = new LinkedList<>();
        GridPos step = goal;
        path.addFirst(step);
        while (!step.equals(start)) {
            step = parent.get(step);
            if (step == null) {
                return null;
            }
            path.addFirst(step);
        }
        return path;
    }

    private boolean isWalkable(Maze maze, GridPos pos, Set<GridPos> traps, GridPos goal, GridPos start) {
        if (!pos.equals(goal) && traps.contains(pos)) {
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
        case 1 -> actions.add(Action.TURN_RIGHT);
        case 2 -> {
            actions.add(Action.TURN_RIGHT);
            actions.add(Action.TURN_RIGHT);
        }
        case 3 -> actions.add(Action.TURN_LEFT);
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
}

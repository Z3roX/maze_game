package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * A* inspired strategy that walks the shortest path to the nearest safe bait and continuously
 * validates that the planned actions still match the observed player state.
 */
public class ShortestPathStrategy implements Strategy {
    private List<GridPos> currentPath;
    private GridPos currentTarget;
    private int pathCursor;
    private boolean replanRequested = true;
    private GridPos lastKnownPosition;
    private Direction lastKnownDirection;
    private Action lastIssuedAction = Action.IDLE;

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(me, "player");

        updateRobotState(me);
        if (needsReplan(model)) {
            rebuildPlan(model, me);
        }

        if (!hasActivePath() || this.pathCursor >= this.currentPath.size() - 1) {
            this.lastIssuedAction = Action.IDLE;
            return Action.IDLE;
        }

        GridPos current = new GridPos(me.getxPosition(), me.getyPosition());
        GridPos next = this.currentPath.get(this.pathCursor + 1);
        Direction needed = directionTowards(current, next);
        if (needed == null) {
            this.replanRequested = true;
            this.lastIssuedAction = Action.IDLE;
            return Action.IDLE;
        }

        Direction currentDir = normalizeDirection(me.getDirection());
        Action action = currentDir == needed ? Action.STEP : rotationTowards(currentDir, needed);

        this.lastIssuedAction = action;
        this.lastKnownPosition = current;
        this.lastKnownDirection = currentDir;
        return action;
    }

    @Override
    public void reset() {
        this.currentPath = null;
        this.currentTarget = null;
        this.pathCursor = 0;
        this.replanRequested = true;
        this.lastKnownPosition = null;
        this.lastKnownDirection = null;
        this.lastIssuedAction = Action.IDLE;
    }

    private boolean needsReplan(GameStatusModel model) {
        if (this.replanRequested) {
            return true;
        }
        if (!hasActivePath()) {
            return hasVisibleSafeBait(model.getBaits());
        }
        if (this.pathCursor >= this.currentPath.size() - 1) {
            return true;
        }
        ObservableMap<Integer, Bait> baits = model.getBaits();
        if (baits == null || baits.isEmpty()) {
            return true;
        }
        return baits.values().stream()
                .noneMatch(b -> b != null && b.isVisible() && b.getBaitType() != BaitType.TRAP && isAtTarget(b));
    }

    private boolean hasActivePath() {
        return this.currentPath != null && !this.currentPath.isEmpty() && this.currentTarget != null;
    }

    private boolean hasVisibleSafeBait(ObservableMap<Integer, Bait> baitMap) {
        if (baitMap == null || baitMap.isEmpty()) {
            return false;
        }
        for (Bait bait : baitMap.values()) {
            if (bait != null && bait.isVisible() && bait.getBaitType() != BaitType.TRAP) {
                return true;
            }
        }
        return false;
    }

    private void rebuildPlan(GameStatusModel model, Player me) {
        this.currentTarget = null;
        this.currentPath = null;
        this.pathCursor = 0;

        Maze maze = model.getMaze();
        if (maze == null) {
            this.replanRequested = false;
            return;
        }
        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            this.replanRequested = false;
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
            this.replanRequested = false;
            return;
        }

        GridPos start = new GridPos(me.getxPosition(), me.getyPosition());
        Direction startDir = normalizeDirection(me.getDirection());
        Map<GridPos, List<GridPos>> paths = new HashMap<>();
        Set<GridPos> traps = collectTrapPositions(baitMap.values());

        int bestLength = Integer.MAX_VALUE;
        List<GridPos> bestPath = null;
        GridPos bestTarget = null;
        for (Bait bait : candidates) {
            GridPos goal = new GridPos(bait.getxPosition(), bait.getyPosition());
            List<GridPos> path = paths.computeIfAbsent(goal,
                    key -> findPath(maze, start, key, traps, startDir));
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
            this.replanRequested = false;
            return;
        }

        this.currentPath = bestPath;
        this.currentTarget = bestTarget;
        int index = bestPath.indexOf(start);
        this.pathCursor = index >= 0 ? index : 0;
        this.replanRequested = false;
        this.lastIssuedAction = Action.IDLE;
    }

    private boolean isAtTarget(Bait bait) {
        return this.currentTarget != null && bait.getxPosition() == this.currentTarget.x()
                && bait.getyPosition() == this.currentTarget.y();
    }

    private void updateRobotState(Player me) {
        GridPos position = new GridPos(me.getxPosition(), me.getyPosition());
        Direction direction = normalizeDirection(me.getDirection());

        if (this.lastKnownPosition != null) {
            if (this.lastIssuedAction == Action.STEP && position.equals(this.lastKnownPosition)) {
                this.replanRequested = true;
            }
        }

        if (hasActivePath()) {
            int index = this.currentPath.indexOf(position);
            if (index < 0) {
                this.replanRequested = true;
            } else if (index < this.pathCursor) {
                this.replanRequested = true;
            } else {
                this.pathCursor = index;
            }
        }

        this.lastKnownPosition = position;
        this.lastKnownDirection = direction;
    }

    private Direction normalizeDirection(Direction direction) {
        if (direction != null) {
            return direction;
        }
        if (this.lastKnownDirection != null) {
            return this.lastKnownDirection;
        }
        return Direction.N;
    }

    private Action rotationTowards(Direction current, Direction needed) {
        int currentIdx = directionIndex(current);
        int targetIdx = directionIndex(needed);
        int diff = (targetIdx - currentIdx + 4) % 4;
        return switch (diff) {
        case 1 -> Action.TURN_RIGHT;
        case 2 -> Action.TURN_LEFT;
        case 3 -> Action.TURN_LEFT;
        default -> Action.IDLE;
        };
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

    protected List<GridPos> findPath(Maze maze, GridPos start, GridPos goal, Set<GridPos> traps, Direction startDir) {
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
                return reconstructPath(parent, goal, start);
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

        return null;
    }

    protected List<GridPos> reconstructPath(Map<GridPos, GridPos> parent, GridPos goal, GridPos start) {
        List<GridPos> reversed = new ArrayList<>();
        GridPos step = goal;
        reversed.add(step);
        while (!step.equals(start)) {
            step = parent.get(step);
            if (step == null) {
                return null;
            }
            reversed.add(step);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    protected boolean isWalkable(Maze maze, GridPos pos, Set<GridPos> traps, GridPos goal, GridPos start) {
        if (!pos.equals(goal) && traps.contains(pos)) {
            return false;
        }
        CellType type = maze.getTypeAt(pos.x(), pos.y());
        if (pos.equals(start)) {
            return type != CellType.WALL && type != CellType.WATER;
        }
        return type == CellType.PATH;
    }

    protected boolean inBounds(GridPos pos, int width, int height) {
        return pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height;
    }

    protected List<GridPos> neighbours(GridPos pos) {
        List<GridPos> neighbours = new ArrayList<>(4);
        neighbours.add(new GridPos(pos.x(), pos.y() - 1));
        neighbours.add(new GridPos(pos.x() + 1, pos.y()));
        neighbours.add(new GridPos(pos.x(), pos.y() + 1));
        neighbours.add(new GridPos(pos.x() - 1, pos.y()));
        return neighbours;
    }

    protected Direction directionTowards(GridPos from, GridPos to) {
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

    private int directionIndex(Direction dir) {
        return switch (dir) {
        case N -> 0;
        case E -> 1;
        case S -> 2;
        case W -> 3;
        };
    }
}

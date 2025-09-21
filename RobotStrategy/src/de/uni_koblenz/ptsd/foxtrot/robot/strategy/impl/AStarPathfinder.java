package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;

/**
 * Grid-based path planner with a simple cost model for a mobile robot.
 *
 * <p>This planner computes a path on a 4-connected grid (N, E, S, W). It uses a
 * breadth-first search (BFS) on the grid to obtain the shortest path in number
 * of steps. The step sequence is then converted into concrete {@link Action}s,
 * inserting left/right turns as needed based on the robot's current facing.
 *
 * <p>The cost model assigns {@link #COST_STEP} to each forward step and
 * {@link #COST_TURN} to each 90° turn. The {@link #estimateCost(Pose, GridPos)}
 * method provides an admissible and fast estimate (Manhattan distance plus a
 * minimal number of turns) that can be used as a heuristic by callers.
 *
 * <p>Note: Although the name suggests A*, this implementation currently uses
 * BFS for path extraction and applies the cost model afterwards.
 */
final class AStarPathfinder {

    /** Cost added for a single forward step. */
    static final double COST_STEP = 1.0;
    /** Cost added for a single 90° turn (left or right). */
    static final double COST_TURN = 0.5;

    /**
     * Immutable robot pose on the grid.
     *
     * @param x      grid x-coordinate (column)
     * @param y      grid y-coordinate (row)
     * @param facing current facing; may be {@code null} to assume {@link Direction#N}
     */
    static final record Pose(int x, int y, Direction facing) {}
    /**
     * Result of a planning request.
     *
     * @param actions ordered, immutable list of actions to reach the goal
     * @param cost    total cost of {@code actions} under the current cost model
     * @param success whether a feasible path within the optional cost cap was found
     */
    static final record Result(List<Action> actions, double cost, boolean success) {
        /** Sentinel for "no plan available". */
        static final Result EMPTY = new Result(List.of(), Double.POSITIVE_INFINITY, false);
    }

    /** 4-neighborhood step deltas in order N, E, S, W. */
    private static final int[][] DELTAS = new int[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
    /** Directions in the same order as {@link #DELTAS}. */
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.N, Direction.E, Direction.S, Direction.W };

    /**
     * Internal BFS node used to reconstruct the shortest step sequence.
     * Each node stores its grid position, the parent node, and the direction
     * of the step taken from the parent to this node.
     */
    private static final class Node {
        final int x;
        final int y;
        final Node parent;
        final Direction step;

        Node(int x, int y, Node parent, Direction step) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.step = step;
        }
    }

    /**
     * Plans a path from {@code start} to {@code goal} without a cost cap.
     *
     * @param maze  the world model used for collision checks
     * @param start start pose (grid coordinates and initial facing)
     * @param goal  goal cell to reach
     * @return a {@link Result}: {@link Result#success()} is {@code true} when a plan exists;
     *         otherwise {@link Result#EMPTY}.
     */
    Result plan(Maze maze, Pose start, GridPos goal) {
        return plan(maze, start, goal, Double.POSITIVE_INFINITY);
    }

    /**
     * Plans a path from {@code start} to {@code goal} honoring a cost cap.
     *
     * <p>A BFS produces the shortest sequence of forward steps (if any). The method
     * then inserts the minimal set of turns to follow that sequence starting from
     * {@code start.facing()}. If the resulting total {@linkplain #costOf(List) cost}
     * exceeds {@code costCap}, the returned result has {@code success = false} and
     * an empty action list (the {@code cost} still reflects the found plan).
     *
     * @param maze    the environment grid
     * @param start   start pose
     * @param goal    goal cell
     * @param costCap upper bound on acceptable total cost
     * @return planning {@link Result}
     */
    Result plan(Maze maze, Pose start, GridPos goal, double costCap) {
        if (maze == null || start == null || goal == null) {
            return Result.EMPTY;
        }
        Node solution = bfs(start.x(), start.y(), goal.x(), goal.y(), maze);
        if (solution == null) {
            return Result.EMPTY;
        }

        List<Direction> steps = new ArrayList<>();
        for (Node cursor = solution; cursor != null && cursor.step != null; cursor = cursor.parent) {
            steps.add(cursor.step);
        }
        Collections.reverse(steps);

        Direction facing = (start.facing() != null) ? start.facing() : Direction.N;
        ArrayList<Action> actions = new ArrayList<>();
        for (Direction stepDir : steps) {
            facing = align(facing, stepDir, actions);
            actions.add(Action.STEP);
        }

        double cost = costOf(actions);
        if (cost > costCap) {
            return new Result(List.of(), cost, false);
        }
        return new Result(List.copyOf(actions), cost, true);
    }

    /**
     * Fast lower-bound estimate of the cost from {@code start} to {@code goal}.
     *
     * <p>Uses Manhattan distance for steps and adds the minimal number of 90° turns
     * required to point roughly toward the goal based on the dominant axis.
     * If {@code start.facing()} is {@code null}, only step cost is returned.
     *
     * @param start start pose
     * @param goal  goal cell
     * @return estimated cost (never {@code NaN})
     */
    double estimateCost(Pose start, GridPos goal) {
        if (start == null || goal == null) {
            return Double.POSITIVE_INFINITY;
        }
        int dx = Math.abs(goal.x() - start.x());
        int dy = Math.abs(goal.y() - start.y());
        double stepCost = (dx + dy) * COST_STEP;
        if (dx == 0 && dy == 0) {
            return 0.0;
        }
        Direction facing = start.facing();
        if (facing == null) {
            return stepCost;
        }
        Direction preferred;
        if (dx >= dy && dx > 0) {
            preferred = (goal.x() >= start.x()) ? Direction.E : Direction.W;
        } else {
            preferred = (goal.y() >= start.y()) ? Direction.S : Direction.N;
        }
        int turns = turnsBetween(facing, preferred);
        return stepCost + turns * COST_TURN;
    }

    /**
     * Computes the total cost of a sequence of actions.
     *
     * @param actions list of actions
     * @return sum of {@link #COST_STEP} and {@link #COST_TURN} over the list
     */
    static double costOf(List<Action> actions) {
        double cost = 0.0;
        for (Action action : actions) {
            cost += costOf(action);
        }
        return cost;
    }

    /**
     * Returns the cost contribution of a single action.
     *
     * @param action action to evaluate
     * @return {@link #COST_TURN} for left/right turns, {@link #COST_STEP} for a step; 0 otherwise
     */
    static double costOf(Action action) {
        return switch (action) {
        case TURN_LEFT, TURN_RIGHT -> COST_TURN;
        case STEP -> COST_STEP;
        default -> 0.0;
        };
    }

    /**
     * Breadth-first search on the grid from (startX, startY) to (goalX, goalY).
     * Blocks are determined by {@link #isWalkable(int, int, Maze)}.
     *
     * @return the final {@link Node} on success (to reconstruct the path), or {@code null}
     *         if no path exists.
     */
    private Node bfs(int startX, int startY, int goalX, int goalY, Maze maze) {
        int width = maze.getWidth();
        int height = maze.getHeight();
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(startX, startY, null, null));
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            Node current = queue.pollFirst();
            for (int i = 0; i < DELTAS.length; i++) {
                int nx = current.x + DELTAS[i][0];
                int ny = current.y + DELTAS[i][1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue;
                }
                if (visited[ny][nx]) {
                    continue;
                }
                if (!isWalkable(nx, ny, maze)) {
                    continue;
                }
                Node next = new Node(nx, ny, current, DIRECTIONS[i]);
                if (nx == goalX && ny == goalY) {
                    return next;
                }
                visited[ny][nx] = true;
                queue.add(next);
            }
        }
        return null;
    }

    /**
     * Returns whether the cell at (x, y) can be traversed.
     *
     * <p>Currently only {@link CellType#PATH} is considered walkable.
     */
    private boolean isWalkable(int x, int y, Maze maze) {
        CellType type = maze.getTypeAt(x, y);
        return type == CellType.PATH;
    }

    /**
     * Turns {@code current} toward {@code desired}, appending the necessary turn
     * actions to {@code actions}. Returns the new facing (which equals {@code desired}).
     */
    private Direction align(Direction current, Direction desired, List<Action> actions) {
        if (current == desired) {
            return desired;
        }
        int delta = Math.floorMod(index(desired) - index(current), 4);
        switch (delta) {
        case 0:
            return desired;
        case 1:
            actions.add(Action.TURN_RIGHT);
            return desired;
        case 2:
            actions.add(Action.TURN_RIGHT);
            actions.add(Action.TURN_RIGHT);
            return desired;
        case 3:
            actions.add(Action.TURN_LEFT);
            return desired;
        default:
            return desired;
        }
    }

    /** Maps {@link Direction} to an integer index 0..3 in order N,E,S,W. */
    private int index(Direction dir) {
        return switch (dir) {
        case N -> 0;
        case E -> 1;
        case S -> 2;
        case W -> 3;
        };
    }

    /**
     * Minimal number of 90° turns to rotate from {@code from} to {@code to}.
     */
    private int turnsBetween(Direction from, Direction to) {
        int delta = Math.floorMod(index(to) - index(from), 4);
        return Math.min(delta, 4 - delta);
    }
}

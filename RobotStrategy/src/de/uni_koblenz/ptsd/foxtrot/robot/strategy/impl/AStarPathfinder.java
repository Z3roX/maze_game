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

final class AStarPathfinder {

    static final double COST_STEP = 1.0;
    static final double COST_TURN = 0.5;

            static final record Pose(int x, int y, Direction facing) {}
            static final record Result(List<Action> actions, double cost, boolean success) {
        static final Result EMPTY = new Result(List.of(), Double.POSITIVE_INFINITY, false);
    }

    private static final int[][] DELTAS = new int[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.N, Direction.E, Direction.S, Direction.W };

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

    Result plan(Maze maze, Pose start, GridPos goal) {
        return plan(maze, start, goal, Double.POSITIVE_INFINITY);
    }

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

    static double costOf(List<Action> actions) {
        double cost = 0.0;
        for (Action action : actions) {
            cost += costOf(action);
        }
        return cost;
    }

    static double costOf(Action action) {
        return switch (action) {
        case TURN_LEFT, TURN_RIGHT -> COST_TURN;
        case STEP -> COST_STEP;
        default -> 0.0;
        };
    }

    private Node bfs(int startX, int startY, int goalX, int goalY, Maze maze) {
        if (startX == goalX && startY == goalY) {
            return new Node(startX, startY, null, null);
        }
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

    private boolean isWalkable(int x, int y, Maze maze) {
        CellType type = maze.getTypeAt(x, y);
        return type == CellType.PATH;
    }

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

    private int index(Direction dir) {
        return switch (dir) {
        case N -> 0;
        case E -> 1;
        case S -> 2;
        case W -> 3;
        };
    }

    private int turnsBetween(Direction from, Direction to) {
        int delta = Math.floorMod(index(to) - index(from), 4);
        return Math.min(delta, 4 - delta);
    }
}



package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

/**
 * A* pathfinder on grid with orientation. Turning costs 0.5, stepping costs 1.0.
 */
final class AStarPathfinder implements Pathfinder {

    private static record State(int x, int y, Direction dir) {}

    @Override
    public Deque<Action> plan(Player me, Target target, GameStatusModel model) {
        Deque<Action> empty = new ArrayDeque<>();
        if (me == null || target == null || model == null) return empty;
        Maze maze = model.getMaze();
        if (maze == null) return empty;

        Direction startDir = me.getDirection();
        if (startDir == null) startDir = Direction.N;

        State start = new State(me.getxPosition(), me.getyPosition(), startDir);
        State goal = new State(target.pos().x(), target.pos().y(), null);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<State, Double> gScore = new HashMap<>();
        Map<State, Node> cameFrom = new HashMap<>();

        Node startNode = new Node(start, null, 0.0, heuristic(start, goal), null);
        open.add(startNode);
        gScore.put(start, 0.0);

        Node goalNode = null;

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.state.x == goal.x && current.state.y == goal.y) {
                goalNode = current;
                break;
            }

            for (Node nbr : neighbors(model, maze, current)) {
                double tentativeG = current.g + (nbr.g - current.g); // nbr.g already = current.g + stepCost
                Double bestKnown = gScore.get(nbr.state);
                if (bestKnown == null || tentativeG < bestKnown) {
                    gScore.put(nbr.state, tentativeG);
                    double f = tentativeG + heuristic(nbr.state, goal);
                    nbr.f = f;
                    cameFrom.put(nbr.state, current);
                    open.add(nbr);
                }
            }
        }

        Deque<Action> plan = new ArrayDeque<>();
        if (goalNode == null) return plan;

        List<Action> rev = new ArrayList<>();
        Node cur = goalNode;
        while (cur != null && cur.action != null) {
            rev.add(cur.action);
            cur = cameFrom.get(cur.state);
        }
        Collections.reverse(rev);
        plan.addAll(rev);
        return plan;
    }

    private static double heuristic(State s, State goal) {
        int dx = Math.abs(s.x - goal.x);
        int dy = Math.abs(s.y - goal.y);
        double md = dx + dy;

        // Minimal turn lower-bound: 0.5 if we need to change axis or both dx & dy > 0
        boolean needHorizontal = dx > 0;
        boolean needVertical = dy > 0;
        boolean dirHorizontal = (s.dir == Direction.E || s.dir == Direction.W);
        boolean dirVertical = (s.dir == Direction.N || s.dir == Direction.S);
        double turnLB = 0.0;
        if ((needHorizontal && needVertical) || (needHorizontal && dirVertical) || (needVertical && dirHorizontal)) {
            turnLB = 0.5;
        }
        return md + turnLB;
    }

    private static List<Node> neighbors(GameStatusModel model, Maze maze, Node node) {
        List<Node> result = new ArrayList<>(3);
        Direction dir = normalize(node.state.dir);

        // turn left
        Direction left = turnLeft(dir);
        result.add(new Node(new State(node.state.x, node.state.y, left), node, node.g + 0.5, 0, Action.TURN_LEFT));

        // turn right
        Direction right = turnRight(dir);
        result.add(new Node(new State(node.state.x, node.state.y, right), node, node.g + 0.5, 0, Action.TURN_RIGHT));

        // step forward
        int nx = node.state.x + dx(dir);
        int ny = node.state.y + dy(dir);
        if (isWalkable(model, maze, nx, ny)) {
            result.add(new Node(new State(nx, ny, dir), node, node.g + 1.0, 0, Action.STEP));
        }
        return result;
    }

    private static boolean isWalkable(GameStatusModel model, Maze maze, int x, int y) {
        if (x < 0 || x >= maze.getWidth() || y < 0 || y >= maze.getHeight()) return false;
        CellType type = maze.getTypeAt(x, y);
        if (type != CellType.PATH) return false;

        if (model != null) {
            var baits = model.getBaits();
            if (baits != null) {
                for (Bait bait : baits.values()) {
                    if (bait == null) continue;
                    if (bait.getBaitType() == BaitType.TRAP
                            && bait.getxPosition() == x && bait.getyPosition() == y) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Direction normalize(Direction d) {
        return (d == null) ? Direction.N : d;
    }

    private static Direction turnLeft(Direction d) {
        Direction dir = normalize(d);
        return switch (dir) {
            case N -> Direction.W;
            case W -> Direction.S;
            case S -> Direction.E;
            case E -> Direction.N;
        };
    }

    private static Direction turnRight(Direction d) {
        Direction dir = normalize(d);
        return switch (dir) {
            case N -> Direction.E;
            case E -> Direction.S;
            case S -> Direction.W;
            case W -> Direction.N;
        };
    }

    private static int dx(Direction d) {
        return switch (d) {
            case E -> 1;
            case W -> -1;
            default -> 0;
        };
    }

    private static int dy(Direction d) {
        return switch (d) {
            case S -> 1;
            case N -> -1;
            default -> 0;
        };
    }

    private static final class Node {
        State state;
        Node parent;
        double g;
        double f;
        Action action;

        Node(State state, Node parent, double g, double f, Action action) {
            this.state = state;
            this.parent = parent;
            this.g = g;
            this.f = f;
            this.action = action;
        }
    }
}

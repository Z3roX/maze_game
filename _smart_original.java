package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;

public final class SmartStrategy implements Strategy {

// ==== Tuning & Feature constants ====
// Scoring
private static final double COST_WEIGHT = 3.0;          // λ: weight of action cost in score (value - λ * steps)
private static final boolean USE_LINEAR_SCORE = true;   // if false, use value / (1 + steps^COST_EXPONENT)
private static final double COST_EXPONENT = 1.1;

// Opponent margins
private static final int OPPONENT_MARGIN_GEM = 2;
private static final int OPPONENT_MARGIN_DEFAULT = 1;
private static final int OPP_SCORING_MARGIN_GEM = 1;     // only penalize if opp at least this much faster
private static final int OPP_SCORING_MARGIN_DEFAULT = 1;

// Candidate limiting
private static final int TOP_K_BAITS = 8;
private static final int TOP_L_OPPONENTS = 4;

// Multi-opponent pressure
private static final int MULTI_NEAR_EXTRA = 2;           // opponents within ourCost + this are considered "near"
private static final double MULTI_NEAR_WEIGHT = 0.75;    // penalty per nearby opponent

// Interception (collision sabotage)
private static final boolean ENABLE_INTERCEPT = true;
private static final boolean ENABLE_INTERCEPT_COFFEE = false; // expand intercept beyond GEM if near & losing
private static final int INTERCEPT_K_STEPS = 8;
private static final int INTERCEPT_MARGIN = 1;
private static final int INTERCEPT_TIMEOUT = 8;

// Teleport handling
private static final boolean ENABLE_TELEPORT_GUARD = true;
private static final int TELEPORT_JUMP_THRESHOLD = 4;    // manhattan delta to consider as "teleport"

// Exploration
private static final boolean ENABLE_EXPLORATION = true;

// Endgame
private static final boolean ENDGAME_ENABLED = false;    // toggle if you want time-based behavior
private static final long ENDGAME_TICKS_THRESHOLD = 600; // heuristic ticks threshold
private static final int ENDGAME_MARGIN_BONUS = 1;       // extra replan margin for GEMs near endgame
// =====================================


    


    private final Pathfinder pathfinder;


// Cache for path plans (start,dir,target) -> plan
private final LruCache<String, Deque<Action>> planCache = new LruCache<>(256);

// Teleport / tick tracking
private GridPos lastPos = null;
private long tickCounter = 0;

    private Deque<Action> currentPlan = new ArrayDeque<>();
    private Target currentTarget;

    public SmartStrategy() {
        this(new AStarPathfinder());
    }

    SmartStrategy(Pathfinder pathfinder) {
        this.pathfinder = Objects.requireNonNull(pathfinder);
    }

    @Override
    
public Action decideNext(GameStatusModel model, Player me) {
        if (model == null || me == null) {
            return Action.IDLE;
        }

// Teleport guard: detect sudden large jumps and force replan
if (ENABLE_TELEPORT_GUARD) {
    if (lastPos != null) {
        int jump = Math.abs(me.getxPosition() - lastPos.x()) + Math.abs(me.getyPosition() - lastPos.y());
        if (jump > TELEPORT_JUMP_THRESHOLD) {
            planNext(model, me);
        }
    }
}


        if (shouldReplan(model)) {
            planNext(model, me);
        }

        // Replan if an opponent is predicted to beat us to our current non-exploration target
        if (currentTarget != null && !currentTarget.isExploration()) {
            int dynMargin = (currentTarget.bait() != null && currentTarget.bait().getBaitType() == BaitType.GEM)
                ? (OPPONENT_MARGIN_GEM + (isEndgame() ? ENDGAME_MARGIN_BONUS : 0))
                : OPPONENT_MARGIN_DEFAULT;
            double oppBest = estimateOpponentSteps(currentTarget.pos(), me, model);
            if (oppBest + dynMargin < currentPlan.size()) { // safety margin = 1
                planNext(model, me);
            }
        }

        if (currentPlan.isEmpty()) {
            return Action.IDLE;
        }
        Action act = currentPlan.pollFirst();
        lastPos = new GridPos(me.getxPosition(), me.getyPosition());
        tickCounter++;
        return act;
    }

@Override
    public void reset() {
        currentPlan.clear();
        currentTarget = null;
    }

    private boolean shouldReplan(GameStatusModel model) {
        if (currentTarget == null || currentPlan.isEmpty()) {
            return true;
        }

        if (currentTarget.isExploration()) {
            return currentPlan.isEmpty();
        }

        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            return true;
        }

        return baitMap.values().stream()
                .filter(Objects::nonNull)
                .noneMatch(b -> sameBait(b, currentTarget.bait()));
    }

    private void planNext(GameStatusModel model, Player me) {

currentPlan.clear();
currentTarget = null;

PlanCandidate normal = selectBestTarget(model, me);
PlanCandidate intercept = null;
if (ENABLE_INTERCEPT) {
    intercept = selectInterceptCandidate(model, me, normal);
}

PlanCandidate chosen = normal;
if (intercept != null && (normal == null || intercept.score() > normal.score())) {
    chosen = intercept;
}


// Exploration fallback if no targets found
if (ENABLE_EXPLORATION && chosen == null) {
    Maze maze = model.getMaze();
    if (maze != null) {
        int w = maze.getWidth();
        int h = maze.getHeight();
        GridPos[] anchors = new GridPos[] {
            new GridPos(1,1),
            new GridPos(w-2,1),
            new GridPos(1,h-2),
            new GridPos(w-2,h-2),
            new GridPos(w/2, h/2)
        };
        GridPos bestPos = null;
        int bestD = Integer.MAX_VALUE;
        for (GridPos gp : anchors) {
            int d = Math.abs(me.getxPosition()-gp.x()) + Math.abs(me.getyPosition()-gp.y());
            if (d < bestD) { bestD = d; bestPos = gp; }
        }
        if (bestPos != null) {
            Target t = Target.of(bestPos, 0);
            Deque<Action> pth = planCached(me, t, model);
            if (pth != null && !pth.isEmpty()) {
                chosen = new PlanCandidate(t, pth, 0);
            }
        }
    }
}

if (chosen != null) {
    currentTarget = chosen.target();
    currentPlan = chosen.plan();
}
}

    
    /**
     * Try to intercept an opponent en route to a high-value GEM by colliding before they arrive.
     */
    private PlanCandidate selectInterceptCandidate(GameStatusModel model, Player me, PlanCandidate normal) {
        ObservableMap<Integer, Player> players = model.getPlayers();
        ObservableMap<Integer, Bait> baits = model.getBaits();
        if (players == null || baits == null || players.isEmpty() || baits.isEmpty()) return null;

        double normalScore = (normal != null ? normal.score() : Double.NEGATIVE_INFINITY);
        PlanCandidate best = null;

        for (Player opp : players.values()) {
            if (opp == null) continue;
            if (me.getID() == opp.getID()) continue;

            Bait targetBait = predictBestBaitFor(opp, baits);
            if (targetBait == null) continue;

            Deque<Action> oppPlan = planCached(opp, Target.of(targetBait, 0), model);
            if (oppPlan == null || oppPlan.isEmpty()) continue;

            Deque<Action> ourToTarget = planCached(me, Target.of(targetBait, 0), model);
            if (ourToTarget != null && !ourToTarget.isEmpty()) {
                if (targetBait.getBaitType() != BaitType.GEM) {
                    if (!ENABLE_INTERCEPT_COFFEE) {
                        continue; // nur GEM
                    }
                // Kaffee erlauben, aber nur wenn nah
                    if (!(targetBait.getBaitType() == BaitType.COFFEE && ourToTarget.size() <= 4)) {
                        continue;
                    }
                }
                int raceMargin = (targetBait.getBaitType() == BaitType.GEM) ? OPPONENT_MARGIN_GEM : OPPONENT_MARGIN_DEFAULT;
                if (ourToTarget.size() + raceMargin < oppPlan.size()) {
                    continue; // we can win the race; no need to intercept
                }
            }

            java.util.List<GridPos> oppEarly = simulatePositionsAlongPath(opp, oppPlan, INTERCEPT_K_STEPS);
            int stepIndex = 0;
            for (GridPos pos : oppEarly) {
                stepIndex++;
                Deque<Action> ourToIntercept = planCached(me, Target.of(pos, 0), model);
                if (ourToIntercept == null || ourToIntercept.isEmpty()) continue;
                int etaMe = ourToIntercept.size();
                if (etaMe > INTERCEPT_TIMEOUT) continue;
                if (etaMe + INTERCEPT_MARGIN >= stepIndex) continue;

                double valueScore = baitValue(targetBait.getBaitType());
                double interceptScore = valueScore - etaMe - 0.25 * stepIndex;

                if (best == null || interceptScore > best.score()) {
                    best = new PlanCandidate(Target.of(pos, interceptScore), ourToIntercept, interceptScore);
                }
            }
        }

        if (best != null && (normal == null || best.score() > normalScore)) {
            return best;
        }
        return null;
    }

    private Bait predictBestBaitFor(Player p, ObservableMap<Integer, Bait> baits) {
        Bait best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Bait b : baits.values()) {
            if (b == null) continue;
            if (!b.isVisible()) continue;
            if (b.getBaitType() != BaitType.GEM) continue;
            int d = Math.abs(p.getxPosition() - b.getxPosition()) + Math.abs(p.getyPosition() - b.getyPosition());
            if (d < bestDist) {
                bestDist = d;
                best = b;
            }
        }
        return best;
    }

    private java.util.List<GridPos> simulatePositionsAlongPath(Player start, Deque<Action> plan, int maxSteps) {
        java.util.List<GridPos> positions = new java.util.ArrayList<>();
        Direction dir = start.getDirection();
        if (dir == null) dir = Direction.NORTH;
        int x = start.getxPosition();
        int y = start.getyPosition();
        int steps = 0;
        for (Action a : plan) {
            switch (a) {
                case TURN_LEFT -> dir = turnLeft(dir);
                case TURN_RIGHT -> dir = turnRight(dir);
                case STEP -> {
                    x += dx(dir);
                    y += dy(dir);
                    steps++;
                    positions.add(new GridPos(x, y));
                    if (steps >= maxSteps) return positions;
                }
                default -> {}
            }
        }
        return positions;
    }

    private Direction turnLeft(Direction d) {
        return switch (d) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
        };
    }

    private Direction turnRight(Direction d) {
        return switch (d) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
        };
    }

    private int dx(Direction d) {
        return switch (d) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private int dy(Direction d) {
        return switch (d) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }


private Deque<Action> planCached(Player p, Target t, GameStatusModel model) {
    if (p == null || t == null) return new ArrayDeque<>();
    // key: px,py,dir; tx,ty
    String dir = String.valueOf(p.getDirection());
    String key = p.getxPosition()+","+p.getyPosition()+","+dir+"->"+t.pos().x()+","+t.pos().y();
    Deque<Action> cached = planCache.get(key);
    if (cached != null) {
        return new ArrayDeque<>(cached); // defensive copy (caller will consume)
    }
    Deque<Action> plan = pathfinder.plan(p, t, model);
    if (plan == null) plan = new ArrayDeque<>();
    planCache.put(key, new ArrayDeque<>(plan)); // store copy
    return plan;
}
private PlanCandidate selectBestTarget(GameStatusModel model, Player me) {

ObservableMap<Integer, Bait> baitMap = model.getBaits();
if (baitMap == null || baitMap.isEmpty()) {
    return null;
}

// Preselect closest TOP_K_BAITS by Manhattan
java.util.List<Bait> pre = new java.util.ArrayList<>();
for (Bait b : baitMap.values()) {
    if (b == null || !b.isVisible() || b.getBaitType() == BaitType.TRAP) continue;
    pre.add(b);
}
pre.sort((b1, b2) -> {
    int d1 = Math.abs(me.getxPosition() - b1.getxPosition()) + Math.abs(me.getyPosition() - b1.getyPosition());
    int d2 = Math.abs(me.getxPosition() - b2.getxPosition()) + Math.abs(me.getyPosition() - b2.getyPosition());
    return Integer.compare(d1, d2);
});
if (pre.size() > TOP_K_BAITS) pre = pre.subList(0, TOP_K_BAITS);

PlanCandidate best = null;
for (Bait b : pre) {
    PlanCandidate cand = evaluateCandidate(b, me, model);
    if (cand != null && (best == null || cand.score() > best.score())) {
        best = cand;
    }
}
return best;
}

    private PlanCandidate evaluateCandidate(Bait bait, Player me, GameStatusModel model) {
        Target baseTarget = Target.of(bait, 0);
        Deque<Action> plan = planCached(me, baseTarget, model);
        if (plan == null || plan.isEmpty()) {
            return null;
        }

        
double ourCost = plan.size();
double opponentCost = estimateOpponentSteps(baseTarget.pos(), me, model);
double valueScore = baitValue(bait.getBaitType());

double score;
if (USE_LINEAR_SCORE) {
    score = valueScore - COST_WEIGHT * ourCost;
} else {
    score = valueScore / (1.0 + Math.pow(ourCost, COST_EXPONENT));
}
// Multi-opponent pressure
score -= multiOpponentPenalty(baseTarget.pos(), me, model, (int) ourCost);

// Additional penalty if opponent is predicted to arrive earlier (with dynamic margin)
int oppScoreMargin = (bait.getBaitType() == BaitType.GEM) ? OPP_SCORING_MARGIN_GEM : OPP_SCORING_MARGIN_DEFAULT;
if (opponentCost < Double.POSITIVE_INFINITY && opponentCost + oppScoreMargin < ourCost) {
    score -= (ourCost - opponentCost);
}

Target scoredTarget = Target.of(bait, score);
        return new PlanCandidate(scoredTarget, plan, score);
    }

    
private double estimateOpponentSteps(GridPos goal, Player me, GameStatusModel model) {

ObservableMap<Integer, Player> players = model.getPlayers();
if (players == null || players.isEmpty()) {
    return Double.POSITIVE_INFINITY;
}

// Preselect TOP_L_OPPONENTS by Manhattan to goal
java.util.List<Player> opps = new java.util.ArrayList<>();
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
if (opps.size() > TOP_L_OPPONENTS) opps = opps.subList(0, TOP_L_OPPONENTS);

double best = Double.POSITIVE_INFINITY;
for (Player player : opps) {
    Deque<Action> oppPlan = planCached(player, Target.of(goal, 0), model);
    if (oppPlan != null && !oppPlan.isEmpty()) {
        best = Math.min(best, oppPlan.size());
    }
}
return best;
}



private boolean isEndgame() {
    if (!ENDGAME_ENABLED) return false;
    return tickCounter >= ENDGAME_TICKS_THRESHOLD;
}
private double multiOpponentPenalty(GridPos goal, Player me, GameStatusModel model, int ourCost) {
    ObservableMap<Integer, Player> players = model.getPlayers();
    if (players == null || players.isEmpty()) {
        return 0.0;
    }
    int close = 0;
    for (Player p : players.values()) {
        if (p == null) continue;
        if (me.getID() == p.getID()) continue;
        int d = Math.abs(p.getxPosition() - goal.x()) + Math.abs(p.getyPosition() - goal.y());
        if (d <= ourCost + MULTI_NEAR_EXTRA) {
            close++;
        }
    }
    return MULTI_NEAR_WEIGHT * close; // tweak weight as desired
}

private static boolean sameBait(Bait a, Bait b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getxPosition() == b.getxPosition()
                && a.getyPosition() == b.getyPosition()
                && a.getBaitType() == b.getBaitType();
    }

    private static double baitValue(BaitType type) {
        return switch (type) {
            case GEM -> 314.0;
            case COFFEE -> 42.0;
            case FOOD -> 13.0;
            case TRAP -> -128.0;
        };
    }

    private record PlanCandidate(Target target, Deque<Action> plan, double score) {
    }


// Simple LRU cache based on LinkedHashMap
private static final class LruCache<K,V> extends LinkedHashMap<K,V> {
    private final int maxEntries;
    LruCache(int maxEntries) {
        super(64, 0.75f, true);
        this.maxEntries = maxEntries;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxEntries;
    }
}

}
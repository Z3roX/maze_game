package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Deque;
import java.util.Objects;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Strategy;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;
import javafx.collections.ObservableMap;

public final class SmartStrategy implements Strategy {

    // tuning snapshot
    private final double COST_WEIGHT;

    private final int OPPONENT_MARGIN_GEM;
    private final int OPPONENT_MARGIN_DEFAULT;
    private final int OPP_SCORING_MARGIN_GEM;
    private final int OPP_SCORING_MARGIN_DEFAULT;

    private final int TOP_K_BAITS;
    private final int TOP_L_OPPONENTS;

    private final int MULTI_NEAR_EXTRA;
    private final double MULTI_NEAR_WEIGHT;

    private final boolean ENABLE_INTERCEPT;
    private final boolean ENABLE_TELEPORT_GUARD;
    private final int TELEPORT_JUMP_THRESHOLD;

    private final boolean ENABLE_EXPLORATION;

    // helpers
    private final SmartTuning tuning;
    private final AStarPathfinder pathfinder;
    private final ExplorationPlanner exploration;
    private final InterceptPlanner intercept;
    private final HysteresisController hysteresis;

    private final LruCache<PlanKey, AStarPathfinder.Result> planCache = new LruCache<>(256);

    // dynamic state
    private GridPos lastPos;
    private long tickCounter;

    private Deque<Action> currentPlan = new ArrayDeque<>();
    private Target currentTarget;

    private Action lastAction;
    private GridPos lastObservedPos;
    private Direction lastObservedDir;
    private int stuckTicks;
    private int lrOscCount;

    private static final boolean DEBUG = true;

    static final record Candidate(Target target, List<Action> actions, double cost, double score) {}

    private static final record PlanKey(int sx, int sy, int dirOrdinal, int gx, int gy) {}

    private static boolean isBetter(Candidate candidate, Candidate incumbent) {
        if (candidate == null) {
            return false;
        }
        if (incumbent == null) {
            return true;
        }
        if (candidate.score() > incumbent.score()) {
            return true;
        }
        if (candidate.score() < incumbent.score()) {
            return false;
        }
        int candidatePriority = targetPriority(candidate.target());
        int incumbentPriority = targetPriority(incumbent.target());
        if (candidatePriority > incumbentPriority) {
            return true;
        }
        if (candidatePriority < incumbentPriority) {
            return false;
        }
        if (candidate.cost() < incumbent.cost()) {
            return true;
        }
        if (candidate.cost() > incumbent.cost()) {
            return false;
        }
        Target candidateTarget = candidate.target();
        Target incumbentTarget = incumbent.target();
        if (candidateTarget == null || incumbentTarget == null) {
            return false;
        }
        GridPos candidatePos = candidateTarget.pos();
        GridPos incumbentPos = incumbentTarget.pos();
        if (candidatePos == null || incumbentPos == null) {
            return false;
        }
        if (candidatePos.x() < incumbentPos.x()) {
            return true;
        }
        if (candidatePos.x() > incumbentPos.x()) {
            return false;
        }
        if (candidatePos.y() < incumbentPos.y()) {
            return true;
        }
        if (candidatePos.y() > incumbentPos.y()) {
            return false;
        }
        return false;
    }

    private static int targetPriority(Target target) {
        if (target == null || target.bait() == null) {
            return 0;
        }
        BaitType type = target.bait().getBaitType();
        if (type == null) {
            return 0;
        }
        return switch (type) {
        case GEM -> 3;
        case COFFEE -> 2;
        case FOOD -> 1;
        case TRAP -> 0;
        };
    }

    public SmartStrategy() {
        this(new AStarPathfinder(), SmartTuning.defaults());
    }

    public SmartStrategy(SmartTuning tuning) {
        this(new AStarPathfinder(), tuning);
    }

    SmartStrategy(AStarPathfinder pathfinder, SmartTuning tuning) {
        this.pathfinder = Objects.requireNonNull(pathfinder);
        this.tuning = (tuning != null) ? tuning : SmartTuning.defaults();
        this.exploration = new ExplorationPlanner(this);
        this.intercept = new InterceptPlanner(this, this.tuning);
        this.hysteresis = new HysteresisController(this.tuning.replanCooldownTicks, this.tuning.scoreDeltaMin,
                this.tuning.scoreRatioMin);

        this.COST_WEIGHT = this.tuning.costWeight;
        this.OPPONENT_MARGIN_GEM = this.tuning.opponentMarginGem;
        this.OPPONENT_MARGIN_DEFAULT = this.tuning.opponentMarginDefault;
        this.OPP_SCORING_MARGIN_GEM = this.tuning.oppScoringMarginGem;
        this.OPP_SCORING_MARGIN_DEFAULT = this.tuning.oppScoringMarginDefault;
        this.TOP_K_BAITS = this.tuning.topKBaits;
        this.TOP_L_OPPONENTS = this.tuning.topLOpponents;
        this.MULTI_NEAR_EXTRA = this.tuning.multiNearExtra;
        this.MULTI_NEAR_WEIGHT = this.tuning.multiNearWeight;
        this.ENABLE_INTERCEPT = this.tuning.enableIntercept;
        this.ENABLE_TELEPORT_GUARD = this.tuning.enableTeleportGuard;
        this.TELEPORT_JUMP_THRESHOLD = this.tuning.teleportJumpThreshold;
        this.ENABLE_EXPLORATION = this.tuning.enableExploration;
    }

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        if (model == null || me == null) {
            return Action.IDLE;
        }

        GridPos nowPos = new GridPos(me.getxPosition(), me.getyPosition());
        Direction nowDir = me.getDirection();

        boolean lastFailed = false;
        if (lastAction != null) {
            switch (lastAction) {
            case STEP -> lastFailed = lastObservedPos != null && nowPos.equals(lastObservedPos);
            case TURN_LEFT, TURN_RIGHT -> lastFailed = lastObservedDir != null && nowDir == lastObservedDir;
            default -> lastFailed = false;
            }
        }
        if (lastFailed) {
            stuckTicks++;
            if (DEBUG) {
                System.out.println("[SmartStrategy] STUCK after " + lastAction + " at " + nowPos.x() + ","
                        + nowPos.y() + " dir=" + nowDir + " (cnt=" + stuckTicks + ")");
            }
            invalidateCachedPlans();
            planNext(model, me);
        } else {
            stuckTicks = 0;
        }
        lastObservedPos = nowPos;
        lastObservedDir = nowDir;

        if (ENABLE_TELEPORT_GUARD && lastPos != null) {
            int jump = Math.abs(me.getxPosition() - lastPos.x()) + Math.abs(me.getyPosition() - lastPos.y());
            if (jump > TELEPORT_JUMP_THRESHOLD) {
                if (DEBUG) {
                    System.out.println("[SmartStrategy] REPLAN (teleport jump=" + jump + ")");
                }
                invalidateCachedPlans();
                planNext(model, me);
            }
        }

        if (currentPlan.isEmpty() || currentTarget == null) {
            planNext(model, me);
        }

        if (currentTarget != null && currentTarget.bait() != null) {
            int dynMargin = (currentTarget.bait().getBaitType() == BaitType.GEM) ? OPPONENT_MARGIN_GEM
                    : OPPONENT_MARGIN_DEFAULT;
            double oppBest = estimateOpponentSteps(currentTarget.pos(), me, model);
            if (oppBest + dynMargin < planCost(currentPlan)) {
                if (DEBUG) {
                    System.out.println("[SmartStrategy] REPLAN (opponent ETA better: " + oppBest + " + " + dynMargin
                            + ")");
                }
                invalidateCachedPlans();
                planNext(model, me);
            }
        }

        if (currentPlan.isEmpty()) {
            lastPos = nowPos;
            tickCounter++;
            lastAction = Action.IDLE;
            return Action.IDLE;
        }

        Action next = currentPlan.pollFirst();
        if (DEBUG) {
            String tgt = (currentTarget != null)
                    ? currentTarget.pos().x() + "," + currentTarget.pos().y()
                    : "-";
            System.out.println("[SmartStrategy] tick=" + tickCounter + " pos=" + nowPos.x() + "," + nowPos.y()
                    + " dir=" + nowDir + " action=" + next + " target=" + tgt + " planLeft=" + currentPlan.size());
        }

        if ((lastAction == Action.TURN_LEFT && next == Action.TURN_RIGHT)
                || (lastAction == Action.TURN_RIGHT && next == Action.TURN_LEFT)) {
            lrOscCount++;
            if (lrOscCount >= 3) {
                if (DEBUG) {
                    System.out.println("[SmartStrategy] LR-oscillation -> REPLAN");
                }
                invalidateCachedPlans();
                planNext(model, me);
                if (!currentPlan.isEmpty()) {
                    next = currentPlan.pollFirst();
                }
                lrOscCount = 0;
            }
        } else {
            lrOscCount = 0;
        }

        lastAction = next;
        if (next == Action.STEP) {
            lastPos = nowPos;
        }
        tickCounter++;
        return next;
    }

    private void planNext(GameStatusModel model, Player me) {
        currentPlan.clear();
        currentTarget = null;
        stuckTicks = 0;
        hysteresis.reset();

        Candidate normal = selectBestTarget(model, me);
        Candidate interceptCand = null;
        if (ENABLE_INTERCEPT) {
            interceptCand = this.intercept.selectInterceptCandidate(model, me, normal);
            if (interceptCand != null && interceptCand.score() < 0.0) {
                if (DEBUG) {
                    System.out.println("[SmartStrategy] skip intercept (score=" + interceptCand.score() + ")");
                }
                interceptCand = null;
            }
        }

        Candidate chosen = normal;
        if (isBetter(interceptCand, chosen)) {
            chosen = interceptCand;
        }

        chosen = hysteresis.apply(currentTarget, chosen, tickCounter, t -> recomputeFor(t, model, me));

        if (ENABLE_EXPLORATION && chosen == null) {
            Candidate exp = exploration.explorationFallback(model, me);
            if (exp != null) {
                chosen = exp;
            }
        }

        if (DEBUG) {
            System.out.println("[SmartStrategy] planNext: normal=" + (normal != null ? normal.score() : null)
                    + " intercept=" + (interceptCand != null ? interceptCand.score() : null));
        }

        if (chosen != null) {
            currentTarget = chosen.target();
            currentPlan = new ArrayDeque<>(chosen.actions());
            hysteresis.onChosen(chosen);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (Action ac : currentPlan) {
                    if (i++ >= 8) {
                        break;
                    }
                    sb.append(ac).append(' ');
                }
                System.out.println("[SmartStrategy] chosen target=" + currentTarget.pos().x() + ","
                        + currentTarget.pos().y() + " score=" + chosen.score() + " planLen=" + currentPlan.size()
                        + " head=" + sb);
            }
        }
    }

    private void invalidateCachedPlans() {
        planCache.clear();
    }

    private Candidate selectBestTarget(GameStatusModel model, Player me) {
        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            return null;
        }

        java.util.List<Bait> pre = new java.util.ArrayList<>();
        for (Bait bait : baitMap.values()) {
            if (bait == null || !bait.isVisible() || bait.getBaitType() == BaitType.TRAP) {
                continue;
            }
            pre.add(bait);
        }
        pre.sort((b1, b2) -> {
            int d1 = Math.abs(me.getxPosition() - b1.getxPosition()) + Math.abs(me.getyPosition() - b1.getyPosition());
            int d2 = Math.abs(me.getxPosition() - b2.getxPosition()) + Math.abs(me.getyPosition() - b2.getyPosition());
            return Integer.compare(d1, d2);
        });
        if (pre.size() > TOP_K_BAITS) {
            pre = pre.subList(0, TOP_K_BAITS);
        }

        Candidate best = null;
        for (Bait bait : pre) {
            Candidate cand = evaluateCandidate(bait, me, model);
            if (cand == null) {
                continue;
            }
            if (isBetter(cand, best)) {
                best = cand;
            }
        }
        return best;
    }

    private Candidate evaluateCandidate(Bait bait, Player me, GameStatusModel model) {
        if (bait == null) {
            return null;
        }
        Target baseTarget = Target.of(bait, 0);
        AStarPathfinder.Result route = planFor(me, baseTarget.pos(), model);
        if (!route.success() || route.actions().isEmpty()) {
            return null;
        }

        double ourCost = route.cost();
        double opponentCost = estimateOpponentSteps(baseTarget.pos(), me, model);
        double valueScore = baitValue(bait.getBaitType());

        double score = valueScore - COST_WEIGHT * ourCost;
        score -= multiOpponentPenalty(baseTarget.pos(), me, model, ourCost);

        int oppScoreMargin = (bait.getBaitType() == BaitType.GEM) ? OPP_SCORING_MARGIN_GEM : OPP_SCORING_MARGIN_DEFAULT;
        if (opponentCost < Double.POSITIVE_INFINITY && opponentCost + oppScoreMargin < ourCost) {
            score -= (ourCost - opponentCost);
        }

        Target scoredTarget = Target.of(bait, score);
        return new Candidate(scoredTarget, route.actions(), ourCost, score);
    }

    private double baitValue(BaitType type) {
        if (type == null) {
            return 0.0;
        }
        return switch (type) {
        case GEM -> 314.0;
        case COFFEE -> 42.0;
        case FOOD -> 13.0;
        case TRAP -> -128.0;
        };
    }

    private Candidate recomputeFor(Target target, GameStatusModel model, Player me) {
        if (target == null) {
            return null;
        }
        AStarPathfinder.Result route = planFor(me, target.pos(), model);
        if (!route.success() || route.actions().isEmpty()) {
            return null;
        }

        double ourCost = route.cost();
        double valueScore = 0.0;
        Bait bait = target.bait();
        if (bait != null) {
            valueScore = baitValue(bait.getBaitType());
        }

        double score = valueScore - COST_WEIGHT * ourCost;
        score -= multiOpponentPenalty(target.pos(), me, model, ourCost);

        double opponentCost = estimateOpponentSteps(target.pos(), me, model);
        int oppScoreMargin = (bait != null && bait.getBaitType() == BaitType.GEM)
                ? OPP_SCORING_MARGIN_GEM
                : OPP_SCORING_MARGIN_DEFAULT;
        if (opponentCost < Double.POSITIVE_INFINITY && opponentCost + oppScoreMargin < ourCost) {
            score -= (ourCost - opponentCost);
        }

        Target scoredTarget = (bait != null) ? Target.of(bait, score) : Target.of(target.pos(), score);
        return new Candidate(scoredTarget, route.actions(), ourCost, score);
    }

    private double estimateOpponentSteps(GridPos goal, Player me, GameStatusModel model) {
        return OpponentHeuristics.estimateOpponentCost(goal, me, model, TOP_L_OPPONENTS, opp -> planFor(opp, goal, model));
    }


    private static double planCost(Iterable<Action> actions) {
        double cost = 0.0;
        if (actions != null) {
            for (Action action : actions) {
                cost += AStarPathfinder.costOf(action);
            }
        }
        return cost;
    }

    private double multiOpponentPenalty(GridPos goal, Player me, GameStatusModel model, double ourCost) {
        return OpponentHeuristics.multiOpponentPenalty(goal, me, model, ourCost, MULTI_NEAR_EXTRA, MULTI_NEAR_WEIGHT);
    }

    AStarPathfinder.Result planFor(Player actor, GridPos goal, GameStatusModel model) {
        if (actor == null || goal == null || model == null) {
            return AStarPathfinder.Result.EMPTY;
        }
        Maze maze = model.getMaze();
        if (maze == null) {
            return AStarPathfinder.Result.EMPTY;
        }
        Direction facing = actor.getDirection();
        PlanKey key = new PlanKey(actor.getxPosition(), actor.getyPosition(), dirOrdinal(facing), goal.x(), goal.y());
        AStarPathfinder.Result cached = planCache.get(key);
        if (cached != null) {
            return cached;
        }
        AStarPathfinder.Pose pose = new AStarPathfinder.Pose(actor.getxPosition(), actor.getyPosition(), facing);
        AStarPathfinder.Result result = pathfinder.plan(maze, pose, goal);
        planCache.put(key, result);
        return result;
    }

    private static int dirOrdinal(Direction dir) {
        return dir == null ? -1 : dir.ordinal();
    }

}


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

public final class SmartStrategy implements Strategy {

    // === Tuning als Instanzwerte (kommen aus SmartTuning) ===
    private final double COST_WEIGHT;
    private final boolean USE_LINEAR_SCORE;
    private final double COST_EXPONENT;

    private final int OPPONENT_MARGIN_GEM;
    private final int OPPONENT_MARGIN_DEFAULT;
    private final int OPP_SCORING_MARGIN_GEM;
    private final int OPP_SCORING_MARGIN_DEFAULT;

    private final int TOP_K_BAITS;
    private final int TOP_L_OPPONENTS;

    private final int MULTI_NEAR_EXTRA;
    private final double MULTI_NEAR_WEIGHT;

    private final boolean ENABLE_INTERCEPT;
    private final boolean ENABLE_INTERCEPT_COFFEE;
    private final int INTERCEPT_K_STEPS;
    private final int INTERCEPT_MARGIN;
    private final int INTERCEPT_TIMEOUT;

    private final boolean ENABLE_TELEPORT_GUARD;
    private final int TELEPORT_JUMP_THRESHOLD;

    private final boolean ENABLE_EXPLORATION;

    private final boolean ENDGAME_ENABLED;
    private final long ENDGAME_TICKS_THRESHOLD;
    private final int ENDGAME_MARGIN_BONUS;

    private final int REPLAN_COOLDOWN_TICKS;
    private final double SCORE_DELTA_MIN;
    private final double SCORE_RATIO_MIN;

    // === Engine / Helfer ===
    private final Pathfinder pathfinder;
    private final SmartTuning tuning;
    private final PlanProvider planProvider;
    private final ExplorationPlanner exploration;
    private final InterceptPlanner intercept;

    private final LruCache<String, Deque<Action>> planCache = new LruCache<>(256);

    private GridPos lastPos = null;
    private long tickCounter = 0;

    private Deque<Action> currentPlan = new ArrayDeque<>();
    private Target currentTarget;
    private final HysteresisController hysteresis;

    // --- Sichtbares Konsolen-Logging ein/aus (unabhängig von SmartTuning) ---
    private static final boolean DEBUG = true;

    // --- Anti-Stuck ---
    private Action lastAction = null;
    private GridPos lastObservedPos = null;
    private Direction lastObservedDir = null;
    private int stuckTicks = 0;
    private int lrOscCount = 0;

    public SmartStrategy() {
        this(new AStarPathfinder(), SmartTuning.defaults());
    }

    public SmartStrategy(SmartTuning tuning) {
        this(new AStarPathfinder(), tuning);
    }

    SmartStrategy(Pathfinder pathfinder, SmartTuning tuning) {
        this.pathfinder = Objects.requireNonNull(pathfinder);
        this.tuning = (tuning != null) ? tuning : SmartTuning.defaults();
        this.planProvider = (actor, target, m) -> planCached(actor, target, m);
        this.exploration = new ExplorationPlanner(this.planProvider);
        this.intercept = new InterceptPlanner(this.tuning, this.planProvider);
        this.hysteresis = new HysteresisController(this.tuning.replanCooldownTicks, this.tuning.scoreDeltaMin, this.tuning.scoreRatioMin);

        // Tuning übernehmen
        this.COST_WEIGHT = this.tuning.costWeight;
        this.USE_LINEAR_SCORE = this.tuning.useLinearScore;
        this.COST_EXPONENT = this.tuning.costExponent;
        this.OPPONENT_MARGIN_GEM = this.tuning.opponentMarginGem;
        this.OPPONENT_MARGIN_DEFAULT = this.tuning.opponentMarginDefault;
        this.OPP_SCORING_MARGIN_GEM = this.tuning.oppScoringMarginGem;
        this.OPP_SCORING_MARGIN_DEFAULT = this.tuning.oppScoringMarginDefault;
        this.TOP_K_BAITS = this.tuning.topKBaits;
        this.TOP_L_OPPONENTS = this.tuning.topLOpponents;
        this.MULTI_NEAR_EXTRA = this.tuning.multiNearExtra;
        this.MULTI_NEAR_WEIGHT = this.tuning.multiNearWeight;
        this.ENABLE_INTERCEPT = this.tuning.enableIntercept;
        this.ENABLE_INTERCEPT_COFFEE = this.tuning.enableInterceptCoffee;
        this.INTERCEPT_K_STEPS = this.tuning.interceptKSteps;
        this.INTERCEPT_MARGIN = this.tuning.interceptMargin;
        this.INTERCEPT_TIMEOUT = this.tuning.interceptTimeout;
        this.ENABLE_TELEPORT_GUARD = this.tuning.enableTeleportGuard;
        this.TELEPORT_JUMP_THRESHOLD = this.tuning.teleportJumpThreshold;
        this.ENABLE_EXPLORATION = this.tuning.enableExploration;
        this.ENDGAME_ENABLED = this.tuning.endgameEnabled;
        this.ENDGAME_TICKS_THRESHOLD = this.tuning.endgameTicksThreshold;
        this.ENDGAME_MARGIN_BONUS = this.tuning.endgameMarginBonus;
        this.REPLAN_COOLDOWN_TICKS = this.tuning.replanCooldownTicks;
        this.SCORE_DELTA_MIN = this.tuning.scoreDeltaMin;
        this.SCORE_RATIO_MIN = this.tuning.scoreRatioMin;
    }

    @Override
    public Action decideNext(GameStatusModel model, Player me) {
        if (model == null || me == null) return Action.IDLE;

        // --- Beobachten & Stuck erkennen (letzte Aktion ohne Effekt?) ---
        GridPos nowPos = new GridPos(me.getxPosition(), me.getyPosition());
        Direction nowDir = me.getDirection();

        boolean lastFailed = false;
        if (lastAction != null) {
            switch (lastAction) {
                case STEP -> lastFailed = (lastObservedPos != null &&
                        nowPos.x() == lastObservedPos.x() && nowPos.y() == lastObservedPos.y());
                case TURN_LEFT, TURN_RIGHT -> lastFailed = (lastObservedDir != null && nowDir == lastObservedDir);
                default -> lastFailed = false;
            }
        }
        boolean requestReplan = false;
        if (lastFailed) {
            stuckTicks++;
            if (DEBUG) System.out.println("[SmartStrategy] STUCK after " + lastAction +
                    " at " + nowPos.x() + "," + nowPos.y() + " dir=" + nowDir + " (cnt=" + stuckTicks + ")");
            if (lastAction == Action.STEP) {
                invalidateCachedPlan(me, currentTarget);
                requestReplan = true;
                if (DEBUG) System.out.println("[SmartStrategy] dropping cached plan for target after failed STEP");
            } else if (stuckTicks >= 2) {
                requestReplan = true;
            }
        } else {
            stuckTicks = 0;
        }
        lastObservedPos = nowPos;
        lastObservedDir = nowDir;

        if (requestReplan) {
            if (DEBUG) System.out.println("[SmartStrategy] REPLAN (stuck " + stuckTicks + ")");
            planNext(model, me);
        }

        // --- Teleport-Guard / großer Sprung? ---
        if (ENABLE_TELEPORT_GUARD) {
            if (lastPos != null) {
                int jump = Math.abs(me.getxPosition() - lastPos.x()) + Math.abs(me.getyPosition() - lastPos.y());
                if (jump > TELEPORT_JUMP_THRESHOLD) {
                    if (DEBUG) System.out.println("[SmartStrategy] REPLAN (teleport jump=" + jump + ")");
                    planCache.clear();
                    planNext(model, me);
                }
            }
        }

        // --- reguläre Replan-Triggers ---
        if (currentPlan.isEmpty() || currentTarget == null) {
            planNext(model, me);
        }

        // Gegner schlägt uns (mit Margin) auf unser aktuelles Ziel?
        if (currentTarget != null && currentTarget.bait() != null) {
            int dynMargin = (currentTarget.bait().getBaitType() == BaitType.GEM) ? OPPONENT_MARGIN_GEM : OPPONENT_MARGIN_DEFAULT;
            double oppBest = estimateOpponentSteps(currentTarget.pos(), me, model);
            if (oppBest + dynMargin < CostUtil.planCost(currentPlan)) {
                if (DEBUG) System.out.println("[SmartStrategy] REPLAN (opponent ETA better: " + oppBest + " + " + dynMargin + ")");
                planNext(model, me);
            }
        }

        // Plan verbrauchen
        if (currentPlan.isEmpty()) {
            lastPos = new GridPos(me.getxPosition(), me.getyPosition());
            tickCounter++;
            return Action.IDLE;
        }

        Action a = currentPlan.pollFirst();
        if (DEBUG) {
            String tgt = (currentTarget != null) ? (currentTarget.pos().x() + "," + currentTarget.pos().y()) : "-";
            System.out.println("[SmartStrategy] tick=" + tickCounter + " pos=" + nowPos.x() + "," + nowPos.y()
                    + " dir=" + nowDir + " action=" + a + " target=" + tgt + " planLeft=" + currentPlan.size());
        }

        // L <-> R Oszillation erkennen & brechen
        if ((lastAction == Action.TURN_LEFT && a == Action.TURN_RIGHT) ||
            (lastAction == Action.TURN_RIGHT && a == Action.TURN_LEFT)) {
            lrOscCount++;
            if (lrOscCount >= 3) {
                if (DEBUG) System.out.println("[SmartStrategy] LR-oscillation -> REPLAN");
                invalidateCachedPlan(me, currentTarget);
                planNext(model, me);
                if (!currentPlan.isEmpty()) {
                    a = currentPlan.pollFirst();
                }
                lrOscCount = 0;
            }
        } else {
            lrOscCount = 0;
        }

        lastAction = a;

        if (a == Action.STEP) {
            lastPos = new GridPos(me.getxPosition(), me.getyPosition());
        }
        tickCounter++;
        return a;
    }

    private void planNext(GameStatusModel model, Player me) {
        currentPlan.clear();
        currentTarget = null;
        hysteresis.reset();

        PlanCandidate normal = selectBestTarget(model, me);
        PlanCandidate interceptCand = null;
        if (ENABLE_INTERCEPT) {
            interceptCand = this.intercept.selectInterceptCandidate(model, me, normal);
        }

        PlanCandidate chosen = normal;
        if (interceptCand != null && (normal == null || interceptCand.score() > normal.score())) {
            chosen = interceptCand;
        }

        // Hysterese anwenden (gegen Flip-Flop)
        chosen = hysteresis.apply(currentTarget, chosen, tickCounter, t -> recomputeFor(t, model, me));

        // Exploration Fallback
        if (ENABLE_EXPLORATION && chosen == null) {
            PlanCandidate exp = exploration.explorationFallback(model, me);
            if (exp != null) chosen = exp;
        }

        if (DEBUG) {
            System.out.println("[SmartStrategy] planNext: normal=" + (normal != null ? normal.score() : null)
                    + " intercept=" + (interceptCand != null ? interceptCand.score() : null));
        }

        if (chosen != null) {
            currentTarget = chosen.target();
            currentPlan = chosen.plan();
            hysteresis.onChosen(chosen);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (Action ac : currentPlan) { if (i++ >= 8) break; sb.append(ac).append(' '); }
                System.out.println("[SmartStrategy] chosen target=" + currentTarget.pos().x() + "," + currentTarget.pos().y()
                        + " score=" + chosen.score() + " planLen=" + currentPlan.size() + " head=" + sb);
            }
        }
    }

    private PlanCandidate selectBestTarget(GameStatusModel model, Player me) {
        ObservableMap<Integer, Bait> baitMap = model.getBaits();
        if (baitMap == null || baitMap.isEmpty()) {
            return null;
        }

        // Preselect TOP_K_BAITS (nächstgelegene) nach Manhattan
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
            if (cand == null) continue;
            if (best == null || cand.score() > best.score()
                    || (Math.abs(cand.score() - best.score()) < 1e-9 && TieBreaker.compare(cand, best) < 0)) {
                best = cand;
            }
        }
        return best;
    }

    private PlanCandidate evaluateCandidate(Bait bait, Player me, GameStatusModel model) {
        Target baseTarget = Target.of(bait, 0);
        Deque<Action> plan = planCached(me, baseTarget, model);
        if (plan == null || plan.isEmpty()) return null;

        double ourCost = CostUtil.planCost(plan);
        double opponentCost = estimateOpponentSteps(baseTarget.pos(), me, model);
        double valueScore = baitValue(bait.getBaitType());

        double score;
        if (USE_LINEAR_SCORE) {
            score = valueScore - COST_WEIGHT * ourCost;
        } else {
            score = valueScore / (1.0 + Math.pow(ourCost, COST_EXPONENT));
        }
        // Crowd / Multi-Opponent Druck
        score -= multiOpponentPenalty(baseTarget.pos(), me, model, ourCost);

        // Wenn Gegner uns (mit Margin) schlägt, zusätzlich bestrafen
        int oppScoreMargin = (bait.getBaitType() == BaitType.GEM) ? OPP_SCORING_MARGIN_GEM : OPP_SCORING_MARGIN_DEFAULT;
        if (opponentCost < Double.POSITIVE_INFINITY && opponentCost + oppScoreMargin < ourCost) {
            score -= (ourCost - opponentCost);
        }

        Target scoredTarget = Target.of(bait, score);
        return new PlanCandidate(scoredTarget, plan, score);
    }

    private double baitValue(BaitType type) {
        if (type == null) return 0.0;
        return switch (type) {
            case GEM -> 314.0;
            case COFFEE -> 42.0;
            case FOOD -> 13.0;
            default -> 0.0;
        };
    }

    private PlanCandidate recomputeFor(Target t, GameStatusModel model, Player me) {
        if (t == null) return null;
        Deque<Action> plan = planCached(me, t, model);
        if (plan == null || plan.isEmpty()) return null;

        double ourCost = CostUtil.planCost(plan);
        double valueScore = 0.0;
        Bait bait = t.bait();
        if (bait != null) valueScore = baitValue(bait.getBaitType());

        double score;
        if (USE_LINEAR_SCORE) {
            score = valueScore - COST_WEIGHT * ourCost;
        } else {
            score = valueScore / (1.0 + Math.pow(ourCost, COST_EXPONENT));
        }
        score -= multiOpponentPenalty(t.pos(), me, model, ourCost);

        double opponentCost = estimateOpponentSteps(t.pos(), me, model);
        int oppScoreMargin = (bait != null && bait.getBaitType() == BaitType.GEM) ? OPP_SCORING_MARGIN_GEM : OPP_SCORING_MARGIN_DEFAULT;
        if (opponentCost < Double.POSITIVE_INFINITY && opponentCost + oppScoreMargin < ourCost) {
            score -= (ourCost - opponentCost);
        }

        Target scoredTarget = (bait != null) ? Target.of(bait, score) : Target.of(t.pos(), score);
        return new PlanCandidate(scoredTarget, plan, score);
    }

    private double estimateOpponentSteps(GridPos goal, Player me, GameStatusModel model) {
        return OpponentHeuristics.estimateOpponentCost(goal, me, model, TOP_L_OPPONENTS, planProvider);
    }

    private double multiOpponentPenalty(GridPos goal, Player me, GameStatusModel model, double ourCost) {
        return OpponentHeuristics.multiOpponentPenalty(goal, me, model, ourCost, MULTI_NEAR_EXTRA, MULTI_NEAR_WEIGHT);
    }

    Deque<Action> planCached(Player p, Target t, GameStatusModel model) {
        if (p == null || t == null) return new ArrayDeque<>();
        String key = planCacheKey(p, t.pos());
        Deque<Action> cached = (key != null) ? planCache.get(key) : null;
        if (cached != null) return new ArrayDeque<>(cached);
        Deque<Action> plan = pathfinder.plan(p, t, model);
        if (plan == null) plan = new ArrayDeque<>();
        if (key != null) {
            planCache.put(key, new ArrayDeque<>(plan));
        }
        return plan;
    }

    private void invalidateCachedPlan(Player actor, Target target) {
        String key = planCacheKey(actor, target != null ? target.pos() : null);
        if (key != null) {
            planCache.remove(key);
        }
    }

    private String planCacheKey(Player actor, GridPos targetPos) {
        if (actor == null || targetPos == null) return null;
        String dir = String.valueOf(actor.getDirection());
        return actor.getxPosition() + "," + actor.getyPosition() + "," + dir
                + "->" + targetPos.x() + "," + targetPos.y();
    }
}

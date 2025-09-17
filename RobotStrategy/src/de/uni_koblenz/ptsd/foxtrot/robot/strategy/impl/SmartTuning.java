package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

/** Centralized tuning/config for SmartStrategy. */
public final class SmartTuning {
    // Scoring
    public final double costWeight;            // λ
    public final boolean useLinearScore;       // if false: value / (1 + steps^costExponent)
    public final double costExponent;

    // Opponent margins
    public final int opponentMarginGem;
    public final int opponentMarginDefault;
    public final int oppScoringMarginGem;
    public final int oppScoringMarginDefault;

    // Candidate limiting
    public final int topKBaits;
    public final int topLOpponents;

    // Multi-opponent pressure
    public final int multiNearExtra;
    public final double multiNearWeight;

    // Interception
    public final boolean enableIntercept;
    public final boolean enableInterceptCoffee;
    public final int interceptKSteps;
    public final int interceptMargin;
    public final int interceptTimeout;

    // Teleport handling
    public final boolean enableTeleportGuard;
    public final int teleportJumpThreshold;

    // Exploration
    public final boolean enableExploration;

    // Endgame
    public final boolean endgameEnabled;
    public final long endgameTicksThreshold;
    public final int endgameMarginBonus;

    // Hysteresis
    public final int replanCooldownTicks;
    public final double scoreDeltaMin;
    public final double scoreRatioMin;

    // Debug + recovery
    public final boolean debugLogging;
    public final int stuckReplanTicks;
    public final int maxStepRetry;

    public SmartTuning(
            double costWeight, boolean useLinearScore, double costExponent,
            int opponentMarginGem, int opponentMarginDefault, int oppScoringMarginGem, int oppScoringMarginDefault,
            int topKBaits, int topLOpponents,
            int multiNearExtra, double multiNearWeight,
            boolean enableIntercept, boolean enableInterceptCoffee, int interceptKSteps, int interceptMargin, int interceptTimeout,
            boolean enableTeleportGuard, int teleportJumpThreshold,
            boolean enableExploration,
            boolean endgameEnabled, long endgameTicksThreshold, int endgameMarginBonus,
            int replanCooldownTicks, double scoreDeltaMin, double scoreRatioMin,
            boolean debugLogging, int stuckReplanTicks, int maxStepRetry) {

        this.costWeight = costWeight;
        this.useLinearScore = useLinearScore;
        this.costExponent = costExponent;
        this.opponentMarginGem = opponentMarginGem;
        this.opponentMarginDefault = opponentMarginDefault;
        this.oppScoringMarginGem = oppScoringMarginGem;
        this.oppScoringMarginDefault = oppScoringMarginDefault;
        this.topKBaits = topKBaits;
        this.topLOpponents = topLOpponents;
        this.multiNearExtra = multiNearExtra;
        this.multiNearWeight = multiNearWeight;
        this.enableIntercept = enableIntercept;
        this.enableInterceptCoffee = enableInterceptCoffee;
        this.interceptKSteps = interceptKSteps;
        this.interceptMargin = interceptMargin;
        this.interceptTimeout = interceptTimeout;
        this.enableTeleportGuard = enableTeleportGuard;
        this.teleportJumpThreshold = teleportJumpThreshold;
        this.enableExploration = enableExploration;
        this.endgameEnabled = endgameEnabled;
        this.endgameTicksThreshold = endgameTicksThreshold;
        this.endgameMarginBonus = endgameMarginBonus;
        this.replanCooldownTicks = replanCooldownTicks;
        this.scoreDeltaMin = scoreDeltaMin;
        this.scoreRatioMin = scoreRatioMin;
        this.debugLogging = debugLogging;
        this.stuckReplanTicks = stuckReplanTicks;
        this.maxStepRetry = maxStepRetry;
    }

    /** Default values mirroring earlier constants. */
    public static SmartTuning defaults() {
        return new SmartTuning(
            3.0, true, 1.1,    // scoring
            2, 1, 1, 1,        // opponent margins
            8, 4,              // candidate limits
            2, 0.75,           // multi-opponent pressure
            true, false, 8, 1, 8,    // intercept
            true, 4,                 // teleport guard
            true,                    // exploration
            false, 600L, 1,          // endgame
            3, 3.0, 1.05,            // hysteresis
            false, 2, 1              // debug/stuck
        );
    }

    /** Same as defaults; explicit name for clarity. */
    public static SmartTuning balanced() { return defaults(); }

    /** Aggressiver auf GEMs, schnellere Commits, weniger Zögern. */
    public static SmartTuning gemRush() {
        return new SmartTuning(
            2.6, true, 1.05,
            2, 1, 1, 1,
            10, 5,
            2, 0.6,
            true, false, 10, 1, 8,
            true, 4,
            true,
            false, 600L, 1,
            2, 2.5, 1.03,
            true, 2, 1
        );
    }

    /** Intercept-Training: Coffee-Intercept an, tieferes K. */
    public static SmartTuning interceptAggro() {
        return new SmartTuning(
            3.0, true, 1.1,
            2, 1, 1, 1,
            8, 6,
            2, 0.75,
            true, true, 12, 1, 10,
            true, 4,
            true,
            false, 600L, 1,
            3, 3.0, 1.05,
            true, 2, 1
        );
    }

    /** Vorsichtigere Variante: mehr Abstand zu Gegnern, stärkere Crowd-Penalty. */
    public static SmartTuning defensiveControl() {
        return new SmartTuning(
            3.4, true, 1.15,
            3, 2, 2, 2,
            8, 6,
            3, 1.0,
            true, false, 8, 2, 10,
            true, 4,
            true,
            true, 540L, 2,
            4, 4.0, 1.06,
            false, 3, 1
        );
    }
}

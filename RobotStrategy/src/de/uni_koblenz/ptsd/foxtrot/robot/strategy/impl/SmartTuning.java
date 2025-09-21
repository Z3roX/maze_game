package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

/**
* Central configuration for {@link SmartStrategy} and its helper components.
*
* <p>This immutable bag of tuning knobs influences candidate generation,
* scoring, opponent handling, exploration fallback, and hysteresis behavior.
* All fields are {@code public final} for simple inspection and dependency-free
* wiring. Typical usage is to construct a single instance (e.g., via a
* factory) and share it across the strategy objects.</p>
*
* <h2>Thread-safety</h2>
* <p>Instances are immutable and therefore thread-safe.</p>
*/

public final class SmartTuning {
    // Scoring
    public final double costWeight;            // lambda in score = value - lambda * cost

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
    public final int interceptKSteps;
    public final int interceptMargin;

    // Teleport handling
    public final boolean enableTeleportGuard;
    public final int teleportJumpThreshold;

    // Exploration
    public final boolean enableExploration;

    // Hysteresis
    public final int replanCooldownTicks;
    public final double scoreDeltaMin;
    public final double scoreRatioMin;

    // Debug + recovery
    public final boolean debugLogging;
    public final int stuckReplanTicks;
    public final int maxStepRetry;

    public SmartTuning(
            double costWeight,
            int opponentMarginGem, int opponentMarginDefault, int oppScoringMarginGem, int oppScoringMarginDefault,
            int topKBaits, int topLOpponents,
            int multiNearExtra, double multiNearWeight,
            boolean enableIntercept, int interceptKSteps, int interceptMargin,
            boolean enableTeleportGuard, int teleportJumpThreshold,
            boolean enableExploration,
            int replanCooldownTicks, double scoreDeltaMin, double scoreRatioMin,
            boolean debugLogging, int stuckReplanTicks, int maxStepRetry) {

        this.costWeight = costWeight;
        this.opponentMarginGem = opponentMarginGem;
        this.opponentMarginDefault = opponentMarginDefault;
        this.oppScoringMarginGem = oppScoringMarginGem;
        this.oppScoringMarginDefault = oppScoringMarginDefault;
        this.topKBaits = topKBaits;
        this.topLOpponents = topLOpponents;
        this.multiNearExtra = multiNearExtra;
        this.multiNearWeight = multiNearWeight;
        this.enableIntercept = enableIntercept;
        this.interceptKSteps = interceptKSteps;
        this.interceptMargin = interceptMargin;
        this.enableTeleportGuard = enableTeleportGuard;
        this.teleportJumpThreshold = teleportJumpThreshold;
        this.enableExploration = enableExploration;
        this.replanCooldownTicks = replanCooldownTicks;
        this.scoreDeltaMin = scoreDeltaMin;
        this.scoreRatioMin = scoreRatioMin;
        this.debugLogging = debugLogging;
        this.stuckReplanTicks = stuckReplanTicks;
        this.maxStepRetry = maxStepRetry;
    }

    public static SmartTuning defaults() {
        return new SmartTuning(
            3.0,
            2, 1, 1, 1,
            8, 4,
            2, 0.75,
            true, 8, 1,
            true, 4,
            true,
            3, 3.0, 1.05,
            false, 2, 1
        );
    }

    public static SmartTuning balanced() {
        return defaults();
    }

    public static SmartTuning gemRush() {
        return new SmartTuning(
            2.6,
            2, 1, 1, 1,
            10, 5,
            2, 0.6,
            true, 10, 1,
            true, 4,
            true,
            2, 2.5, 1.03,
            true, 2, 1
        );
    }

    public static SmartTuning interceptAggro() {
        return new SmartTuning(
            3.0,
            2, 1, 1, 1,
            8, 6,
            2, 0.75,
            true, 12, 1,
            true, 4,
            true,
            3, 3.0, 1.05,
            true, 2, 1
        );
    }

    public static SmartTuning defensiveControl() {
        return new SmartTuning(
            3.4,
            3, 2, 2, 2,
            8, 6,
            3, 1.0,
            true, 8, 2,
            true, 4,
            true,
            4, 4.0, 1.06,
            false, 3, 1
        );
    }
}


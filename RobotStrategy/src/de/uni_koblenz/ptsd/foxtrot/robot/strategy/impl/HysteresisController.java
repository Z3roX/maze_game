package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.function.Function;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

final class HysteresisController {
    private final int cooldownTicks;
    private final double scoreDeltaMin;
    private final double scoreRatioMin;

    private long lastSwitchTick = Long.MIN_VALUE;
    private double currentTargetScore = Double.NEGATIVE_INFINITY;

    HysteresisController(int cooldownTicks, double scoreDeltaMin, double scoreRatioMin) {
        this.cooldownTicks = cooldownTicks;
        this.scoreDeltaMin = scoreDeltaMin;
        this.scoreRatioMin = scoreRatioMin;
    }

    long lastSwitchTick() { return lastSwitchTick; }
    double currentTargetScore() { return currentTargetScore; }
    void reset() { lastSwitchTick = Long.MIN_VALUE; currentTargetScore = Double.NEGATIVE_INFINITY; }

    SmartStrategy.Candidate apply(Target currentTarget, SmartStrategy.Candidate chosen, long tickCounter, Function<Target, SmartStrategy.Candidate> recompute) {
        if (currentTarget == null || chosen == null) return chosen;
        boolean isDifferent = !currentTarget.pos().equals(chosen.target().pos());
        if (!isDifferent) return chosen;

        boolean cooldownActive = (tickCounter - lastSwitchTick) < cooldownTicks;
        double gain = chosen.score() - currentTargetScore;
        boolean bigEnoughGain = gain >= Math.max(scoreDeltaMin, Math.abs(currentTargetScore) * (scoreRatioMin - 1.0));

        if (cooldownActive || !bigEnoughGain) {
            SmartStrategy.Candidate stick = recompute.apply(currentTarget);
            return (stick != null) ? stick : chosen;
        } else {
            lastSwitchTick = tickCounter;
            return chosen;
        }
    }

    void onChosen(SmartStrategy.Candidate chosen) {
        if (chosen != null) {
            currentTargetScore = chosen.score();
        } else {
            currentTargetScore = Double.NEGATIVE_INFINITY;
        }
    }
}


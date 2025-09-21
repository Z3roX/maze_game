package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.function.Function;

import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

/**
* Hysteresis gate for target switching to prevent rapid flip‑flopping.
*
* <p>This helper keeps track of the last switch time and the score of the
* currently pursued target. When the strategy proposes a new {@link Target}
* (the <em>chosen</em> candidate), {@link #apply(Target, SmartStrategy.Candidate, long, java.util.function.Function)}
* decides whether to accept the switch. A switch is allowed only if:
* <ul>
* <li>the cooldown (in ticks) since the last switch has expired, and</li>
* <li>the new candidate improves sufficiently over the current target,
* either by an absolute delta (scoreDeltaMin) or by a relative ratio
* (scoreRatioMin).</li>
* </ul>
*
* <p>If switching is denied, the controller asks the caller to recompute
* a fresh candidate for the current target and returns that instead. Call
* {@link #onChosen(SmartStrategy.Candidate)} after the caller finally commits
* to a candidate so the internal state stays consistent.
*/

final class HysteresisController {
    private final int cooldownTicks;
    private final double scoreDeltaMin;
    private final double scoreRatioMin;

    private long lastSwitchTick = Long.MIN_VALUE;
    private double currentTargetScore = Double.NEGATIVE_INFINITY;

    /**
    * Creates a new controller.
    *
    * @param cooldownTicks minimum number of ticks that must pass after a switch
    * before another switch is allowed
    * @param scoreDeltaMin minimal absolute score improvement required to switch
    * @param scoreRatioMin minimal multiplicative improvement required to switch
    * (e.g., 1.10 means “at least +10% better”)
    */

    HysteresisController(int cooldownTicks, double scoreDeltaMin, double scoreRatioMin) {
        this.cooldownTicks = cooldownTicks;
        this.scoreDeltaMin = scoreDeltaMin;
        this.scoreRatioMin = scoreRatioMin;
    }

    /** Returns the tick index at which the last accepted target switch occurred. */
    long lastSwitchTick() { return lastSwitchTick; }


    /** Returns the score of the currently pursued/locked-in target. */
    double currentTargetScore() { return currentTargetScore; }


    /** Resets the controller to its initial state (no switch history, score = -∞). */
    void reset() { lastSwitchTick = Long.MIN_VALUE; currentTargetScore = Double.NEGATIVE_INFINITY; }


    /**
    * Decide whether to switch to a newly proposed candidate, applying hysteresis.
    *
    * @param currentTarget the target we are currently pursuing (may be {@code null})
    * @param chosen the newly proposed candidate from the planner (may be {@code null})
    * @param tickCounter the current global tick/time counter used for cooldown checks
    * @param recompute callback to obtain a fresh candidate for {@code currentTarget}
    * if switching is denied
    * @return the candidate to execute next: either {@code chosen} (switch accepted)
    * or a recomputed candidate for {@code currentTarget} (stick with current)
    */

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

    /**
    * Notify the controller which candidate was actually selected by the caller.
    * Updates the internal score baseline used to measure future improvements.
    *
    * @param chosen the committed candidate, or {@code null} if no target is active
    */

    void onChosen(SmartStrategy.Candidate chosen) {
        if (chosen != null) {
            currentTargetScore = chosen.score();
        } else {
            currentTargetScore = Double.NEGATIVE_INFINITY;
        }
    }
}


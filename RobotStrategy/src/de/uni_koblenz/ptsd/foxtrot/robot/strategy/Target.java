package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;

/**
 * Value object describing a goal for the robot.
 * <p>
 * A target either references a concrete {@code Bait} (gem) to pursue
 * or an exploratory grid position the robot should navigate to when no
 * specific bait is selected. The {@code utility} expresses how desirable
 * the target is, allowing strategies to rank competing options.
 */

public final class Target {
    private final GridPos pos;
    private final Bait bait;
    private final double utility;

    private Target(GridPos pos, Bait bait, double utility) {
        this.pos = pos;
        this.bait = bait;
        this.utility = utility;
    }

    /**
     * Create a target for a concrete bait (gem).
     *
     * @param bait     the bait to pursue (must not be {@code null})
     * @param utility  desirability score used for ranking (higher is better)
     * @return a new {@code Target} bound to the given bait
     */

    public static Target of(Bait bait, double utility) {
        return new Target(new GridPos(bait.getxPosition(), bait.getyPosition()), bait, utility);
    }

    /**
     * Create an exploratory target for an arbitrary grid position.
     *
     * @param pos      grid coordinate to move toward (must not be {@code null})
     * @param utility  desirability score used for ranking (higher is better)
     * @return a new {@code Target} at the given position with no bait bound
     */

    public static Target of(GridPos pos, double utility) {
        return new Target(pos, null, utility);
    }

    /** @return the target grid position (never {@code null}) */
    public GridPos pos() { return pos; }

    /** @return the bait this target is bound to, or {@code null} if exploratory */
    public Bait bait() { return bait; }

    /** @return the desirability score for ranking this target */
    public double utility() { return utility; }

    /** @return {@code true} if this target is exploratory (no bait bound) */
    public boolean isExploration() { return bait == null; }
}


package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;

public final class Target {
    private final GridPos pos;
    private final Bait bait;
    private final double utility;

    private Target(GridPos pos, Bait bait, double utility) {
        this.pos = pos;
        this.bait = bait;
        this.utility = utility;
    }

    public static Target of(Bait bait, double utility) {
        return new Target(new GridPos(bait.getxPosition(), bait.getyPosition()), bait, utility);
    }

    public static Target of(GridPos pos, double utility) {
        return new Target(pos, null, utility);
    }

    public GridPos pos() { return pos; }
    public Bait bait() { return bait; }
    public double utility() { return utility; }
    public boolean isExploration() { return bait == null; }
}

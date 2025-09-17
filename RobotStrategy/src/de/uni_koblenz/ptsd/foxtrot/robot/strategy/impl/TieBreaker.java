package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.GridPos;

/** Deterministic tie-breaker for equal-scoring candidates. */
final class TieBreaker {
    private TieBreaker() {}

    private static int baitPriority(BaitType type) {
        if (type == null) return -1;
        return switch (type) {
            case GEM -> 3;
            case COFFEE -> 2;
            case FOOD -> 1;
            default -> -1;
        };
    }

    /** @return negative if a preferred to b, zero if equal, positive if b preferred */
    static int compare(PlanCandidate a, PlanCandidate b) {
        var ta = (a.target() != null) ? a.target().bait() : null;
        var tb = (b.target() != null) ? b.target().bait() : null;
        var taType = (ta != null) ? ta.getBaitType() : null;
        var tbType = (tb != null) ? tb.getBaitType() : null;
        int pr = Integer.compare(baitPriority(tbType), baitPriority(taType)); // <0 => a wins
        if (pr != 0) return pr;

        double ca = CostUtil.planCost(a.plan());
        double cb = CostUtil.planCost(b.plan());
        int cmpCost = Double.compare(ca, cb);
        if (cmpCost != 0) return cmpCost;

        GridPos pa = a.target().pos();
        GridPos pb = b.target().pos();
        if (pa.x() != pb.x()) return Integer.compare(pa.x(), pb.x());
        if (pa.y() != pb.y()) return Integer.compare(pa.y(), pb.y());
        return 0;
    }
}

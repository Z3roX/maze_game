package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;

public interface Strategy {
    Action decideNext(GameStatusModel model, Player me);
    default void reset() {}
}

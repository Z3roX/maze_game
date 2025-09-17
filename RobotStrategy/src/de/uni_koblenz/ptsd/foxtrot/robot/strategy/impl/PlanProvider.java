package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.Deque;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

interface PlanProvider {
    Deque<Action> planFor(Player actor, Target target, GameStatusModel model);
}

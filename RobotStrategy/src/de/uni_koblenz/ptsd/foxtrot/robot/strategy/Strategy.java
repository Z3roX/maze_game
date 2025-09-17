package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;

/**
 * A robot strategy decides what to do next based on the current model state.
 */
public interface Strategy {

    /**
     * Determines the next action the robot should perform.
     *
     * @param model the shared game status model (never {@code null})
     * @param me    the player entity representing the controlled robot (never {@code null})
     * @return the action to execute; never {@code null}
     */
    Action decideNext(GameStatusModel model, Player me);

    /**
     * Resets any internal state so that a new planning cycle can begin.
     */
    void reset();
}

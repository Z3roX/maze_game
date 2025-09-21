package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;

/**
 * A decision policy that selects the next {@link Action} based on the current
 * game model and the perspective of the controlled player.
 * <p>
 * Implementations should be side-effect free and stateless where possible; if
 * transient state is needed (e.g., cached paths), override {@link #reset()} to
 * clear it when the round/game resets.
 */

public interface Strategy {
    /**
     * Decide the next atomic {@link Action} to perform.
     *
     * @param model the current immutable snapshot of the game state
     * @param me the player this strategy controls
     * @return the next action; never {@code null}
     */
    Action decideNext(GameStatusModel model, Player me);

    /**
     * Reset any internal, transient state (e.g., cached paths or timers).
     * <p>Default is a no-op.</p>
     */

    default void reset() {}
}


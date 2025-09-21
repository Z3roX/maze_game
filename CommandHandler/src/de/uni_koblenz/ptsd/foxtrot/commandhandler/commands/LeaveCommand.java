package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/**
* Represents a player's leave action from a game/lobby and updates/cleans up
* the {@link GameStatusModel} accordingly.
* <p>
* Typical tasks: removing player mappings, resetting status values, or triggering
* appropriate UI updates.
* </p>
*
*/
public class LeaveCommand implements Command {
    private final int playerId;

    public LeaveCommand(int playerId) {
        this.playerId = playerId;
    }

    /**
    * Applies the leave changes to the model.
    */

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getPlayers() != null) {
            model.getPlayers().remove(playerId);
        }
    }
}


package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/** Removes a player with the given id from the model. */
public class LeaveCommand implements Command {
    private final int playerId;

    public LeaveCommand(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getPlayers() != null) {
            model.getPlayers().remove(playerId);
        }
    }
}


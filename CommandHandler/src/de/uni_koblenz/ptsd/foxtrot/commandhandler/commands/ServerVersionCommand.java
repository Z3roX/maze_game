package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/**
* Stores the server identifier/version in the {@link GameStatusModel} and sets
* the state to {@link State#CONNECTED}.
*/ 
public class ServerVersionCommand implements Command {

    private final int serverId;

    public ServerVersionCommand(int serverId) {
        this.serverId = serverId;
    }

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        model.setserverID(serverId);
        model.setState(State.CONNECTED);
    }
}
package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/**
* Marks the client as disconnected because the user quit intentionally.
* Sets the {@link State} of the {@link GameStatusModel} to {@link State#DISCONNECTED}.
*/
public class QuitCommand implements Command {

    @Override
    public void execute() {
        GameStatusModel.getInstance().setState(State.DISCONNECTED);
    }
}
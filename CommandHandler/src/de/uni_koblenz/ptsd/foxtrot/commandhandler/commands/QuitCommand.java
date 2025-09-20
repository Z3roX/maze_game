package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/** Marks the client as disconnected due to quit. */
public class QuitCommand implements Command {
    @Override
    public void execute() {
        GameStatusModel.getInstance().setState(State.DISCONNECTED);
    }
}
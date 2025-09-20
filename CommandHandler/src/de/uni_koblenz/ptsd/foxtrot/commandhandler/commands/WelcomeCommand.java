package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;

/** Marks the client as logged in and stores its id. */
public class WelcomeCommand implements Command {
    private final int clientId;

    public WelcomeCommand(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        model.setclientID(clientId);
        model.setState(State.LOGGEDIN);
    }
}
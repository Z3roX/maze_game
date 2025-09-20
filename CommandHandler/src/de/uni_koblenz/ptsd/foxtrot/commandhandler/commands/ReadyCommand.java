package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import javafx.application.Platform;

/** Marks the game as active/ready. */
public class ReadyCommand implements Command {
    @Override
    public void execute() {
    	GameStatusModel model = GameStatusModel.getInstance();

        Platform.runLater(() -> {
            model.setState(State.ACTIVE);
            model.setReady(true);
        });
    }
}
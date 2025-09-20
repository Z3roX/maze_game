package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;


import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import javafx.application.Platform;
import javafx.scene.control.Alert;


/** Marks the client as disconnected due to termination. */
public class TerminateCommand implements Command {

    @Override
    public void execute() {
        GameStatusModel.getInstance().setState(State.DISCONNECTED);
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Server has terminated.");
            a.setHeaderText(null);
            a.showAndWait();
            
        });
    }
}

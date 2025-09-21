package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;


import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import javafx.application.Platform;
import javafx.scene.control.Alert;


/**
* Displays or logs an informational message for the user (e.g., a JavaFX dialog).
* <p>
* Useful to communicate server messages, status changes, or error situations in a
* user-friendly way.
* </p>
*
* <h2>Threading</h2>
* <p>UI interactions are typically executed on the JavaFX Application Thread via
* {@link Platform#runLater(Runnable)}.</p>
*
*/
public class InfoCommand implements Command {
    private final int infoCode;

    public InfoCommand(int infoCode) {
        this.infoCode = infoCode;
    }

    public int getInfoCode() {
        return this.infoCode;
    }

    /**
    * Performs the display/logging of the message.
    * Implementations may open an {@link Alert}, write to logs, etc.
    */

    @Override
    public void execute() {
        Platform.runLater(() -> {
            GameStatusModel model = GameStatusModel.getInstance();

            switch (this.infoCode) {
            case 453 -> model.setReady(true); // STEP not possible

            case 452 -> { // Nickname already used
                Alert a = new Alert(Alert.AlertType.ERROR, "Nickname already in use. Please choose another.");
                a.setHeaderText(null);
                a.showAndWait();
            }

            case 451 -> { // Too many clients
                Alert a = new Alert(Alert.AlertType.ERROR,
                        "Too many clients connected. Please wait and try again later.");
                a.setHeaderText(null);
                a.showAndWait();
            }

            case 457 -> { // Login timeout
                Alert a = new Alert(Alert.AlertType.ERROR, "Login timeout. Please reconnect.");
                a.setHeaderText(null);
                a.showAndWait();
                
            }

            default -> {
                System.err.println("Unhandled INFO code: " + this.infoCode);
            }
            }
        });
    }
}
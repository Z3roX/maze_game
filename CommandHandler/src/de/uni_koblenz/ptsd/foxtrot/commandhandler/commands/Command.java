package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;


/**
* Base interface for executable commands in the client.
* <p>
* A {@code Command} encapsulates a concrete action (e.g., mutating the game state
* or displaying a UI notification) behind a uniform {@link #execute()} method.
* Instances are typically submitted to a {@code CommandHandler} which processes
* them sequentially.
* </p>
*
* <h2>Threading</h2>
* <p>
* Implementations may perform UI operations. If needed, they should marshal
* execution to the JavaFX Application Thread (e.g., using {@code Platform.runLater}).
* </p>
*
* @since 1.0
*/
public interface Command {
/**
* Executes the command.
* <p>
* Implementations may have side effects (e.g., mutating the {@code GameStatusModel}
* or showing dialogs). This method should not leak unchecked exceptions.
* </p>
*/
void execute();
}
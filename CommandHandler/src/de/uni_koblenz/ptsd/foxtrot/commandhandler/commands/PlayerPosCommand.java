package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.PlayerEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.collections.FXCollections;

/**
* Updates a player's position and facing direction in the {@link GameStatusModel}.
* <p>
* If the player with the given {@code playerId} does not yet exist, they are created
* in the model and then updated. The optional {@code event} is reserved for future
* extensions (e.g., animation, state transitions) and is currently not used in model updates.
* </p>
*
* <h2>Threading</h2>
* <p>
* Changes to the {@code GameStatusModel} may trigger UI updates if it is observed
* (e.g., JavaFX bindings). This class does not explicitly marshal to the FX thread.
* </p>
*
* @since 1.0
*/
public class PlayerPosCommand implements Command {
    private final int playerId;
    private final int x;
    private final int y;
    private final Direction direction;
    private final PlayerEvent event; // kept for completeness; not used in model update


    public PlayerPosCommand(int playerId, int x, int y, Direction direction, PlayerEvent event) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.event = event;
    }

    /** Ensures the player exists in the model and updates position and direction. */
    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getPlayers() == null) {
            model.setPlayers(FXCollections.observableHashMap());
        }
        Player player = model.getPlayers().get(this.playerId);
        if (player == null) {
            player = new Player(0, 0, this.playerId, "Player" + this.playerId);
            model.getPlayers().put(this.playerId, player);
        }

        player.setxPosition(this.x);
        player.setyPosition(this.y);
        player.setDirection(this.direction);
    }
}

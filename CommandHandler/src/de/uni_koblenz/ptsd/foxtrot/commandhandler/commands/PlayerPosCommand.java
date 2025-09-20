package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.PlayerEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.collections.FXCollections;

/** Updates a player's position and direction. */
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

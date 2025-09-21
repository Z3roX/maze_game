package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.collections.FXCollections;

/**
* Updates the score of a player in the {@link GameStatusModel}.
* <p>
* If the player with the given ID does not exist yet, a placeholder player is created
* and the score is set.
* </p>
*
*/
public class PlayerScoreCommand implements Command {
    private final int playerId;
    private final int score;

    public PlayerScoreCommand(int playerId, int score) {
        this.playerId = playerId;
        this.score = score;
    }

    /** Ensures the player exists in the model and sets the score. */
    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getPlayers() == null) {
            model.setPlayers(FXCollections.observableHashMap());
        }
        Player player = model.getPlayers().get(playerId);
        if (player == null) {
            player = new Player(0, 0, playerId, "Player" + playerId);
            model.getPlayers().put(playerId, player);
        }
        player.setScore(score);
    }
}


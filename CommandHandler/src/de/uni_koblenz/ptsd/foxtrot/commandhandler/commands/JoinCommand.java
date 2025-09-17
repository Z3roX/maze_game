package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.collections.FXCollections;

/** Adds a player with id and nickname to the model. */
public class JoinCommand implements Command {
    private final int playerId;
    private final String nickname;

    public JoinCommand(int playerId, String nickname) {
        this.playerId = playerId;
        this.nickname = nickname;
    }

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getPlayers() == null) {
            model.setPlayers(FXCollections.observableHashMap());
        }
        Player player = new Player(0, 0, playerId, nickname);
        model.getPlayers().put(playerId, player);
    }
}


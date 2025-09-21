package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import javafx.collections.FXCollections;

/**
* Represents a player's join action to a game/lobby and writes the relevant
* information to the {@link GameStatusModel}.
* <p>
* Typical tasks: setting player identifiers, room/team data, initial status,
* or initializing additional model-related data.
* </p>
*
*/
public class JoinCommand implements Command {
    private final int playerId;
    private final String nickname;

    public JoinCommand(int playerId, String nickname) {
        this.playerId = playerId;
        this.nickname = nickname;
    }


    /**
     * Applies the join information to the model.
     * <p>
     * Note: adapt the implementation to your concrete model fields (e.g., player map,
     * lobby name, team assignment). This placeholder keeps the original logic separate.
     * </p>
     */
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


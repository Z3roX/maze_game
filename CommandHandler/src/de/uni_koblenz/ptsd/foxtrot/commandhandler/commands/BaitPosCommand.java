package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import javafx.collections.FXCollections;

public class BaitPosCommand implements Command {
    private final int x;
    private final int y;
    private final BaitType type;
    private final BaitEvent event;

    public BaitPosCommand(int x, int y, BaitType type, BaitEvent event) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.event = event;
    }

    @Override
    public void execute() {
        GameStatusModel model = GameStatusModel.getInstance();
        if (model.getBaits() == null) {
            model.setBaits(FXCollections.observableHashMap());
        }
        int key = baitKey(this.x, this.y);
        switch (this.event) {
        case APP:
            model.getBaits().put(key, new Bait(this.x, this.y, this.type, true));
            break;
        case VAN:
            model.getBaits().remove(key);
            break;
        default:
            break;
        }
    }

    private static int baitKey(int x, int y) {
        // Simple coordinate-based stable key
        return x * 100_000 + y;
    }
}

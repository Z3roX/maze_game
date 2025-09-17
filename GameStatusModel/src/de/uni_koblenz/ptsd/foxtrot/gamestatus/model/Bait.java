package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;

public class Bait extends Entity {
    private BaitType baitType;
    private boolean visible;

    public Bait(int xPosition, int yPosition, BaitType baitType, boolean visible) {
        super(xPosition, yPosition);
        this.baitType = baitType;
        this.visible = visible;
    }

    public BaitType getBaitType() {
        return this.baitType;
    }

    public void setBaitType(BaitType baitType) {
        this.baitType = baitType;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}

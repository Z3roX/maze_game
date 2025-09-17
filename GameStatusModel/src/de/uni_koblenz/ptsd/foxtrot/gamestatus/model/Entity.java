package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public abstract class Entity {
    private IntegerProperty xPosition = new SimpleIntegerProperty();
    private IntegerProperty yPosition = new SimpleIntegerProperty();

    public Entity(int xPosition, int yPosition) {
        this.xPosition = new SimpleIntegerProperty(xPosition);
        this.yPosition = new SimpleIntegerProperty(yPosition);
    }

    public int getxPosition() {
        return this.xPosition.get();
    }

    public void setxPosition(int x) {
        this.xPosition.set(x);
    }

    public IntegerProperty xPositionProperty() {
        return this.xPosition;
    }

    public int getyPosition() {
        return this.yPosition.get();
    }

    public void setyPosition(int y) {
        this.yPosition.set(y);
    }

    public IntegerProperty yPositionProperty() {
        return this.yPosition;
    }

}

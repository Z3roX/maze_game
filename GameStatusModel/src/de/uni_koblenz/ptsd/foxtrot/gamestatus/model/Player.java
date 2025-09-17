package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Player extends Entity {
    private int ID;
    private String nickName;
    private ObjectProperty<Direction> direction = new SimpleObjectProperty<Direction>();
    private IntegerProperty score = new SimpleIntegerProperty();

    public Player(int xPosition, int yPosition, int iD, String nickName) {
        super(xPosition, yPosition);
        this.ID = iD;
        this.nickName = nickName;

    }

    public int getID() {
        return this.ID;
    }

    public void setID(int iD) {
        this.ID = iD;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public ObjectProperty<Direction> directionProperty() {
        return this.direction;
    }

    public Direction getDirection() {
        return this.direction.get();
    }

    public void setDirection(Direction direction) {
        this.direction.set(direction);
    }

    public int getScore() {
        return this.score.get();
    }

    public void setScore(int score) {
        this.score.set(score);
    }

    public IntegerProperty scoreProperty() {
        return this.score;
    }

}

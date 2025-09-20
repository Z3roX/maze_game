package de.uni_koblenz.ptsd.foxtrot.gamestatus.model;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Represents a player in the MazeGame.
 * <p>
 * A {@code Player} is an {@link Entity} with a unique identifier,
 * a nickname, a current viewing {@link Direction}, and an accumulated score.
 * Player positions are managed via the inherited {@code xPosition} and
 * {@code yPosition} properties from {@link Entity}.
 * </p>
 *
 * <p>
 * The server assigns each player an ID and communicates updates about
 * position, direction, and score via protocol messages. The client keeps
 * this information synchronized through this class.
 * </p>
 *
 * <p>
 * <b>Attributes:</b>
 * <ul>
 *   <li>{@code ID} – unique player identifier assigned by the server</li>
 *   <li>{@code nickName} – name chosen by the user when joining the game</li>
 *   <li>{@code direction} – current viewing direction of the player</li>
 *   <li>{@code score} – accumulated score of the player</li>
 * </ul>
 * </p>
 *
 * <p><b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.</p>
 *
 * 
 */
public class Player extends Entity {
	
	// Unique player identifier assigned by the server.
    private int ID;
    
    // Nickname chosen by the user.
    private String nickName;
    
    // Current viewing direction of the player.
    private ObjectProperty<Direction> direction = new SimpleObjectProperty<Direction>();
    
    // Current score of the player.
    private IntegerProperty score = new SimpleIntegerProperty();

    /**
     * Creates a new {@code Player} at the given maze coordinates.
     *
     * @param xPosition the initial horizontal coordinate of the player
     * @param yPosition the initial vertical coordinate of the player
     * @param iD        the unique player ID assigned by the server
     * @param nickName  the nickname of the player
     */
    public Player(int xPosition, int yPosition, int iD, String nickName) {
        super(xPosition, yPosition);
        this.ID = iD;
        this.nickName = nickName;

    }

    /**
     * Returns the unique player ID.
     *
     * @return the player ID
     */
    public int getID() {
        return this.ID;
    }

    /**
     * Sets the unique player ID.
     *
     * @param iD the new player ID
     */
    public void setID(int iD) {
        this.ID = iD;
    }

    /**
     * Returns the nickname of the player.
     *
     * @return the nickname
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * Sets the nickname of the player.
     *
     * @param nickName the new nickname
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * Property accessor for the viewing direction.
     *
     * @return the {@link ObjectProperty} of the direction
     */
    public ObjectProperty<Direction> directionProperty() {
        return this.direction;
    }

    /**
     * Returns the current viewing direction of the player.
     *
     * @return the {@link Direction}
     */
    public Direction getDirection() {
        return this.direction.get();
    }

    /**
     * Sets the current viewing direction of the player.
     *
     * @param direction the new {@link Direction}
     */
    public void setDirection(Direction direction) {
        this.direction.set(direction);
    }

    /**
     * Returns the current score of the player.
     *
     * @return the score
     */
    public int getScore() {
        return this.score.get();
    }

    /**
     * Sets the score of the player.
     *
     * @param score the new score value
     */
    public void setScore(int score) {
        this.score.set(score);
    }

    /**
     * Property accessor for the score.
     *
     * @return the {@link IntegerProperty} of the score
     */
    public IntegerProperty scoreProperty() {
        return this.score;
    }

}

package de.uni_koblenz.ptsd.foxtrot.robot.strategy;

/**
 * Describes the primitive actions the robot strategy can perform.
 */
public enum Action {
    /** rotate 90 degrees to the left */
    TURN_LEFT,
    /** rotate 90 degrees to the right */
    TURN_RIGHT,
    /** move one tile forward */
    STEP,
    /** do nothing for one tick */
    IDLE
}

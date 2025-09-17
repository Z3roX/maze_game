module RobotStrategy {
    requires transitive GameStatusModel;
    requires transitive MazeGameProtocol;
    requires javafx.base;
    requires javafx.graphics;
    requires java.logging;

    exports de.uni_koblenz.ptsd.foxtrot.robot.strategy;
    exports de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;
}

module RobotStrategy {
    exports de.uni_koblenz.ptsd.foxtrot.robot.strategy;
    exports de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

    requires transitive GameStatusModel;
    requires transitive MazeGameProtocol;
    requires transitive CommandHandler;
    requires transitive javafx.base;
    requires javafx.graphics;
    requires java.logging;
}


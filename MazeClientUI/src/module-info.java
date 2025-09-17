module MazeClientUI {
    requires GameStatusModel;
    requires MazeGameProtocol;
    requires CommandHandler;
    requires RobotStrategy;
    requires javafx.graphics;
    requires javafx.base;
    requires java.logging;
    requires javafx.fxml;
    requires javafx.controls;

    opens de.uni_koblenz.ptsd.foxtrot.mazeclient.gui to javafx.fxml;

    exports de.uni_koblenz.ptsd.foxtrot.mazeclient to javafx.graphics;
}

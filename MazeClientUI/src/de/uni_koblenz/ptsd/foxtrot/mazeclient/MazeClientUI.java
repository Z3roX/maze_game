package de.uni_koblenz.ptsd.foxtrot.mazeclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MazeClientUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(this.getClass()
                .getResource("/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/ConnectDialog.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Connect to Maze Server");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

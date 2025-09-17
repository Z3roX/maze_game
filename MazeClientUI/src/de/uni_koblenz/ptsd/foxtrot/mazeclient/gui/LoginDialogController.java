package de.uni_koblenz.ptsd.foxtrot.mazeclient.gui;

import java.io.IOException;

import de.uni_koblenz.ptsd.foxtrot.mazeclient.app.MazeClientLogic;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginDialogController {

    @FXML
    private TextField nicknameTextField;
    @FXML
    private Button playButton;

    @FXML
    public void loginPlayer(ActionEvent event) {
        String nickname = this.nicknameTextField.getText().trim();
        if (nickname.isEmpty()) {
            this.showError("Please enter a nickname");
            return;
        }

        try {
            MazeClientLogic.getInstance().login(nickname);
            MazeClientLogic.getInstance().requestMaze();

            FXMLLoader loader = new FXMLLoader(this.getClass()
                    .getResource("/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/MazeClientUI.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Maze Game");
            stage.show();

            ((Stage) this.playButton.getScene().getWindow()).close();

        } catch (IOException e) {
            this.showError("Login failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

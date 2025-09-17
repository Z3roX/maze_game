package de.uni_koblenz.ptsd.foxtrot.mazeclient.gui;

import java.io.IOException;

import de.uni_koblenz.ptsd.foxtrot.mazeclient.app.MazeClientLogic;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConnectDialogController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        this.statusLabel.setText("Not connected");
        this.hostField.setText("localhost");
        this.portField.setText("12345");
    }

    @FXML
    private void connect() {
        try {
            String host = this.hostField.getText().trim();
            int port = Integer.parseInt(this.portField.getText().trim());

            MazeClientLogic.getInstance().connect(host, port);
            this.statusLabel.setText("Connected to " + host + ":" + port);
            this.statusLabel.setStyle("-fx-text-fill: green;");

            FXMLLoader loader = new FXMLLoader(this.getClass()
                    .getResource("/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/LoginDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Choose Nickname");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

            ((Stage) this.connectButton.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            this.showError("Port must be a number");
        } catch (IOException e) {
            this.showError("Connection failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

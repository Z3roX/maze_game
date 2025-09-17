package de.uni_koblenz.ptsd.foxtrot.mazeclient.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Bait;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Player;
import de.uni_koblenz.ptsd.foxtrot.mazeclient.app.MazeClientLogic;
import de.uni_koblenz.ptsd.foxtrot.mazeclient.app.Zoom;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.StrategyMode;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class MazeClientAppController {

    @FXML
    private Button stepButton;
    @FXML
    private Button turnLeftButton;
    @FXML
    private Button turnRightButton;
    @FXML
    private ComboBox<StrategyMode> strategyChoice;
    @FXML
    private Slider zoomSlider;

    @FXML
    private TableView<Player> scoreTable;
    @FXML
    private TableColumn<Player, Number> idColumn;
    @FXML
    private TableColumn<Player, String> nameColumn;
    @FXML
    private TableColumn<Player, Number> scoreColumn;

    @FXML
    private StackPane mazeStack;

    private final MazeClientLogic logic = MazeClientLogic.getInstance();
    private final GameStatusModel model = this.logic.getModel();

    private final Map<Integer, Node> playerNodes = new HashMap<>();
    private final Map<Integer, Node> baitNodes = new HashMap<>();

    private final ObservableList<Player> scoreItems = FXCollections.observableArrayList();

    private final Pane mazeGroup = new Pane();
    private final Pane entityGroup = new Pane();
    private static final double TILE_SIZE = 24.0;

    @FXML
    private void initialize() {
        this.mazeStack.getChildren().addAll(this.mazeGroup, this.entityGroup);

        this.idColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
        this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("nickName"));
        this.scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        this.scoreTable.setItems(this.scoreItems);

        Zoom.enableZoom(this.mazeStack);
        Zoom.bindSlider(this.mazeStack, this.zoomSlider);

        this.setupStrategyChoice();

        this.drawMaze(this.model.getMaze());

        // Maze Updates
        this.model.mazeProperty().addListener((obs, oldM, newM) -> {
            if (newM != null) {
                this.drawMaze(newM);
            }
        });

        this.model.getPlayers().addListener((MapChangeListener<Integer, Player>) change -> {
            if (change.wasAdded()) {
                this.addPlayerNode(change.getKey(), change.getValueAdded());
            }
            if (change.wasRemoved()) {
                this.removePlayerNode(change.getKey());
            }
            this.scoreItems.setAll(this.model.getPlayers().values());
        });

        this.model.getBaits().addListener((MapChangeListener<Integer, Bait>) change -> {
            if (change.wasAdded()) {
                this.addBaitNode(change.getKey(), change.getValueAdded());
            }
            if (change.wasRemoved()) {
                this.removeBaitNode(change.getKey());
            }
        });

        this.model.readyProperty().addListener((obs, oldVal, newVal) -> {
            this.stepButton.setDisable(!newVal);
            this.turnLeftButton.setDisable(!newVal);
            this.turnRightButton.setDisable(!newVal);
        });

        this.stepButton.setDisable(true);
        this.turnLeftButton.setDisable(true);
        this.turnRightButton.setDisable(true);

        this.mazeStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setOnCloseRequest(e -> {
                    MazeClientLogic.getInstance().disconnect();
                    GameStatusModel.getInstance().reset();
                });
            }
        });

    }

    @FXML
    private void disconnect() {
        try {
            this.logic.disconnect();
        } catch (Exception ignored) {
        } finally {
            this.scoreItems.clear();
            this.mazeGroup.getChildren().clear();
            this.entityGroup.getChildren().clear();
        }
    }

    @FXML
    private void step() {
        try {
            this.logic.step();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.model.setReady(false);

    }

    @FXML
    private void turnLeft() {
        try {
            this.logic.turnLeft();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.model.setReady(false);

    }

    @FXML
    private void turnRight() {
        try {
            this.logic.turnRight();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.model.setReady(false);

    }

    private void drawMaze(Maze maze) {
        this.mazeGroup.getChildren().clear();
        if (maze == null) {
            return;
        }

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                CellType type = maze.getTypeAt(x, y);

                String file = switch (type) {
                case PATH -> "Cell_Path.png";
                case WALL -> "Cell_Wall.png";
                case WATER -> "Cell_Water.png";
                case UNKNOWN -> null;
                };

                if (file != null) {
                    String path = "/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/images/" + file;
                    try {
                        ImageView tile = new ImageView(new Image(this.getClass().getResource(path).toExternalForm()));
                        tile.setFitWidth(TILE_SIZE);
                        tile.setFitHeight(TILE_SIZE);
                        tile.setPreserveRatio(false);

                        // Position
                        tile.setTranslateX(x * TILE_SIZE);
                        tile.setTranslateY(y * TILE_SIZE);

                        this.mazeGroup.getChildren().add(tile);
                    } catch (Exception e) {
                        System.err.println("Maze image could not load:  " + path);
                    }
                }
            }
        }
        double widthPx = maze.getWidth() * TILE_SIZE;
        double heightPx = maze.getHeight() * TILE_SIZE;

        this.mazeGroup.setPrefSize(widthPx, heightPx);
        this.entityGroup.setPrefSize(widthPx, heightPx);

    }

    private void addPlayerNode(int id, Player player) {
        ImageView view = new ImageView();
        view.setFitWidth(TILE_SIZE * 0.8);
        view.setFitHeight(TILE_SIZE * 0.8);
        view.setPreserveRatio(true);
        String prefix = id <= 3 ? "Dummy" : "Player";

        Map<Direction, String> dirMap = Map.of(Direction.N, "up", Direction.S, "down", Direction.E, "right",
                Direction.W, "left");

        // Hilfsfunktion zum Laden des Bildes
        Consumer<Direction> setImage = dir -> {
            String d = dir != null ? dirMap.get(dir) : "down";
            String path = "/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/images/" + prefix + "_" + d + ".png";
            try {
                view.setImage(new Image(this.getClass().getResource(path).toExternalForm()));
            } catch (Exception e) {
                System.err.println("Picture is missing: " + path);
            }
        };

        // Startbild
        setImage.accept(player.getDirection());
        double offset = TILE_SIZE * (1 - 0.8) / 2; // also 0.1 * TILE_SIZE
        view.translateXProperty().bind(player.xPositionProperty().multiply(TILE_SIZE).add(offset));
        view.translateYProperty().bind(player.yPositionProperty().multiply(TILE_SIZE).add(offset));

        // Bei Richtungswechsel Bild anpassen
        player.directionProperty().addListener((obs, oldDir, newDir) -> setImage.accept(newDir));

        this.entityGroup.getChildren().add(view);
        this.playerNodes.put(id, view);

    }

    private void removePlayerNode(int id) {
        Node node = this.playerNodes.remove(id);
        if (node != null) {
            this.entityGroup.getChildren().remove(node);
        }
    }

    private void addBaitNode(int id, Bait bait) {
        ImageView baitView = new ImageView();
        baitView.setFitWidth(TILE_SIZE * 0.6);
        baitView.setFitHeight(TILE_SIZE * 0.6);
        baitView.setPreserveRatio(true);

        // Mapping BaitType
        String file = switch (bait.getBaitType()) {
        case COFFEE -> "Bait_Coffee.png";
        case FOOD -> "Bait_Food.png";
        case GEM -> "Bait_Gem.png";
        case TRAP -> "Bait_Trap.png";
        };

        String path = "/de/uni_koblenz/ptsd/foxtrot/mazeclient/gui/resources/images/" + file;

        try {
            baitView.setImage(new Image(this.getClass().getResource(path).toExternalForm()));
        } catch (Exception e) {
            System.err.println("Bait image could not load: " + path);
        }

        // Offset zum Zentrieren
        double offset = TILE_SIZE * (1 - 0.6) / 2;
        baitView.translateXProperty().bind(bait.xPositionProperty().multiply(TILE_SIZE).add(offset));
        baitView.translateYProperty().bind(bait.yPositionProperty().multiply(TILE_SIZE).add(offset));

        this.entityGroup.getChildren().add(baitView);
        this.baitNodes.put(id, baitView);

    }

    private void removeBaitNode(int id) {
        Node node = this.baitNodes.remove(id);
        if (node != null) {
            this.entityGroup.getChildren().remove(node);
        }
    }

    private void setupStrategyChoice() {
        this.strategyChoice.setItems(FXCollections.observableArrayList(StrategyMode.values()));
        this.strategyChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(StrategyMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatStrategyMode(item));
            }
        });
        this.strategyChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StrategyMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatStrategyMode(item));
            }
        });
        this.strategyChoice.valueProperty().addListener((obs, oldMode, newMode) -> this.logic.setStrategyMode(newMode));
        this.strategyChoice.getSelectionModel().select(this.logic.getStrategyMode());
    }

    private static String formatStrategyMode(StrategyMode mode) {
        return switch (mode) {
        case OFF -> "Off";
        case ASTAR -> "A*";
        case SMART -> "R*";
        };
    }

}

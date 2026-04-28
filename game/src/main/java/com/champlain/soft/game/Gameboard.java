package com.champlain.soft.game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gameboard extends Application {
    private static final int SCENE_WIDTH = 800;
    private static final int SCENE_HEIGHT = 800;
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int BOMB_COUNT = 8;
    private static final int INITIAL_LIVES = 5;
    private static final int PLAYER_START_ROW = 1;
    private static final int PLAYER_START_COL = 1;
    private static final double CELL_SIZE = 80;

    enum CellType {
        GRASS, PRINCESS, BOMB, WALL
    }

    private final CellType[][] matrix = new CellType[ROWS][COLS];
    private final Random random = new Random();

    private Image grassImage;
    private Image playerImage;
    private Image princessImage;
    private Image bombImage;
    private Image wallImage;
    private Image guardianImage;

    private AudioClip moveSound;
    private AudioClip wallSound;
    private AudioClip restartSound;
    private AudioClip winSound;
    private AudioClip loseSound;

    private Stage primaryStage;
    private GridPane grid;
    private Label topLabel;
    private Label overlayTitleLabel;
    private Label overlayMessageLabel;
    private ImageView overlayImageView;
    private StackPane overlayPane;

    private int playerRow = PLAYER_START_ROW;
    private int playerCol = PLAYER_START_COL;
    private int moveCount;
    private int lives;
    private boolean gameOver;
    private boolean alertOpen;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        loadImages();

        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        topLabel = new Label();
        topLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        topLabel.setAlignment(Pos.CENTER);
        topLabel.setMaxWidth(Double.MAX_VALUE);

        buildOverlay();
        initMatrix();
        drawBoard();
        updateTopLabel("Find the princess and avoid the bombs.");

        topLabel.setStyle("-fx-background-color: rgba(255,255,255,0.88); -fx-background-radius: 14; -fx-padding: 8 14 8 14;");

        StackPane sceneRoot = new StackPane(grid, topLabel, overlayPane);
        sceneRoot.setStyle("-fx-background-color: #f8fafc;");
        sceneRoot.setFocusTraversable(true);
        StackPane.setAlignment(topLabel, Pos.TOP_CENTER);
        StackPane.setMargin(topLabel, new Insets(6, 0, 0, 0));

        Scene scene = new Scene(sceneRoot, SCENE_WIDTH, SCENE_HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleKeyPress(event.getCode()));
        sceneRoot.setOnMouseClicked(event -> sceneRoot.requestFocus());

        stage.setTitle("Rescue the Princess");
        stage.setScene(scene);
        stage.show();
        sceneRoot.requestFocus();
    }

    private void initMatrix() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                boolean isPerimeter = row == 0 || col == 0 || row == ROWS - 1 || col == COLS - 1;
                matrix[row][col] = isPerimeter ? CellType.WALL : CellType.GRASS;
            }
        }

        playerRow = PLAYER_START_ROW;
        playerCol = PLAYER_START_COL;
        moveCount = 0;
        lives = INITIAL_LIVES;
        gameOver = false;
        alertOpen = false;
        hideOverlay();

        placeRandomObject(CellType.PRINCESS);
        for (int index = 0; index < BOMB_COUNT; index++) {
            placeRandomObject(CellType.BOMB);
        }
    }

    private void drawBoard() {
        grid.getChildren().clear();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);
                cell.setStyle("-fx-border-color: black;");

                cell.getChildren().add(createTile(grassImage));

                CellType cellType = matrix[row][col];
                if (cellType == CellType.WALL) {
                    cell.getChildren().add(createTile(wallImage));
                } else if (cellType == CellType.PRINCESS) {
                    cell.getChildren().add(createTile(princessImage));
                } else if (cellType == CellType.BOMB && gameOver) {
                    cell.getChildren().add(createTile(bombImage));
                }

                if (row == playerRow && col == playerCol) {
                    cell.getChildren().add(createTile(playerImage));
                }

                grid.add(cell, col, row);
            }
        }
    }

    private void handleKeyPress(KeyCode keyCode) {
        if (alertOpen) {
            return;
        }

        if (gameOver && keyCode == KeyCode.R) {
            restartGame();
            return;
        }

        if (gameOver) {
            return;
        }

        int nextRow = playerRow;
        int nextCol = playerCol;

        if (keyCode == KeyCode.UP || keyCode == KeyCode.W) {
            nextRow--;
        } else if (keyCode == KeyCode.DOWN || keyCode == KeyCode.S) {
            nextRow++;
        } else if (keyCode == KeyCode.LEFT || keyCode == KeyCode.A) {
            nextCol--;
        } else if (keyCode == KeyCode.RIGHT || keyCode == KeyCode.D) {
            nextCol++;
        } else if (keyCode == KeyCode.R) {
            restartGame();
            return;
        } else {
            return;
        }

        movePlayer(nextRow, nextCol);
    }

    private void movePlayer(int nextRow, int nextCol) {
        CellType destination = matrix[nextRow][nextCol];

        if (destination == CellType.WALL) {
            updateTopLabel("A wall is blocking the way.");
            playSound(wallSound);
            return;
        }

        playerRow = nextRow;
        playerCol = nextCol;
        moveCount++;

        if (destination == CellType.BOMB) {
            handleBombHit();
            return;
        }

        if (destination == CellType.PRINCESS) {
            gameOver = true;
            updateTopLabel("You win!");
            showOverlay("YOU WIN!", "The princess has been rescued.", princessImage, "#166534", "#dcfce7");
            playSound(winSound);
            drawBoard();
            return;
        }

        updateTopLabel("Keep going.");
        playSound(moveSound);
        drawBoard();
    }

    private void handleBombHit() {
        lives--;

        if (lives <= 0) {
            gameOver = true;
            updateTopLabel("Game over.");
            showOverlay("YOU LOSE", "You ran out of lives.", guardianImage, "#991b1b", "#fee2e2");
            playSound(loseSound);
        } else {
            updateTopLabel("You hit a bomb and lost a life.");
            playSound(loseSound);
            showBombAlert();
        }

        drawBoard();
    }

    private void restartGame() {
        initMatrix();
        updateTopLabel("Find the princess and avoid the bombs.");
        playSound(restartSound);
        drawBoard();
    }

    private void placeRandomObject(CellType type) {
        List<int[]> availableCells = new ArrayList<>();

        for (int row = 1; row < ROWS - 1; row++) {
            for (int col = 1; col < COLS - 1; col++) {
                if (row == PLAYER_START_ROW && col == PLAYER_START_COL) {
                    continue;
                }
                if (matrix[row][col] == CellType.GRASS) {
                    availableCells.add(new int[]{row, col});
                }
            }
        }

        int[] position = availableCells.get(random.nextInt(availableCells.size()));
        matrix[position[0]][position[1]] = type;
    }

    private void loadImages() {
        grassImage = loadImage("assets/grass.png");
        playerImage = loadImage("assets/player.png");
        princessImage = loadImage("assets/princess.png");
        bombImage = loadImage("assets/bomb.png");
        wallImage = loadImage("assets/wall.png");
        guardianImage = loadImage("assets/gardian.png");
        moveSound = loadSound("sounds/move.wav");
        wallSound = loadSound("sounds/wall.wav");
        restartSound = loadSound("sounds/restart.wav");
        winSound = loadSound("sounds/win.wav");
        loseSound = loadSound("sounds/lose.wav");
    }

    private Image loadImage(String name) {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalStateException("Missing image resource: " + name);
        }
        return new Image(stream);
    }

    private AudioClip loadSound(String name) {
        var resource = getClass().getResource(name);
        if (resource == null) {
            throw new IllegalStateException("Missing sound resource: " + name);
        }
        return new AudioClip(resource.toExternalForm());
    }

    private ImageView createTile(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(CELL_SIZE);
        imageView.setFitHeight(CELL_SIZE);
        imageView.setPreserveRatio(false);
        return imageView;
    }

    private void buildOverlay() {
        overlayTitleLabel = new Label();
        overlayTitleLabel.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 64));

        overlayMessageLabel = new Label("Press R to play again.");
        overlayMessageLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        overlayImageView = new ImageView();
        overlayImageView.setFitWidth(170);
        overlayImageView.setFitHeight(170);
        overlayImageView.setPreserveRatio(true);

        VBox overlayCard = new VBox(24, overlayImageView, overlayTitleLabel, overlayMessageLabel);
        overlayCard.setAlignment(Pos.CENTER);

        overlayPane = new StackPane(overlayCard);
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
    }

    private void showOverlay(String title, String message, Image image, String textColor, String backgroundColor) {
        overlayTitleLabel.setText(title);
        overlayTitleLabel.setStyle("-fx-text-fill: " + textColor + ";");
        overlayMessageLabel.setText(message + " Press R to play again.");
        overlayMessageLabel.setStyle("-fx-text-fill: " + textColor + ";");
        overlayImageView.setImage(image);
        overlayPane.setStyle("-fx-background-color: " + backgroundColor + ";");
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
    }

    private void hideOverlay() {
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
    }

    private void showBombAlert() {
        alertOpen = true;
        Alert alert = new Alert(Alert.AlertType.WARNING, "", ButtonType.OK);
        alert.initOwner(primaryStage);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Bomb Hit");
        alert.setHeaderText("You lost 1 life");
        alert.setContentText("Lives left: " + lives + ". Click OK to keep playing.");
        alert.showAndWait();
        alertOpen = false;
    }

    private void updateTopLabel(String message) {
        topLabel.setText("Lives: " + lives + "  |  Moves: " + moveCount + "  |  " + message + "  |  Arrow Keys / WASD");
    }

    private void playSound(AudioClip clip) {
        if (clip != null) {
            clip.play();
        }
    }
}

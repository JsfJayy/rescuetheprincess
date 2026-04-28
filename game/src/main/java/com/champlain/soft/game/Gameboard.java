package com.champlain.soft.game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media   .AudioClip;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
    private static final double CELL_SIZE = (double) SCENE_WIDTH / COLS;

    enum CellType {
        GRASS, PLAYER, PRINCESS, BOMB, WALL
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

    private GridPane grid;
    private Label statusLabel;
    private Label movesLabel;
    private Label bombsLabel;
    private Label livesLabel;
    private Label objectiveLabel;
    private Label overlayTitleLabel;
    private Label overlayMessageLabel;
    private ImageView statusIconView;
    private ImageView overlayImageView;
    private StackPane overlayPane;
    private int playerRow = PLAYER_START_ROW;
    private int playerCol = PLAYER_START_COL;
    private int moveCount;
    private int lives;
    private boolean gameOver;

    @Override
    public void start(Stage stage) {
        loadImages();
        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(0);
        grid.setVgap(0);

        statusLabel = new Label("Use arrow keys or WASD to rescue the princess.");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        statusLabel.setWrapText(true);

        objectiveLabel = new Label("Reach the princess, avoid the hidden bombs, and press R to restart after the game ends.");
        objectiveLabel.setWrapText(true);
        objectiveLabel.setStyle("-fx-text-fill: #334155;");

        movesLabel = createInfoLabel();
        bombsLabel = createInfoLabel();
        livesLabel = createInfoLabel();
        statusIconView = new ImageView();
        statusIconView.setFitWidth(84);
        statusIconView.setFitHeight(84);
        statusIconView.setPreserveRatio(true);
        buildOverlay();

        Button restartButton = new Button("Restart");
        restartButton.setFont(Font.font("System", FontWeight.BOLD, 16));
        restartButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 12;");
        restartButton.setOnAction(event -> restartGame());

        initMatrix();
        drawBoard(grid);
        updateInfoPanel(grassImage, "Quest in progress.");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #e0f2fe, #f8fafc);");
        root.setTop(buildHeader());
        root.setCenter(grid);
        root.setRight(buildSidebar(restartButton));

        StackPane sceneRoot = new StackPane(root, overlayPane);
        Scene scene = new Scene(sceneRoot, SCENE_WIDTH, SCENE_HEIGHT);
        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));

        stage.setTitle("Rescue the Princess");
        stage.setScene(scene);
        stage.show();
        grid.requestFocus();
    }

    private void initMatrix() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean isPerimeter = r == 0 || c == 0 || r == ROWS - 1 || c == COLS - 1;
                matrix[r][c] = isPerimeter ? CellType.WALL : CellType.GRASS;
            }
        }

        playerRow = PLAYER_START_ROW;
        playerCol = PLAYER_START_COL;
        moveCount = 0;
        lives = INITIAL_LIVES;
        gameOver = false;
        hideOverlay();

        placeRandomObject(CellType.PRINCESS);
        for (int index = 0; index < BOMB_COUNT; index++) {
            placeRandomObject(CellType.BOMB);
        }
    }

    private void drawBoard(GridPane grid) {
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
        } else {
            return;
        }

        movePlayer(nextRow, nextCol);
    }

    private void movePlayer(int nextRow, int nextCol) {
        CellType destination = matrix[nextRow][nextCol];

        if (destination == CellType.WALL) {
            statusLabel.setText("A wall is blocking the way.");
            updateInfoPanel(playerImage, "The perimeter walls cannot be crossed.");
            playSound(wallSound);
            return;
        }

        playerRow = nextRow;
        playerCol = nextCol;
        moveCount++;

        if (destination == CellType.BOMB) {
            handleBombHit();
            return;
        } else if (destination == CellType.PRINCESS) {
            gameOver = true;
            statusLabel.setText("You rescued the princess! Press R to play again.");
            updateInfoPanel(princessImage, "Mission complete. The princess is safe.");
            showOverlay("YOU WIN!", "The princess has been rescued. Press R or Restart to play again.", princessImage, "#14532d");
            playSound(winSound);
        } else {
            statusLabel.setText("Keep going. Find the princess and avoid the hidden bombs.");
            updateInfoPanel(playerImage, "Search the grid carefully. Bombs stay hidden until you lose.");
            playSound(moveSound);
        }

        drawBoard(grid);
    }

    private void restartGame() {
        initMatrix();
        statusLabel.setText("Use arrow keys or WASD to rescue the princess.");
        updateInfoPanel(grassImage, "Quest in progress.");
        playSound(restartSound);
        drawBoard(grid);
    }

    private void handleBombHit() {
        lives--;

        if (lives <= 0) {
            gameOver = true;
            statusLabel.setText("Boom! You lost your last life. Press R to restart.");
            updateInfoPanel(guardianImage, "All bombs are now revealed on the board.");
            showOverlay("GAME OVER", "You ran out of lives. Press R or Restart to try again.", guardianImage, "#7f1d1d");
            playSound(loseSound);
        } else {
            statusLabel.setText("Boom! You lost a life, but the game continues.");
            updateInfoPanel(guardianImage, "You still have " + lives + " lives left. Keep going and avoid the next bomb.");
            playSound(wallSound);
        }

        drawBoard(grid);
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

    private Label createInfoLabel() {
        Label label = new Label();
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        return label;
    }

    private VBox buildSidebar(Button restartButton) {
        Label panelTitle = new Label("Mission Details");
        panelTitle.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox legend = new VBox(
                10,
                createLegendRow(playerImage, "Player"),
                createLegendRow(princessImage, "Princess"),
                createLegendRow(bombImage, "Bomb"),
                createLegendRow(wallImage, "Wall"),
                createLegendRow(grassImage, "Safe ground")
        );

        VBox sidebar = new VBox(18, panelTitle, statusIconView, movesLabel, livesLabel, bombsLabel, objectiveLabel, legend, restartButton);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 22; -fx-border-color: #bfdbfe; -fx-border-radius: 22;");
        return sidebar;
    }

    private VBox buildHeader() {
        Label title = new Label("Rescue the Princess");
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 34));

        Label subtitle = new Label("JavaFX image grid project with movement, bombs, win condition, and game over.");
        subtitle.setFont(Font.font("System", 16));
        subtitle.setStyle("-fx-text-fill: #334155;");

        VBox header = new VBox(8, title, subtitle, statusLabel);
        header.setPadding(new Insets(0, 0, 20, 0));
        return header;
    }

    private void buildOverlay() {
        overlayTitleLabel = new Label();
        overlayTitleLabel.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 56));

        overlayMessageLabel = new Label();
        overlayMessageLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        overlayMessageLabel.setWrapText(true);
        overlayMessageLabel.setMaxWidth(420);
        overlayMessageLabel.setAlignment(Pos.CENTER);

        overlayImageView = new ImageView();
        overlayImageView.setFitWidth(150);
        overlayImageView.setFitHeight(150);
        overlayImageView.setPreserveRatio(true);

        VBox overlayCard = new VBox(22, overlayImageView, overlayTitleLabel, overlayMessageLabel);
        overlayCard.setAlignment(Pos.CENTER);
        overlayCard.setPadding(new Insets(36));
        overlayCard.setMaxWidth(560);
        overlayCard.setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 28; -fx-border-color: white; -fx-border-radius: 28;");

        overlayPane = new StackPane(overlayCard);
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setStyle("-fx-background-color: rgba(15, 23, 42, 0.72);");
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
    }

    private HBox createLegendRow(Image image, String text) {
        ImageView icon = new ImageView(image);
        icon.setFitWidth(30);
        icon.setFitHeight(30);
        icon.setPreserveRatio(false);

        Label label = new Label(text);
        label.setFont(Font.font("System", 15));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(12, icon, label, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateInfoPanel(Image statusImage, String details) {
        statusIconView.setImage(statusImage);
        movesLabel.setText("Moves: " + moveCount);
        livesLabel.setText("Lives: " + lives);
        bombsLabel.setText("Bombs hidden: " + BOMB_COUNT);
        objectiveLabel.setText(details);
    }

    private void showOverlay(String title, String message, Image image, String color) {
        overlayTitleLabel.setText(title);
        overlayTitleLabel.setStyle("-fx-text-fill: " + color + ";");
        overlayMessageLabel.setText(message);
        overlayImageView.setImage(image);
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
    }

    private void hideOverlay() {
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
    }

    private void playSound(AudioClip clip) {
        if (clip != null) {
            clip.play();
        }
    }
}

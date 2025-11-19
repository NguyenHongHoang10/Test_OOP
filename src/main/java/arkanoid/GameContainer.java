package arkanoid;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class GameContainer extends StackPane {

    private final Game game;
    private final Button pauseButton;
    private final Button settingButton;
    private final HBox buttonContainer;
    private final VBox pauseMenu;
    private final Button btnContinue;
    private final Button btnRestart;
    private final Button btnMenu;
    private final VBox levelCompleteMenu;
    private final Text scoreText;
    private final Button btnNextLevel;
    private final Button btnRestartLevel;
    private final Button btnHomeLevelComplete;
    private final VBox gameCompleteMenu;
    private final Text finalScoreText;
    private final Button btnRestartFinal;
    private final Button btnHomeFinal;
    private final VBox confirmQuitMenu;
    private final Button btnYes;
    private final Button btnNo;
    private final VBox gameOverMenu;
    private final Button btnRestartGameOver;
    private final Button btnMenuGameOver;
    private final SettingsMenu settingsMenu;

    public GameContainer(Game gameInstance) {
        this.game = gameInstance;

        // Tạo nút Setting
        settingButton = new Button("⚙");
        settingButton.setPrefSize(40, 40);
        settingButton.setFont(Font.font(16));
        // Ngăn nút "lấy" focus bàn phím khi được click
        settingButton.setFocusTraversable(false);
        // Gán hành động (gọi hàm public mới trong Game)
        settingButton.setOnAction(e -> game.openSettings());

        // Tạo nút Pause
        pauseButton = new Button("⏸");
        pauseButton.setPrefSize(40, 40);
        pauseButton.setFont(Font.font(16));
        pauseButton.setFocusTraversable(false);
        // Gán hành động (gọi hàm public mới trong Game)
        pauseButton.setOnAction(e -> game.togglePause());

        // Tạo HBox để chứa 2 nút
        buttonContainer = new HBox(5);
        buttonContainer.getChildren().addAll(settingButton, pauseButton);
        buttonContainer.setAlignment(Pos.TOP_RIGHT);

        // Tạo các nút trong menu
        btnContinue = createMenuButton("▶ Continue");
        btnRestart = createMenuButton("↺ Restart");
        btnMenu = createMenuButton("⌂ Menu");

        // Gán hành động cho các nút
        btnContinue.setOnAction(e -> game.resume());
        btnRestart.setOnAction(e -> game.restartCurrentLevel());
        btnMenu.setOnAction(e -> game.returnToMenu());

        // Tạo VBox chứa các nút;
        pauseMenu = new VBox(20);
        pauseMenu.getChildren().addAll(btnContinue, btnRestart, btnMenu);
        pauseMenu.setAlignment(Pos.CENTER);

        // Đặt nền mờ cho VBox
        pauseMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85),
                new CornerRadii(10),
                Insets.EMPTY
        )));

        // Đặt kích thước tối đa cho VBox
        pauseMenu.setMaxSize(300, 300);

        // Ẩn menu pause lúc đầu
        pauseMenu.setVisible(false);

        // Tiêu đề chúc mừng
        Text completeTitle = new Text("LEVEL COMPLETE!");
        completeTitle.setFont(Font.font(32));
        completeTitle.setFill(Color.LIGHTGREEN);

        // Text hiển thị điểm
        scoreText = new Text("Score: 0");
        scoreText.setFont(Font.font(20));
        scoreText.setFill(Color.WHITE);

        // Tạo các nút
        btnNextLevel = createMenuButton("▶ Next Level");
        btnRestartLevel = createMenuButton("↺ Restart");
        btnHomeLevelComplete = createMenuButton("⌂ Home");

        // Gán hành động
        btnNextLevel.setOnAction(e -> game.loadNextLevel());
        btnRestartLevel.setOnAction(e -> game.restartCurrentLevel());
        btnHomeLevelComplete.setOnAction(e -> game.returnToMenu());

        // Tạo VBox chứa các nút
        levelCompleteMenu = new VBox(20);
        levelCompleteMenu.getChildren().addAll(completeTitle, scoreText,
                btnNextLevel, btnRestartLevel, btnHomeLevelComplete);
        levelCompleteMenu.setAlignment(Pos.CENTER);
        levelCompleteMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        levelCompleteMenu.setMaxSize(300, 400);
        levelCompleteMenu.setVisible(false);

        // Xây dựng Game Complete Menu
        Text gameCompleteTitle = new Text("CONGRATULATIONS!");
        gameCompleteTitle.setFont(Font.font(32));
        gameCompleteTitle.setFill(Color.GOLD);

        Text gameCompleteSubtitle = new Text("You completed the game!");
        gameCompleteSubtitle.setFont(Font.font(20));
        gameCompleteSubtitle.setFill(Color.WHITE);

        finalScoreText = new Text("Final Score: 0");
        finalScoreText.setFont(Font.font(24));
        finalScoreText.setFill(Color.WHITE);

        // Nút Restart (chơi lại màn cuối) và Home
        btnRestartFinal = createMenuButton("↺ Restart Level 6");
        btnHomeFinal = createMenuButton("⌂ Home");

        btnRestartFinal.setOnAction(e -> game.restartCurrentLevel());
        btnHomeFinal.setOnAction(e -> game.returnToMenu());

        gameCompleteMenu = new VBox(20);
        gameCompleteMenu.getChildren().addAll(gameCompleteTitle,
                gameCompleteSubtitle, finalScoreText, btnRestartFinal, btnHomeFinal);
        gameCompleteMenu.setAlignment(Pos.CENTER);
        gameCompleteMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        gameCompleteMenu.setMaxSize(400, 400);
        gameCompleteMenu.setVisible(false);

        // Xây dựng Confirm Quit Menu
        Text confirmTitle = new Text("Do you want to quit?");
        confirmTitle.setFont(Font.font(24));
        confirmTitle.setFill(Color.WHITE);

        btnYes = new Button("Yes");
        btnYes.setFont(Font.font(20));
        btnYes.setPrefWidth(100);
        btnYes.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;");
        btnYes.setOnMouseEntered(e -> {
            btnYes.setScaleX(1.03);
            btnYes.setScaleY(1.03);
        });
        btnYes.setOnMouseExited(e -> {
            btnYes.setScaleX(1.0);
            btnYes.setScaleY(1.0);
        });
        btnYes.setFocusTraversable(false);
        btnYes.setOnAction(e -> Platform.exit());

        btnNo = new Button("No");
        btnNo.setFont(Font.font(20));
        btnNo.setPrefWidth(100);
        btnNo.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;");
        btnNo.setOnMouseEntered(e -> {
            btnNo.setScaleX(1.03);
            btnNo.setScaleY(1.03);
        });
        btnNo.setOnMouseExited(e -> {
            btnNo.setScaleX(1.0);
            btnNo.setScaleY(1.0);
        });
        btnNo.setFocusTraversable(false);
        btnNo.setOnAction(e -> game.cancelQuit());

        // HBox để chứa 2 nút Yes/No
        HBox yesNoBox = new HBox(20);
        yesNoBox.getChildren().addAll(btnYes, btnNo);
        yesNoBox.setAlignment(Pos.CENTER);

        confirmQuitMenu = new VBox(20);
        confirmQuitMenu.getChildren().addAll(confirmTitle, yesNoBox);
        confirmQuitMenu.setAlignment(Pos.CENTER);
        confirmQuitMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        confirmQuitMenu.setMaxSize(300, 200);
        confirmQuitMenu.setVisible(false);

        // Xây dựng Game Over Menu
        Text gameOverTitle = new Text("GAME OVER");
        gameOverTitle.setFont(Font.font(32));
        gameOverTitle.setFill(Color.RED);

        // Nút Restart và Menu
        btnRestartGameOver = createMenuButton("↺ Restart");
        btnMenuGameOver = createMenuButton("⌂ Menu");

        btnRestartGameOver.setOnAction(e -> game.restartCurrentLevel());
        btnMenuGameOver.setOnAction(e -> game.returnToMenu());

        gameOverMenu = new VBox(20);
        gameOverMenu.getChildren().addAll(gameOverTitle, btnRestartGameOver, btnMenuGameOver);
        gameOverMenu.setAlignment(Pos.CENTER);
        gameOverMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        gameOverMenu.setMaxSize(300, 300);
        gameOverMenu.setVisible(false);

        // Nút "Back" của menu này sẽ gọi hàm closeSettingsAndPause()
        settingsMenu = new SettingsMenu(() -> {
            game.closeSettingsAndPause();
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });

        // 4. Thêm Game (lớp dưới) và HBox (lớp trên) vào StackPane
        getChildren().addAll(game, pauseMenu, levelCompleteMenu,
                gameCompleteMenu, confirmQuitMenu, gameOverMenu,
                settingsMenu, buttonContainer);

        // 5. Căn chỉnh HBox ra góc trên bên phải của StackPane
        StackPane.setAlignment(buttonContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(buttonContainer, new Insets(10, 10, 0, 0));
        StackPane.setAlignment(pauseMenu, Pos.CENTER);
        StackPane.setAlignment(levelCompleteMenu, Pos.CENTER);
        StackPane.setAlignment(gameCompleteMenu, Pos.CENTER);
        StackPane.setAlignment(confirmQuitMenu, Pos.CENTER);
        StackPane.setAlignment(gameOverMenu, Pos.CENTER);
        StackPane.setAlignment(settingsMenu, Pos.CENTER);

        game.setOnPause(() -> {
            GameState state = game.getGameState();

            // Ẩn tất cả các menu trước
            pauseMenu.setVisible(false);
            levelCompleteMenu.setVisible(false);
            gameCompleteMenu.setVisible(false);
            confirmQuitMenu.setVisible(false);
            gameOverMenu.setVisible(false);
            settingsMenu.setVisible(false);

            // Kiểm tra theo thứ tự ưu tiên
            if (state.isGameComplete()) {
                finalScoreText.setText("Final Score: " + state.getScore());
                gameCompleteMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isLevelComplete()) {
                scoreText.setText("Score: " + state.getScore());
                levelCompleteMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isConfirmOverlay()) {
                confirmQuitMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isSettingsOverlay()) {
                settingsMenu.onShow();
                settingsMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isShowMessage()) {
                gameOverMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else {
                pauseMenu.setVisible(true);
                buttonContainer.setVisible(false);
            }
        });

        game.setOnResume(() -> {
            pauseMenu.setVisible(false);
            levelCompleteMenu.setVisible(false);
            gameCompleteMenu.setVisible(false);
            confirmQuitMenu.setVisible(false);
            gameOverMenu.setVisible(false);
            settingsMenu.setVisible(false);
            buttonContainer.setVisible(true);
        });
    }

    // Hàm tiện ích để tạo các nút trong menu cho đồng bộ
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(240);
        btn.setFont(Font.font(24));
        btn.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;");
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.03);
            btn.setScaleY(1.03);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        btn.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.45)));
        btn.setFocusTraversable(false);
        return btn;
    }

}

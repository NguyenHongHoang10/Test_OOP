package arkanoid;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.text.Text;


public class GameContainer extends StackPane {

    private final Game game;
    private final Button pauseButton;
    private final Button settingButton;
    private final HBox buttonContainer; // Hộp chứa 2 nút
    private final VBox pauseMenu;
    private final Button btnContinue;
    private final Button btnRestart;
    private final Button btnMenu;
    private final VBox levelCompleteMenu;
    private final Text scoreText; // Text để hiển thị điểm
    private final Button btnNextLevel;
    private final Button btnRestartLevel;
    private final Button btnHomeLevelComplete; // Nút "Home" (Menu)
    private final VBox gameCompleteMenu;
    private final Text finalScoreText; // Text điểm cuối cùng
    private final Button btnRestartFinal; // Nút Restart (chơi lại màn 6)
    private final Button btnHomeFinal;    // Nút Home (về menu)
    private final VBox confirmQuitMenu;
    private final Button btnYes;
    private final Button btnNo;
    private final VBox gameOverMenu;
    private final Button btnRestartGameOver;
    private final Button btnMenuGameOver;
    private final SettingsMenu settingsMenu;

    public GameContainer(Game gameInstance) {
        this.game = gameInstance;

        // 1. Tạo nút Setting (bên trái)
        settingButton = new Button("⚙"); // Ký tự Setting
        settingButton.setPrefSize(40, 40);
        settingButton.setFont(Font.font(16));
        // QUAN TRỌNG: Ngăn nút "lấy" focus bàn phím khi được click
        settingButton.setFocusTraversable(false);
        // Gán hành động (gọi hàm public mới trong Game)
        settingButton.setOnAction(e -> game.openSettings());

        // 2. Tạo nút Pause (bên phải)
        pauseButton = new Button("⏸"); // Ký tự Pause
        pauseButton.setPrefSize(40, 40);
        pauseButton.setFont(Font.font(16));
        pauseButton.setFocusTraversable(false);
        // Gán hành động (gọi hàm public mới trong Game)
        pauseButton.setOnAction(e -> game.togglePause());

        // 3. Tạo HBox để chứa 2 nút
        buttonContainer = new HBox(5); // Khoảng cách 5px
        buttonContainer.getChildren().addAll(settingButton, pauseButton);
        buttonContainer.setAlignment(Pos.TOP_RIGHT); // Căn nội dung trong HBox (không cần thiết lắm)

        // 2a. Tạo các nút trong menu
        btnContinue = createMenuButton("▶ Continue");
        btnRestart = createMenuButton("↺ Restart");
        btnMenu = createMenuButton("⌂ Menu");

        // 2b. Gán hành động cho các nút
        btnContinue.setOnAction(e -> game.resume()); // Gọi resume (sẽ trigger onResumeCallback)
        btnRestart.setOnAction(e -> game.restartCurrentLevel());
        btnMenu.setOnAction(e -> game.returnToMenu());

        // 2c. Tạo VBox chứa các nút
        pauseMenu = new VBox(20); // Khoảng cách 20px
        pauseMenu.getChildren().addAll(btnContinue, btnRestart, btnMenu);
        pauseMenu.setAlignment(Pos.CENTER);

        // 2d. Đặt nền mờ cho VBox
        pauseMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), // Nền mờ (alpha 0.85)
                new CornerRadii(10), // Bo góc
                Insets.EMPTY
        )));

        // 2e. Đặt kích thước tối đa cho VBox
        pauseMenu.setMaxSize(300, 300);

        // 2f. Ẩn menu pause lúc đầu
        pauseMenu.setVisible(false);

        // 3a. Tiêu đề chúc mừng
        Text completeTitle = new Text("LEVEL COMPLETE!");
        completeTitle.setFont(Font.font(32));
        completeTitle.setFill(Color.LIGHTGREEN);

        // 3b. Text hiển thị điểm
        scoreText = new Text("Score: 0"); // Sẽ được cập nhật
        scoreText.setFont(Font.font(20));
        scoreText.setFill(Color.WHITE);

        // 3c. Tạo các nút
        btnNextLevel = createMenuButton("▶ Next Level");
        btnRestartLevel = createMenuButton("↺ Restart");
        btnHomeLevelComplete = createMenuButton("⌂ Home"); // "Home" (quay về menu chính)

        // 3d. Gán hành động
        btnNextLevel.setOnAction(e -> game.loadNextLevel());
        btnRestartLevel.setOnAction(e -> game.restartCurrentLevel());
        btnHomeLevelComplete.setOnAction(e -> game.returnToMenu());

        // 3e. Tạo VBox chứa các nút
        levelCompleteMenu = new VBox(20);
        levelCompleteMenu.getChildren().addAll(completeTitle, scoreText, btnNextLevel, btnRestartLevel, btnHomeLevelComplete);
        levelCompleteMenu.setAlignment(Pos.CENTER);
        levelCompleteMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        levelCompleteMenu.setMaxSize(300, 400); // Rộng hơn 1 chút cho tiêu đề
        levelCompleteMenu.setVisible(false); // Ẩn lúc đầu

        //Xây dựng Game Complete Menu
        Text gameCompleteTitle = new Text("CONGRATULATIONS!");
        gameCompleteTitle.setFont(Font.font(32));
        gameCompleteTitle.setFill(Color.GOLD); // Màu vàng

        Text gameCompleteSubtitle = new Text("You completed the game!");
        gameCompleteSubtitle.setFont(Font.font(20));
        gameCompleteSubtitle.setFill(Color.WHITE);

        finalScoreText = new Text("Final Score: 0");
        finalScoreText.setFont(Font.font(24));
        finalScoreText.setFill(Color.WHITE);

        // Nút Restart (chơi lại màn cuối) và Home
        btnRestartFinal = createMenuButton("↺ Restart Level 6");
        btnHomeFinal = createMenuButton("⌂ Home");

        btnRestartFinal.setOnAction(e -> game.restartCurrentLevel()); // Vẫn dùng hàm restart cũ
        btnHomeFinal.setOnAction(e -> game.returnToMenu());

        gameCompleteMenu = new VBox(20);
        gameCompleteMenu.getChildren().addAll(gameCompleteTitle, gameCompleteSubtitle, finalScoreText, btnRestartFinal, btnHomeFinal);
        gameCompleteMenu.setAlignment(Pos.CENTER);
        gameCompleteMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        gameCompleteMenu.setMaxSize(400, 400);
        gameCompleteMenu.setVisible(false); // Ẩn lúc đầu

        //Xây dựng Confirm Quit Menu
        Text confirmTitle = new Text("Do you want to quit?");
        confirmTitle.setFont(Font.font(24));
        confirmTitle.setFill(Color.WHITE);

        btnYes = new Button("Yes");
        btnYes.setFont(Font.font(20));
        btnYes.setPrefWidth(100);
        btnYes.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;" );
        btnYes.setOnMouseEntered(e -> {
            btnYes.setScaleX(1.03);
            btnYes.setScaleY(1.03);
        });
        btnYes.setOnMouseExited(e -> {
            btnYes.setScaleX(1.0);
            btnYes.setScaleY(1.0);
        });
        btnYes.setFocusTraversable(false);
        btnYes.setOnAction(e -> Platform.exit()); // Tắt game

        btnNo = new Button("No");
        btnNo.setFont(Font.font(20));
        btnNo.setPrefWidth(100);
        btnNo.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;" );
        btnNo.setOnMouseEntered(e -> {
            btnNo.setScaleX(1.03);
            btnNo.setScaleY(1.03);
        });
        btnNo.setOnMouseExited(e -> {
            btnNo.setScaleX(1.0);
            btnNo.setScaleY(1.0);
        });
        btnNo.setFocusTraversable(false);
        btnNo.setOnAction(e -> game.cancelQuit()); // Gọi hàm mới trong Game.java

        // HBox để chứa 2 nút Yes/No
        HBox yesNoBox = new HBox(20); // Khoảng cách 20px
        yesNoBox.getChildren().addAll(btnYes, btnNo);
        yesNoBox.setAlignment(Pos.CENTER);

        confirmQuitMenu = new VBox(20);
        confirmQuitMenu.getChildren().addAll(confirmTitle, yesNoBox);
        confirmQuitMenu.setAlignment(Pos.CENTER);
        confirmQuitMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), new CornerRadii(10), Insets.EMPTY
        )));
        confirmQuitMenu.setMaxSize(300, 200);
        confirmQuitMenu.setVisible(false); // Ẩn lúc đầu

        // Xây dựng Game Over Menu ===
        Text gameOverTitle = new Text("GAME OVER");
        gameOverTitle.setFont(Font.font(32));
        gameOverTitle.setFill(Color.RED); // Màu đỏ

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
        gameOverMenu.setVisible(false); // Ẩn lúc đầu

        // Nút "Back" của menu này sẽ gọi hàm closeSettingsAndPause()
        settingsMenu = new SettingsMenu(() -> {
            game.closeSettingsAndPause();
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });

        // 4. Thêm Game (lớp dưới) và HBox (lớp trên) vào StackPane
        getChildren().addAll(game, pauseMenu, levelCompleteMenu,
                gameCompleteMenu,confirmQuitMenu,gameOverMenu,settingsMenu, buttonContainer);

        // 5. Căn chỉnh HBox ra góc trên bên phải của StackPane
        StackPane.setAlignment(buttonContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(buttonContainer, new Insets(10, 10, 0, 0)); // Cách lề 10px
        StackPane.setAlignment(pauseMenu, Pos.CENTER); // Căn VBox ra giữa
        StackPane.setAlignment(levelCompleteMenu, Pos.CENTER); // Căn giữa
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
            } else if (state.isConfirmOverlay()) { // KIỂM TRA MỚI
                confirmQuitMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isSettingsOverlay()) {
                settingsMenu.onShow(); // Cập nhật nút On/Off
                settingsMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else if (state.isShowMessage()) {
                gameOverMenu.setVisible(true);
                buttonContainer.setVisible(false);
            } else {
                // Pause thường
                pauseMenu.setVisible(true);
                buttonContainer.setVisible(false);
            }
        });

        game.setOnResume(() -> {
            pauseMenu.setVisible(false);
            levelCompleteMenu.setVisible(false); // Ẩn cả menu qua màn
            gameCompleteMenu.setVisible(false);
            confirmQuitMenu.setVisible(false);
            gameOverMenu.setVisible(false);
            settingsMenu.setVisible(false);
            buttonContainer.setVisible(true); // Hiện lại nút '⚙' và '⏸'
        });
    }

    /**
     * Hàm tiện ích để tạo các nút trong menu cho đồng bộ
     */
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(240);
        btn.setFont(Font.font(24));
        btn.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;" );
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.03);
            btn.setScaleY(1.03);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        // subtle shadow
        btn.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.45)));
        btn.setFocusTraversable(false); // Ngăn không cho nút chiếm focus
        return btn;
    }

}

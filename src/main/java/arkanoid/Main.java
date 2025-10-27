package arkanoid;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        double width = 800, height = 600;

        // Holder để tham chiếu scene trong callback Game (do lambda cần biến final/effectively final)
        class MenuSceneHolder { Scene scene; }
        class LevelSceneHolder { Scene scene; }
        MenuSceneHolder menuSceneHolder = new MenuSceneHolder();
        LevelSceneHolder levelSceneHolder = new LevelSceneHolder();

        // Khai báo biến HighScore service
        HighScoreService highScoreService = HighScoreService.get();

        // Tạo Game trước nhưng không bắt đầu trận
        Game game = new Game(
                width,
                height,
                () -> Platform.runLater(() -> stage.setScene(menuSceneHolder.scene)),   // callback về menu
                () -> Platform.runLater(() -> stage.setScene(levelSceneHolder.scene))   // callback về màn chọn level
        );

        GameContainer gameContainer = new GameContainer(game);
        Scene gameScene = new Scene(gameContainer, width, height);


        // Dùng constructor mới: truyền continueSavedCallback
        MenuPane menuPane = new MenuPane(
                game,
                // Khi nhấn "Start Game" -> Mở chọn Level
                () -> {
                    LevelSelectPane levelSelectPane = new LevelSelectPane(
                            () -> { // Chọn Level 1
                                game.startNewGame();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 2
                                game.startLevel2();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 3
                                game.startLevel3();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 4
                                game.startLevel4();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 5
                                game.startLevel5();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> {
                                game.startLevel6(); // chọn Level 6
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> stage.setScene(menuSceneHolder.scene) // Nút "Trở lại"
                    );
                    stage.setScene(new Scene(levelSelectPane, width, height));
                },
                // Continue callback
                () -> {
                    if (game.isGameStarted()) game.pause();
                    stage.setScene(gameScene);
                    game.requestFocus();
                },
                // Continue (chưa có session): load từ file và vào game chờ SPACE (không overlay)
                () -> {
                    stage.setScene(gameScene);
                    boolean ok = SaveLoad.get().loadIntoAndPrepareContinue(game);
                    if (!ok) {
                        // Không có file hợp lệ → quay lại menu (tuỳ chọn)
                        stage.setScene(menuSceneHolder.scene);
                    }
                },
                // LeaderBoard
                () -> {
                    Scene lb = HighScoreUI.createLeaderboardScene(width, height, highScoreService,
                            () -> stage.setScene(menuSceneHolder.scene));
                    stage.setScene(lb);
                },
                // Exit: Lưu rồi thoát
                () -> {
                    SaveLoad.get().save(game);
                    Platform.exit();
                }
        );

        Scene menuScene = new Scene(menuPane, width, height);
        menuSceneHolder.scene = menuScene;

        // Hiển thị menu lúc bắt đầu
        stage.setTitle("Arkanoid - JavaFX OOP");
        stage.setScene(menuScene);
        stage.setResizable(false);

        // Lưu khi đóng cửa sổ
        stage.setOnCloseRequest(e -> {
            try { SaveLoad.get().save(game); } catch (Exception ignored) {}
        });

        // Khi thắng Level 6 (gameComplete) hoặc Game Over, nếu lọt Top 10 thì hiện overlay nhập tên
        final boolean[] promptedThisRun = { false };
        Timeline watcher = new Timeline(new KeyFrame(Duration.millis(250), ev -> {
            GameState s = game.getGameState();
            boolean ended = s.isGameComplete() || (s.getLives() <= 0 && s.isShowMessage());
            if (!promptedThisRun[0] && ended) {
                int score = s.getScore();
                boolean shown = HighScoreUI.promptNameIfQualified(
                        gameContainer, score, highScoreService,
                        () -> game.pause() // lưu xong thì hiện Restart/Menu
                );
                // Dù có hiển thị hay không, ghi nhận đã xử lý để không lặp
                promptedThisRun[0] = true;
            }
            // Reset cờ khi ván mới thực sự chạy
            if (s.isRunning() && s.getLives() > 0 && !s.isLevelComplete() && !s.isGameComplete() && !s.isShowMessage()) {
                promptedThisRun[0] = false;
            }
        }));
        watcher.setCycleCount(Timeline.INDEFINITE);
        watcher.play();

        stage.show();
        game.requestFocus(); // để nhận input
    }

    public static void main(String[] args) {
        launch(args);
    }
}

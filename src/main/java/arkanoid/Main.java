package arkanoid;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        double width = 800, height = 600;

        // Holder để tham chiếu scene trong callback Game
        class MenuSceneHolder {
            Scene scene;
        }
        class LevelSceneHolder {
            Scene scene;
        }
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

        // Tạo StackPane làm root cho menu, chứa MenuPane và SettingsMenu
        StackPane menuRoot = new StackPane();
        Scene menuScene = new Scene(menuRoot, width, height);
        menuSceneHolder.scene = menuScene;

        // Tạo SettingsMenu
        SettingsMenu settingsMenu = new SettingsMenu(() -> {
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });
        settingsMenu.setVisible(false);
        StackPane.setAlignment(settingsMenu, Pos.CENTER);

        // Tạo Scene "How to play"
        HowToPlayScene howToPlayScene = new HowToPlayScene(width, height,
                () -> stage.setScene(menuSceneHolder.scene)
        );

        // Dùng constructor mới để truyền continueSavedCallback
        MenuPane menuPane = new MenuPane(
                game,
                // Khi nhấn "Start Game" thì Mở chọn Level
                () -> {
                    LevelSelectPane levelSelectPane = new LevelSelectPane(
                            () -> { // Chọn Level 1
                                game.resetScore();
                                game.resetLives();
                                game.startNewGame(0);
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 2
                                game.resetScore();
                                game.resetLives();
                                game.startNewGame(1);
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 3
                                game.resetScore();
                                game.resetLives();
                                game.startNewGame(2);
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 4
                                game.resetScore();
                                game.resetLives();
                                game.startNewGame(3);
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // Chọn Level 5
                                game.resetScore();
                                game.resetLives();
                                game.startNewGame(4);
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> { // chọn Level 6
                                game.resetScore();
                                game.resetLives();
                                game.startLevel6();
                                stage.setScene(gameScene);
                                game.requestFocus();
                            },
                            () -> stage.setScene(menuSceneHolder.scene) // Nút Back
                    );
                    stage.setScene(new Scene(levelSelectPane, width, height));
                },
                // Continue callback
                () -> {
                    SoundManager.get().stopBgm();
                    if (game.isGameStarted()) game.pause();
                    stage.setScene(gameScene);
                    game.requestFocus();
                    int idx = game.getGameState().getCurrentLevelIndex();
                    SoundManager.get().startBgm(idx == 5 ? SoundManager.Bgm.BOSS : SoundManager.Bgm.LEVEL);
                },
                // Continue load từ file
                () -> {
                    SoundManager.get().stopBgm();
                    stage.setScene(gameScene);
                    boolean ok = SaveLoad.get().loadIntoAndPrepareContinue(game);
                    if (!ok) {
                        // Không có file hợp lệ thì quay lại menu
                        stage.setScene(menuSceneHolder.scene);
                    } else {
                        int idx = game.getGameState().getCurrentLevelIndex();
                        SoundManager.get().startBgm(idx == 5 ? SoundManager.Bgm.BOSS : SoundManager.Bgm.LEVEL);
                    }
                },
                // LeaderBoard
                () -> {
                    Scene lb = HighScoreUI.createLeaderboardScene(width, height, highScoreService,
                            () -> stage.setScene(menuSceneHolder.scene));
                    stage.setScene(lb);
                },
                () -> stage.setScene(howToPlayScene.getScene()),
                () -> {
                    settingsMenu.onShow();
                    settingsMenu.setVisible(true);
                },
                // Exit thì lưu rồi thoát
                () -> {
                    SaveLoad.get().save(game);
                    Platform.exit();
                }
        );


        menuRoot.getChildren().addAll(menuPane, settingsMenu);

        // Tạo màn StoryScene, kết thúc sẽ về menu
        StoryScene storyScene = new StoryScene(width, height, () -> {
            stage.setScene(menuScene);
            SoundManager.get().stopBgm();
            SoundManager.get().startBgm(SoundManager.Bgm.MENU);
        });
        // Tạo IntroScene, kết thúc sẽ chuyển sang StoryScene và chạy StoryScene
        IntroScene introScene = new IntroScene(width, height, () -> {
            stage.setScene(storyScene.getScene());
            storyScene.play(); // Bắt đầu cuộn chữ

        });

        // Hiển thị menu lúc bắt đầu
        stage.setTitle("Arkanoid - JavaFX OOP");
        stage.setScene(introScene.getScene()); // Bắt đầu bằng Intro
        stage.setResizable(false);
        SoundManager.get().startBgm(SoundManager.Bgm.INTRO);


        // Lưu khi đóng cửa sổ
        stage.setOnCloseRequest(e -> {
            try {
                SaveLoad.get().save(game);
            } catch (Exception ignored) {
            }
        });

        // Khi thắng Level 6 (gameComplete) hoặc Game Over, nếu lọt Top 10 thì hiện overlay nhập tên
        final boolean[] promptedThisRun = {false};
        Timeline watcher = new Timeline(new KeyFrame(Duration.millis(250), ev -> {
            GameState s = game.getGameState();
            boolean ended = s.isGameComplete() || (s.getLives() <= 0 && s.isShowMessage());
            if (!promptedThisRun[0] && ended) {
                int score = s.getScore();
                boolean shown = HighScoreUI.promptNameIfQualified(
                        gameContainer, score, highScoreService,
                        () -> game.pause()
                );
                // Ghi nhận đã xử lý để không lặp
                promptedThisRun[0] = true;
            }
            // Reset cờ khi ván mới thực sự chạy
            if (s.isRunning() && s.getLives() > 0 && !s.isLevelComplete()
                    && !s.isGameComplete() && !s.isShowMessage()) {
                promptedThisRun[0] = false;
            }
        }));
        watcher.setCycleCount(Timeline.INDEFINITE);
        watcher.play();

        stage.show();
        introScene.play();
        game.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

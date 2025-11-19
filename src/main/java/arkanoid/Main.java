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
import arkanoid.SoundManager;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        double width = 800, height = 600;

        // Holder để tham chiếu scene trong callback Game (do lambda cần biến final/effectively final)
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

        // (Phải tạo menuScene trước, vì HowToPlayScene cần nó)
        //Scene menuScene = new Scene(new StackPane(), width, height); // Placeholder
        //menuSceneHolder.scene = menuScene;

        // 1. Tạo StackPane làm root cho menu
        //    Nó sẽ chứa MenuPane và SettingsMenu
        StackPane menuRoot = new StackPane();
        Scene menuScene = new Scene(menuRoot, width, height); // Đặt menuRoot làm gốc
        menuSceneHolder.scene = menuScene;

        // 2. Tạo SettingsMenu (overlay)
        //    Nút "Back" của nó sẽ chỉ ẩn chính nó
        SettingsMenu settingsMenu = new SettingsMenu(() -> {
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });
        settingsMenu.setVisible(false); // Ẩn lúc đầu
        StackPane.setAlignment(settingsMenu, Pos.CENTER);

        // 1. Tạo Scene "How to play" (MỚI)
        // (Sử dụng File 1, HowToPlayScene.java, mà bạn đã có)
        HowToPlayScene howToPlayScene = new HowToPlayScene(width, height,
                () -> stage.setScene(menuSceneHolder.scene) // Callback: quay về menu
        );

        // Dùng constructor mới: truyền continueSavedCallback
        MenuPane menuPane = new MenuPane(
                game,
                // Khi nhấn "Start Game" -> Mở chọn Level
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
                            () -> {
                                game.resetScore();
                                game.resetLives();
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
                    SoundManager.get().stopBgm();
                    if (game.isGameStarted()) game.pause();
                    stage.setScene(gameScene);
                    game.requestFocus();
                    int idx = game.getGameState().getCurrentLevelIndex(); // 0..5
                    SoundManager.get().startBgm(idx == 5 ? SoundManager.Bgm.BOSS : SoundManager.Bgm.LEVEL);
                },
                // Continue (chưa có session): load từ file và vào game chờ SPACE (không overlay)
                () -> {
                    SoundManager.get().stopBgm();
                    stage.setScene(gameScene);
                    boolean ok = SaveLoad.get().loadIntoAndPrepareContinue(game);
                    if (!ok) {
                        // Không có file hợp lệ → quay lại menu (tuỳ chọn)
                        stage.setScene(menuSceneHolder.scene);
                    } else {
                        int idx = game.getGameState().getCurrentLevelIndex(); // 0..5
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
                    settingsMenu.onShow(); // Cập nhật nút On/Off
                    settingsMenu.setVisible(true);
                },
                // Exit: Lưu rồi thoát
                () -> {
                    SaveLoad.get().save(game);
                    Platform.exit();
                }
        );

//        Scene menuScene = new Scene(menuPane, width, height);
//        menuSceneHolder.scene = menuScene;
        //menuScene.setRoot(menuPane);
        menuRoot.getChildren().addAll(menuPane, settingsMenu);

        // 1. Tạo StoryScene (Màn 2)
        // Khi StoryScene kết thúc, nó sẽ gọi "chuyển sang menuScene"
        StoryScene storyScene = new StoryScene(width, height, () -> {
            stage.setScene(menuScene);
            SoundManager.get().stopBgm();
            SoundManager.get().startBgm(SoundManager.Bgm.MENU);
        });
        // 2. Tạo IntroScene (Màn 1)
        // Khi IntroScene kết thúc, nó sẽ gọi "chuyển sang storyScene" VÀ "chạy storyScene"
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
        introScene.play(); // Bảo class mới tự chạy animation
        game.requestFocus(); // để nhận input
    }

    public static void main(String[] args) {
        launch(args);
    }
}

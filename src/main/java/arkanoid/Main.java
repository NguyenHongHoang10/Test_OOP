package arkanoid;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        double width = 800, height = 600;

        // Holder để tham chiếu scene trong callback Game (do lambda cần biến final/effectively final)
        class MenuSceneHolder { Scene scene; }
        class LevelSceneHolder { Scene scene; }
        MenuSceneHolder menuSceneHolder = new MenuSceneHolder();
        LevelSceneHolder levelSceneHolder = new LevelSceneHolder();

        // Tạo Game trước nhưng không bắt đầu trận
        Game game = new Game(
                width,
                height,
                () -> Platform.runLater(() -> stage.setScene(menuSceneHolder.scene)),   // callback về menu
                () -> Platform.runLater(() -> stage.setScene(levelSceneHolder.scene))   // callback về màn chọn level
        );

        GameContainer gameContainer = new GameContainer(game);

        Scene gameScene = new Scene(gameContainer, width, height);

        // Tạo MenuPane riêng và truyền các callback
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
                // Exit callback
                () -> Platform.exit()
        );

        Scene menuScene = new Scene(menuPane, width, height);
        menuSceneHolder.scene = menuScene;

        // Hiển thị menu lúc bắt đầu
        stage.setTitle("Arkanoid - JavaFX OOP");
        stage.setScene(menuScene);
        stage.setResizable(false);
        stage.show();
        game.requestFocus(); // để nhận input
    }

    public static void main(String[] args) {
        launch(args);
    }
}

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
        MenuSceneHolder menuSceneHolder = new MenuSceneHolder();

        // Tạo Game trước nhưng không bắt đầu trận
        Game game = new Game(width, height, () -> {
        // Callback trả về menu -- chạy trên JavaFX thread
            Platform.runLater(() -> stage.setScene(menuSceneHolder.scene));
        });
        Scene gameScene = new Scene(game, width, height);

        // Tạo MenuPane riêng và truyền các callback
        MenuPane menuPane = new MenuPane(
                // Start callback
                () -> {
                    game.startNewGame();
                    stage.setScene(gameScene);
                    game.requestFocus();
                },
                // Continue callback
                () -> {
                    if (game.isGameStarted()) game.resume();
                    else game.startNewGame();
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

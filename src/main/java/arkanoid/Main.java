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

        // Thêm khởi tạo Save/Load + HighScore
        SaveManager saveManager = new SaveManager();              // Quản lý lưu/tải
        HighScoreManager highScoreManager = new HighScoreManager(); // Bảng xếp hạng Top 10

        // Tạo Game trước nhưng không bắt đầu trận
        // Callback trả về menu -- chạy trên JavaFX thread
        // Dùng constructor mở rộng có Save/HighScore
        Game game = new Game(width, height, () -> {
            Platform.runLater(() -> stage.setScene(menuSceneHolder.scene));
        }, saveManager, highScoreManager);
        Scene gameScene = new Scene(game, width, height);

        // Tạo MenuPane (bản mới có nhập tên)
        MenuPane menuPane = new MenuPane(
                // Start callback có tên người chơi
                (playerName) -> {
                    game.startNewGame(playerName); // Truyền tên vào GameState
                    stage.setScene(gameScene);
                    game.requestFocus();
                },
                // Continue callback: load từ save nếu có, nếu không sẽ startNewGame như cũ
                () -> {
                    if (saveManager.hasSave()) {
                        boolean ok = game.loadFromSave();
                        if (!ok) {
                            // nếu load lỗi, fallback sang ván mới
                            game.startNewGame("Player");
                        }
                    } else {
                        if (game.isGameStarted()) game.resume();
                        else game.startNewGame("Player");
                    }
                    stage.setScene(gameScene);
                    game.requestFocus();
                },
                // Exit callback
                Platform::exit
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

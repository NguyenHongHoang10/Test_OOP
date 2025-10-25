package arkanoid;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class MenuPane extends VBox {
    private final Game game;

    private final Button startBtn;
    private final Button continueBtn;
    private final Button exitBtn;
    private final Button introductionBtn;
    private final Button leaderboardBtn;
    private final Button settingBtn;

    private final Text continueErrorText;


    public MenuPane(Game game, Runnable startCallback, Runnable continueCallback, Runnable exitCallback) {
        super(12);
        this.game = game;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        setPrefSize(800, 600);

        Text title = new Text("ARKANOID");
        title.setFill(Color.BLACK);
        title.setFont(Font.font(48));

        startBtn = new Button("▶ Start New Game");
        startBtn.setFont(Font.font(24));
        startBtn.setPrefWidth(240);
        startBtn.setOnAction(e -> startCallback.run());

        continueBtn = new Button("Continue");
        continueBtn.setFont(Font.font(24));
        continueBtn.setPrefWidth(240);
        continueBtn.setOnAction(e -> {
            if (canContinue()) {
                continueCallback.run(); // Chạy hành động như cũ
            } else {
                showContinueError(); // Hiển thị lỗi
            }
    });

        // Nút Introduction
        introductionBtn = new Button("ℹ Introduction");
        introductionBtn.setFont(Font.font(24));
        introductionBtn.setPrefWidth(240);
        introductionBtn.setOnAction(e -> {
            // Hiện tại chưa làm gì
            System.out.println("Introduction clicked - Not implemented");
        });

        // Nút Leaderboard
        leaderboardBtn = new Button("🏆 Leaderboard");
        leaderboardBtn.setFont(Font.font(24));
        leaderboardBtn.setPrefWidth(240);
        leaderboardBtn.setOnAction(e -> {
            // Hiện tại chưa làm gì
            System.out.println("Leaderboard clicked - Not implemented");
        });

        // Nút Setting
        settingBtn = new Button("⚙ Setting");
        settingBtn.setFont(Font.font(24));
        settingBtn.setPrefWidth(240);
        settingBtn.setOnAction(e -> {
            // Hiện tại chưa làm gì
            System.out.println("Setting clicked - Not implemented");
        });

        exitBtn = new Button("❌ Exit");
        exitBtn.setFont(Font.font(24));
        exitBtn.setPrefWidth(240);
        exitBtn.setOnAction(e -> exitCallback.run());

        continueErrorText = new Text("Nothing to continue");
        continueErrorText.setFont(Font.font(16));
        continueErrorText.setFill(Color.ORANGERED);
        continueErrorText.setVisible(false); // Ẩn lúc đầu

        getChildren().addAll(title, startBtn, continueBtn,introductionBtn,
                leaderboardBtn, settingBtn, exitBtn, continueErrorText);
    }

        /**
         * Kiểm tra trạng thái của Game để quyết định có thể "Continue" hay không.
         */
        private boolean canContinue() {
            // 1. Nếu game chưa bao giờ bắt đầu -> không thể
            if (!game.isGameStarted()) {
                return false;
            }

            GameState state = game.getGameState();

            // 2. Nếu đang ở màn hình "Game Over" (showMessage) hoặc "Level Complete" (levelComplete)
            // (Chúng ta dùng 'isShowMessage' vì nó được set=true khi Game Over hoặc Win màn cuối)
            if (state.isShowMessage() || state.isLevelComplete()) {
                return false;
            }

            // 3. Nếu game đã bắt đầu VÀ không ở trạng thái kết thúc -> có thể
            return true;
        }

        /**
         * Hiển thị thông báo lỗi "Nothing to continue" trong 3 giây.
         */
        private void showContinueError() {
            continueErrorText.setVisible(true);
            // Tạo một đối tượng PauseTransition để ẩn text sau 3 giây
            PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
            visiblePause.setOnFinished(event -> continueErrorText.setVisible(false));
            visiblePause.play();
        }
}

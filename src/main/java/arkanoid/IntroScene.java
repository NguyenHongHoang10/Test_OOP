package arkanoid;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Lớp này quản lý Scene và Animation cho màn hình Intro
 * (sử dụng IntroBackground.jpg).
 */
public class IntroScene {

    private final Scene scene;
    private final SequentialTransition sequence;

    public IntroScene(double width, double height, Runnable onFinishedCallback) {

        StackPane root = new StackPane();
        root.setPrefSize(width, height);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));

        // Tải ảnh nền
        Image introImage = null;
        try {
            String path = "/Image/Background/IntroBackground.jpg";
            introImage = new Image(getClass().getResourceAsStream(path));
            if (introImage.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + introImage.getException().getMessage());
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy ảnh IntroBackground.jpg. " + e.getMessage());
        }

        if (introImage != null) {
            ImageView introView = new ImageView(introImage);
            introView.setFitWidth(width);
            introView.setFitHeight(height);
            introView.setPreserveRatio(true);
            root.getChildren().add(introView);
        } else {
            root.getChildren().add(new javafx.scene.text.Text("Loading..."));
        }

        this.scene = new Scene(root, width, height);

        // Tạo hiệu ứng (đen -> rõ -> đen)
        root.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.0)); // Giữ 1 giây

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        this.sequence = new SequentialTransition(fadeIn, hold, fadeOut);

        // Khi kết thúc: gọi callback (để chuyển sang Menu)
        sequence.setOnFinished(e -> onFinishedCallback.run());

        // Xử lý SKIP
        this.scene.setOnKeyPressed((KeyEvent event) -> {
            sequence.stop();
            onFinishedCallback.run();
        });
    }

    public Scene getScene() {
        return this.scene;
    }

    public void play() {
        this.sequence.play();
    }
}
package arkanoid;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Lớp này quản lý Scene và Animation cho màn hình cuộn cốt truyện.
 */
public class StoryScene {

    private final Scene scene;
    private final TranslateTransition scrollAnimation;
    private final VBox storyContainer;
    private final double sceneHeight;

    /**
     * Khởi tạo Scene cuộn cốt truyện.
     * @param width Chiều rộng màn hình
     * @param height Chiều cao màn hình
     * @param onFinishedCallback Hành động (Runnable) sẽ được gọi khi hiệu ứng kết thúc hoặc bị skip.
     */
    public StoryScene(double width, double height, Runnable onFinishedCallback) {
        this.sceneHeight = height;

        // 1. Tạo Pane gốc
        StackPane root = new StackPane();
        root.setPrefSize(width, height);
        try {
            // 1. Tải ảnh từ thư mục resources
            String path = "/Image/Background/storyBackground.png";
            Image bgImage = new Image(getClass().getResourceAsStream(path));

            if (bgImage.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + bgImage.getException().getMessage());
            }

            // 2. Tạo đối tượng BackgroundSize (che phủ 100%)
            BackgroundSize bgSize = new BackgroundSize(1.0, 1.0, true, true, false, true);

            // 3. Tạo BackgroundImage
            BackgroundImage backgroundImage = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    bgSize
            );

            // 4. Đặt nền cho StackPane (root)
            root.setBackground(new Background(backgroundImage));

        } catch (Exception e) {
            System.err.println("Không thể tải ảnh storyBackground.jpg. Sử dụng nền đen dự phòng.");
            e.printStackTrace();
            // Nền đen dự phòng
            root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        }
        // Cắt (clip) để nội dung không tràn ra ngoài
        root.setClip(new javafx.scene.shape.Rectangle(width, height));

        // 2. Tải text từ file (resources/data/story.txt)
        String storyContent = loadStoryText("/data/story.txt");

        Text storyText = new Text(storyContent);
        storyText.setFont(Font.font("Arial", 28)); // Cỡ chữ (bạn có thể thay đổi)
        storyText.setFill(Color.WHITE);
        storyText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // SỬA LỖI (CHỐNG TRÀN): Đặt chiều rộng để text tự động xuống dòng
        storyText.setWrappingWidth(width * 0.9); // 80% chiều rộng màn hình

        // 3. Đặt Text vào VBox để dễ căn chỉnh và di chuyển
        storyContainer = new VBox(storyText);
        storyContainer.setAlignment(Pos.CENTER);
        root.getChildren().add(storyContainer);

        // SỬA LỖI (CHỐNG LỆCH): Căn VBox lên trên cùng (không căn giữa)
        StackPane.setAlignment(storyContainer, Pos.TOP_CENTER);

        // SỬA LỖI (CHỐNG NHÁY): Đặt vị trí bắt đầu ở dưới màn hình
        storyContainer.setTranslateY(height);

        // 4. Tạo Scene
        this.scene = new Scene(root, width, height);

        // 5. Tạo hiệu ứng (Animation)
        this.scrollAnimation = new TranslateTransition();
        scrollAnimation.setNode(storyContainer);

        // 6. Xử lý khi kết thúc: Gọi callback (để chuyển sang Menu)
        scrollAnimation.setOnFinished(e -> onFinishedCallback.run());

        // 7. Xử lý SKIP (nhấn phím bất kỳ)
        this.scene.setOnKeyPressed((KeyEvent event) -> {
            scrollAnimation.stop(); // Dừng hiệu ứng
            onFinishedCallback.run(); // Chuyển cảnh ngay
        });
    }

    /**
     * Trả về Scene để Main.java có thể hiển thị
     */
    public Scene getScene() {
        return this.scene;
    }

    /**
     * Bắt đầu chạy hiệu ứng (gọi sau khi stage.show())
     */
    public void play() {
        // Chúng ta cần dùng Platform.runLater để JavaFX tính toán xong
        // chiều cao thực tế (layout bounds) của khối text
        Platform.runLater(() -> {
            double textHeight = storyContainer.getLayoutBounds().getHeight();

            // Di chuyển đến khi text đi hết lên trên
            scrollAnimation.setToY(-textHeight);

            // Tốc độ: 60 pixels/giây (bạn có thể điều chỉnh)
            double scrollDistance = sceneHeight + textHeight;
            double scrollDurationSeconds = scrollDistance / 60.0;

            scrollAnimation.setDuration(Duration.seconds(scrollDurationSeconds));
            scrollAnimation.play();
        });
    }

    /**
     * Hàm nội bộ: Thử tải text từ file
     */
    private String loadStoryText(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new Exception("Không tìm thấy file: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải story.txt: " + e.getMessage());
            return "Failed to load story.";
        }
    }
}
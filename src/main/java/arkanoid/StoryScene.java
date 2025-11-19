package arkanoid;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


//quản lý Scene và Animation cho màn hình cuộn cốt truyện
public class StoryScene {


    private final Scene scene;
    private final TranslateTransition scrollAnimation;
    private final VBox storyContainer;
    private final double sceneHeight;


    //Khởi tạo Scene cuộn cốt truyện.
    public StoryScene(double width, double height, Runnable onFinishedCallback) {
        this.sceneHeight = height;


        // Tạo Pane gốc
        StackPane root = new StackPane();
        root.setPrefSize(width, height);
        try {
            // Tải ảnh từ thư mục resources
            String path = "/Image/Background/storyBackground.png";
            Image bgImage = new Image(getClass().getResourceAsStream(path));


            if (bgImage.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + bgImage.getException().getMessage());
            }


            // Tạo đối tượng BackgroundSize
            BackgroundSize bgSize = new BackgroundSize(1.0, 1.0, true, true, false, true);


            //  Tạo BackgroundImage
            BackgroundImage backgroundImage = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    bgSize
            );


            // Đặt nền cho StackPane
            root.setBackground(new Background(backgroundImage));


        } catch (Exception e) {
            System.err.println("Không thể tải ảnh storyBackground.jpg. Sử dụng nền đen dự phòng.");
            e.printStackTrace();
            // Nền đen dự phòng
            root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        }
        // Cắt để nội dung không tràn ra ngoài
        root.setClip(new javafx.scene.shape.Rectangle(width, height));


        // Tải text từ file
        String storyContent = loadStoryText("/data/story.txt");


        Text storyText = new Text(storyContent);
        storyText.setFont(Font.font("Arial", 28));
        storyText.setFill(Color.WHITE);
        storyText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);


        //Đặt chiều rộng để text tự động xuống dòng
        storyText.setWrappingWidth(width * 0.9);


        // Đặt Text vào VBox
        storyContainer = new VBox(storyText);
        storyContainer.setAlignment(Pos.CENTER);
        root.getChildren().add(storyContainer);


        // Căn VBox lên trên cùng
        StackPane.setAlignment(storyContainer, Pos.TOP_CENTER);


        // Đặt vị trí bắt đầu ở dưới màn hình
        storyContainer.setTranslateY(height);


        // Tạo Scene
        this.scene = new Scene(root, width, height);


        // Tạo hiệu ứng
        this.scrollAnimation = new TranslateTransition();
        scrollAnimation.setNode(storyContainer);


        // Xử lý khi kết thúc thì chuyển sang Menu
        scrollAnimation.setOnFinished(e -> onFinishedCallback.run());


        // Xử lý skip bằng cách nhấn phím bất kỳ
        this.scene.setOnKeyPressed((KeyEvent event) -> {
            scrollAnimation.stop();
            onFinishedCallback.run();
        });
    }


    public Scene getScene() {
        return this.scene;
    }


    //Bắt đầu chạy hiệu ứng (gọi sau khi stage.show())
    public void play() {
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


    // tải text từ file
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


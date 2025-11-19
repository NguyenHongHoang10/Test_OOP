package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

// Scene hướng dẫn cách chơi với ảnh và nút Back
public class HowToPlayScene {

    private final Scene scene;

    // Khởi tạo Scene "How to Play".
    public HowToPlayScene(double width, double height, Runnable onBackCallback) {
        StackPane root = new StackPane();
        root.setPrefSize(width, height);

        // Tải ảnh nền
        Image helpImage = null;
        try {
            String path = "/Image/Background/help.png";
            helpImage = new Image(getClass().getResourceAsStream(path));
            if (helpImage.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + helpImage.getException().getMessage());
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy ảnh help.png. " + e.getMessage());
        }

        if (helpImage != null) {
            ImageView helpView = new ImageView(helpImage);
            helpView.setFitWidth(width);
            helpView.setFitHeight(height);
            helpView.setPreserveRatio(true); // Giữ tỷ lệ ảnh
            root.getChildren().add(helpView);
        } else {
            // nếu không tải được ảnh
            root.getChildren().add(new Text("Không thể tải ảnh hướng dẫn (help.png)"));
        }

        // Tạo nút Back
        Button backBtn = new Button("⬅ Back");
        backBtn.setFont(javafx.scene.text.Font.font(24));
        backBtn.setCursor(Cursor.HAND);
        backBtn.setOnAction(e -> onBackCallback.run());

        // Thêm nút vào góc trên bên trái
        root.getChildren().add(backBtn);
        StackPane.setAlignment(backBtn, Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new Insets(20));

        this.scene = new Scene(root, width, height);
    }

    public Scene getScene() {
        return this.scene;
    }
}

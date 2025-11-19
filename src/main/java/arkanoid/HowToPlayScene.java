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

/**
 * Lớp này quản lý Scene (màn hình) "How to Play".
 * Nó hiển thị ảnh help.png và một nút "Back".
 */
public class HowToPlayScene {

    private final Scene scene;

    /**
     * Khởi tạo Scene "How to Play".
     * @param width Chiều rộng màn hình
     * @param height Chiều cao màn hình
     * @param onBackCallback Hành động (Runnable) sẽ được gọi khi nhấn nút "Back".
     */
    public HowToPlayScene(double width, double height, Runnable onBackCallback) {
        StackPane root = new StackPane();
        root.setPrefSize(width, height);

        // 1. Tải ảnh nền
        Image helpImage = null;
        try {
            // Đường dẫn dựa trên cấu trúc thư mục của bạn
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
            helpView.setPreserveRatio(true); // Giữ tỷ lệ ảnh (căn giữa)
            root.getChildren().add(helpView);
        } else {
            // Dự phòng nếu không tải được ảnh
            root.getChildren().add(new Text("Không thể tải ảnh hướng dẫn (help.png)"));
        }

        // 2. Tạo nút Back
        Button backBtn = new Button("⬅ Back");
        backBtn.setFont(javafx.scene.text.Font.font(24));
        backBtn.setCursor(Cursor.HAND);
        backBtn.setOnAction(e -> onBackCallback.run());

        // 3. Thêm nút vào góc (góc trên bên trái)
        root.getChildren().add(backBtn);
        StackPane.setAlignment(backBtn, Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new Insets(20));

        this.scene = new Scene(root, width, height);
    }

    /**
     * Trả về Scene để Main.java có thể hiển thị
     */
    public Scene getScene() {
        return this.scene;
    }
}
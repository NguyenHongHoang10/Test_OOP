package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * LevelSelectPane đã được viết lại để sử dụng ảnh nền làm bản đồ
 * và các vùng "hotspot" (trong suốt) để chọn level.
 */
public class LevelSelectPane extends StackPane {

    private final double WIDTH = 800;
    private final double HEIGHT = 600;

    // === BẮT ĐẦU THAY ĐỔI: Cập nhật Constructor ===
    public LevelSelectPane(
            Runnable level1Callback,
            Runnable level2Callback,
            Runnable level3Callback,
            Runnable level4Callback,
            Runnable level5Callback,
            Runnable level6Callback,
            Runnable backCallback
    ) {
        // === KẾT THÚC THAY ĐỔI ===
        setPrefSize(WIDTH, HEIGHT);

        // 1. Tải ảnh nền
        Image bgImage = new Image(getClass().getResourceAsStream("/Image/Background/levelSelectBackground.jpg"));
        ImageView bgView = new ImageView(bgImage);
        bgView.setFitWidth(WIDTH);
        bgView.setFitHeight(HEIGHT);

        // 2. Tạo Pane để chứa các "vùng nóng" (hotspots)
        // Dùng Pane cho phép chúng ta đặt tọa độ tuyệt đối (X, Y)
        Pane hotspotPane = new Pane();
        hotspotPane.setPrefSize(WIDTH, HEIGHT);

        // 3. Tạo các vùng nóng (đã ước lượng tọa độ)
        // (Bạn có thể cần tinh chỉnh lại tọa độ (x,y) và (w,h) cho khớp)

        // Level 1 (Trái Đất) - Dùng hình tròn (cx, cy, radius)
        Circle spot1 = new Circle(65, 333, 27);
        setupHotspot(spot1, 0, level1Callback); // 0 = level 1

        // Level 2 (Thiên thạch) - Dùng hình chữ nhật (x, y, width, height)
        Rectangle spot2 = new Rectangle(145, 260, 100, 90);
        setupHotspot(spot2,1, level2Callback); // 1 = level 2

        // Level 3 (Trạm 1)
        Rectangle spot3 = new Rectangle(265, 260, 145, 75);
        setupHotspot(spot3, 2, level3Callback);

        // Level 4 (Không có trên ảnh - bỏ qua)
        // Chúng ta vẫn giữ level4Callback để tránh lỗi, nhưng không tạo hotspot

        // Level 5 (Trạm 2)
        Rectangle spot5 = new Rectangle(410, 190, 180, 135);
        setupHotspot(spot5, 4, level5Callback); // Index 4 = Level 5

        // Level 6 (Trùm)
        Rectangle spot6 = new Rectangle(600, 120, 185, 190);
        setupHotspot(spot6, 5, level6Callback); // Index 5 = Level 6

        // Thêm các vùng nóng vào Pane
        hotspotPane.getChildren().addAll(spot1, spot2, spot3, spot5, spot6);

        // 4. Tạo nút "Back"
        Button backBtn = new Button("⬅ Back");
        backBtn.setFont(javafx.scene.text.Font.font(24));
        backBtn.setPrefWidth(240);
        backBtn.setOnAction(e -> backCallback.run());

        // 5. Thêm mọi thứ vào StackPane
        getChildren().addAll(
                bgView,      // Lớp dưới cùng: Ảnh nền
                hotspotPane, // Lớp giữa: Các vùng bấm
                backBtn      // Lớp trên cùng: Nút Back
        );

        // Đặt nút Back ở góc dưới bên trái
        StackPane.setAlignment(backBtn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(backBtn, new Insets(20));
    }

    /**
     * Hàm trợ giúp để thiết lập hiệu ứng và logic cho một vùng nóng
     */
    private void setupHotspot(Shape shape, int levelIndex, Runnable callback) {
        // Nếu đã mở khóa:
        shape.setFill(Color.TRANSPARENT); // 1. Bắt đầu trong suốt

        // 2. Hiệu ứng Hover: Hiện highlight
        shape.setOnMouseEntered(e -> shape.setFill(Color.rgb(255, 255, 255, 0.0))); // 30% trắng

        // 3. Hiệu ứng Thoát Hover: Ẩn highlight
        shape.setOnMouseExited(e -> shape.setFill(Color.TRANSPARENT));

        // 4. Hành động Click
        shape.setOnMouseClicked(e -> callback.run());

        // 5. Đổi con trỏ chuột
        shape.setCursor(Cursor.HAND);
    }
}

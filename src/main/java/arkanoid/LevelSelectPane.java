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

public class LevelSelectPane extends StackPane {

    private final double WIDTH = 800;
    private final double HEIGHT = 600;

    // Cập nhật Constructor
    public LevelSelectPane(
            Runnable level1Callback,
            Runnable level2Callback,
            Runnable level3Callback,
            Runnable level4Callback,
            Runnable level5Callback,
            Runnable level6Callback,
            Runnable backCallback
    ) {
        setPrefSize(WIDTH, HEIGHT);

        // Tải ảnh nền
        Image bgImage = new Image(getClass().getResourceAsStream("/Image/Background/levelSelectBackground.jpg"));
        ImageView bgView = new ImageView(bgImage);
        bgView.setFitWidth(WIDTH);
        bgView.setFitHeight(HEIGHT);

        // Tạo Pane để chứa các vùng lựa chọn level
        Pane hotspotPane = new Pane();
        hotspotPane.setPrefSize(WIDTH, HEIGHT);

        // Tạo các vùng lựa chọn level
        // Level 1
        Circle spot1 = new Circle(65, 333, 27);
        setupHotspot(spot1, 0, level1Callback);

        // Level 2
        Rectangle spot2 = new Rectangle(145, 260, 100, 90);
        setupHotspot(spot2, 1, level2Callback);

        // Level 3
        Rectangle spot3 = new Rectangle(265, 260, 145, 75);
        setupHotspot(spot3, 2, level3Callback);

        // Level 4 (đã xoá)

        // Level 5
        Rectangle spot5 = new Rectangle(410, 190, 180, 135);
        setupHotspot(spot5, 4, level5Callback);

        // Level 6
        Rectangle spot6 = new Rectangle(600, 120, 185, 190);
        setupHotspot(spot6, 5, level6Callback);

        // Thêm các vùng chọn level vào Pane
        hotspotPane.getChildren().addAll(spot1, spot2, spot3, spot5, spot6);

        // Tạo nút Back
        Button backBtn = new Button("⬅ Back");
        backBtn.setFont(javafx.scene.text.Font.font(24));
        backBtn.setPrefWidth(240);
        backBtn.setOnAction(e -> backCallback.run());

        // Thêm tất cả vào StackPane
        getChildren().addAll(
                bgView,
                hotspotPane,
                backBtn
        );

        // Đặt nút Back ở góc dưới bên trái
        StackPane.setAlignment(backBtn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(backBtn, new Insets(20));
    }

    // Thiết lập hiệu ứng và logic cho một vùng chọn level
    private void setupHotspot(Shape shape, int levelIndex, Runnable callback) {
        // Bắt đầu trong suốt
        shape.setFill(Color.TRANSPARENT);

        // Hiệu ứng Hover: Hiện highlight
        shape.setOnMouseEntered(e -> shape.setFill(Color.rgb(255, 255, 255, 0.0))); // 30% trắng

        // Hiệu ứng Thoát Hover: Ẩn highlight
        shape.setOnMouseExited(e -> shape.setFill(Color.TRANSPARENT));

        // Hành động Click
        shape.setOnMouseClicked(e -> callback.run());

        // Đổi con trỏ chuột
        shape.setCursor(Cursor.HAND);
    }
}

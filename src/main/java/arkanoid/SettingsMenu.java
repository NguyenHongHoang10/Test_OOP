package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;

/**
 * Lớp UI mới cho Menu Cài đặt (Settings).
 * Đây là một VBox tự quản lý các nút Âm thanh và nút Back.
 */
public class SettingsMenu extends VBox {
    private final Button btnSoundOn;
    private final Button btnSoundOff;
    private final Button btnSettingsBack;

    /**
     * Khởi tạo VBox menu cài đặt.
     * @param onBackCallback Hành động (Runnable) sẽ được gọi khi nhấn nút "Back".
     */
    public SettingsMenu(Runnable onBackCallback) {
        super(20); // Khoảng cách giữa các nút

        Text title = new Text("Settings");
        title.setFont(Font.font(32));
        title.setFill(Color.WHITE);

        // Tạo các nút
        btnSoundOn = createMenuButton("Sound: ON");
        btnSoundOff = createMenuButton("Sound: OFF");
        btnSettingsBack = createMenuButton("⬅ Back");

        // Gán hành động
        btnSoundOn.setOnAction(e -> {
            SoundManager.get().setMuted(true); // Tắt âm thanh
            updateSoundButtons();
            SoundManager.get().play(SoundManager.Sfx.CLICK);
        });

        btnSoundOff.setOnAction(e -> {
            SoundManager.get().setMuted(false); // Bật âm thanh
            updateSoundButtons();
            SoundManager.get().play(SoundManager.Sfx.CLICK);
        });

        btnSettingsBack.setOnAction(e -> {
            // 1. Tự động ẩn chính nó khi được nhấp
            setVisible(false);

            // 2. Chạy callback (nếu có)
            if (onBackCallback != null) {
                onBackCallback.run();
            }
            // (Không cần play sound ở đây, vì callback sẽ play)
        });

        // Thêm các thành phần vào VBox
        getChildren().addAll(title, btnSoundOn, btnSoundOff, btnSettingsBack);
        setAlignment(Pos.CENTER);

        // Đặt style nền mờ (giống pauseMenu trong GameContainer)
        setBackground(new Background(new BackgroundFill(
                Color.rgb(20, 24, 30, 0.85), // Nền đen mờ 85%
                new CornerRadii(10), // Bo góc
                Insets.EMPTY
        )));
        setMaxSize(300, 400); // Giới hạn kích thước
        setVisible(false); // Ẩn lúc đầu

        updateSoundButtons(); // Cập nhật trạng thái nút ban đầu
    }

    /**
     * Hàm này nên được gọi khi menu được hiển thị
     * để đảm bảo các nút ON/OFF hiển thị đúng.
     */
    public void onShow() {
        updateSoundButtons();
    }

    /**
     * Cập nhật nút nào được hiển thị (ON hoặc OFF)
     * dựa trên trạng thái của SoundManager.
     */
    private void updateSoundButtons() {
        boolean isMuted = SoundManager.get().isMuted();
        btnSoundOn.setVisible(!isMuted); // Hiện nút ON nếu CHƯA mute
        btnSoundOff.setVisible(isMuted); // Hiện nút OFF nếu ĐANG mute
    }

    /**
     * Hàm tiện ích để tạo nút (lấy từ GameContainer)
     */
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(240);
        btn.setFont(Font.font(24));
        btn.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;" );
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.03);
            btn.setScaleY(1.03);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        btn.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.45)));
        btn.setFocusTraversable(false);
        return btn;
    }
}
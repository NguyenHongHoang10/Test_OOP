package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * ScorePopup (cập nhật)
 * - chữ to hơn, màu sáng, viền/đổ bóng để dễ đọc
 * - scale animation nhẹ
 * - thời gian sống kéo dài hơn
 */
public class ScorePopup {
    private double x, y;          // vị trí (center)
    private double vy;           // vận tốc thẳng đứng (px/s) — âm là lên
    private double life;         // remaining seconds
    private final double maxLife;
    private final String text;
    private final Font font;
    private final Color color;

    // CẤU HÌNH MỚI (đã tăng kích thước/màu/thời gian)
    public static final double DEFAULT_LIFE = 1.40;    // tăng từ 0.8 -> 1.4s
    public static final double DEFAULT_VY = -46.0;     // vẫn lên; bạn có thể đặt -60 nếu muốn bay nhanh hơn
    public static final Font DEFAULT_FONT = Font.font("Arial", 26); // lớn hơn (26)
    public static final Color DEFAULT_COLOR = Color.web("#FFDF5A"); // vàng sáng/gold

    public ScorePopup(double centerX, double centerY, String text) {
        this(centerX, centerY, text, DEFAULT_FONT, DEFAULT_COLOR, DEFAULT_LIFE, DEFAULT_VY);
    }

    public ScorePopup(double centerX, double centerY, String text, Font font, Color color, double life, double initialVy) {
        this.x = centerX;
        this.y = centerY;
        this.text = text;
        this.font = font;
        this.color = color;
        this.maxLife = life;
        this.life = life;
        this.vy = initialVy;
    }

    public void update(double dt) {
        if (life <= 0) return;
        life -= dt;
        // easing trên vận tốc để tạo cảm giác "nảy nhẹ" khi bắt đầu, rồi chậm dần
        vy *= 0.96;
        y += vy * dt;
    }

    public boolean isAlive() {
        return life > 0;
    }

    /**
     * render với outline + shadow + scale animation
     */
    public void render(GraphicsContext gc) {
        if (life <= 0) return;
        double t = Math.max(0, life / maxLife); // t từ 1 -> 0

        // scale animation: bắt đầu hơi lớn, dần về 1.0
        // scale = 1.0 + 0.35 * (1 - t)^(0.6) => tạo cảm giác 'pop' nhẹ
        double scale = 1.0 + 0.35 * Math.pow(1.0 - t, 0.6);

        // alpha fade (sử dụng pow để fade mượt hơn ở cuối)
        double alpha = Math.pow(t, 0.9);

        // đo kích thước text để canh giữa
        Text meas = new Text(text);
        meas.setFont(font);
        double textW = meas.getLayoutBounds().getWidth();
        double textH = meas.getLayoutBounds().getHeight();

        gc.save();

        gc.setFont(font);
        // dịch transform để vẽ theo center với scale
        gc.translate(x, y);
        gc.scale(scale, scale);

        // shadow (mờ, hơi lệch) để nổi chữ trên background
        gc.setGlobalAlpha(alpha * 0.9);
        gc.setFill(Color.rgb(6, 6, 6, 0.65)); // bóng đen mờ
        gc.fillText(text, -textW / 2.0 + 2, -textH / 2.0 + 4);

        // outline (stroke) để chữ rõ nét trên mọi nền
        gc.setLineWidth(2.5);
        gc.setStroke(Color.rgb(10, 10, 10, 0.85));
        gc.setGlobalAlpha(alpha);
        gc.strokeText(text, -textW / 2.0, -textH / 2.0);

        // main fill (gold bright)
        gc.setFill(color);
        gc.fillText(text, -textW / 2.0, -textH / 2.0);

        gc.restore();
        gc.setGlobalAlpha(1.0);
    }
}

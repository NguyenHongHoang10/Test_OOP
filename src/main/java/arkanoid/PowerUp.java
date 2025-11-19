package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import java.util.EnumMap;
import java.util.Map;

public class PowerUp {
    public enum PowerType { SHRINK_PADDLE, EXPAND_PADDLE, TINY_BALL, MULTI_BALL, SLOW_BALL, FAST_BALL, NEXT_LEVEL,
        EXTRA_LIFE, SUDDEN_DEATH, LASER_PADDLE, FIREBALL, BARRIER, WEAKEN, SCORE_MULTIPLIER }

    double x, y, w = 24, h = 24;
    double vy = 80; // rơi xuống
    PowerType type;
    private Image image; // Ảnh đại diện cho power-up (cục bộ)

    // Map tĩnh (static) để lưu trữ tất cả ảnh đã được tải trước
    // Dùng EnumMap để tối ưu hiệu suất
    private static final Map<PowerType, Image> powerUpImages = new EnumMap<>(PowerType.class);

    /**
     * Khối (block) tĩnh này sẽ chạy MỘT LẦN khi lớp PowerUp được tải.
     * Nó sẽ tải tất cả các ảnh từ /Image/PowerUp/ và lưu vào Map.
     */
    static {
        // Giả sử tất cả ảnh của bạn đều là .png
        // Nếu bạn dùng đuôi file khác (như .jpg), hãy đổi ở đây
        String fileExtension = ".png";

        for (PowerType pt : PowerType.values()) {
            // Lấy tên enum (ví dụ: "FIREBALL") làm tên file
            String fileName = pt.name() + fileExtension;
            // Đường dẫn tuyệt đối từ gốc thư mục resources
            String path = "/Image/PowerUp/" + fileName;

            try {
                // Tải ảnh
                Image img = new Image(PowerUp.class.getResourceAsStream(path));

                if (img.isError()) {
                    // Báo lỗi nếu ảnh bị lỗi hoặc không tìm thấy
                    System.err.println("LỖI: Không tải được ảnh power-up: " + path);
                } else {
                    powerUpImages.put(pt, img); // Thêm ảnh đã tải vào Map
                }
            } catch (Exception e) {
                // Báo lỗi nếu không tìm thấy file (ví dụ: NullPointerException)
                System.err.println("LỖI: Không tìm thấy tài nguyên: " + path);
            }
        }
    }

    /**
     * Hàm public static để các lớp khác (như GameRenderer)
     * có thể truy cập các ảnh đã được tải trước.
     */
    public static Image getPowerUpImage(PowerType type) {
        return powerUpImages.get(type);
    }

    public PowerUp(double x, double y, PowerType type) {
        this.x = x; this.y = y; this.type = type;
        this.image = powerUpImages.get(type);
    }

    public void update(double dt) {
        y += vy * dt;
    }

    public void render(GraphicsContext gc) {
        if (this.image != null) {
            // Nếu có ảnh, vẽ ảnh (căn giữa tại x, y)
            gc.drawImage(image, x - w/2, y - h/2, w, h);
        } else {
            // Nếu không có ảnh (tải lỗi), vẽ hình tròn màu như cũ
            gc.setFill(colorForType(type));
            gc.fillOval(x - w/2, y - h/2, w, h);
            gc.setStroke(Color.WHITE);
            gc.strokeOval(x - w/2, y - h/2, w, h);
        }
    }

    public boolean collidesWithPaddle(Paddle paddle) {
        double px = paddle.getX();
        double py = paddle.getY();
        double pw = paddle.getWidth();
        double ph = paddle.getHeight();
        return x + w/2 > px && x - w/2 < px + pw && y + h/2 > py && y - h/2 < py + ph;
    }

    public Color colorForType(PowerType t) {
        switch (t) {
            case SHRINK_PADDLE: return Color.BROWN;
            case EXPAND_PADDLE: return Color.LIGHTGREEN;
            case TINY_BALL: return Color.SKYBLUE;
            case MULTI_BALL: return Color.GOLD;
            case SLOW_BALL: return Color.CORNFLOWERBLUE;
            case FAST_BALL: return Color.ORANGERED;
            case NEXT_LEVEL: return Color.DARKVIOLET;
            case EXTRA_LIFE: return Color.PINK;
            case SUDDEN_DEATH: return Color.BLACK;
            case LASER_PADDLE: return Color.SALMON;
            case FIREBALL: return Color.RED;
            case BARRIER: return Color.DEEPSKYBLUE;
            case WEAKEN: return Color.DARKORANGE;
            case SCORE_MULTIPLIER: return Color.BEIGE;
            default: return Color.WHITE;
        }
    }
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return w;
    }

    public double getHeight() {
        return h;
    }
}
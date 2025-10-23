package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Brick extends GameObject {
    public enum Type { NORMAL, INDESTRUCTIBLE, EXPLOSIVE }

    private int hits; // số lần bị đánh
    private final Type type;

    // Constructor mặc định cho gạch bình thường
    public Brick(double x, double y, double width, double height, int hits) {
        super(x, y, width, height);
        this.hits = Math.max(1, hits);
        this.type = Type.NORMAL;
    }

    // Constructor cho phép chỉ định type (INDESTRUCTIBLE, EXPLOSIVE, ...)
    public Brick(double x, double y, double width, double height, Type type, int hits) {
        super(x, y, width, height);
        this.type = type;
        if (type == Type.INDESTRUCTIBLE) {
            this.hits = Integer.MAX_VALUE; // không bao giờ giảm
        } else {
            this.hits = Math.max(1, hits);
        }
    }

    // Màu theo số lần chịu đòn để dễ phân biệt
    private Color colorByHits(int hits) {
        switch (hits) {
            case 1: return Color.LIGHTGREEN;
            case 2: return Color.GOLD;
            case 3: return Color.ORANGERED;
            default: return Color.GRAY;
        }
    }

    @Override
    public void update(double deltaTime) {
        // bricks are static; nothing per-frame for now
    }

    // render brick với màu dựa trên số hits
    @Override
    public void render(GraphicsContext gc) {
        Color fill;
        switch (type) {
            case INDESTRUCTIBLE:
                fill = Color.DARKSLATEGRAY;
                break;
            case EXPLOSIVE:
                fill = Color.CRIMSON; // explosive có màu nổi bật
                break;
            default:
                fill = colorByHits(hits);
        }
        gc.setFill(fill);
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(Color.DARKGRAY);
        gc.strokeRoundRect(x, y, width, height, 6, 6);

        // Nếu là explosive, vẽ một dấu chấm nhỏ ở giữa để dễ nhận biết
        if (type == Type.EXPLOSIVE) {
            gc.setFill(Color.YELLOW);
            double cx = x + width/2.0;
            double cy = y + height/2.0;
            gc.fillOval(cx - 3, cy - 3, 6, 6);
        }
    }

    // Giảm số lần chịu đòn; trả true nếu brick bị phá hoàn toàn
    public boolean hit() {
        if (type == Type.INDESTRUCTIBLE) return false; // không thể phá
        hits--;
        return hits <= 0;
    }

    // Getter số hits (dùng để debug / UI nếu cần)
    public int getHits() {
        return this.hits;
    }

    // Không phá hủy gạch nếu hits giảm xuống 1 (theo yêu cầu).
    public void weaken() {
        if (this.type == Type.NORMAL && this.hits > 1) {
            this.hits = this.hits - 1;
        }
    }

    public boolean isDestructible() {
        return type != Type.INDESTRUCTIBLE;
    }

    public Type getType() { return type; }
}

package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Được tách ra từ lớp nội bộ private static class Game.HUDMessage
public class HUDMessage {
    String text;
    double life; // remaining seconds
    double maxLife;

    HUDMessage(String text, double life) {
        this.text = text;
        this.maxLife = life;
        this.life = life;
    }

    void update(double dt) {
        life -= dt;
    }

    boolean isAlive() {
        return life > 0;
    }

    void render(GraphicsContext gc, double canvasWidth) {
        double alpha = Math.max(0, life / maxLife);
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        // ước tính độ rộng bằng số ký tự * 6 px (ước lượng đơn giản)
        double estWidth = text.length() * 6;
        gc.fillText(text, canvasWidth / 2.0 - estWidth / 2.0, 40);
        gc.setGlobalAlpha(1.0);
    }
}

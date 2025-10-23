package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class GameRenderer {
    private final GraphicsContext gc;
    private final double width, height;

    public GameRenderer(GraphicsContext gc, double width, double height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    // Hàm render chính, nhận dữ liệu từ các Manager
    public void render(GameState state, EntityManager entities, Paddle paddle) {
        // 1. Vẽ nền
        gc.setFill(Color.rgb(20, 24, 30));
        gc.fillRect(0, 0, width, height);

        // 2. Vẽ HUD (High, Score, Lives)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        // Hiển thị High Score ở trên Score
        gc.fillText("HighScore: " + state.getHighScore(), 10, 22);
        gc.fillText("Score: " + state.getScore(), 10, 42);
        gc.fillText("Lives: " + state.getLives(), width - 110, 22);

        // 3. Vẽ các đối tượng game
        paddle.render(gc);
        for (Ball bl : entities.getBalls()) bl.render(gc);
        for (Brick b : entities.getBricks()) b.render(gc);
        for (PowerUp pu : entities.getPowerUps()) pu.render(gc);
        for (Bullet bu : entities.getBullets()) bu.render(gc);

        // 4. Vẽ tin nhắn HUD (ví dụ: "FIREBALL -10s")
        for (HUDMessage hm : entities.getHudMessages()) hm.render(gc, width);

        // 5. Vẽ rào chắn (Barrier)
        if (state.isBarrierActive()) {
            gc.setGlobalAlpha(0.9);
            gc.setFill(Color.rgb(30, 180, 255, 0.25));
            gc.fillRect(0, state.getBarrierY() - state.getBarrierThickness() / 2.0, width, state.getBarrierThickness());
            gc.setStroke(Color.rgb(100, 220, 255));
            gc.setLineWidth(2);
            gc.strokeRect(0, state.getBarrierY() - state.getBarrierThickness() / 2.0, width, state.getBarrierThickness());
            gc.setGlobalAlpha(1.0);
        }

        // 6. Vẽ thông báo "Launch Ball"
        if (state.isRunning() && entities.getBalls().stream().anyMatch(Ball::isStuck)) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(14));
            gc.fillText("Press SPACE to launch ball", width / 2 - 90, height - 10);
        }

        // 7. Vẽ các Overlay (Game Over, Pause, Confirm)
        if (state.isShowMessage()) {
            renderEndGameOverlay(state.isWin(), state.getScore());
        } else if (state.isPauseOverlay()) {
            renderPauseOverlay();
        } else if (state.isConfirmOverlay()) {
            renderConfirmOverlay();
        }
    }

    private void renderEndGameOverlay(boolean win, int score) {
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(36));
        String text = win ? "YOU WIN!" : "GAME OVER";
        gc.fillText(text, width / 2 - 110, height / 2 - 20);
        gc.setFont(Font.font(20));
        gc.fillText("Score: " + score, width / 2 - 60, height / 2 + 10);
        gc.fillText("Press S to startNewGame", width / 2 - 115, height / 2 + 50);
        gc.fillText("Press R to return menu", width / 2 - 110, height / 2 + 90);
    }

    private void renderPauseOverlay() {
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(28));
        gc.fillText("PAUSED", width / 2 - 60, height / 2 - 20);
        gc.setFont(Font.font(16));
        gc.fillText("Nhấn P để tiếp tục", width / 2 - 80, height / 2 + 10);
        gc.fillText("Nhấn O để quay về Menu", width / 2 - 95, height / 2 + 35);
    }

    private void renderConfirmOverlay() {
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(20));
        String line1 = "Do you want to quit?";
        String line2 = "Press Y to quit, N to continue";

        Text temp1 = new Text(line1); temp1.setFont(gc.getFont());
        double w1 = temp1.getLayoutBounds().getWidth();
        double h1 = temp1.getLayoutBounds().getHeight();
        Text temp2 = new Text(line2); temp2.setFont(gc.getFont());
        double w2 = temp2.getLayoutBounds().getWidth();

        double centerX1 = (width - w1) / 2;
        double centerX2 = (width - w2) / 2;
        double centerY = height / 2;

        gc.fillText(line1, centerX1, centerY);
        gc.fillText(line2, centerX2, centerY + h1 + 15);
    }
}

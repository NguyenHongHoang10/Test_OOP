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
    public void render(GameState state, EntityManager entities, Paddle paddle, Boss boss, boolean bossLevel) {
        // 1. Vẽ nền
        gc.setFill(Color.rgb(20, 24, 30));
        gc.fillRect(0, 0, width, height);

        // 2. Vẽ HUD (Điểm, Mạng)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Score: " + state.getScore(), 10, 22);
        gc.fillText("Lives: " + state.getLives(), 150, 22);

        // Cập nhật highscore trong RAM nếu score hiện tại vượt qua
        HighScoreService svc = HighScoreService.get();
        svc.maybeUpdateBest(state.getScore());
        int best = svc.getCachedBest();

        int levelHuman = Math.max(1, state.getCurrentLevelIndex() + 1);
        gc.fillText("HighScore: " + best, 300, 22);
        gc.fillText("Level: " + levelHuman, 470, 22);


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

        // 8. Vẽ Boss (nếu có)
        if (bossLevel && boss != null) {
            boss.render(gc);
        }
    }
}

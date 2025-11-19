package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class GameRenderer {
    private final GraphicsContext gc;
    private final double width, height;
    private final Image heartImage;

    // Danh sách để lưu trữ các ảnh nền đã tải
    private final List<Image> backgroundImages = new ArrayList<>();
    // Màu nền dự phòng nếu không tải được ảnh
    private final Color fallbackBackgroundColor = Color.rgb(20, 24, 30);

    public GameRenderer(GraphicsContext gc, double width, double height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
        // Lấy ảnh trái tim (EXTRA_LIFE) từ lớp PowerUp
        this.heartImage = PowerUp.getPowerUpImage(PowerUp.PowerType.EXTRA_LIFE);
        loadBackgrounds();
    }

    // Hàm tải ảnh
    // Tải trước tất cả ảnh nền từ /Image/Background/

    private void loadBackgrounds() {
        for (int i = 1; i <= 8; i++) {
            String path = "/Image/Background/level_b" + i + ".png";
            try {
                Image img = new Image(getClass().getResourceAsStream(path));
                if (img.isError()) {
                    System.err.println("Lỗi tải ảnh nền: " + path);
                    backgroundImages.add(null);
                } else {
                    backgroundImages.add(img);
                }
            } catch (Exception e) {
                System.err.println("Không tìm thấy tài nguyên ảnh nền: " + path);
                backgroundImages.add(null);
            }
        }
    }

    // Hàm render chính, nhận dữ liệu từ các Manager
    public void render(GameState state, EntityManager entities, CollisionManager collisionManager,
                       PowerUpManager power, Paddle paddle, Boss boss, boolean bossLevel) {
        gc.save();

        // apply screen shake
        double offsetX = 0, offsetY = 0;
        if (collisionManager.getShakeTime() > 0 && collisionManager.getShakeDuration() > 0) {
            double t = collisionManager.getShakeTime() / collisionManager.getShakeDuration();
            double amp = collisionManager.getShakeMagnitude() * t;
            offsetX = (Math.random() * 2.0 - 1.0) * amp;
            offsetY = (Math.random() * 2.0 - 1.0) * amp;
            gc.translate(offsetX, offsetY);
        }

        // Vẽ nền
        int levelIndex = state.getCurrentLevelIndex();
        Image bgToDraw = null;

        // Kiểm tra xem có ảnh cho màn này không
        if (levelIndex >= 0 && levelIndex < backgroundImages.size()) {
            bgToDraw = backgroundImages.get(levelIndex);
        }

        if (bgToDraw != null) {
            // Vẽ ảnh nền (kéo dãn để vừa màn hình)
            gc.drawImage(bgToDraw, 0, 0, width, height);
        } else {
            // Nếu không có ảnh, vẽ màu nền dự phòng
            gc.setFill(fallbackBackgroundColor);
            gc.fillRect(0, 0, width, height);
        }

        // Vẽ HUD (Điểm, Mạng)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Score: " + state.getScore(), 10, 42);
        if (heartImage != null) {
            int lives = state.getLives();
            double heartWidth = 20;
            double heartHeight = 20;
            double startX = 345;
            double startY = 45 - heartHeight;

            for (int i = 0; i < lives; i++) {
                // Vẽ 1 trái tim
                // Vị trí X = startX + (số thứ tự * (chiều rộng + 4px đệm))
                gc.drawImage(heartImage, startX + (i * (heartWidth + 4)), startY, heartWidth, heartHeight);
            }
        } else {
            // Nếu ảnh trái tim tải lỗi, vẽ lại text cũ
            gc.fillText("Lives: " + state.getLives(), 160, 42);
        }

        // Cập nhật highscore trong RAM nếu score hiện tại vượt qua
        HighScoreService svc = HighScoreService.get();
        svc.maybeUpdateBest(state.getScore());
        int best = svc.getCachedBest();

        int levelHuman = Math.max(1, state.getCurrentLevelIndex() + 1);
        gc.fillText("HighScore: " + best, 10, 22);
        gc.fillText("Level: " + (levelHuman > 3 ? levelHuman - 1 : levelHuman), 350, 22);


        // Vẽ các đối tượng game
        paddle.render(gc);
        for (Ball bl : entities.getBalls()) bl.render(gc);
        for (Brick b : entities.getBricks()) b.render(gc);
        for (PowerUp pu : entities.getPowerUps()) pu.render(gc);
        for (Explosion ex : collisionManager.getExplosions()) ex.render(gc);
        for (ScorePopup sp : collisionManager.getScorePopups()) sp.render(gc);
        for (Bullet bu : entities.getBullets()) bu.render(gc);

        // Vẽ cổng + flying bricks khi nó hoạt động
        if (power.isNextLevelInProgress()) {
            gc.save();
            // ánh sáng nhẹ phía sau
            double g = power.getPortalGlow();
            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
            double glowR = power.getPortalBaseRadius() * (1.0 + 1.6 * g);
            gc.setGlobalAlpha(0.28 * (0.8 + 0.4 * Math.sin(g * 6.28)));
            javafx.scene.paint.RadialGradient rg = new javafx.scene.paint.RadialGradient(
                    0, 0, power.getPortalX(), power.getPortalY(), glowR, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, javafx.scene.paint.Color.rgb(255, 255, 255, 0.65)),
                    new javafx.scene.paint.Stop(1.0, javafx.scene.paint.Color.rgb(120, 80, 160, 0.05))
            );
            gc.setFill(rg);
            gc.fillOval(power.getPortalX() - glowR, power.getPortalY() - glowR, glowR * 2, glowR * 2);

            // vẽ cổng next level
            gc.setGlobalAlpha(0.95);
            gc.setStroke(javafx.scene.paint.Color.rgb(220, 200, 255, 0.95));
            gc.setLineWidth(3.0 + 3.0 * g);
            gc.strokeOval(power.getPortalX() - power.getPortalBaseRadius() * 2,
                    power.getPortalY() - power.getPortalBaseRadius() * 2,
                    power.getPortalBaseRadius() * 4, power.getPortalBaseRadius() * 4);

            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
            gc.restore();

            // vẽ flying bricks
            for (FlyingBrick fb : entities.getFlyingBricks()) {
                gc.save();
                // dịch gạch đến trung tâm
                double bw = fb.brick.getWidth();
                double bh = fb.brick.getHeight();
                double drawW = bw * fb.scale;
                double drawH = bh * fb.scale;
                gc.translate(fb.x, fb.y);
                gc.rotate(Math.toDegrees(fb.angle));
                // vẽ hình chữ nhật trung tâm
                int hit = fb.brick.getHits();
                Color c = fb.brick.colorByHits(hit);
                gc.setGlobalAlpha(1.0);
                gc.setFill(c);
                gc.fillRect(-drawW / 2.0, -drawH / 2.0, drawW, drawH);
                // vẽ viền cho viên gạch
                gc.setStroke(c.darker());
                gc.setLineWidth(1.0);
                gc.strokeRect(-drawW / 2.0, -drawH / 2.0, drawW, drawH);
                gc.restore();
            }

            // vẽ hiệu ứng -1 hit
            if (power.isWhiteFlashActive() && power.getWhiteFlashAlpha() > 0.001) {
                gc.save();
                gc.setGlobalAlpha(Math.max(0.0, Math.min(1.0, power.getWhiteFlashAlpha())));
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.fillRect(0, 0, width, height);
                gc.restore();
            }
        }

        // Vẽ tin nhắn HUD
        for (HUDMessage hm : entities.getHudMessages()) hm.render(gc, width);

        // Vẽ rào chắn
        if (state.isBarrierActive()) {
            gc.setGlobalAlpha(0.9);
            gc.setFill(Color.rgb(30, 180, 255, 0.25));
            gc.fillRect(0, state.getBarrierY() - state.getBarrierThickness() / 2.0,
                    width, state.getBarrierThickness());
            gc.setStroke(Color.rgb(100, 220, 255));
            gc.setLineWidth(2);
            gc.strokeRect(0, state.getBarrierY() - state.getBarrierThickness() / 2.0,
                    width, state.getBarrierThickness());
            gc.setGlobalAlpha(1.0);
        }

        if (power.isWeakenInProgress() && power.getCurrentShockwave() != null) {
            power.getCurrentShockwave().render(gc);
        }

        // Vẽ thông báo "Launch Ball"
        if (state.isRunning() && entities.getBalls().stream().anyMatch(Ball::isStuck)) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(14));
            gc.fillText("Press SPACE to launch ball", width / 2 - 90, height - 10);
        }

        // Vẽ Boss
        if (bossLevel && boss != null) {
            boss.render(gc);
        }

        gc.restore();

        // vẽ gạch vỡ
        collisionManager.getEmitter().render(gc);
        collisionManager.getDebrisEmitter().render(gc);

        // vẽ flash
        if (collisionManager.getFlashAlpha() > 0.001) {
            gc.setGlobalAlpha(Math.min(1.0, collisionManager.getFlashAlpha()));
            gc.setFill(javafx.scene.paint.Color.rgb(255, 240, 220));
            gc.fillRect(0, 0, width, height);
            gc.setGlobalAlpha(1.0);
        }
    }
}

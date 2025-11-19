package arkanoid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra phản xạ vận tốc khi bóng va chạm gạch:
 * - Đụng từ TRÊN/DƯỚI/TRÁI/PHẢI -> đảo dấu thành phần vận tốc tương ứng
 * - Va chạm tại GÓC -> phải đảo ít nhất một thành phần (vx hoặc vy)
 * - Gạch bất tử -> vẫn phản xạ
 * - Fireball -> không phản xạ (giữ nguyên vx, vy)
 *
 * Lưu ý:
 * - Ball(x,y, r, paddle) dùng toạ độ TÂM (centerX, centerY).
 * - Để chắc chắn có va chạm, đặt tâm bóng chồng vào gạch 1px theo hướng muốn kiểm tra.
 * - Các test này thuần logic, không cần JavaFX Toolkit; chỉ tắt âm để tránh gọi media.
 */
public class BallBrickCollisionTest {

    @BeforeAll
    static void mute() {
        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }

    @Test
    void collideFromTop_invertsVy() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        // Đặt bóng ngay phía trên gạch, chồng xuống 1px, đi xuống
        Ball ball = new Ball(brick.getX() + brick.getWidth()/2.0, brick.getY() - 8 + 1, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(30, 120);

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit, "Phải phát hiện va chạm");
        assertTrue(ball.getVy() < 0, "Đụng từ TRÊN: vy phải đảo dấu (đi lên)");
    }

    @Test
    void collideFromBottom_invertsVyOpposite() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        // Dưới gạch, chồng lên 1px, đi lên
        Ball ball = new Ball(brick.getX() + brick.getWidth()/2.0, brick.getY() + brick.getHeight() + 8 - 1, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(0, -150);

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit);
        assertTrue(ball.getVy() > 0, "Đụng từ DƯỚI: vy phải đảo dấu (đi xuống)");
    }

    @Test
    void collideFromLeft_invertsVx() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        // Bên trái gạch, chồng sang phải 1px, đi sang phải
        Ball ball = new Ball(brick.getX() - 8 + 1, brick.getY() + brick.getHeight()/2.0, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(160, 0);

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit);
        assertTrue(ball.getVx() < 0, "Đụng từ TRÁI: vx phải đảo dấu (đi sang trái)");
    }

    @Test
    void collideFromRight_invertsVxOpposite() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        // Bên phải gạch, chồng sang trái 1px, đi sang trái
        Ball ball = new Ball(brick.getX() + brick.getWidth() + 8 - 1, brick.getY() + brick.getHeight()/2.0, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(-160, 0);

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit);
        assertTrue(ball.getVx() > 0, "Đụng từ PHẢI: vx phải đảo dấu (đi sang phải)");
    }

    @Test
    void collideAtCorner_invertsAtLeastOneComponent() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        // Góc trên-trái: đặt bóng chồng 1px theo cả hai trục, đi chéo xuống-phải
        Ball ball = new Ball(brick.getX() - 8 + 1, brick.getY() - 8 + 1, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(120, 120);

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit);

        // Tại góc, tuỳ triển khai có thể đảo vx hoặc vy (hoặc cả hai);
        // Kiểm tra tối thiểu: ít nhất MỘT thành phần đã đảo dấu.
        boolean vxInverted = ball.getVx() < 0;
        boolean vyInverted = ball.getVy() < 0;
        assertTrue(vxInverted || vyInverted, "Va góc: phải đảo ít nhất một thành phần vận tốc");
    }

    @Test
    void collideWithIndestructible_reflects() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick steel = new Brick(200, 200, 40, 20, Brick.Type.INDESTRUCTIBLE, Integer.MAX_VALUE);

        Ball ball = new Ball(steel.getX() + steel.getWidth()/2.0, steel.getY() - 8 + 1, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(0, 150);

        boolean hit = ball.collideWithBrick(steel, ball.isFireball());
        assertTrue(hit);
        assertTrue(ball.getVy() < 0, "Gạch bất tử vẫn phải phản xạ bóng");
    }

    @Test
    void fireballDoesNotReflect_velocityUnchanged() {
        Paddle paddle = new Paddle(0,0,120,16,800);
        Brick brick = new Brick(200, 200, 40, 20, 1);

        Ball ball = new Ball(brick.getX() + brick.getWidth()/2.0, brick.getY() + brick.getHeight()/2.0, 8, paddle);
        ball.setStuck(false);
        ball.setFireball(true);
        ball.setVelocity(110, -170); // sẽ được chuẩn hoá trong setVelocity

        double vx0 = ball.getVx();
        double vy0 = ball.getVy();

        boolean hit = ball.collideWithBrick(brick, ball.isFireball());
        assertTrue(hit);
        assertEquals(vx0, ball.getVx(), 1e-6, "Fireball không đổi vx");
        assertEquals(vy0, ball.getVy(), 1e-6, "Fireball không đổi vy");
    }
}

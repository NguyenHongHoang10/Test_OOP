package arkanoid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BallPhysicsTest {

    @BeforeAll
    static void mute() {
        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }

    @Test
    void testWallBounceHorizontal() {
        Paddle paddle = new Paddle(100, 560, 120, 16, 800);
        Ball ball = new Ball(8, 100, 8, paddle); // tâm = bán kính -> x=0
        ball.setStuck(false);
        ball.setVelocity(-200, -150); // đang đi sang trái
        ball.setX(-1);                // đảm bảo đang “đụng tường”
        ball.collideWithWalls(800, 600);
        assertTrue(ball.getVx() > 0, "Sau khi đập tường trái vx phải dương");
    }

    @Test
    void testClampBounceAngle() {
        Paddle paddle = new Paddle(300, 560, 120, 16, 800);
        Ball ball = new Ball(320, 550, 8, paddle);
        ball.setStuck(false);
        ball.setVelocity(500, 10); // gần như ngang
        ball.collideWithWalls(800, 600);
        double angleDeg = Math.toDegrees(Math.atan2(-ball.getVy(), ball.getVx()));
        assertTrue(Math.abs(angleDeg) >= 14.9 && Math.abs(angleDeg) <= 165.1,
                "Góc sau clamp phải trong [~15°,~165°], thực tế = " + angleDeg);
    }

    @Test
    void testPaddleBounceCenterProducesVerticalLaunch() {
        Paddle paddle = new Paddle(300, 560, 120, 16, 800);
        Ball ball = new Ball(paddle.getX() + paddle.getWidth() / 2, paddle.getY() - 10, 8, paddle);
        ball.setStuck(false);
        ball.setY(paddle.getY() - ball.getRadius() * 2 + 0.5);
        ball.setVelocity(0, 200); // hướng xuống
        ball.collideWithPaddle(paddle);
        assertTrue(ball.getVy() < 0, "Sau va chạm với paddle, bóng phải đi lên (vy < 0)");
    }

    @Test
    void testFireballDoesNotReverseVelocityOnBrick() {
        Paddle paddle = new Paddle(200, 560, 120, 16, 800);
        Ball ball = new Ball(220, 200, 8, paddle);
        ball.setFireball(true);
        ball.setStuck(false);
        ball.setVelocity(120, -180); // setVelocity() sẽ chuẩn hoá

        double vx0 = ball.getVx();
        double vy0 = ball.getVy();

        Brick normalBrick = new Brick(210, 190, 40, 20, 1);
        assertTrue(ball.collideWithBrick(normalBrick, ball.isFireball()), "Fireball phải báo va chạm");
        assertEquals(vx0, ball.getVx(), 1e-6, "Fireball không đổi vx");
        assertEquals(vy0, ball.getVy(), 1e-6, "Fireball không đổi vy");
    }
}

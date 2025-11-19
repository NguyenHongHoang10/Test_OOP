package arkanoid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BulletBrickCollisionTest {

    @BeforeAll
    static void mute() { try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {} }

    @Test
    void bulletHitsBrick_removesBrick_addsScore_killsBullet() {
        CollisionManager cm = new CollisionManager();
        EntityManager em = new EntityManager();
        GameState gs = new GameState();
        PowerUpManager pm = new PowerUpManager();
        Paddle paddle = new Paddle(300, 560, 120, 16, 800);

        Brick br = new Brick(200, 150, 40, 20, 1);
        em.addBrick(br);

        // Đạn nằm chồng gạch
        Bullet bu = new Bullet(200 + 20, 150 + 10);
        // Đẩy về góc trên trái của viên đạn để chắc chắn overlap
        bu.x = 200 + 10; bu.y = 150 + 5;
        em.addBullet(bu);

        int before = em.countRemainingDestructibleBricks();
        cm.handleCollisions(em, gs, paddle, pm, 800, 600, 1/60.0);

        int after = em.countRemainingDestructibleBricks();
        assertTrue(after == before - 1, "Phải xoá đúng 1 gạch");
        assertFalse(bu.isAlive(), "Đạn phải bị kill sau khi trúng gạch");
        // +100 điểm cho 1 viên gạch
        assertTrue(gs.getScore() >= 100, "Điểm phải tăng >= 100");
    }
}

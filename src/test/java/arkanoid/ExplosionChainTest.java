package arkanoid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExplosionChainTest {

    @BeforeAll
    static void mute() {
        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }

    @Test
    void testExplosiveChainRemovesNeighbors() {
        CollisionManager cm = new CollisionManager();
        EntityManager em = new EntityManager();
        GameState gs = new GameState();
        PowerUpManager pm = new PowerUpManager();
        Paddle paddle = new Paddle(300, 560, 120, 16, 800);

        // Theo thuật toán trong CollisionManager: stepX = w + 8 (=48), stepY = h + 6 (=26)
        Brick center = new Brick(300, 200, 40, 20, Brick.Type.EXPLOSIVE, 1);
        Brick nR = new Brick(300 + 48, 200, 40, 20, 1);
        Brick nL = new Brick(300 - 48, 200, 40, 20, 1);
        Brick nD = new Brick(300, 200 + 26, 40, 20, 1);
        Brick nU = new Brick(300, 200 - 26, 40, 20, 1);

        em.addBrick(center);
        em.addBrick(nR);
        em.addBrick(nL);
        em.addBrick(nD);
        em.addBrick(nU);

        Ball ball = new Ball(320, 210, 8, paddle);
        ball.setFireball(true);
        ball.setStuck(false);
        em.addBall(ball);

        cm.handleCollisions(em, gs, paddle, pm, 800, 600, 1/60.0);

        int remaining = em.countRemainingDestructibleBricks();
        assertTrue(remaining < 5, "Sau nổ phải xoá bớt gạch, còn lại = " + remaining);
    }
}

package arkanoid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PowerUpManagerTest {

    @BeforeAll
    static void mute() {
        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }

    @Test
    void testExpandPaddleEffectAppliesAndExpires() {
        PowerUpManager pm = new PowerUpManager();
        EntityManager em = new EntityManager();
        GameState gs = new GameState();
        Paddle paddle = new Paddle(100, 560, 120, 16, 800);

        double originalWidth = paddle.getWidth();
        pm.applyPowerUp(PowerUp.PowerType.EXPAND_PADDLE, em, gs, paddle, 600);
        assertTrue(paddle.getWidth() > originalWidth, "Paddle phải rộng hơn sau EXPAND");

        double t = 0;
        while (t < 11) {
            pm.updateActiveEffects(1, em, gs, paddle);
            t += 1;
        }
        assertEquals(originalWidth, paddle.getWidth(), 1e-3, "Sau hết hạn width phải khôi phục");
    }
}

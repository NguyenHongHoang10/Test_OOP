package arkanoid;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.hypot;
import static org.junit.jupiter.api.Assertions.*;

public class SaveLoadTest {

    @BeforeAll
    static void bootJavaFX() {
        // Khởi động JavaFX và tắt âm cho toàn bộ test
        FxTestLauncher.startup();
        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }

    @Test
    void testSnapshotAndReloadKeepsDirectionAndSpeed() throws Exception {
        // 1) Tạo Game trên FX thread
        AtomicReference<Game> refGame = new AtomicReference<>();
        CountDownLatch created = new CountDownLatch(1);
        Platform.runLater(() -> {
            Game g = new Game(800, 600, () -> {}, () -> {});
            g.startNewGame(0);
            refGame.set(g);
            created.countDown();
        });
        assertTrue(created.await(3, TimeUnit.SECONDS), "Timeout khi tạo Game");
        Game game = refGame.get();

        // 2) Lấy bóng hiện có và set trạng thái trên FX thread
        AtomicReference<Ball> refBall = new AtomicReference<>();
        CountDownLatch ballReady = new CountDownLatch(1);
        Platform.runLater(() -> {
            EntityManager em = (EntityManager) getField(game, "entityManager");
            Ball b = em.getBalls().get(0);
            b.setStuck(false);
            b.setVelocity(150, -190); // sẽ bị chuẩn hoá về base speed
            b.setFireball(true);
            refBall.set(b);
            ballReady.countDown();
        });
        assertTrue(ballReady.await(3, TimeUnit.SECONDS), "Timeout khi setup ball");
        Ball b0 = refBall.get();

        // Lưu hướng và base speed hiện tại để so sánh sau khi load
        double len0 = hypot(b0.getVx(), b0.getVy());
        double ux0 = b0.getVx() / len0;
        double uy0 = b0.getVy() / len0;
        double base0 = b0.getBaseSpeed();

        // 3) Save: API SaveLoad.save tự gọi Platform.runLater, nhưng ta flush để chắc chắn xong
        SaveLoad.get().save(game);
        FxTestLauncher.fxFlush();

        // 4) Load và chờ apply xong
        boolean ok = SaveLoad.get().loadIntoAndPrepareContinue(game);
        assertTrue(ok, "Load phải trả về true");
        FxTestLauncher.fxFlush();

        // 5) Đọc lại trạng thái trên FX thread và assert
        AtomicReference<Ball> refBall2 = new AtomicReference<>();
        CountDownLatch read = new CountDownLatch(1);
        Platform.runLater(() -> {
            EntityManager em2 = (EntityManager) getField(game, "entityManager");
            refBall2.set(em2.getBalls().get(0));
            read.countDown();
        });
        assertTrue(read.await(3, TimeUnit.SECONDS), "Timeout khi đọc lại ball");
        Ball r = refBall2.get();

        assertNotNull(r, "Phải có bóng sau khi load");
        assertTrue(r.isFireball(), "Fireball state phải giữ lại");

        // Độ lớn vận tốc phải bằng base speed (vì chuẩn hoá)
        double speed = hypot(r.getVx(), r.getVy());
        assertEquals(r.getBaseSpeed(), speed, 1e-6, "Tốc độ sau load phải bằng base speed");
        assertEquals(base0, r.getBaseSpeed(), 1e-9, "Base speed phải giữ nguyên");

        // Hướng vận tốc gần giống ban đầu (cosine similarity ~ 1)
        double len1 = hypot(r.getVx(), r.getVy());
        double ux1 = r.getVx() / len1;
        double uy1 = r.getVy() / len1;
        double cos = ux0 * ux1 + uy0 * uy1;
        assertTrue(cos > 0.999, "Hướng vận tốc sau load phải bảo toàn (cos=" + cos + ")");
    }

    private Object getField(Object target, String name) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            return null;
        }
    }
}

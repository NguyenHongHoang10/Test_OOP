package arkanoid;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestFXBoot {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void initFxOnce() throws Exception {
        if (STARTED.get()) {
            return;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Cannot initialize JavaFX Platform for tests");
            }
        } catch (IllegalStateException already) {
            // Toolkit đã khởi động (hoặc ở trạng thái sẵn sàng) -> bỏ qua
        }
        STARTED.set(true);

        try { SoundManager.get().setMuted(true); } catch (Throwable ignored) {}
    }
}

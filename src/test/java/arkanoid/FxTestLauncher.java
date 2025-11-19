package arkanoid;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Khởi động JavaFX Application Thread một lần cho môi trường test.
 * Dùng Application.launch() trong một daemon thread, rồi chờ tới khi start(Stage) được gọi.
 */
public final class FxTestLauncher {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final CountDownLatch READY = new CountDownLatch(1);

    private FxTestLauncher() {}

    public static void startup() {
        if (STARTED.get()) return;
        Thread t = new Thread(() -> Application.launch(HeadlessApp.class));
        t.setDaemon(true);
        t.start();
        try {
            if (!READY.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Không khởi động được JavaFX trong 5s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi chờ JavaFX khởi động", e);
        }
        STARTED.set(true);
    }

    public static final class HeadlessApp extends Application {
        @Override
        public void start(Stage stage) {
            // Không cần show stage trong test, chỉ cần Toolkit hoạt động
            READY.countDown();
        }
    }

    /**
     * Tiện ích flush hàng đợi JavaFX: đảm bảo mọi runLater trước đó đã chạy xong.
     */
    public static void fxFlush() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi chờ fxFlush", e);
        }
    }
}

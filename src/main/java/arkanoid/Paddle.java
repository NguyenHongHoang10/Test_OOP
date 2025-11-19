package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import java.util.HashSet;
import java.util.Set;

public class Paddle extends GameObject {
    // Tập các phím đang nhấn (LEFT/RIGHT hoặc A/D)
    private final Set<KeyCode> keys = new HashSet<>();
    // Tốc độ di chuyển theo px/giây
    private final double speed = 600;
    // Chiều rộng của khu vực chơi dùng để giới hạn paddle không đi quá biên
    private final double arenaWidth;
    private boolean hasLaser = false;
    // hiệu ứng nhấp nháy khi trúng đạn
    private boolean isBlinking = false;
    private long blinkStartTime = 0;
    private long blinkDuration = 400;

    private Image platformImage;        // Ảnh thanh đòn (phần trên, co dãn)
    private Image platformBaseImage;    // Ảnh đế (phần dưới, cố định)
    private Image platformReducedImage; // Ảnh thu nhỏ
    private Image platformEnlargedImage; // Ảnh phóng to
    private Image laserImage;           // Ảnh súng laser

    private final double initialWidth;
    double newPlatformHeight;
    private double baseWidth;     // Chiều rộng gốc của đế
    private double baseHeight;    // Chiều cao gốc của đế
    private double nativeLaserWidth;    // Chiều rộng gốc của súng
    private double nativeLaserHeight;   // Chiều cao gốc của súng

    public Paddle(double x, double y, double width, double height, double arenaWidth) {
        newPlatformHeight = height / 2.0;

        super(x, y, width, height);
        this.arenaWidth = arenaWidth;
        this.initialWidth = width;

        try {
            // (Đường dẫn dựa trên cấu trúc thư mục bạn cung cấp)
            platformImage = new Image(getClass().getResourceAsStream("/Image/Paddle/platform.png"));
            platformBaseImage = new Image(getClass().getResourceAsStream("/Image/Paddle/platform_base-Sheet.png"));
            platformReducedImage = new Image(getClass().getResourceAsStream("/Image/Paddle/platform_reduced.png"));
            platformEnlargedImage = new Image(getClass().getResourceAsStream("/Image/Paddle/platform_enlarged.png"));
            laserImage = new Image(getClass().getResourceAsStream("/Image/Paddle/laser.png"));

            // Lấy kích thước gốc của các ảnh
            double nativeBaseWidth = platformBaseImage.getWidth();
            double nativeBaseHeight = platformBaseImage.getHeight();
            nativeLaserWidth = laserImage.getWidth();           // Rộng
            nativeLaserHeight = laserImage.getHeight();         // Cao

            this.baseHeight = nativeBaseHeight / 2.0;
            this.baseWidth = nativeBaseWidth / 2.0;

            // Cập nhật chiều cao tổng thể của GameObject (dùng cho va chạm)
            this.height = newPlatformHeight + baseHeight;

        } catch (Exception e) {
            System.err.println("LỖI: Không thể tải ảnh Paddle từ /Image/Paddle/: " + e.getMessage());
            e.printStackTrace();
            // Nếu tải lỗi, sẽ dùng render() dự phòng (vẽ hình xanh)
            platformImage = null; // Đặt tất cả về null
            platformBaseImage = null;
            platformReducedImage = null;
            platformEnlargedImage = null;
            laserImage = null;
        }
    }

    // gọi paddle khi bị trúng đạn
    public void hit() {
        isBlinking = true;
        blinkStartTime = System.currentTimeMillis();
    }

    // Gọi khi phím được nhấn
    public void press(KeyCode code) {
        keys.add(code);
    }

    // Gọi khi phím được nhả
    public void release(KeyCode code) {
        keys.remove(code);
    }

    public void clearKeys() {
        keys.clear();
    }

    @Override
    public void update(double deltaTime) {
        // Di chuyển trái/phải theo tập keys
        if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.A)) {
            x -= speed * deltaTime;
        }
        if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.D)) {
            x += speed * deltaTime;
        }
        // giới hạn vùng của paddle
        if (x < 0) x = 0;
        if (x + width > arenaWidth) x = arenaWidth - width;
        // Dừng nhấp nháy sau khi hết thời gian
        if (isBlinking && System.currentTimeMillis() - blinkStartTime > blinkDuration) {
            isBlinking = false;
        }
    }

    public void setHasLaser(boolean on) {
        this.hasLaser = on;
    }

    public boolean hasLaser() {
        return this.hasLaser;
    }

    public void resetBlink() {
        isBlinking = false;
    }

    // thêm helper trả về 2 vị trí nòng súng (center x,y)
    public double[] getLaserGunPositions() {
        // trả về array length 4: [x1,y1,x2,y2]
        double gx1 = getX() + 18; // offset trái (tùy chỉnh theo width)
        double gx2 = getX() + getWidth() - 18;
        double gy = getY() - 6; // ngay trên paddle
        return new double[]{gx1, gy, gx2, gy};
    }
    @Override
    public void render(GraphicsContext gc) {
        if (platformImage != null && platformBaseImage != null
                && platformReducedImage != null && platformEnlargedImage != null) {

            // 1. Chọn ảnh platform (phần trên) dựa theo chiều rộng hiện tại
            Image imageToDraw;
            if (this.width < this.initialWidth) {
                imageToDraw = platformReducedImage;
            } else if (this.width > this.initialWidth) {
                imageToDraw = platformEnlargedImage;
            } else {
                imageToDraw = platformImage;
            }
            
            double alpha = 1.0;
            if (isBlinking && (System.currentTimeMillis() / 100) % 2 == 0) {
                alpha = 0.3; // 30% mờ (hiệu ứng nhấp nháy)
            }

            gc.save();
            gc.setGlobalAlpha(alpha);

            // Vẽ platform
            gc.drawImage(imageToDraw, x, y, width, newPlatformHeight);

            // Vẽ base
            double baseX = x + (width / 2.0) - (baseWidth / 2.0);
            double baseY = y + newPlatformHeight;
            gc.drawImage(platformBaseImage, baseX, baseY, baseWidth, baseHeight);

            //  Vẽ laser nếu có
            if (this.hasLaser && laserImage != null) {
                double renderLaserWidth = nativeLaserWidth / 4.0;
                double renderLaserHeight = nativeLaserHeight / 4.0;
                double[] pos = getLaserGunPositions();
                double laserX1 = pos[0] - (renderLaserWidth / 2.0);
                double laserX2 = pos[2] - (renderLaserWidth / 2.0);
                double laserY = pos[1] - renderLaserHeight + 8;
                gc.drawImage(laserImage, laserX1, laserY, renderLaserWidth, renderLaserHeight);
                gc.drawImage(laserImage, laserX2, laserY, renderLaserWidth, renderLaserHeight);
            }

            gc.restore();

        } else {
            // Dự phòng: vẽ hình chữ nhật khi ảnh chưa load được
            if (isBlinking && (System.currentTimeMillis() / 100) % 2 == 0) {
                gc.setFill(Color.WHITE);
            } else {
                gc.setFill(Color.DODGERBLUE);
            }
            gc.fillRoundRect(x, y, width, height, 10, 10);
        }
    }
}

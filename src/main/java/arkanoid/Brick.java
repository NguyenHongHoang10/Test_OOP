package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Brick extends GameObject {
    public enum Type {NORMAL, INDESTRUCTIBLE, EXPLOSIVE}

    public enum WarpMode {FADE, SCALE}

    private int hits; // số lần chịu va chạm trước khi bị phá
    private final Type type;

    private boolean warpAnimating = false;
    private WarpMode warpMode = WarpMode.FADE;
    private double warpDelay = 0.0;    // thời gian chờ trước khi bắt đầu (s)
    private double warpTime = 0.0;     // thời gian đã chạy animation (s)
    private double warpDuration = 0.5; // độ dài animation (s). mặc định 0.5
    private double renderAlpha = 1.0;
    private double renderScale = 1.0;
    private boolean interactive = true; // nếu false => ignore va chạm cho tới khi warp xong

    private double flashTimer = 0.0;         // thời gian còn lại để hiển thị đền flash
    public static final double FLASH_DURATION = 0.18; // độ dài flash
    private Color flashColor = Color.WHITE;

    // 1. Ảnh cho gạch thường
    private static Image imgBrick1;
    private static Image imgBrick2;
    private static Image imgBrick3;

    // 2. Ảnh gạch đặc biệt
    private static Image imgUnbreakable;

    // 3. Ảnh động cho gạch nổ
    private static final List<Image> imgExplosiveFrames = new ArrayList<>();

    // 4. Biến điều khiển animation
    private static double explosiveAnimTimer = 0.0;
    private static int explosiveAnimIndex = 0;
    private static final double EXPLOSIVE_FRAME_DURATION = 0.1; // Thời gian mỗi khung hình (0.1s)
    // Chuỗi ping-pong
    private static final int[] explosiveAnimSequence = {0, 1, 2, 3, 3, 2, 1, 0};


    static {
        // Tải 3 khung hình gạch thường
        imgBrick1 = loadImage("/Image/Brick/brick1.png");
        imgBrick2 = loadImage("/Image/Brick/brick2.png");
        imgBrick3 = loadImage("/Image/Brick/brick3.png");
        imgUnbreakable = loadImage("/Image/Brick/unbreakable_brick.png");

        // Tải 4 khung hình gạch nổ
        imgExplosiveFrames.add(loadImage("/Image/Brick/explosive_brick1.png"));
        imgExplosiveFrames.add(loadImage("/Image/Brick/explosive_brick2.png"));
        imgExplosiveFrames.add(loadImage("/Image/Brick/explosive_brick3.png"));
        imgExplosiveFrames.add(loadImage("/Image/Brick/explosive_brick4.png"));
    }

    //Hàm trợ giúp tải ảnh (trả về null nếu lỗi)
    private static Image loadImage(String path) {
        try {
            Image img = new Image(Brick.class.getResourceAsStream(path));
            if (img.isError()) {
                System.err.println("Lỗi tải ảnh Brick: " + path);
                return null;
            }
            return img;
        } catch (Exception e) {
            System.err.println("Không tìm thấy tài nguyên Brick: " + path);
            return null;
        }
    }

    // Cập nhật animation gạch nổ (gọi mỗi frame từ Game)
    public static void updateAnimation(double dt) {
        explosiveAnimTimer += dt;
        if (explosiveAnimTimer >= EXPLOSIVE_FRAME_DURATION) {
            explosiveAnimTimer -= EXPLOSIVE_FRAME_DURATION;
            // Chuyển sang khung hình tiếp theo trong chuỗi 8 frame
            explosiveAnimIndex = (explosiveAnimIndex + 1) % explosiveAnimSequence.length;
        }
    }

    // Constructor mặc định cho gạch bình thường
    public Brick(double x, double y, double width, double height, int hits) {
        super(x, y, width, height);
        this.hits = Math.max(1, hits);
        this.type = Type.NORMAL;
    }

    // Constructor cho phép chỉ định type (INDESTRUCTIBLE, EXPLOSIVE, ...)
    public Brick(double x, double y, double width, double height, Type type, int hits) {
        super(x, y, width, height);
        this.type = type;
        if (type == Type.INDESTRUCTIBLE) {
            this.hits = Integer.MAX_VALUE;
        } else {
            this.hits = Math.max(1, hits);
        }
    }

    // Màu theo số lần va chạm để dễ phân biệt
    protected Color colorByHits(int hits) {
        switch (hits) {
            case 1:
                return Color.LIGHTGREEN;
            case 2:
                return Color.GOLD;
            case 3:
                return Color.ORANGERED;
            default:
                return Color.GRAY;
        }
    }

    @Override
    public void update(double deltaTime) {
        updateFlash(deltaTime);
    }

    // render brick với màu dựa trên số hits
    @Override
    public void render(GraphicsContext gc) {
        // lưu trạng thái
        gc.save();

        gc.setGlobalAlpha(renderAlpha);

        // áp dụng tỉ lệ xung quanh tâm
        double cx = getX() + getWidth() / 2.0;
        double cy = getY() + getHeight() / 2.0;
        gc.translate(cx, cy);
        gc.scale(renderScale, renderScale);
        gc.translate(-cx, -cy);

        Image imageToDraw = null;

        if (type == Type.INDESTRUCTIBLE) {
            imageToDraw = imgUnbreakable;
        } else if (type == Type.EXPLOSIVE) {
            // Lấy index ảnh từ chuỗi
            int frameIndex = explosiveAnimSequence[explosiveAnimIndex];
            if (frameIndex < imgExplosiveFrames.size()) {
                imageToDraw = imgExplosiveFrames.get(frameIndex);
            }
        } else { // Gạch thường
            switch (hits) {
                case 1:
                    imageToDraw = imgBrick1;
                    break;
                case 2:
                    imageToDraw = imgBrick2;
                    break;
                case 3:
                    imageToDraw = imgBrick3;
                    break;
                default:
                    imageToDraw = imgBrick3;
            }
        }
        // Bắt đầu vẽ
        if (imageToDraw != null) {
            // Vẽ ảnh đã chọn
            gc.drawImage(imageToDraw, x, y, width, height);
        } else {
            // Nếu ảnh tải lỗi, vẽ màu như cũ
            Color fill;
            switch (type) {
                case INDESTRUCTIBLE:
                    fill = Color.DARKSLATEGRAY;
                    break;
                case EXPLOSIVE:
                    fill = Color.CRIMSON;
                    break;
                default:
                    fill = colorByHits(hits);
            }
            gc.setFill(fill);
            gc.fillRoundRect(x, y, width, height, 6, 6);
            gc.setStroke(Color.DARKGRAY);
            gc.strokeRoundRect(x, y, width, height, 6, 6);
        }

        if (flashTimer > 0.0001) {
            double alpha = flashTimer / FLASH_DURATION;
            gc.save();
            gc.setGlobalAlpha(alpha * 0.95);
            gc.setFill(flashColor);
            gc.fillRect(getX(), getY(), getWidth(), getHeight());
            gc.setGlobalAlpha(1.0);
            gc.restore();
        }

        gc.restore();

        // reset global alpha để không ảnh hưởng phần vẽ sau
        gc.setGlobalAlpha(1.0);
    }

    // Giảm số lần chịu đòn; trả true nếu brick bị phá hoàn toàn
    public boolean hit() {
        if (type == Type.INDESTRUCTIBLE) return false;
        hits--;
        return hits <= 0;
    }

    // Getter số hits
    public int getHits() {
        return this.hits;
    }

    // Không phá hủy gạch nếu hits giảm xuống 1.
    public void weaken() {
        if (this.type == Type.NORMAL && this.hits > 1) {
            this.hits = this.hits - 1;
        }
        this.flashTimer = FLASH_DURATION;
    }

    public boolean isDestructible() {
        return type != Type.INDESTRUCTIBLE;
    }

    public Type getType() {
        return type;
    }

    // Khởi động warp cho viên gạch này
    public void startWarp(WarpMode mode, double delay, double duration) {
        this.warpMode = mode;
        this.warpDelay = Math.max(0.0, delay);
        this.warpDuration = Math.max(0.01, duration);
        this.warpTime = 0.0;
        this.warpAnimating = true;
        this.renderAlpha = (mode == WarpMode.FADE ? 0.0 : 1.0);
        this.renderScale = (mode == WarpMode.SCALE ? 0.0 : 1.0);
        this.interactive = false;
    }

    // gọi mỗi frame để cập nhật animation
    public void updateWarp(double dt) {
        if (!warpAnimating) return;
        if (warpDelay > 0.0) {
            warpDelay -= dt;
            if (warpDelay > 0.0) return;
        }
        warpTime += dt;
        double t = Math.min(1.0, warpTime / warpDuration);

        if (warpMode == WarpMode.FADE) {
            renderAlpha = easeOutCubic(t);
            renderScale = 1.0;
        } else {
            renderScale = easeOutBack(t);
            renderAlpha = 1.0;
        }

        if (t >= 1.0) {
            // kết thúc animation
            warpAnimating = false;
            renderAlpha = 1.0;
            renderScale = 1.0;
            interactive = true; // bật tương tác trở lại
        }
    }

    // các hàm trợ giúp dễ dàng
    private double easeOutCubic(double x) {
        double t = x - 1.0;
        return 1.0 + t * t * t;
    }

    // vượt quá một chút cho tỷ lệ pop
    private double easeOutBack(double x) {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    // getter dùng bởi Game collision logic
    public boolean isWarpAnimating() {
        return warpAnimating;
    }

    // gọi điều này khi gạch sẽ nhấp nháy
    public void flash(Color color, double duration) {
        this.flashColor = color != null ? color : javafx.scene.paint.Color.WHITE;
        this.flashTimer = Math.max(this.flashTimer, duration);
    }

    // gọi mỗi khung hình trong bản cập nhật gạch
    public void updateFlash(double dt) {
        if (flashTimer > 0.0) {
            flashTimer = Math.max(0.0, flashTimer - dt);
        }
    }

}

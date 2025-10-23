package arkanoid;

import java.util.ArrayList;
import java.util.List;

///GameSnapshot dùng cho Save/Load toàn bộ trận.

public class GameSnapshot {

    // Thông tin chung
    public String playerName = "";  // tên người chơi
    public int lives;
    public int score;
    public double scoreMultiplier;
    public int currentLevelIndex;
    public boolean barrierActive;
    public double barrierY;

    // Thanh đỡ
    public double paddleX;
    public double paddleY;
    public double paddleWidth;
    public double paddleHeight;
    public boolean paddleHasLaser;

    // Danh sách thực thể
    public final List<BallData> balls = new ArrayList<>();
    public final List<BrickData> bricks = new ArrayList<>();
    public final List<PowerUpData> powerUps = new ArrayList<>();
    public final List<BulletData> bullets = new ArrayList<>();
    public final List<EffectData> effects = new ArrayList<>();

    // Dữ liệu từng loại thực thể
    public static class BallData {
        public double cx, cy;          // tâm bóng
        public double radius;
        public double vx, vy;
        public double baseSpeed;
        public boolean stuckToPaddle;
        public boolean fireball;
    }

    public static class BrickData {
        public double x, y, w, h;
        public String type; // kiểu gạch
        public int hits;
    }

    public static class PowerUpData {
        public double cx, cy;
        public String type; // kiểu vật phẩm
    }

    public static class BulletData {
        public double cx, cy; // tâm đạn
    }

    ///EffectData (Dữ liệu hiệu ứng đang hoạt động)

    public static class EffectData {
        public String type;         // tên hiệu ứng
        public double remaining;    // thời gian còn lại (tính theo giây)
        public double originalPaddleWidth = -1;
        public boolean originalHasLaser = false;
        public double originalScoreMultiplier = 1.0;

        // Lưu trạng thái ban đầu theo chỉ số bóng để khôi phục khi hiệu ứng kết thúc
        public final List<IntDouble> originalSpeeds = new ArrayList<>();
        public final List<IntDouble> originalRadii = new ArrayList<>();
        public final List<IntBool> originalFireball = new ArrayList<>();
    }

    // Class cặp index-value dùng cho việc khôi phục
    public static class IntDouble {
        public int index;
        public double value;
        public IntDouble() {}
        public IntDouble(int index, double value) { this.index = index; this.value = value; }
    }

    public static class IntBool {
        public int index;
        public boolean value;
        public IntBool() {}
        public IntBool(int index, boolean value) { this.index = index; this.value = value; }
    }
}

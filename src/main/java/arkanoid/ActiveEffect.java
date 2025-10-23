package arkanoid;

import java.util.HashMap;
import java.util.Map;

// Được tách ra từ lớp nội bộ private static class Game.ActiveEffect
public class ActiveEffect {
    PowerUp.PowerType type;
    double remaining; // seconds

    //lưu trữ các giá trị ban đầu để phục hồi
    double originalScoreMultiplier = 1.0;
    double originalPaddleWidth = -1;
    Map<Ball, Double> originalSpeeds = new HashMap<>();
    Map<Ball, Double> originalRadii = new HashMap<>();
    public boolean originalHasLaser = false;
    public Map<Ball, Boolean> originalFireball = new HashMap<>();


    ActiveEffect(PowerUp.PowerType type, double duration) {
        this.type = type; this.remaining = duration;
    }

    void update(double dt) {
        remaining -= dt;
    }

    boolean isExpired() { return remaining <= 0; }
}
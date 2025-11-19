package arkanoid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityManager {
    private final List<Ball> balls = new ArrayList<>();
    private final List<Brick> bricks = new ArrayList<>();
    private final List<FlyingBrick> flyingBricks = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<HUDMessage> hudMessages = new ArrayList<>();

    public EntityManager() {
    }

    // Cập nhật các đối tượng cần update
    public void updateAll(double dt) {

        // Cập nhật power-up đang rơi
        Iterator<PowerUp> pIt = powerUps.iterator();
        while (pIt.hasNext()) {
            PowerUp pu = pIt.next();
            pu.update(dt);
            if (pu.y > 700) { // Ra khỏi màn hình
                pIt.remove();
            }
        }

        // Cập nhật đạn
        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet bullet = bit.next();
            bullet.update(dt);
            if (!bullet.isAlive()) {
                bit.remove();
            }
        }

        // Cập nhật tin nhắn HUD
        Iterator<HUDMessage> hIt = hudMessages.iterator();
        while (hIt.hasNext()) {
            HUDMessage hm = hIt.next();
            hm.update(dt);
            if (!hm.isAlive()) {
                hIt.remove();
            }
        }
    }

    // Getters
    public List<Ball> getBalls() {
        return balls;
    }

    public List<Brick> getBricks() {
        return bricks;
    }

    public List<FlyingBrick> getFlyingBricks() {
        return flyingBricks;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public List<ActiveEffect> getActiveEffects() {
        return activeEffects;
    }

    public List<HUDMessage> getHudMessages() {
        return hudMessages;
    }

    // Adders
    public void addBall(Ball b) {
        balls.add(b);
    }

    public void addBrick(Brick b) {
        bricks.add(b);
    }

    public void addPowerUp(PowerUp p) {
        powerUps.add(p);
    }

    public void addBullet(Bullet b) {
        bullets.add(b);
    }

    public void addActiveEffect(ActiveEffect e) {
        activeEffects.add(e);
    }

    public void addHUDMessage(HUDMessage h) {
        hudMessages.add(h);
    }

    // Removers
    public void removeActiveEffect(ActiveEffect e) {
        activeEffects.remove(e);
    }

    // Clearers
    public void clearAll() {
        balls.clear();
        bricks.clear();
        powerUps.clear();
        bullets.clear();
        activeEffects.clear();
        hudMessages.clear();
    }

    public void clearBricks() {
        bricks.clear();
    }

    public void clearBalls() {
        balls.clear();
    }

    // Đếm số gạch có thể phá còn lại
    public int countRemainingDestructibleBricks() {
        int cnt = 0;
        for (Brick b : bricks) {
            if (b.isDestructible()) cnt++;
        }
        return cnt;
    }
}

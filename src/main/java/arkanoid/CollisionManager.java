package arkanoid;

import javafx.scene.paint.Color;

import java.util.*;

public class CollisionManager {
    // Hiệu ứng nổ trực quan
    private final List<Explosion> explosions;
    // HUD hiện điểm số
    private final List<ScorePopup> scorePopups = new ArrayList<>();

    // particle emitter
    private ParticleEmitter emitter;
    private DebrisEmitter debrisEmitter;
    // rung màn hình / choáng
    private double shakeTime = 0;
    private double shakeDuration = 0;
    private double shakeMagnitude = 0;
    private double flashAlpha = 0;


    public CollisionManager() {
        explosions = new ArrayList<>();
        this.emitter = new ParticleEmitter();
        this.debrisEmitter = new DebrisEmitter();
    }

    public ParticleEmitter getEmitter() {
        return emitter;
    }

    public DebrisEmitter getDebrisEmitter() {
        return debrisEmitter;
    }

    public double getFlashAlpha() {
        return flashAlpha;
    }

    public List<Explosion> getExplosions() {
        return explosions;
    }

    public List<ScorePopup> getScorePopups() {
        return scorePopups;
    }

    public double getShakeTime() {
        return shakeTime;
    }

    public double getShakeDuration() {
        return shakeDuration;
    }

    public double getShakeMagnitude() {
        return shakeMagnitude;
    }

    public void setShakeTime(double shakeTime) {
        this.shakeTime = shakeTime;
    }

    public void setShakeDuration(double shakeDuration) {
        this.shakeDuration = shakeDuration;
    }

    public void setShakeMagnitude(double shakeMagnitude) {
        this.shakeMagnitude = shakeMagnitude;
    }

    public void setFlashAlpha(double flashAlpha) {
        this.flashAlpha = flashAlpha;
    }

    //Xử lý tất cả các va chạm trong game và trả về kết quả (sự kiện).
    public CollisionResult handleCollisions(EntityManager entities, GameState state, Paddle paddle,
                                            PowerUpManager powerUpManager, double gameWidth, double gameHeight, double dt) {

        CollisionResult result = new CollisionResult();
        List<Ball> ballsToRemove = new ArrayList<>();

        // 1. Va chạm của Bóng
        for (Ball bl : entities.getBalls()) {
            bl.update(dt);
            bl.collideWithWalls(gameWidth, gameHeight);
            bl.collideWithPaddle(paddle);

            // Bóng vs Gạch
            handleBallBrickCollisions(bl, entities, state, powerUpManager);

            if (state.isBarrierActive() && bl.getY() > gameHeight - 40) {
                bl.bounceUp(); // Nảy lên
                SoundManager.get().play(SoundManager.Sfx.BARRIER_BREAK);
                bl.setPositionY(state.getBarrierY() - bl.getRadius() - 30); // Đặt lại vị trí
                state.consumeBarrier(); // Dùng mất rào chắn
            }

            // cập nhật score popups
            java.util.Iterator<ScorePopup> spIt = scorePopups.iterator();
            while (spIt.hasNext()) {
                ScorePopup sp = spIt.next();
                sp.update(dt);
                if (!sp.isAlive()) spIt.remove();
            }

            // Cập nhật các explosion
            Iterator<Explosion> expIt = explosions.iterator();
            while (expIt.hasNext()) {
                Explosion ex = expIt.next();
                ex.update(dt);
                if (!ex.isAlive()) expIt.remove();
            }

            // Bóng vs Đáy
            if (bl.getY() > gameHeight) {
                ballsToRemove.add(bl); // Đánh dấu để xóa
            }
        }
        entities.getBalls().removeAll(ballsToRemove);

        // 2. Va chạm của Đạn
        handleBulletBrickCollisions(entities, state, powerUpManager);

        // 3. Va chạm của PowerUp (với Paddle)
        Iterator<PowerUp> pIt = entities.getPowerUps().iterator();
        while (pIt.hasNext()) {
            PowerUp pu = pIt.next();
            if (pu.collidesWithPaddle(paddle)) {
                result.collectedPowerUps.add(pu);
                pIt.remove();
            }
        }

        // 4. Kiểm tra điều kiện Thắng (hết gạch)
        if (entities.countRemainingDestructibleBricks() == 0) {
            result.allDestructibleBricksCleared = true;
        }

        // 5. Kiểm tra điều kiện Thua (mất bóng cuối cùng)
        if (entities.getBalls().isEmpty() && !ballsToRemove.isEmpty()) {
            result.ballLost = true; // Quả bóng cuối cùng vừa bị mất
        }

        return result;
    }

    // Xử lý va chạm giữa 1 quả bóng và tất cả các viên gạch
    private void handleBallBrickCollisions(Ball bl, EntityManager entities, GameState state, PowerUpManager powerUpManager) {
        Iterator<Brick> it = entities.getBricks().iterator();
        while (it.hasNext()) {
            Brick b = it.next();
            if (bl.collideWithBrick(b, bl.isFireball())) {
                Brick.Type t = b.getType();

                if (bl.isFireball()) {
                    // Fireball phá hủy ngay lập tức
                    double bx = b.getX() + b.getWidth() / 2.0;
                    double by = b.getY() + b.getHeight() / 2.0;
                    if (t == Brick.Type.EXPLOSIVE) {
                        handleExplosion(b, entities, state); // Kích nổ
                        SoundManager.get().play(SoundManager.Sfx.EXPLOSION);
                    } else if (t == Brick.Type.INDESTRUCTIBLE) {
                        SoundManager.get().play(SoundManager.Sfx.BOUNCE_PADDLE);
                        // Không phá hủy gạch bất tử
                        continue;
                    } else {
                        SoundManager.get().play(SoundManager.Sfx.BRICK_BREAK);
                        it.remove();
                    }
                    state.addScore(100);
                    powerUpManager.trySpawnPowerUp(bx, by, entities);
                } else {
                    if (t == Brick.Type.INDESTRUCTIBLE) {
                        // Gạch bất tử: chỉ nảy lại
                        SoundManager.get().play(SoundManager.Sfx.BOUNCE_PADDLE);
                        continue;
                    } else if (t == Brick.Type.EXPLOSIVE) {
                        SoundManager.get().play(SoundManager.Sfx.EXPLOSION);
                    } else {
                        SoundManager.get().play(SoundManager.Sfx.BRICK_BREAK);
                    }
                    // Gạch thường
                    boolean removed = b.hit(); // Gạch bị đánh
                    if (removed) {
                        // Gạch bị phá hủy
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        it.remove();
                        state.addScore(100);
                        powerUpManager.trySpawnPowerUp(bx, by, entities);
                        // Tạo hiệu ứng nổ cho viên trung tâm
                        double maxR = Math.max(b.getWidth(), b.getHeight()) * 1.8;

                        debrisEmitter.emitDebris(bx, by, b.getWidth(), b.getHeight(), 20, Color.rgb(0, 255, 255));

                        int displayed = (int) Math.round(100 * state.getScoreMultiplier());
                        scorePopups.add(new ScorePopup(bx, by - 6, "+" + displayed)); // -6 để nhấc popup lên hơi trên brick
                        if (t == Brick.Type.EXPLOSIVE) {
                            handleExplosion(b, entities, state); // Kích nổ
                        }
                    } else {
                        // Gạch chỉ bị trúng (chưa vỡ)
                        if (b.isDestructible()) state.addScore(50);
                    }

                }
                break; // Chỉ xử lý 1 va chạm gạch mỗi bóng, mỗi khung hình
            }
        }
    }

    // Xử lý va chạm giữa đạn và gạch
    private void handleBulletBrickCollisions(EntityManager entities, GameState state, PowerUpManager powerUpManager) {
        Iterator<Bullet> bit = entities.getBullets().iterator();
        while (bit.hasNext()) {
            Bullet bullet = bit.next();
            if (!bullet.isAlive()) continue;

            Iterator<Brick> bIt2 = entities.getBricks().iterator();
            while (bIt2.hasNext()) {
                Brick br2 = bIt2.next();
                if (bullet.collidesWithBrick(br2)) {
                    SoundManager.get().play(SoundManager.Sfx.LASER_SHOT);
                    // Đạn phá hủy gạch ngay lập tức
                    double bx = br2.getX() + br2.getWidth() / 2.0;
                    double by = br2.getY() + br2.getHeight() / 2.0;
                    bIt2.remove();
                    state.addScore(100);
                    powerUpManager.trySpawnPowerUp(bx, by, entities);
                    bullet.kill(); // Đạn biến mất
                    break; // Đạn chỉ trúng 1 gạch
                }
            }
        }
    }

    // Xử lý logic gạch nổ
    private void handleExplosion(Brick center, EntityManager entities, GameState state) {
        double stepX = center.getWidth() + 8;
        double stepY = center.getHeight() + 6;
        int[] dir = new int[]{-1, 0, 1};
        Queue<Brick> q = new LinkedList<>();
        Set<Brick> visited = new HashSet<>();
        Set<Brick> toRemoveSet = new HashSet<>();

        q.add(center);
        visited.add(center);

        while (!q.isEmpty()) {
            Brick cur = q.poll();
            if (cur.isDestructible()) toRemoveSet.add(cur);

            double cx = cur.getX() + cur.getWidth() / 2.0;
            double cy = cur.getY() + cur.getHeight() / 2.0;

            // 1) 8 gạch lân cận để kích nổ dây chuyền
            for (int dx : dir) {
                for (int dy : dir) {
                    if (dx == 0 && dy == 0) continue;
                    double tx = cx + dx * stepX;
                    double ty = cy + dy * stepY;

                    for (Brick b : entities.getBricks()) {
                        if (visited.contains(b)) continue;
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        double tolX = b.getWidth() * 0.6;
                        double tolY = b.getHeight() * 0.6;
                        if (Math.abs(bx - tx) <= tolX && Math.abs(by - ty) <= tolY) {
                            if (b.getType() == Brick.Type.EXPLOSIVE) {
                                visited.add(b);
                                q.add(b);
                            }
                            break;
                        }
                    }
                }
            }

            // 2) 4 hướng nổ (2 ô)
            int[][] card = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : card) {
                int dx = d[0], dy = d[1];
                for (int step = 1; step <= 2; step++) {
                    double tx = cx + dx * step * stepX;
                    double ty = cy + dy * step * stepY;
                    Brick found = null;
                    for (Brick b : entities.getBricks()) {
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        double tolX = b.getWidth() * 0.6;
                        double tolY = b.getHeight() * 0.6;
                        if (Math.abs(bx - tx) <= tolX && Math.abs(by - ty) <= tolY) {
                            found = b;
                            break;
                        }
                    }
                    if (found != null) {
                        if (found.getType() == Brick.Type.EXPLOSIVE && !visited.contains(found)) {
                            visited.add(found);
                            q.add(found);
                        }
                        if (found.isDestructible()) {
                            toRemoveSet.add(found);
                            double fx = found.getX() + found.getWidth() / 2.0;
                            double fy = found.getY() + found.getHeight() / 2.0;
                            double mR = Math.max(found.getWidth(), found.getHeight()) * 1.6;
                            explosions.add(new Explosion(fx, fy, mR, 0.45));
                        }
                    }
                }
            }
        }

        // Xóa tất cả các gạch trong vụ nổ
        for (Brick b : toRemoveSet) {
            // tạo hiệu ứng nổ nếu chưa có (đảm bảo center cũng có)
            double bx = b.getX() + b.getWidth() / 2.0;
            double by = b.getY() + b.getHeight() / 2.0;
            explosions.add(new Explosion(bx, by, Math.max(b.getWidth(), b.getHeight()) * 1.6, 0.45));
            // vị trí trung tâm của viên nổ
            double cx = center.getX() + center.getWidth() / 2.0;
            double cy = center.getY() + center.getHeight() / 2.0;
            // hạt nổ li ti
            emitter.emitExplosion(cx, cy, 28);
            emitter.emitSmoke(cx, cy + 6, 6);
            // rung màn hình
            shakeDuration = 0.45;
            shakeTime = shakeDuration;
            shakeMagnitude = 10;
            // choáng
            flashAlpha = Math.max(flashAlpha, 0.85);
            if (entities.getBricks().remove(b)) {
                state.addScore(100);
            }
        }
    }
}

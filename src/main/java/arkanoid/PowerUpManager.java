package arkanoid;

import java.util.*;
import arkanoid.SoundManager;

public class PowerUpManager {
    private final Random random = new Random();

    // weaken / shockwave
    private boolean weakenInProgress = false;
    private Shockwave currentShockwave = null;
    // bộ gạch đã bị ảnh hưởng bởi sóng xung kích
    private final Set<Brick> shockAffected = new HashSet<>();

    // Next-level warp (portal) effect
    private boolean nextLevelInProgress = false;
    private double portalX = 0;
    private double portalY = 0;
    private double portalBaseRadius = 10.0;
    private double portalGlow = 0.0;
    private double whiteFlashAlpha = 0.0;
    private final double WHITE_FLASH_DURATION = 0.32; // thời gian fade trắng
    private boolean whiteFlashActive = false;

    public PowerUpManager() {
    }

    public boolean isWeakenInProgress() {
        return weakenInProgress;
    }

    public Shockwave getCurrentShockwave() {
        return currentShockwave;
    }

    public boolean isNextLevelInProgress() {
        return nextLevelInProgress;
    }

    public Set<Brick> getShockAffected() {
        return shockAffected;
    }

    public double getPortalX() {
        return portalX;
    }

    public double getPortalY() {
        return portalY;
    }

    public double getPortalGlow() {
        return portalGlow;
    }

    public double getWhiteFlashAlpha() {
        return whiteFlashAlpha;
    }

    public double getWHITE_FLASH_DURATION() {
        return WHITE_FLASH_DURATION;
    }

    public boolean isWhiteFlashActive() {
        return whiteFlashActive;
    }

    public double getPortalBaseRadius() {
        return portalBaseRadius;
    }



    public void setWeakenInProgress(boolean weakenInProgress) {
        this.weakenInProgress = weakenInProgress;
    }

    public void setCurrentShockwave(Shockwave currentShockwave) {
        this.currentShockwave = currentShockwave;
    }

    public void setNextLevelInProgress(boolean nextLevelInProgress) {
        this.nextLevelInProgress = nextLevelInProgress;
    }

    public void setPortalX(double portalX) {
        this.portalX = portalX;
    }

    public void setPortalY(double portalY) {
        this.portalY = portalY;
    }

    public void setPortalBaseRadius(double portalBaseRadius) {
        this.portalBaseRadius = portalBaseRadius;
    }

    public void setPortalGlow(double portalGlow) {
        this.portalGlow = portalGlow;
    }

    public void setWhiteFlashAlpha(double whiteFlashAlpha) {
        this.whiteFlashAlpha = whiteFlashAlpha;
    }

    public void setWhiteFlashActive(boolean whiteFlashActive) {
        this.whiteFlashActive = whiteFlashActive;
    }

    // Cập nhật các hiệu ứng đang hoạt động và xử lý khi chúng hết hạn
    public void updateActiveEffects(double dt, EntityManager entities, GameState state, Paddle paddle) {
        Iterator<ActiveEffect> effIt = entities.getActiveEffects().iterator();
        while (effIt.hasNext()) {
            ActiveEffect ae = effIt.next();
            ae.update(dt);
            if (ae.isExpired()) {
                // Khôi phục trạng thái ban đầu
                switch (ae.type) {
                    case SHRINK_PADDLE:
                    case EXPAND_PADDLE:
                        if (ae.originalPaddleWidth > 0) paddle.setWidth(ae.originalPaddleWidth);
                        break;
                    case TINY_BALL:
                        for (Map.Entry<Ball, Double> e : ae.originalRadii.entrySet()) {
                            Ball bl = e.getKey();
                            if (entities.getBalls().contains(bl)) bl.setRadius(e.getValue());
                        }
                        break;
                    case SLOW_BALL:
                    case FAST_BALL:
                        for (Map.Entry<Ball, Double> e : ae.originalSpeeds.entrySet()) {
                            Ball bl = e.getKey();
                            if (entities.getBalls().contains(bl)) bl.setBaseSpeed(e.getValue());
                        }
                        break;
                    case LASER_PADDLE:
                        paddle.setHasLaser(ae.originalHasLaser);
                        break;
                    case FIREBALL:
                        for (Map.Entry<Ball, Boolean> e : ae.originalFireball.entrySet()) {
                            Ball bl = e.getKey();
                            if (entities.getBalls().contains(bl)) bl.setFireball(e.getValue());
                        }
                        break;
                    case SCORE_MULTIPLIER:
                        state.setScoreMultiplier(ae.originalScoreMultiplier);
                        break;
                    default:
                        break;
                }
                effIt.remove();
            }
        }
    }

    public boolean checkActiveEffect(PowerUp.PowerType type, EntityManager entities, double time) {
        for (ActiveEffect ae : entities.getActiveEffects()) {
            if (ae.type == type) {
                ae.setRemaining(ae.remaining + time);
                return true;
            }
        }
        return false;
    }

    // Được gọi khi paddle nhặt được power-up
    public void applyPowerUp(PowerUp.PowerType type, EntityManager entities, GameState state, Paddle paddle, double gameHeight) {
        switch (type) {
            case SHRINK_PADDLE: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_BAD);
                if (!checkActiveEffect(type, entities, 10.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 10.0);
                    eff.originalPaddleWidth = 120;
                    paddle.setWidth(eff.originalPaddleWidth * 0.7);
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("SHRINK PADDLE +10s", 2.5));
                }
                break;
            }
            case EXPAND_PADDLE: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 10.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 10.0);
                    eff.originalPaddleWidth = 120;
                    paddle.setWidth(eff.originalPaddleWidth * 1.4);
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("EXPAND PADDLE +10s", 2.5));
                }
                break;
            }
            case TINY_BALL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 10.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 10.0);
                    for (Ball bl : entities.getBalls()) {
                        eff.originalRadii.put(bl, bl.getRadius());
                        bl.setRadius(bl.getRadius() * 0.6);
                    }
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("TINY BALL +10s", 2.5));
                }
                break;
            }
            case MULTI_BALL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                SoundManager.get().play(SoundManager.Sfx.MULTIBALL);
                List<Ball> newBalls = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    double existingSpeed = 350; // Tốc độ mặc định phòng trường hợp không tìm thấy bóng
                    if (!entities.getBalls().isEmpty()) {
                        // Lấy tốc độ của quả bóng đầu tiên trong danh sách
                        existingSpeed = entities.getBalls().get(0).getBaseSpeed();
                    }
                    Ball nb = new Ball(paddle.getX() + paddle.getWidth() / 2, paddle.getY() - 10, 8, paddle);
                    nb.setBaseSpeed(existingSpeed);
                    applyActiveEffectsToBall(nb, entities.getActiveEffects()); // Áp dụng hiệu ứng hiện có cho bóng mới

                    double angle = Math.toRadians(20) * (i == 0 ? -1 : 1);
                    double speedVal = nb.getBaseSpeed();
                    double vxNew = Math.sin(angle) * speedVal;
                    double vyNew = -Math.cos(angle) * speedVal;
                    nb.setVelocity(vxNew, vyNew);
                    nb.setStuck(false);
                    newBalls.add(nb);
                }
                entities.getBalls().addAll(newBalls);
                entities.addHUDMessage(new HUDMessage("MULTI BALL", 2.5));
                break;
            }
            case SLOW_BALL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 8.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 8.0);
                    for (Ball bl : entities.getBalls()) {
                        eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                        bl.setBaseSpeed(bl.getBaseSpeed() * 0.7);
                    }
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("SLOW BALL +8s", 2.5));
                }
                break;
            }
            case FAST_BALL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_BAD);
                if (!checkActiveEffect(type, entities, 8.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 8.0);
                    for (Ball bl : entities.getBalls()) {
                        eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                        bl.setBaseSpeed(bl.getBaseSpeed() * 1.3);
                    }
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("FAST BALL +8s", 2.5));
                }
                break;
            }
            case NEXT_LEVEL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                entities.addHUDMessage(new HUDMessage("NEXT LEVEL", 2.5));
                triggerNextLevelEffect(-1, -1, entities);
                SoundManager.get().play(SoundManager.Sfx.PORTAL);
                break;
            }
            case EXTRA_LIFE: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                state.incrementLives();
                entities.addHUDMessage(new HUDMessage("+1 LIFE", 2.5));
                break;
            }
            case SUDDEN_DEATH: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_BAD);
                state.setLives(Math.max(1, state.getLives()));
                entities.addHUDMessage(new HUDMessage("SUDDEN DEATH", 2.5));
                break;
            }
            case LASER_PADDLE: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 10.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 10.0);
                    eff.originalHasLaser = paddle.hasLaser();
                    paddle.setHasLaser(true);
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("LASER PADDLE +10s", 2.5));
                }
                break;
            }
            case FIREBALL: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 8.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 8.0);
                    for (Ball bl : entities.getBalls()) {
                        eff.originalFireball.put(bl, bl.isFireball());
                        bl.setFireball(true);
                    }
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("FIREBALL +8s", 2.5));
                }
                break;
            }
            case BARRIER: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                SoundManager.get().play(SoundManager.Sfx.BARRIER_ON);
                state.setBarrierActive(true, gameHeight - 24); // Đặt rào chắn gần đáy
                entities.addHUDMessage(new HUDMessage("BARRIER (1 lần)", 2.5));
                break;
            }
            case WEAKEN: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                SoundManager.get().play(SoundManager.Sfx.SHOCKWAVE);
                int reduced = 0;
                for (Brick br : entities.getBricks()) {
                    if (br.getType() == Brick.Type.NORMAL && br.getHits() > 1) {
                        br.weaken();
                        reduced++;
                    }
                }
                triggerWeaken(paddle);
                entities.addHUDMessage(new HUDMessage("WEAKEN: -" + reduced + " hits", 2.5));
                break;
            }
            case SCORE_MULTIPLIER: {
                SoundManager.get().play(SoundManager.Sfx.POWER_PICK_GOOD);
                if (!checkActiveEffect(type, entities, 15.0)) {
                    ActiveEffect eff = new ActiveEffect(type, 15.0);
                    eff.originalScoreMultiplier = state.getScoreMultiplier();
                    state.setScoreMultiplier(state.getScoreMultiplier() * 2.0);
                    entities.addActiveEffect(eff);
                    entities.addHUDMessage(new HUDMessage("SCORE x2 -15s", 2.5));
                }
                break;
            }
        }
    }

    // Được gọi khi một viên gạch vỡ
    public void trySpawnPowerUp(double x, double y, EntityManager entities) {
        double spawnChance = 0.3; // 30% chance
        if (random.nextDouble() > spawnChance) return;

        // Trọng số (giữ nguyên từ Game.java)
        Map<PowerUp.PowerType, Double> weights = new LinkedHashMap<>();
        weights.put(PowerUp.PowerType.SHRINK_PADDLE, 0.5);
        weights.put(PowerUp.PowerType.EXPAND_PADDLE, 0.5);
        weights.put(PowerUp.PowerType.TINY_BALL, 0.8);
        weights.put(PowerUp.PowerType.MULTI_BALL, 0.6);
        weights.put(PowerUp.PowerType.SLOW_BALL, 0.8);
        weights.put(PowerUp.PowerType.FAST_BALL, 0.6);
        weights.put(PowerUp.PowerType.NEXT_LEVEL, 0.2);
        weights.put(PowerUp.PowerType.EXTRA_LIFE, 0.4);
        weights.put(PowerUp.PowerType.SUDDEN_DEATH, 0.3);
        weights.put(PowerUp.PowerType.LASER_PADDLE, 0.5);
        weights.put(PowerUp.PowerType.FIREBALL, 0.5);
        weights.put(PowerUp.PowerType.BARRIER, 0.6);
        weights.put(PowerUp.PowerType.WEAKEN, 0.7);
        weights.put(PowerUp.PowerType.SCORE_MULTIPLIER, 0.7);

        double total = 0;
        for (double v : weights.values()) total += v;
        double r = random.nextDouble() * total;
        double cum = 0;
        PowerUp.PowerType chosen = PowerUp.PowerType.SHRINK_PADDLE;
        for (Map.Entry<PowerUp.PowerType, Double> e : weights.entrySet()) {
            cum += e.getValue();
            if (r <= cum) {
                chosen = e.getKey();
                break;
            }
        }

        entities.addPowerUp(new PowerUp(x, y, chosen));
        SoundManager.get().play(SoundManager.Sfx.PORTAL);
    }

    // Áp dụng các hiệu ứng đang hoạt động cho một quả bóng mới
    public void applyActiveEffectsToBall(Ball bl, List<ActiveEffect> activeEffects) {
        double radiusMultiplier = 1.0;
        double speedMultiplier = 1.0;
        for (ActiveEffect ae : activeEffects) {
            if (ae.type == PowerUp.PowerType.TINY_BALL) radiusMultiplier *= 0.6;
            if (ae.type == PowerUp.PowerType.SLOW_BALL) speedMultiplier *= 0.7;
            if (ae.type == PowerUp.PowerType.FAST_BALL) speedMultiplier *= 1.3;
            if (ae.type == PowerUp.PowerType.FIREBALL) {
                ae.originalFireball.put(bl, bl.isFireball());
                bl.setFireball(true);
            }
        }
        if (radiusMultiplier != 1.0) {
            bl.setRadius(bl.getRadius() * radiusMultiplier);
        }
        if (speedMultiplier != 1.0) {
            bl.setBaseSpeed(bl.getBaseSpeed() * speedMultiplier);
        }
    }

    public void triggerWeaken(Paddle paddle) {
        if (weakenInProgress) return; // ignore if one active
        // origin = paddle center top (or center)
        double ox = paddle.getX() + paddle.getWidth() * 0.5;
        double oy = paddle.getY() + paddle.getHeight() * 0.5;
        // compute maxRadius = diagonal of screen
        double maxR = Math.hypot(800, 600) * 1.1;
        double speed = Math.max(800, 600) * 2.4; // px/s (fast) -> ~2.4 screen widths per second
        double thickness = 18.0; // ring thickness
        currentShockwave = new Shockwave(ox, oy, speed, maxR, thickness);
        weakenInProgress = true;
        shockAffected.clear();

    }

    public void triggerNextLevelEffect(double px, double py, EntityManager entities) {
        if (nextLevelInProgress) return;

        // chọn vị trí cổng
        portalX = (px > 0) ? px : 800 * 0.5;
        portalY = (py > 0) ? py : 600 * 0.45; // hơi lên trên trung tâm để cảm giác thu hút

        // thu thập những viên gạch còn lại
        List<Brick> remaining = new ArrayList<>(entities.getBricks());
        if (remaining.isEmpty()) {
            // không có gạch: chỉ cần flash và tải cấp độ tiếp theo
            whiteFlashAlpha = 1.0;
            whiteFlashActive = true;
            nextLevelInProgress = true;
            return;
        }

        entities.getBricks().clear();

        entities.getFlyingBricks().clear();

        // tạo FlyingBrick với các brick còn lại
        for (Brick b : remaining) {
            double bx = b.getX() + b.getWidth() * 0.5;
            double by = b.getY() + b.getHeight() * 0.5;
            // direction toward portal
            double dx = portalX - bx;
            double dy = portalY - by;
            double dist = Math.max(1.0, Math.hypot(dx, dy));
            double dirx = dx / dist, diry = dy / dist;
            // speed: base + factor by distance (so far bricks move faster)
            double speed = 220 + Math.random() * 180 + dist * 0.15;
            double vx = dirx * speed;
            double vy = diry * speed;
            double angV = (Math.random() - 0.5) * 8.0;
            FlyingBrick fb = new FlyingBrick(b, bx, by, vx, vy, angV);
            entities.getFlyingBricks().add(fb);
        }

        // khởi tạo hình ảnh cổng
        portalBaseRadius = 16.0;
        portalGlow = 0.0;
        whiteFlashAlpha = 0.0;
        whiteFlashActive = false;
        nextLevelInProgress = true;
    }
}

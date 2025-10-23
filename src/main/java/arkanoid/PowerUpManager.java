package arkanoid;

import java.util.*;

public class PowerUpManager {
    private final Random random = new Random();

    public PowerUpManager() {}

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
                        // Dự phòng: nếu không có map originalRadii (ví dụ load từ save), khôi phục theo tỉ lệ nghịch
                        if (!ae.originalRadii.isEmpty()) {
                            for (Map.Entry<Ball, Double> e : ae.originalRadii.entrySet()) {
                                Ball bl = e.getKey();
                                if (entities.getBalls().contains(bl)) bl.setRadius(e.getValue());
                            }
                        } else {
                            for (Ball bl : entities.getBalls()) {
                                bl.setRadius(bl.getRadius() / 0.6); // revert
                            }
                        }
                        break;
                    case SLOW_BALL:
                    case FAST_BALL:
                        // Dự phòng: nếu không có map originalSpeeds, khôi phục theo tỉ lệ nghịch
                        if (!ae.originalSpeeds.isEmpty()) {
                            for (Map.Entry<Ball, Double> e : ae.originalSpeeds.entrySet()) {
                                Ball bl = e.getKey();
                                if (entities.getBalls().contains(bl)) bl.setBaseSpeed(e.getValue());
                            }
                        } else {
                            double factor = (ae.type == PowerUp.PowerType.SLOW_BALL) ? (1.0 / 0.7) : (1.0 / 1.3);
                            for (Ball bl : entities.getBalls()) {
                                bl.setBaseSpeed(bl.getBaseSpeed() * factor);
                            }
                        }
                        break;
                    case LASER_PADDLE:
                        paddle.setHasLaser(ae.originalHasLaser);
                        break;
                    case FIREBALL:
                        // Dự phòng: nếu không có map originalFireball, tắt fireball cho tất cả bóng
                        if (!ae.originalFireball.isEmpty()) {
                            for (Map.Entry<Ball, Boolean> e : ae.originalFireball.entrySet()) {
                                Ball bl = e.getKey();
                                if (entities.getBalls().contains(bl)) bl.setFireball(e.getValue());
                            }
                        } else {
                            for (Ball bl : entities.getBalls()) bl.setFireball(false);
                        }
                        break;
                    case SCORE_MULTIPLIER:
                        state.setScoreMultiplier(ae.originalScoreMultiplier);
                        break;
                    default: break;
                }
                effIt.remove();
            }
        }
    }

    // Được gọi khi paddle nhặt được power-up
    public void applyPowerUp(PowerUp.PowerType type, EntityManager entities, GameState state, Paddle paddle, double gameHeight) {
        switch (type) {
            case SHRINK_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                eff.originalPaddleWidth = paddle.getWidth();
                paddle.setWidth(eff.originalPaddleWidth * 0.7);
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("SHRINK PADDLE -10s", 2.5));
                break;
            }
            case EXPAND_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                eff.originalPaddleWidth = paddle.getWidth();
                paddle.setWidth(eff.originalPaddleWidth * 1.4);
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("EXPAND PADDLE +10s", 2.5));
                break;
            }
            case TINY_BALL: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                for (Ball bl : entities.getBalls()) {
                    eff.originalRadii.put(bl, bl.getRadius());
                    bl.setRadius(bl.getRadius() * 0.6);
                }
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("TINY BALL -10s", 2.5));
                break;
            }
            case MULTI_BALL: {
                List<Ball> newBalls = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    Ball nb = new Ball(paddle.getX() + paddle.getWidth() / 2, paddle.getY() - 10, 8, paddle);
                    nb.setBaseSpeed(300);
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
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                for (Ball bl : entities.getBalls()) {
                    eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                    bl.setBaseSpeed(bl.getBaseSpeed() * 0.7);
                }
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("SLOW BALL -8s", 2.5));
                break;
            }
            case FAST_BALL: {
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                for (Ball bl : entities.getBalls()) {
                    eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                    bl.setBaseSpeed(bl.getBaseSpeed() * 1.3);
                }
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("FAST BALL -8s", 2.5));
                break;
            }
            case NEXT_LEVEL:
                // NEXT_LEVEL được xử lý đặc biệt trong Game.java
                break;
            case EXTRA_LIFE: {
                state.incrementLives();
                entities.addHUDMessage(new HUDMessage("+1 LIFE", 2.5));
                break;
            }
            case SUDDEN_DEATH: {
                state.setLives(Math.max(1, state.getLives()));
                entities.addHUDMessage(new HUDMessage("SUDDEN DEATH", 2.5));
                break;
            }
            case LASER_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                eff.originalHasLaser = paddle.hasLaser();
                paddle.setHasLaser(true);
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("LASER PADDLE +10s", 2.5));
                break;
            }
            case FIREBALL: {
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                for (Ball bl : entities.getBalls()) {
                    eff.originalFireball.put(bl, bl.isFireball());
                    bl.setFireball(true);
                }
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("FIREBALL -8s", 2.5));
                break;
            }
            case BARRIER: {
                state.setBarrierActive(true, gameHeight - 24); // Đặt rào chắn gần đáy
                entities.addHUDMessage(new HUDMessage("BARRIER (1 lần)", 2.5));
                break;
            }
            case WEAKEN: {
                int reduced = 0;
                for (Brick br : entities.getBricks()) {
                    if (br.getType() == Brick.Type.NORMAL && br.getHits() > 1) {
                        br.weaken();
                        reduced++;
                    }
                }
                entities.addHUDMessage(new HUDMessage("WEAKEN: -" + reduced + " hits", 2.5));
                break;
            }
            case SCORE_MULTIPLIER: {
                ActiveEffect eff = new ActiveEffect(type, 15.0);
                eff.originalScoreMultiplier = state.getScoreMultiplier();
                state.setScoreMultiplier(state.getScoreMultiplier() * 2.0);
                entities.addActiveEffect(eff);
                entities.addHUDMessage(new HUDMessage("SCORE x2 -15s", 2.5));
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
        weights.put(PowerUp.PowerType.SHRINK_PADDLE, 1.0);
        weights.put(PowerUp.PowerType.EXPAND_PADDLE, 1.0);
        weights.put(PowerUp.PowerType.TINY_BALL, 0.8);
        weights.put(PowerUp.PowerType.MULTI_BALL, 1.2);
        weights.put(PowerUp.PowerType.SLOW_BALL, 0.8);
        weights.put(PowerUp.PowerType.FAST_BALL, 0.6);
        weights.put(PowerUp.PowerType.NEXT_LEVEL, 0.25);
        weights.put(PowerUp.PowerType.EXTRA_LIFE, 0.4);
        weights.put(PowerUp.PowerType.SUDDEN_DEATH, 0.2);
        weights.put(PowerUp.PowerType.LASER_PADDLE, 0.5);
        weights.put(PowerUp.PowerType.FIREBALL, 0.5);
        weights.put(PowerUp.PowerType.BARRIER, 0.6);
        weights.put(PowerUp.PowerType.WEAKEN, 0.7);
        weights.put(PowerUp.PowerType.SCORE_MULTIPLIER, 0.7);

        double total = 0; for (double v : weights.values()) total += v;
        double r = random.nextDouble() * total;
        double cum = 0;
        PowerUp.PowerType chosen = PowerUp.PowerType.SHRINK_PADDLE;
        for (Map.Entry<PowerUp.PowerType, Double> e : weights.entrySet()) {
            cum += e.getValue(); if (r <= cum) { chosen = e.getKey(); break; }
        }

        entities.addPowerUp(new PowerUp(x, y, chosen));
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

}

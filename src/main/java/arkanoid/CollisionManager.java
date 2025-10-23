package arkanoid;

import java.util.*;

public class CollisionManager {

    public CollisionManager() {}

    /**
     * Xử lý tất cả các va chạm trong game và trả về kết quả (sự kiện).
     */
    public CollisionResult handleCollisions(EntityManager entities, GameState state, Paddle paddle,
                                            PowerUpManager powerUpManager, double gameWidth, double gameHeight) {

        CollisionResult result = new CollisionResult();
        List<Ball> ballsToRemove = new ArrayList<>();

        // 1. Va chạm của Bóng
        for (Ball bl : entities.getBalls()) {
            bl.collideWithWalls(gameWidth, gameHeight);
            bl.collideWithPaddle(paddle);

            // Bóng vs Gạch
            handleBallBrickCollisions(bl, entities, state, powerUpManager);

            // Bóng vs Đáy (Mất mạng)
            if (bl.getY() > gameHeight) {
                if (state.isBarrierActive()) {
                    bl.bounceUp(); // Nảy lên
                    bl.setPositionY(state.getBarrierY() - bl.getRadius() - 1); // Đặt lại vị trí
                    state.consumeBarrier(); // Dùng mất rào chắn
                } else {
                    ballsToRemove.add(bl); // Đánh dấu để xóa
                }
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
                result.collectedPowerUps.add(pu); // Báo cáo đã nhặt
                pIt.remove(); // Xóa khỏi thế giới game
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
            if (bl.collideWithBrick(b)) {
                Brick.Type t = b.getType();

                if (bl.isFireball()) {
                    // Fireball phá hủy ngay lập tức
                    double bx = b.getX() + b.getWidth() / 2.0;
                    double by = b.getY() + b.getHeight() / 2.0;
                    it.remove();
                    state.addScore(100);
                    powerUpManager.trySpawnPowerUp(bx, by, entities);
                } else {
                    // Gạch thường
                    boolean removed = b.hit(); // Gạch bị đánh
                    if (removed) {
                        // Gạch bị phá hủy
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        it.remove();
                        state.addScore(100);
                        powerUpManager.trySpawnPowerUp(bx, by, entities);

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

    // Xử lý logic gạch nổ (chuyển từ Game.java)
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

            // 1) 8 hàng xóm để kích nổ dây chuyền
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
                        if (found.isDestructible()) toRemoveSet.add(found);
                    }
                }
            }
        }

        // Xóa tất cả các gạch trong vụ nổ
        for (Brick b : toRemoveSet) {
            if (entities.getBricks().remove(b)) {
                state.addScore(100);
            }
        }
    }
}
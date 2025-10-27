package arkanoid;

import javafx.application.Platform;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * SaveLoad: Dịch vụ Save/Load trạng thái game.
 *
 * Yêu cầu:
 * - KHÔNG autosave theo chu kỳ; chỉ lưu khi thoát (Exit hoặc đóng cửa sổ).
 * - Start New Game: KHÔNG xóa file cũ; khi thoát sẽ ghi đè file.
 * - Continue (vừa mở app, chưa có session): load từ file, và HIỆN menu Pause (Continue/Restart/Menu),
 *   KHÔNG chạy luôn. Bóng giữ nguyên trạng thái đã lưu (đang bay tiếp tục bay; dính thì chờ SPACE).
 *
 * Vị trí lưu:
 * - Ưu tiên: src/main/resources/data/save.dat (IDE/dev).
 * - Nếu chạy JAR (resources read-only): fallback {user.home}/.arkanoid/save.dat.
 */
public final class SaveLoad {
    private static final SaveLoad I = new SaveLoad();
    public static SaveLoad get() { return I; }

    private volatile boolean printedSavePathOnce = false;
    private volatile boolean printedLoadPathOnce = false;

    private SaveLoad() {}

    // Kiểm tra có file save cho Continue lần đầu khi mở app
    public boolean hasSave() {
        return Files.exists(resolveSavePath());
    }

    // Lưu trạng thái hiện tại (ghi đè). Gọi khi Exit hoặc đóng cửa sổ.
    public void save(Game game) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> save(game)); // snapshot trên FX thread
            return;
        }
        SaveData data = snapshot(game);
        Path path = resolveSavePath();
        if (!printedSavePathOnce) {
            System.out.println("[SaveLoad] Lưu vào: " + path.toAbsolutePath());
            printedSavePathOnce = true;
        }
        try {
            Files.createDirectories(path.getParent());

            // Ghi đè trực tiếp (đủ dùng vì chỉ lưu khi thoát). Có thể nâng cấp atomic move nếu muốn.
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(
                            path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
                oos.writeObject(data);
            }
        } catch (Exception ex) {
            System.err.println("Save thất bại: " + ex.getMessage());
        }
    }

    /**
     * Load dữ liệu vào Game và HIỆN menu Pause:
     * - apply(state) xong sẽ gọi game.pause() để hiển thị bảng Continue/Restart/Menu.
     * - KHÔNG “đặt lại” bóng về paddle; giữ nguyên stuck/vị trí/vận tốc như lúc lưu.
     */
    public boolean loadIntoAndPrepareContinue(Game game) {
        Path path = resolveSavePath();
        if (!Files.exists(path)) return false;
        if (!printedLoadPathOnce) {
            System.out.println("[SaveLoad] Load từ: " + path.toAbsolutePath());
            printedLoadPathOnce = true;
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            SaveData data = (SaveData) ois.readObject();
            Platform.runLater(() -> {
                apply(game, data);
                // Tắt các overlay phụ (nếu có), sau đó chuyển sang trạng thái Pause để hiện menu
                GameState st = game.getGameState();
                st.setConfirmOverlay(false);
                st.setLevelComplete(false);
                st.setGameComplete(false);
                // Đánh dấu đã có “trận đang dở” để Menu có thể coi là có session (phòng khi bạn quay lại menu)
                st.setGameStarted(true);

                // GỌI pause() để GameContainer hiển thị bảng Continue/Restart/Menu
                game.pause();

                // KHÔNG setRunning(true) và KHÔNG reset bóng; giữ nguyên đúng trạng thái đã load
                game.requestFocus();
            });
            return true;
        } catch (Exception ex) {
            System.err.println("Load thất bại: " + ex.getMessage());
            return false;
        }
    }

    // Ưu tiên dev path: src/main/resources/data/save.dat; rồi target/classes/data; cuối cùng user.home
    private Path resolveSavePath() {
        Path dev = Paths.get("src", "main", "resources", "data", "save.dat");
        if (canCreateParent(dev)) return dev;

        Path classesData = tryResolveClassesData();
        if (classesData != null && canCreateParent(classesData)) return classesData;

        Path home = Paths.get(System.getProperty("user.home"), ".arkanoid", "save.dat");
        if (!canCreateParent(home)) {
            System.err.println("Cảnh báo: không thể tạo thư mục lưu (quyền ghi?).");
        } else {
            System.err.println("Không ghi được vào resources, dùng: " + home);
        }
        return home;
    }

    private Path tryResolveClassesData() {
        try {
            URL dataUrl = SaveLoad.class.getResource("/data/");
            if (dataUrl == null) return null;
            if ("file".equals(dataUrl.getProtocol())) {
                return Paths.get(dataUrl.toURI()).resolve("save.dat");
            }
        } catch (URISyntaxException ignored) {}
        return null;
    }

    private boolean canCreateParent(Path p) {
        try { Files.createDirectories(p.getParent()); return true; }
        catch (Exception e) { return false; }
    }

    // Snapshot toàn bộ trạng thái Game
    private SaveData snapshot(Game game) {
        SaveData d = new SaveData();

        GameState gs = game.getGameState();
        d.state = GState.from(gs);

        // Lấy các thành phần private của Game qua reflection (không sửa Game.java)
        Paddle paddle = (Paddle) getField(game, "paddle");
        EntityManager em = (EntityManager) getField(game, "entityManager");
        Boss boss = (Boss) getField(game, "boss");
        d.bossLevel = getBoolean(game, "bossLevel", false);

        d.paddle = PaddleM.from(paddle);

        // Balls
        for (Ball b : em.getBalls()) d.balls.add(BallM.from(b));

        // Bricks
        for (Brick br : em.getBricks()) d.bricks.add(BrickM.from(br));

        // PowerUps / Bullets / HUD
        for (PowerUp pu : em.getPowerUps()) d.powerUps.add(PowerUpM.from(pu));
        for (Bullet bu : em.getBullets()) d.bullets.add(BulletM.from(bu));
        for (HUDMessage hm : em.getHudMessages()) d.huds.add(HUDM.from(hm));

        // ActiveEffects: map Ball -> index để khôi phục chính xác
        Map<Ball,Integer> idx = new HashMap<>();
        for (int i = 0; i < em.getBalls().size(); i++) idx.put(em.getBalls().get(i), i);
        for (ActiveEffect ae : em.getActiveEffects()) d.effects.add(EffM.from(ae, idx));

        // Boss
        d.boss = BossM.from(boss);

        return d;
    }

    // Áp dữ liệu đã load vào Game (giữ nguyên stuck/vị trí/vận tốc của bóng)
    private void apply(Game game, SaveData d) {
        GameState gs = game.getGameState();
        Paddle paddle = (Paddle) getField(game, "paddle");
        EntityManager em = (EntityManager) getField(game, "entityManager");

        gs.setRunning(false);   // dừng vòng lặp trong lúc apply
        em.clearAll();          // dọn sạch thực thể cũ

        d.state.applyTo(gs);    // GameState

        gs.setGameStarted(true); // Đánh dấu đã có session

        d.paddle.applyTo(paddle); // Paddle

        // Balls (khôi phục đầy đủ)
        List<Ball> balls = new ArrayList<>();
        for (BallM bm : d.balls) balls.add(bm.toBall(paddle));
        em.getBalls().addAll(balls);

        // Bricks
        for (BrickM bm : d.bricks) em.addBrick(bm.toBrick());

        // PowerUp / Bullet / HUD
        for (PowerUpM pm : d.powerUps) em.addPowerUp(pm.toPowerUp());
        for (BulletM bl : d.bullets) em.addBullet(bl.toBullet());
        for (HUDM hm : d.huds) em.addHUDMessage(hm.toHUD());

        // ActiveEffect
        for (EffM ef : d.effects) em.addActiveEffect(ef.toEffect(balls));

        // Boss + cờ bossLevel
        Boss newBoss = d.boss != null && d.boss.present ? d.boss.toBoss() : null;
        setField(game, "boss", newBoss);
        setBoolean(game, "bossLevel", d.bossLevel);
    }

    // ===================== Model Serializable nội bộ =====================

    private static class SaveData implements Serializable {
        String version = "1.0";
        long ts = System.currentTimeMillis();
        GState state;
        PaddleM paddle;
        List<BallM> balls = new ArrayList<>();
        List<BrickM> bricks = new ArrayList<>();
        List<PowerUpM> powerUps = new ArrayList<>();
        List<BulletM> bullets = new ArrayList<>();
        List<HUDM> huds = new ArrayList<>();
        List<EffM> effects = new ArrayList<>();
        BossM boss;
        boolean bossLevel;
    }

    private static class GState implements Serializable {
        int lives, score, currentLevelIndex;
        double scoreMul, barrierY, barrierThickness;
        boolean running, win, showMsg, gameStarted, pauseOverlay, confirmOverlay,
                barrierActive, levelComplete, gameComplete;

        static GState from(GameState s) {
            GState d = new GState();
            d.lives = s.getLives();
            d.score = s.getScore();
            d.scoreMul = s.getScoreMultiplier();
            d.running = s.isRunning();
            d.win = s.isWin();
            d.showMsg = s.isShowMessage();
            d.gameStarted = s.isGameStarted();        // ghi nhận cờ gameStarted
            d.pauseOverlay = s.isPauseOverlay();
            d.confirmOverlay = s.isConfirmOverlay();
            d.barrierActive = s.isBarrierActive();
            d.barrierY = s.getBarrierY();
            d.barrierThickness = s.getBarrierThickness();
            d.currentLevelIndex = s.getCurrentLevelIndex();
            d.levelComplete = s.isLevelComplete();
            d.gameComplete = s.isGameComplete();
            return d;
        }
        void applyTo(GameState s) {
            s.setLives(lives);
            s.setScore(score);
            s.setScoreMultiplier(scoreMul);
            s.setRunning(running);
            s.setWin(win);
            s.setShowMessage(showMsg);
            s.setPauseOverlay(pauseOverlay);
            s.setConfirmOverlay(confirmOverlay);
            if (barrierActive) s.setBarrierActive(true, barrierY); else s.consumeBarrier();
            s.setCurrentLevelIndex(currentLevelIndex);
            s.setLevelComplete(levelComplete);
            s.setGameComplete(gameComplete);
            // gameStarted không có setter public -> sẽ set bằng reflection sau load (để MenuPane nhận biết có session)
        }
    }

    private static class PaddleM implements Serializable {
        double x,y,w,h; boolean hasLaser;
        static PaddleM from(Paddle p){
            PaddleM d=new PaddleM();
            d.x=p.getX(); d.y=p.getY(); d.w=p.getWidth(); d.h=p.getHeight(); d.hasLaser=p.hasLaser();
            return d;
        }
        void applyTo(Paddle p){
            // cùng package -> gán được field protected
            p.x = x; p.y = y; p.setWidth(w); p.setHasLaser(hasLaser);
        }
    }

    private static class BallM implements Serializable {
        double cx, cy, r, vx, vy, baseSpeed; boolean stuck, fireball;
        static BallM from(Ball b){
            BallM d=new BallM();
            d.cx = b.getX() + b.getRadius(); // lưu theo tâm
            d.cy = b.getY() + b.getRadius();
            d.r  = b.getRadius();
            d.baseSpeed = b.getBaseSpeed();
            d.fireball  = b.isFireball();
            d.stuck     = b.isStuck();
            // vx, vy lấy qua reflection
            d.vx = getDouble(b,"vx",0);
            d.vy = getDouble(b,"vy",-d.baseSpeed);
            return d;
        }
        Ball toBall(Paddle p){
            Ball nb = new Ball(cx,cy,r,p);
            nb.setBaseSpeed(baseSpeed);
            nb.setVelocity(vx,vy);  // sẽ được chuẩn hóa theo baseSpeed bên trong Ball
            nb.setFireball(fireball);
            nb.setStuck(stuck);
            nb.setX(cx - r);
            nb.setY(cy - r);
            return nb;
        }
    }

    // Brick (type: 0=NORMAL, 1=INDESTRUCTIBLE, 2=EXPLOSIVE, 3=MOVING)
    private static class BrickM implements Serializable {
        int type; double x,y,w,h; int hits;
        double leftBound, rightBound, direction, x0; // MovingBrick
        static BrickM from(Brick b){
            BrickM d=new BrickM();
            d.x=b.getX(); d.y=b.getY(); d.w=b.getWidth(); d.h=b.getHeight(); d.hits=b.getHits();
            if (b instanceof MovingBrick mb){
                d.type=3;
                d.direction=getDouble(mb,"direction",1);
                d.leftBound=getDouble(mb,"leftBound",0);
                d.rightBound=getDouble(mb,"rightBound",800);
                try { d.x0 = mb.getX0(); }
                catch (Throwable t) { d.x0 = getDouble(mb, "x0", d.x); }
            } else {
                Brick.Type t=b.getType();
                d.type = (t==Brick.Type.INDESTRUCTIBLE)?1:(t==Brick.Type.EXPLOSIVE?2:0);
            }
            return d;
        }
        Brick toBrick(){
            if (type==3){
                // Khôi phục đúng bound: constructor sẽ tính
                // leftBound' = max(leftParam, x0 - range/2)
                // rightBound' = min(rightParam - w, x0 + range/2)
                // Ta muốn leftBound' == leftBound và rightBound' == rightBound đã lưu
                double leftParam  = leftBound;
                double rightParam = rightBound + w; // vì constructor sẽ trừ width

                double initX0 = (x0 == 0.0 ? x : x0); // tương thích save cũ nếu chưa có x0
                MovingBrick mb = new MovingBrick(initX0, y, w, h, Math.max(1,hits), leftParam, rightParam);

                // Hướng di chuyển
                mb.setDirection(direction);

                // Đặt lại vị trí hiện tại theo state đã lưu (kẹp trong [leftBound, rightBound - w])
                double clampedX = Math.max(leftBound, Math.min(rightBound - w, x));
                mb.setX(clampedX);

                return mb;
            }
            if (type==1) return new Brick(x,y,w,h, Brick.Type.INDESTRUCTIBLE, Integer.MAX_VALUE);
            if (type==2) return new Brick(x,y,w,h, Brick.Type.EXPLOSIVE, 1);
            return new Brick(x,y,w,h, Math.max(1,hits));
        }
    }

    private static class PowerUpM implements Serializable {
        double x,y,vy; PowerUp.PowerType type;
        static PowerUpM from(PowerUp p){ PowerUpM d=new PowerUpM(); d.x=p.x; d.y=p.y; d.vy=p.vy; d.type=p.type; return d; }
        PowerUp toPowerUp(){ PowerUp pu=new PowerUp(x,y,type); pu.vy=vy; return pu; }
    }

    private static class BulletM implements Serializable {
        double x,y,w,h; boolean alive;
        static BulletM from(Bullet b){ BulletM d=new BulletM(); d.x=b.x; d.y=b.y; d.w=b.w; d.h=b.h; d.alive=b.isAlive(); return d; }
        Bullet toBullet(){ Bullet nb=new Bullet(x+w/2.0,y+h/2.0); nb.x=x; nb.y=y; nb.w=w; nb.h=h; if(!alive) nb.kill(); return nb; }
    }

    private static class HUDM implements Serializable {
        String text; double life,maxLife;
        static HUDM from(HUDMessage h){ HUDM d=new HUDM(); d.text=h.text; d.life=h.life; d.maxLife=h.maxLife; return d; }
        HUDMessage toHUD(){ HUDMessage h=new HUDMessage(text,maxLife); h.life=life; return h; }
    }

    private static class EffM implements Serializable {
        PowerUp.PowerType type; double remaining;
        double originalScoreMul=1.0, originalPaddleW=-1; boolean originalHasLaser=false;
        Map<Integer,Double> speedByBall=new HashMap<>();
        Map<Integer,Double> radiusByBall=new HashMap<>();
        Map<Integer,Boolean> fireByBall=new HashMap<>();

        static EffM from(ActiveEffect ae, Map<Ball,Integer> index){
            EffM d=new EffM();
            d.type=ae.type; d.remaining=getDouble(ae,"remaining",0);
            d.originalScoreMul=ae.originalScoreMultiplier; d.originalPaddleW=ae.originalPaddleWidth; d.originalHasLaser=ae.originalHasLaser;
            for (var e: ae.originalSpeeds.entrySet()){ Integer i=index.get(e.getKey()); if(i!=null) d.speedByBall.put(i,e.getValue()); }
            for (var e: ae.originalRadii.entrySet()){ Integer i=index.get(e.getKey()); if(i!=null) d.radiusByBall.put(i,e.getValue()); }
            for (var e: ae.originalFireball.entrySet()){ Integer i=index.get(e.getKey()); if(i!=null) d.fireByBall.put(i,e.getValue()); }
            return d;
        }
        ActiveEffect toEffect(List<Ball> balls){
            ActiveEffect ae=new ActiveEffect(type, remaining);
            ae.originalScoreMultiplier=originalScoreMul; ae.originalPaddleWidth=originalPaddleW; ae.originalHasLaser=originalHasLaser;
            for (var e: speedByBall.entrySet()){ int i=e.getKey(); if(i>=0&&i<balls.size()) ae.originalSpeeds.put(balls.get(i), e.getValue()); }
            for (var e: radiusByBall.entrySet()){ int i=e.getKey(); if(i>=0&&i<balls.size()) ae.originalRadii.put(balls.get(i), e.getValue()); }
            for (var e: fireByBall.entrySet()){ int i=e.getKey(); if(i>=0&&i<balls.size()) ae.originalFireball.put(balls.get(i), e.getValue()); }
            return ae;
        }
    }

    private static class BossM implements Serializable {
        boolean present=false; double x,y,w,h,leftBound,rightBound,health,maxHealth; List<BossBulletM> bullets=new ArrayList<>();
        static BossM from(Boss b){
            BossM d=new BossM(); if(b==null) return d;
            d.present=true; d.x=b.getX(); d.y=b.getY(); d.w=b.getWidth(); d.h=b.getHeight();
            d.leftBound=getDouble(b,"leftBound",100); d.rightBound=getDouble(b,"rightBound",770);
            d.health=getDouble(b,"health",500); d.maxHealth=getDouble(b,"maxHealth",500);
            for (BossBullet bb: b.getBullets()) d.bullets.add(BossBulletM.from(bb));
            return d;
        }
        Boss toBoss(){
            Boss boss=new Boss(x,y,w,h,leftBound,rightBound);
            // Khớp HP bằng cách “trừ” chênh lệch (vì Boss không có setter HP)
            double curMax=getDouble(boss,"maxHealth",maxHealth);
            double cur   =getDouble(boss,"health",curMax);
            double delta =cur - health;
            if (delta>0) boss.takeDamage(delta);
            boss.getBullets().clear();
            for (BossBulletM bm: bullets) boss.getBullets().add(bm.toBullet());
            return boss;
        }
    }

    private static class BossBulletM implements Serializable {
        double x,y,w,h;
        static BossBulletM from(BossBullet b){
            BossBulletM d=new BossBulletM();
            d.x = b.getX(); d.y = b.getY(); d.w = b.getWidth(); d.h = b.getHeight();
            return d;
        }
        BossBullet toBullet(){ return new BossBullet(x,y,w,h,300); }
    }

    // ===================== Reflection helpers =====================

    private static Object getField(Object target, String name){
        try { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (Exception e){ return null; }
    }
    private static void setField(Object target, String name, Object val){
        try { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target,val); }
        catch (Exception ignored) {}
    }
    private static double getDouble(Object target,String name,double def){
        try { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); return f.getDouble(target);}
        catch(Exception e){ return def; }
    }
    private static boolean getBoolean(Object target,String name,boolean def){
        try { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); return f.getBoolean(target);}
        catch(Exception e){ return def; }
    }
    private static void setBoolean(Object target,String name,boolean val){
        try { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); f.setBoolean(target,val); }
        catch(Exception ignored){}
    }
}

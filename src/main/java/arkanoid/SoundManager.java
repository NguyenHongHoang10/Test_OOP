package arkanoid;


import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


public final class SoundManager {
    private static final SoundManager I = new SoundManager();

    public static SoundManager get() {
        return I;
    }


    // SFX key
    public enum Sfx {
        // Gameplay cơ bản
        BOUNCE_WALL,           // genericballhit.wav
        BOUNCE_PADDLE,         // steelballhit.wav
        BRICK_HIT,             // genericballhit.wav
        BRICK_BREAK,           // blockdestroyed.wav
        EXPLOSION,             // explosion.wav
        BALL_LOST,             // balllost.wav


        // Power-up
        POWER_PICK_GOOD,       // poweruppicked.wav
        POWER_PICK_BAD,        // negativepowerup.wav
        MULTIBALL,             // machinegun.wav (tạm)
        BARRIER_ON,            // electric.wav (tạm)
        BARRIER_BREAK,         // steelballhit.wav (tạm)
        SHOCKWAVE,             // electric.wav (tạm)
        PORTAL,                // electric.wav (tạm)


        // Laser / Fireball
        LASER_SHOT,            // laser.wav
        LASER_CHARGE1,         // lasercharge1.wav
        LASER_CHARGE2,         // lasercharge2.wav
        LASER_CHARGE3,         // lasercharge3.wav
        FIRE_START,            // electric.wav (tạm)
        FIRE_LOOP,             // lasercharge2.wav (loop tạm)
        FIRE_END,              // steelballhit.wav (tạm)


        // Boss
        BOSS_SHOOT,            // machinegun.wav (tạm)
        BOSS_HIT,              // steelballhit.wav (tạm)
        BOSS_DEATH,            // explosion.wav (tạm)


        // UI / Flow
        CLICK,                 // click.wav
        BUTTON,                // button.wav
        HOVER,                 // hover.wav
        PAUSE,                 // pause.wav
        GAME_OVER,             // defeat.wav
        VICTORY                // victory.wav
    }


    // BGM key
    public enum Bgm {
        INTRO,       // on-the-road-to-the-eighties_59sec-177566.wav
        STORY,       // lady-of-the-80x27s-128379.wav
        MENU,        // main-menu-space-120280.wav
        LEVEL,       // cyberpunk-2099-10701.wav
        LEVEL_ALT,   // this-minimal-technology-pure-12327.wav
        BOSS,        // cyber-attack-dark-epic-and-mystically-music-7594.wav
        VICTORY_T,   // victory.wav
        GAMEOVER_T   // defeat.wav
    }


    private final Map<Sfx, AudioClip> sfxClips = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Media> sfxMedias = new EnumMap<>(Sfx.class);
    private final Map<Sfx, MediaPlayer> loopPlayers = new EnumMap<>(Sfx.class);
    private final Map<Bgm, Media> bgmMedias = new EnumMap<>(Bgm.class);
    private MediaPlayer bgmPlayer;


    private double masterVolume = 1.0;
    private double sfxVolume = 1.0;
    private double bgmVolume = 1.0;
    private boolean muted = false;


    // cooldown để tránh spam SFX quá dày
    private final Map<Sfx, Long> lastPlayMs = new HashMap<>();
    private final long bounceCooldownMs = 40;


    public boolean isMuted() {
        return this.muted;
    }


    private SoundManager() {
        // Map Sfx đến filename trong thư mục sound
        mapSfx(Sfx.BOUNCE_WALL, "genericballhit.wav");
        mapSfx(Sfx.BOUNCE_PADDLE, "steelballhit.wav");
        mapSfx(Sfx.BRICK_HIT, "genericballhit.wav");
        mapSfx(Sfx.BRICK_BREAK, "blockdestroyed.wav");
        mapSfx(Sfx.EXPLOSION, "explosion.wav");
        mapSfx(Sfx.BALL_LOST, "balllost.wav");


        mapSfx(Sfx.POWER_PICK_GOOD, "poweruppicked.wav");
        mapSfx(Sfx.POWER_PICK_BAD, "negativepowerup.wav");
        mapSfx(Sfx.MULTIBALL, "machinegun.wav");
        mapSfx(Sfx.BARRIER_ON, "electric.wav");
        mapSfx(Sfx.BARRIER_BREAK, "steelballhit.wav");
        mapSfx(Sfx.SHOCKWAVE, "electric.wav");
        mapSfx(Sfx.PORTAL, "electric.wav");


        mapSfx(Sfx.LASER_SHOT, "laser.wav");
        mapSfx(Sfx.LASER_CHARGE1, "lasercharge1.wav");
        mapSfx(Sfx.LASER_CHARGE2, "lasercharge2.wav");
        mapSfx(Sfx.LASER_CHARGE3, "lasercharge3.wav");
        mapSfx(Sfx.FIRE_START, "electric.wav");
        mapSfx(Sfx.FIRE_LOOP, "lasercharge2.wav");
        mapSfx(Sfx.FIRE_END, "steelballhit.wav");


        mapSfx(Sfx.BOSS_SHOOT, "machinegun.wav");
        mapSfx(Sfx.BOSS_HIT, "blockdestroyed.wav");
        mapSfx(Sfx.BOSS_DEATH, "explosion.wav");


        mapSfx(Sfx.CLICK, "click.wav");
        mapSfx(Sfx.BUTTON, "button.wav");
        mapSfx(Sfx.HOVER, "hover.wav");
        mapSfx(Sfx.PAUSE, "pause.wav");
        mapSfx(Sfx.GAME_OVER, "defeat.wav");
        mapSfx(Sfx.VICTORY, "victory.wav");


        // Map Bgm -> filename
        mapBgm(Bgm.INTRO, "main-menu-space-120280.wav");
        mapBgm(Bgm.STORY, "lady-of-the-80x27s-128379.wav");
        mapBgm(Bgm.MENU, "lady-of-the-80x27s-128379.wav");
        mapBgm(Bgm.LEVEL, "cyberpunk-2099-10701.wav");
        mapBgm(Bgm.LEVEL_ALT, "this-minimal-technology-pure-12327.wav");
        mapBgm(Bgm.BOSS, "cyber-attack-dark-epic-and-mystically-music-7594.wav");
        mapBgm(Bgm.VICTORY_T, "victory.wav");
        mapBgm(Bgm.GAMEOVER_T, "defeat.wav");
    }


    private void mapSfx(Sfx key, String filename) {
        URL url = getClass().getResource("/sound/" + filename);
        if (url == null) {
            System.err.println("[Sound] Missing SFX file: " + filename);
            return;
        }
        try {
            // thử nạp AudioClip trước
            AudioClip clip = new AudioClip(url.toExternalForm());
            sfxClips.put(key, clip);
        } catch (Throwable t) {
            // fallback Media
            try {
                Media m = new Media(url.toExternalForm());
                sfxMedias.put(key, m);
            } catch (Throwable t2) {
                System.err.println("[Sound] Unsupported SFX format: " + filename + " (" + t2.getMessage() + ")");
            }
        }
    }


    private void mapBgm(Bgm key, String filename) {
        URL url = getClass().getResource("/sound/" + filename);
        if (url == null) {
            System.err.println("[Sound] Missing BGM file: " + filename);
            return;
        }
        try {
            Media media = new Media(url.toExternalForm());
            bgmMedias.put(key, media);
        } catch (Throwable t) {
            System.err.println("[Sound] Unsupported BGM format: " + filename + " (" + t.getMessage() + ")");
        }
    }

    public void play(Sfx sfx) {
        if (muted) return;
        Runnable r = () -> {
            // cooldown cho một vài SFX dễ spam
            if (sfx == Sfx.BOUNCE_WALL || sfx == Sfx.BOUNCE_PADDLE || sfx == Sfx.BRICK_HIT) {
                long now = System.currentTimeMillis();
                long last = lastPlayMs.getOrDefault(sfx, 0L);
                if (now - last < bounceCooldownMs) return;
                lastPlayMs.put(sfx, now);
            }


            AudioClip clip = sfxClips.get(sfx);
            if (clip != null) {
                clip.play(masterVolume * sfxVolume);
                return;
            }
            Media media = sfxMedias.get(sfx);
            if (media != null) {
                MediaPlayer p = new MediaPlayer(media);
                p.setVolume(muted ? 0.0 : masterVolume * sfxVolume);
                p.setOnError(() -> System.err.println("[Sound] SFX play error: " + p.getError()));
                p.setOnEndOfMedia(() -> {
                    p.stop();
                    p.dispose();
                });
                p.play();
            }
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }


    public void loop(Sfx sfx) {
        if (muted) return;
        Runnable r = () -> {
            stopLoop(sfx);
            AudioClip clip = sfxClips.get(sfx);
            if (clip != null) {
                clip.setCycleCount(AudioClip.INDEFINITE);
                clip.play(masterVolume * sfxVolume);
                return;
            }
            Media media = sfxMedias.get(sfx);
            if (media != null) {
                MediaPlayer p = new MediaPlayer(media);
                p.setCycleCount(MediaPlayer.INDEFINITE);
                p.setVolume(muted ? 0.0 : masterVolume * sfxVolume);
                loopPlayers.put(sfx, p);
                p.play();
            }
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }


    public void stopLoop(Sfx sfx) {
        System.out.println("Stop loop SFX: " + sfx);
        Runnable r = () -> {
            // Dừng MediaPlayer nếu có
            MediaPlayer p = loopPlayers.remove(sfx);
            if (p != null) {
                p.stop();
                p.dispose();
            }
            // Dừng AudioClip nếu có
            AudioClip clip = sfxClips.get(sfx);
            if (clip != null) {
                clip.stop();
            }


        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }


    public void startBgm(Bgm bgm) {
        Runnable r = () -> {
            stopBgm();
            Media media = bgmMedias.get(bgm);
            if (media == null) {
                System.err.println("[Sound] BGM not loaded: " + bgm);
                return;
            }
            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(muted ? 0.0 : masterVolume * bgmVolume);
            bgmPlayer.setOnError(() -> System.err.println("[Sound] BGM error: " + bgmPlayer.getError()));
            bgmPlayer.play();
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }


    public void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }


    public void setMasterVolume(double v) {
        masterVolume = clamp01(v);
        applyVolumes();
    }

    public void setSfxVolume(double v) {
        sfxVolume = clamp01(v);
    }

    public void setBgmVolume(double v) {
        bgmVolume = clamp01(v);
        applyVolumes();
    }


    public void setMuted(boolean m) {
        muted = m;
        applyVolumes();
        if (bgmPlayer != null) {
            if (muted) bgmPlayer.pause();
            else bgmPlayer.play();
        }
        // dừng toàn bộ loop sfx khi mute
        if (muted) {
            for (Map.Entry<Sfx, MediaPlayer> e : loopPlayers.entrySet()) {
                if (e.getValue() != null) e.getValue().pause();
            }
        } else {
            for (Map.Entry<Sfx, MediaPlayer> e : loopPlayers.entrySet()) {
                if (e.getValue() != null) e.getValue().play();
            }
        }
    }


    private void applyVolumes() {
        double bgmVol = muted ? 0.0 : masterVolume * bgmVolume;
        if (bgmPlayer != null) bgmPlayer.setVolume(bgmVol);
        // AudioClip volume áp dụng mỗi lần play(), MediaPlayer loop đã set trong loop()
    }


    private double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }


    // Tắt BGM bằng cách giảm âm lượng dần trong 'seconds' rồi stop
    public void fadeOutBgm(double seconds) {
        if (bgmPlayer == null) return;
        if (seconds <= 0) {
            stopBgm();
            return;
        }


        final double startVol = bgmPlayer.getVolume();
        final long start = System.nanoTime();
        final long durNs = (long) (seconds * 1e9);


        // Dùng AnimationTimer đơn giản
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (double) (now - start) / durNs;
                if (t >= 1.0) {
                    bgmPlayer.setVolume(0.0);
                    stopBgm();
                    stop();
                    return;
                }
                double newVol = startVol * (1.0 - t);
                bgmPlayer.setVolume(newVol);
            }
        };
        timer.start();
    }


    // Fade-out một SFX loop rồi dừng
    public void fadeOutLoop(Sfx sfx, double seconds) {
        MediaPlayer p = loopPlayers.get(sfx);
        if (p == null) {
            stopLoop(sfx);
            return;
        }
        if (seconds <= 0) {
            stopLoop(sfx);
            return;
        }


        final double startVol = p.getVolume();
        final long start = System.nanoTime();
        final long durNs = (long) (seconds * 1e9);


        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (double) (now - start) / durNs;
                if (t >= 1.0) {
                    p.setVolume(0.0);
                    stopLoop(sfx);
                    stop();
                    return;
                }
                double newVol = startVol * (1.0 - t);
                p.setVolume(newVol);
            }
        };
        timer.start();
    }
}

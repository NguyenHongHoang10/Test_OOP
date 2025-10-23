package arkanoid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/// SaveManager là Trình quản lý Lưu/Tải và lưu Ảnh chụp (GameSnapshot) vào bộ nhớ và tải lại.
/// Định dạng file: văn bản thuần
/// Đường dẫn: - Save: ~/.arkanoid/save.txt

public class SaveManager {
    private final Path dir = Paths.get(System.getProperty("user.home"), ".arkanoid");
    private final Path file = dir.resolve("save.txt");
    private static final String VERSION = "1";

    public boolean hasSave() {
        return Files.exists(file);
    }

    public boolean deleteSave() {
        try {
            if (Files.exists(file)) Files.delete(file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Ghi snapshot ra file
    public void save(GameSnapshot snap) {
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            List<String> lines = serialize(snap);
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) { }
    }

    // Tải snapshot từ file (null nếu lỗi)
    public GameSnapshot load() {
        try {
            if (!Files.exists(file)) return null;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return deserialize(lines);
        } catch (IOException e) {
            return null;
        }
    }

    // ---- Serialize (ghi) ----
    private List<String> serialize(GameSnapshot s) {
        List<String> out = new ArrayList<>();
        out.add("version=" + VERSION);
        out.add("playerName=" + esc(s.playerName));
        out.add("lives=" + s.lives);
        out.add("score=" + s.score);
        out.add("mult=" + s.scoreMultiplier);
        out.add("level=" + s.currentLevelIndex);
        out.add("barActive=" + (s.barrierActive ? 1 : 0));
        out.add("barY=" + s.barrierY);

        out.add("paddle.x=" + s.paddleX);
        out.add("paddle.y=" + s.paddleY);
        out.add("paddle.w=" + s.paddleWidth);
        out.add("paddle.h=" + s.paddleHeight);
        out.add("paddle.hasLaser=" + (s.paddleHasLaser ? 1 : 0));

        out.add("[Balls]");
        out.add("count=" + s.balls.size());
        for (int i = 0; i < s.balls.size(); i++) {
            GameSnapshot.BallData b = s.balls.get(i);
            out.add("ball=" + i +
                    ";cx=" + b.cx + ";cy=" + b.cy + ";r=" + b.radius +
                    ";vx=" + b.vx + ";vy=" + b.vy +
                    ";speed=" + b.baseSpeed +
                    ";stuck=" + (b.stuckToPaddle ? 1 : 0) +
                    ";fire=" + (b.fireball ? 1 : 0));
        }

        out.add("[Bricks]");
        out.add("count=" + s.bricks.size());
        for (int i = 0; i < s.bricks.size(); i++) {
            GameSnapshot.BrickData b = s.bricks.get(i);
            out.add("brick=" + i +
                    ";x=" + b.x + ";y=" + b.y + ";w=" + b.w + ";h=" + b.h +
                    ";type=" + b.type + ";hits=" + b.hits);
        }

        out.add("[PowerUps]");
        out.add("count=" + s.powerUps.size());
        for (int i = 0; i < s.powerUps.size(); i++) {
            GameSnapshot.PowerUpData p = s.powerUps.get(i);
            out.add("pu=" + i + ";cx=" + p.cx + ";cy=" + p.cy + ";type=" + p.type);
        }

        out.add("[Bullets]");
        out.add("count=" + s.bullets.size());
        for (int i = 0; i < s.bullets.size(); i++) {
            GameSnapshot.BulletData b = s.bullets.get(i);
            out.add("bu=" + i + ";cx=" + b.cx + ";cy=" + b.cy);
        }

        out.add("[Effects]");
        out.add("count=" + s.effects.size());
        for (int i = 0; i < s.effects.size(); i++) {
            GameSnapshot.EffectData e = s.effects.get(i);
            out.add("eff=" + i + ";type=" + e.type + ";remain=" + e.remaining
                    + ";origPW=" + e.originalPaddleWidth
                    + ";origHasLaser=" + (e.originalHasLaser ? 1 : 0)
                    + ";origMult=" + e.originalScoreMultiplier);

            // Lưu map theo cặp index:value, cách nhau bằng dấu phẩy
            out.add("effOrigSpeed=" + i + ";" + joinIntDouble(e.originalSpeeds));
            out.add("effOrigRadius=" + i + ";" + joinIntDouble(e.originalRadii));
            out.add("effOrigFire=" + i + ";" + joinIntBool(e.originalFireball));
        }
        return out;
    }

    private String joinIntDouble(List<GameSnapshot.IntDouble> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < list.size(); k++) {
            var p = list.get(k);
            if (k > 0) sb.append(",");
            sb.append(p.index).append(":").append(p.value);
        }
        return sb.toString();
    }

    private String joinIntBool(List<GameSnapshot.IntBool> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < list.size(); k++) {
            var p = list.get(k);
            if (k > 0) sb.append(",");
            sb.append(p.index).append(":").append(p.value ? 1 : 0);
        }
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace("\r", " ").replace(";", " ").replace("=", " ");
    }

    // ---- Deserialize (đọc) ----
    private GameSnapshot deserialize(List<String> lines) {
        GameSnapshot s = new GameSnapshot();
        int i = 0;

        // Đọc header key=value
        for (; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) break; // tới sections
            String[] kv = splitKV(line);
            switch (kv[0]) {
                case "version": /* ignore for now */ break;
                case "playerName": s.playerName = kv[1]; break;
                case "lives": s.lives = toInt(kv[1]); break;
                case "score": s.score = toInt(kv[1]); break;
                case "mult": s.scoreMultiplier = toDouble(kv[1]); break;
                case "level": s.currentLevelIndex = toInt(kv[1]); break;
                case "barActive": s.barrierActive = toInt(kv[1]) == 1; break;
                case "barY": s.barrierY = toDouble(kv[1]); break;

                case "paddle.x": s.paddleX = toDouble(kv[1]); break;
                case "paddle.y": s.paddleY = toDouble(kv[1]); break;
                case "paddle.w": s.paddleWidth = toDouble(kv[1]); break;
                case "paddle.h": s.paddleHeight = toDouble(kv[1]); break;
                case "paddle.hasLaser": s.paddleHasLaser = toInt(kv[1]) == 1; break;
            }
        }

        // Đọc từng section
        while (i < lines.size()) {
            String header = lines.get(i).trim();
            i++;
            if (header.equalsIgnoreCase("[Balls]")) {
                i = readBalls(lines, i, s);
            } else if (header.equalsIgnoreCase("[Bricks]")) {
                i = readBricks(lines, i, s);
            } else if (header.equalsIgnoreCase("[PowerUps]")) {
                i = readPowerUps(lines, i, s);
            } else if (header.equalsIgnoreCase("[Bullets]")) {
                i = readBullets(lines, i, s);
            } else if (header.equalsIgnoreCase("[Effects]")) {
                i = readEffects(lines, i, s);
            } else {
                // bỏ qua phần không xác định hoặc để trống
            }
        }

        return s;
    }

    private int readBalls(List<String> lines, int i, GameSnapshot s) {
        int count = 0;
        if (i < lines.size() && lines.get(i).startsWith("count=")) {
            count = toInt(lines.get(i).substring("count=".length()).trim());
            i++;
        }
        for (int k = 0; k < count && i < lines.size(); k++, i++) {
            String line = lines.get(i).trim();
            Map<String, String> map = parseRecord(line);
            GameSnapshot.BallData bd = new GameSnapshot.BallData();
            bd.cx = toDouble(map.get("cx"));
            bd.cy = toDouble(map.get("cy"));
            bd.radius = toDouble(map.get("r"));
            bd.vx = toDouble(map.get("vx"));
            bd.vy = toDouble(map.get("vy"));
            bd.baseSpeed = toDouble(map.get("speed"));
            bd.stuckToPaddle = toInt(map.get("stuck")) == 1;
            bd.fireball = toInt(map.get("fire")) == 1;
            s.balls.add(bd);
        }
        return i;
    }

    private int readBricks(List<String> lines, int i, GameSnapshot s) {
        int count = 0;
        if (i < lines.size() && lines.get(i).startsWith("count=")) {
            count = toInt(lines.get(i).substring("count=".length()).trim());
            i++;
        }
        for (int k = 0; k < count && i < lines.size(); k++, i++) {
            String line = lines.get(i).trim();
            Map<String, String> map = parseRecord(line);
            GameSnapshot.BrickData bd = new GameSnapshot.BrickData();
            bd.x = toDouble(map.get("x"));
            bd.y = toDouble(map.get("y"));
            bd.w = toDouble(map.get("w"));
            bd.h = toDouble(map.get("h"));
            bd.type = map.get("type");
            bd.hits = toInt(map.get("hits"));
            s.bricks.add(bd);
        }
        return i;
    }

    private int readPowerUps(List<String> lines, int i, GameSnapshot s) {
        int count = 0;
        if (i < lines.size() && lines.get(i).startsWith("count=")) {
            count = toInt(lines.get(i).substring("count=".length()).trim());
            i++;
        }
        for (int k = 0; k < count && i < lines.size(); k++, i++) {
            String line = lines.get(i).trim();
            Map<String, String> map = parseRecord(line);
            GameSnapshot.PowerUpData pd = new GameSnapshot.PowerUpData();
            pd.cx = toDouble(map.get("cx"));
            pd.cy = toDouble(map.get("cy"));
            pd.type = map.get("type");
            s.powerUps.add(pd);
        }
        return i;
    }

    private int readBullets(List<String> lines, int i, GameSnapshot s) {
        int count = 0;
        if (i < lines.size() && lines.get(i).startsWith("count=")) {
            count = toInt(lines.get(i).substring("count=".length()).trim());
            i++;
        }
        for (int k = 0; k < count && i < lines.size(); k++, i++) {
            String line = lines.get(i).trim();
            Map<String, String> map = parseRecord(line);
            GameSnapshot.BulletData bd = new GameSnapshot.BulletData();
            bd.cx = toDouble(map.get("cx"));
            bd.cy = toDouble(map.get("cy"));
            s.bullets.add(bd);
        }
        return i;
    }

    private int readEffects(List<String> lines, int i, GameSnapshot s) {
        int count = 0;
        if (i < lines.size() && lines.get(i).startsWith("count=")) {
            count = toInt(lines.get(i).substring("count=".length()).trim());
            i++;
        }
        // Đọc các dòng effect cơ bản + các dòng map original*
        Map<Integer, GameSnapshot.EffectData> temp = new HashMap<>();

        for (int read = 0; read < count && i < lines.size(); ) {
            String line = lines.get(i).trim();
            if (line.startsWith("eff=")) {
                Map<String, String> map = parseRecord(line);
                int idx = toInt(map.get("eff"));
                GameSnapshot.EffectData ed = new GameSnapshot.EffectData();
                ed.type = map.get("type");
                ed.remaining = toDouble(map.get("remain"));
                ed.originalPaddleWidth = toDouble(map.get("origPW"));
                ed.originalHasLaser = toInt(map.get("origHasLaser")) == 1;
                ed.originalScoreMultiplier = toDouble(map.get("origMult"));
                temp.put(idx, ed);
                i++;
                read++;
            } else if (line.startsWith("effOrigSpeed=")) {
                String[] kv = line.split("=", 2);
                String payload = kv.length > 1 ? kv[1] : "";
                String[] parts = payload.split(";", 2);
                int idx = toInt(parts[0]);
                GameSnapshot.EffectData ed = temp.computeIfAbsent(idx, k -> new GameSnapshot.EffectData());
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    for (String token : parts[1].split(",")) {
                        String[] pair = token.split(":");
                        if (pair.length == 2) {
                            ed.originalSpeeds.add(new GameSnapshot.IntDouble(toInt(pair[0]), toDouble(pair[1])));
                        }
                    }
                }
                i++;
            } else if (line.startsWith("effOrigRadius=")) {
                String[] kv = line.split("=", 2);
                String payload = kv.length > 1 ? kv[1] : "";
                String[] parts = payload.split(";", 2);
                int idx = toInt(parts[0]);
                GameSnapshot.EffectData ed = temp.computeIfAbsent(idx, k -> new GameSnapshot.EffectData());
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    for (String token : parts[1].split(",")) {
                        String[] pair = token.split(":");
                        if (pair.length == 2) {
                            ed.originalRadii.add(new GameSnapshot.IntDouble(toInt(pair[0]), toDouble(pair[1])));
                        }
                    }
                }
                i++;
            } else if (line.startsWith("effOrigFire=")) {
                String[] kv = line.split("=", 2);
                String payload = kv.length > 1 ? kv[1] : "";
                String[] parts = payload.split(";", 2);
                int idx = toInt(parts[0]);
                GameSnapshot.EffectData ed = temp.computeIfAbsent(idx, k -> new GameSnapshot.EffectData());
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    for (String token : parts[1].split(",")) {
                        String[] pair = token.split(":");
                        if (pair.length == 2) {
                            ed.originalFireball.add(new GameSnapshot.IntBool(toInt(pair[0]), toInt(pair[1]) == 1));
                        }
                    }
                }
                i++;
            } else {
                // bỏ qua dòng trống/khác
                i++;
            }
        }

        // Đổ vào snapshot
        for (int idx = 0; idx < count; idx++) {
            GameSnapshot.EffectData ed = temp.get(idx);
            if (ed != null) s.effects.add(ed);
        }
        return i;
    }

    private String[] splitKV(String line) {
        int eq = line.indexOf('=');
        if (eq < 0) return new String[]{line, ""};
        return new String[]{line.substring(0, eq).trim(), line.substring(eq + 1).trim()};
    }

    private Map<String, String> parseRecord(String line) {
        Map<String, String> map = new HashMap<>();
        for (String part : line.split(";")) {
            int idx = part.indexOf('=');
            if (idx < 0) {
                // cũng có dạng key:value cho token đầu
                idx = part.indexOf(':');
                if (idx < 0) continue;
                String k = part.substring(0, idx).trim();
                String v = part.substring(idx + 1).trim();
                map.put(k, v);
            } else {
                String k = part.substring(0, idx).trim();
                String v = part.substring(idx + 1).trim();
                map.put(k, v);
            }
        }
        return map;
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private double toDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }
}

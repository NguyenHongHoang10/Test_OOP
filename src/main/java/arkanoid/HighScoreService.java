package arkanoid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * HighScoreService
 * - Lưu/đọc bảng xếp hạng Top 10.
 * - File: data/highscore (cùng thư mục với save.dat).
 * - Định dạng TSV: "score<TAB>timestamp<TAB>name" mỗi dòng.
 */
public final class HighScoreService {

    public static final int MAX_ENTRIES = 10;
    private static final HighScoreService I = new HighScoreService();

    public static HighScoreService get() { return I; }

    private HighScoreService() {}

    public static final class Entry {
        public final String name;
        public final int score;
        public final long ts;
        public Entry(String name, int score, long ts) {
            this.name = name;
            this.score = score;
            this.ts = ts;
        }
    }

    // Cache: đọc từ file một lần, sau đó chỉ cập nhật trong RAM khi score lớn hơn
    private int cachedBest = -1;

    private boolean initialized = false;

    // API công khai

    /**
     * Trả về thứ hạng (1..10) nếu score lọt Top 10, ngược lại -1.
     */
    public synchronized int qualifyRank(int score) {
        List<Entry> list = readAll();
        list.sort(this::cmp);
        if (list.size() < MAX_ENTRIES) {
            int pos = 0;
            while (pos < list.size() && score <= list.get(pos).score) pos++;
            return pos + 1;
        } else {
            Entry last = list.get(MAX_ENTRIES - 1);
            if (score > last.score) {
                int pos = 0;
                while (pos < list.size() && score <= list.get(pos).score) pos++;
                return pos + 1;
            }
            return -1;
        }
    }

    /**
     * Ghi nhận điểm người chơi vào bảng xếp hạng (tự cắt còn Top 10).
     */
    public synchronized void submit(String rawName, int score) {
        String name = sanitize(rawName);
        long ts = System.currentTimeMillis();
        List<Entry> list = readAll();
        list.add(new Entry(name, score, ts));
        list.sort(this::cmp);
        if (list.size() > MAX_ENTRIES) {
            list = new ArrayList<>(list.subList(0, MAX_ENTRIES));
        }
        writeAll(list);
        // cập nhật cache
        if (cachedBest < score) {
            cachedBest = score;
        }
    }

    /**
     * Lấy Top n (mặc định dùng n=10).
     */
    public synchronized List<Entry> getTop(int n) {
        List<Entry> list = readAll();
        list.sort(this::cmp);
        if (list.size() > n) return new ArrayList<>(list.subList(0, n));
        return list;
    }

    /**
     * Lấy điểm cao nhất (để hiển thị HUD).
     */
    public synchronized int getBestScore() {
        return readAll().stream().mapToInt(e -> e.score).max().orElse(0);
    }

    /** Đảm bảo cachedBest đã được nạp từ file một lần. */
    private void ensureInit() {
        if (!initialized) {
            cachedBest = getBestScore(); // đọc file 1 lần
            initialized = true;
        }
    }

    /** Trả về highscore đang có trong RAM (đã load trước đó). */
    public synchronized int getCachedBest() {
        ensureInit();
        return Math.max(0, cachedBest);
    }

    /** Nếu score hiện tại vượt cachedBest thì cập nhật cachedBest (chỉ trong RAM). */
    public synchronized void maybeUpdateBest(int score) {
        ensureInit();
        if (score > cachedBest) cachedBest = score;
    }

    public Path getHighScoreFile() {
        return resolveDataDir().resolve("highscore");
    }

    // Nội bộ

    private int cmp(Entry a, Entry b) {
        if (a.score != b.score) return Integer.compare(b.score, a.score); // giảm dần
        return Long.compare(a.ts, b.ts); // cùng điểm: ai đạt trước đứng trước
    }

    private String sanitize(String s) {
        if (s == null) s = "";
        s = s.trim();
        if (s.isEmpty()) s = "Player";
        if (s.length() > 20) s = s.substring(0, 20);
        s = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        return s;
    }

    private List<Entry> readAll() {
        Path file = getHighScoreFile();
        if (!Files.exists(file)) return new ArrayList<>();
        List<Entry> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t", 3);
                if (parts.length < 3) continue;
                int score = parseInt(parts[0], 0);
                long ts = parseLong(parts[1], 0L);
                String name = parts[2];
                out.add(new Entry(name, score, ts));
            }
        } catch (IOException ignored) {}
        return out;
    }

    private void writeAll(List<Entry> entries) {
        Path file = getHighScoreFile();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Entry e : entries) {
                    bw.write(e.score + "\t" + e.ts + "\t" + e.name);
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            System.err.println("Ghi highscore thất bại: " + ex.getMessage());
        }
    }

    private int parseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private long parseLong(String s, long def) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; } }

    /**
     * Xác định thư mục data giống SaveLoad: ưu tiên src/main/resources/data,
     * nếu không -> target/classes/data, cuối cùng {user.home}/.arkanoid
     */
    private Path resolveDataDir() {
        Path dev = Paths.get("src", "main", "resources", "data");
        if (canCreate(dev)) return dev;

        Path classesData = tryResolveClassesData();
        if (classesData != null && canCreate(classesData)) return classesData;

        Path home = Paths.get(System.getProperty("user.home"), ".arkanoid");
        if (!canCreate(home)) {
            System.err.println("Cảnh báo: không thể tạo thư mục data cho highscore");
        }
        return home;
    }

    private boolean canCreate(Path dir) {
        try { Files.createDirectories(dir); return true; }
        catch (Exception e) { return false; }
    }

    private Path tryResolveClassesData() {
        try {
            java.net.URL dataUrl = HighScoreService.class.getResource("/data/");
            if (dataUrl == null) return null;
            if ("file".equals(dataUrl.getProtocol())) {
                return Paths.get(dataUrl.toURI());
            }
        } catch (Exception ignored) {}
        return null;
    }
}

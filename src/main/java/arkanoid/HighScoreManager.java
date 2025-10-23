package arkanoid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * HighScoreManager (Bảng xếp hạng) – lưu Top 10 "tên + điểm" vào bộ nhớ.
 *
 * Đường dẫn:
 *   - Highscore: ~/.arkanoid/highscores.csv
 * Định dạng:
 *   - Mỗi dòng: name,score
 *   - Dấu phẩy trong tên sẽ được thay bằng khoảng trắng để đơn giản.
 */
public class HighScoreManager {

    public static class Entry {
        public final String name;
        public final int score;
        public Entry(String name, int score) { this.name = name; this.score = score; }
    }

    private final Path dir = Paths.get(System.getProperty("user.home"), ".arkanoid");
    private final Path file = dir.resolve("highscores.csv");
    private static final int MAX = 10;

    public List<Entry> getTop10() {
        List<Entry> list = new ArrayList<>();
        try {
            if (!Files.exists(file)) return list;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    int score = 0;
                    try { score = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                    list.add(new Entry(name, score));
                }
            }
            // Sắp xếp giảm dần theo điểm
            list.sort((a, b) -> Integer.compare(b.score, a.score));
            if (list.size() > MAX) return new ArrayList<>(list.subList(0, MAX));
            return list;
        } catch (IOException e) {
            return list;
        }
    }

    // Thêm điểm mới, trả về danh sách Top10 sau khi chèn
    public List<Entry> submitScore(String name, int score) {
        String safeName = sanitize(name);
        List<Entry> list = getTop10();
        list.add(new Entry(safeName, score));
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        if (list.size() > MAX) list = new ArrayList<>(list.subList(0, MAX));
        save(list);
        return list;
    }

    public boolean isTop10Candidate(int score) {
        List<Entry> top = getTop10();
        if (top.size() < MAX) return true;
        int minTop = top.get(top.size() - 1).score;
        return score > minTop;
    }

    private void save(List<Entry> list) {
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            for (Entry e : list) {
                lines.add(sanitize(e.name) + "," + e.score);
            }
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) { }
    }

    private String sanitize(String s) {
        if (s == null) return "Player";
        return s.replace(",", " ").replace("\n", " ").replace("\r", " ").trim();
    }
}

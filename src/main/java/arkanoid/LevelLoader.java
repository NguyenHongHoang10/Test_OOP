package arkanoid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 0: trống
 * 1-9: gạch bình thường với số hits tương ứng
 * X: gạch không thể phá (indestructible)
 * E: gạch nổ (explosive)
 * W: gạch di chuyển - 1 hit
 * M: gạch di chuyển - 2 hit
 * S: gạch di chuyển - 3 hit
 * U: gạch di chuyển - 5 hit
 * B: boss
 */
public class LevelLoader {
    public static LevelData loadLevel(String resourcePath, double gameWidth) throws IOException {
        InputStream in = LevelLoader.class.getResourceAsStream(resourcePath);
        if (in == null) throw new IOException("Level resource not found: " + resourcePath);

        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        List<String> grid = new ArrayList<>();
        LevelData data = new LevelData();

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                // Nếu dòng chứa '#boss' thì bật cờ boss
                if (line.toLowerCase().contains("boss")) {
                    data.hasBoss = true;
                }
                continue;
            }
            grid.add(line);
        }

        br.close();

        if (grid.isEmpty()) return data;

        int rows = grid.size();
        int cols = grid.get(0).length();

        double startX = 30;
        double startY = 60;
        double brickW = (gameWidth - 60) / cols;
        double brickH = 24;

        for (int r = 0; r < rows; r++) {
            String row = grid.get(r);
            for (int c = 0; c < Math.min(cols, row.length()); c++) {
                char ch = row.charAt(c);
                if (ch == '0') continue;

                Brick brick = null;

                double x = startX + c * brickW;
                double y = startY + r * (brickH + 6);
                double w = brickW - 8;
                double h = brickH;

                if (ch == 'X' || ch == 'x') {
                    brick = new Brick(x, y, w, h, Brick.Type.INDESTRUCTIBLE, Integer.MAX_VALUE);
                } else if (ch == 'E' || ch == 'e') {
                    brick = new Brick(x, y, w, h, Brick.Type.EXPLOSIVE, 1);
                } else if (ch == 'W') {
                    brick = new MovingBrick(x, y, w, h, 1, MovingBrick.BrickType.WEAK);
                } else if (ch == 'M') {
                    brick = new MovingBrick(x, y, w, h, 2, MovingBrick.BrickType.MEDIUM);
                } else if (ch == 'S') {
                    brick = new MovingBrick(x, y, w, h, 3, MovingBrick.BrickType.STRONG);
                } else if (ch == 'U') {
                    brick = new MovingBrick(x, y, w, h, 4, MovingBrick.BrickType.ULTRA);
                } else if (ch == 'B' || ch == 'b') {
                    data.hasBoss = true;
                } else if (Character.isDigit(ch)) {
                    int hits = Character.getNumericValue(ch);
                    brick = new Brick(x, y, w, h, hits);
                } else {
                    // Ký tự khác thì coi như gạch thường 1 hit
                    brick = new Brick(x, y, w, h, 1);
                }

                if (brick != null) {
                    data.bricks.add(brick);
                }

                // Gạch di chuyển xen kẽ hướng
                if (r % 2 == 1 && brick instanceof MovingBrick mb) {
                    mb.setDirection(-1);
                }
            }
        }
        return data;
    }
}

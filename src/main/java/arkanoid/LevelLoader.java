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
            if (line.isEmpty()) continue;
            // dòng lưới
            grid.add(line);
        }

        br.close();

        if (grid.isEmpty()) return data; // no bricks

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
                if (ch == '0') continue; // empty
                Brick brick;
                if (ch == 'X' || ch == 'x') {
                    brick = new Brick(startX + c * brickW, startY + r * (brickH + 6), brickW - 8, brickH, Brick.Type.INDESTRUCTIBLE, Integer.MAX_VALUE);
                } else if (ch == 'E' || ch == 'e') {
                    // explosive with 1 hit
                    brick = new Brick(startX + c * brickW, startY + r * (brickH + 6), brickW - 8, brickH, Brick.Type.EXPLOSIVE, 1);
                } else if (Character.isDigit(ch)) {
                    int hits = Character.getNumericValue(ch);
                    brick = new Brick(startX + c * brickW, startY + r * (brickH + 6), brickW - 8, brickH, hits);
                } else {
                    // unknown char -> treat as normal 1-hit
                    brick = new Brick(startX + c * brickW, startY + r * (brickH + 6), brickW - 8, brickH, 1);
                }
                data.bricks.add(brick);
            }
        }

        return data;
    }
}

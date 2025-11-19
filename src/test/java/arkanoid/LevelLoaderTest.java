package arkanoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LevelLoaderTest {

    @Test
    void testBossFlagOnLevel6() throws Exception {
        LevelData data = LevelLoader.loadLevel("/levels/level6.txt", 800);
        assertTrue(data.hasBoss, "Level6.txt phải bật cờ boss");
    }

    @Test
    void testAtLeastOneLevelHasMovingBricks() throws Exception {
        String[] files = {
                "/levels/level1.txt",
                "/levels/level2.txt",
                "/levels/level3.txt",
                "/levels/level4.txt",
                "/levels/level5.txt",
                "/levels/level6.txt"
        };
        boolean hasMoving = false;
        for (String f : files) {
            LevelData d = LevelLoader.loadLevel(f, 800);
            if (d.bricks.stream().anyMatch(b -> b instanceof MovingBrick)) {
                hasMoving = true;
                break;
            }
        }
        assertTrue(hasMoving, "Ít nhất một level phải có MovingBrick (W/M/S/U) trong bộ level của bạn");
    }
}

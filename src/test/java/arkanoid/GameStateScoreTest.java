package arkanoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ test tập trung vào logic điểm số trong GameState:
 * - Cộng điểm mặc định (multiplier = 1)
 * - Cộng nhiều lần
 * - Đổi multiplier giữa chừng
 * - Reset multiplier về 1
 * - Cộng 0 điểm không đổi
 *
 * Lưu ý: Chỉ kiểm tra addScore và multiplier ở mức hành vi; không đụng JavaFX.
 */
public class GameStateScoreTest {

    @Test
    void defaultMultiplierIs1_add100_equals100() {
        GameState s = new GameState();
        s.addScore(100);
        assertEquals(100, s.getScore(), "Mặc định multiplier = 1 nên +100 phải ra 100");
    }

    @Test
    void multipleAdds_accumulate() {
        GameState s = new GameState();
        s.addScore(100);
        s.addScore(250);
        assertEquals(350, s.getScore(), "Cộng nhiều lần phải tích lũy đúng");
    }

    @Test
    void changeMultiplier_thenAffectsOnlySubsequentAdds() {
        GameState s = new GameState();
        // Lần 1: multiplier mặc định (=1)
        s.addScore(100);               // +100 -> 100

        // Lần 2: bật 2x
        s.setScoreMultiplier(2.0);
        s.addScore(100);               // +200 -> 300

        // Lần 3: đổi sang 3x
        s.setScoreMultiplier(3.0);
        s.addScore(100);               // +300 -> 600

        assertEquals(600, s.getScore(),
                "Thứ tự cộng: 100 (1x) + 100 (2x) + 100 (3x) = 600");
    }

    @Test
    void resetMultiplierTo1_thenAddNormal() {
        GameState s = new GameState();
        s.setScoreMultiplier(2.0);
        s.addScore(50);                // +100 -> 100
        // Trở về bình thường
        s.setScoreMultiplier(1.0);
        s.addScore(50);                // +50 -> 150
        assertEquals(150, s.getScore(),
                "Sau khi reset multiplier về 1, cộng điểm phải quay lại bình thường");
    }

    @Test
    void addZeroPoints_noChange() {
        GameState s = new GameState();
        s.addScore(0);
        assertEquals(0, s.getScore(), "Cộng 0 điểm thì tổng không đổi");
        s.setScoreMultiplier(3.0);
        s.addScore(0);
        assertEquals(0, s.getScore(), "Cộng 0 với multiplier khác cũng không đổi");
    }
}

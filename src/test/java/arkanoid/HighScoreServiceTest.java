package arkanoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HighScoreServiceTest {

    @Test
    void testSubmitAndBest() {
        HighScoreService svc = HighScoreService.get();
        // Nộp vài điểm mẫu
        svc.submit("Alice", 1000);
        svc.submit("Charlie", 1200);
        // Chỉ kiểm tra best ≥ 1200, không kiểm tra qualifyRank(900) vì phụ thuộc dữ liệu file hiện có
        assertTrue(svc.getBestScore() >= 1200, "Best score phải >= 1200 sau khi submit");
    }
}

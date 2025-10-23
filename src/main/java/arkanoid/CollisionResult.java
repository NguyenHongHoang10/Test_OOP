package arkanoid;

import java.util.ArrayList;
import java.util.List;

// Lớp tiện ích để truyền thông tin sự kiện từ CollisionManager về Game
public class CollisionResult {
    public boolean ballLost = false;
    public boolean allDestructibleBricksCleared = false;
    public final List<PowerUp> collectedPowerUps = new ArrayList<>();
}
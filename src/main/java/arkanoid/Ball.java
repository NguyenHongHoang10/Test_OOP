package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Ball extends GameObject {
    private double vx, vy; // vận tốc theo trục x,y
    private double speed; // vận tốc tổng cố định (để chuẩn hóa khi đổi hướng)
    private double radius; // bán kính bóng
    private boolean stuckToPaddle = true; // true nếu bóng đang nằm trên paddle
    private Paddle paddle; // tham chiếu tới paddle để đặt bóng trên paddle khi stuck
    private boolean fireball = false;     // nếu true => phá hủy mọi brick chạm phải
    private boolean penetrating = false;   // nếu true => không nảy khi va chạm brick (xuyên phá)

    public Ball(double x, double y, double radius, Paddle paddle) {
        // x,y truyền vào là tâm, chuyển về góc trái trên cho GameObject
        super(x - radius, y - radius, radius * 2, radius * 2);
        this.radius = radius;
        this.paddle = paddle;
        this.speed = 150;
        this.vx = 0;
        this.vy = -speed; // mặc định hướng lên
    }

    // Setter & Getter thêm cho Boss collision
    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    // Thiết lập vận tốc cơ bản (dùng khi load level có metadata speed)
    public void setBaseSpeed(double newSpeed) {
        this.speed = newSpeed;
        normalizeVelocity();
    }

    // Lấy base speed hiện tại
    public double getBaseSpeed() {
        return this.speed;
    }

    // Thiết lập vận tốc trực tiếp (vx, vy). Hàm sẽ chuẩn hóa để magnitude = base speed
    public void setVelocity(double newVx, double newVy) {
        this.vx = newVx;
        this.vy = newVy;
        normalizeVelocity();
    }

    // Phóng bóng khi người chơi nhấn SPACE
    public void launch() {
        if (!stuckToPaddle) return;
        stuckToPaddle = false;
        this.vy = -speed; // hướng lên
        // Thêm thành phần ngang ngẫu nhiên để tạo độ khó/độ đa dạng
        this.vx = 120 - Math.random() * 240;
        normalizeVelocity();
    }

    // Đặt bóng dính lại trên paddle (khi mất mạng hoặc restart)
    public void resetToPaddle() {
        stuckToPaddle = true;
        positionOnPaddle();
        vx = 0;
        vy = -speed;
    }

    // Đặt vị trí bóng dựa trên vị trí paddle (đặt tâm bóng trên paddle)
    private void positionOnPaddle() {
        this.x = paddle.getX() + paddle.getWidth() / 2 - radius;
        this.y = paddle.getY() - radius * 2 - 1;
    }

    // Chuẩn hóa vector vận tốc để magnitude = speed
    private void normalizeVelocity() {
        double len = Math.sqrt(vx * vx + vy * vy);
        if (len == 0) return;
        vx = vx / len * speed;
        vy = vy / len * speed;
    }

    // Cập nhật vị trí bóng; nếu đang dính paddle thì đồng bộ vị trí
    @Override
    public void update(double deltaTime) {
        if (stuckToPaddle) {
            positionOnPaddle();
            return;
        }
        x += vx * deltaTime;
        y += vy * deltaTime;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(Color.ORANGE);
        gc.fillOval(x, y, radius * 2, radius * 2);
    }

    public double centerX() {
        return x + radius;
    }

    public double centerY() {
        return y + radius;
    }

    public double getRadius() {
        return radius;
    }

    // Thiết lập bán kính mới cho quả bóng (dùng cho tiny ball)
    public void setRadius(double newRadius) {
        this.radius = newRadius;
        this.width = newRadius * 2;
        this.height = newRadius * 2;
    }

    public boolean isFireball() { return fireball; }
    public void setFireball(boolean f) { this.fireball = f; }

    // Kiểm tra va chạm và trả phản xạ với 2 cạnh tường trái/phải và trần
    public void collideWithWalls(double arenaWidth, double arenaHeight) {
        if (x <= 0) {
            x = 0;
            vx = Math.abs(vx); // bật về phải
        } else if (x + width >= arenaWidth) {
            x = arenaWidth - width;
            vx = -Math.abs(vx); // bật về trái
        }

        if (y <= 0) {
            y = 0;
            vy = Math.abs(vy); // bật xuống
        }
    }

    // Va chạm với paddle: tính góc nảy dựa trên vị trí chạm trên paddle
    public void collideWithPaddle(Paddle p) {
        if (!this.intersects(p)) return;

        // Đặt bóng ngay trên paddle để tránh dính nhiều khung
        y = p.getY() - getHeight() - 0.5;

        // Tính vị trí tương đối (-1 .. 1): -1 là mép trái, 0 giữa, +1 mép phải
        double relative = (this.centerX() - (p.getX() + p.getWidth() / 2)) / (p.getWidth() / 2);
        double maxBounceAngle = Math.toRadians(75);
        double angle = relative * maxBounceAngle;

        // Tạo vận tốc mới theo angle
        double newVy = -Math.abs(Math.cos(angle)) * speed; // luôn hướng lên
        double newVx = Math.sin(angle) * speed;

        vx = newVx;
        vy = newVy;
    }

    // Va chạm với brick: kiểm tra gần nhất giữa tâm bóng và cạnh brick
    // Trả về true nếu có va chạm (dùng để giảm HP hoặc xóa brick)
    public boolean collideWithBrick(Brick b) {
        // Sử dụng phương pháp tính độ chen (penetration) theo trục X và Y
        // dx/dy: khoảng cách từ tâm bóng đến tâm brick
        double brickCenterX = b.getX() + b.getWidth() / 2.0;
        double brickCenterY = b.getY() + b.getHeight() / 2.0;
        double dx = centerX() - brickCenterX;
        double dy = centerY() - brickCenterY;

        // Tính độ chồng lấp (overlap) theo từng trục
        double overlapX = (b.getWidth() / 2.0 + radius) - Math.abs(dx);
        double overlapY = (b.getHeight() / 2.0 + radius) - Math.abs(dy);

        // Nếu không có chồng lấp trên cả 2 trục => không va chạm
        if (overlapX <= 0 || overlapY <= 0) {
            return false;
        }

        // Quyết định phản xạ: phản xạ theo trục có độ chồng lấp nhỏ hơn
        // (nghĩa là hướng va chạm chính là theo trục đó)
        if (overlapX < overlapY) {
            // Phản xạ theo trục X (thay đổi vx)
            // Điều chỉnh vị trí sang cạnh tương ứng để tránh "dính"
            if (dx > 0) {
                // bóng ở phía phải của tâm brick => đặt sang phải
                x += overlapX;
            } else {
                // bóng ở phía trái => đặt sang trái
                x -= overlapX;
            }
            vx = -vx;
        } else if (overlapY < overlapX) {
            // Phản xạ theo trục Y (thay đổi vy)
            if (dy > 0) {
                // bóng ở phía dưới tâm brick => đặt xuống dưới
                y += overlapY;
            } else {
                // bóng ở phía trên => đặt lên trên
                y -= overlapY;
            }
            vy = -vy;
        } else {
            // Trường hợp overlapX == overlapY (va chạm góc): phản xạ cả hai thành phần
            vx = -vx;
            vy = -vy;
        }
        return true;
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public boolean isStuck() {
        return stuckToPaddle;
    }

    public void setStuck(boolean s) {
        stuckToPaddle = s;
    }

    public void setPositionY(double newY) {
        this.y = newY;
    }

    // Đảo chiều theo trục Y để bật bóng lên (dùng khi barrier chặn 1 lần).
    public void bounceUp() {
        // Đảm bảo vector hướng lên; giữ nguyên vx, chỉ đảo vy thành hướng lên.
        // Nếu muốn chính xác hơn, có thể điều chỉnh vx tùy nhu cầu.
        this.vy = -Math.abs(this.vy);
    }

    public void destroy() {
        System.out.println("Ball destroyed by boss!");
    }
    public void reverseY() {
        this.vy = -this.vy;
    }

    public void reverseX() {
        this.vx = -vx;
    }
}

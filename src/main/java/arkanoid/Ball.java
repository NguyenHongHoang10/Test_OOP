package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Ball extends GameObject {
    private double vx, vy; // vận tốc theo trục x,y
    private double speed; // vận tốc tổng cố định (để chuẩn hóa khi đổi hướng)
    private double radius; // bán kính bóng
    private boolean stuckToPaddle = true; // true nếu bóng đang nằm trên paddle
    private Paddle paddle; // tham chiếu tới paddle để đặt bóng trên paddle khi stuck
    private boolean fireball = false;     // nếu true => phá hủy mọi brick chạm phải
    private boolean penetrating = false;   // nếu true => không nảy khi va chạm brick (xuyên phá)
    private final List<TrailSegment> trail = new ArrayList<>();
    private double trailTimer = 0.0;
    private double trailInterval = 0.02;    // sinh 1 trail mỗi 0.02s (tăng để vệt mượt hơn)
    private int trailMax = 30;              // số segment tối đa giữ lại
    private double trailLife = 0.45;        // mỗi segment sống 0.45s
    private double trailRadiusMul = 0.9;    // segment radius = ball.radius * mul
    private static Image imgBallNormal; // Ảnh bóng thường
    private static Image imgFireball;   // Ảnh bóng lửa


    static {
        try {
            imgBallNormal = new Image(Ball.class.getResourceAsStream("/Image/Ball/ball.png"));
            if (imgBallNormal.isError()) imgBallNormal = null;
        } catch (Exception e) {
            System.err.println("Không tải được ảnh bóng (ball.png). Sẽ dùng màu cam dự phòng.");
            imgBallNormal = null;
        }

        try {
            // Tải ảnh fireball mới
            imgFireball = new Image(Ball.class.getResourceAsStream("/Image/Ball/fireball.png"));
            if (imgFireball.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + imgFireball.getException().getMessage());
            }
        } catch (Exception e) {
            System.err.println("LỖI: Không tải được ảnh /Image/Ball/fireball.png. " + e.getMessage());
            imgFireball = null;
        }
    }

    public Ball(double x, double y, double radius, Paddle paddle) {
        // x,y truyền vào là tâm, chuyển về góc trái trên cho GameObject
        super(x - radius, y - radius, radius * 2, radius * 2);
        this.radius = radius;
        this.paddle = paddle;
        this.speed = 200;
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

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
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
        // Thêm thành phần ngang ngẫu nhiên để tạo độ khó/độ đa dạng
        this.vx = 200 - Math.random() * 400;
        this.vy = -Math.sqrt(speed * speed - vx * vx);
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
        // quản lý trail
        trailTimer += deltaTime;
        while (trailTimer >= trailInterval) {
            trailTimer -= trailInterval;
            // thêm segment tại vị trí tâm quả bóng
            trail.add(new TrailSegment(centerX(), centerY(), radius * trailRadiusMul, trailLife));
            if (trail.size() > trailMax) {
                trail.remove(0);
            }
        }
        // update segments (và loại bỏ đã chết)
        java.util.Iterator<TrailSegment> it = trail.iterator();
        while (it.hasNext()) {
            TrailSegment s = it.next();
            s.update(deltaTime);
            if (!s.isAlive()) it.remove();
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // sao chép ảnh chụp nhanh để tránh sửa đổi đồng thời
        List<TrailSegment> snapshot = new ArrayList<>(trail);

        if (this.fireball) {
            // Additive glow: vẽ nhiều vòng lớn mờ dần với BlendMode.ADD
            gc.setGlobalBlendMode(BlendMode.ADD);
            gc.setGlobalAlpha(0.28);
            gc.setFill(Color.rgb(255, 140, 30));
            double haloR = radius * 2.4;
            gc.fillOval(centerX() - haloR, centerY() - haloR, haloR * 2, haloR * 2);
            gc.setGlobalAlpha(1.0);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);

            // ánh sáng mềm mại bên ngoài (lớn, rất mờ)
            for (TrailSegment s : snapshot) {
                double a = s.alpha(); // 0..1
                // bán kính ngoài lớn hơn bán kính đoạn để tạo ra quầng sáng mềm
                double outerR = s.radius * 2.2;
                gc.setGlobalAlpha(a * 0.12);
                gc.setFill(Color.rgb(255, 160, 40));
                gc.fillOval(s.x - outerR, s.y - outerR, outerR * 2, outerR * 2);
            }

            // ánh sáng bên trong (sáng hơn)
            for (TrailSegment s : snapshot) {
                double a = s.alpha();
                double innerR = s.radius * 1.15;
                gc.setGlobalAlpha(a * 0.28);
                gc.setFill(Color.rgb(255, 100, 20));
                gc.fillOval(s.x - innerR, s.y - innerR, innerR * 2, innerR * 2);
            }

            // lõi tròn nhỏ cho vẻ ngoài sắc nét hơn
            for (TrailSegment s : snapshot) {
                double a = s.alpha();
                gc.setGlobalAlpha(a * 0.9);
                gc.setFill(Color.rgb(255, 220, 140));
                gc.fillOval(s.x - s.radius, s.y - s.radius, s.radius * 2, s.radius * 2);
            }

            // khôi phục sự pha trộn và alpha
            gc.setGlobalAlpha(1.0);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        }

        for (TrailSegment s : snapshot) {
            double a = s.alpha();
            gc.setGlobalAlpha(a * 0.85);
            gc.setFill(Color.ORANGE.deriveColor(0, 1, 1, 0.55));
            gc.fillOval(s.x - s.radius, s.y - s.radius, s.radius * 2, s.radius * 2);
            gc.setGlobalAlpha(a * 0.6);
            gc.setStroke(Color.ORANGE.deriveColor(0, 1, 1, 0.9));
            gc.strokeOval(s.x - s.radius, s.y - s.radius, s.radius * 2, s.radius * 2);
            gc.setGlobalAlpha(1.0);
        }

        // 3. Thay thế code vẽ hình tròn màu cam bằng logic if/else

        if (this.fireball && imgFireball != null) {
            // Vẽ ảnh fireball (tại tọa độ x, y của GameObject)
            gc.drawImage(imgFireball, x, y, radius * 2, radius * 2);

        } else if (imgBallNormal != null) {
            // Vẽ ảnh bóng thường
            gc.drawImage(imgBallNormal, x, y, radius * 2, radius * 2);

        } else {
            // Dự phòng (fallback): Vẽ hình tròn màu cam
            gc.setFill(javafx.scene.paint.Color.ORANGE);
            gc.fillOval(x, y, radius * 2, radius * 2);
            gc.setStroke(javafx.scene.paint.Color.DARKGRAY);
            gc.strokeOval(x, y, radius * 2, radius * 2);
        }
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

    public boolean isFireball() {
        return fireball;
    }

    public void setFireball(boolean f) {
        this.fireball = f;
    }

    // Kiểm tra va chạm và trả phản xạ với 2 cạnh tường trái/phải và trần
    public void collideWithWalls(double arenaWidth, double arenaHeight) {
        if (x <= 0) {
            x = 0;
            vx = Math.abs(vx); // bật về phải
            SoundManager.get().play(SoundManager.Sfx.BOUNCE_WALL);
        } else if (x + width >= arenaWidth) {
            x = arenaWidth - width;
            vx = -Math.abs(vx); // bật về trái
            SoundManager.get().play(SoundManager.Sfx.BOUNCE_WALL);
        }

        if (y <= 0) {
            y = 0;
            vy = Math.abs(vy); // bật xuống
            SoundManager.get().play(SoundManager.Sfx.BOUNCE_WALL);
        }
        clampBounceAngle();
    }

    // Va chạm với paddle: tính góc nảy dựa trên vị trí chạm trên paddle
    public void collideWithPaddle(Paddle p) {
        if (!this.intersects(p)) return;
        SoundManager.get().play(SoundManager.Sfx.BOUNCE_PADDLE);
        // Kiểm tra xem có phải va chạm cạnh bên hoặc phía dưới paddle không
        if (y + height > paddle.getY() + 12) {
            // Va chạm cạnh bên hoặc phía dưới paddle
            if (x + width / 2 < paddle.getX() + paddle.getWidth() / 2) {
                // Bóng ở bên trái paddle, đẩy ra bên trái
                x = paddle.getX() - width - 0.5;
            } else {
                // Bóng ở bên phải paddle, đẩy ra bên phải
                x = paddle.getX() + paddle.getWidth() + 0.5;
            }
            vx = -vx;
            vy = Math.abs(vy); // đảm bảo bóng đi xuống
            return;
        }


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
        clampBounceAngle(); // giới hạn góc nảy
        normalizeVelocity(); // chuẩn hóa lại vận tốc

    }

    // Va chạm với brick: kiểm tra gần nhất giữa tâm bóng và cạnh brick
    // Trả về true nếu có va chạm (dùng để giảm HP hoặc xóa brick)
    public boolean collideWithBrick(Brick b, boolean fball) {
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
        if (fball && b.getType() != Brick.Type.INDESTRUCTIBLE) {
            // Nếu là fireball thì không phản xạ, chỉ phá hủy brick
            return true;
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
        clampBounceAngle(); // giới hạn góc nảy
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

    // Giới hạn góc nảy để tránh bóng bay ngang quá lâu
    private void clampBounceAngle() {
        double minAngle = Math.toRadians(15); // tối thiểu 15° so với ngang
        double maxAngle = Math.toRadians(165); // tối đa 165° so với ngang

        double angle = Math.atan2(-vy, vx); // tính góc so với trục ngang

        // Ép góc về trong khoảng cho phép
        if (Math.abs(angle) < minAngle) {
            angle = angle < 0 ? -minAngle : minAngle;
        } else if (Math.abs(angle) > maxAngle) {
            angle = angle < 0 ? -maxAngle : maxAngle;
        }

        double speed = Math.sqrt(vx * vx + vy * vy);
        vx = speed * Math.cos(angle);
        vy = -speed * Math.sin(angle);
    }
}

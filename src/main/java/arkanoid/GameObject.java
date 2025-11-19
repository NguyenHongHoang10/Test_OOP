package arkanoid;

import javafx.scene.canvas.GraphicsContext;

public abstract class GameObject {
    // Vị trí
    protected double x, y;
    // Kích thước của đối tượng
    protected double width, height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // update: cập nhật trạng thái đối tượng mỗi khung hình (deltaTime tính bằng giây)
    public abstract void update(double deltaTime);

    // render: vẽ đối tượng lên GraphicsContext
    public abstract void render(GraphicsContext gc);

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    // Kiểm tra va chạm theo AABB giữa 2 GameObject (hình chữ nhật)
    public boolean intersects(GameObject other) {
        return x < other.x + other.width && x + width > other.x &&
                y < other.y + other.height && y + height > other.y;
    }
}

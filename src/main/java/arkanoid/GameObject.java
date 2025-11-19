package arkanoid;

import javafx.scene.canvas.GraphicsContext;

public abstract class GameObject {
    protected double x, y;
    protected double width, height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // cập nhật trạng thái đối tượng mỗi khung hình
    public abstract void update(double deltaTime);

    // vẽ đối tượng lên GraphicsContext
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

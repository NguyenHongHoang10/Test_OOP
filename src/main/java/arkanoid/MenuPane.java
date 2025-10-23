package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class MenuPane extends VBox {
    private final Button startBtn;
    private final Button continueBtn;
    private final Button exitBtn;

    // Thêm ô nhập tên người chơi (player name input)
    private final TextField nameField;

    public MenuPane(Runnable startCallback, Runnable continueCallback, Runnable exitCallback) {
        super(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        setPrefSize(800, 600);

        Text title = new Text("ARKANOID");
        title.setFill(Color.BLACK);
        title.setFont(Font.font(48));

        // Thêm hàng nhập tên (startCallback sẽ không nhận tên)
        Label nameLabel = new Label("Player Name:");
        nameLabel.setFont(Font.font(16));
        nameField = new TextField();
        nameField.setPromptText("Nhập tên người chơi...");
        nameField.setMaxWidth(260);

        startBtn = new Button("▶ Start Game");
        startBtn.setFont(Font.font(24));
        startBtn.setPrefWidth(240);
        startBtn.setOnAction(e -> startCallback.run());

        continueBtn = new Button("Continue");
        continueBtn.setFont(Font.font(24));
        continueBtn.setPrefWidth(240);
        continueBtn.setOnAction(e -> continueCallback.run());

        exitBtn = new Button("❌ Exit");
        exitBtn.setFont(Font.font(24));
        exitBtn.setPrefWidth(240);
        exitBtn.setOnAction(e -> exitCallback.run());

        getChildren().addAll(title, nameLabel, nameField, startBtn, continueBtn, exitBtn);
    }

    // Thêm constructor mới: truyền thẳng tên sang startCallbackWithName (đề xuất dùng)
    public MenuPane(Consumer<String> startCallbackWithName, Runnable continueCallback, Runnable exitCallback) {
        this(() -> {}, continueCallback, exitCallback);
        // ghi đè lại hành vi nút start để chuyển tên
        startBtn.setOnAction(e -> {
            String name = getPlayerName();
            startCallbackWithName.accept(name);
        });
    }

    // Thêm phần lấy tên người chơi từ ô nhập
    public String getPlayerName() {
        String s = nameField.getText();
        if (s == null || s.trim().isEmpty()) return "Player";
        return s.trim();
    }

}

package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MenuPane extends VBox {
    private final Button startBtn;
    private final Button continueBtn;
    private final Button exitBtn;

    public MenuPane(Runnable startCallback, Runnable continueCallback, Runnable exitCallback) {
        super(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        setPrefSize(800, 600);

        Text title = new Text("ARKANOID");
        title.setFill(Color.BLACK);
        title.setFont(Font.font(48));

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

        getChildren().addAll(title, startBtn, continueBtn, exitBtn);
    }
}

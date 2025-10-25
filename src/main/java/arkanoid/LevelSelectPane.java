package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class LevelSelectPane extends VBox {

    public LevelSelectPane(
            Runnable level1Callback,
            Runnable level2Callback,
            Runnable level3Callback,
            Runnable level4Callback,
            Runnable level5Callback,
            Runnable level6Callback,
            Runnable backCallback
    ) {
        super(20);

        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        setPrefSize(800, 600);

        // Tiêu đề
        Text title = new Text("CHOOSE LEVEL");
        title.setFill(Color.BLACK);
        title.setFont(Font.font(40));

        // Lưới chứa 6 ô level
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(40);
        grid.setAlignment(Pos.CENTER);

        Button level1 = createLevelButton("LEVEL 1", level1Callback);
        Button level2 = createLevelButton("LEVEL 2", level2Callback);
        Button level3 = createLevelButton("LEVEL 3", level3Callback);
        Button level4 = createLevelButton("LEVEL 4", level4Callback);
        Button level5 = createLevelButton("LEVEL 5", level5Callback);
        Button level6 = createLevelButton("LEVEL 6", level6Callback);

        // Nút Level
        grid.add(level1, 0, 0);
        grid.add(level2, 1, 0);
        grid.add(level3, 2, 0);
        grid.add(level4, 0, 1);
        grid.add(level5, 1, 1);
        grid.add(level6, 2, 1);

        // Nút Back
        Button backBtn = new Button("⬅ Back");
        backBtn.setFont(Font.font(24));
        backBtn.setPrefWidth(240);
        backBtn.setOnAction(e -> backCallback.run());

        getChildren().addAll(title, grid, backBtn);
    }

    private Button createLevelButton(String text, Runnable callback) {
        Button btn = new Button(text);
        btn.setFont(Font.font(24));
        btn.setPrefSize(180, 80);
        btn.setOnAction(e -> callback.run());
        btn.setStyle(
                "-fx-background-color: linear-gradient(#ffffff, #d0d0d0);" +
                        "-fx-border-color: #333333;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(#a0c4ff, #80b3ff);" +
                        "-fx-border-color: #333333;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(#ffffff, #d0d0d0);" +
                        "-fx-border-color: #333333;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;"
        ));
        return btn;
    }
}

package arkanoid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.image.Image;

import java.util.List;

/**
 * HighScoreUI: Gom các UI tiện ích cho HighScore
 * - createTopRightInfoBar: thanh góc phải hiển thị HighScore + Level
 * - createLeaderboardScene: màn LeaderBoard Top 10, có nút Back ở góc dưới trái
 * - promptNameIfQualified: overlay "Congratulations" nhập tên nếu lọt Top 10
 */
public final class HighScoreUI {
    private HighScoreUI() {
    }

    /* 1) LeaderBoard Scene (Top 10) */
    public static Scene createLeaderboardScene(double width, double height,
                                               HighScoreService svc,
                                               Runnable onBack) {
        BorderPane root = new BorderPane();
        root.setPrefSize(width, height);
        try {
            // 1. Tải ảnh từ thư mục resources
            String path = "/Image/Background/leaderboardBackground.jpg";
            Image bgImage = new Image(HighScoreUI.class.getResourceAsStream(path));

            if (bgImage.isError()) {
                throw new Exception("Lỗi khi tải ảnh: " + bgImage.getException().getMessage());
            }

            // 2. Tạo đối tượng BackgroundSize (che phủ 100%)
            BackgroundSize bgSize = new BackgroundSize(1.0, 1.0, true, true, false, true);

            // 3. Tạo BackgroundImage
            BackgroundImage backgroundImage = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    bgSize
            );

            // 4. Đặt nền cho BorderPane (root)
            root.setBackground(new Background(backgroundImage));

        } catch (Exception e) {
            System.err.println("Không thể tải ảnh nền leaderboardBackground.jpg. Sử dụng màu nền dự phòng.");
            e.printStackTrace();
            // Nếu tải ảnh thất bại, dùng lại màu nền cũ (từ file 18)
            root.setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        }

        Text title = new Text("LEADERBOARD");
        title.setFill(Color.BLACK);
        title.setFont(Font.font(36));
        VBox header = new VBox(title);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        root.setTop(header);

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(15);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(55);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(30);
        grid.getColumnConstraints().addAll(c1, c2, c3);

        Label hRank = boldLabel("#");
        Label hName = boldLabel("Name");
        Label hScore = boldLabel("Score");
        grid.addRow(0, hRank, hName, hScore);

        List<HighScoreService.Entry> top = svc.getTop(HighScoreService.MAX_ENTRIES);
        for (int i = 0; i < top.size(); i++) {
            HighScoreService.Entry e = top.get(i);
            grid.addRow(i + 1,
                    normalLabel(String.valueOf(i + 1)),
                    normalLabel(e.name),
                    normalLabel(String.valueOf(e.score)));
        }
        root.setCenter(grid);

        Button back = new Button("⟵ Back");
        back.setFont(Font.font(16));
        back.setOnAction(ev -> {
            if (onBack != null) onBack.run();
        });
        BorderPane.setAlignment(back, Pos.BOTTOM_LEFT);
        BorderPane.setMargin(back, new Insets(10));
        root.setBottom(back);

        return new Scene(root, width, height);
    }

    private static Label boldLabel(String s) {
        Label l = new Label(s);
        l.setTextFill(Color.BLACK);
        l.setFont(Font.font(20));
        return l;
    }

    private static Label normalLabel(String s) {
        Label l = new Label(s);
        l.setTextFill(Color.BLACK);
        l.setFont(Font.font(18));
        return l;
    }

    /* 2) Overlay nhập tên khi lọt Top 10 */
    public static boolean promptNameIfQualified(Pane overlayParent,
                                                int score,
                                                HighScoreService svc,
                                                Runnable afterSaved) {
        int rank = svc.qualifyRank(score);
        if (rank <= 0) return false;

        StackPane mask = new StackPane();
        mask.setPickOnBounds(true);
        mask.setStyle("-fx-background-color: rgba(0,0,0,0.55);");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setBackground(new Background(new BackgroundFill(Color.web("#222833"), new CornerRadii(10), Insets.EMPTY)));
        box.setBorder(new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        box.setMaxWidth(420);

        Text congrats = new Text("Congratulations!");
        congrats.setFill(Color.WHITE);
        congrats.setFont(Font.font(28));

        Text info = new Text("You reached Top " + rank + ". Please enter your name:");
        info.setFill(Color.LIGHTGRAY);
        info.setFont(Font.font(16));

        TextField nameField = new TextField();
        nameField.setPromptText("Your name (max 20 chars)");
        nameField.setPrefWidth(300);

        Label warn = new Label("");
        warn.setTextFill(Color.ORANGERED);

        Button save = new Button("Save");
        save.setDefaultButton(true);
        save.setOnAction(ev -> {
            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                warn.setText("Name is required");
                return;
            }
            svc.submit(name, score);
            overlayParent.getChildren().remove(mask);
            if (afterSaved != null) afterSaved.run();
        });

        box.getChildren().addAll(congrats, info, nameField, save, warn);
        StackPane.setAlignment(box, Pos.CENTER);
        mask.getChildren().add(box);

        overlayParent.getChildren().add(mask);
        nameField.requestFocus();
        return true;
    }
}
